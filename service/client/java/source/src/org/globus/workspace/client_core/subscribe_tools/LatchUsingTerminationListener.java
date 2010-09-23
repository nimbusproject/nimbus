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

package org.globus.workspace.client_core.subscribe_tools;

import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;
import org.globus.workspace.common.print.Print;
import org.globus.workspace.client_core.repr.Workspace;

public class LatchUsingTerminationListener extends GenericTerminationListener {

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private final CountDownLatch latch;


    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    /**
     * No printing possible
     * @param terminationLatch may not be null
     */
    public LatchUsingTerminationListener(CountDownLatch terminationLatch) {
        super();
        this.latch = terminationLatch;
        this.checkNulls();
    }

    /**
     * @param print may be null (will be disabled)
     * @param terminationLatch may not be null
     */
    public LatchUsingTerminationListener(Print print,
                                         CountDownLatch terminationLatch) {
        super(print);
        this.latch = terminationLatch;
        this.checkNulls();
    }

    private void checkNulls() {
        if (this.latch == null) {
            throw new IllegalArgumentException(
                    "terminationLatch may not be null");
        }
    }

    // -------------------------------------------------------------------------
    // overrides GenericTerminationListener
    // -------------------------------------------------------------------------

    /**
     * hook for children (one can also override terminationOccured entirely)
     *
     * @param workspace never going to be null
     */
    protected void theTerminationOccured(Workspace workspace) {
        this.latch.countDown();
    }
}
