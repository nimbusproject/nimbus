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

package org.globus.workspace.client.modes;

import org.apache.axis.types.URI;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.workspace.client.AllArguments;
import org.globus.workspace.client.modes.aux.CommonLogs;
import org.globus.workspace.client.modes.aux.SubscribeWait;
import org.globus.workspace.client_core.ParameterProblem;
import org.globus.workspace.client_core.StubConfigurator;
import org.globus.workspace.client_core.ExecutionProblem;
import org.globus.workspace.client_core.ExitNow;
import org.globus.workspace.client_core.utils.EPRUtils;
import org.globus.workspace.client_core.repr.State;
import org.globus.workspace.client_core.actions.ShutdownSave_Group;
import org.globus.workspace.client_core.actions.ShutdownSave_Instance;
import org.globus.workspace.common.print.Print;
import org.globus.workspace.client_core.print.PrCodes;
import org.nimbustools.messaging.gt4_0.generated.negotiable.PostShutdown_Type;
import org.nimbustools.messaging.gt4_0.generated.types.OperationDisabledFault;
import org.nimbustools.messaging.gt4_0.generated.types.WorkspaceShutdownFault;
import org.nimbustools.messaging.gt4_0.generated.types.WorkspaceUnknownFault;
import org.globus.workspace.client_common.CommonStrings;
import org.oasis.wsrf.faults.BaseFaultType;

public class ShutdownSave extends Subscribe {

    
    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(ShutdownSave.class.getName());

    
    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private PostShutdown_Type postRequest;
    
    // note that "subscribe" has same meaning here whether polling or not
    boolean subscribeBeforeAction;

    protected ShutdownSave_Instance instanceAction;
    protected ShutdownSave_Group groupAction;

    
    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    public ShutdownSave(Print print,
                        AllArguments arguments,
                        StubConfigurator stubConfigurator) {
        super(print, arguments, stubConfigurator);
    }


    // -------------------------------------------------------------------------
    // GET/SET
    // -------------------------------------------------------------------------

    protected void setInstanceAction() throws ParameterProblem {
        this.instanceAction =
                new ShutdownSave_Instance(this.epr, this.stubConf, this.pr);
    }

    protected void setGroupAction() throws ParameterProblem {
        this.groupAction =
                new ShutdownSave_Group(this.epr, this.stubConf, this.pr);
    }

    public String name() {
        return "Shutdown-save";
    }

    // -------------------------------------------------------------------------
    // VALIDATION
    // -------------------------------------------------------------------------

    public void validateOptionsImpl() throws ParameterProblem {
        
        this.subscribeBeforeAction = this.args.subscriptions;
        CommonLogs.logBoolean(this.subscribeBeforeAction,
                              "subscription",
                              this.pr, logger);

        if (EPRUtils.isGroupEPR(this.epr) && this.subscribeBeforeAction) {
            if (this.pr.enabled()) {
                final String err = "Cannot subscribe via group EPR yet, " +
                        "disabled requested subscribe mode.";
                if (this.pr.useThis()) {
                    this.pr.errln(err);
                } else if (this.pr.useLogging()) {
                    logger.error(err);
                }
            }
            this.subscribeBeforeAction = false;
        }

        if (this.subscribeBeforeAction) {
            super.validateOptionsImpl();
            this.handleExitState();
        } else {
            this.validateEndpoint();
            this.validateName();
        }

        this.validateSaveTarget();

        if (this.isGroupRequest) {
            this.setGroupAction();
        } else {
            this.setInstanceAction();
        }
    }

    protected void handleExitState() throws ParameterProblem {

        if (this.exitState != null) {

            if (this.pr.enabled()) {
                final String dbg = "Explicit exit state provided for " +
                        "shutdown-save... '" + this.exitState.toString() + "'";
                if (this.pr.useThis()) {
                    this.pr.dbg(dbg);
                } else if (this.pr.useLogging()) {
                    logger.debug(dbg);
                }
            }

            if (!this.exitState.isTransportReady()) {
                final String err = "Explicit exit state provided for " +
                    "shutdown-save is not 'TransportReady' which is illegal";
                throw new ParameterProblem(err);
            }
        }

        this.exitState = new State(State.STATE_TransportReady);
    }

    protected void validateSaveTarget() throws ParameterProblem {

        if (this.args.saveTarget == null) {
            return; // *** EARLY RETURN ***
        }

        this.postRequest = new PostShutdown_Type();

        try {
            final URI uri = new URI(this.args.saveTarget);
            this.postRequest.setRootPartitionUnpropagationTarget(uri);
        } catch (URI.MalformedURIException e) {
            final String err = "Save target override is not a valid URI: '" +
                    this.args.saveTarget + "'";
            throw new ParameterProblem(err, e);
        }

        if (this.pr.enabled()) {
            final String dbg = "Save target override going to be used: '" +
                    this.args.saveTarget + "'";
            if (this.pr.useThis()) {
                this.pr.dbg(dbg);
            } else if (this.pr.useLogging()) {
                logger.debug(dbg);
            }
        }
    }
    

    // -------------------------------------------------------------------------
    // RUN
    // -------------------------------------------------------------------------


    public void runImpl() throws ParameterProblem, ExecutionProblem, ExitNow {

        if (this.args.dryrun) {

            if (this.pr.enabled()) {
                final String msg = "Dryrun, done.";
                if (this.pr.useThis()) {
                    this.pr.infoln(PrCodes.SHUTDOWNSAVE__DRYRUN, msg);
                } else if (this.pr.useLogging()) {
                    logger.info(msg);
                }
            }

            return; // *** EARLY RETURN ***
        }

        try {

            SubscribeWait waiter = null;
            State useExitState = this.exitState;
            if (this.subscribeBeforeAction) {

                if (this.autodestroy) {
                    // there's no need for the waiter to wait for exit states
                    // in the auto-destroy case because we're setting up an
                    // additional destroy task when TransportReady is reached
                    useExitState = null;
                }

                waiter = this.subscribeLaunch.subscribeNoWait(
                                                     this.workspaces,
                                                     useExitState,
                                                     this.veryTerseNotifyState,
                                                     this.autodestroy,
                                                     false,
                                                     this.exitState,
                                                     false);
            }

            this._runImpl();

            if (waiter != null) {
                waiter.run(useExitState);
            }

        } catch (ExitNow e) {
            throw e;
        } catch (BaseFaultType e) {
            final String err = CommonStrings.faultStringOrCommonCause(e);
            throw new ExecutionProblem(err, e);
        }
    }

    private void _runImpl() throws ExecutionProblem,
                                   OperationDisabledFault,
                                   WorkspaceShutdownFault,
                                   ParameterProblem,
                                   WorkspaceUnknownFault {

        if (this.instanceAction != null) {
            if (this.postRequest != null) {
                this.instanceAction.setPostShutdownRequest(this.postRequest);
            }
            this.instanceAction.shutdownSave();
        } else if (this.groupAction != null) {
            if (this.postRequest != null) {
                this.groupAction.setPostShutdownRequest(this.postRequest);
            }
            this.groupAction.shutdownSave();
        }
    }
    
}
