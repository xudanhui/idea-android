package org.jetbrains.android;

import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.facet.FacetTypeRegistry;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.android.facet.AndroidFacet;

/**
 * @author yole
 */
public class AndroidManager implements ApplicationComponent {
    @NonNls
    @NotNull
    public String getComponentName() {
        return "AndroidManager";
    }

    public void initComponent() {
        FacetTypeRegistry.getInstance().registerFacetType(AndroidFacet.ourFacetType);
    }

    public void disposeComponent() {
    }
}
