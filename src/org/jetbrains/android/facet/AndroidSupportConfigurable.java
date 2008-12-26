package org.jetbrains.android.facet;

import com.intellij.facet.FacetManager;
import com.intellij.facet.ModifiableFacetModel;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.fileTemplates.FileTemplateUtil;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.ide.util.newProjectWizard.FrameworkSupportConfigurable;
import com.intellij.javaee.ExternalResourceManagerEx;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.JavadocOrderRootType;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import org.jetbrains.android.AndroidManager;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
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
        final FacetManager facetManager = FacetManager.getInstance(module);
        ModifiableFacetModel model = facetManager.createModifiableModel();
        AndroidFacet facet = facetManager.createFacet(AndroidFacet.getFacetType(), "Android", null);
        facet.getConfiguration().setSdkPath(mySdkPathField.getText());
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
                    createResources(project, files [0]);
                }
            });
        }

        ExternalResourceManagerEx.getInstanceEx().addIgnoredResource(AndroidManager.NAMESPACE);
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
        VirtualFile androidRoot = JarFileSystem.getInstance().findFileByPath(androidJar.getPath() + JarFileSystem.JAR_SEPARATOR);
        androidLibraryModel.addRoot(androidRoot, OrderRootType.CLASSES);
        if (javadocDir != null) {
            androidLibraryModel.addRoot(javadocDir, JavadocOrderRootType.getInstance());
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

    private static void createAndroidManifest(Project project, VirtualFile rootDir) {
        rootDir.refresh(false, false);
        PsiDirectory directory = PsiManager.getInstance(project).findDirectory(rootDir);
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

    private static void createResources(final Project project, final VirtualFile rootDir) {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            public void run() {
                try {
                    VirtualFile resDir = rootDir.createChildDirectory(project, "res");
                    VirtualFile drawableDir = resDir.createChildDirectory(project, "drawable");
                    VirtualFile iconFile = drawableDir.createChildData(project, "icon.png");
                    InputStream iconStream = AndroidSupportConfigurable.class.getResourceAsStream("/icons/androidLarge.png");
                    try {
                        byte[] bytes = FileUtil.adaptiveLoadBytes(iconStream);
                        iconFile.setBinaryContent(bytes);
                    }
                    finally {
                        iconStream.close();
                    }
                } catch (IOException e) {
                    LOG.error(e);
                }
            }
        });
    }

    private void createUIComponents() {
        myPanel = new JPanel() {
            public Dimension getMinimumSize() {
                Dimension minimumSize = super.getMinimumSize();
                return new Dimension(400, minimumSize.height);
            }

            public Dimension getPreferredSize() {
                Dimension preferredSize = super.getPreferredSize();
                return new Dimension(400, preferredSize.height);
            }
        };
    }
}
