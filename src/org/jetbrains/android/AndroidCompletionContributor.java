package org.jetbrains.android;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.LookupItem;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.*;
import org.jetbrains.android.dom.StyleableProvider;
import org.jetbrains.android.dom.attrs.AttributeDefinition;
import org.jetbrains.android.dom.attrs.AttributeDefinitions;
import org.jetbrains.android.dom.attrs.StyleableDefinition;
import org.jetbrains.android.dom.layout.LayoutStyleableProvider;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author coyote
 */
public class AndroidCompletionContributor extends CompletionContributor {
    private static PsiElement getParent(final PsiElement element) {
        return ApplicationManager.getApplication().runReadAction(new Computable<PsiElement>() {
            public PsiElement compute() {
                return element.getParent();
            }
        });
    }
    @Override
    public boolean fillCompletionVariants(CompletionParameters parameters, CompletionResultSet result) {
        PsiElement position = parameters.getPosition();
        PsiFile file = parameters.getOriginalFile();
        if (!(file instanceof XmlFile)) return true;
        XmlFile xmlFile = (XmlFile) file;
        StyleableProvider provider = getProvider(xmlFile);
        if (provider == null) return true;
        if (parameters.getCompletionType() == CompletionType.BASIC && position instanceof XmlToken) {
            XmlToken token = (XmlToken) position;
            PsiElement parent = getParent(token);
            if (token.getTokenType().toString().equals("XML_NAME")) {
                if (parent instanceof XmlAttribute) {
                    XmlAttribute attribute = (XmlAttribute) parent;
                    PsiElement possibleTag = getParent(attribute);
                    if (possibleTag instanceof XmlTag) {
                        completeWithAttributes(provider, (XmlTag) possibleTag, result);
                        return false;
                    }
                }
                else if (parent instanceof XmlTag) {
                    PsiElement grandParent = getParent(parent);
                    XmlTag t = grandParent instanceof XmlTag ? (XmlTag) grandParent : null;
                    completeWithTagNames(provider, t, result);
                    return false;
                }
            }
            else if (token.getTokenType().toString().equals("XML_ATTRIBUTE_VALUE_TOKEN")) {
                if (parent instanceof XmlAttributeValue) {
                    
                }
            }
        }
        return true;
    }

    @Nullable
    private StyleableProvider getProvider(final XmlFile file) {
        Module module = ApplicationManager.getApplication().runReadAction(new Computable<Module>() {
            public Module compute() {
                return ModuleUtil.findModuleForPsiElement(file);
            }
        });
        if (module == null) return null;
        final AndroidFacet facet = AndroidFacet.getInstance(module);
        if (facet == null) return null;
        return facet.getStyleableProviderForFile(file, module);
    }

    private void completeWithTagNames(@NotNull StyleableProvider provider, @Nullable XmlTag parent, CompletionResultSet result) {
        List<StyleableDefinition> styleablesToComplete = new ArrayList<StyleableDefinition>();
        if (parent == null) {
            AttributeDefinitions attrDefs = provider.getAttributeDefinitions();
            if (attrDefs == null) return;
            for (String name : attrDefs.getStyleableNames()) {
                StyleableDefinition definition = attrDefs.getStyleableByName(name);
                if (definition.getParents().isEmpty() || provider instanceof LayoutStyleableProvider) {
                    styleablesToComplete.add(definition);
                }
            }
        }
        else {
            StyleableDefinition definition = provider.getStyleableByTagName(parent.getName());
            while (definition != null) {
                for (StyleableDefinition child : definition.getChildren()) {
                    styleablesToComplete.add(child);
                }
                definition = definition.getSuperclass();
            }
        }
        for (StyleableDefinition definition : styleablesToComplete) {
            String s = provider.getTagName(definition);
            if (s != null) result.addElement(new LookupItem<String>(s, s));
        }
    }

    private void completeWithAttributes(@NotNull StyleableProvider provider, @NotNull XmlTag tag, CompletionResultSet result) {
        StyleableDefinition styleable = provider.getStyleableByTagName(tag.getName());
        if (styleable != null) {
            XmlTag parentTag = tag.getParentTag();
            for (AttributeDefinition attribute : provider.getAttributes(styleable, parentTag)) {
                String name = AndroidManager.NAMESPACE_KEY + ':' + attribute.getName();
                result.addElement(new LookupItem<String>(name, name));
            }
        }
    }
}
