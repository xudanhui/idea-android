package org.jetbrains.android.facet;

import com.intellij.facet.FacetManager;
import com.intellij.ide.util.newProjectWizard.FrameworkSupportConfigurable;
import com.intellij.ide.util.newProjectWizard.FrameworkSupportProvider;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import org.jetbrains.android.util.AndroidBundle;
import org.jetbrains.annotations.NotNull;

/**
 * @author yole
 */
public class AndroidSupportProvider extends FrameworkSupportProvider {
    public AndroidSupportProvider() {
        super("Android", AndroidBundle.message("support.title"));
    }

    @NotNull
    public FrameworkSupportConfigurable createConfigurable(Project project) {
        return new AndroidSupportConfigurable();
    }

    public boolean isSupportAlreadyAdded(@NotNull Module module) {
        return !FacetManager.getInstance(module).getFacetsByType(AndroidFacet.ID).isEmpty();
    }
}
