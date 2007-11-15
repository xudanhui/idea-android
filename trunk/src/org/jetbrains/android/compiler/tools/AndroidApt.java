package org.jetbrains.android.compiler.tools;

import com.intellij.openapi.compiler.CompilerMessageCategory;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * AndroidApt decorator.
 *
 * @author Alexey Efimov
 */
public final class AndroidApt {
    @NonNls
    public static final String TOOL = "aapt";

    private AndroidApt() {
    }

    @NotNull
    public static Map<CompilerMessageCategory, List<String>> compile(String rootDirPath, String outDir, String resourceDir, String sdkPath) throws IOException {
        return ExternalCompilerTool.execute(
                sdkPath + File.separator + "tools" + File.separator + TOOL,
                "compile",
                "-m",
                "-J",
                outDir,
                "-M",
                rootDirPath + File.separator + "AndroidManifest.xml",
                "-S", resourceDir,
                "-I", sdkPath + File.separator + "android.jar"
        );
    }

}
