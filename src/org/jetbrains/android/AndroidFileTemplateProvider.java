package org.jetbrains.android;

import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptorFactory;
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptor;
import com.intellij.ide.fileTemplates.FileTemplateDescriptor;
import com.intellij.openapi.fileTypes.StdFileTypes;

/**
 * @author yole
 */
public class AndroidFileTemplateProvider implements FileTemplateGroupDescriptorFactory {
    public FileTemplateGroupDescriptor getFileTemplatesDescriptor() {
        final FileTemplateGroupDescriptor group = new FileTemplateGroupDescriptor("Android", AndroidManager.ANDROID_ICON);
        group.addTemplate(new FileTemplateDescriptor("AndroidManifest.xml", StdFileTypes.XML.getIcon()));
        return group;
    }
}
