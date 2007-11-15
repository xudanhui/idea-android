package org.jetbrains.android.compiler.tools;

import com.intellij.openapi.compiler.CompilerMessageCategory;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * IDL compiler.
 *
 * @author Alexey Efimov
 */
public final class AndroidIdl {
    @NonNls
    public static final String TOOL = "aidl";

    private AndroidIdl() {
    }

    @NotNull
    public static Map<CompilerMessageCategory, List<String>> execute(String sdkPath, String file) throws IOException {
        return ExternalCompilerTool.execute(
                sdkPath + File.separator + "tools" + File.separator + TOOL,
                file
        );
    }
}
