package org.jetbrains.android.compiler;

import com.intellij.compiler.impl.CompilerUtil;
import com.intellij.facet.FacetManager;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.*;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomManager;
import org.jetbrains.android.compiler.tools.AndroidApt;
import org.jetbrains.android.dom.manifest.Manifest;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.android.facet.AndroidFacetConfiguration;
import org.jetbrains.android.AndroidManager;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Apt compiler.
 *
 * @author Alexey Efimov
 */
public class AndroidAptCompiler implements SourceGeneratingCompiler, ProjectComponent {
    private static final GenerationItem[] EMPTY_GENERATION_ITEM_ARRAY = {};

    private final Project myProject;
    private final CompilerManager myCompilerManager;

    public AndroidAptCompiler(Project project, CompilerManager compilerManager) {
        myProject = project;
        myCompilerManager = compilerManager;
    }

    public GenerationItem[] getGenerationItems(CompileContext context) {
        Module[] affectedModules = context.getCompileScope().getAffectedModules();
        if (affectedModules != null && affectedModules.length > 0) {
            Application application = ApplicationManager.getApplication();
            return application.runReadAction(new PrepareAction(context));
        }
        return EMPTY_GENERATION_ITEM_ARRAY;
    }

    public GenerationItem[] generate(CompileContext context, GenerationItem[] items, VirtualFile outputRootDirectory) {
        if (items != null && items.length > 0) {
            Application application = ApplicationManager.getApplication();
            GenerationItem[] generationItems = application.runReadAction(new GenerateAction(context, items));
            for (GenerationItem item : generationItems) {
                File generatedFile = ((AptGenerationItem) item).getGeneratedFile();
                if (generatedFile != null) {
                    CompilerUtil.refreshIOFile(generatedFile);
                }
            }
            return generationItems;
        }
        return EMPTY_GENERATION_ITEM_ARRAY;
    }

    @NotNull
    public String getDescription() {
        return AndroidApt.TOOL;
    }

    public boolean validateConfiguration(CompileScope scope) {
        return true;
    }

    public ValidityState createValidityState(DataInputStream is) throws IOException {
        return null;
    }

    public void projectOpened() {
    }

    public void projectClosed() {
    }

    @NonNls
    @NotNull
    public String getComponentName() {
        return "AndroidAptCompiler";
    }

    public void initComponent() {
        myCompilerManager.addCompiler(this);
    }

    public void disposeComponent() {
        myCompilerManager.removeCompilableFileType(StdFileTypes.XML);
    }

    private final static class AptGenerationItem implements GenerationItem {
        private final Module myModule;
        private final String myRootPath;
        private final String myResourcesPath;
        private final String mySourceRootPath;
        private final String mySdkPath;
        private final File myGeneratedFile;

        private AptGenerationItem(Module module, String rootPath, String resourcesPath, String sourceRootPath, String sdkPath, String packageValue) {
            myModule = module;
            myRootPath = rootPath;
            myResourcesPath = resourcesPath;
            mySourceRootPath = sourceRootPath;
            mySdkPath = sdkPath;
            myGeneratedFile = new File(sourceRootPath, packageValue.replace('.', File.separatorChar) + File.separatorChar + "R.java");
        }

        public String getRootPath() {
            return myRootPath;
        }

        public String getSourceRootPath() {
            return mySourceRootPath;
        }

        public String getResourcesPath() {
            return myResourcesPath;
        }

        public String getPath() {
            return null;
        }

        public ValidityState getValidityState() {
            return null;
        }

        public Module getModule() {
            return myModule;
        }

        public boolean isTestSource() {
            return false;
        }

        public String getSdkPath() {
            return mySdkPath;
        }

        public File getGeneratedFile() {
            return myGeneratedFile;
        }
    }

    private final class PrepareAction implements Computable<GenerationItem[]> {
        private final CompileContext myContext;

        public PrepareAction(CompileContext context) {
            myContext = context;
        }

        public GenerationItem[] compute() {
            CompileScope compileScope = myContext.getCompileScope();
            PsiManager psiManager = PsiManager.getInstance(myProject);
            DomManager domManager = DomManager.getDomManager(myProject);
            Module[] modules = compileScope.getAffectedModules();
            List<GenerationItem> items = new ArrayList<GenerationItem>();
            for (Module module : modules) {
                AndroidFacet facet = FacetManager.getInstance(module).getFacetByType(AndroidFacet.ID);
                if (facet != null) {
                    ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
                    VirtualFile[] sourceRoots = rootManager.getSourceRoots();
                    VirtualFile[] roots = rootManager.getContentRoots();
                    AndroidFacetConfiguration configuration = facet.getConfiguration();
                    for (VirtualFile root : roots) {
                        VirtualFile manifestFile = root.findChild(AndroidManager.MANIFEST_FILE_NAME);
                        if (manifestFile != null) {
                            PsiFile manifestPsiFile = psiManager.findFile(manifestFile);
                            if (manifestPsiFile instanceof XmlFile) {
                                XmlFile manifestXmlFile = (XmlFile) manifestPsiFile;
                                DomFileElement<Manifest> manifest = domManager.getFileElement(manifestXmlFile, Manifest.class);
                                if (manifest != null) {
                                    Manifest rootElement = manifest.getRootElement();
                                    VirtualFile res = root.findChild(configuration.RESOURCES_PATH);
                                    if (res != null && res.isDirectory()) {
                                        for (VirtualFile sourceRoot : sourceRoots) {
                                            items.add(new AptGenerationItem(module, root.getPath(), res.getPath(), sourceRoot.getPath(), configuration.SDK_PATH, rootElement.getPackage().getValue()));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return items.toArray(new GenerationItem[items.size()]);
        }
    }

    private static final class GenerateAction implements Computable<GenerationItem[]> {
        private final CompileContext myContext;
        private final GenerationItem[] myItems;

        public GenerateAction(CompileContext context, GenerationItem[] items) {
            myContext = context;
            myItems = items;
        }

        public GenerationItem[] compute() {
            List<GenerationItem> results = new ArrayList<GenerationItem>(myItems.length);
            for (GenerationItem item : myItems) {
                if (item instanceof AptGenerationItem) {
                    AptGenerationItem aptItem = (AptGenerationItem) item;
                    try {
                        Map<CompilerMessageCategory, List<String>> messages = AndroidApt.compile(
                                aptItem.getRootPath(),
                                aptItem.getSourceRootPath(),
                                aptItem.getResourcesPath(),
                                aptItem.getSdkPath()
                        );
                        addMessages(messages);
                        if (messages.get(CompilerMessageCategory.ERROR).isEmpty()) {
                            results.add(aptItem);
                        }
                    } catch (IOException e) {
                        myContext.addMessage(CompilerMessageCategory.ERROR, e.getMessage(), null, -1, -1);
                    }
                }
            }
            return results.toArray(new GenerationItem[results.size()]);
        }

        private void addMessages(Map<CompilerMessageCategory, List<String>> messages) {
            for (CompilerMessageCategory category : messages.keySet()) {
                List<String> messageList = messages.get(category);
                for (String message : messageList) {
                    myContext.addMessage(category, message, null, -1, -1);
                }
            }
        }
    }
}
