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
import org.apache.axis.message.addressing.EndpointReferenceType;
import org.nimbustools.ctxbroker.generated.gt4_0.broker.NimbusContextBrokerPortType;
import org.nimbustools.ctxbroker.generated.gt4_0.broker.NimbusContextualizationFault;
import org.nimbustools.ctxbroker.generated.gt4_0.types.VoidType;

import java.rmi.RemoteException;

public class Ctx_NoMoreInjections extends WSAction_Ctx {

    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    /**
     * @see WSAction_Ctx
     */
    public Ctx_NoMoreInjections(EndpointReferenceType epr,
                    StubConfigurator stubConf,
                    Print debug) {
        super(epr, stubConf, debug);
    }

    /**
     * @see WSAction_Ctx
     */
    public Ctx_NoMoreInjections(NimbusContextBrokerPortType ctxBrokerPortType,
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
     * @see #noMoreInjections()
     */
    protected Object action() throws Exception {
        this.noMoreInjections();
        return null;
    }

    /**
     * Calls 'noMoreInjections' on context broker resource.
     *
     * @throws ParameterProblem validation problem
     * @throws ExecutionProblem general problem running (connection errors etc)
     * @throws NimbusContextualizationFault broker reports problem
     */
    public void noMoreInjections() throws ParameterProblem,
                                          ExecutionProblem,
                                          NimbusContextualizationFault {

        this.validateAll();

        try {
            ((NimbusContextBrokerPortType) this.portType)
                    .noMoreInjections(new VoidType());
        } catch (NimbusContextualizationFault e) {
            throw e;
        } catch (RemoteException e) {
            throw RMIUtils.generalRemoteException(e);
        }
    }
}
