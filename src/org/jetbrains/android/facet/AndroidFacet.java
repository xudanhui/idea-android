package org.jetbrains.android.facet;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetManager;
import com.intellij.facet.FacetTypeId;
import com.intellij.facet.FacetTypeRegistry;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.CompilerModuleExtension;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomManager;
import org.jetbrains.android.AndroidManager;
import org.jetbrains.android.dom.StyleableProvider;
import org.jetbrains.android.dom.layout.LayoutStyleableProvider;
import org.jetbrains.android.dom.manifest.Manifest;
import org.jetbrains.android.dom.manifest.ManifestStyleableProvider;
import org.jetbrains.android.dom.resources.ResourceElement;
import org.jetbrains.android.dom.resources.Resources;
import org.jetbrains.android.util.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

/**
 * @author yole
 */
public class AndroidFacet extends Facet<AndroidFacetConfiguration> {
    public static final FacetTypeId<AndroidFacet> ID = new FacetTypeId<AndroidFacet>("android");
    private final Map<Key<? extends StyleableProvider>, StyleableProvider> styleableProviders =
            new HashMap<Key<? extends StyleableProvider>, StyleableProvider>();  

    public AndroidFacet(@NotNull Module module, String name, @NotNull AndroidFacetConfiguration configuration) {
        super(getFacetType(), module, name, configuration, null);
        registerStyleableProvider(ManifestStyleableProvider.KEY, new ManifestStyleableProvider(this));
        registerStyleableProvider(LayoutStyleableProvider.KEY, new LayoutStyleableProvider(this));
    }

    public static AndroidFacet getInstance(Module module) {
        return FacetManager.getInstance(module).getFacetByType(ID);
    }

    private <T extends StyleableProvider> void registerStyleableProvider(Key<T> key, T provider) {
        styleableProviders.put(key, provider);
    }

    public <T extends StyleableProvider> T getStyleableProvider(Key<T> key) {
        return (T) styleableProviders.get(key);
    }

    public Collection<StyleableProvider> getStyleableProviders() {
        return styleableProviders.values();
    }

    public StyleableProvider getStyleableProviderForFile(@NotNull XmlFile file, Module module) {
        for (Map.Entry<Key<? extends StyleableProvider>, StyleableProvider> entry : styleableProviders.entrySet()) {
            StyleableProvider provider = entry.getValue();
            if (provider.isMyFile(file, module)) return provider;
        }
        return null;
    }

    @Nullable
    public VirtualFile getManifestFile() {
        VirtualFile[] files = ModuleRootManager.getInstance(getModule()).getContentRoots();
        for (VirtualFile contentRoot : files) {
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

    @Nullable
    public VirtualFile getSdkResourcesDir() {
        final String sdkPath = getSdkPath();
        if (StringUtil.isEmpty(sdkPath)) return null;
        final VirtualFile sdkRoot = LocalFileSystem.getInstance().findFileByPath(sdkPath);
        if (sdkRoot == null) return null;
        return sdkRoot.findFileByRelativePath("tools/lib/res/default");
    }

    @Nullable
    public VirtualFile getResourceTypeDir(String resourceType, @Nullable String resPackage) {
        VirtualFile resourcesDir = "android".equals(resPackage) ? getSdkResourcesDir() : getResourcesDir();
        if (resourcesDir == null) return null;
        return resourcesDir.findChild(resourceType);
    }

    public String getSdkPath() {
        return getConfiguration().getSdkPath();
    }

    public String getOutputPackage() {
        VirtualFile compilerOutput = CompilerModuleExtension.getInstance(getModule()).getCompilerOutputPath();
        return new File(compilerOutput.getPath(), getModule().getName() + ".apk").getPath();
    }

    @Nullable
    public Manifest getManifest() {
        final VirtualFile manifestFile = getManifestFile();
        if (manifestFile == null) return null;
        return loadDomElement(manifestFile, Manifest.class);
    }

    public List<Resources> getValueResources(@Nullable String resPackage) {
        List<Resources> result = new ArrayList<Resources>();
        VirtualFile valuesDir = getResourceTypeDir("values", resPackage);
        if (valuesDir != null) {
            for (VirtualFile valuesFile : valuesDir.getChildren()) {
                if (!valuesFile.isDirectory() && valuesFile.getFileType().equals(StdFileTypes.XML)) {
                    Resources resources = loadDomElement(valuesFile, Resources.class);
                    if (resources != null) {
                        result.add(resources);
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

    public List<ResourceElement> getResources(@Nullable String resType, @Nullable String resPackage) {
        List<ResourceElement> result = new ArrayList<ResourceElement>();
        List<Resources> resourceFiles = getValueResources(resPackage);
        for (Resources res : resourceFiles) {
            if (resType == null || resType.equals("string")) {
                result.addAll(res.getStrings());
            }
            if (resType == null || resType.equals("drawable")) {
                result.addAll(res.getDrawables());
            }
            if (resType == null || resType.equals("color")) {
                result.addAll(res.getColors());
            }
            if (resType == null || resType.equals("style")) {
                result.addAll(res.getStyles());
            }
        }
        return result;
    }

    @Nullable
    public PsiFile findResourceFile(String resType, String resourceName, @Nullable String resPackage) {
        if (resType.equals("drawable")) {
            return findDrawable(resourceName, resPackage);
        }
        return null;
    }

    private static final String[] DRAWABLE_EXTENSIONS = new String[]{".png", ".9.png", ".jpg"};

    @Nullable
    private PsiFile findDrawable(String resourceName, @Nullable String resPackage) {
        VirtualFile typeDir = getResourceTypeDir("drawable", resPackage);
        if (typeDir == null) return null;
        for (String ext : DRAWABLE_EXTENSIONS) {
            final VirtualFile drawableFile = typeDir.findChild(resourceName + ext);
            if (drawableFile != null) {
                return ApplicationManager.getApplication().runReadAction(new Computable<PsiFile>() {
                    public PsiFile compute() {
                        return PsiManager.getInstance(getModule().getProject()).findFile(drawableFile);
                    }
                });
            }
        }
        return null;
    }

    public List<String> getResourceFileNames(@NotNull String resourceType) {
        List<String> result = new ArrayList<String>();
        if (resourceType.equals("drawable")) {
            VirtualFile drawablesDir = getResourceTypeDir("drawable", null);
            if (drawablesDir != null) {
                VirtualFile[] files = drawablesDir.getChildren();
                for (VirtualFile file : files) {
                    if (file.isDirectory()) continue;
                    for (String ext : DRAWABLE_EXTENSIONS) {
                        String fileName = file.getName();
                        if (fileName.endsWith(ext)) {
                            result.add(fileName.substring(0, fileName.length() - ext.length()));
                        }
                    }
                }
            }
        }
        return result;
    }

    public static AndroidFacetType getFacetType() {
        return (AndroidFacetType) FacetTypeRegistry.getInstance().findFacetType(AndroidFacet.ID);
    }
}
