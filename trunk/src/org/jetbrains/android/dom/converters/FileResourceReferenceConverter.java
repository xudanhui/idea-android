package org.jetbrains.android.dom.converters;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.CustomReferenceConverter;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.ResolvingConverter;
import org.jetbrains.android.dom.ResourceType;
import org.jetbrains.android.dom.resources.ResourceValue;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author yole
 */
public class FileResourceReferenceConverter extends ResolvingConverter<ResourceValue> implements CustomReferenceConverter<ResourceValue> {
    @NotNull
    public Collection<? extends ResourceValue> getVariants(ConvertContext context) {
        List<ResourceValue> result = new ArrayList<ResourceValue>();
        AndroidFacet facet = AndroidFacet.getInstance(context.getModule());
        ResourceType resourceType = context.getInvocationElement().getAnnotation(ResourceType.class);
        if (facet != null && resourceType != null) {
            List<String> files = facet.getResourceFileNames(resourceType.value());
            for(String file: files) {
                result.add(ResourceValue.referenceTo('@', resourceType.value(), file));
            }
        }
        return result;
    }

    public ResourceValue fromString(@Nullable @NonNls String s, ConvertContext context) {
        return ResourceValue.parse(s);
    }

    public String toString(@Nullable ResourceValue resourceValue, ConvertContext context) {
        return resourceValue == null ? null : resourceValue.toString();
    }

    @NotNull
    public PsiReference[] createReferences(GenericDomValue<ResourceValue> value, PsiElement element, ConvertContext context) {
        ResourceValue ref = value.getValue();
        if (ref != null && ref.isReference()) {
            String resType = ref.getResourceType();
            AndroidFacet facet = AndroidFacet.getInstance(context.getModule());
            if (facet != null) {
                PsiFile file = facet.findResourceFile(resType, ref.getResourceName());
                return new PsiReference[] { new FileResourceReference(value, file) };
            }
        }
        return new PsiReference[0];
    }
}
