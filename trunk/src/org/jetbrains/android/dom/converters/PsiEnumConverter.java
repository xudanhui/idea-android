package org.jetbrains.android.dom.converters;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.ResolvingConverter;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author yole
 */
public class PsiEnumConverter extends ResolvingConverter<String> {
    private String myEnumClassName;

    public PsiEnumConverter(String enumClassName) {
        myEnumClassName = enumClassName;
    }

    @NotNull
    public Collection<? extends String> getVariants(ConvertContext context) {
        ArrayList<String> result = new ArrayList<String>();
        final Project project = context.getPsiManager().getProject();
        GlobalSearchScope scope = GlobalSearchScope.allScope(project);
        PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(myEnumClassName, scope);
        if (psiClass != null) {
            PsiField[] fields = psiClass.getFields();
            for(PsiField field: fields) {
                String name = field.getName();
                if (name != null) {
                    result.add(name.toLowerCase());
                }
            }
        }
        return result;
    }

    public String fromString(@Nullable @NonNls String s, ConvertContext context) {
        if (s == null) return null;
        final Project project = context.getPsiManager().getProject();
        GlobalSearchScope scope = GlobalSearchScope.allScope(project);
        PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(myEnumClassName, scope);
        if (psiClass != null) {
            PsiField field = psiClass.findFieldByName(s.toUpperCase(), false);
            if (field == null) return null;
        }
        return s;
    }

    public String toString(@Nullable String s, ConvertContext context) {
        return s;
    }
}
