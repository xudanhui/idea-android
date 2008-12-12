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
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.psi.search.ProjectScope;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomManager;
import com.intellij.util.Processor;
import org.jetbrains.android.AndroidManager;
import org.jetbrains.android.dom.attrs.AttributeDefinitions;
import org.jetbrains.android.dom.attrs.StyleableDefinition;
import org.jetbrains.android.dom.manifest.Manifest;
import org.jetbrains.android.dom.resources.ResourceElement;
import org.jetbrains.android.dom.resources.Resources;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

/**
 * @author yole
 */
public class AndroidFacet extends Facet<AndroidFacetConfiguration> {
    public static final FacetTypeId<AndroidFacet> ID = new FacetTypeId<AndroidFacet>("android");
    private Map<String, PsiClass> viewClassMap = null;

    private AttributeDefinitions myManifestAttributeDefinitions;
    private AttributeDefinitions myLayoutAttributeDefinitions;

    public AndroidFacet(@NotNull Module module, String name, @NotNull AndroidFacetConfiguration configuration) {
        super(getFacetType(), module, name, configuration, null);
    }

    public static AndroidFacet getInstance(Module module) {
        return FacetManager.getInstance(module).getFacetByType(ID);
    }

    private void addViewClassToMap(PsiClass viewClass) {
        viewClassMap.put(viewClass.getName(), viewClass);
    }

    private synchronized Map<String, PsiClass> getViewClassMap() {
        if (viewClassMap == null) {
            viewClassMap = new HashMap<String, PsiClass>();
            Project project = getModule().getProject();
            JavaPsiFacade facade = JavaPsiFacade.getInstance(project);
            PsiClass viewClass = facade.findClass("android.view.View", ProjectScope.getAllScope(project));
            
            if (viewClass != null) {
                addViewClassToMap(viewClass);
                ClassInheritorsSearch.search(viewClass).forEach(new Processor<PsiClass>() {
                    public boolean process(PsiClass psiClass) {
                        addViewClassToMap(psiClass);
                        return true;
                    }
                });
            }
        }
        return viewClassMap;
    }

    public Set<String> getViewClassNames() {
        return getViewClassMap().keySet();
    }

    public PsiClass getViewClass(String name) {
        return getViewClassMap().get(name);
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
    private VirtualFile getResourceTypeDir(String resourceType, @Nullable String resPackage) {
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

    public List<String> getResourceFileNames(String resourceType) {
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

    public AttributeDefinitions getManifestAttributeDefinitions() {
        if (myManifestAttributeDefinitions == null) {
            myManifestAttributeDefinitions = parseAttributeDefinitions("attrs_manifest.xml");
        }
        return myManifestAttributeDefinitions;
    }

    public AttributeDefinitions getLayoutAttributeDefinitions() {
        if (myLayoutAttributeDefinitions == null) {
            myLayoutAttributeDefinitions = parseAttributeDefinitions("attrs.xml");
            linkSuperclasses(myLayoutAttributeDefinitions);
        }
        return myLayoutAttributeDefinitions;
    }

    private void linkSuperclasses(AttributeDefinitions attributeDefinitions) {
        for (String name : attributeDefinitions.getStyleableNames()) {
            final StyleableDefinition definition = attributeDefinitions.getStyleableDefinition(name);
//            final PsiClass superClass = findSuperclass(name);
//            if (superClass != null) {
//                StyleableDefinition superclassDefinition = attributeDefinitions.getStyleableDefinition(superClass.getName());
//                definition.setSuperclass(superclassDefinition);
//            }
            StyleableDefinition baseStyleable = getBaseStyleable(attributeDefinitions, name);
            definition.setSuperclass(baseStyleable);
        }
    }

    public PsiClass findSuperclass(String name) {
        /*JavaPsiFacade facade = JavaPsiFacade.getInstance(getModule().getProject());
        PsiClass layoutClass = facade.findClass("android.widget." + name, getModule().getModuleWithDependenciesAndLibrariesScope(false));
        if (layoutClass != null) {
            return layoutClass.getSuperClass();
        }
        return null;*/
        PsiClass psiClass = getViewClass(name);
        if (psiClass == null) return null;
        return psiClass.getSuperClass();
    }

    private AttributeDefinitions parseAttributeDefinitions(String fileName) {
        final VirtualFile sdkValuesDir = getResourceTypeDir("values", "android");
        if (sdkValuesDir == null) return null;
        final VirtualFile vFile = sdkValuesDir.findChild(fileName);
        if (vFile == null) return null;
        final PsiFile file = PsiManager.getInstance(getModule().getProject()).findFile(vFile);
        if (!(file instanceof XmlFile)) return null;
        return new AttributeDefinitions((XmlFile) file);
    }

    public static AndroidFacetType getFacetType() {
        return (AndroidFacetType) FacetTypeRegistry.getInstance().findFacetType(AndroidFacet.ID);
    }

    public StyleableDefinition getManifestStyleableByTagName(String tagName) {
        String styleableName = getManifestStyleableName(tagName);
        AttributeDefinitions definitions = getManifestAttributeDefinitions();
        return definitions.getStyleableDefinition(styleableName);
    }

    private StyleableDefinition getBaseStyleable(AttributeDefinitions definitions, String styleableName) {
        PsiClass superClass = findSuperclass(styleableName);
        while (superClass != null) {
            StyleableDefinition definition = definitions.getStyleableDefinition(superClass.getName());
            if (definition != null) return definition;
            superClass = superClass.getSuperClass();
        }
        return null;
    }

    public StyleableDefinition getLayoutStyleableByTagName(String tagName) {
        final AttributeDefinitions attrDefs = getLayoutAttributeDefinitions();
        final StyleableDefinition definition = attrDefs.getStyleableDefinition(tagName);
        if (definition != null) {
            return definition;
        }
        /*else {
            // e.g. TimePicker is not listed in attrs.xml
            final PsiClass superClass = findSuperclass(tagName);
            if (superClass != null) {
                final StyleableDefinition superStyleable = attrDefs.getStyleableDefinition(superClass.getName());
                if (superStyleable != null) {
                    return superStyleable;
                }
            }
        }
        return null;*/
        return getBaseStyleable(attrDefs, tagName);
    }

    private static String getManifestStyleableName(String tagName) {
        String prefix = "AndroidManifest";
        if (tagName.equals("manifest")) return prefix;
        String[] parts = tagName.split("-");
        StringBuilder builder = new StringBuilder(prefix);
        for (String part : parts) {
            char first = part.charAt(0);
            String remained = part.substring(1);
            builder.append(Character.toUpperCase(first)).append(remained);
        }
        return builder.toString();
    }
}
