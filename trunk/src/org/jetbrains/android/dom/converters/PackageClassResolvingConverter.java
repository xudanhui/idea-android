package org.jetbrains.android.dom.converters;

import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
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
    private final String extendClassName;

    public PackageClassResolvingConverter(String extendClassName) {
        this.extendClassName = extendClassName;
    }

    public PackageClassResolvingConverter() {
        extendClassName = null;
    }

    @NotNull
    public Collection<? extends PsiClass> getVariants(ConvertContext context) {
        final List<PsiClass> result = new ArrayList<PsiClass>();
        DomElement domElement = context.getInvocationElement();
        Manifest manifest = domElement.getParentOfType(Manifest.class, true);
        final String packageName = manifest == null ? null : manifest.getPackage().getValue();
        ExtendClass extendClass = domElement.getAnnotation(ExtendClass.class);
        String extendClassName = extendClass != null ? extendClass.value() : this.extendClassName;
        if (extendClassName != null && packageName != null) {
            Project project = context.getPsiManager().getProject();
            PsiClass baseClass = JavaPsiFacade.getInstance(project).findClass(extendClassName,
                    GlobalSearchScope.allScope(project));
            if (baseClass != null) {
                Query<PsiClass> query = ClassInheritorsSearch.search(baseClass,
                        GlobalSearchScope.moduleWithDependenciesScope(context.getModule()),
                        true);
                result.addAll(query.findAll());
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
            String className;
            if (s.startsWith(".")) {
                className = packageName + s;
            }
            else {
                className = packageName + "." + s;
            }
            return JavaPsiFacade.getInstance(context.getPsiManager().getProject()).findClass(className,
                    GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(context.getModule()));
        }
        return null;
    }

    public String toString(@Nullable PsiClass psiClass, ConvertContext context) {
        if (psiClass == null) return null;
        String qName = psiClass.getQualifiedName();
        if (qName == null) return null;
        DomElement domElement = context.getInvocationElement();
        Manifest manifest = domElement.getParentOfType(Manifest.class, true);
        final String packageName = manifest == null ? null : manifest.getPackage().getValue();
        PsiJavaFile psiFile = (PsiJavaFile) psiClass.getContainingFile();
        if (Comparing.equal(psiFile.getPackageName(), packageName)) {
            return psiClass.getName();
        }
        else if (packageName != null && qName.startsWith(packageName)) {
            return qName.substring(packageName.length());
        }
        return qName;
    }

    public void bindReference(GenericDomValue<PsiClass> genericValue, ConvertContext context, PsiElement newTarget) {
        genericValue.setStringValue(((PsiClass) newTarget).getName());
    }
}
