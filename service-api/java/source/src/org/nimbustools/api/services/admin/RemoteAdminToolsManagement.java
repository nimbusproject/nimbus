/**
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
 *
 */
package org.nimbustools.api.services.admin;


import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * This interface handles all the work done by RemoteAdminToolsMain and is mapped to the service over rmi
 */
public interface RemoteAdminToolsManagement extends Remote {

    public static final int SHUTDOWN_ALL = 0;
    public static final int SHUTDOWN_ID = 1;
    public static final int SHUTDOWN_HOST = 2;

    public String getAllRunningVMs() throws RemoteException;
    public String getVMsByDN(String userDN) throws RemoteException;
    public String getVMsByUser(String user) throws RemoteException;
    public String getAllVMsByHost(String hostname) throws RemoteException;
    public String getAllVMsByGroup(String groupId) throws RemoteException;
    public String shutdown(int type, String typeID, String seconds) throws RemoteException;
}
