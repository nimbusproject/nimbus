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

package org.nimbustools.messaging.gt4_0.group;

import org.nimbustools.messaging.gt4_0.GeneralPurposeResource;
import org.nimbustools.messaging.gt4_0.generated.types.ShutdownEnumeration;
import org.nimbustools.messaging.gt4_0.generated.negotiable.PostShutdown_Type;
import org.nimbustools.api.services.rm.Manager;
import org.nimbustools.api.services.rm.ManageException;
import org.nimbustools.api.services.rm.DoesNotExistException;
import org.nimbustools.api.services.rm.OperationDisabledException;
import org.nimbustools.api.repr.ShutdownTasks;
import org.nimbustools.api.repr.Caller;
import org.globus.wsrf.config.ConfigException;

import java.net.URISyntaxException;

public class GroupResource extends GeneralPurposeResource {

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected GroupTranslate translate;
    
    
    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    /**
     * @param idString instance key
     * @param manager Manager impl
     * @param secDescPath path to resource-security descriptor template
     * @param translate move between GT/Axis1 and VWS
     * @throws ConfigException problem with secDescPath
     * @throws DoesNotExistException gone (race with a destroyer)
     * @throws ManageException general problem
     */
    public GroupResource(String idString,
                         Manager manager,
                         String secDescPath,
                         GroupTranslate translate)
            throws ConfigException, ManageException, DoesNotExistException {

        super(idString, Manager.GROUP, manager, secDescPath, translate);

        if (translate == null) {
            throw new IllegalArgumentException("translate may not be null");
        }
        this.translate = translate;
    }


    // -------------------------------------------------------------------------
    // REMOTE CLIENT INVOCATIONS - MUTATIVE
    // -------------------------------------------------------------------------

    /**
     * Only invoked from WS client via WorkspaceGroupService
     * @param callerDN caller name
     * @throws OperationDisabledException operation is disabled
     * @throws ManageException general
     * @throws DoesNotExistException gone (race with a destroyer)
     */
    public void start(String callerDN) throws OperationDisabledException,
                                              ManageException,
                                              DoesNotExistException {

        final Caller caller = this.translate.getCaller(callerDN);
        this.manager.start(this.id, Manager.GROUP, caller);
    }


    /**
     * Only invoked from WS client via WorkspaceGroupService
     *
     * @param shutdownEnum  Type of shutdown event
     * @param postTask      possible request adjustment
     * @param appendID      if there is a new unprop target, append ID to URL?
     *                      (used to differentiate group files)
     * @param callerDN      caller name
     * @throws OperationDisabledException operation is disabled
     * @throws ManageException general
     * @throws DoesNotExistException gone (race with a destroyer)
     * @throws URISyntaxException problem with request
     */
    public void shutdown(ShutdownEnumeration shutdownEnum,
                         PostShutdown_Type postTask,
                         boolean appendID,
                         String callerDN) throws OperationDisabledException,
                                                 ManageException,
                                                 DoesNotExistException,
                                                 URISyntaxException {

        final Caller caller = this.translate.getCaller(callerDN);

        final ShutdownTasks tasks =
                this.translate.getShutdownTasks(postTask, appendID);

        if (ShutdownEnumeration.Normal.equals(shutdownEnum)) {
            this.manager.shutdown(this.id, Manager.GROUP, tasks, caller);
        } else if (ShutdownEnumeration.Pause.equals(shutdownEnum)) {
            this.manager.pause(this.id, Manager.GROUP, tasks, caller);
        } else if (ShutdownEnumeration.ReadyForTransport.equals(shutdownEnum)) {
            this.manager.shutdownSave(this.id, Manager.GROUP, tasks, caller);
        } else if (ShutdownEnumeration.Reboot.equals(shutdownEnum)) {
            this.manager.reboot(this.id, Manager.GROUP, tasks, caller);
        } else if (ShutdownEnumeration.Serialize.equals(shutdownEnum)) {
            this.manager.serialize(this.id, Manager.GROUP, tasks, caller);
        }
    }
}
