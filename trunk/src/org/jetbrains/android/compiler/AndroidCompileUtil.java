package org.jetbrains.android.compiler;

import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompilerMessageCategory;

import java.util.List;
import java.util.Map;

/**
 * @author yole
 */
public class AndroidCompileUtil {
    static void addMessages(CompileContext context, Map<CompilerMessageCategory, List<String>> messages) {
        for (CompilerMessageCategory category : messages.keySet()) {
            List<String> messageList = messages.get(category);
            for (String message : messageList) {
                context.addMessage(category, message, null, -1, -1);
            }
        }
    }
}
