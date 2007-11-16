package org.jetbrains.android.facet;

import com.intellij.ide.util.newProjectWizard.FrameworkSupportConfigurable;
import com.intellij.ide.util.newProjectWizard.FrameworkSupportProvider;
import com.intellij.openapi.module.Module;
import com.intellij.facet.FacetManager;
import org.jetbrains.annotations.NotNull;

/**
 * @author yole
 */
public class AndroidSupportProvider extends FrameworkSupportProvider {
    public AndroidSupportProvider() {
        super("Android", "Android");
    }

    @NotNull
    public FrameworkSupportConfigurable createConfigurable() {
        return new AndroidSupportConfigurable();
    }

    public boolean isSupportAlreadyAdded(@NotNull Module module) {
        return !FacetManager.getInstance(module).getFacetsByType(AndroidFacet.ID).isEmpty();
    }
}
