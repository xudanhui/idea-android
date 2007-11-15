package org.jetbrains.android.facet;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetType;
import com.intellij.facet.autodetecting.FacetDetector;
import com.intellij.facet.autodetecting.FacetDetectorRegistry;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileFilter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.android.AndroidManager;

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
        return new AndroidFacetConfiguration();
    }

    public AndroidFacet createFacet(@NotNull Module module, String name, @NotNull AndroidFacetConfiguration configuration, @Nullable Facet underlyingFacet) {
        return new AndroidFacet(module, name, configuration);
    }

    public void registerDetectors(FacetDetectorRegistry<AndroidFacetConfiguration> detectorRegistry) {
        FacetDetector<VirtualFile, AndroidFacetConfiguration> detector = new FacetDetector<VirtualFile, AndroidFacetConfiguration>() {
            public AndroidFacetConfiguration detectFacet(VirtualFile source, Collection<AndroidFacetConfiguration> existentFacetConfigurations) {
                if (!existentFacetConfigurations.isEmpty()) {
                  return existentFacetConfigurations.iterator().next();
                }
                return new AndroidFacetConfiguration();
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
        return IconLoader.getIcon("/icons/android.png");
    }
}
