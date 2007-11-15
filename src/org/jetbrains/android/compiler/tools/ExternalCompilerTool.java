package org.jetbrains.android.compiler.tools;

import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.android.util.AndroidBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;

/**
 * Abstract external tool for compiler.
 *
 * @author Alexey Efimov
 */
public final class ExternalCompilerTool {
    @NonNls
    private static final String[] COMMAND_COM = {"command.com", "/C"};
    @NonNls
    private static final String[] CMD_EXE = {"cmd.exe", "/C"};

    private ExternalCompilerTool() {
    }

    @NotNull
    protected static Map<CompilerMessageCategory, List<String>> execute(String... argv) throws IOException {
        return performCommand(toPlatformDependedCommand(argv));
    }

    private static String[] toPlatformDependedCommand(String... argv) {
        if (SystemInfo.isWindows) {
            List<String> command = new ArrayList<String>();
            command.addAll(Arrays.asList(SystemInfo.isWindows9x ? COMMAND_COM : CMD_EXE));
            command.addAll(Arrays.asList(argv));
            return command.toArray(new String[command.size()]);
        }
        return argv;
    }

    @NotNull
    protected static Map<CompilerMessageCategory, List<String>> performCommand(String... command) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(command);
        ProcessResult result = readProcessOutput(builder.start());
        Map<CompilerMessageCategory, List<String>> messages = result.getMessages();
        int code = result.getExitCode();
        if (code != 0 && messages.get(CompilerMessageCategory.ERROR).isEmpty()) {
            throw new IOException(AndroidBundle.message("command.0.execution.failed.with.exit.code.1", command, code));
        } else {
            return messages;
        }
    }

    @NotNull
    private static ProcessResult readProcessOutput(Process process) throws IOException {
        OSProcessHandler handler = new OSProcessHandler(process, "");
        final List<String> information = new ArrayList<String>();
        final List<String> error = new ArrayList<String>();
        handler.addProcessListener(new ProcessAdapter() {
            public void onTextAvailable(final ProcessEvent event, final Key outputType) {
                if (outputType == ProcessOutputTypes.STDOUT) {
                    filter(event.getText(), information);
                } else if (outputType == ProcessOutputTypes.STDERR) {
                    filter(event.getText(), error);
                }
            }
        });
        handler.startNotify();
        handler.waitFor();
        int exitCode = handler.getProcess().exitValue();
        return new ProcessResult(information, error, exitCode);
    }

    private static void filter(@NonNls String output, @NotNull List<String> buffer) {
        if (!StringUtil.isEmptyOrSpaces(output)) {
            String[] lines = output.split("[\\n\\r]+");
            buffer.addAll(Arrays.asList(lines));
        }
    }

    private static final class ProcessResult {
        private final int myExitCode;
        private final Map<CompilerMessageCategory, List<String>> myMessages;

        public ProcessResult(List<String> information, List<String> error, int exitCode) {
            myExitCode = exitCode;
            myMessages = new HashMap<CompilerMessageCategory, List<String>>(2);
            myMessages.put(CompilerMessageCategory.INFORMATION, information);
            myMessages.put(CompilerMessageCategory.ERROR, error);
        }

        public Map<CompilerMessageCategory, List<String>> getMessages() {
            return myMessages;
        }

        public int getExitCode() {
            return myExitCode;
        }
    }
}
