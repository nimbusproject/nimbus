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

import org.globus.workspace.client_core.WSAction_Status;
import org.globus.workspace.client_core.ParameterProblem;
import org.globus.workspace.client_core.ExecutionProblem;
import org.globus.workspace.client_core.StubConfigurator;
import org.globus.workspace.common.print.Print;
import org.globus.workspace.client_core.utils.RMIUtils;
import org.globus.workspace.client_core.repr.Workspace;
import org.globus.workspace.client_core.repr.State;
import org.globus.workspace.client_core.repr.Networking;
import org.globus.workspace.client_core.repr.Schedule;
import org.nimbustools.messaging.gt4_0.generated.status.CurrentWorkspaces_Type;
import org.nimbustools.messaging.gt4_0.generated.status.VoidType;
import org.nimbustools.messaging.gt4_0.generated.status.WorkspaceStatusPortType;
import org.nimbustools.messaging.gt4_0.generated.status.WorkspaceStatusFault;
import org.nimbustools.messaging.gt4_0.generated.status.OneCurrentWorkspace_Type;
import org.nimbustools.messaging.gt4_0.generated.types.CurrentState;
import org.nimbustools.messaging.gt4_0.generated.types.Schedule_Type;
import org.nimbustools.messaging.gt4_0.generated.metadata.logistics.Logistics;
import org.nimbustools.messaging.gt4_0.generated.metadata.logistics.VirtualNetwork_Type;
import org.nimbustools.messaging.gt4_0.common.InvalidDurationException;
import org.apache.axis.message.addressing.EndpointReferenceType;

import java.rmi.RemoteException;

public class Status_QueryAll extends WSAction_Status {

    
    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Workspace[] NO_WORKSPACES = new Workspace[0];

    
    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    public Status_QueryAll(EndpointReferenceType epr,
                           StubConfigurator stubConf,
                           Print debug) {
        super(epr, stubConf, debug);
    }

    public Status_QueryAll(WorkspaceStatusPortType statusPortType,
                           Print debug) {
        super(statusPortType, debug);
    }


    // -------------------------------------------------------------------------
    // EXECUTE
    // -------------------------------------------------------------------------

    /**
     * Calls queryAll()
     *
     * @return Workspace[], never null but could be length zero
     * @throws Exception see queryAll()
     * @see #queryAll()
     */
    protected Object action() throws Exception {
        this.queryAll();
        return null;
    }

    public Workspace[] queryAll() throws WorkspaceStatusFault,
                                         ExecutionProblem,
                                         ParameterProblem {

        this.validateAll();
        
        try {
            return this._queryAll();
        } catch (WorkspaceStatusFault e) {
            throw e;
        } catch (RemoteException e) {
            throw RMIUtils.generalRemoteException(e);
        }
    }

    private Workspace[] _queryAll() throws ExecutionProblem,
                                           RemoteException {

        final CurrentWorkspaces_Type current =
                ((WorkspaceStatusPortType)this.portType)
                                .queryCurrentWorkspaces(new VoidType());

        try {
            return convert(current);
        } catch (InvalidDurationException e) {
            final String err = "Problem converting query result: ";
            throw new ExecutionProblem(err + e.getMessage(), e);
        }
    }

    public static Workspace[] convert(CurrentWorkspaces_Type current)
        throws InvalidDurationException {

        if (current == null) {
            return NO_WORKSPACES;
        }

        final OneCurrentWorkspace_Type[] crs = current.getOneCurrentWorkspace();
        if (crs == null || crs.length == 0) {
            return NO_WORKSPACES;
        }

        final Workspace[] workspaces = new Workspace[crs.length];

        for (int i = 0; i < crs.length; i++) {
            workspaces[i] = convertOne(crs[i]);
        }

        return workspaces;
    }

    public static Workspace convertOne(OneCurrentWorkspace_Type oneCurrent)
            throws InvalidDurationException {

        if (oneCurrent == null) {
            throw new IllegalArgumentException("oneCurrent may not be null");
        }

        final Workspace workspace = new Workspace();

        final CurrentState curr = oneCurrent.getCurrentState();

        workspace.setCurrentState(State.fromCurrentState_Type(curr));

        final Logistics log = oneCurrent.getLogistics();

        final VirtualNetwork_Type t_network = log.getNetworking();

        if (t_network != null &&
                t_network.getNic() != null &&
                    t_network.getNic().length > 0) {
            workspace.setCurrentNetworking(new Networking(t_network));
        }

        workspace.setEpr(oneCurrent.getEpr());

        final Schedule_Type xmlSched = oneCurrent.getSchedule();
        if (xmlSched != null) {
            final Schedule schedule = new Schedule(xmlSched);
            workspace.setCurrentSchedule(schedule);
        }

        return workspace;
    }
}
