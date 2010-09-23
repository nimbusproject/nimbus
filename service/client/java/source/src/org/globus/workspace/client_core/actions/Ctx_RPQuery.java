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
import org.nimbustools.messaging.gt4_0.common.CommonUtil;
import org.nimbustools.messaging.gt4_0.common.Constants_GT4_0;
import org.nimbustools.ctxbroker.generated.gt4_0.broker.NimbusContextBrokerPortType;
import org.nimbustools.ctxbroker.generated.gt4_0.broker.NimbusContextualizationFault;
import org.nimbustools.ctxbroker.generated.gt4_0.types.ContextualizationContext;
import org.globus.wsrf.encoding.ObjectDeserializer;
import org.globus.wsrf.encoding.DeserializationException;
import org.apache.axis.message.addressing.EndpointReferenceType;
import org.apache.axis.message.MessageElement;
import org.oasis.wsrf.properties.GetResourcePropertyResponse;

import java.rmi.RemoteException;

public class Ctx_RPQuery extends WSAction_Ctx {

    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    /**
     * @see WSAction_Ctx
     */
    public Ctx_RPQuery(EndpointReferenceType epr,
                       StubConfigurator stubConf,
                       Print debug) {
        super(epr, stubConf, debug);
    }

    /**
     * @see WSAction_Ctx
     */
    public Ctx_RPQuery(NimbusContextBrokerPortType ctxBrokerPortType,
                       Print debug) {
        super(ctxBrokerPortType, debug);
    }

    // -------------------------------------------------------------------------
    // EXECUTE
    // -------------------------------------------------------------------------

    /**
     * Calls query()
     *
     * @return null
     * @throws Exception see query()
     * @see #query()
     */
    protected Object action() throws Exception {
        this.query();
        return null;
    }

    /**
     * Query context broker resource property once
     *
     * @throws ParameterProblem validation problem
     * @throws ExecutionProblem general problem running (connection errors etc)
     * @throws NimbusContextualizationFault broker reports problem
     * @return ContextualizationContext context
     */
    public ContextualizationContext query()
                                        throws ParameterProblem,
                                               ExecutionProblem,
                                               NimbusContextualizationFault {

        this.validateAll();

        try {
            final GetResourcePropertyResponse resp =
                    ((NimbusContextBrokerPortType) this.portType)
                        .getResourceProperty(
                            Constants_GT4_0.CTXBROKER_RP);
            return this.convert(resp);
        } catch (NimbusContextualizationFault e) {
            throw e;
        } catch (RemoteException e) {
            throw RMIUtils.generalRemoteException(e);
        }
    }

    protected ContextualizationContext convert(GetResourcePropertyResponse resp)
            throws ExecutionProblem {

        if (resp == null) {
            throw new ExecutionProblem("No response element from query?");
        }

        final MessageElement elem = resp.get_any()[0];
        if (elem == null) {
            throw new ExecutionProblem("No message in query response");
        }

        final ContextualizationContext context;
        try {
            context = (ContextualizationContext)
                    ObjectDeserializer.toObject(elem,
                                                ContextualizationContext.class);
        } catch (DeserializationException e) {
            final String err = CommonUtil.genericExceptionMessageWrapper(e);
            throw new ExecutionProblem(
                    "Problem deserializing query response: " + err, e);
        }

        return context;
    }
}
