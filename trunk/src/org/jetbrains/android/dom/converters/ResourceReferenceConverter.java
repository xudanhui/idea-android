package org.jetbrains.android.dom.converters;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.CustomReferenceConverter;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.ResolvingConverter;
import com.intellij.util.xml.impl.GenericDomValueReference;
import org.jetbrains.android.dom.resources.ResourceReference;
import org.jetbrains.android.dom.resources.Resources;
import org.jetbrains.android.dom.resources.ResourceString;
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
public class ResourceReferenceConverter extends ResolvingConverter<ResourceReference> implements CustomReferenceConverter<ResourceReference> {
    @NotNull
    public Collection<? extends ResourceReference> getVariants(ConvertContext context) {
        List<ResourceReference> result = new ArrayList<ResourceReference>();
        return result;
    }

    public ResourceReference fromString(@Nullable @NonNls String s, ConvertContext context) {
        if (s == null) {
            return null;
        }
        if (s.startsWith("@")) {
            return ResourceReference.reference(s);
        }
        return ResourceReference.literal(s);
    }

    public String toString(@Nullable ResourceReference resourceElement, ConvertContext context) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @NotNull
    public PsiReference[] createReferences(GenericDomValue<ResourceReference> value, PsiElement element, ConvertContext context) {
        ResourceReference ref = value.getValue();
        if (ref != null && ref.isReference()) {
            String resType = ref.getResourceType();
            AndroidFacet facet = AndroidFacet.getInstance(context.getModule());
            List<Resources> resources = facet.getValueResources();
            for(Resources resourcesFile: resources) {
                if (resType.equals("string")) {
                    List<ResourceString> list = resourcesFile.getStrings();
                    for(ResourceString rs: list) {
                        if (ref.getResourceName().equals(rs.getName().getValue())) {
                            return new PsiReference[] { new GenericDomValueReference(rs.getName())};                        
                        }
                    }
                }
            }
        }
        return new PsiReference[0];
    }
}
