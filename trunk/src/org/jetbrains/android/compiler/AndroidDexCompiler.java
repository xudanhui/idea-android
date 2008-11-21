package org.jetbrains.android.compiler;

import com.intellij.facet.FacetManager;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.*;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.CompilerModuleExtension;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.android.compiler.tools.AndroidDx;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.android.facet.AndroidFacetConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Android Dex compiler.
 *
 * @author Alexey Efimov
 */
public class AndroidDexCompiler implements ClassPostProcessingCompiler {
    @NotNull
    public ProcessingItem[] getProcessingItems(CompileContext context) {
        Module[] affectedModules = context.getCompileScope().getAffectedModules();
        if (affectedModules != null && affectedModules.length > 0) {
            Application application = ApplicationManager.getApplication();
            return application.runReadAction(new PrepareAction(context));
        }
        return ProcessingItem.EMPTY_ARRAY;
    }

    public ProcessingItem[] process(CompileContext context, ProcessingItem[] items) {
        if (items != null && items.length > 0) {
            Application application = ApplicationManager.getApplication();
            return application.runReadAction(new ProcessAction(context, items));
        }
        return ProcessingItem.EMPTY_ARRAY;
    }

    @NotNull
    public String getDescription() {
        return AndroidDx.TOOL;
    }

    public boolean validateConfiguration(CompileScope scope) {
        return true;
    }

    public ValidityState createValidityState(DataInput is) throws IOException {
        return null;
    }

    private final class PrepareAction implements Computable<ProcessingItem[]> {
        private final CompileContext myContext;

        public PrepareAction(CompileContext context) {
            myContext = context;
        }

        public ProcessingItem[] compute() {
            CompileScope compileScope = myContext.getCompileScope();
            Module[] modules = compileScope.getAffectedModules();
            List<ProcessingItem> items = new ArrayList<ProcessingItem>();
            for (Module module : modules) {
                AndroidFacet facet = FacetManager.getInstance(module).getFacetByType(AndroidFacet.ID);
                if (facet != null) {
                    CompilerModuleExtension extension = CompilerModuleExtension.getInstance(module);
                    VirtualFile outputPath = extension.getCompilerOutputPath();
                    AndroidFacetConfiguration configuration = facet.getConfiguration();
                    items.add(new DexItem(module, outputPath, configuration.getSdkPath()));
                }
            }
            return items.toArray(new ProcessingItem[items.size()]);
        }
    }

    private final static class ProcessAction implements Computable<ProcessingItem[]> {
        private final CompileContext myContext;
        private final ProcessingItem[] myItems;

        public ProcessAction(CompileContext context, ProcessingItem[] items) {
            myContext = context;
            myItems = items;
        }

        public ProcessingItem[] compute() {
            List<ProcessingItem> results = new ArrayList<ProcessingItem>(myItems.length);
            for (ProcessingItem item : myItems) {
                if (item instanceof DexItem) {
                    DexItem dexItem = (DexItem) item;
                    try {
                        Map<CompilerMessageCategory, List<String>> messages = AndroidDx.dex(
                                dexItem.getSdkPath(),
                                dexItem.getFile().getPath()
                        );
                        addMessages(messages);
                        if (messages.get(CompilerMessageCategory.ERROR).isEmpty()) {
                            results.add(dexItem);
                        }
                    } catch (IOException e) {
                        myContext.addMessage(CompilerMessageCategory.ERROR, e.getMessage(), null, -1, -1);
                    }
                }
            }
            return results.toArray(new ProcessingItem[results.size()]);
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

    private final static class DexItem implements ProcessingItem {
        private final Module myModule;
        private final VirtualFile myOutputPath;
        private final String mySdkPath;

        public DexItem(Module module, VirtualFile outputPath, String sdkPath) {
            myModule = module;
            myOutputPath = outputPath;
            mySdkPath = sdkPath;
        }

        @NotNull
        public VirtualFile getFile() {
            return myOutputPath;
        }

        @Nullable
        public ValidityState getValidityState() {
            return null;
        }

        public String getSdkPath() {
            return mySdkPath;
        }

        public Module getModule() {
            return myModule;
        }
    }
}
