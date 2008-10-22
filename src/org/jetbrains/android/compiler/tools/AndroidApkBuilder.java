package org.jetbrains.android.compiler.tools;

import com.intellij.openapi.compiler.CompilerMessageCategory;
import org.jetbrains.annotations.NonNls;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.util.ArrayList;

/**
 * @author yole
 */
public class AndroidApkBuilder {
    @NonNls
    public static final String TOOL = "apkbuilder";

    public static Map<CompilerMessageCategory, List<String>> execute(String sdkPath,
                                                                     String apkPath,
                                                                     String dexPath,
                                                                     String outputPath) throws IOException {
        final Map<CompilerMessageCategory, List<String>> messages = ExternalCompilerTool.execute(buildToolPath(sdkPath),
                outputPath,
                "-z", apkPath,
                "-f", dexPath);
        return filterUsingKeystoreMessages(messages);
    }

    private static Map<CompilerMessageCategory, List<String>> filterUsingKeystoreMessages(Map<CompilerMessageCategory, List<String>> messages) {
        List<String> infoMessages = messages.get(CompilerMessageCategory.INFORMATION);
        if (infoMessages == null) {
            infoMessages = new ArrayList<String>();
            messages.put(CompilerMessageCategory.INFORMATION, infoMessages);
        }
        final List<String> errors = messages.get(CompilerMessageCategory.ERROR);
        for (Iterator<String> iterator = errors.iterator(); iterator.hasNext();) {
            String s = iterator.next();
            if (s.startsWith("Using keystore:")) {
                // not actually an error
                infoMessages.add(s);
                iterator.remove();
            }
        }
        return messages;
    }

    private static String buildToolPath(String sdkPath) {
        return sdkPath + File.separator + "tools" + File.separator + TOOL;
    }
}
