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

package org.globus.workspace.client_core.utils;

import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;
import edu.emory.mathcs.backport.java.util.concurrent.Callable;

public class LatchWaiter implements Callable {
    
    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private final CountDownLatch done;
    private final CountDownLatch waitFor;

    
    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    /**
     * @param waitForThisLatch latch to call await on
     * @param doneLatch latch to call countDown on when <code>waitForThisLatch</code>
     *                  is successfully awaited on 
     */
    public LatchWaiter(CountDownLatch waitForThisLatch,
                       CountDownLatch doneLatch) {

        if (doneLatch == null) {
            throw new IllegalArgumentException(
                    "doneLatch may not be null");
        }

        if (waitForThisLatch == null) {
            throw new IllegalArgumentException(
                    "waitForThisLatch may not be null");
        }

        this.done = doneLatch;
        this.waitFor = waitForThisLatch;
    }

    
    // -------------------------------------------------------------------------
    // implements Callable
    // -------------------------------------------------------------------------

    /**
     * Run <code>#waitForZero()</code>
     * @return always null
     * @throws Exception uncaught problem
     */
    public Object call() throws Exception {
        this.waitForZero();
        return null;
    }


    // -------------------------------------------------------------------------
    // WAIT
    // -------------------------------------------------------------------------

    /**
     * Await waitForThisLatch and then countdown on given doneLatch
     * 
     * @throws InterruptedException another thread interrupted this thread
     * @see CountDownLatch
     */
    public void waitForZero() throws InterruptedException {
        this.waitFor.await();
        this.countDownExitLatch();
    }

    /**
     * @see #waitForZero()
     */
    protected void countDownExitLatch() {
        this.done.countDown();
    }
}
