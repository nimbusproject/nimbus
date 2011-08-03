/*
 * Copyright 1999-2008 University of Chicago
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.globus.workspace.service.impls.async;

import java.util.ArrayList;
import java.util.List;

// TODO: move to executor
public class WorkspaceThreadPool {

    private final List workThreads = new ArrayList<WorkspaceThread>();
    private final WorkspaceRequestQueue queue;

    public WorkspaceThreadPool(WorkspaceRequestQueue queue) {
        this.queue = queue;
    }

    public void startThreads(int threads) {
        for (int i = 0; i < threads; i++) {
            WorkspaceThread thread = createThread();
            thread.setName(thread.getName() + "_WorkspTaskThrd");
            this.workThreads.add(thread);
            thread.start();
        }
    }

    public int getThreads() {
        return this.workThreads.size();
    }

    protected WorkspaceThread createThread() {
        return new WorkspaceThread(this.queue, this);
    }

    public void stopThreads(int numThreads) {
        this.queue.stopThreads(numThreads);
    }

    public void stopThreads() {
        this.queue.stopThreads(this.workThreads.size());
    }

    public void removeThread(WorkspaceThread thread) {
        this.workThreads.remove(thread);
        synchronized (this) {
            notifyAll();
        }
    }

    public synchronized void waitForThreads()
        throws InterruptedException {
        while (!isDone()) {
            wait();
        }
    }

    public synchronized void waitForThreads(int timeout)
        throws InterruptedException {
        while (timeout > 0 && !isDone()) {
            wait(5000);
            timeout -= 5000;
        }
    }

    private boolean isDone() {
        int size = workThreads.size();
        return ((size == 0) ||
                (size == 1 &&
                        workThreads.contains(Thread.currentThread())));
    }
}
