package org.jetbrains.android.run;

import com.intellij.execution.junit2.configuration.ConfigurationModuleSelector;
import com.intellij.ide.util.TreeClassChooser;
import com.intellij.ide.util.TreeClassChooserFactory;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author yole
 */
public class AndroidRunConfigurationEditor extends SettingsEditor<AndroidRunConfiguration> {
    private JPanel myPanel;
    private JComboBox myModulesComboBox;
    private TextFieldWithBrowseButton myActivityField;
    private ConfigurationModuleSelector myModuleSelector;

    public AndroidRunConfigurationEditor(final Project project) {
        myModuleSelector = new ConfigurationModuleSelector(project, myModulesComboBox);
        myActivityField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                PsiClass activityClass = PsiManager.getInstance(project).findClass("android.app.Activity",
                        project.getAllScope());
                PsiClass initialSelection = PsiManager.getInstance(project).findClass(myActivityField.getText(),
                        myModuleSelector.getModule().getModuleWithDependenciesScope());
                TreeClassChooser chooser = TreeClassChooserFactory.getInstance(project).createInheritanceClassChooser(
                        "Select activity class", myModuleSelector.getModule().getModuleWithDependenciesScope(),
                        activityClass, initialSelection);
                chooser.showDialog();
                PsiClass selClass = chooser.getSelectedClass();
                if (selClass != null) {
                    myActivityField.setText(selClass.getQualifiedName());
                }
            }
        });
    }

    protected void resetEditorFrom(AndroidRunConfiguration configuration) {
        myModuleSelector.reset(configuration);
        myActivityField.setText(configuration.ACTIVITY_CLASS);
    }

    protected void applyEditorTo(AndroidRunConfiguration configuration) throws ConfigurationException {
        myModuleSelector.applyTo(configuration);
        configuration.ACTIVITY_CLASS = myActivityField.getText();
    }

    @NotNull
    protected JComponent createEditor() {
        return myPanel;
    }

    protected void disposeEditor() {
    }
}
