package org.jetbrains.android.dom.converters;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.CustomReferenceConverter;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.ResolvingConverter;
import org.jetbrains.android.dom.resources.ResourceString;
import org.jetbrains.android.dom.resources.ResourceValue;
import org.jetbrains.android.dom.resources.Resources;
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
public class ResourceReferenceConverter extends ResolvingConverter<ResourceValue> implements CustomReferenceConverter<ResourceValue> {
    @NotNull
    public Collection<? extends ResourceValue> getVariants(ConvertContext context) {
        List<ResourceValue> result = new ArrayList<ResourceValue>();
        return result;
    }

    public ResourceValue fromString(@Nullable @NonNls String s, ConvertContext context) {
        if (s == null) {
            return null;
        }
        if (s.startsWith("@")) {
            return ResourceValue.reference(s);
        }
        return ResourceValue.literal(s);
    }

    public String toString(@Nullable ResourceValue resourceElement, ConvertContext context) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @NotNull
    public PsiReference[] createReferences(GenericDomValue<ResourceValue> value, PsiElement element, ConvertContext context) {
        ResourceValue ref = value.getValue();
        if (ref != null && ref.isReference()) {
            String resType = ref.getResourceType();
            AndroidFacet facet = AndroidFacet.getInstance(context.getModule());
            List<Resources> resources = facet.getValueResources();
            GenericDomValue target = null;
            for(Resources resourcesFile: resources) {
                if (resType.equals("string")) {
                    List<ResourceString> list = resourcesFile.getStrings();
                    for(ResourceString rs: list) {
                        if (ref.getResourceName().equals(rs.getName().getValue())) {
                            target = rs.getName();
                        }
                    }
                }
            }
            return new PsiReference[] { new ResourceReference(value, target)};
        }
        return new PsiReference[0];
    }
}
