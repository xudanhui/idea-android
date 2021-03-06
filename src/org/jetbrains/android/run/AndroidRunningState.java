package org.jetbrains.android.run;

import com.android.ddmlib.*;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import static com.intellij.execution.process.ProcessOutputTypes.STDERR;
import static com.intellij.execution.process.ProcessOutputTypes.STDOUT;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import org.jetbrains.android.dom.manifest.Manifest;
import org.jetbrains.android.facet.AndroidFacet;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author coyote
 */
public class AndroidRunningState extends CommandLineState {
    private static final int MAX_INSTALLATION_ATTEMPT_COUNT = 5;
    private static final int MAX_LAUNCHING_ATTEMPT_COUNT = 5;
    private static final int WAITING_TIME = 5;

    private final String activityName;
    private final String packageName;
    private final AndroidFacet facet;
    private boolean debugMode;
    private DebugLauncher debugLauncher;

    private boolean stopped;
    private OSProcessHandler processHandler;

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    public void setDebugLauncher(DebugLauncher debugLauncher) {
        this.debugLauncher = debugLauncher;
    }

    protected OSProcessHandler startProcess() throws ExecutionException {
        run();
        return processHandler;
    }

    private static class MyReceiver extends MultiLineReceiver {
        private static final Pattern FAILURE = Pattern.compile("Failure\\s+\\[(.*)\\]");
        private static final Pattern ERROR = Pattern.compile("Error\\s+[Tt]ype\\s+(\\d+).*");
        private int errorType = -1;
        private String failureMessage = null;
        private StringBuilder output = new StringBuilder();

        public void processNewLines(String[] lines) {
            for (String line : lines) {
                if (line.length() > 0) {
                    Matcher failureMatcher = FAILURE.matcher(line);
                    if (failureMatcher.matches()) {
                        failureMessage = failureMatcher.group(1);
                    }
                    Matcher errorMatcher = ERROR.matcher(line);
                    if (errorMatcher.matches()) {
                        errorType = Integer.parseInt(errorMatcher.group(1));
                    }
                }
                output.append(line).append('\n');
            }
        }

        public boolean isCancelled() {
            return false;
        }
    }

    public AndroidRunningState(ExecutionEnvironment environment, AndroidFacet facet, String activityName) throws ExecutionException {
        super(environment);
        this.activityName = activityName;
        this.facet = facet;
        final Manifest manifest = facet.getManifest();
        if (manifest == null) {
            throw new ExecutionException("Can't start application");
        }
        packageName = ApplicationManager.getApplication().runReadAction(new Computable<String>() {
            public String compute() {
                return manifest.getPackage().getValue();
            }
        });
    }

    private void run() throws ExecutionException {
        launchEmulator();
        final AndroidDebugBridge.IDeviceChangeListener deviceChangeListener = new AndroidDebugBridge.IDeviceChangeListener() {
            boolean installed = false;

            public void deviceConnected(Device device) {
                processHandler.notifyTextAvailable("Device connected.\n", STDOUT);
            }

            public void deviceDisconnected(Device device) {
                processHandler.notifyTextAvailable("Device disconnected.\n", STDOUT);
            }

            public void deviceChanged(final Device device, int changeMask) {
                if (!installed && device.isOnline()) {
                    processHandler.notifyTextAvailable("Device is online.\n", STDOUT);
                    installed = true;
                    new Thread(new Runnable() {
                        public void run() {
                            if (!prepareAndStart(device) && !stopped) {
                                AndroidRunningState.this.processHandler.destroyProcess();
                            }
                        }
                    }).start();
                }
            }
        };
        AndroidDebugBridge.addDeviceChangeListener(deviceChangeListener);
        final AndroidDebugBridge.IClientChangeListener clientChangeListener = new AndroidDebugBridge.IClientChangeListener() {
            public void clientChanged(Client client, int changeMask) {
                ClientData data = client.getClientData();
                String description = data.getClientDescription();
                if (description != null && description.equals(packageName)) {
                    if (data.getDebuggerConnectionStatus() == ClientData.DEBUGGER_WAITING) {
                        if (debugLauncher != null) {
                            String port = Integer.toString(client.getDebuggerListenPort());
                            debugLauncher.launchDebug(port);
                        }
                    }
                }
            }
        };
        AndroidDebugBridge.addClientChangeListener(clientChangeListener);
        processHandler.addProcessListener(new ProcessAdapter() {
            @Override
            public void processWillTerminate(ProcessEvent event, boolean willBeDestroyed) {
                AndroidDebugBridge.removeDeviceChangeListener(deviceChangeListener);
                AndroidDebugBridge.removeClientChangeListener(clientChangeListener);
                stopped = true;
                synchronized (AndroidRunningState.this) {
                    AndroidRunningState.this.notifyAll();
                }
            }
        });
    }

