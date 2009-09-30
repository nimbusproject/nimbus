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

package org.globus.workspace.service;

import org.nimbustools.api.services.rm.ManageException;
import org.nimbustools.api.services.rm.OperationDisabledException;
import org.nimbustools.api.services.rm.DoesNotExistException;
import org.nimbustools.api.repr.ShutdownTasks;

public interface GroupResource {

    // -------------------------------------------------------------------------
    // GENERAL / MGMT
    // -------------------------------------------------------------------------

    public String getID();

    public void setID(String groupid);

    public String getCreatorID();

    public void setCreatorID(String creator);

    /**
     * Don't call unless you are managing the instance cache (or not using
     * one, perhaps). 
     * 
     * @throws ManageException problem
     * @throws DoesNotExistException missing
     */
    public void remove()

            throws ManageException, DoesNotExistException;

    
    // -------------------------------------------------------------------------
    // ACTIONS
    // -------------------------------------------------------------------------

    public void start()

            throws ManageException, DoesNotExistException,
                   OperationDisabledException;

    public void shutdown(ShutdownTasks tasks)

            throws ManageException, DoesNotExistException,
                   OperationDisabledException;

    public void shutdownSave(ShutdownTasks tasks)

            throws ManageException, DoesNotExistException,
                   OperationDisabledException;

    public void pause(ShutdownTasks tasks)

            throws ManageException, DoesNotExistException,
                   OperationDisabledException;

    public void serialize(ShutdownTasks tasks)

            throws ManageException, DoesNotExistException,
                   OperationDisabledException;

    public void reboot(ShutdownTasks tasks)

            throws ManageException, DoesNotExistException,
                   OperationDisabledException;
}
