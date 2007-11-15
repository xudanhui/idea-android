package org.jetbrains.android.compiler.tools;

import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.android.util.AndroidBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Abstract external tool for compiler.
 *
 * @author Alexey Efimov
 */
public final class ExternalCompilerTool {
    private ExternalCompilerTool() {
    }

    @NonNls
    private static final String COMMAND_COM = "command.com /C ";
    @NonNls
    private static final String CMD_EXE = "cmd.exe /C ";

    @NotNull
    protected static Map<CompilerMessageCategory, List<String>> execute(String... argv) throws IOException {
        return performCommand(formatCommand(argv));
    }

    @NotNull
    protected static Map<CompilerMessageCategory, List<String>> performCommand(String command) throws IOException {
        Process process = Runtime.getRuntime().exec(command);
        try {
            ProcessResult result = readProcessOutput(process);
            Map<CompilerMessageCategory, List<String>> messages = result.getMessages();
            int code = result.getExitCode();
            if (code != 0 && messages.get(CompilerMessageCategory.ERROR).isEmpty()) {
                throw new IOException(AndroidBundle.message("command.0.execution.failed.with.exit.code.1", command, code));
            } else {
                return messages;
            }
        } finally {
            process.destroy();
        }
    }

    @NotNull
    private static ProcessResult readProcessOutput(Process process) throws IOException {
        InputStream out = process.getInputStream();
        try {
            InputStream err = process.getErrorStream();
            try {
                List<String> information = new ArrayList<String>();
                List<String> error = new ArrayList<String>();
                filter(StreamUtil.readText(out), information);
                filter(StreamUtil.readText(err), error);
                Map<CompilerMessageCategory, List<String>> messages = new HashMap<CompilerMessageCategory, List<String>>();
                messages.put(CompilerMessageCategory.ERROR, error);
                messages.put(CompilerMessageCategory.INFORMATION, information);
                int code = process.waitFor();
                return new ProcessResult(messages, code);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                err.close();
            }
        } finally {
            out.close();
        }
    }

    @NotNull
    protected static String formatCommand(String... argv) {
        StringBuilder command = new StringBuilder(SystemInfo.isWindows ? SystemInfo.isWindows9x ? COMMAND_COM : CMD_EXE : "");
        for (String arg : argv) {
            if (StringUtil.isNotEmpty(arg)) {
                if (command.length() > 0) {
                    command.append(' ');
                }
                command.append(quote(arg));
            }
        }
        return command.toString();
    }

    public static String quote(String arg) {
        if (arg.indexOf(' ') != -1) {
            arg = String.format("\"%s\"", arg);
        }
        return arg;
    }

    private static void filter(@NonNls String output, @NotNull List<String> buffer) {
        if (!StringUtil.isEmptyOrSpaces(output)) {
            String[] lines = output.split("[\\n\\r]+");
            buffer.addAll(Arrays.asList(lines));
        }
    }

    private static final class ProcessResult {
        private final Map<CompilerMessageCategory, List<String>> messages;
        private final int exitCode;

        private ProcessResult(Map<CompilerMessageCategory, List<String>> messages, int exitCode) {
            this.messages = messages;
            this.exitCode = exitCode;
        }

        public Map<CompilerMessageCategory, List<String>> getMessages() {
            return messages;
        }

        public int getExitCode() {
            return exitCode;
        }
    }
}
