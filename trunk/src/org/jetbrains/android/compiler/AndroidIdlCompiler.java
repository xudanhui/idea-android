package org.jetbrains.android.compiler;

import com.intellij.compiler.impl.CompilerUtil;
import com.intellij.facet.FacetManager;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.*;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.android.compiler.tools.AndroidIdl;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.android.fileTypes.AndroidIdlFileType;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Android IDL compiler.
 *
 * @author Alexey Efimov
 */
public class AndroidIdlCompiler implements SourceGeneratingCompiler {
    private static final GenerationItem[] EMPTY_GENERATION_ITEM_ARRAY = {};

    private final Project myProject;

    public AndroidIdlCompiler(Project project) {
        myProject = project;
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
                File generatedFile = ((IdlGenerationItem) item).getGeneratedFile();
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
        return AndroidIdl.TOOL;
    }

    public boolean validateConfiguration(CompileScope scope) {
        return true;
    }

    public ValidityState createValidityState(DataInput is) throws IOException {
        return null;
    }

    private final static class IdlGenerationItem implements GenerationItem {
        private final Module myModule;
        private final VirtualFile myFile;
        private final boolean myTestSource;
        private final String mySdkPath;
        private final File myGeneratedFile;

        public IdlGenerationItem(Module module, VirtualFile file, boolean testSource, String sdkPath) {
            myModule = module;
            myFile = file;
            myTestSource = testSource;
            mySdkPath = sdkPath;
            myGeneratedFile = new File(VfsUtil.virtualToIoFile(file.getParent()), file.getNameWithoutExtension() + ".java");
        }

        public VirtualFile getFile() {
            return myFile;
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
            return myTestSource;
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
            ProjectFileIndex fileIndex = ProjectRootManager.getInstance(myProject).getFileIndex();
            CompileScope compileScope = myContext.getCompileScope();
            VirtualFile[] files = compileScope.getFiles(AndroidIdlFileType.ourFileType, false);
            if (files != null) {
                List<GenerationItem> items = new ArrayList<GenerationItem>(files.length);
                for (VirtualFile file : files) {
                    Module module = myContext.getModuleByFile(file);
                    AndroidFacet facet = FacetManager.getInstance(module).getFacetByType(AndroidFacet.ID);
                    if (facet != null) {
                        String sdkPath = facet.getConfiguration().getSdkPath();
                        IdlGenerationItem generationItem = new IdlGenerationItem(module, file, fileIndex.isInTestSourceContent(file), sdkPath);
                        if (myContext.isMake()) {
                            File generatedFile = generationItem.getGeneratedFile();
                            if (generatedFile == null || !generatedFile.exists() || generatedFile.lastModified() <= file.getModificationStamp()) {
                                items.add(generationItem);
                            }
                        } else {
                            items.add(generationItem);
                        }
                    }
                }
                return items.toArray(new GenerationItem[items.size()]);
            }
            return EMPTY_GENERATION_ITEM_ARRAY;
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
                if (item instanceof IdlGenerationItem) {
                    IdlGenerationItem idlItem = (IdlGenerationItem) item;
                    try {
                        Map<CompilerMessageCategory, List<String>> messages = AndroidIdl.execute(
                                idlItem.getSdkPath(),
                                idlItem.getFile().getPath()
                        );
                        addMessages(messages, idlItem.getFile().getUrl());
                        if (messages.get(CompilerMessageCategory.ERROR).isEmpty()) {
                            results.add(idlItem);
                        }
                    } catch (IOException e) {
                        myContext.addMessage(CompilerMessageCategory.ERROR, e.getMessage(), idlItem.getFile().getUrl(), -1, -1);
                    }
                }
            }
            return results.toArray(new GenerationItem[results.size()]);
        }

        private void addMessages(Map<CompilerMessageCategory, List<String>> messages, String url) {
            for (CompilerMessageCategory category : messages.keySet()) {
                List<String> messageList = messages.get(category);
                for (String message : messageList) {
                    myContext.addMessage(category, message, url, -1, -1);
                }
            }
        }
    }
}