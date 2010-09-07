/*
 * Copyright 1999-2007 University of Chicago
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

package org.globus.workspace.status.client;

import org.nimbustools.messaging.gt4_0.common.Constants_GT4_0;
import org.nimbustools.messaging.gt4_0.generated.status.WorkspaceStatusPortType;
import org.nimbustools.messaging.gt4_0.generated.status.StatusRPSet;
import org.globus.wsrf.WSRFConstants;
import org.apache.axis.message.addressing.EndpointReferenceType;
import org.apache.axis.message.addressing.ReferencePropertiesType;
import org.apache.axis.message.MessageElement;
import org.apache.axis.client.Stub;
import org.oasis.wsrf.properties.QueryResourceProperties_Element;
import org.oasis.wsrf.properties.QueryResourcePropertiesResponse;
import org.oasis.wsrf.properties.QueryExpressionType;

import java.io.PrintStream;
import java.io.IOException;

public class RPQuery extends Thread implements Constants_GT4_0 {

    private PrintStream out = null;
    private WorkspaceStatusClient client = null;
    private EndpointReferenceType endpoint = null;

    public RPQuery(WorkspaceStatusClient _client,
                   EndpointReferenceType _endpoint,
                   PrintStream _out) {
        this.out = _out;
        this.client = _client;
        this.endpoint = _endpoint;
    }

    public void run() {
        try {
            queryRPDoc();
        } catch (Exception e) {
            WorkspaceStatusClient.die(e,
                                      "Problem running RP query",
                                      this.client.isDebugMode());
        }
    }

    private void queryRPDoc() throws Exception {
        if (this.client == null) {
            throw new Exception("client is null, cannot query for RP");
        }

        if (this.endpoint == null) {
            throw new Exception("no workspace factory endpoint in client");
        }

        if (this.out == null) {
            throw new Exception("no printstream");
        }

        WorkspaceStatusPortType statusPort =
                this.client.initStatusPort(this.endpoint);

        this.client.setOptions((Stub)statusPort);

        // return the entire RP document
        String queryStr = "/";

        QueryResourceProperties_Element query =
                new QueryResourceProperties_Element();

        query.setQueryExpression(
             createQueryExpression(WSRFConstants.XPATH_1_DIALECT,
                   queryStr));

        QueryResourcePropertiesResponse resp =
                statusPort.queryResourceProperties(query);

        StatusRPSet rpSet = (StatusRPSet) resp.get_any()[0].
                        getValueAsType(STATUS_RP_SET, StatusRPSet.class);

        String impl = null;
        ReferencePropertiesType refProps = this.endpoint.getProperties();
        if (refProps != null) {
             MessageElement key = refProps.get(RESOURCE_KEY_QNAME);
            if (key != null) {
                impl = key.getValue();
            }
        }

        if (rpSet == null) {
            this.out.println("Charge granularity: empty response");
        } else {
            this.out.println("Charge granularity for '" + impl  +
                    "' implementation: " +
                    rpSet.getChargeGranularity());
        }
    }

    private QueryExpressionType createQueryExpression(String dialect,
                                                      String queryString)
        throws IOException {

        QueryExpressionType query = new QueryExpressionType();
        query.setDialect(dialect);

        if (queryString != null) {
            query.setValue(queryString);
        }

        return query;
    }

}
