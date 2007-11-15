package org.jetbrains.android;

import com.intellij.codeInspection.InspectionToolProvider;
import com.intellij.facet.FacetTypeRegistry;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.fileTypes.FileTypeManager;
import org.jetbrains.android.dom.manifest.ManifestDomInspection;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.android.fileTypes.AndroidIdlFileType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * @author yole
 */
public class AndroidManager implements ApplicationComponent, InspectionToolProvider {
    @NonNls
    @NotNull
    public String getComponentName() {
        return "AndroidManager";
    }

    public void initComponent() {
        FacetTypeRegistry.getInstance().registerFacetType(AndroidFacet.ourFacetType);
        FileTypeManager.getInstance().registerFileType(AndroidIdlFileType.ourFileType, AndroidIdlFileType.DEFAULT_ASSOCIATED_EXTENSIONS);
    }

    public void disposeComponent() {
    }

    public Class[] getInspectionClasses() {
        return new Class[]{
                ManifestDomInspection.class
        };
    }
}
