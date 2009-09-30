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
import org.nimbustools.ctxbroker.generated.gt4_0.types.RetrieveResponse_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.description.AgentDescription_Type;

import java.rmi.RemoteException;

/**
 * Calls retrieve on the broker, acting like a context agent.  Something a
 * developer would use for looking at SOAP messages etc. (unless you are
 * developing a Java based context agent).
 */
public class Ctx_Retrieve extends WSAction_Ctx {

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected AgentDescription_Type retrieveSend;


    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    /**
     * @see WSAction_Ctx
     */
    public Ctx_Retrieve(EndpointReferenceType epr,
                        StubConfigurator stubConf,
                        Print debug) {
        super(epr, stubConf, debug);
    }

    /**
     * @see WSAction_Ctx
     */
    public Ctx_Retrieve(NimbusContextBrokerPortType ctxBrokerPortType,
                        Print debug) {
        super(ctxBrokerPortType, debug);
    }

    
    // -------------------------------------------------------------------------
    // PROPERTIES
    // -------------------------------------------------------------------------

    public AgentDescription_Type getRetrieveSend() {
        return this.retrieveSend;
    }

    public void setRetrieveSend(AgentDescription_Type retrieveSend) {
        this.retrieveSend = retrieveSend;
    }

    
    // -------------------------------------------------------------------------
    // VALIDATION
    // -------------------------------------------------------------------------

    /**
     * @throws ParameterProblem issue that will stop creation attempt
     */
    public void validateAll() throws ParameterProblem {
        super.validateAll();
        if (this.retrieveSend == null) {
            throw new ParameterProblem("there is no configured " +
                    "RetrieveSend_Type, cannot run Ctx_Retrieve");
        }
    }


    // -------------------------------------------------------------------------
    // EXECUTE
    // -------------------------------------------------------------------------

    /**
     * Calls retrieve()
     *
     * @return RetrieveResponse_Type
     * @throws Exception see lock()
     * @see #retrieve()
     */
    protected Object action() throws Exception {
        this.retrieve();
        return null;
    }

    /**
     * Calls 'retrieve' on context broker resource, acting like a context agent
     * (for developer use).
     *
     * @throws ParameterProblem validation problem
     * @throws ExecutionProblem general problem running (connection errors etc)
     * @throws NimbusContextualizationFault broker reports problem
     * @return resp
     */
    public RetrieveResponse_Type retrieve() throws ParameterProblem,
                                                   ExecutionProblem,
                                                   NimbusContextualizationFault {

        this.validateAll();

        try {
            return ((NimbusContextBrokerPortType) this.portType)
                        .retrieve(this.retrieveSend);
        } catch (NimbusContextualizationFault e) {
            throw e;
        } catch (RemoteException e) {
            throw RMIUtils.generalRemoteException(e);
        }
    }
}
