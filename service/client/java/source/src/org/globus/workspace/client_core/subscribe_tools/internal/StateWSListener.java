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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.axis.message.addressing.EndpointReferenceType;
import org.apache.axis.message.MessageElement;
import org.globus.workspace.common.print.Print;
import org.globus.workspace.client_core.print.PrCodes;
import org.globus.workspace.client_core.repr.State;
import org.globus.workspace.client_core.utils.EPRUtils;
import org.globus.workspace.client_core.utils.StateUtils;
import org.globus.workspace.client_core.subscribe_tools.StateChangeConduit;
import org.globus.wsrf.NotifyCallback;
import org.globus.wsrf.core.notification.ResourcePropertyValueChangeNotificationElementType;

import java.util.List;

public class StateWSListener implements NotifyCallback {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(StateWSListener.class.getName());

    
    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private final StateChangeConduit conduit;
    private final Print pr;

    
    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    /**
     * @see SubscriptionMasterImpl
     * 
     * @param stateChangeConduit may not be null
     * @param print may not be null
     */
    StateWSListener(StateChangeConduit stateChangeConduit,
                    Print print) {
        
        if (stateChangeConduit == null) {
            throw new IllegalArgumentException(
                    "stateChangeConduit may not be null");
        }

        this.conduit = stateChangeConduit;
        
        if (print == null) {
            throw new IllegalArgumentException(
                    "print may not be null, use disabled impl instead");
        }
        this.pr = print;
    }

    // -------------------------------------------------------------------------
    // implements NotifyCallback
    // -------------------------------------------------------------------------

    public void deliver(List topicPath,
                        EndpointReferenceType producer,
                        Object message) {

        String serviceAddress;
        try {
            if (producer == null) {
                throw new IllegalArgumentException(
                                "producer EPR is null");
            } else if (producer.getAddress() == null) {
                throw new IllegalArgumentException(
                                "producer EPR address is null");
            } else {
                serviceAddress = producer.getAddress().toString();
            }
        } catch (Throwable t) {
            serviceAddress = "[[could not parse service address from " +
                    "EPR: " + t.getMessage() + "]]";
        }

        String workspaceID;
        try {
            final int id = EPRUtils.getIdFromEPR(producer);
            workspaceID = Integer.toString(id);
        } catch (Throwable t) {
            workspaceID = "[[could not parse workspace ID from EPR: " +
                    t.getMessage() + "]]";
        }

        final State newState;

        final ResourcePropertyValueChangeNotificationElementType valueChange =
                (ResourcePropertyValueChangeNotificationElementType) message;
        
        try {
            final MessageElement[] me =
                    valueChange.getResourcePropertyValueChangeNotification().
                            getNewValue().get_any();

            newState =
                    StateUtils.fromWireHelper(workspaceID, serviceAddress, me);
            
        } catch (Exception e) {

            // nothing to do here but print

            if (this.pr.enabled()) {
                final String err = e.getMessage();
                if (this.pr.useThis()) {
                    this.pr.errln(PrCodes.WSLISTEN__ERRORS, err);
                    e.printStackTrace(this.pr.getDebugProxy());
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.error(err, e);
                    } else {
                        logger.error(err);
                    }
                }
            }

            return; // *** EARLY RETURN ***
        }

        this.conduit.stateChange(producer, newState);
    }
    
}
