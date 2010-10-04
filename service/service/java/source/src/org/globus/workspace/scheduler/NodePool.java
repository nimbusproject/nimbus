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

import java.util.Collection;
import java.util.List;

public interface NodePool {

    //Create
    public void addNode(VmmNode node);
    public void addNodes(Collection<VmmNode> nodes);

    //Read
    public List<VmmNode> listNodes();
    public VmmNode getNode(String hostname);

    //Update
    public void updateNode(VmmNode node);

    //Delete
    public void removeNode(String hostname);
    public void removeNodes(Collection<String> hostnames);
}
