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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.workspace.common.print.Print;
import org.globus.workspace.client_core.print.PrCodes;
import org.globus.workspace.client_core.repr.Workspace;
import org.globus.workspace.client_core.repr.State;
import org.nimbustools.messaging.gt4_0.common.CommonUtil;

public class GenericStateChangeListener extends GenericListener
                                        implements StateChangeListener {

    
    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    protected static final Log logger =
            LogFactory.getLog(GenericStateChangeListener.class.getName());

    
    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------


    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    /**
     * No printing possible
     */
    public GenericStateChangeListener() {
        super();
    }

    /**
     * @param print may be null (will be disabled)
     */
    public GenericStateChangeListener(Print print) {
        super(print);
    }

    
    // -------------------------------------------------------------------------
    // implements StateChangeListener
    // -------------------------------------------------------------------------

    public void newState(SubscriptionMaster master,
                         Workspace workspace,
                         State newState) {

        if (!this.goodInput(master, workspace, newState)) {
            return; // *** EARLY RETURN ***
        }

        final State oldState = workspace.getCurrentState();
        workspace.setCurrentState(newState);
        this.printChanged(workspace, oldState, newState);
        this.theNewState(workspace, oldState, newState);
    }

    // -------------------------------------------------------------------------
    // hooks for children
    // -------------------------------------------------------------------------

    /**
     * hook for children (one can also override newState entirely)
     * 
     * @param workspace never going to be null
     * @param oldState may be null
     * @param newState never going to be null
     */
    protected void theNewState(Workspace workspace,
                               State oldState,
                               State newState) {
        
        // do nothing, but do not require by making abstract
    }

    // -------------------------------------------------------------------------
    // printing
    // -------------------------------------------------------------------------

    protected void printChanged(Workspace workspace,
                                State oldState,
                                State newState) {

        if (!this.pr.enabled()) {
            return; // *** EARLY RETURN ***
        }

        final String oldStateStr;
        if (oldState != null) {
            oldStateStr = oldState.getState();
        } else {
            oldStateStr = "UNKNOWN-state";
        }

        final String newStateStr = newState.getState();
        final String msg = "\"" + this.workspaceID(workspace) +
                "\" state change: " + oldStateStr + " --> " + newStateStr;

        if (this.pr.useThis()) {
            this.pr.infoln(PrCodes.LISTENER_STATECHANGE__INSTANCE_STATE_CHANGE,
                           msg);
        } else if (this.pr.useLogging()) {
            logger.info(msg);
        }

        final Exception exception = newState.getProblem();
        if (exception != null) {
            final String err = "Problem with " + this.workspaceID(workspace) +
                    ": " + CommonUtil.genericExceptionMessageWrapper(exception);

            if (this.pr.useThis()) {
                this.pr.errln(PrCodes.LISTENER_STATECHANGE__INSTANCE_STATE_PROBLEMS,
                               err);
                exception.printStackTrace(this.pr.getDebugProxy());
            } else if (this.pr.useLogging()) {
                if (logger.isDebugEnabled()) {
                    logger.error(err, exception);
                } else {
                    logger.error(err);
                }
            }
        }
    }

    
    // -------------------------------------------------------------------------
    // checks
    // -------------------------------------------------------------------------

    protected boolean goodInput(SubscriptionMaster master,
                                Workspace workspace,
                                State newState) {

        if (master == null || workspace == null) {

            if (workspace == null) {
                this.errlogNoMaster(PrCodes.LISTENER_STATECHANGE__ERRORS,
                                    workspace);
            }

            if (workspace == null) {
                this.errlogNoWorkspace(PrCodes.LISTENER_STATECHANGE__ERRORS);
            }

            if (newState == null) {
                this.errlogNoState(PrCodes.LISTENER_STATECHANGE__ERRORS,
                                   workspace);
            }

            return false; // *** EARLY RETURN ***
        }

        return true;
    }

    protected void errlogNoState(int prcode, Workspace workspace) {

        if (!this.pr.enabled()) {
            return; // *** EARLY RETURN ***
        }

        final String err = "Listener did not receive state: " +
                this.workspaceID(workspace);

        if (this.pr.useThis()) {
            this.pr.errln(prcode, err);
        } else if (this.pr.useLogging()) {
            logger.error(err);
        }
    }
}
