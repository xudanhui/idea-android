package org.jetbrains.android.dom.layout;

import com.intellij.util.xml.DomFileDescription;
import com.intellij.psi.xml.XmlFile;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.android.AndroidManager;

/**
 * @author yole
 */
public class LayoutDomFileDescription extends DomFileDescription<LayoutElement> {
    public LayoutDomFileDescription() {
        super(LayoutElement.class, "LinearLayout");
    }

    public boolean acceptsOtherRootTagNames() {
        return true;
    }

    protected void initializeFileDescription() {
        registerNamespacePolicy(AndroidManager.NAMESPACE_KEY, AndroidManager.NAMESPACE);
    }

    public boolean isMyFile(@NotNull XmlFile file, @Nullable Module module) {
        if (module == null) return false;
        VirtualFile virtualFile = file.getVirtualFile();
        if (virtualFile == null) return false;
        VirtualFile parent = virtualFile.getParent();
        if (parent == null || !parent.getName().equals("layout")) {
            return false;
        }
        AndroidFacet facet = AndroidFacet.getInstance(module);
        if (facet == null) {
            return false;
        }
        VirtualFile p = parent.getParent();
        VirtualFile resourcesDir = facet.getResourcesDir();
        return p == resourcesDir;
    }
}
