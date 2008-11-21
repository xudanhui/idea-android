package org.jetbrains.android.run;

import com.android.ddmlib.*;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import static com.intellij.execution.process.ProcessOutputTypes.STDERR;
import static com.intellij.execution.process.ProcessOutputTypes.STDOUT;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Key;
import org.jetbrains.android.dom.manifest.Manifest;
import org.jetbrains.android.facet.AndroidFacet;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author coyote
 */
public class ApplicationRunner {
    private static final int MAX_INSTALLATION_ATTEMPT_COUNT = 5;
    private static final int MAX_LAUNCHING_ATTEMPT_COUNT = 5;
    private static final int WAITING_TIME = 5;

    private final String activityName;
    private final String packageName;
    private final AndroidFacet facet;
    private final boolean debugMode;

    private OSProcessHandler emulatorHandler;
    private ProgressDialog progressDialog = null;
    private String debugPort = null;

    public ApplicationRunner(AndroidFacet facet, String activityName, boolean debugMode) throws ExecutionException {
        this.activityName = activityName;
        this.facet = facet;
        this.debugMode = debugMode;
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

    public void run() throws ExecutionException {
        launchEmulator();
        final AndroidDebugBridge.IDeviceChangeListener deviceChangeListener = new AndroidDebugBridge.IDeviceChangeListener() {
            boolean installed = false;

            public synchronized void deviceConnected(Device device) {
                printText("Device connected.\n", STDOUT);
            }

            public void deviceDisconnected(Device device) {
                printText("Device disconnected.\n", STDOUT);
            }

            public void deviceChanged(Device device, int changeMask) {
                if (!installed && device.isOnline()) {
                    printText("Device is online.\n", STDOUT);
                    installed = true;
                    if (!prepareAndStart(device)) {
                        emulatorHandler.destroyProcess();
                    }
                }
            }
        };
        if (debugMode) {
            progressDialog = new ProgressDialog();
        }
        AndroidDebugBridge.addDeviceChangeListener(deviceChangeListener);
        final AndroidDebugBridge.IClientChangeListener clientChangeListener = new AndroidDebugBridge.IClientChangeListener() {
            public void clientChanged(Client client, int changeMask) {
                ClientData data = client.getClientData();
                String description = data.getClientDescription();
                if (description != null && description.equals(packageName)) {
                    if (data.getDebuggerConnectionStatus() == ClientData.DEBUGGER_WAITING) {
                        debugPort = Integer.toString(client.getDebuggerListenPort());
                        progressDialog.dispose();
                    }
                }
            }
        };
        AndroidDebugBridge.addClientChangeListener(clientChangeListener);
        emulatorHandler.addProcessListener(new ProcessAdapter() {
            @Override
            public void processWillTerminate(ProcessEvent event, boolean willBeDestroyed) {
                AndroidDebugBridge.removeDeviceChangeListener(deviceChangeListener);
                AndroidDebugBridge.removeClientChangeListener(clientChangeListener);
            }
        });
        if (debugMode) {
            progressDialog.pack();
            progressDialog.setLocationByPlatform(true);
            progressDialog.setVisible(true);
            if (progressDialog.isCanceled()) {
                if (!emulatorHandler.isProcessTerminated()) {
                    emulatorHandler.destroyProcess();
                }
            }
        }
    }

    public OSProcessHandler getProcessHandler() {
        return emulatorHandler;
    }

    private void launchEmulator() throws ExecutionException {
        String emulatorPath = facet.getConfiguration().getToolPath("emulator");
        Process process;
        try {
            process = Runtime.getRuntime().exec(emulatorPath);
        } catch (IOException e) {
            throw new ExecutionException("Can't launch android emulator (I/O error)");
        }
        emulatorHandler = new OSProcessHandler(process, "");
    }

    private boolean prepareAndStart(Device device) {
        String remotePath = "/data/local/tmp/" + packageName;
        String localPath = facet.getOutputPackage();
        if (!uploadApp(emulatorHandler, device, remotePath, localPath)) return false;
        if (!installApp(device, remotePath)) return false;
        return launchApp(device, packageName);
    }

    private boolean uploadApp(ProcessHandler handler, Device device, String remotePath, String localPath) {
        this.printText("Uploading file\n\tlocal path: " + localPath + "\n\tremote path: "
                + remotePath + '\n', STDOUT);
        SyncService service = device.getSyncService();
        SyncService.SyncResult result = service.pushFile(localPath, remotePath,
                SyncService.getNullProgressMonitor());
        if (result.getCode() != SyncService.RESULT_OK) {
            handler.notifyTextAvailable("Can't upload file: " + result.getMessage() + ".\n", STDERR);
            return false;
        }
        return true;
    }

    private void printText(String message, Key outputType) {
        if (progressDialog == null || !progressDialog.isVisible()) {
            emulatorHandler.notifyTextAvailable(message, outputType);
        } else {
            progressDialog.appendText(message);
        }
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

    private boolean launchApp(final Device device, final String packageName) {
        final String activityPath = packageName + '/' + activityName;
        this.printText("Launching application: " + activityPath + ".\n", STDOUT);
        MyReceiver receiver = new MyReceiver();
        int attemptCount = 0;
        while (true) {
            try {
                device.executeShellCommand("am start " + (debugMode ? "-D " : "") +
                        "-n \"" + activityPath + "\"", receiver);
            } catch (IOException e) {
                this.printText("Can't launch application (I/O error).\n", STDERR);
            }
            if (receiver.errorType != 2 || attemptCount >= MAX_LAUNCHING_ATTEMPT_COUNT) {
                break;
            }
            this.printText("Device is not ready. Waiting for " + WAITING_TIME + " sec.\n", STDOUT);
            try {
                Thread.sleep(WAITING_TIME * 1000);
            } catch (InterruptedException e) {
            }
            receiver = new MyReceiver();
        }
        boolean success = receiver.errorType == -1;
        if (success) {
            this.printText(receiver.output.toString(), STDOUT);
        } else {
            this.printText(receiver.output.toString(), STDERR);
        }
        return success;
    }

    private boolean installApp(Device device, String remotePath) {
        printText("Installing application.\n", STDOUT);
        int attemptCount = 0;
        MyReceiver receiver = new MyReceiver();
        while (true) {
            try {
                device.executeShellCommand("pm install \"" + remotePath + "\"", receiver);
            } catch (IOException e) {
                this.printText("Can't install application (I/O error).\n", STDERR);
            }
            if (receiver.errorType != 1 || attemptCount >= MAX_INSTALLATION_ATTEMPT_COUNT) {
                break;
            }
            this.printText("Device is not ready. Waiting for " + WAITING_TIME + " sec.\n", STDOUT);
            attemptCount++;
            try {
                Thread.sleep(WAITING_TIME * 1000);
            } catch (InterruptedException e) {
            }
            receiver = new MyReceiver();
        }
        if (receiver.failureMessage != null && receiver.failureMessage.equals("INSTALL_FAILED_ALREADY_EXISTS")) {
            receiver = new MyReceiver();
            this.printText("Application is already installed. Reinstalling.\n", STDOUT);
            try {
                device.executeShellCommand("pm install -r \"" + remotePath + '\"', receiver);
            } catch (IOException e) {
                this.printText("Can't reinstall application (I/O error).\n", STDERR);
            }
        }
        boolean success = receiver.errorType == -1 && receiver.failureMessage == null;
        this.printText(receiver.output.toString(), success ? STDOUT : STDERR);
        return success;
    }

    public String getDebugPort() {
        return debugPort;
    }
}
