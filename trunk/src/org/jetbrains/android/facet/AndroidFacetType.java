package org.jetbrains.android.facet;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetType;
import com.intellij.facet.autodetecting.FacetDetector;
import com.intellij.facet.autodetecting.FacetDetectorRegistry;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileFilter;
import org.jetbrains.android.AndroidManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;

/**
 * @author yole
 */
public class AndroidFacetType extends FacetType<AndroidFacet, AndroidFacetConfiguration> {
    public AndroidFacetType() {
        super(AndroidFacet.ID, "android", "Android");
    }

    public AndroidFacetConfiguration createDefaultConfiguration() {
        AndroidFacetConfiguration configuration = new AndroidFacetConfiguration();
        if (PropertiesComponent.getInstance().isValueSet(AndroidFacetConfiguration.DEFAULT_SDK_PATH_PROPERTY)) {
            configuration.setSdkPath( PropertiesComponent.getInstance().getValue(AndroidFacetConfiguration.DEFAULT_SDK_PATH_PROPERTY)); 
        }
        return configuration;
    }

    public AndroidFacet createFacet(@NotNull Module module, String name, @NotNull AndroidFacetConfiguration configuration, @Nullable Facet underlyingFacet) {
        return new AndroidFacet(module, name, configuration);
    }

    public boolean isSuitableModuleType(ModuleType moduleType) {
        return moduleType instanceof JavaModuleType;
    }

    public void registerDetectors(FacetDetectorRegistry<AndroidFacetConfiguration> detectorRegistry) {
        FacetDetector<VirtualFile, AndroidFacetConfiguration> detector = new FacetDetector<VirtualFile, AndroidFacetConfiguration>() {
            public AndroidFacetConfiguration detectFacet(VirtualFile source, Collection<AndroidFacetConfiguration> existentFacetConfigurations) {
                if (!existentFacetConfigurations.isEmpty()) {
                  return existentFacetConfigurations.iterator().next();
                }
                return createDefaultConfiguration();
            }
        };
        VirtualFileFilter androidManifestFilter = new VirtualFileFilter() {
            public boolean accept(VirtualFile file) {
                return file.getName().equals(AndroidManager.MANIFEST_FILE_NAME);
            }
        };
        detectorRegistry.registerUniversalDetector(StdFileTypes.XML, androidManifestFilter, detector);
    }

    public Icon getIcon() {
        return AndroidManager.ANDROID_ICON;
    }
}
