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

import org.apache.axis.message.addressing.EndpointReferenceType;
import org.globus.workspace.client_core.ExecutionProblem;
import org.globus.workspace.client_core.ParameterProblem;
import org.globus.workspace.client_core.StubConfigurator;
import org.globus.workspace.client_core.WSAction_Instance;
import org.globus.workspace.common.print.Print;
import org.globus.workspace.client_core.utils.RMIUtils;
import org.nimbustools.messaging.gt4_0.generated.WorkspacePortType;
import org.nimbustools.messaging.gt4_0.generated.types.WorkspaceStartFault;
import org.nimbustools.messaging.gt4_0.generated.types.OperationDisabledFault;
import org.nimbustools.messaging.gt4_0.generated.types.VoidType;
import org.nimbustools.messaging.gt4_0.generated.types.WorkspaceUnknownFault;

import java.rmi.RemoteException;

public class Start_Instance extends WSAction_Instance {


    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    /**
     * @see WSAction_Instance
     */
    public Start_Instance(EndpointReferenceType epr,
                          StubConfigurator stubConf,
                          Print debug) {
        super(epr, stubConf, debug);
    }

    /**
     * @see WSAction_Instance
     */
    public Start_Instance(WorkspacePortType instancePortType,
                          Print debug) {
        super(instancePortType, debug);
    }

    
    // -------------------------------------------------------------------------
    // EXECUTE
    // -------------------------------------------------------------------------

    /**
     * Calls start()
     *
     * @return null
     * @throws Exception see start()
     * @see #start()
     */
    protected Object action() throws Exception {
        this.start();
        return null;
    }

    /**
     * Start/un-pause a workspace.
     *
     * @throws ParameterProblem validation problem
     * @throws ExecutionProblem general problem running (connection errors etc)
     * @throws WorkspaceStartFault problem starting
     * @throws OperationDisabledFault not allowed to start
     * @throws WorkspaceUnknownFault unknown
     */
    public void start() throws ParameterProblem,
                               ExecutionProblem,
                               WorkspaceStartFault,
                               OperationDisabledFault,
                               WorkspaceUnknownFault {

        this.validateAll();

        try {

            ((WorkspacePortType) this.portType).start(new VoidType());

        } catch (WorkspaceUnknownFault e) {
            throw e;
        } catch (WorkspaceStartFault e) {
            throw e;
        } catch (OperationDisabledFault e) {
            throw e;
        } catch (RemoteException e) {
            throw RMIUtils.generalRemoteException(e);
        }
    }
}
