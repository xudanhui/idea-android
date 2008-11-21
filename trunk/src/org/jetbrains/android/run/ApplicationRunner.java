package org.jetbrains.android.run;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.Device;
import com.android.ddmlib.MultiLineReceiver;
import com.android.ddmlib.SyncService;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import static com.intellij.execution.process.ProcessOutputTypes.STDERR;
import static com.intellij.execution.process.ProcessOutputTypes.STDOUT;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.SystemInfo;
import org.jetbrains.android.dom.manifest.Manifest;
import org.jetbrains.android.facet.AndroidFacet;

import java.io.File;
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
    private final AndroidFacet facet;

    private OSProcessHandler emulatorHandler;

    private static boolean ddmLibInitialized;

    public ApplicationRunner(AndroidFacet facet, String activityName) {
        this.activityName = activityName;
        this.facet = facet;
    }

    public void run() throws ExecutionException {
        if (!ddmLibInitialized) {
            AndroidDebugBridge.init(true);
            ddmLibInitialized = true;
        }
        final AndroidDebugBridge.IDeviceChangeListener deviceChangeListener = new AndroidDebugBridge.IDeviceChangeListener() {
            boolean installed = false;

            public void deviceConnected(Device device) {
            }

            public void deviceDisconnected(Device device) {
            }

            public void deviceChanged(Device device, int changeMask) {
                if (!installed && device.isOnline()) {
                    installed = true;
                    if (!prepareAndStart(device)) {
                        emulatorHandler.destroyProcess();
                    }
                }
            }
        };
        AndroidDebugBridge.addDeviceChangeListener(deviceChangeListener);
        AndroidDebugBridge.createBridge(getToolPath("adb"), true);
        try {
            launchEmulator();
        }
        catch (ExecutionException e) {
            AndroidDebugBridge.removeDeviceChangeListener(deviceChangeListener);
            AndroidDebugBridge.disconnectBridge();
            AndroidDebugBridge.terminate();
            throw e;
        }
        emulatorHandler.addProcessListener(new ProcessAdapter() {
            @Override
            public void processWillTerminate(ProcessEvent event, boolean willBeDestroyed) {
                AndroidDebugBridge.removeDeviceChangeListener(deviceChangeListener);
                AndroidDebugBridge.disconnectBridge();
                AndroidDebugBridge.terminate();
                emulatorHandler.notifyTextAvailable("Disconnected\n", STDOUT);
            }
        });
    }

    public OSProcessHandler getProcessHandler() {
        return emulatorHandler;
    }

    private void launchEmulator() throws ExecutionException {
        String emulatorPath = getToolPath("emulator");
        Process process;
        try {
            process = Runtime.getRuntime().exec(emulatorPath);
        } catch (IOException e) {
            throw new ExecutionException("Can't launch android emulator (I/O error)");
        }
        emulatorHandler = new OSProcessHandler(process, "");
    }

    private String getToolPath(String toolName) {
        if (SystemInfo.isWindows) {
            toolName += ".exe";
        }
        String sdkPath = facet.getConfiguration().SDK_PATH;
        File file = new File(new File(sdkPath, "tools"), toolName);
        return file.getAbsolutePath();
    }

    private boolean prepareAndStart(Device device) {
        final Manifest manifest = facet.getManifest();
        if (manifest == null) {
            emulatorHandler.notifyTextAvailable("Can't start application\n", STDERR);
        }
        String packageName = ApplicationManager.getApplication().runReadAction(new Computable<String>() {
            public String compute() {
                return manifest.getPackage().getValue();
            }
        });
        String remotePath = "/data/local/tmp/" + packageName;
        String localPath = facet.getOutputPackage();

        if (!uploadApp(emulatorHandler, device, remotePath, localPath)) return false;
        if (!installApp(device, emulatorHandler, remotePath)) return false;
        return launchApp(emulatorHandler, device, packageName);
    }

    private boolean uploadApp(ProcessHandler handler, Device device, String remotePath, String localPath) {
        handler.notifyTextAvailable("Uploading file\n\tlocal path: " +
                localPath + "\n\tremote path: " + remotePath + '\n', STDOUT);
        SyncService service = device.getSyncService();
        SyncService.SyncResult result = service.pushFile(localPath, remotePath,
                SyncService.getNullProgressMonitor());
        if (result.getCode() != SyncService.RESULT_OK) {
            handler.notifyTextAvailable("Can't upload file: " + result.getMessage() + '\n', STDERR);
            return false;
        }
        return true;
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

    private boolean launchApp(final ProcessHandler handler, final Device device, final String packageName) {
        final String activityPath = packageName + '/' + activityName;
        handler.notifyTextAvailable("Launching application: " + activityPath + '\n', STDOUT);
        MyReceiver receiver = new MyReceiver();
        int attemptCount = 0;
        while (true) {
            try {
                device.executeShellCommand("am start -n \"" + activityPath + "\"", receiver);
            } catch (IOException e) {
                handler.notifyTextAvailable("Can't launch application (I/O error).\n", STDERR);
            }
            if (receiver.errorType != 2 || attemptCount >= MAX_LAUNCHING_ATTEMPT_COUNT) {
                break;
            }
            handler.notifyTextAvailable("Device is not ready. Waiting for " + WAITING_TIME + " sec.\n", STDOUT);
            try {
                Thread.sleep(WAITING_TIME * 1000);
            } catch (InterruptedException e) {
            }
            receiver = new MyReceiver();
        }
        boolean success = receiver.errorType == -1;
        if (success) {
            handler.notifyTextAvailable(receiver.output.toString(), STDOUT);
        } else {
            handler.notifyTextAvailable(receiver.output.toString(), STDERR);
        }
        return success;
    }

    private boolean installApp(Device device, ProcessHandler handler, String remotePath) {
        handler.notifyTextAvailable("Installing application.\n", STDOUT);
        int attemptCount = 0;
        MyReceiver receiver = new MyReceiver();
        while (true) {
            try {
                device.executeShellCommand("pm install \"" + remotePath + "\"", receiver);
            } catch (IOException e) {
                handler.notifyTextAvailable("Can't install application (I/O error).\n", STDERR);
            }
            if (receiver.errorType != 1 || attemptCount >= MAX_INSTALLATION_ATTEMPT_COUNT) {
                break;
            }
            handler.notifyTextAvailable("Device is not ready. Waiting for " + WAITING_TIME + " sec.\n", STDOUT);
            attemptCount++;
            try {
                Thread.sleep(WAITING_TIME * 1000);
            } catch (InterruptedException e) {
            }
            receiver = new MyReceiver();
        }
        if (receiver.failureMessage != null && receiver.failureMessage.equals("INSTALL_FAILED_ALREADY_EXISTS")) {
            receiver = new MyReceiver();
            handler.notifyTextAvailable("Application is already installed. Reinstalling.\n", STDOUT);
            try {
                device.executeShellCommand("pm install -r \"" + remotePath + '\"', receiver);
            } catch (IOException e) {
                handler.notifyTextAvailable("Can't reinstall application (I/O error).\n", STDERR);
            }
        }
        boolean success = receiver.errorType == -1 && receiver.failureMessage == null;
        handler.notifyTextAvailable(receiver.output.toString(), success ? STDOUT : STDERR);
        return success;
    }
}
