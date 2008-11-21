package org.jetbrains.android.facet;

import com.intellij.facet.ui.FacetEditorTab;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.ide.util.PropertiesComponent;
import org.jetbrains.annotations.Nls;

import javax.swing.*;

/**
 * @author yole
 */
public class AndroidFacetEditorTab extends FacetEditorTab {
    private JPanel myPanel;
    private TextFieldWithBrowseButton mySdkPathField;
    private AndroidFacetConfiguration myConfiguration;

    public AndroidFacetEditorTab(Project project, AndroidFacetConfiguration androidFacetConfiguration) {
        myConfiguration = androidFacetConfiguration;
        mySdkPathField.addBrowseFolderListener("Select Android SDK Path", "",
                project, new FileChooserDescriptor(false, true, false, false, false, false));
    }

    @Nls
    public String getDisplayName() {
        return "Android SDK Settings";
    }

    public JComponent createComponent() {
        return myPanel;
    }

    public boolean isModified() {
        return !Comparing.strEqual(mySdkPathField.getText(), myConfiguration.getSdkPath(), true);
    }

    public void apply() throws ConfigurationException {
        myConfiguration.setSdkPath(mySdkPathField.getText());
        PropertiesComponent.getInstance().setValue(AndroidFacetConfiguration.DEFAULT_SDK_PATH_PROPERTY,
                myConfiguration.getSdkPath());
    }

    public void reset() {
        mySdkPathField.setText(myConfiguration.getSdkPath());
    }

    public void disposeUIResources() {
    }
}
