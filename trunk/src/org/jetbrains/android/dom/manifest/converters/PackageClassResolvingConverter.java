package org.jetbrains.android.dom.manifest.converters;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.util.Processor;
import com.intellij.util.Query;
import com.intellij.util.xml.*;
import org.jetbrains.android.dom.manifest.Manifest;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author yole
 */
public class PackageClassResolvingConverter extends ResolvingConverter<PsiClass> {
    @NotNull
    public Collection<? extends PsiClass> getVariants(ConvertContext context) {
        final List<PsiClass> result = new ArrayList<PsiClass>();
        DomElement domElement = context.getInvocationElement();
        Manifest manifest = domElement.getParentOfType(Manifest.class, true);
        final String packageName = manifest == null ? null : manifest.getPackage().getValue();
        ExtendClass extendClass = domElement.getAnnotation(ExtendClass.class);
        if (extendClass != null && packageName != null) {
            PsiClass baseClass = context.findClass(extendClass.value(),
                    GlobalSearchScope.allScope(context.getPsiManager().getProject()));
            if (baseClass != null) {
                Query<PsiClass> query = ClassInheritorsSearch.search(baseClass,
                        GlobalSearchScope.moduleWithDependenciesScope(context.getModule()),
                        true);
                query.forEach(new Processor<PsiClass>() {
                    public boolean process(PsiClass psiClass) {
                        PsiDirectory directory = psiClass.getContainingFile().getContainingDirectory();
                        if (directory.getPackage().getQualifiedName().equals(packageName)) {
                            result.add(psiClass);
                        }
                        return true;
                    }
                });
            }
        }
        return result;
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
