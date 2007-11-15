package org.jetbrains.android.compiler.tools;

import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.util.SystemInfo;
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
    public static Map<CompilerMessageCategory, List<String>> dex(String sdkPath, String classesDir) throws IOException {
        return ExternalCompilerTool.execute(
                sdkPath + File.separator + "tools" + File.separator + TOOL,
                SystemInfo.isWindows ? "" : "-JXmx384M",
                "--dex",
                "--output=" + ExternalCompilerTool.quote(classesDir + File.separatorChar + "classes.dex"),
                "--locals=full",
                "--positions=lines",
                classesDir
        );
    }
}