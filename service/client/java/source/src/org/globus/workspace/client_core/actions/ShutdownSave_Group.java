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
import org.globus.workspace.client_core.ExecutionProblem;
import org.globus.workspace.client_core.ParameterProblem;
import org.globus.workspace.client_core.StubConfigurator;
import org.globus.workspace.client_core.WSAction_Group;
import org.globus.workspace.common.print.Print;
import org.globus.workspace.client_core.utils.RMIUtils;
import org.nimbustools.messaging.gt4_0.generated.group.WorkspaceGroupPortType;
import org.nimbustools.messaging.gt4_0.generated.types.ShutdownEnumeration;
import org.nimbustools.messaging.gt4_0.generated.types.WorkspaceShutdownFault;
import org.nimbustools.messaging.gt4_0.generated.types.OperationDisabledFault;
import org.nimbustools.messaging.gt4_0.generated.types.WorkspaceShutdownRequest_Type;
import org.nimbustools.messaging.gt4_0.generated.types.WorkspaceUnknownFault;
import org.nimbustools.messaging.gt4_0.generated.negotiable.PostShutdown_Type;

import java.rmi.RemoteException;

public class ShutdownSave_Group extends WSAction_Group {


    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private PostShutdown_Type postShutdownRequest;


    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    /**
     * @see WSAction_Group
     */
    public ShutdownSave_Group(EndpointReferenceType epr,
                              StubConfigurator stubConf,
                              Print debug) {
        super(epr, stubConf, debug);
    }

    /**
     * @see WSAction_Group
     */
    public ShutdownSave_Group(WorkspaceGroupPortType groupPortType,
                              Print debug) {
        super(groupPortType, debug);
    }


    // -------------------------------------------------------------------------
    // GET/SET
    // -------------------------------------------------------------------------

    public PostShutdown_Type getPostShutdownRequest() {
        return this.postShutdownRequest;
    }

    public void setPostShutdownRequest(PostShutdown_Type request) {
        this.postShutdownRequest = request;
    }

    // -------------------------------------------------------------------------
    // EXECUTE
    // -------------------------------------------------------------------------

    /**
     * Calls shutdownSave()
     * 
     * @return null
     * @throws Exception see shutdownSave()
     * @see #shutdownSave()
     */
    protected Object action() throws Exception {
        this.shutdownSave();
        return null;
    }

    /**
     * Shutdown-save group of workspaces.
     *
     * @throws ParameterProblem validation problem
     * @throws ExecutionProblem general problem running (connection errors etc)
     * @throws WorkspaceShutdownFault couldn't shutdown-save
     * @throws OperationDisabledFault not allowed to shutdown-save
     * @throws WorkspaceUnknownFault unknown
     */
    public void shutdownSave() throws ParameterProblem,
                                      ExecutionProblem,
                                      WorkspaceShutdownFault,
                                      OperationDisabledFault,
                                      WorkspaceUnknownFault {

        this.validateAll();

        try {

            final WorkspaceShutdownRequest_Type req =
                    new WorkspaceShutdownRequest_Type();
            req.setShutdownType(ShutdownEnumeration.ReadyForTransport);

            if (this.postShutdownRequest != null) {
                req.setPostShutdown(this.postShutdownRequest);
            }

            ((WorkspaceGroupPortType) this.portType).shutdown(req);

        } catch (WorkspaceUnknownFault e) {
            throw e;
        } catch (WorkspaceShutdownFault e) {
            throw e;
        } catch (OperationDisabledFault e) {
            throw e;
        } catch (RemoteException e) {
            throw RMIUtils.generalRemoteException(e);
        }
    }
}
