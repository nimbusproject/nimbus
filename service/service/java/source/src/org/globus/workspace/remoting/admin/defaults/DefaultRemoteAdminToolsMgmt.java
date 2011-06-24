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
package org.globus.workspace.remoting.admin.defaults;

import com.google.gson.Gson;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.workspace.remoting.admin.VMTranslation;
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
import java.util.List;

public class DefaultRemoteAdminToolsMgmt implements RemoteAdminToolsManagement {

    protected static final Log logger =
            LogFactory.getLog(DefaultRemoteAdminToolsMgmt.class.getName());

    protected Manager manager;
    protected ReprFactory reprFactory;
    protected DataSource authzDataSource;
    private AuthzDBAdapter authz;

    private final Gson gson;

    public DefaultRemoteAdminToolsMgmt() {
        this.gson = new Gson();
    }

    public void initialize() throws Exception {
    }

    public String getAllRunningVMs() throws RemoteException {
        try {
            VM[] allRunningVms = manager.getGlobalAll();
            final List<VMTranslation> vmts = new ArrayList<VMTranslation>(allRunningVms.length);
            for(int i = 0; i < allRunningVms.length; i++) {
                vmts.add(translateVM(allRunningVms[i]));
            }
            return gson.toJson(vmts);
        }
        catch (ManageException e) {
            throw new RemoteException(e.getMessage());
        }
    }

    public String getVMsByUser(String user) throws RemoteException {
        try {
            authz = new AuthzDBAdapter(authzDataSource);
            String userId = authz.getCanonicalUserIdFromFriendlyName(user);

            List<UserAlias> userAlias;
            userAlias = authz.getUserAliases(userId);
            if(userAlias.size() == 0)
                return null;

            final List<VMTranslation> vmts = new ArrayList<VMTranslation>();

            for(int i = 0; i < userAlias.size(); i++) {
                String aliasDN = userAlias.get(i).getAliasName();
                final _Caller caller = this.reprFactory._newCaller();
                caller.setIdentity(aliasDN);
                VM[] vmsByCaller = manager.getAllByCaller(caller);
                for(int j = 0; j < vmsByCaller.length; j++) {
                    vmts.add(translateVM(vmsByCaller[j]));
                }

            }
            return gson.toJson(vmts);
        }
        catch (AuthzDBException e) {
            throw new RemoteException(e.getMessage());
        }
        catch (ManageException e) {
            throw new RemoteException(e.getMessage());
        }
    }

    public String shutdownAllVMs(String seconds) throws RemoteException {
        try {
            VM[] allRunning = manager.getGlobalAll();
            if(allRunning.length == 0)
                return "No VMs currently running";

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
            return null;
        }
        catch (DoesNotExistException e) {
            throw new RemoteException(e.getMessage());
        }
        catch (ManageException e) {
            throw new RemoteException(e.getMessage());
        }
        catch (OperationDisabledException e) {
            throw new RemoteException(e.getMessage());
        }
        catch (InterruptedException e) {
            throw new RemoteException(e.getMessage());
        }
    }

    public String shutdownVM(String id, String seconds) throws RemoteException {
        try {
            if(!manager.exists(id, manager.INSTANCE))
                return "VM ID: " + id + " does not exist";

            VM instance = manager.getInstance(id);
            Caller caller = instance.getCreator();
            manager.shutdown(id, manager.INSTANCE, null, caller);

            if(seconds == null) {
                for(int i = 0; i <= 10; i++) {
                    Thread.sleep(3000);
                    instance = manager.getInstance(id);
                    if(instance.getState().getState().equals("Propagated")) {
                        manager.trash(id, manager.INSTANCE, caller);
                        return null;
                    }
                }
                manager.trash(id, manager.INSTANCE, caller);
                return null;
            }
            else {
                int mill = (Integer.parseInt(seconds)) * 1000;
                for(int i = 0; i <= mill; i += 3000) {
                    Thread.sleep(3000);
                    instance = manager.getInstance(id);
                    if(instance.getState().getState().equals("Propagated")) {
                           manager.trash(id, manager.INSTANCE, caller);
                           return null;
                    }
                }
                manager.trash(id, manager.INSTANCE, caller);
                return null;
            }
        }
        catch (DoesNotExistException d) {
            return "VM " + id + " does not exist";
        }
        catch (ManageException e) {
            throw new RemoteException(e.getMessage());
        }
        catch (OperationDisabledException e) {
            throw new RemoteException(e.getMessage());
        }
        catch (InterruptedException e) {
            throw new RemoteException(e.getMessage());
        }
    }

    private VMTranslation translateVM(VM vm) {
        String id = vm.getID();
        String groupId = vm.getGroupID();
        String creatorId = vm.getCreator().getIdentity();
        String state = vm.getState().getState();
        VMTranslation vmt = new VMTranslation(id, groupId, creatorId, state);
        return vmt;
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
