package org.jetbrains.android.compiler.tools;

import com.intellij.openapi.compiler.CompilerMessageCategory;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.android.AndroidManager;

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
                buildToolPath(sdkPath),
                "compile",
                "-m",
                "-J",
                outDir,
                "-M",
                buildManifestPath(rootDirPath),
                "-S", resourceDir,
                "-I", buildJarPath(sdkPath)
        );
    }

    @NotNull
    public static Map<CompilerMessageCategory, List<String>> packageResources(String rootDirPath,
                                                                              String sdkPath,
                                                                              String resourceDir,
                                                                              String outputPath) throws IOException {
        return ExternalCompilerTool.execute(
                buildToolPath(sdkPath),
                "package",
                "-f",     // force overwrite of existing files
                "-c",     // compile resources from assets
                "-M", buildManifestPath(rootDirPath),
                "-S", resourceDir,
                "-I", buildJarPath(sdkPath),
                outputPath);
    }

    private static String buildToolPath(String sdkPath) {
        return sdkPath + File.separator + "tools" + File.separator + TOOL;
    }

    private static String buildManifestPath(String rootDirPath) {
        return rootDirPath + File.separator + AndroidManager.MANIFEST_FILE_NAME;
    }

    private static String buildJarPath(String sdkPath) {
        return sdkPath + File.separator + "android.jar";
    }

}
