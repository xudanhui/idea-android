package org.jetbrains.android.facet;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetTypeId;
import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;

/**
 * @author yole
 */
public class AndroidFacet extends Facet<AndroidFacetConfiguration> {
    public static final FacetTypeId<AndroidFacet> ID = new FacetTypeId<AndroidFacet>("android");

    public static final AndroidFacetType ourFacetType = new AndroidFacetType();

    public AndroidFacet(@NotNull Module module, String name, @NotNull AndroidFacetConfiguration configuration) {
        super(ourFacetType, module, name, configuration, null);
    }
}
