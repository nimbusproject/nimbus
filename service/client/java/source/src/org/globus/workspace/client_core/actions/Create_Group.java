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

import org.globus.workspace.client_core.Action;
import org.globus.workspace.client_core.ParameterProblem;
import org.globus.workspace.client_core.ExecutionProblem;
import org.globus.workspace.client_core.StubConfigurator;
import org.globus.workspace.common.print.Print;
import org.globus.workspace.client_core.repr.Workspace;
import org.globus.workspace.client_core.repr.Group;
import org.globus.workspace.client_core.repr.Schedule;
import org.globus.workspace.client_core.repr.Networking;
import org.nimbustools.messaging.gt4_0.generated.types.WorkspaceResourceRequestDeniedFault;
import org.nimbustools.messaging.gt4_0.generated.types.WorkspaceSchedulingFault;
import org.nimbustools.messaging.gt4_0.generated.types.WorkspaceMetadataFault;
import org.nimbustools.messaging.gt4_0.generated.types.WorkspaceCreationFault;
import org.nimbustools.messaging.gt4_0.generated.types.WorkspaceCreateResponse_Type;
import org.nimbustools.messaging.gt4_0.generated.types.CreatedWorkspace_Type;
import org.nimbustools.messaging.gt4_0.generated.types.Schedule_Type;
import org.nimbustools.messaging.gt4_0.generated.types.WorkspaceContextualizationFault;
import org.nimbustools.messaging.gt4_0.generated.ensemble.WorkspaceEnsembleFault;
import org.nimbustools.messaging.gt4_0.generated.WorkspaceFactoryPortType;
import org.nimbustools.messaging.gt4_0.generated.metadata.logistics.VirtualNetwork_Type;
import org.nimbustools.messaging.gt4_0.common.InvalidDurationException;
import org.apache.axis.message.addressing.EndpointReferenceType;

import java.util.ArrayList;

/**
 * See Action class notes for usage information.
 *
 * @see Action
 *
 * @see Create
 *
 * @see Workspace for notes on what will be populated for you after creation
 *
 * @see Group for notes on what will be populated for you after creation
 *
 */
public class Create_Group extends Create {

    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    /**
     * @see Create
     */
    public Create_Group(StubConfigurator stubConf,
                        Print debug) {
        super(stubConf, debug);
    }

    /**
     * @see Create
     */
    public Create_Group(EndpointReferenceType epr,
                        StubConfigurator stubConf,
                        Print debug) {
        super(epr, stubConf, debug);
    }

    /**
     * @see Create
     */
    public Create_Group(WorkspaceFactoryPortType factoryPortType,
                        Print debug) {
        super(factoryPortType, debug);
    }


    // -------------------------------------------------------------------------
    // VALIDATE
    // -------------------------------------------------------------------------

    /**
     * Will only accept requests with NodeNumber > 1
     *
     * @throws ParameterProblem issue that will stop creation attempt
     * @see Create#validateDeploymentRequest()
     * @see Create#validateDeploymentRequestNodeNumber()
     */
    protected void validateDeploymentRequestNodeNumber()
            throws ParameterProblem {

        final int nodeNum = (int) this.req.getNodeNumber();
        if (nodeNum < 2) {
            throw new ParameterProblem(
                    "this class only supports group requests, " +
                            "deployment request is asking for " + nodeNum);
        }
    }


    // -------------------------------------------------------------------------
    // EXECUTE
    // -------------------------------------------------------------------------

    protected Object action() throws Exception {
        return this.createGroup();
    }

    /**
     * Create workspace group.
     *
     * @return Group
     * @throws ParameterProblem validation problem
     * @throws ExecutionProblem general problem running (connection errors etc)
     * @throws WorkspaceResourceRequestDeniedFault request can not be fulfilled
     *         because of either lack of current resources or the credential
     *         is not authorized to get requested resources (this includes
     *         less familiar limited resources such as public IP addresses)
     * @throws WorkspaceSchedulingFault issue scheduling the request
     * @throws WorkspaceMetadataFault invalid metadata
     * @throws WorkspaceEnsembleFault problem with ensemble service interaction
     * @throws WorkspaceContextualizationFault problem from context broker
     * @throws WorkspaceCreationFault uncategorized factory request issue
     */
    public Group createGroup() throws ParameterProblem,
                                      ExecutionProblem,
                                      WorkspaceResourceRequestDeniedFault,
                                      WorkspaceSchedulingFault,
                                      WorkspaceMetadataFault,
                                      WorkspaceEnsembleFault,
                                      WorkspaceContextualizationFault,
                                      WorkspaceCreationFault {

        this.validateAll();
        final WorkspaceCreateResponse_Type response = this.createImpl();
        return this.handleGroupCreation(response);
    }

    protected Group handleGroupCreation(
                            WorkspaceCreateResponse_Type response)

            throws ExecutionProblem {

        // length and not-null checked already
        final CreatedWorkspace_Type[] allrefs = response.getCreatedWorkspace();

        final EndpointReferenceType groupEPR = response.getGroupEPR();
        final EndpointReferenceType ensembleEPR = response.getEnsembleEPR();
        final EndpointReferenceType contextEPR = response.getContextEPR();

        final ArrayList workspaceList = new ArrayList(allrefs.length);
        for (int i = 0; i < allrefs.length; i++) {

            final Workspace workspace = new Workspace();

            // these things are not based on any information returned
            try {
                this.populateInitialRepr(workspace);
            } catch (ParameterProblem e) {
                throw new ExecutionProblem(
                            "unexpected problem: " + e.getMessage(), e);
            }
            
            final Schedule_Type xmlSchedule = allrefs[i].getSchedule();
            if (xmlSchedule == null) {
                throw new ExecutionProblem(
                        "(?) no schedule in factory response");
            }

            try {
                workspace.setInitialSchedule(new Schedule(xmlSchedule));
                // this is intentionally a separate object:
                workspace.setCurrentSchedule(new Schedule(xmlSchedule));
            } catch (InvalidDurationException e) {
                throw new ExecutionProblem(
                        "(?) invalid data in factory response: " +
                                e.getMessage(), e);
            }

            final VirtualNetwork_Type xmlNetwork = allrefs[i].getNetworking();
            if (xmlNetwork != null) {
                workspace.setCurrentNetworking(new Networking(xmlNetwork));
            }

            workspace.setEpr(allrefs[i].getEpr());
            workspace.setGroupMemberEPR(groupEPR);
            workspace.setEnsembleMemberEPR(ensembleEPR);
            workspace.setContextMemberEPR(contextEPR);

            workspaceList.add(workspace);
        }

        final Group group = new Group();

        group.setGroupEPR(groupEPR);
        group.setEnsembleMemberEPR(ensembleEPR);
        group.setContextMemberEPR(contextEPR);

        final Workspace[] workspaces =
                (Workspace[]) workspaceList.toArray(
                                    new Workspace[workspaceList.size()]);

        group.setWorkspaces(workspaces);

        return group;
    }
    
}
