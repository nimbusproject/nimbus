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

package org.globus.workspace.client_core.subscribe_tools.internal;

import org.globus.workspace.client_core.subscribe_tools.SubscriptionMaster;
import org.globus.workspace.client_core.subscribe_tools.StateChangeListener;
import org.globus.workspace.client_core.repr.Workspace;
import org.globus.workspace.client_core.repr.State;
import edu.emory.mathcs.backport.java.util.concurrent.Callable;

public class DeliverStateChangeCallbackTask implements Callable {

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private final SubscriptionMaster m;
    private final Workspace w;
    private final StateChangeListener l;
    private final State s;

    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    /**
     * @param master may not be null
     * @param workspace may not be null
     * @param listener may not be null
     * @param newState may not be null
     */
    public DeliverStateChangeCallbackTask(SubscriptionMaster master,
                                          Workspace workspace,
                                          StateChangeListener listener,
                                          State newState) {
        if (master == null) {
            throw new IllegalArgumentException("master may not be null");
        }
        this.m = master;

        if (workspace == null) {
            throw new IllegalArgumentException("workspace may not be null");
        }
        this.w = workspace;

        if (listener == null) {
            throw new IllegalArgumentException("listener may not be null");
        }
        this.l = listener;

        if (newState == null) {
            throw new IllegalArgumentException("newState may not be null");
        }
        this.s = newState;
    }

    // -------------------------------------------------------------------------
    // implements Callable
    // -------------------------------------------------------------------------

    /**
     * Run a StateChangeListener callback
     * @return always null
     * @throws Exception uncaught problem
     */
    public Object call() throws Exception {
        this.l.newState(this.m, this.w, this.s);
        return null;
    }
}
