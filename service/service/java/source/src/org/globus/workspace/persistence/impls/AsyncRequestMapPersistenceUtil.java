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

package org.globus.workspace.persistence.impls;

import org.globus.workspace.async.AsyncRequest;
import org.globus.workspace.async.AsyncRequestStatus;
import org.globus.workspace.network.Association;
import org.globus.workspace.network.AssociationEntry;
import org.globus.workspace.persistence.DataConvert;
import org.globus.workspace.persistence.PersistenceAdapterConstants;
import org.globus.workspace.persistence.WorkspaceDatabaseException;
import org.globus.workspace.service.binding.vm.FileCopyNeed;
import org.globus.workspace.service.binding.vm.VirtualMachine;
import org.globus.workspace.service.binding.vm.VirtualMachineDeployment;
import org.globus.workspace.service.binding.vm.VirtualMachinePartition;
import org.nimbustools.api._repr._Caller;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.CannotTranslateException;
import org.nimbustools.api.repr.ReprFactory;
import org.nimbustools.api.repr.ctx.Context;
import org.nimbustools.api.repr.vm.NIC;
import org.nimbustools.api.services.rm.ManageException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class AsyncRequestMapPersistenceUtil
                        implements PersistenceAdapterConstants {

    public static PreparedStatement getInsertAsyncRequest(AsyncRequest asyncRequest, ReprFactory repr, Connection c)
                                                            throws SQLException {

        final PreparedStatement pstmt = c.prepareStatement(SQL_INSERT_ASYNC_REQUEST);
        pstmt.setString(1, asyncRequest.getId());
        pstmt.setDouble(2, asyncRequest.getMaxBid());
        if (asyncRequest.isSpotRequest()) {
            pstmt.setInt(3, 1);
        }
        else {
            pstmt.setInt(3, 0);
        }
        pstmt.setString(4, asyncRequest.getGroupID());
        if (asyncRequest.isPersistent()) {
            pstmt.setInt(5, 1);
        }
        else {
            pstmt.setInt(5, 0);
        }
        pstmt.setString(6, asyncRequest.getCaller().getIdentity());
        if (asyncRequest.getCaller().isSuperUser()) {
            pstmt.setInt(7, 1);
        }
        else {
            pstmt.setInt(7, 0);
        }
        pstmt.setString(8, asyncRequest.getSshKeyName());

        if (asyncRequest.getCreationTime() != null) {
            pstmt.setLong(9, asyncRequest.getCreationTime().getTimeInMillis());
        }
        else {
            pstmt.setInt(9,0);
        }
        DataConvert dataConvert = new DataConvert(repr);
        String nics = dataConvert.nicsAsString(asyncRequest.getRequestedNics());
        pstmt.setString(10, nics);

        pstmt.setString(11, asyncRequest.getStatus().toString());
        return pstmt;
    }

    public static PreparedStatement getUpdateAsyncRequest(AsyncRequest asyncRequest, ReprFactory repr, Connection c)
            throws SQLException {

        final PreparedStatement pstmt = c.prepareStatement(SQL_UPDATE_ASYNC_REQUEST);
        pstmt.setString(1, asyncRequest.getId());
        pstmt.setDouble(2, asyncRequest.getMaxBid());
        if (asyncRequest.isSpotRequest()) {
            pstmt.setInt(3, 1);
        }
        else {
            pstmt.setInt(3, 0);
        }
        pstmt.setString(4, asyncRequest.getGroupID());
        if (asyncRequest.isPersistent()) {
            pstmt.setInt(5, 1);
        }
        else {
            pstmt.setInt(5, 0);
        }
        pstmt.setString(6, asyncRequest.getCaller().getIdentity());
        if (asyncRequest.getCaller().isSuperUser()) {
            pstmt.setInt(7, 1);
        }
        else {
            pstmt.setInt(7, 0);
        }
        pstmt.setString(8, asyncRequest.getSshKeyName());

        if (asyncRequest.getCreationTime() != null) {
            pstmt.setLong(9, asyncRequest.getCreationTime().getTimeInMillis());
        }
        else {
            pstmt.setInt(9,0);
        }
        DataConvert dataConvert = new DataConvert(repr);
        String nics = dataConvert.nicsAsString(asyncRequest.getRequestedNics());
        pstmt.setString(10, nics);
        pstmt.setString(11, asyncRequest.getStatus().toString());
        return pstmt;
    }

    public static PreparedStatement getAsyncRequest(String id, Connection c)
                                                            throws SQLException {

        final PreparedStatement pstmt = c.prepareStatement(SQL_LOAD_ASYNC_REQUEST);

        pstmt.setString(1, id);

        return pstmt;
    }

    public static PreparedStatement getAllAsyncRequests(Connection c)
                                                            throws SQLException {

        final PreparedStatement pstmt = c.prepareStatement(SQL_LOAD_ALL_ASYNC_REQUESTS);
        return pstmt;
    }

    public static PreparedStatement getDeleteAsyncRequestVMs(AsyncRequest asyncRequest, Connection c)
                                                            throws SQLException {

        final PreparedStatement pstmt = c.prepareStatement(SQL_DELETE_ASYNC_REQUESTS_VMS);
        pstmt.setString(1, asyncRequest.getId());
        return pstmt;
    }

    public static AsyncRequest rsToAsyncRequest(ResultSet rs, ReprFactory repr, Connection c)
                                                            throws SQLException, CannotTranslateException {

        final String id = rs.getString("id");
        final Double maxBid = rs.getDouble("max_bid");
        final boolean isSpotInstance = rs.getBoolean("spot");
        final String groupID = rs.getString("group_id");
        final boolean isPersistent = rs.getBoolean("persistent");
        final String creatorDN = rs.getString("creator_dn");
        final boolean isSuperuser = rs.getBoolean("creator_is_superuser");
        _Caller caller = repr._newCaller();
        caller.setIdentity(creatorDN);
        caller.setSuperUser(isSuperuser);
        // NOTE: this context isn't persisted because it doesn't seem to be used
        final Context context = repr._newContext();

        final long t = rs.getLong("creation_time");
        final String sshKeyName = rs.getString("ssh_key_name");
        final Calendar creationTime = Calendar.getInstance();
        creationTime.setTimeInMillis(t);
        final String nicsAsString = rs.getString("nics");

        DataConvert dataConvert = new DataConvert(repr);
        NIC[] nics = null;
        nics = dataConvert.getNICs(nicsAsString);
        AsyncRequestStatus status = AsyncRequestStatus.valueOf(rs.getString("status"));

        //public AsyncRequest(String id, boolean spotinstances, Double spotPrice, boolean persistent, Caller caller, String groupID, VirtualMachine[] bindings, Context context, NIC[] requestedNics, String sshKeyName, Calendar creationTime) {
        //AsyncRequest testRequest = new AsyncRequest(testID, testSpotinstances, testMaxBid, false, null, testGroupID, null, null, null, null, null);
        AsyncRequest asyncRequest = new AsyncRequest(id, isSpotInstance, maxBid, isPersistent, caller, groupID, null, context, nics, sshKeyName, creationTime);

        asyncRequest.setStatus(status);
        return asyncRequest;
    }

    public static VirtualMachine[] getAsyncVMs(String asyncID, Connection c) throws SQLException, WorkspaceDatabaseException{

        final PreparedStatement pstmt = c.prepareStatement(SQL_LOAD_ASYNC_REQUESTS_VMS);
        pstmt.setString(1, asyncID);
        ResultSet rs = pstmt.executeQuery();

        ArrayList<VirtualMachine> vms = new ArrayList<VirtualMachine>();

        if (rs == null || !rs.next()) {
            return vms.toArray(new VirtualMachine[vms.size()]);
        }

        do {
            VirtualMachine newVM = new VirtualMachine();
            newVM.setID(rs.getInt("id"));
            newVM.setName(rs.getString("name"));
            newVM.setNode(rs.getString("node"));
            newVM.setPropagateRequired(rs.getBoolean("prop_required"));
            newVM.setUnPropagateRequired(rs.getBoolean("unprop_required"));
            newVM.setNetwork(rs.getString("network"));
            newVM.setKernel(rs.getString("kernel_parameters"));
            newVM.setVmm(rs.getString("vmm"));
            newVM.setVmmVersion(rs.getString("vmm_version"));
            newVM.setAssociationsNeeded(rs.getString("assocs_needed"));
            newVM.setMdUserData(rs.getString("md_user_data"));
            newVM.setPreemptable(rs.getBoolean("preemptable"));
            newVM.setCredentialName(rs.getString("credential_name"));

            int binding_index = rs.getInt("binding_index");

            addDeployment(newVM, asyncID, binding_index, c);
            addPartitions(newVM, asyncID, binding_index, c);
            addFileCopies(newVM, asyncID, binding_index, c);

            vms.add(newVM);
        } while (rs.next());

        rs.close();
        pstmt.close();

        VirtualMachine[] final_vms = vms.toArray(new VirtualMachine[vms.size()]);
        return final_vms;
    }

    public static void addDeployment(VirtualMachine vm, String asyncID, int binding_index, Connection c) throws SQLException {

        final PreparedStatement pstmt = c.prepareStatement(SQL_LOAD_ASYNC_REQUESTS_VM_DEPLOYMENT);
        pstmt.setString(1, asyncID);
        pstmt.setInt(2, binding_index);
        ResultSet rs = pstmt.executeQuery();

        if (rs == null || !rs.next()) {
            //No deployment for this VM
            return;
        }

        final VirtualMachineDeployment dep = new VirtualMachineDeployment();
        dep.setRequestedState(rs.getInt("requested_state"));
        dep.setRequestedShutdown(rs.getInt("requested_shutdown"));
        dep.setMinDuration(rs.getInt("min_duration"));
        dep.setIndividualPhysicalMemory(rs.getInt("ind_physmem"));
        dep.setIndividualCPUCount(rs.getInt("ind_physcpu"));
        vm.setDeployment(dep);
    }

    public static void addPartitions(VirtualMachine vm, String asyncID, int binding_index, Connection c) throws SQLException {

        final PreparedStatement pstmt = c.prepareStatement(SQL_LOAD_ASYNC_REQUESTS_VM_PARTITIONS);
        pstmt.setString(1, asyncID);
        pstmt.setInt(2, binding_index);
        ResultSet rs = pstmt.executeQuery();

        if (rs == null || !rs.next()) {
            //No partitions for this VM
            return;
        }

        final ArrayList partitions = new ArrayList(8);

        do {
            final VirtualMachinePartition partition =
                    new VirtualMachinePartition();
            partition.setImage(rs.getString("image"));
            partition.setImagemount(rs.getString("imagemount"));
            partition.setReadwrite(rs.getBoolean("readwrite"));
            partition.setRootdisk(rs.getBoolean("rootdisk"));
            partition.setBlankspace(rs.getInt("blankspace"));
            partition.setPropRequired(rs.getBoolean("prop_required"));
            partition.setUnPropRequired(rs.getBoolean("unprop_required"));
            partition.setAlternateUnpropTarget(rs.getString("alternate_unprop"));

            partitions.add(partition);
        } while (rs.next());

        VirtualMachinePartition[] final_partitions = new VirtualMachinePartition[partitions.size()];
        vm.setPartitions(final_partitions);
    }

    public static void addFileCopies(VirtualMachine vm, String asyncID, int binding_index, Connection c) throws SQLException, WorkspaceDatabaseException {

        final PreparedStatement pstmt = c.prepareStatement(SQL_LOAD_ASYNC_REQUESTS_VM_FILE_COPY);
        pstmt.setString(1, asyncID);
        pstmt.setInt(2, binding_index);
        ResultSet rs = pstmt.executeQuery();

        if (rs == null || !rs.next()) {
            //No file copies for this VM
            return;
        }

        do {
            String sourcePath = rs.getString("sourcepath");
            String destPath = rs.getString("destpath");
            boolean onImage = rs.getBoolean("on_image");
            try {
                FileCopyNeed need = new FileCopyNeed(sourcePath, destPath, onImage);
                vm.addFileCopyNeed(need);
            } catch(Exception e) {
                throw new WorkspaceDatabaseException("", e);
            }
        } while(rs.next());
    }

    public static void putAsyncRequestBindings(AsyncRequest asyncRequest, Connection c) throws SQLException {

        VirtualMachine[] bindings = asyncRequest.getBindings();
        for (int i=0; i<bindings.length; i++) {

            VirtualMachine binding = bindings[i];
            final PreparedStatement pstmt = c.prepareStatement(SQL_INSERT_ASYNC_REQUESTS_VMS);
            pstmt.setString(1, asyncRequest.getId());
            pstmt.setInt(2, i);
            pstmt.setInt(3, binding.getID());
            pstmt.setString(4, binding.getName());
            pstmt.setString(5, binding.getNode());
            boolean propRequired = binding.isPropagateRequired();
            if (propRequired) {
                pstmt.setInt(6, 1);
            }
            else {
                pstmt.setInt(6, 0);
            }
            boolean unpropRequired = binding.isUnPropagateRequired();
            if (unpropRequired) {
                pstmt.setInt(7, 1);
            }
            else {
                pstmt.setInt(7, 0);
            }
            pstmt.setString(8, binding.getNetwork());
            pstmt.setString(9, binding.getKernelParameters());
            pstmt.setString(10, binding.getVmm());
            pstmt.setString(11, binding.getVmmVersion());
            pstmt.setString(12, binding.getAssociationsNeeded());
            pstmt.setString(13, binding.getMdUserData());
            boolean preemptable = binding.isPreemptable();
            if (preemptable) {
                pstmt.setInt(14, 1);
            }
            else {
                pstmt.setInt(14, 0);
            }
            pstmt.setString(15, binding.getCredentialName());
            pstmt.executeUpdate();
            pstmt.close();
        }
    }

    public static PreparedStatement getInsertAsyncRequestVM(String id, int vmid, Connection c) throws SQLException {

        final PreparedStatement pstmt = c.prepareStatement(SQL_INSERT_ASYNC_REQUESTS_VMS);
        pstmt.setString(1, id);
        pstmt.setInt(2, vmid);
        return pstmt;
    }

    public static PreparedStatement[] getRemoveAsyncVMs(AsyncRequest asyncRequest, Connection c) throws SQLException {

        PreparedStatement[] pstmts = new PreparedStatement[4];

        pstmts[0] = c.prepareStatement(SQL_DELETE_ASYNC_REQUESTS_VMS);
        pstmts[0].setString(1, asyncRequest.getId());

        pstmts[1] = c.prepareStatement(SQL_DELETE_ASYNC_REQUESTS_VM_DEPLOYMENT);
        pstmts[1].setString(1, asyncRequest.getId());

        pstmts[2] = c.prepareStatement(SQL_DELETE_ASYNC_REQUESTS_VM_PARTITIONS);
        pstmts[2].setString(1, asyncRequest.getId());

        pstmts[3] = c.prepareStatement(SQL_DELETE_ASYNC_REQUESTS_VM_FILE_COPY);
        pstmts[3].setString(1, asyncRequest.getId());

        return pstmts;
    }
}
