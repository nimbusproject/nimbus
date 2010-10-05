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
package org.globus.workspace.remoting.admin;

import java.io.IOException;
import java.rmi.Remote;

public interface RemoteNodePool extends Remote {
    //Create
    public void addNodes(String nodeJson) throws IOException;

    //Read
    public String listNodes() throws IOException;
    public String getNode(String hostname) throws IOException;

    //Update
    public void updateNodes(String nodeJson) throws IOException;

    //Delete
    public void removeNodes(String[] hostnames) throws IOException;
}
