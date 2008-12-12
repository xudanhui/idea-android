package org.jetbrains.android.dom.layout;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.xml.DomFileDescription;
import org.jetbrains.android.AndroidManager;
import org.jetbrains.android.dom.resources.ResourcesDomFileDescription;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
        return ResourcesDomFileDescription.isInResourceDirectory(file, "layout");
    }

    public static boolean isLayoutFile(@NotNull final XmlFile file, @Nullable final Module module) {
        final LayoutDomFileDescription description = new LayoutDomFileDescription();
        return ApplicationManager.getApplication().runReadAction(new Computable<Boolean>() {
            public Boolean compute() {
                return description.isMyFile(file, module);
            }
        });
    }
}
