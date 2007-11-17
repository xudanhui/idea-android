package org.jetbrains.android;

import com.intellij.codeInspection.InspectionToolProvider;
import com.intellij.facet.FacetTypeRegistry;
import com.intellij.ide.fileTemplates.FileTemplateDescriptor;
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptor;
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptorFactory;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.util.IconLoader;
import org.jetbrains.android.dom.manifest.ManifestDomInspection;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.android.fileTypes.AndroidIdlFileType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author yole
 */
public class AndroidManager implements ApplicationComponent, InspectionToolProvider, FileTemplateGroupDescriptorFactory {
    public static final String MANIFEST_FILE_NAME = "AndroidManifest.xml";
    public static final String CLASSES_FILE_NAME = "classes.dex";

    public static final Icon ANDROID_ICON = IconLoader.getIcon("/icons/android.png");

    @NonNls
    @NotNull
    public String getComponentName() {
        return "AndroidManager";
    }

    public void initComponent() {
        FacetTypeRegistry.getInstance().registerFacetType(AndroidFacet.ourFacetType);
        FileTypeManager.getInstance().registerFileType(AndroidIdlFileType.ourFileType, AndroidIdlFileType.DEFAULT_ASSOCIATED_EXTENSIONS);
    }

    public void disposeComponent() {
    }

    public Class[] getInspectionClasses() {
        return new Class[]{
                ManifestDomInspection.class
        };
    }

    public FileTemplateGroupDescriptor getFileTemplatesDescriptor() {
        final FileTemplateGroupDescriptor group = new FileTemplateGroupDescriptor("Android", ANDROID_ICON);
        group.addTemplate(new FileTemplateDescriptor("AndroidManifest.xml", StdFileTypes.XML.getIcon()));
        return group;
    }
}
