package org.jetbrains.android.compiler;

import com.intellij.compiler.impl.CompilerUtil;
import com.intellij.facet.FacetManager;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.*;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.android.compiler.tools.Apt;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.android.facet.AndroidFacetConfiguration;
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

    private final CompilerManager compilerManager;

    public AndroidAptCompiler(CompilerManager compilerManager) {
        this.compilerManager = compilerManager;
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
        return Apt.TOOL;
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
        compilerManager.addCompiler(this);
    }

    public void disposeComponent() {
        compilerManager.removeCompilableFileType(StdFileTypes.XML);
    }

    private final class AptGenerationItem implements GenerationItem {
        private final Module module;
        private final String rootPath;
        private final String resPath;
        private final String sourceRootPath;
        private final String sdkPath;
        private String path;

        private AptGenerationItem(Module module, String rootPath, String resPath, String sourceRootPath, String sdkPath) {
            this.module = module;
            this.rootPath = rootPath;
            this.resPath = resPath;
            this.sourceRootPath = sourceRootPath;
            this.sdkPath = sdkPath;
        }

        public String getRootPath() {
            return rootPath;
        }

        public String getSourceRootPath() {
            return sourceRootPath;
        }

        public String getResPath() {
            return resPath;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public ValidityState getValidityState() {
            return null;
        }

        public Module getModule() {
            return module;
        }

        public boolean isTestSource() {
            return false;
        }

        public String getSdkPath() {
            return sdkPath;
        }

        public String getAndroidJar() {
            return sdkPath + File.separator + "android.jar";
        }

        public File getGeneratedFile() {
            return null;
        }
    }

    private final class PrepareAction implements Computable<GenerationItem[]> {
        private final CompileContext context;

        public PrepareAction(CompileContext context) {
            this.context = context;
        }

        public GenerationItem[] compute() {
            CompileScope compileScope = context.getCompileScope();
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
                        VirtualFile res = root.findChild("res");
                        if (res != null && res.isDirectory()) {
                            for (VirtualFile sourceRoot : sourceRoots) {
                                items.add(new AptGenerationItem(module, root.getPath(), res.getPath(), sourceRoot.getPath(), configuration.SDK_PATH));
                            }
                        }
                    }
                }
            }
            return items.toArray(new GenerationItem[items.size()]);
        }
    }

    private static class GenerateAction implements Computable<GenerationItem[]> {
        private final CompileContext context;
        private final GenerationItem[] items;

        public GenerateAction(CompileContext context, GenerationItem[] items) {
            this.context = context;
            this.items = items;
        }

        public GenerationItem[] compute() {
            List<GenerationItem> results = new ArrayList<GenerationItem>(items.length);
            for (GenerationItem item : items) {
                if (item instanceof AptGenerationItem) {
                    AptGenerationItem aptItem = (AptGenerationItem) item;
                    try {
                        Map<CompilerMessageCategory, List<String>> messages = Apt.compile(
                                aptItem.getRootPath(),
                                aptItem.getSourceRootPath(),
                                aptItem.getResPath(),
                                aptItem.getSdkPath()
                        );
                        addMessages(messages);
                        if (messages.get(CompilerMessageCategory.ERROR).isEmpty()) {
                            results.add(aptItem);
                        }
                    } catch (IOException e) {
                        context.addMessage(CompilerMessageCategory.ERROR, e.getMessage(), null, -1, -1);
                    }
                }
            }
            return results.toArray(new GenerationItem[results.size()]);
        }

        private void addMessages(Map<CompilerMessageCategory, List<String>> messages) {
            for (CompilerMessageCategory category : messages.keySet()) {
                List<String> messageList = messages.get(category);
                for (String message : messageList) {
                    context.addMessage(category, message, null, -1, -1);
                }
            }
        }
    }
}
