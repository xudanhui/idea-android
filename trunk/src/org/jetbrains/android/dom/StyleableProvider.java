package org.jetbrains.android.dom;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.project.Project;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.android.dom.attrs.AttributeDefinitions;
import org.jetbrains.android.dom.attrs.StyleableDefinition;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author coyote
 */
public abstract class StyleableProvider {
    protected final AndroidFacet facet;
    private AttributeDefinitions definitions = null;

    public StyleableProvider(@NotNull AndroidFacet facet) {
        this.facet = facet;
    }

    protected abstract String getStyleableNameByTagName(@NotNull String tagName);

    protected abstract String getTagNameByStyleableName(@NotNull String styleableName); 
    
    @NotNull
    public abstract String getAttrsFilename();
    
    public abstract boolean isMyFile(@NotNull XmlFile file, @Nullable Module module);

    @NotNull
    public AttributeDefinitions getAttributeDefinitions() {
        if (definitions == null) {
            definitions = parseAttributeDefinitions(getAttrsFilename());
        }
        return definitions;
    }

    public StyleableDefinition getStyleableByTagName(String tagName) {
        String styleableName = getStyleableNameByTagName(tagName);
        if (styleableName == null) return null;
        AttributeDefinitions definitions = getAttributeDefinitions();
        return definitions.getStyleableByName(styleableName);
    }

    public String getTagName(StyleableDefinition styleable) {
        return getTagNameByStyleableName(styleable.getName());
    }

    private AttributeDefinitions parseAttributeDefinitions(String fileName) {
        final VirtualFile sdkValuesDir = facet.getResourceTypeDir("values", "android");
        if (sdkValuesDir == null) return null;
        final VirtualFile vFile = sdkValuesDir.findChild(fileName);
        if (vFile == null) return null;
        Project project = facet.getModule().getProject();
        final PsiFile file = PsiManager.getInstance(project).findFile(vFile);
        if (!(file instanceof XmlFile)) return null;
        return new AttributeDefinitions((XmlFile) file);
    }
}
