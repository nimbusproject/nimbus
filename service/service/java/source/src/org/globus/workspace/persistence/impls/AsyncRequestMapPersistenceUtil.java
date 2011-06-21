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
import org.globus.workspace.network.Association;
import org.globus.workspace.network.AssociationEntry;
import org.globus.workspace.persistence.DataConvert;
import org.globus.workspace.persistence.PersistenceAdapterConstants;
import org.nimbustools.api._repr._Caller;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.CannotTranslateException;
import org.nimbustools.api.repr.ReprFactory;
import org.nimbustools.api.repr.ctx.Context;
import org.nimbustools.api.repr.vm.NIC;
import org.nimbustools.api.services.rm.ManageException;

import javax.xml.crypto.Data;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;

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

        //public AsyncRequest(String id, boolean spotinstances, Double spotPrice, boolean persistent, Caller caller, String groupID, VirtualMachine[] bindings, Context context, NIC[] requestedNics, String sshKeyName, Calendar creationTime) {
        //AsyncRequest testRequest = new AsyncRequest(testID, testSpotinstances, testMaxBid, false, null, testGroupID, null, null, null, null, null);
        return new AsyncRequest(id, isSpotInstance, maxBid, isPersistent, caller, groupID, null, context, nics, sshKeyName, creationTime);
    }

    public static ArrayList<Integer> getVMIDs(String id, Connection c) throws SQLException{

        final PreparedStatement pstmt = c.prepareStatement(SQL_LOAD_ASYNC_REQUESTS_VMS);
        pstmt.setString(1, id);
        ResultSet rs = pstmt.executeQuery();

        ArrayList<Integer> ids = new ArrayList<Integer>();

        if (rs == null || !rs.next()) {
            return ids;
        }

        do {
            ids.add(rs.getInt(1));
        } while (rs.next());

        return ids;
    }

    public static PreparedStatement getInsertAsyncRequestVM(String id, int vmid, Connection c) throws SQLException {

        final PreparedStatement pstmt = c.prepareStatement(SQL_INSERT_ASYNC_REQUESTS_VMS);
        pstmt.setString(1, id);
        pstmt.setInt(2, vmid);
        return pstmt;
    }
}
