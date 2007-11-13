package org.jetbrains.android.run;

import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author yole
 */
public class AndroidRunConfigurationEditor extends SettingsEditor<AndroidRunConfiguration> {
    private JPanel myPanel;

    protected void resetEditorFrom(AndroidRunConfiguration s) {
    }

    protected void applyEditorTo(AndroidRunConfiguration s) throws ConfigurationException {
    }

    @NotNull
    protected JComponent createEditor() {
        return myPanel;
    }

    protected void disposeEditor() {
    }
}
