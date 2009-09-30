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

import org.globus.workspace.common.print.Print;
import org.globus.workspace.client_core.print.PrCodes;
import org.globus.workspace.client_core.ParameterProblem;
import org.globus.workspace.client_core.ExecutionProblem;
import org.globus.workspace.client_core.ExitNow;
import org.globus.workspace.client_core.StubConfigurator;
import org.globus.workspace.client_core.actions.ShutdownSave_Instance;
import org.globus.workspace.client_core.actions.ShutdownSave_Group;
import org.globus.workspace.client.AllArguments;
import org.globus.workspace.client.modes.aux.SingleShotMode;
import org.nimbustools.messaging.gt4_0.generated.types.OperationDisabledFault;
import org.nimbustools.messaging.gt4_0.generated.types.WorkspaceShutdownFault;
import org.nimbustools.messaging.gt4_0.generated.types.WorkspaceUnknownFault;
import org.nimbustools.messaging.gt4_0.generated.negotiable.PostShutdown_Type;
import org.globus.workspace.client_common.CommonStrings;
import org.oasis.wsrf.faults.BaseFaultType;
import org.apache.axis.types.URI;

/**
 * This mode is currently unused.  It was written before ShutdownSave was
 * a subscribing mode.  Keeping this here in case someone finds it useful. 
 */
public class SingleShotShutdownSave extends SingleShotMode {

    private PostShutdown_Type postShutdownRequest;

    public SingleShotShutdownSave(Print print,
                                  AllArguments arguments,
                                  StubConfigurator stubConfigurator) {
        super(print, arguments, stubConfigurator);
    }

    public String name() {
        return "Shutdown-save";
    }

    protected void setInstanceAction() throws ParameterProblem {
        this.instanceAction =
                new ShutdownSave_Instance(this.epr, this.stubConf, this.pr);
    }

    protected void setGroupAction() throws ParameterProblem {
        this.groupAction =
                new ShutdownSave_Group(this.epr, this.stubConf, this.pr);
    }

    protected void validateOptionsImpl() throws ParameterProblem {
        super.validateOptionsImpl();
        this.validateSaveTarget();
    }

    protected void validateSaveTarget() throws ParameterProblem {

        if (this.args.saveTarget == null) {
            return; // *** EARLY RETURN ***
        }

        this.postShutdownRequest = new PostShutdown_Type();

        try {
            final URI uri = new URI(this.args.saveTarget);
            this.postShutdownRequest.setRootPartitionUnpropagationTarget(uri);
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
            this._runImpl();
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
            this._doInstance();
        } else if (this.groupAction != null) {
            this._doGroup();
        }
    }

    private void _doInstance() throws ExecutionProblem,
                                      OperationDisabledFault,
                                      WorkspaceShutdownFault,
                                      ParameterProblem,
                                      WorkspaceUnknownFault {
        
        final ShutdownSave_Instance action = 
                (ShutdownSave_Instance) this.instanceAction;

        if (this.postShutdownRequest != null) {
            action.setPostShutdownRequest(this.postShutdownRequest);
        }

        action.shutdownSave();
    }

    private void _doGroup() throws ExecutionProblem,
                                   OperationDisabledFault,
                                   WorkspaceShutdownFault,
                                   ParameterProblem,
                                   WorkspaceUnknownFault {

        final ShutdownSave_Group action =
                (ShutdownSave_Group) this.groupAction;

        if (this.postShutdownRequest != null) {
            action.setPostShutdownRequest(this.postShutdownRequest);
        }

        action.shutdownSave();
    }
}
