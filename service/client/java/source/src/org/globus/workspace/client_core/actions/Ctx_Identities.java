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
import org.nimbustools.ctxbroker.generated.gt4_0.broker.NimbusContextBrokerPortType;
import org.nimbustools.ctxbroker.generated.gt4_0.broker.NimbusContextualizationFault;
import org.nimbustools.ctxbroker.generated.gt4_0.types.Node_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.types.IdentitiesSend_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.types.IdentitiesResponse_Type;
import org.apache.axis.message.addressing.EndpointReferenceType;

import java.rmi.RemoteException;

public class Ctx_Identities extends WSAction_Ctx {

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private boolean queryAll;
    private String queryIP;
    private String queryHost;
    private static final Node_Type[] EMPTY_RESPONSE = new Node_Type[0];

    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    /**
     * @see WSAction_Ctx
     */
    public Ctx_Identities(EndpointReferenceType epr,
                          StubConfigurator stubConf,
                          Print debug) {
        super(epr, stubConf, debug);
    }

    /**
     * @see WSAction_Ctx
     */
    public Ctx_Identities(NimbusContextBrokerPortType ctxBrokerPortType,
                          Print debug) {
        super(ctxBrokerPortType, debug);
    }


    // -------------------------------------------------------------------------
    // GET/SET
    // -------------------------------------------------------------------------

    public boolean isQueryAll() {
        return this.queryAll;
    }

    public void setQueryAll(boolean queryAll) {
        this.queryAll = queryAll;
    }

    public String getQueryIP() {
        return this.queryIP;
    }

    public void setQueryIP(String queryIP) {
        this.queryIP = queryIP;
    }

    public String getQueryHost() {
        return this.queryHost;
    }

    public void setQueryHost(String queryHost) {
        this.queryHost = queryHost;
    }


    // -------------------------------------------------------------------------
    // VALIDATE
    // -------------------------------------------------------------------------

    /**
     * @throws org.globus.workspace.client_core.ParameterProblem issue that will stop creation attempt
     */
    public void validateAll() throws ParameterProblem {
        super.validateAll();
        this.validateQuery();
    }

    protected void validateQuery() throws ParameterProblem {

        int numSet = 0;
        if (this.queryAll) {
            numSet += 1;
        }
        if (this.queryHost != null) {
            numSet += 1;
        }
        if (this.queryIP != null) {
            numSet += 1;
        }

        if (numSet > 1) {
            throw new ParameterProblem(
                    "Can only choose one query method: all, ip, or host");
        }

        if (numSet == 0) {
            throw new ParameterProblem(
                    "Must choose one query method: all, ip, or host");
        }
    }


    // -------------------------------------------------------------------------
    // EXECUTE
    // -------------------------------------------------------------------------

    /**
     * Calls identities()
     *
     * @return Node_Type[] may be len zero, never null
     * @throws Exception see identities()
     * @see #identities()
     */
    protected Object action() throws Exception {
        this.identities();
        return null;
    }

    /**
     * Calls 'identities' on context broker resource.
     *
     * @throws ParameterProblem validation problem
     * @throws ExecutionProblem general problem running (connection errors etc)
     * @throws NimbusContextualizationFault broker reports problem
     * @return Node_Type[] may be len zero, never null
     */
    public Node_Type[] identities() throws ParameterProblem,
                                           ExecutionProblem,
                                           NimbusContextualizationFault {

        this.validateAll();

        final IdentitiesSend_Type send = new IdentitiesSend_Type();
        if (this.queryAll) {
            send.setAll(Boolean.TRUE);
        } else if (this.queryHost != null) {
            send.setHost(this.queryHost);
        } else if (this.queryIP != null) {
            send.setIp(this.queryIP);
        } else {
            throw new IllegalStateException("validation failed?");
        }

        final IdentitiesResponse_Type response;
        try {
            response = ((NimbusContextBrokerPortType)
                                        this.portType).identities(send);
        } catch (NimbusContextualizationFault e) {
            throw e;
        } catch (RemoteException e) {
            throw RMIUtils.generalRemoteException(e);
        }

        if (response == null) {
            throw new ExecutionProblem("No response element from query?");
        }

        final Node_Type[] ret = response.getNode();
        if (ret == null || ret.length == 0) {
            return EMPTY_RESPONSE;
        } else {
            return ret;
        }
    }
}
