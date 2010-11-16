/*
 * Copyright 1999-2010 University of Chicago
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
package org.globus.workspace.scheduler;

import org.globus.workspace.persistence.WorkspaceDatabaseException;
import org.globus.workspace.scheduler.defaults.ResourcepoolEntry;

import java.util.List;

public interface NodeManagement {

    //Create
    public ResourcepoolEntry addNode(String hostname,
                                     String pool,
                                     String networks,
                                     int memory,
                                     boolean active)
            throws NodeExistsException, NodeManagementDisabled, WorkspaceDatabaseException;

    //Read
    public List<ResourcepoolEntry> getNodes()
            throws NodeManagementDisabled, WorkspaceDatabaseException;
    public ResourcepoolEntry getNode(String hostname)
            throws NodeManagementDisabled, WorkspaceDatabaseException;

    //Update

    /**
     * Updates an existing pool entry.
     *
     * Null values for any of the parameters mean no update to that field.
     * But at least one field must be specified.
     * @param hostname the node to be updated, required
     * @param pool the new resourcepool name, can be null
     * @param networks the new networks association list, can be null
     * @param memory the new max memory value for the node, can be null
     * @param active the new active state for the node, can be null
     * @return the updated ResourcepoolEntry
     * @throws NodeInUseException node was in use and could not be updated
     * @throws NodeNotFoundException node wasn't found
     */
    public ResourcepoolEntry updateNode(String hostname,
                              String pool,
                              String networks,
                              Integer memory,
                              Boolean active)
            throws NodeInUseException, NodeNotFoundException,
                   NodeManagementDisabled, WorkspaceDatabaseException;

    //Delete                                                            `
    public boolean removeNode(String hostname)
            throws NodeInUseException, NodeManagementDisabled, WorkspaceDatabaseException;
}
