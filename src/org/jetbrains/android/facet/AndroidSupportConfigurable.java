package org.jetbrains.android.facet;

import com.intellij.facet.FacetManager;
import com.intellij.facet.ModifiableFacetModel;
import com.intellij.facet.impl.FacetUtil;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.ide.util.newProjectWizard.FrameworkSupportConfigurable;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author yole
 */
public class AndroidSupportConfigurable extends FrameworkSupportConfigurable {
    private TextFieldWithBrowseButton mySdkPathField;
    private JPanel myPanel;
    private boolean myInitialized = false;

    @Nullable
    public JComponent getComponent() {
        if (!myInitialized) {
            myInitialized = true;
            mySdkPathField.addBrowseFolderListener("Select Android SDK Path", "",
                    null, new FileChooserDescriptor(false, true, false, false, false, false));
            mySdkPathField.setText(PropertiesComponent.getInstance().getValue(AndroidFacetConfiguration.DEFAULT_SDK_PATH_PROPERTY));
        }
        return myPanel;
    }

    public void addSupport(Module module, ModifiableRootModel modifiableRootModel, @Nullable Library library) {
        ModifiableFacetModel model = FacetManager.getInstance(module).createModifiableModel();
        AndroidFacet facet = FacetUtil.createFacet(AndroidFacet.ourFacetType, module, null);
        facet.getConfiguration().SDK_PATH = mySdkPathField.getText();
        model.addFacet(facet);
        model.commit();
    }

    private void createUIComponents() {
        myPanel = new JPanel() {
            public Dimension getMinimumSize() {
                Dimension minimumSize = super.getMinimumSize();
                return new Dimension(500, minimumSize.height);
            }
        };
    }
}
