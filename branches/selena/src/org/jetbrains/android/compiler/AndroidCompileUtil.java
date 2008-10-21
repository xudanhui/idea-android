package org.jetbrains.android.compiler;

import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompilerMessageCategory;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author yole
 */
public class AndroidCompileUtil {
    private static Pattern ourMessagePattern = Pattern.compile("(.+):(\\d+):.+");

    static void addMessages(CompileContext context, Map<CompilerMessageCategory, List<String>> messages) {
        for (CompilerMessageCategory category : messages.keySet()) {
            List<String> messageList = messages.get(category);
            for (String message : messageList) {
                String url = null;
                int line = -1;
                Matcher matcher = ourMessagePattern.matcher(message);
                if (matcher.matches()) {
                    String fileName = matcher.group(1);
                    if (new File(fileName).exists()) {
                        url = "file://" + fileName;
                        line = Integer.parseInt(matcher.group(2));
                    }
                }
                context.addMessage(category, message, url, line, -1);
            }
        }
    }
}
