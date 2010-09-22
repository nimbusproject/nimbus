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
import org.nimbustools.messaging.gt4_0.common.InvalidDurationException;
import org.nimbustools.messaging.gt4_0.common.Constants_GT4_0;
import org.globus.workspace.client_core.ExecutionProblem;
import org.globus.workspace.client_core.ParameterProblem;
import org.globus.workspace.client_core.StubConfigurator;
import org.globus.workspace.client_core.WSAction_Instance;
import org.globus.workspace.common.print.Print;
import org.globus.workspace.client_core.repr.Networking;
import org.globus.workspace.client_core.repr.State;
import org.globus.workspace.client_core.repr.Workspace;
import org.globus.workspace.client_core.repr.Schedule;
import org.nimbustools.messaging.gt4_0.generated.ServiceRPSet;
import org.nimbustools.messaging.gt4_0.generated.WorkspacePortType;
import org.nimbustools.messaging.gt4_0.generated.metadata.logistics.Logistics;
import org.nimbustools.messaging.gt4_0.generated.metadata.logistics.VirtualNetwork_Type;
import org.nimbustools.messaging.gt4_0.generated.types.CurrentState;
import org.nimbustools.messaging.gt4_0.generated.types.Schedule_Type;
import org.globus.wsrf.WSRFConstants;
import org.oasis.wsrf.properties.QueryExpressionType;
import org.oasis.wsrf.properties.QueryResourcePropertiesResponse;
import org.oasis.wsrf.properties.QueryResourceProperties_Element;
import org.oasis.wsrf.properties.ResourceUnknownFaultType;

import java.io.IOException;

public class RPQueryInstance extends WSAction_Instance {


    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    /**
     * @see WSAction_Instance
     */
    public RPQueryInstance(EndpointReferenceType epr,
                           StubConfigurator stubConf,
                           Print debug) {
        super(epr, stubConf, debug);
    }

    /**
     * @see WSAction_Instance
     */
    public RPQueryInstance(WorkspacePortType instancePortType,
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
     * Workspace object created with currentState, EPR, currentNetworking
     *
     * TODO: add more
     *
     * @return Workspace never null
     * @throws ParameterProblem validation problem
     * @throws ExecutionProblem problem running
     * @throws ResourceUnknownFaultType gone
     */
    public Workspace queryOnce() throws ParameterProblem,
                                        ExecutionProblem,
                                        ResourceUnknownFaultType {

        this.validateAll();

        try {
            return this._queryOnce();
        } catch (ResourceUnknownFaultType e) {
            throw e;
        } catch (Exception e) {
            final String err = "Problem querying resource properties: ";
            throw new ExecutionProblem(err + e.getMessage(), e);
        }
    }

    private Workspace _queryOnce() throws Exception {

        // return the entire RP document
        final String queryStr = "/";

        final QueryResourceProperties_Element query =
                new QueryResourceProperties_Element();

        query.setQueryExpression(
             createQueryExpression(WSRFConstants.XPATH_1_DIALECT,
                   queryStr));

        final QueryResourcePropertiesResponse resp =
                ((WorkspacePortType)this.portType).
                                            queryResourceProperties(query);

        final ServiceRPSet rpSet = (ServiceRPSet) resp.get_any()[0].
                        getValueAsType(Constants_GT4_0.RP_SET,
                                       ServiceRPSet.class);

        if (rpSet == null) {
            throw new ExecutionProblem("No factory RP set returned");
        }
        return convert(rpSet, this.epr);
    }

    /**
     * @param rpSet may not be null
     * @param endpoint epr of workspace
     * @return Workspace representation
     */
    public static Workspace convert(ServiceRPSet rpSet,
                                    EndpointReferenceType endpoint)
            throws InvalidDurationException {

        if (rpSet == null) {
            throw new IllegalArgumentException("rpSet may not be null");
        }

        final Workspace workspace = new Workspace();

        final CurrentState curr = rpSet.getCurrentState();

        workspace.setCurrentState(State.fromCurrentState_Type(curr));

        final Logistics log = rpSet.getLogistics();

        final VirtualNetwork_Type t_network = log.getNetworking();

        if (t_network != null &&
                t_network.getNic() != null &&
                    t_network.getNic().length > 0) {
            workspace.setCurrentNetworking(new Networking(t_network));
        }

        workspace.setEpr(endpoint);

        final Schedule_Type xmlSched = rpSet.getSchedule();
        if (xmlSched != null) {
            final Schedule schedule = new Schedule(xmlSched);
            workspace.setCurrentSchedule(schedule);
        }

        return workspace;
    }

    private static QueryExpressionType createQueryExpression(String dialect,
                                                             String queryString)
            throws IOException {

        final QueryExpressionType query = new QueryExpressionType();
        query.setDialect(dialect);

        if (queryString != null) {
            query.setValue(queryString);
        }

        return query;
    }
}
