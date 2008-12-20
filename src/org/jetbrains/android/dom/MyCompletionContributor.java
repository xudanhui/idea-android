package org.jetbrains.android.dom;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.LookupItem;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlToken;
import org.jetbrains.android.AndroidManager;
import org.jetbrains.android.dom.attrs.AttributeDefinition;
import org.jetbrains.android.dom.attrs.AttributeDefinitions;
import org.jetbrains.android.dom.attrs.StyleableDefinition;
import org.jetbrains.android.dom.layout.LayoutStyleableProvider;
import org.jetbrains.android.facet.AndroidFacet;

import java.util.ArrayList;
import java.util.List;

/**
 * @author coyote
 */
public class MyCompletionContributor extends CompletionContributor {
    @Override
    public boolean fillCompletionVariants(CompletionParameters parameters, CompletionResultSet result) {
        PsiElement position = parameters.getPosition();
        PsiFile file = parameters.getOriginalFile();
        if (!(file instanceof XmlFile)) return true;
        XmlFile xmlFile = (XmlFile) file;
        if (parameters.getCompletionType() == CompletionType.BASIC && position instanceof XmlToken) {
            XmlToken token = (XmlToken) position;
            PsiElement parent = token.getParent();
            if (token.getTokenType().toString().equals("XML_NAME")) {
                if (parent instanceof XmlAttribute) {
                    XmlAttribute attribute = (XmlAttribute) parent;
                    PsiElement possibleTag = attribute.getParent();
                    if (possibleTag instanceof XmlTag) {
                        completeWithAttributes((XmlTag) possibleTag, xmlFile, result);
                        return false;
                    }
                }
                else if (parent instanceof XmlTag) {
                    PsiElement grandParent = parent.getParent();
                    XmlTag t = grandParent instanceof XmlTag ? (XmlTag) grandParent : null;
                    completeWithTagNames(t, xmlFile, result);
                    return false;
                }
            }
        }
        return true;
    }

    private StyleableProvider getProvider(XmlFile file) {
        Module module = ModuleUtil.findModuleForPsiElement(file);
        final AndroidFacet facet = AndroidFacet.getInstance(module);
        if (facet == null) return null;
        return facet.getStyleableProviderForFile(file, module);
    }

    private void completeWithTagNames(XmlTag tag, XmlFile file, CompletionResultSet result) {
        StyleableProvider provider = getProvider(file);
        List<StyleableDefinition> styleablesToComplete = new ArrayList<StyleableDefinition>();
        if (tag == null) {
            AttributeDefinitions attrDefs = provider.getAttributeDefinitions();
            for (String name : attrDefs.getStyleableNames()) {
                StyleableDefinition definition = attrDefs.getStyleableByName(name);
                if (definition.getParents().isEmpty() || provider instanceof LayoutStyleableProvider) {
                    styleablesToComplete.add(definition);
                }
            }
        }
        else {
            StyleableDefinition definition = provider.getStyleableByTagName(tag.getName());
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

    private void completeWithAttributes(XmlTag tag, final XmlFile file, CompletionResultSet result) {
        StyleableProvider provider = getProvider(file);
        StyleableDefinition definition = provider.getStyleableByTagName(tag.getName());
        if (definition != null) {
            List<AttributeDefinition> attributes = definition.getAttributes();
            for (AttributeDefinition attribute : attributes) {
                String name = AndroidManager.NAMESPACE_KEY + ':' + attribute.getName();
                result.addElement(new LookupItem<String>(name, name));
            }
        }
    }
}
