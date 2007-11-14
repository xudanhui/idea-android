package org.jetbrains.android.dom.manifest.converters;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.ResolvingConverter;
import com.intellij.util.xml.GenericDomValue;
import org.jetbrains.android.dom.manifest.Manifest;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

/**
 * @author yole
 */
public class PackageClassResolvingConverter extends ResolvingConverter<PsiClass> {
    @NotNull
    public Collection<? extends PsiClass> getVariants(ConvertContext context) {
        return Collections.emptyList();
    }

    public PsiClass fromString(@Nullable @NonNls String s, ConvertContext context) {
        if (s == null) return null;
        DomElement domElement = context.getInvocationElement();
        Manifest manifest = domElement.getParentOfType(Manifest.class, true);
        if (manifest != null) {
            String packageName = manifest.getPackage().getValue();
            return context.getPsiManager().findClass(packageName + "." + s,
                    GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(context.getModule()));
        }
        return null;
    }

    public String toString(@Nullable PsiClass psiClass, ConvertContext context) {
        return psiClass == null ? null : psiClass.getName();
    }

    public void bindReference(GenericDomValue<PsiClass> genericValue, ConvertContext context, PsiElement newTarget) {
        genericValue.setStringValue(((PsiClass) newTarget).getName());
    }
}
