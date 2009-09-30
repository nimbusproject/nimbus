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

import java.util.LinkedList;

// TODO: move to executor
public class WorkspaceRequestQueue {

    private final LinkedList requests = new LinkedList();
    int waitingThreads;
    private final Semaphore semaphore = new Semaphore();

    public WorkspaceRequest dequeue() throws InterruptedException {
        addWaitingThread();
        semaphore.waitForSignal();
        removeWaitingThread();

        Object request;
        synchronized (this) {
            request = this.requests.removeFirst();
        }

        return (WorkspaceRequest) request;
    }

    private synchronized void addWaitingThread() {
        this.waitingThreads++;
    }

    private synchronized void removeWaitingThread() {
        this.waitingThreads--;
    }

    public synchronized int enqueue(WorkspaceRequest request) {
        this.requests.addLast(request);
        semaphore.sendSignal();
        return this.waitingThreads;
    }

    public synchronized void stopThreads(int threads) {
        for (int i = 0; i < threads; i++) {
            this.requests.addFirst(null);
            semaphore.sendSignal();
        }
    }
}
