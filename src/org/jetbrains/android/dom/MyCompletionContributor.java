package org.jetbrains.android.dom;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.LookupItem;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlToken;
import org.jetbrains.android.dom.attrs.AttributeDefinition;
import org.jetbrains.android.dom.attrs.StyleableDefinition;
import org.jetbrains.android.dom.layout.LayoutDomFileDescription;
import org.jetbrains.android.dom.manifest.ManifestDomFileDescription;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.android.AndroidManager;

import java.util.List;

/**
 * @author coyote
 */
public class MyCompletionContributor extends CompletionContributor {
    @Override
    public boolean fillCompletionVariants(CompletionParameters parameters, CompletionResultSet result) {
        if (!super.fillCompletionVariants(parameters, result)) return false;
        PsiElement position = parameters.getPosition();
        PsiFile file = parameters.getOriginalFile();
        if (!(file instanceof XmlFile)) return true;
        if (parameters.getCompletionType() == CompletionType.BASIC && position instanceof XmlToken) {
            XmlToken token = (XmlToken) position;
            PsiElement possibleAttribute = token.getParent();
            if (possibleAttribute instanceof XmlAttribute && token.getTokenType().toString().equals("XML_NAME")) {
                XmlAttribute attribute = (XmlAttribute) possibleAttribute;
                PsiElement possibleTag = attribute.getParent();
                if (possibleTag instanceof XmlTag) {
                    complete((XmlTag) possibleTag, (XmlFile) file, result);
                }
            }
        }
        return true;
    }

    private void completeForStyleable(StyleableDefinition definition, CompletionResultSet result) {
        List<AttributeDefinition> attributes = definition.getAttributes();
        for (AttributeDefinition attribute : attributes) {
            String name = AndroidManager.NAMESPACE_KEY + ':' + attribute.getName();
            result.addElement(new LookupItem<String>(name, name));
        }
    }

    private void complete(XmlTag tag, final XmlFile file, CompletionResultSet result) {
        Module module = ModuleUtil.findModuleForPsiElement(tag);
        final AndroidFacet facet = AndroidFacet.getInstance(module);
        if (facet == null) return;
        final String tagName = tag.getName();
        Computable<StyleableDefinition> action = null;
        if (new ManifestDomFileDescription().isMyFile(file, module)) {
            action = new Computable<StyleableDefinition>() {
                public StyleableDefinition compute() {
                    return facet.getManifestStyleableByTagName(tagName);
                }
            };
        }
        else if (LayoutDomFileDescription.isLayoutFile(file, module)) {
            action = new Computable<StyleableDefinition>() {
                public StyleableDefinition compute() {
                    return facet.getLayoutStyleableByTagName(tagName);
                }
            };
        }
        if (action != null) {
            StyleableDefinition definition = ApplicationManager.getApplication().runReadAction(action);
            completeForStyleable(definition, result);
        }
    }
}
