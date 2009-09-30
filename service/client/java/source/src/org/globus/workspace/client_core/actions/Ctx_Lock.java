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

import org.globus.workspace.client_core.WSAction_Ctx;
import org.globus.workspace.client_core.StubConfigurator;
import org.globus.workspace.client_core.ParameterProblem;
import org.globus.workspace.client_core.ExecutionProblem;
import org.globus.workspace.client_core.utils.RMIUtils;
import org.globus.workspace.common.print.Print;
import org.nimbustools.messaging.gt4_0.generated.ctx.WorkspaceContextBrokerPortType;
import org.nimbustools.messaging.gt4_0.generated.types.VoidType;
import org.nimbustools.messaging.gt4_0.generated.types.WorkspaceContextualizationFault;
import org.apache.axis.message.addressing.EndpointReferenceType;

import java.rmi.RemoteException;

public class Ctx_Lock extends WSAction_Ctx {

    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    /**
     * @see WSAction_Ctx
     */
    public Ctx_Lock(EndpointReferenceType epr,
                    StubConfigurator stubConf,
                    Print debug) {
        super(epr, stubConf, debug);
    }

    /**
     * @see WSAction_Ctx
     */
    public Ctx_Lock(WorkspaceContextBrokerPortType ctxBrokerPortType,
                    Print debug) {
        super(ctxBrokerPortType, debug);
    }


    // -------------------------------------------------------------------------
    // EXECUTE
    // -------------------------------------------------------------------------

    /**
     * Calls lock()
     *
     * @return null
     * @throws Exception see lock()
     * @see #lock()
     */
    protected Object action() throws Exception {
        this.lock();
        return null;
    }

    /**
     * Calls 'lock' on context broker resource.
     *
     * @throws ParameterProblem validation problem
     * @throws ExecutionProblem general problem running (connection errors etc)
     * @throws WorkspaceContextualizationFault broker reports problem
     */
    public void lock() throws ParameterProblem,
                              ExecutionProblem,
                              WorkspaceContextualizationFault {

        this.validateAll();

        try {
            ((WorkspaceContextBrokerPortType) this.portType).lock(new VoidType());
        } catch (WorkspaceContextualizationFault e) {
            throw e;
        } catch (RemoteException e) {
            throw RMIUtils.generalRemoteException(e);
        }
    }
}
