package org.jetbrains.android.run;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.execution.junit2.configuration.ConfigurationModuleSelector;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author yole
 */
public class AndroidRunConfigurationEditor extends SettingsEditor<AndroidRunConfiguration> {
    private JPanel myPanel;
    private JComboBox myModulesComboBox;
    private ConfigurationModuleSelector myModuleSelector;

    public AndroidRunConfigurationEditor(Project project) {
        myModuleSelector = new ConfigurationModuleSelector(project, myModulesComboBox);
    }

    protected void resetEditorFrom(AndroidRunConfiguration configuration) {
        myModuleSelector.reset(configuration);
    }

    protected void applyEditorTo(AndroidRunConfiguration configuration) throws ConfigurationException {
        myModuleSelector.applyTo(configuration);
    }

    @NotNull
    protected JComponent createEditor() {
        return myPanel;
    }

    protected void disposeEditor() {
    }
}
