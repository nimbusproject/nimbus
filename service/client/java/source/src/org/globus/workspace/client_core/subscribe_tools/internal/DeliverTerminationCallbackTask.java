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

import edu.emory.mathcs.backport.java.util.concurrent.Callable;
import org.globus.workspace.client_core.subscribe_tools.TerminationListener;
import org.globus.workspace.client_core.subscribe_tools.SubscriptionMaster;
import org.globus.workspace.client_core.repr.Workspace;

public class DeliverTerminationCallbackTask implements Callable {

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private final SubscriptionMaster m;
    private final Workspace w;
    private final TerminationListener l;

    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    /**
     * @param subscriptionMaster may not be null
     * @param workspace may not be null
     * @param listener may not be null
     */
    public DeliverTerminationCallbackTask(SubscriptionMaster subscriptionMaster,
                           Workspace workspace,
                           TerminationListener listener) {
        if (subscriptionMaster == null) {
            throw new IllegalArgumentException("subscriptionMaster is null");
        }
        this.m = subscriptionMaster;

        if (workspace == null) {
            throw new IllegalArgumentException("workspace is null");
        }
        this.w = workspace;

        if (listener == null) {
            throw new IllegalArgumentException("listener is null");
        }
        this.l = listener;
    }

    // -------------------------------------------------------------------------
    // implements Callable
    // -------------------------------------------------------------------------

    /**
     * Run a TerminationListener callback
     * @return always null
     * @throws Exception uncaught problem
     */
    public Object call() throws Exception {
        this.l.terminationOccured(this.m, this.w);
        return null;
    }
}
