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

import org.globus.workspace.scheduler.defaults.ResourcepoolEntry;

import java.util.List;

public interface NodeManagement {

    //Create
    public ResourcepoolEntry addNode(String hostname,
                                     String pool,
                                     String associations,
                                     int memory)
            throws NodeExistsException;

    //Read
    public List<ResourcepoolEntry> getNodes();
    public ResourcepoolEntry getNode(String hostname);

    //Update
    public void updateNode(ResourcepoolEntry node)
            throws NodeInUseException;

    //Delete                                                            `
    public boolean removeNode(String hostname)
            throws NodeInUseException;
}
