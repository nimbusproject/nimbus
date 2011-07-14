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
import org.globus.workspace.groupauthz.Group;
import org.globus.workspace.groupauthz.GroupAuthz;
import org.globus.workspace.remoting.admin.VMTranslation;
import org.globus.workspace.service.WorkspaceHome;
import org.globus.workspace.service.binding.authorization.CreationAuthorizationCallout;
import org.globus.workspace.service.binding.vm.VirtualMachine;
import org.nimbus.authz.AuthzDBException;
import org.nimbus.authz.UserAlias;
import org.nimbustools.api._repr._Caller;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.ReprFactory;
import org.nimbustools.api.repr.vm.ResourceAllocation;
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

/**
 * Remote class that connects service and RemoteAdminToolsMain
 * Handles DB queries and information requests
 */
public class DefaultRemoteAdminToolsMgmt implements RemoteAdminToolsManagement {

    protected static final Log logger =
            LogFactory.getLog(DefaultRemoteAdminToolsMgmt.class.getName());

    protected Manager manager;
    protected ReprFactory reprFactory;
    protected DataSource authzDataSource;
    protected WorkspaceHome workspaceHome;
    protected CreationAuthorizationCallout authzCallout;
    private AuthzDBAdapter authz;

    private final Gson gson;
    private String errorMsg;

