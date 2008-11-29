package org.jetbrains.android.run;

import com.intellij.debugger.ui.DebuggerPanelsManager;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.*;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.process.OSProcessHandler;
import static com.intellij.execution.process.ProcessOutputTypes.STDERR;
import com.intellij.execution.runners.DefaultProgramRunner;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.execution.ui.RunContentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author coyote
 */
public class AndroidDebugRunner extends DefaultProgramRunner {
    private static class DebugState extends CommandLineState implements RemoteState {
        private final AndroidRunningState runState;
        private final Project project;
        private final String debugPort;

        private DebugState(ExecutionEnvironment env, AndroidRunningState runState, String debugPort, Project project) {
            super(env);
            this.runState = runState;
            this.debugPort = debugPort;
            this.project = project;
        }

        public RemoteConnection getRemoteConnection() {
            return new RemoteConnection(true, "localhost", debugPort, false);
        }

        protected OSProcessHandler startProcess() throws ExecutionException {
            setConsoleBuilder(TextConsoleBuilderFactory.getInstance().createBuilder(project));
            return new OSProcessHandler(runState.getEmulatorProcess(), "");
        }
    }

    @Override
    protected RunContentDescriptor doExecute(final Project project, final Executor executor, final RunProfileState state,
                                             final RunContentDescriptor contentToReuse, final ExecutionEnvironment environment)
            throws ExecutionException {
        if (!(state instanceof AndroidRunningState)) {
            throw new ExecutionException("Incorrect RunProfileState");
        }
        final AndroidRunningState runState = (AndroidRunningState) state;
        runState.setDebugMode(true);
        final RunContentDescriptor runDescriptor = super.doExecute(project, executor, state, contentToReuse, environment);
        if (runDescriptor == null) {
            throw new ExecutionException("Can't run an application");
        }
        runState.setDebugLauncher(new DebugLauncher() {
            public void launchDebug(final String debugPort) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        final DebuggerPanelsManager manager = DebuggerPanelsManager.getInstance(project);
                        final DebugState st = new DebugState(environment, runState, debugPort, project);
                        try {
                            runState.getProcessSurrogate().unbend();
                            final RunContentDescriptor debugDescriptor = manager.attachVirtualMachine(executor, AndroidDebugRunner.this,
                                    environment, st, contentToReuse, st.getRemoteConnection(), false);
                            if (debugDescriptor == null) {
                                runState.notifyTextAvailable("Can't start debugging.", STDERR);
                                return;
                            }
                            ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
                            ContentManager contentManager = toolWindowManager.getToolWindow(executor.getToolWindowId()).getContentManager();
                            Content content = contentManager.getContent(runDescriptor.getComponent());
                            debugDescriptor.setAttachedContent(content);
                            RunContentManager runContentManager = ExecutionManager.getInstance(project).getContentManager();
                            runContentManager.showRunContent(executor, debugDescriptor);
                            debugDescriptor.getProcessHandler().startNotify();
                        } catch (ExecutionException e) {
                            runState.notifyTextAvailable("ExecutionException: " + e.getMessage() + '.', STDERR);
                        }
                    }
                });
            }
        });
        return runDescriptor;
    }

    @NotNull
    public String getRunnerId() {
        return "AndroidDebugRunner";
    }

    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        return true;
    }
}
