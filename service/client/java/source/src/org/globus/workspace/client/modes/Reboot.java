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
import org.globus.workspace.client_core.actions.Reboot_Instance;
import org.globus.workspace.client_core.actions.Reboot_Group;
import org.globus.workspace.client.AllArguments;
import org.globus.workspace.client.modes.aux.SingleShotMode;
import org.nimbustools.messaging.gt4_0.generated.types.OperationDisabledFault;
import org.nimbustools.messaging.gt4_0.generated.types.WorkspaceShutdownFault;
import org.nimbustools.messaging.gt4_0.generated.types.WorkspaceUnknownFault;
import org.globus.workspace.client_common.CommonStrings;
import org.oasis.wsrf.faults.BaseFaultType;

public class Reboot extends SingleShotMode {

    public Reboot(Print print,
                  AllArguments arguments,
                  StubConfigurator stubConfigurator) {
        super(print, arguments, stubConfigurator);
    }

    public String name() {
        return "Reboot";
    }

    protected void setInstanceAction() throws ParameterProblem {
        this.instanceAction =
                new Reboot_Instance(this.epr,  this.stubConf, this.pr);
    }

    protected void setGroupAction() throws ParameterProblem {
        this.groupAction =
                new Reboot_Group(this.epr,  this.stubConf, this.pr);
    }

    public void runImpl() throws ParameterProblem, ExecutionProblem, ExitNow {

        if (this.args.dryrun) {

            if (this.pr.enabled()) {
                final String msg = "Dryrun, done.";
                if (this.pr.useThis()) {
                    this.pr.infoln(PrCodes.REBOOT__DRYRUN, msg);
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
            ((Reboot_Instance)this.instanceAction).reboot();
        } else if (this.groupAction != null) {
            ((Reboot_Group)this.groupAction).reboot();
        }
    }
}
