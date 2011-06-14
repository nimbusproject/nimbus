package org.globus.workspace.testing.suites.spotinstances;

import org.globus.workspace.xen.xenssh.MockShutdownTrash;

import java.util.concurrent.Callable;

public class DestroyEnableFutureTask  implements Callable {

    private final int secondsToWait;

    public DestroyEnableFutureTask(int secondsToWait) {
        if (secondsToWait < 1) {
            throw new IllegalArgumentException("secondsToWait is too low");
        }
        this.secondsToWait = secondsToWait;
    }

    public Object call() throws Exception {
        Thread.sleep(secondsToWait * 1000);
        MockShutdownTrash.setFail(false);
        return null;
    }
}
