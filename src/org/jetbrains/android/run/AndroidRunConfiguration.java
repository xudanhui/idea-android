package org.jetbrains.android.run;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.*;
import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.DefaultJDOMExternalizer;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.listeners.RefactoringElementListener;
import org.jdom.Element;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author yole, coyote
 */
public class AndroidRunConfiguration extends ModuleBasedConfiguration<JavaRunConfigurationModule> {
    public String ACTIVITY_CLASS = "";

    public AndroidRunConfiguration(String name, Project project, ConfigurationFactory factory) {
        super(name, new JavaRunConfigurationModule(project, false), factory);
    }

    public void checkConfiguration() throws RuntimeConfigurationException {
        final JavaRunConfigurationModule configurationModule = getConfigurationModule();
        configurationModule.checkModuleAndClassName(ACTIVITY_CLASS, "Activity class not specified");
    }

    public Collection<Module> getValidModules() {
        final List<Module> result = new ArrayList<Module>();
        Module[] modules = ModuleManager.getInstance(getProject()).getModules();
        for (Module module : modules) {
            if (AndroidFacet.getInstance(module) != null) {
                result.add(module);
            }
        }
        return result;
    }

    public void readExternal(Element element) throws InvalidDataException {
        super.readExternal(element);
        readModule(element);
        DefaultJDOMExternalizer.readExternal(this, element);
    }

    public void writeExternal(Element element) throws WriteExternalException {
        super.writeExternal(element);
        writeModule(element);
        DefaultJDOMExternalizer.writeExternal(this, element);
    }

    protected ModuleBasedConfiguration createInstance() {
        return new AndroidRunConfiguration(getName(), getProject(), AndroidRunConfigurationType.getInstance().getFactory());
    }

    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new AndroidRunConfigurationEditor(getProject());
    }

    @Nullable
    public RefactoringElementListener getRefactoringElementListener(PsiElement element) {
        if (element instanceof PsiClass && Comparing.strEqual(((PsiClass) element).getQualifiedName(), ACTIVITY_CLASS, true)) {
            return new RefactoringElementListener() {
                public void elementMoved(@NotNull PsiElement newElement) {
                    ACTIVITY_CLASS = ((PsiClass) newElement).getQualifiedName();
                }

                public void elementRenamed(@NotNull PsiElement newElement) {
                    ACTIVITY_CLASS = ((PsiClass) newElement).getQualifiedName();
                }
            };
        }
        return null;
    }

    public synchronized RunProfileState getState(@NotNull Executor executor,
                                                 @NotNull final ExecutionEnvironment executionEnvironment) throws ExecutionException {
        final Module module = getConfigurationModule().getModule();
        if (module == null) {
            throw new ExecutionException("Module is not found");
        }
        AndroidFacet facet = AndroidFacet.getInstance(module);
        if (facet == null) {
            throw new ExecutionException("No Android facet found for module");
        }
        AndroidRunningState state = new AndroidRunningState(executionEnvironment, facet, ACTIVITY_CLASS);
        TextConsoleBuilder builder = TextConsoleBuilderFactory.getInstance().createBuilder(getProject());
        state.setConsoleBuilder(builder);
        return state;
    }
}
