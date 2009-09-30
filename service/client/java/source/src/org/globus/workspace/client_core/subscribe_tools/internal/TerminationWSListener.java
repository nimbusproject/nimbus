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

import org.globus.wsrf.NotifyCallback;
import org.globus.workspace.common.print.Print;
import org.globus.workspace.client_core.subscribe_tools.TerminationConduit;
import org.apache.axis.message.addressing.EndpointReferenceType;

import java.util.List;

/**
 * @see SubscriptionMasterImpl
 */
public class TerminationWSListener implements NotifyCallback {

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private final TerminationConduit conduit;

    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    /**
     * @see SubscriptionMasterImpl
     * 
     * @param terminationConduit may not be null
     * @param print may not be null
     */
    TerminationWSListener(TerminationConduit terminationConduit,
                          Print print) {
        
        if (terminationConduit == null) {
            throw new IllegalArgumentException(
                    "terminationConduit may not be null");
        }

        this.conduit = terminationConduit;

        if (print == null) {
            throw new IllegalArgumentException(
                    "print may not be null, use disabled impl instead");
        }
        // will probably need print later
    }

    // -------------------------------------------------------------------------
    // implements NotifyCallback
    // -------------------------------------------------------------------------

    public void deliver(List topicPath,
                        EndpointReferenceType producer,
                        Object message) {

        this.conduit.terminationOccured(producer);
    }
}
