package org.jetbrains.android.facet;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetManager;
import com.intellij.facet.FacetTypeId;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomManager;
import org.jetbrains.android.AndroidManager;
import org.jetbrains.android.dom.manifest.Manifest;
import org.jetbrains.android.dom.resources.Resources;
import org.jetbrains.android.dom.resources.ResourceElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yole
 */
public class AndroidFacet extends Facet<AndroidFacetConfiguration> {
    public static final FacetTypeId<AndroidFacet> ID = new FacetTypeId<AndroidFacet>("android");

    public static final AndroidFacetType ourFacetType = new AndroidFacetType();

    public AndroidFacet(@NotNull Module module, String name, @NotNull AndroidFacetConfiguration configuration) {
        super(ourFacetType, module, name, configuration, null);
    }

    public static AndroidFacet getInstance(Module module) {
        return FacetManager.getInstance(module).getFacetByType(ID);
    }

    @Nullable
    public VirtualFile getManifestFile() {
        VirtualFile[] files = ModuleRootManager.getInstance(getModule()).getContentRoots();
        for(VirtualFile contentRoot: files) {
            VirtualFile manifest = contentRoot.findChild(AndroidManager.MANIFEST_FILE_NAME);
            if (manifest != null) {
                return manifest;
            }
        }
        return null;
    }

    @Nullable
    public VirtualFile getResourcesDir() {
        VirtualFile manifestFile = getManifestFile();
        if (manifestFile != null) {
            VirtualFile parent = manifestFile.getParent();
            assert parent != null;
            return parent.findChild(getConfiguration().RESOURCES_PATH);
        }
        return null;
    }

    public String getSdkPath() {
        return getConfiguration().SDK_PATH;
    }

    public String getOutputPackage() {
        VirtualFile compilerOutput = ModuleRootManager.getInstance(getModule()).getCompilerOutputPath();
        return new File(compilerOutput.getPath(), getModule().getName() + ".apk").getPath();
    }

    @Nullable
    public Manifest getManifest() {
        final VirtualFile manifestFile = getManifestFile();
        if (manifestFile == null) return null;
        return loadDomElement(manifestFile, Manifest.class);
    }

    public List<Resources> getValueResources() {
        List<Resources> result = new ArrayList<Resources>();
        VirtualFile resDir = getResourcesDir();
        if (resDir != null) {
            VirtualFile valuesDir = resDir.findChild("values");
            if (valuesDir != null) {
                for(VirtualFile valuesFile: valuesDir.getChildren()) {
                    if (!valuesFile.isDirectory() && valuesFile.getFileType().equals(StdFileTypes.XML)) {
                        Resources resources = loadDomElement(valuesFile, Resources.class);
                        if (resources != null) {
                            result.add(resources);
                        }
                    }
                }
            }
        }
        return result;
    }

    private <T extends DomElement> T loadDomElement(final VirtualFile manifestFile, final Class<T> aClass) {
        return ApplicationManager.getApplication().runReadAction(new Computable<T>() {
            public T compute() {
                PsiFile file = PsiManager.getInstance(getModule().getProject()).findFile(manifestFile);
                if (file == null || !(file instanceof XmlFile)) {
                    return null;
                }
                DomManager domManager = DomManager.getDomManager(getModule().getProject());
                DomFileElement<T> element = domManager.getFileElement((XmlFile) file, aClass);
                if (element == null) return null;
                return element.getRootElement();
            }
        });
    }

    public List<ResourceElement> getResourcesOfType(String resType) {
        List<ResourceElement> result = new ArrayList<ResourceElement>();
        List<Resources> resourceFiles = getValueResources();
        for(Resources res: resourceFiles) {
            if (resType.equals("string")) {
                result.addAll(res.getStrings());
            }
        }
        return result;
    }
}
