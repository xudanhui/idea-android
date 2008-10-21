package org.jetbrains.android.dom.resources;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.xml.DomFileDescription;
import org.jetbrains.android.facet.AndroidFacet;
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
        if (psiDirectory != null && module != null) {
            AndroidFacet facet = AndroidFacet.getInstance(module);
            if (facet == null) return false;
            VirtualFile directory = psiDirectory.getVirtualFile();
            return directory.getName().equals("values") &&
                    Comparing.equal(directory.getParent(), facet.getResourcesDir());
        }
        return false;
    }
}
