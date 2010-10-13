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

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteNodeManagement extends Remote {
    //Create
    public String addNodes(String nodeJson) throws RemoteException;

    //Read
    public String listNodes() throws RemoteException;
    public String getNode(String hostname) throws RemoteException;

    //Update

    // this rather sucks. null values mean no update of that field.
    // but all fields need to be part of signature, so this won't
    // scale to well. also it is not parallel with how howNodes()
    // works
    public String updateNodes(String[] hostnames,
                              Boolean active,
                              String pool,
                              Integer memory,
                              String networks)
            throws RemoteException;

    //Delete
    public String removeNodes(String[] hostnames) throws RemoteException;
}
