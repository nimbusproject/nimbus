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

package org.globus.workspace.client_core.actions;

import org.globus.workspace.client_core.WSAction_Ensemble;
import org.globus.workspace.client_core.StubConfigurator;
import org.globus.workspace.client_core.ParameterProblem;
import org.globus.workspace.client_core.ExecutionProblem;
import org.globus.workspace.client_core.utils.RMIUtils;
import org.globus.workspace.common.print.Print;
import org.nimbustools.messaging.gt4_0.generated.ensemble.WorkspaceEnsemblePortType;
import org.nimbustools.messaging.gt4_0.generated.ensemble.VoidType;
import org.nimbustools.messaging.gt4_0.generated.ensemble.WorkspaceEnsembleFault;
import org.nimbustools.messaging.gt4_0.generated.types.WorkspaceUnknownFault;
import org.apache.axis.message.addressing.EndpointReferenceType;

import java.rmi.RemoteException;

public class Ensemble_Done extends WSAction_Ensemble {


    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    /**
     * @see WSAction_Ensemble
     */
    public Ensemble_Done(EndpointReferenceType epr,
                         StubConfigurator stubConf,
                         Print debug) {
        super(epr, stubConf, debug);
    }

    /**
     * @see WSAction_Ensemble
     */
    public Ensemble_Done(WorkspaceEnsemblePortType ensemblePortType,
                         Print debug) {
        super(ensemblePortType, debug);
    }


    // -------------------------------------------------------------------------
    // EXECUTE
    // -------------------------------------------------------------------------

    /**
     * Calls done()
     *
     * @return null
     * @throws Exception see done()
     * @see #done()
     */
    protected Object action() throws Exception {
        this.done();
        return null;
    }

    /**
     * Calls 'done' on ensemble resource.
     *
     * @throws org.globus.workspace.client_core.ParameterProblem validation problem
     * @throws org.globus.workspace.client_core.ExecutionProblem general problem running (connection errors etc)
     * @throws WorkspaceEnsembleFault severe
     * @throws WorkspaceUnknownFault gone
     */
    public void done() throws ParameterProblem,
                                 ExecutionProblem,
                                 WorkspaceUnknownFault,
                                 WorkspaceEnsembleFault {

        this.validateAll();

        try {

            ((WorkspaceEnsemblePortType) this.portType).done(new VoidType());

        } catch (WorkspaceUnknownFault e) {
            throw e;
        } catch (WorkspaceEnsembleFault e) {
            throw e;
        } catch (RemoteException e) {
            throw RMIUtils.generalRemoteException(e);
        }
    }
}
