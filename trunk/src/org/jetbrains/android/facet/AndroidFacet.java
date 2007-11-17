package org.jetbrains.android.facet;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetManager;
import com.intellij.facet.FacetTypeId;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.xml.DomManager;
import com.intellij.util.xml.DomFileElement;
import org.jetbrains.android.AndroidManager;
import org.jetbrains.android.dom.manifest.Manifest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

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
        return ApplicationManager.getApplication().runReadAction(new Computable<Manifest>() {
            public Manifest compute() {
                PsiFile file = PsiManager.getInstance(getModule().getProject()).findFile(manifestFile);
                if (file == null || !(file instanceof XmlFile)) {
                    return null;
                }
                DomManager domManager = DomManager.getDomManager(getModule().getProject());
                DomFileElement<Manifest> element = domManager.getFileElement((XmlFile) file, Manifest.class);
                if (element == null) return null;
                return element.getRootElement();
            }
        });
    }
}
