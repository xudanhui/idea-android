package org.jetbrains.android.facet;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author yole
 */
public class AndroidFacetType extends FacetType<AndroidFacet, AndroidFacetConfiguration> {
    public AndroidFacetType() {
        super(AndroidFacet.ID, "android", "Android");
    }

    public AndroidFacetConfiguration createDefaultConfiguration() {
        return new AndroidFacetConfiguration();
    }

    public AndroidFacet createFacet(@NotNull Module module, String name, @NotNull AndroidFacetConfiguration configuration, @Nullable Facet underlyingFacet) {
        return new AndroidFacet(module, name, configuration);
    }

    public Icon getIcon() {
        return IconLoader.getIcon("/icons/android.png");
    }
}
