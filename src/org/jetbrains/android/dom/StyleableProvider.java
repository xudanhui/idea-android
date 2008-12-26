package org.jetbrains.android.dom;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.android.dom.attrs.AttributeDefinition;
import org.jetbrains.android.dom.attrs.AttributeDefinitions;
import org.jetbrains.android.dom.attrs.StyleableDefinition;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.ArrayList;

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

    @Nullable
    public synchronized AttributeDefinitions getAttributeDefinitions() {
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
        if (definitions == null) return null;
        return definitions.getStyleableByName(styleableName);
    }

    @Nullable
    public String getTagName(StyleableDefinition styleable) {
        return getTagNameByStyleableName(styleable.getName());
    }

    @Nullable
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
            LOG.info("File " + fileName + " is not found in " + valuePath + " directory");
            return null;
        }
        if (!(file instanceof XmlFile)) {
            LOG.info("File " + fileName + " in " + valuePath + " is not an xml file");
            return null;
        }
        return new AttributeDefinitions((XmlFile) file);
    }

    public AttributeDefinition findAttribute(String name, StyleableDefinition styleable, XmlTag parentTag) {
        AttributeDefinition def = styleable.findAttribute(name);
        if (def != null) return def;
        StyleableDefinition parentTagStyleable = getParentTagStyleable(parentTag);
        return parentTagStyleable != null ? parentTagStyleable.findLayoutAttribute(name) : null;
    }

    private StyleableDefinition getParentTagStyleable(XmlTag parentTag) {
        String parentTagName = parentTag != null ? parentTag.getName() : "ViewGroup";
        return getStyleableByTagName(parentTagName);
    }

    public List<AttributeDefinition> getAttributes(StyleableDefinition styleable, XmlTag parentTag) {
        List<AttributeDefinition> attrs = new ArrayList<AttributeDefinition>();
        attrs.addAll(styleable.getAttributes());
        StyleableDefinition parentTagStyleable = getParentTagStyleable(parentTag);
        attrs.addAll(parentTagStyleable.getLayoutAttributes());
        return attrs;
    }
}
