package org.jetbrains.android.compiler.tools;

import com.intellij.openapi.compiler.CompilerMessageCategory;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Dx tool.
 *
 * @author Alexey Efimov
 */
public final class AndroidDx {
    @NonNls
    public static final String TOOL = "dx";

    private AndroidDx() {
    }

    @NotNull
    public static Map<CompilerMessageCategory, List<String>> dex(String sdkPath, String outputFilePath, String classesDir) throws IOException {
        return ExternalCompilerTool.execute(
                sdkPath + File.separator + "tools" + File.separator + TOOL,
                "-JXmx384M",
                "--dex",
                "--output=" + ExternalCompilerTool.quote(outputFilePath),
                "--locals=full",
                classesDir
        );
    }
}