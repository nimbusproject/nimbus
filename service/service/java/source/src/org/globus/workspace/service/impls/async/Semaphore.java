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

/**
 * Copy of org.globus.wsrf.container.Semaphore
 *
 * todo: use another thread pool architecture
 */
public class Semaphore {

    private int semaphore;

    public Semaphore() {
        this(0);
    }

    public Semaphore(int initialValue) {
        this.semaphore = initialValue;
    }

    public void acquire() throws InterruptedException {
        waitForSignal();
    }

    public synchronized void waitForSignal() throws InterruptedException {
        if (this.semaphore > 0) {
            this.semaphore--;
            return;
        }

        while (this.semaphore < 1) {
            wait();
        }

        this.semaphore--;
    }

    public void release() {
        sendSignal();
    }

    public synchronized void sendSignal() {
        this.semaphore++;
        notify();
    }

}
