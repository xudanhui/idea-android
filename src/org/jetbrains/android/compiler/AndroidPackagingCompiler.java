package org.jetbrains.android.compiler;

import com.intellij.openapi.compiler.*;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.io.ZipUtil;
import org.jetbrains.android.compiler.tools.AndroidApt;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.android.facet.AndroidFacetConfiguration;
import org.jetbrains.android.AndroidManager;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author yole
 */
public class AndroidPackagingCompiler implements ProjectComponent, PackagingCompiler {
    private CompilerManager myCompilerManager;

    public AndroidPackagingCompiler(CompilerManager compilerManager) {
        myCompilerManager = compilerManager;
    }

    public void projectOpened() {
    }

    public void projectClosed() {
    }

    @NonNls
    @NotNull
    public String getComponentName() {
        return "AndroidPackagingCompiler";
    }

    public void initComponent() {
        myCompilerManager.addCompiler(this);
    }

    public void disposeComponent() {
        myCompilerManager.removeCompiler(this);
    }

    public void processOutdatedItem(CompileContext context, String url, @Nullable ValidityState state) {
    }

    @NotNull
    public ProcessingItem[] getProcessingItems(CompileContext context) {
        final List<ProcessingItem> items = new ArrayList<ProcessingItem>();
        Module[] affectedModules = context.getCompileScope().getAffectedModules();
        for(Module module: affectedModules) {
            AndroidFacet facet = AndroidFacet.getInstance(module);
            if (facet != null) {
                VirtualFile manifestFile = facet.getManifestFile();
                VirtualFile resourcesDir = facet.getResourcesDir();
                if (manifestFile != null && resourcesDir != null) {
                    AndroidFacetConfiguration configuration = facet.getConfiguration();
                    VirtualFile outputDir = context.getModuleOutputDirectory(module);
                    String tempOutputPath = new File(outputDir.getPath(), module.getName() + ".apk.tmp").getPath();
                    String outputPath = new File(outputDir.getPath(), module.getName() + ".apk").getPath();
                    String classesDexPath = new File(outputDir.getPath(), AndroidManager.CLASSES_FILE_NAME).getPath();
                    items.add(new AptPackagingItem(module, manifestFile, configuration.SDK_PATH, resourcesDir.getPath(),
                            tempOutputPath, outputPath, classesDexPath));
                }
            }
        }
        return items.toArray(new ProcessingItem[items.size()]);
    }

    public ProcessingItem[] process(CompileContext context, ProcessingItem[] items) {
        final List<ProcessingItem> result = new ArrayList<ProcessingItem>();
        for(ProcessingItem processingItem: items) {
            AptPackagingItem item = (AptPackagingItem) processingItem;
            String rootDir = item.getFile().getParent().getPath();
            try {
                Map<CompilerMessageCategory,List<String>> messages = AndroidApt.packageResources(rootDir,
                        item.getSdkPath(), item.getResourcesPath(), item.getOutputPath());
                AndroidCompileUtil.addMessages(context, messages);
                if (messages.get(CompilerMessageCategory.ERROR).isEmpty()) {
                    addClassesDex(item.getOutputPath(), item.getFinalPath(), item.getClassesDexPath());
                }
            } catch (IOException e) {
                context.addMessage(CompilerMessageCategory.ERROR, e.getMessage(), null, -1, -1);
            }
            if (context.getMessages(CompilerMessageCategory.ERROR).length == 0) {
                result.add(item);
            }
        }
        return result.toArray(new ProcessingItem[result.size()]);
    }

    private void addClassesDex(String outputPath, String finalPath, String classesDexPath) throws IOException {
        FileInputStream is = new FileInputStream(outputPath);
        try {
            FileOutputStream os = new FileOutputStream(finalPath);
            try {
                Map<String, File> map = new HashMap<String, File>();
                map.put(AndroidManager.CLASSES_FILE_NAME, new File(classesDexPath));
                ZipUtil.update(is, os, map);
            }
            finally {
                os.close();
            }
        }
        finally {
            is.close();
        }
        FileUtil.delete(new File(outputPath));
    }

    @NotNull
    public String getDescription() {
        return "Android Packaging Compiler";
    }

    public boolean validateConfiguration(CompileScope scope) {
        return true;
    }

    public ValidityState createValidityState(DataInputStream is) throws IOException {
        return new ResourcesValidityState(is);
    }

    private class AptPackagingItem implements ProcessingItem {
        private Module myModule;
        private VirtualFile myFile;
        private String mySdkPath;
        private String myResourcesPath;
        private String myOutputPath;
        private String myFinalPath;
        private String myClassesDexPath;

        private AptPackagingItem(Module module, VirtualFile file, String sdkPath, String resourcesPath, String outputPath,
                                 String finalPath, String classesDexPath) {
            myModule = module;
            myFile = file;
            mySdkPath = sdkPath;
            myResourcesPath = resourcesPath;
            myOutputPath = outputPath;
            myFinalPath = finalPath;
            myClassesDexPath = classesDexPath;
        }

        @NotNull
        public VirtualFile getFile() {
            return myFile;
        }

        @Nullable
        public ValidityState getValidityState() {
            return new ResourcesValidityState(myModule);
        }

        public String getSdkPath() {
            return mySdkPath;
        }

        public String getResourcesPath() {
            return myResourcesPath;
        }

        public String getOutputPath() {
            return myOutputPath;
        }

        public String getFinalPath() {
            return myFinalPath;
        }

        public String getClassesDexPath() {
            return myClassesDexPath;
        }
    }
}
