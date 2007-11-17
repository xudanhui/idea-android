package org.jetbrains.android.run;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.*;
import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.process.*;
import com.intellij.execution.runners.RunnerInfo;
import com.intellij.facet.FacetManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.psi.PsiClass;
import org.jdom.Element;
import org.jetbrains.android.dom.manifest.Manifest;
import org.jetbrains.android.dom.manifest.Activity;
import org.jetbrains.android.facet.AndroidFacet;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author yole
 */
public class AndroidRunConfiguration extends ModuleBasedConfiguration {
    public AndroidRunConfiguration(String name, Project project, ConfigurationFactory factory) {
        super(name, new RunConfigurationModule(project, false), factory);
    }

    public Collection<Module> getValidModules() {
        final List<Module> result = new ArrayList<Module>();
        Module[] modules = ModuleManager.getInstance(getProject()).getModules();
        for(Module module: modules) {
            if (AndroidFacet.getInstance(module) != null) {
                result.add(module);
            }
        }
        return result;
    }

    public void readExternal(Element element) throws InvalidDataException {
        super.readExternal(element);
        readModule(element);
    }

    public void writeExternal(Element element) throws WriteExternalException {
        super.writeExternal(element);
        writeModule(element);
    }

    protected ModuleBasedConfiguration createInstance() {
        return new AndroidRunConfiguration(getName(), getProject(), AndroidRunConfigurationType.getInstance().getFactory());
    }

    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new AndroidRunConfigurationEditor(getProject());
    }

    public RunProfileState getState(DataContext context,
                                    RunnerInfo runnerInfo,
                                    RunnerSettings runnerSettings,
                                    ConfigurationPerRunnerSettings configurationSettings) throws ExecutionException {
        final Module module = getConfigurationModule().getModule();
        AndroidFacet androidFacet = FacetManager.getInstance(module).getFacetByType(AndroidFacet.ID);
        if (androidFacet == null) {
            throw new ExecutionException("No Android facet found for module");
        }
        File toolsPath = new File(androidFacet.getConfiguration().SDK_PATH, "tools");
        String fileName = SystemInfo.isWindows ? "emulator.exe" : "emulator";
        final File emulatorPath = new File(toolsPath, fileName);
        if (!emulatorPath.exists()) {
            throw new ExecutionException("Android emulator not found");
        }
        CommandLineState state = new CommandLineState(runnerSettings, configurationSettings) {
            protected GeneralCommandLine createCommandLine() throws ExecutionException {
                GeneralCommandLine commandLine = new GeneralCommandLine();
                commandLine.setExePath(emulatorPath.getAbsolutePath());
                return commandLine;
            }

            protected OSProcessHandler startProcess() throws ExecutionException {
                final AndroidEmulatorProcessHandler processHandler = new AndroidEmulatorProcessHandler(createCommandLine(), module);
                ProcessTerminatedListener.attach(processHandler);
                return processHandler;
            }
        };
        TextConsoleBuilder consoleBuilder = TextConsoleBuilderFactory.getInstance().createBuilder(getProject());
        state.setConsoleBuilder(consoleBuilder);
        return state;
    }

    private class AndroidEmulatorProcessHandler extends OSProcessHandler {
        private AndroidFacet myFacet;

        public AndroidEmulatorProcessHandler(GeneralCommandLine commandLine, final Module module) throws ExecutionException {
            super(commandLine.createProcess(), commandLine.getCommandLineString());
            myFacet = AndroidFacet.getInstance(module);

            addProcessListener(new ProcessAdapter() {
                public void startNotified(ProcessEvent event) {
                    waitForDevice();
                }
            });
        }

        private void waitForDevice() {
            Runnable startedRunnable = new Runnable() {
                public void run() {
                    deployApp();
                }
            };
            runAdbProcess(myFacet.getSdkPath(), this, startedRunnable,
                    "wait-for-device");
        }

        private void deployApp() {
            Runnable deployedRunnable = new Runnable() {
                public void run() {
                    startActivity();
                }
            };
            runAdbProcess(myFacet.getSdkPath(), this, deployedRunnable,
                    "install", myFacet.getOutputPackage());
        }

        private void startActivity() {
            Manifest manifest = myFacet.getManifest();
            if (manifest == null) return;
            String packageName = manifest.getPackage().getValue();
            List<Activity> list = manifest.getApplication().getActivities();
            if (list.isEmpty()) return;
            PsiClass aClass = list.get(0).getActivityClass().getValue();
            if (aClass == null) return;
            String activityName = packageName + "/" + aClass.getQualifiedName();
            runAdbProcess(myFacet.getSdkPath(), this, null,
                    "shell", "am", "start", "-n", activityName);
        }
    }

    private void runAdbProcess(String sdkPath, final ProcessHandler textReceiver, final Runnable terminateRunnable, String... params) {
        GeneralCommandLine cmdLine = new GeneralCommandLine();
        cmdLine.setExePath(new File(sdkPath, "tools/adb").getPath());
        cmdLine.addParameters(params);
        OSProcessHandler adbHandler;
        try {
            adbHandler = new OSProcessHandler(cmdLine.createProcess(), cmdLine.getCommandLineString());
        } catch (ExecutionException e) {
            textReceiver.notifyTextAvailable(e.getMessage(), ProcessOutputTypes.STDERR);
            return;
        }
        adbHandler.addProcessListener(new ProcessAdapter() {
            public void onTextAvailable(ProcessEvent event, Key outputType) {
                textReceiver.notifyTextAvailable(event.getText(), outputType);
            }

            public void processTerminated(ProcessEvent event) {
                if (terminateRunnable != null) {
                    terminateRunnable.run();
                }
            }
        });
        adbHandler.startNotify();
    }

}
