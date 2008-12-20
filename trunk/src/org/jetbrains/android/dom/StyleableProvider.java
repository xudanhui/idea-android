package org.jetbrains.android.dom;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import org.jetbrains.android.dom.attrs.AttributeDefinitions;
import org.jetbrains.android.dom.attrs.StyleableDefinition;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author coyote
 */
public abstract class StyleableProvider {
    private static final Logger LOG = Logger.getInstance("#org.jetbrains.android.dom.StyleableProvider");

    protected final AndroidFacet facet;
    private AttributeDefinitions definitions = null;
    protected boolean forAllFiles = false;

    public StyleableProvider(@NotNull AndroidFacet facet) {
        this.facet = facet;
    }

    void setForAllFiles(boolean forAllFiles) {
        this.forAllFiles = forAllFiles;
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

    @Nullable
    public StyleableDefinition getStyleableByTagName(String tagName) {
        String styleableName = getStyleableNameByTagName(tagName);
        if (styleableName == null) return null;
        AttributeDefinitions definitions = getAttributeDefinitions();
        return definitions.getStyleableByName(styleableName);
    }

    @Nullable
    public String getTagName(StyleableDefinition styleable) {
        return getTagNameByStyleableName(styleable.getName());
    }

    @NotNull
    private AttributeDefinitions parseAttributeDefinitions(final String fileName) {
        PsiFile file = ApplicationManager.getApplication().runReadAction(new Computable<PsiFile>() {
            public PsiFile compute() {
                final VirtualFile sdkValuesDir = facet.getResourceTypeDir("values", "android");
                if (sdkValuesDir == null) return null;
                final VirtualFile vFile = sdkValuesDir.findChild(fileName);
                if (vFile == null) return null;
                Project project = facet.getModule().getProject();
                return PsiManager.getInstance(project).findFile(vFile);
            }
        });
        String valuePath = "<sdk>/tools/lib/res/default/value";
        if (file == null) {
            LOG.error("File " + file + " is not found in " + valuePath + " directory");
        }
        if (!(file instanceof XmlFile)) {
            LOG.error("File " + file + " in " + valuePath + " is not an xml file");
        }
        return new AttributeDefinitions((XmlFile) file);
    }
}
