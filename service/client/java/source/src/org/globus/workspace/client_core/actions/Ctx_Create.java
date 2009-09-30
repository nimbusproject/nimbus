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
import org.nimbustools.ctxbroker.generated.gt4_0.types.CreateContext_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.types.CreateContextResponse_Type;

import java.rmi.RemoteException;

public class Ctx_Create extends WSAction_Ctx {

    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    /**
     * @see WSAction_Ctx
     */
    public Ctx_Create(EndpointReferenceType epr,
                      StubConfigurator stubConf,
                      Print debug) {
        super(epr, stubConf, debug);
    }

    /**
     * @see WSAction_Ctx
     */
    public Ctx_Create(NimbusContextBrokerPortType ctxBrokerPortType,
                      Print debug) {
        super(ctxBrokerPortType, debug);
    }

    // -------------------------------------------------------------------------
    // EXECUTE
    // -------------------------------------------------------------------------

    /**
     * Calls create()
     *
     * @return null
     * @throws Exception see inject()
     * @see #create()
     */
    protected Object action() throws Exception {
        return this.create();
    }

    protected CreateContext_Type getCreateArguments() {
        final CreateContext_Type t_create = new CreateContext_Type();
        t_create.setExpectInjections(false);
        return t_create;
    }

    /**
     * Creates a context broker resource (a new context).  The created context
     * will not expect any data injections.
     *
     * @throws ParameterProblem validation problem
     * @throws ExecutionProblem general problem running (connection errors etc)
     * @throws NimbusContextualizationFault broker reports problem
     * @return CreateResponse_Type response with contact info and new ctx EPR
     */
    public CreateContextResponse_Type create()
            throws ParameterProblem, ExecutionProblem,
                   NimbusContextualizationFault {

        this.validateAll();

        final CreateContext_Type t_create = this.getCreateArguments();
        if (t_create == null) {
            // note to object extenders
            throw new IllegalArgumentException("getCreateArguments may " +
                    "not return null");
        }

        final CreateContextResponse_Type resp;
        try {
            resp =
                ((NimbusContextBrokerPortType) this.portType).create(t_create);

            if (resp == null ||
                    resp.getContextEPR() == null ||
                        resp.getContact() == null ||
                        resp.getContact().getBrokerURL() == null ||
                        resp.getContact().getContextID() == null ||
                        resp.getContact().getSecret() == null) {
                
                throw new ExecutionProblem("did not receive a valid result " +
                        "from context create operation (?)");
            }
        } catch (NimbusContextualizationFault e) {
            throw e;
        } catch (RemoteException e) {
            throw RMIUtils.generalRemoteException(e);
        }

        return resp;
    }
}
