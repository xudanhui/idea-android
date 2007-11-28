package org.jetbrains.android.compiler;

import com.intellij.compiler.impl.CompilerUtil;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.*;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.android.compiler.tools.AndroidApt;
import org.jetbrains.android.dom.manifest.Manifest;
import org.jetbrains.android.facet.AndroidFacet;
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

    private final CompilerManager myCompilerManager;

    public AndroidAptCompiler(CompilerManager compilerManager) {
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
            context.getProgressIndicator().setText("Generating R.java...");
            GenerationItem[] generationItems = doGenerate(context, items);
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

    private static GenerationItem[] doGenerate(CompileContext context, GenerationItem[] items) {
        List<GenerationItem> results = new ArrayList<GenerationItem>(items.length);
        for (GenerationItem item : items) {
            if (item instanceof AptGenerationItem) {
                AptGenerationItem aptItem = (AptGenerationItem) item;
                try {
                    Map<CompilerMessageCategory, List<String>> messages = AndroidApt.compile(
                            aptItem.getRootPath(),
                            aptItem.getSourceRootPath(),
                            aptItem.getResourcesPath(),
                            aptItem.getSdkPath()
                    );
                    AndroidCompileUtil.addMessages(context, messages);
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
    @NotNull
    public String getDescription() {
        return AndroidApt.TOOL;
    }

    public boolean validateConfiguration(CompileScope scope) {
        return true;
    }

    public ValidityState createValidityState(DataInputStream is) throws IOException {
        return new ResourcesValidityState(is);
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
            return "R.java";
        }

        public ValidityState getValidityState() {
            return new ResourcesValidityState(myModule, false);
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

    private static final class PrepareAction implements Computable<GenerationItem[]> {
        private final CompileContext myContext;

        public PrepareAction(CompileContext context) {
            myContext = context;
        }

        public GenerationItem[] compute() {
            CompileScope compileScope = myContext.getCompileScope();
            Module[] modules = compileScope.getAffectedModules();
            List<GenerationItem> items = new ArrayList<GenerationItem>();
            for (Module module : modules) {
                AndroidFacet facet = AndroidFacet.getInstance(module);
                if (facet != null) {
                    Manifest manifest = facet.getManifest();
                    VirtualFile resourcesDir = facet.getResourcesDir();
                    if (manifest != null && resourcesDir != null) {
                        String packageName = manifest.getPackage().getValue();
                        if (packageName != null) {
                            ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
                            VirtualFile[] sourceRoots = rootManager.getSourceRoots();
                            for (VirtualFile sourceRoot : sourceRoots) {
                                items.add(new AptGenerationItem(module, resourcesDir.getParent().getPath(), resourcesDir.getPath(),
                                        sourceRoot.getPath(), facet.getSdkPath(), packageName));
                            }
                        }
                    }
                }
            }
            return items.toArray(new GenerationItem[items.size()]);
        }
    }
}
