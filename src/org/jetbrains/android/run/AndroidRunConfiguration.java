package org.jetbrains.android.run;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.*;
import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.runners.JavaProgramRunner;
import com.intellij.execution.runners.RunnerInfo;
import com.intellij.facet.FacetManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.JDOMExternalizable;
import com.intellij.openapi.util.SystemInfo;
import org.jetbrains.android.facet.AndroidFacet;

import java.io.File;

/**
 * @author yole
 */
public class AndroidRunConfiguration extends RunConfigurationBase {
    protected AndroidRunConfiguration(Project project, ConfigurationFactory factory, String name) {
        super(project, factory, name);
    }

    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new AndroidRunConfigurationEditor();
    }

    public JDOMExternalizable createRunnerSettings(ConfigurationInfoProvider provider) {
        return null;
    }

    public SettingsEditor<JDOMExternalizable> getRunnerSettingsEditor(JavaProgramRunner runner) {
        return null;
    }

    public RunProfileState getState(DataContext context,
                                    RunnerInfo runnerInfo,
                                    RunnerSettings runnerSettings,
                                    ConfigurationPerRunnerSettings configurationSettings) throws ExecutionException {
        Module module = DataKeys.MODULE.getData(context);
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
        };
        TextConsoleBuilder consoleBuilder = TextConsoleBuilderFactory.getInstance().createBuilder(getProject());
        state.setConsoleBuilder(consoleBuilder);
        return state;
    }

    public void checkConfiguration() throws RuntimeConfigurationException {
    }

    public Module[] getModules() {
        return new Module[0];
    }
}
