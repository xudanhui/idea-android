package org.jetbrains.android.facet;

import com.intellij.facet.FacetManager;
import com.intellij.facet.ModifiableFacetModel;
import com.intellij.facet.impl.FacetUtil;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.fileTemplates.FileTemplateUtil;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.ide.util.newProjectWizard.FrameworkSupportConfigurable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;

/**
 * @author yole
 */
public class AndroidSupportConfigurable extends FrameworkSupportConfigurable {
    private static final Logger LOG = Logger.getInstance("#org.jetbrains.android.facet.AndroidSupportConfigurable");

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

        VirtualFile sdkDir = LocalFileSystem.getInstance().findFileByPath(mySdkPathField.getText());
        if (sdkDir != null) {
            VirtualFile androidJar = sdkDir.findChild("android.jar");
            if (androidJar != null) {
                addAndroidJar(modifiableRootModel, androidJar, findJavadocDir(sdkDir));
            }
        }

        final VirtualFile[] files = modifiableRootModel.getContentRoots();
        if (files.length > 0) {
            final Project project = module.getProject();
            StartupManager.getInstance(project).registerPostStartupActivity(new Runnable() {
                public void run() {
                    createAndroidManifest(project, files[0]);
                }
            });
        }
    }

    private static VirtualFile findJavadocDir(VirtualFile sdkDir) {
        VirtualFile docsDir = sdkDir.findChild("docs");
        if (docsDir != null) {
            VirtualFile referenceDir = docsDir.findChild("reference");
            if (referenceDir != null) {
                return referenceDir;
            }
        }
        return null;
    }

    private static void addAndroidJar(ModifiableRootModel modifiableRootModel, VirtualFile androidJar, @Nullable VirtualFile javadocDir) {
        LibraryTable libraryTable = modifiableRootModel.getModuleLibraryTable();
        LibraryTable.ModifiableModel libraryModel = libraryTable.getModifiableModel();
        Library androidLibrary = libraryModel.createLibrary("Android SDK");
        libraryModel.commit();

        Library.ModifiableModel androidLibraryModel = androidLibrary.getModifiableModel();
        androidLibraryModel.addRoot(androidJar, OrderRootType.SOURCES);
        if (javadocDir != null) {
            androidLibraryModel.addRoot(javadocDir, OrderRootType.JAVADOC);
        }
        androidLibraryModel.commit();

        moveAndroidJarToTop(modifiableRootModel);
    }

    private static void moveAndroidJarToTop(ModifiableRootModel modifiableRootModel) {
        OrderEntry[] entries = modifiableRootModel.getOrderEntries();
        OrderEntry[] newEntries = new OrderEntry[entries.length];
        newEntries [0] = entries [entries.length-1];
        System.arraycopy(entries, 0, newEntries, 1, entries.length-1);
        modifiableRootModel.rearrangeOrderEntries(newEntries);
    }

    private static void createAndroidManifest(Project project, VirtualFile file) {
        PsiDirectory directory = PsiManager.getInstance(project).findDirectory(file);
        if (directory != null) {
            FileTemplate template = FileTemplateManager.getInstance().getJ2eeTemplate("AndroidManifest.xml");
            Properties properties = FileTemplateManager.getInstance().getDefaultProperties();
            try {
                FileTemplateUtil.createFromTemplate(template, "AndroidManifest.xml", properties, directory);
            } catch (Exception e) {
                LOG.error(e);
            }
        }
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