    public DefaultRemoteAdminToolsMgmt() {
        this.gson = new Gson();
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

    public String getAllVMsByHost(String hostname) throws RemoteException {
        VM[] vms = getVMByHost(hostname);

        if(vms == null)
            return null;

        final List<VMTranslation> vmts = new ArrayList<VMTranslation>(vms.length);
        for(int i = 0; i < vms.length; i++) {
            vmts.add(translateVM(vms[i]));
        }
        return gson.toJson(vmts);
    }

    public String getAllVMsByGroupId(String groupId) throws RemoteException {
        Group group = getGroupByGroupId(groupId);
        VM[] vms = getAllVMsByGroup(group);

        if(vms == null)
            return null;

        final List<VMTranslation> vmts = new ArrayList<VMTranslation>(vms.length);
        for(int i = 0; i < vms.length; i++) {
            vmts.add(translateVM(vms[i]));
        }
        return gson.toJson(vmts);
    }

    public String getAllVMsByGroupName(String groupName) throws RemoteException {
        Group group = getGroupByGroupName(groupName);
        VM[] vms = getAllVMsByGroup(group);

        if(vms == null)
            return null;

        final List<VMTranslation> vmts = new ArrayList<VMTranslation>(vms.length);
        for(int i = 0; i < vms.length; i++) {
            vmts.add(translateVM(vms[i]));
        }
        return gson.toJson(vmts);
    }

    public String getVMsByDN(String userDN) throws RemoteException {
        try {
            final _Caller caller = this.reprFactory._newCaller();
            caller.setIdentity(userDN);
            VM[] vms = manager.getAllByCaller(caller);
            final List<VMTranslation> vmts = new ArrayList<VMTranslation>(vms.length);
            for(int i = 0; i < vms.length; i++) {
                vmts.add(translateVM(vms[i]));
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
            VM[] vms = getVMsByUserId(userId);
            final List<VMTranslation> vmts = new ArrayList<VMTranslation>(vms.length);
            for(int i = 0; i < vms.length; i++) {
                vmts.add(translateVM(vms[i]));
            }
            return gson.toJson(vmts);
        }
        catch (AuthzDBException e) {
            return null;
        }
    }

    /*
     * This class handles shutdown by host, id and all.
     * The constants for int type are in the interface for this class
     * typeID is either the id or hostname, depending on if shutting down by id or host, or null if shutting down all
     * seconds is optional
     */
    public String shutdown(int type, String typeID, String seconds) throws RemoteException {
        try {
            VM[] vms;
            vms = typeSet(type, typeID);

            if(vms == null || vms.length == 0)
                return errorMsg;

            for(int i = 0; i < vms.length; i++) {
                String id = vms[i].getID();
                Caller caller = vms[i].getCreator();
                manager.shutdown(id, manager.INSTANCE, null, caller);
            }

            //checks every 3 seconds to see if one of the vms has entered propagation mode
            //up to a max of 30 seconds before trashing all vms
            //I decided against checking every single vm for entering propagation mode since they mostly enter at
            //about the same speed
            if(seconds == null) {
                for(int i = 0; i <= 10; i++) {
                    Thread.sleep(3000);
                    vms = typeSet(type, typeID);
                    if(vms[0].getState().getState().equals("Propagated"))
                        break;
                }
            }
            //same as above, but max time is the amount of seconds entered by the user
            else {
                int mill = (Integer.parseInt(seconds)) * 1000;
                for(int i = 0; i <= mill; i += 3000) {
                    Thread.sleep(3000);
                    vms = typeSet(type, typeID);
                    if(vms[0].getState().getState().equals("Propagated"))
                        break;
                }
            }

            //eventually trashes all vms regardless of whether or not they enter propagation mode
            vms = typeSet(type, typeID);

            for(int i = 0; i < vms.length; i++) {
                String id = vms[i].getID();
                Caller caller = vms[i].getCreator();
                manager.trash(id, manager.INSTANCE, caller);
            }
            return null;
        }
        catch (ManageException e) {
            throw new RemoteException(e.getMessage());
        }
        catch (DoesNotExistException e) {
            throw new RemoteException(e.getMessage());
        }
        catch (OperationDisabledException e) {
            throw new RemoteException(e.getMessage());
        }
        catch (InterruptedException e) {
            throw new RemoteException(e.getMessage());
        }
    }

    private VM[] typeSet(int type, String typeID) throws RemoteException {
        try {
            if(type == SHUTDOWN_HOST)
                return getVMByHost(typeID);
            else if(type == SHUTDOWN_ID)
                return getVMById(typeID);
            else if(type == SHUTDOWN_UNAME) {
                authz = new AuthzDBAdapter(authzDataSource);
                String userId = authz.getCanonicalUserIdFromFriendlyName(typeID);
                VM[] vms = getVMsByUserId(userId);
                if(vms == null)
                    errorMsg = "No VMs with user name " + typeID + " found";
                return vms;
            }
            else if(type == SHUTDOWN_DN) {
                final _Caller caller = this.reprFactory._newCaller();
                caller.setIdentity(typeID);
                VM[] vms = manager.getAllByCaller(caller);
                if(vms.length == 0)
                    errorMsg = "No VMs with DN " + typeID + " found";
                return vms;
            }
            else if(type == SHUTDOWN_GID) {
                Group group = getGroupByGroupId(typeID);
                if(group == null)
                    return null;
                VM[] vms = getAllVMsByGroup(group);
                if(vms == null)
                    errorMsg = "No VMs with group id " + typeID + " found";
                return vms;
            }
            else if(type == SHUTDOWN_GNAME) {
                Group group = getGroupByGroupName(typeID);
                if(group == null)
                    return null;
                VM[] vms = getAllVMsByGroup(group);
                if(vms == null)
                    errorMsg = "No VMs with group name " + typeID + " found";
                return vms;
            }
            else {
                VM[] vms = manager.getGlobalAll();
                if(vms.length == 0)
                    errorMsg = "No running VMs available for shutdown";
                return vms;
            }
        }
        catch (ManageException e) {
            throw new RemoteException(e.getMessage());
        }
        catch (AuthzDBException e) {
            errorMsg = "User " + typeID + " does not exist";
            return null;
        }
    }

    private VM[] getVMsByUserId(String userId) throws RemoteException {
        try {
            List<UserAlias> userAlias;
            userAlias = authz.getUserAliases(userId);
            List<UserAlias> dnAlias = new ArrayList<UserAlias>(userAlias.size());

            for(int i = 0; i < userAlias.size(); i++) {
                if(userAlias.get(i).getAliasType() == AuthzDBAdapter.ALIAS_TYPE_DN)
                    dnAlias.add(userAlias.get(i));
            }
            if(dnAlias.size() == 0) {
                return null;
            }
            else if(dnAlias.size() > 1)
                throw new RemoteException("User_Alias size: " + dnAlias.size());

            String aliasDN = dnAlias.get(0).getAliasName();
            final _Caller caller = this.reprFactory._newCaller();
            caller.setIdentity(aliasDN);
            VM[] vmsByCaller = manager.getAllByCaller(caller);
            return vmsByCaller;
        }
        catch (AuthzDBException e) {
            throw new RemoteException(e.getMessage());
        }
        catch (ManageException e) {
            return null;
        }
    }

    private VM[] getVMById(String id) {
        try {
            if(!manager.exists(id, manager.INSTANCE)) {
                errorMsg = "VM ID: " + id + " does not exist";
                return null;
            }

            VM[] vms = new VM[1];
            VM instance = manager.getInstance(id);
            vms[0] = instance;
            return vms;
        }
        catch (DoesNotExistException e) {
            errorMsg = "VM ID: " + id + " does not exist";
            return null;
        }
        catch (ManageException e) {
            errorMsg = "VM ID: " + id + " does not exist";
            return null;
        }
    }

    /*
     * Looks through all running vms and compares hostnames
     */
    private VM[] getVMByHost(String hostname) {
        try {
            VM[] vms;
            VM[] all = manager.getGlobalAll();
            int cnt = 0;

            vms = new VM[all.length];
            for(int i = 0; i < all.length; i++) {
                String id = all[i].getID();
                String host = workspaceHome.find(id).getVM().getNode();
                if(host.equals(hostname))
                    vms[cnt++] = all[i];
            }

            if(cnt == 0) {
                errorMsg = "No vms with hostname " + hostname + " found";
                return null;
            }
            else
                return vms;
        }
        catch (DoesNotExistException e) {
            errorMsg = "Hostname " + hostname + " not found";
            return null;
        }
        catch (ManageException e) {
            errorMsg = "No running VMs found";
            return null;
        }
    }

    private VM[] getAllVMsByGroup(Group group) throws RemoteException {
        try {
            if(group != null) {
                String[] dns = group.getIdentities();
                if(dns == null || dns.length == 0)
                    return null;

                authz = new AuthzDBAdapter(authzDataSource);
                ArrayList<VM> allVMs = new ArrayList<VM>(0);

                for(int i = 0; i < dns.length; i++) {
                    final _Caller caller = this.reprFactory._newCaller();
                    caller.setIdentity(dns[i]);
                    VM[] vmsByCaller = manager.getAllByCaller(caller);
                    for(int j = 0; j < vmsByCaller.length; j++) {
                        allVMs.add(vmsByCaller[j]);
                    }
                }
                if(allVMs.size() == 0)
                    return null;

                VM[] vms = new VM[allVMs.size()];
                return allVMs.toArray(vms);
            }
            return null;
        }
        catch (ManageException e) {
            throw new RemoteException(e.getMessage());
        }
    }

    private Group getGroupByGroupId(String groupId) {
        int id;
        try {
            id = Integer.parseInt(groupId);
        }
        catch(NumberFormatException e) {
            errorMsg = "Group id must be a positive integer";
            return null;
        }

        GroupAuthz groupAuthz = (GroupAuthz) authzCallout;
        Group[] groups = groupAuthz.getGroups();
        if(id <= 0 || id > groups.length || groups[id -1] == null) {
            errorMsg = "Group " + groupId + " not found";
            return null;
        }

        return groups[id -1];
    }

    private Group getGroupByGroupName(String groupName) {
        GroupAuthz groupAuthz = (GroupAuthz) authzCallout;
        Group[] groups = groupAuthz.getGroups();
        for(int i = 0; i < groups.length; i++) {
            if(groups[i] != null && groups[i].getName().equals(groupName))
                return groups[i];
        }
        errorMsg = "Group " + groupName + " not found";
        return null;
    }

    /*
     * Translates vm info into string format for passing over rmi with Gson
     */
    private VMTranslation translateVM(VM vm) throws RemoteException {
        try {
            GroupAuthz groupAuthz = (GroupAuthz) authzCallout;

            String id = vm.getID();
            VirtualMachine vmlong = workspaceHome.find(id).getVM();
            String node = vmlong.getNode();
            String creatorId = vm.getCreator().getIdentity();
            String groupId = Integer.toString(groupAuthz.getGroupIDFromCaller(creatorId));
            String groupName = groupAuthz.getGroupName(creatorId);
            String state = vm.getState().getState();
            String startTime = vm.getSchedule().getStartTime().getTime().toString();
            String endTime = vm.getSchedule().getDestructionTime().getTime().toString();

            ResourceAllocation ra = vm.getResourceAllocation();
            String memory = Integer.toString(ra.getMemory());
            String cpuCount = Integer.toString(ra.getIndCpuCount());
            String uri = vm.getVMFiles()[0].getURI().getPath();

            VMTranslation vmt = new VMTranslation(id, node, creatorId, groupId, groupName, state, startTime, endTime,
                    memory, cpuCount, uri);
            return vmt;
        }
        catch (ManageException e) {
            throw new RemoteException(e.getMessage());
        }
        catch (DoesNotExistException e) {
            throw new RemoteException(e.getMessage());
        }
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

    public void setWorkspaceHome(WorkspaceHome workspaceHome) {
        this.workspaceHome = workspaceHome;
    }

    public void setAuthzCallout(CreationAuthorizationCallout authzCallout) {
        this.authzCallout = authzCallout;
    }
}
