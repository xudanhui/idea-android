package org.jetbrains.android.dom.resources;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.xml.DomFileDescription;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author yole
 */
public class ResourcesDomFileDescription extends DomFileDescription<Resources> {
    public ResourcesDomFileDescription() {
        super(Resources.class, "resources");
    }

    public boolean isMyFile(@NotNull XmlFile file, @Nullable Module module) {
        PsiDirectory psiDirectory = file.getContainingDirectory();
        if (psiDirectory == null || !"values".equals(psiDirectory.getName())) {
            return false;
        }
        psiDirectory = psiDirectory.getParent();
        if (psiDirectory == null || !"res".equals(psiDirectory.getName())) {
            return false;
        }
        return true;
    }
}
