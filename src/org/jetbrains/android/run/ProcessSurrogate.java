package org.jetbrains.android.run;

import java.io.OutputStream;
import java.io.InputStream;

/**
 * @author coyote
 */
public class ProcessSurrogate extends Process {
    private final Process process;
    private boolean unbended = false;

    public ProcessSurrogate(Process process) {
        this.process = process;
    }

    public OutputStream getOutputStream() {
        return process.getOutputStream();
    }

    public InputStream getInputStream() {
        return process.getInputStream();
    }

    public InputStream getErrorStream() {
        return process.getErrorStream();
    }

    private void waitForProcessTerminated() {
        try {
            process.waitFor();
            unbended = true;
        } catch (InterruptedException e) {
        }
        synchronized (this) {
            notifyAll();
        }
    }

    public int waitFor() throws InterruptedException {
        Thread thread = new Thread(new Runnable() {
            public void run() {
                waitForProcessTerminated();
            }
        });
        thread.start();
        synchronized (this) {
            while (!unbended) {
                wait();
            }
        }
        return 0;
    }

    public synchronized void unbend() {
        unbended = true;
        notifyAll();
    }

    public int exitValue() {
        return process.exitValue();
    }

    public void destroy() {
        if (!unbended) process.destroy();
    }
}