    private void launchEmulator() throws ExecutionException {
        String emulatorPath = facet.getConfiguration().getToolPath("emulator");
        Process process;
        try {
            process = Runtime.getRuntime().exec(emulatorPath);
        } catch (IOException e) {
            throw new ExecutionException("Can't launch android emulator (I/O error)");
        }
        processHandler = new OSProcessHandler(process, "");
    }

    public OSProcessHandler getProcessHandler() {
        return processHandler;
    }

    private boolean prepareAndStart(Device device) {
        String remotePath = "/data/local/tmp/" + packageName;
        String localPath = facet.getOutputPackage();
        if (!uploadApp(device, remotePath, localPath)) return false;
        if (!installApp(device, remotePath)) return false;
        return launchApp(device, packageName);
    }

    private boolean uploadApp(Device device, String remotePath, String localPath) {
        if (stopped) return false;
        processHandler.notifyTextAvailable("Uploading file\n\tlocal path: " + localPath + "\n\tremote path: "
                + remotePath + '\n', STDOUT);
        SyncService service = device.getSyncService();
        if (service == null) {
            processHandler.notifyTextAvailable("Can't upload file: device is not available.\n", STDERR);
            return false;
        }
        SyncService.SyncResult result = service.pushFile(localPath, remotePath,
                SyncService.getNullProgressMonitor());
        if (result.getCode() != SyncService.RESULT_OK) {
            processHandler.notifyTextAvailable("Can't upload file: " + result.getMessage() + ".\n", STDERR);
            return false;
        }
        return true;
    }

    private synchronized boolean launchApp(final Device device, final String packageName) {
        final String activityPath = packageName + '/' + activityName;
        processHandler.notifyTextAvailable("Launching application: " + activityPath + ".\n", STDOUT);
        MyReceiver receiver = new MyReceiver();
        int attemptCount = 0;
        while (true) {
            if (stopped) return false;
            try {
                device.executeShellCommand("am start " + (debugMode ? "-D " : "") +
                        "-n \"" + activityPath + "\"", receiver);
            } catch (IOException e) {
                processHandler.notifyTextAvailable("Can't launch application (I/O error).\n", STDERR);
            }
            if (receiver.errorType != 2 || attemptCount >= MAX_LAUNCHING_ATTEMPT_COUNT) {
                break;
            }
            processHandler.notifyTextAvailable("Device is not ready. Waiting for " + WAITING_TIME + " sec.\n", STDOUT);
            try {
                wait(WAITING_TIME * 1000);
            } catch (InterruptedException e) {
            }
            receiver = new MyReceiver();
        }
        boolean success = receiver.errorType == -1;
        if (success) {
            processHandler.notifyTextAvailable(receiver.output.toString(), STDOUT);
        } else {
            processHandler.notifyTextAvailable(receiver.output.toString(), STDERR);
        }
        return success;
    }

    private synchronized boolean installApp(Device device, String remotePath) {
        processHandler.notifyTextAvailable("Installing application.\n", STDOUT);
        int attemptCount = 0;
        MyReceiver receiver = new MyReceiver();
        while (true) {
            if (stopped) return false;
            try {
                device.executeShellCommand("pm install \"" + remotePath + "\"", receiver);
            } catch (IOException e) {
                processHandler.notifyTextAvailable("Can't install application (I/O error).\n", STDERR);
                return false;
            }
            if (receiver.errorType != 1 || attemptCount >= MAX_INSTALLATION_ATTEMPT_COUNT) {
                break;
            }
            processHandler.notifyTextAvailable("Device is not ready. Waiting for " + WAITING_TIME + " sec.\n", STDOUT);
            attemptCount++;
            try {
                wait(WAITING_TIME * 1000);
            } catch (InterruptedException e) {
            }
            receiver = new MyReceiver();
        }
        if (receiver.failureMessage != null && receiver.failureMessage.equals("INSTALL_FAILED_ALREADY_EXISTS")) {
            if (stopped) return false;
            receiver = new MyReceiver();
            processHandler.notifyTextAvailable("Application is already installed. Reinstalling.\n", STDOUT);
            try {
                device.executeShellCommand("pm install -r \"" + remotePath + '\"', receiver);
            } catch (IOException e) {
                processHandler.notifyTextAvailable("Can't reinstall application (I/O error).\n", STDERR);
                return false;
            }
        }
        boolean success = receiver.errorType == -1 && receiver.failureMessage == null;
        processHandler.notifyTextAvailable(receiver.output.toString(), success ? STDOUT : STDERR);
        return success;
    }
}
