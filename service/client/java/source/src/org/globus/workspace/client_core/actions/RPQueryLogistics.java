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

import org.globus.workspace.client_core.ParameterProblem;
import org.globus.workspace.client_core.ExecutionProblem;
import org.globus.workspace.client_core.WSAction_Instance;
import org.globus.workspace.client_core.StubConfigurator;
import org.globus.workspace.client_core.repr.LogisticsWrapper;
import org.globus.workspace.client_core.repr.Networking;
import org.globus.workspace.common.print.Print;
import org.nimbustools.messaging.gt4_0.generated.WorkspacePortType;
import org.nimbustools.messaging.gt4_0.generated.metadata.logistics.Logistics;
import org.nimbustools.messaging.gt4_0.generated.metadata.logistics.VirtualNetwork_Type;
import org.nimbustools.messaging.gt4_0.common.Constants_GT4_0;
import org.globus.wsrf.encoding.ObjectDeserializer;
import org.apache.axis.message.addressing.EndpointReferenceType;
import org.apache.axis.message.MessageElement;
import org.oasis.wsrf.properties.GetResourcePropertyResponse;

public class RPQueryLogistics extends WSAction_Instance {

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------


    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    /**
     * @see WSAction_Instance
     */
    public RPQueryLogistics(EndpointReferenceType epr,
                            StubConfigurator stubConf,
                            Print debug) {
        super(epr, stubConf, debug);
    }

    /**
     * @see WSAction_Instance
     */
    public RPQueryLogistics(WorkspacePortType instancePortType,
                            Print debug) {
        super(instancePortType, debug);
    }

    // -------------------------------------------------------------------------
    // EXECUTE
    // -------------------------------------------------------------------------

    protected Object action() throws Exception {
        return this.queryOnce();
    }

    /**
     * query once (the action)
     *
     * @return LogisticsWrapper never null
     * @throws ParameterProblem validation problem
     * @throws ExecutionProblem problem running
     */
    public LogisticsWrapper queryOnce() throws ParameterProblem,
                                               ExecutionProblem {

        this.validateAll();

        try {
            return this._queryOnce();
        } catch (Exception e) {
            final String err = "Problem querying logistics: ";
            throw new ExecutionProblem(err + e.getMessage(), e);
        }
    }

    private LogisticsWrapper _queryOnce() throws Exception {

        final GetResourcePropertyResponse resp =
            ((WorkspacePortType) this.portType).getResourceProperty(
                                        Constants_GT4_0.RP_LOGISTICS);

        final MessageElement msg = resp.get_any()[0];

        final Logistics log = (Logistics)
                ObjectDeserializer.toObject(msg, Logistics.class);

        final VirtualNetwork_Type t_network = log.getNetworking();

        final Networking net;
        if (t_network == null ||
                t_network.getNic() == null ||
                    t_network.getNic().length == 0) {
            net = null;
        } else {
            net = new Networking(t_network);
        }

        final LogisticsWrapper wrappa = new LogisticsWrapper();
        wrappa.setNetworking(net);
        return wrappa;
    }
}
