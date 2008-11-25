package org.jetbrains.android.run;

import com.intellij.debugger.ui.DebuggerPanelsManager;
import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.*;
import com.intellij.execution.process.OSProcessHandler;
import static com.intellij.execution.process.ProcessOutputTypes.STDERR;
import com.intellij.execution.runners.DefaultProgramRunner;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * @author coyote
 */
public class AndroidDebugRunner extends DefaultProgramRunner {
    private static class DebugState implements RemoteState {
        private final AndroidRunningState runState;
        private final RunContentDescriptor descriptor;
        private final String debugPort;

        private DebugState(AndroidRunningState runState, String debugPort, RunContentDescriptor descriptor) {
            this.runState = runState;
            this.debugPort = debugPort;
            this.descriptor = descriptor;
        }

        protected OSProcessHandler startProcess() throws ExecutionException {
            return runState.getProcessHandler();
        }

        public RemoteConnection getRemoteConnection() {
            return new RemoteConnection(true, "localhost", debugPort, false);
        }

        public ExecutionResult execute(Executor executor, @NotNull ProgramRunner runner) throws ExecutionException {
            return new DefaultExecutionResult(descriptor.getExecutionConsole(), descriptor.getProcessHandler());
        }

        public RunnerSettings getRunnerSettings() {
            return runState.getRunnerSettings();
        }

        public ConfigurationPerRunnerSettings getConfigurationSettings() {
            return runState.getConfigurationSettings();
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
        final JComponent component = new JPanel(new BorderLayout(1, 1));
        if (runDescriptor == null) {
            throw new ExecutionException("Can't run an application");
        }
        component.add(runDescriptor.getComponent());
        runState.setDebugLauncher(new DebugLauncher() {
            public void launchDebug(final String debugPort) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        final DebuggerPanelsManager manager = DebuggerPanelsManager.getInstance(project);
                        final DebugState st = new DebugState(runState, debugPort, runDescriptor);
                        try {
                            final RunContentDescriptor debugDescriptor = manager.attachVirtualMachine(executor, AndroidDebugRunner.this,
                                    environment, st, runDescriptor, st.getRemoteConnection(), false);
                            if (debugDescriptor == null) {
                                runState.getProcessHandler().notifyTextAvailable("Can't start debugging.", STDERR);
                                return;
                            }
                            final JComponent newComponent = debugDescriptor.getComponent();
                            component.removeAll();
                            component.add(newComponent);
                        } catch (ExecutionException e) {
                            runState.getProcessHandler().notifyTextAvailable("ExecutionException: " + e.getMessage() + '.', STDERR);
                        }
                    }
                });
            }
        });
        return new RunContentDescriptor(runDescriptor.getExecutionConsole(), runDescriptor.getProcessHandler(), component,
                runDescriptor.getDisplayName());
    }

    @NotNull
    public String getRunnerId() {
        return "runner";
    }

    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        return true;
    }
}
