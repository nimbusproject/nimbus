package org.globus.workspace.remoting.admin.defaults;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nimbus.authz.AuthzDBException;
import org.nimbus.authz.UserAlias;
import org.nimbustools.api._repr._Caller;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.ReprFactory;
import org.nimbustools.api.repr.vm.VM;
import org.nimbustools.api.services.admin.RemoteAdminToolsManagement;
import org.nimbustools.api.services.rm.DoesNotExistException;
import org.nimbustools.api.services.rm.ManageException;
import org.nimbustools.api.services.rm.Manager;
import org.nimbus.authz.AuthzDBAdapter;
import org.nimbustools.api.services.rm.OperationDisabledException;

import javax.sql.DataSource;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

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
 * User: rrusnak
 */
public class DefaultRemoteAdminToolsMgmt implements RemoteAdminToolsManagement {
    private static final Log logger =
            LogFactory.getLog(DefaultRemoteAdminToolsMgmt.class.getName());

    protected Manager manager;
    protected ReprFactory reprFactory;
    protected DataSource authzDataSource;
    private AuthzDBAdapter authz;


    public void initialize() throws Exception {
    }

    public Hashtable getAllRunningVMs() throws RemoteException {
        try {
            VM[] allRunningVms = manager.getGlobalAll();
            return returnVMs(allRunningVms);
        }
        catch (ManageException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Hashtable getVMsByUser(String user) throws RemoteException {
        try {
            authz = new AuthzDBAdapter(authzDataSource);
            String userId = authz.getCanonicalUserIdFromFriendlyName(user);

            List<UserAlias> userAlias;
            userAlias = authz.getUserAliases(userId);
            if(userAlias.size() == 0)
                return null;

            Hashtable returnedVMs = new Hashtable(userAlias.size());
            for(int i = 0; i < userAlias.size(); i++) {
                String aliasDN = userAlias.get(i).getAliasName();
                final _Caller caller = this.reprFactory._newCaller();
                caller.setIdentity(aliasDN);
                VM[] vmsByCaller = manager.getAllByCaller(caller);
                returnedVMs.putAll(returnVMs(vmsByCaller));
            }
            return returnedVMs;
        }
        catch (AuthzDBException e) {
            e.printStackTrace();
            return null;
        }
        catch (ManageException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String shutdownAllVMs(String seconds) throws RemoteException {
        try {
            VM[] allRunning = manager.getGlobalAll();
            for(int i = 0; i < allRunning.length; i++) {
                String id = allRunning[i].getID();
                Caller caller = allRunning[i].getCreator();
                manager.shutdown(id, manager.INSTANCE, null, caller);
            }

            if(seconds == null) {
                for(int i = 0; i <= 10; i++) {
                    Thread.sleep(3000);
                    allRunning = manager.getGlobalAll();
                    if(allRunning[0].getState().getState().equals("Propagated"))
                        break;
                }
            }
            else {
                int mill = (Integer.parseInt(seconds)) * 1000;
                for(int i = 0; i <= mill; i += 3000) {
                    Thread.sleep(3000);
                    allRunning = manager.getGlobalAll();
                    if(allRunning[0].getState().getState().equals("Propagated"))
                        break;
                }
            }

            allRunning = manager.getGlobalAll();
            for(int i = 0; i < allRunning.length; i++) {
                String id = allRunning[i].getID();
                Caller caller = allRunning[i].getCreator();
                manager.trash(id, manager.INSTANCE, caller);
            }
            return "All VMs successfully shut down";
        }
        catch (DoesNotExistException d) {
            d.printStackTrace();
            return null;
        }
        catch (ManageException e) {
            e.printStackTrace();
            return null;
        }
        catch (OperationDisabledException e) {
            e.printStackTrace();
            return null;
        }
        catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String shutdownVM(String id, String seconds) throws RemoteException {
        try {
            VM instance = manager.getInstance(id);
            Caller caller = instance.getCreator();
            manager.shutdown(id, manager.INSTANCE, null, caller);

            if(seconds == null) {
                for(int i = 0; i <= 10; i++) {
                    Thread.sleep(3000);
                    instance = manager.getInstance(id);
                    if(instance.getState().getState().equals("Propagated")) {
                        manager.trash(id, manager.INSTANCE, caller);
                        return "VM " + id + " shutdown";
                    }
                }
                manager.trash(id, manager.INSTANCE, caller);
                return "VM " + id + " shutdown";
            }
            else {
                int mill = (Integer.parseInt(seconds)) * 1000;
                for(int i = 0; i <= mill; i += 3000) {
                    Thread.sleep(3000);
                    instance = manager.getInstance(id);
                    if(instance.getState().getState().equals("Propagated")) {
                           manager.trash(id, manager.INSTANCE, caller);
                           return "VM " + id + " shutdown";
                    }
                }
                manager.trash(id, manager.INSTANCE, caller);
                return "VM " + id + " shutdown";
            }
        }
        catch (DoesNotExistException d) {
            return "VM " + id + " does not exist";
        }
        catch (ManageException e) {
            e.printStackTrace();
            return "ManageException thrown";
        }
        catch (OperationDisabledException e) {
            e.printStackTrace();
            return "OperationDisabledException thrown";
        }
        catch (InterruptedException e) {
            e.printStackTrace();
            return "InterruptedException thrown";
        }
    }

    public String test(String user) throws RemoteException {
        try {
            authz = new AuthzDBAdapter(authzDataSource);
            String userId = authz.getCanonicalUserIdFromFriendlyName(user);

            List<UserAlias> userAlias;
            userAlias = authz.getUserAliases(userId);
            if(userAlias.size() == 0)
                return null;

            String aliasDN = userAlias.get(0).getAliasName();
            final _Caller caller = this.reprFactory._newCaller();
            caller.setIdentity(aliasDN);
            return caller.getIdentity();
        }
        catch (AuthzDBException e) {
            e.printStackTrace();
            return null;
        }
        //catch (ManageException e) {
            //e.printStackTrace();
          //  return null;
        //}
    }

    private Hashtable returnVMs(VM[] vms) {
        int vmSize = vms.length;
            Hashtable ht = new Hashtable(vmSize);

            for(int i = 0; i < vmSize; i++) {
                ArrayList al = new ArrayList();
                String id = vms[i].getID();
                Caller caller = vms[i].getCreator();

                al.add(vms[i].getGroupID());
                al.add(vms[i].getCreator().getIdentity());
                al.add(vms[i].getState().getState());
                ht.put(id, al);
            }
            return ht;
    }

    public void setManager(Manager manager) {
        this.manager = manager;
    }

    public void setReprFactory(ReprFactory reprFactory) {
        this.reprFactory = reprFactory;
    }

    public void setAuthzDataSource(DataSource authzDataSource) {
        this.authzDataSource = authzDataSource;
    }
}
