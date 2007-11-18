package org.jetbrains.android.run;

import com.intellij.execution.ExecutionUtil;
import com.intellij.execution.Location;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl;
import com.intellij.execution.junit.RuntimeConfigurationProducer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Nullable;

/**
 * @author yole
 */
public class AndroidConfigurationProducer extends RuntimeConfigurationProducer implements Cloneable {
    private PsiElement mySourceElement;

    public AndroidConfigurationProducer() {
        super(AndroidRunConfigurationType.getInstance());
    }

    public PsiElement getSourceElement() {
        return mySourceElement;
    }

    @Nullable
    protected RunnerAndConfigurationSettingsImpl createConfigurationByElement(Location location, ConfigurationContext configurationContext) {
        location = ExecutionUtil.stepIntoSingleClass(location);
        PsiElement element = location.getPsiElement();
        PsiClass activityClass = element.getManager().findClass("android.app.Activity", element.getProject().getAllScope());
        if (activityClass == null) return null;
        while((element = PsiTreeUtil.getParentOfType(element, PsiClass.class)) != null) {
            PsiClass elementClass = (PsiClass) element;
            if (elementClass.isInheritor(activityClass, true)) {
                mySourceElement = elementClass;
                return createConfiguration((PsiClass) mySourceElement, configurationContext);
            }
        }
        return null;
    }

    private RunnerAndConfigurationSettingsImpl createConfiguration(PsiClass psiClass, ConfigurationContext context) {
        Project project = psiClass.getProject();
        RunnerAndConfigurationSettingsImpl settings = cloneTemplateConfiguration(project, context);
        final AndroidRunConfiguration configuration = (AndroidRunConfiguration) settings.getConfiguration();
        configuration.ACTIVITY_CLASS = psiClass.getQualifiedName();
        configuration.setName(ExecutionUtil.getPresentableClassName(configuration.ACTIVITY_CLASS,
                configuration.getConfigurationModule()));
        configuration.setModule(ExecutionUtil.findModule(psiClass));
        return settings;
    }

    public int compareTo(Object o) {
        return -1;
    }
}
