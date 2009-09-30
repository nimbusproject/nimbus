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

import org.globus.workspace.client_core.repr.Workspace;
import org.globus.workspace.common.print.Print;
import org.globus.workspace.client_core.print.PrCodes;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class GenericTerminationListener extends GenericListener
                                        implements TerminationListener {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    protected static final Log logger =
            LogFactory.getLog(GenericTerminationListener.class.getName());


    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    /**
     * No printing possible
     */
    public GenericTerminationListener() {
        super();
    }

    /**
     * @param print may be null (will be disabled)
     */
    public GenericTerminationListener(Print print) {
        super(print);
    }

    
    // -------------------------------------------------------------------------
    // implements TerminationListener
    // -------------------------------------------------------------------------

    public void terminationOccured(SubscriptionMaster master,
                                   Workspace workspace) {

        if (!this.goodInput(master, workspace)) {
            return; // *** EARLY RETURN ***
        }

        workspace.setTerminated(true);
        master.untrackWorkspace(workspace);
        this.printTerminated(workspace);
        this.theTerminationOccured(workspace);
    }
    

    // -------------------------------------------------------------------------
    // hooks for children
    // -------------------------------------------------------------------------

    /**
     * hook for children (one can also override terminationOccured entirely)
     * 
     * @param workspace never going to be null
     */
    protected void theTerminationOccured(Workspace workspace) {
        // do nothing but don't make abstract, implementation would be required
    }
    
    // -------------------------------------------------------------------------
    // printing
    // -------------------------------------------------------------------------

    protected void printTerminated(Workspace workspace) {

        if (!this.pr.enabled()) {
            return; // *** EARLY RETURN ***
        }

        final String msg = "\"" +
                this.workspaceID(workspace) + "\" was terminated.";
        if (this.pr.useThis()) {
            this.pr.infoln(PrCodes.LISTENER_TERMINATION__INSTANCE_ID_PRINT, msg);
        } else if (this.pr.useLogging()) {
            logger.info(msg);
        }
    }
    

    // -------------------------------------------------------------------------
    // checks
    // -------------------------------------------------------------------------

    protected boolean goodInput(SubscriptionMaster master, Workspace workspace) {

        if (master == null || workspace == null) {

            if (workspace == null) {
                this.errlogNoMaster(PrCodes.LISTENER_TERMINATION__ERRORS,
                                    workspace);
            }

            if (workspace == null) {
                this.errlogNoWorkspace(PrCodes.LISTENER_TERMINATION__ERRORS);
            }

            return false; // *** EARLY RETURN ***
        }

        return true;
    }
}
