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

package org.globus.workspace.persistence;

import java.io.IOException;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.workspace.Lager;
import org.globus.workspace.WorkspaceConstants;
import org.globus.workspace.creation.IdempotentInstance;
import org.globus.workspace.creation.IdempotentReservation;
import org.globus.workspace.creation.defaults.IdempotentInstanceImpl;
import org.globus.workspace.creation.defaults.IdempotentReservationImpl;
import org.globus.workspace.network.Association;
import org.globus.workspace.network.AssociationEntry;
import org.globus.workspace.persistence.impls.AssociationPersistenceUtil;
import org.globus.workspace.persistence.impls.IdempotencyPersistenceUtil;
import org.globus.workspace.persistence.impls.VMPersistence;
import org.globus.workspace.persistence.impls.VirtualMachinePersistenceUtil;
import org.globus.workspace.scheduler.backfill.Backfill;
import org.globus.workspace.scheduler.defaults.ResourcepoolEntry;
import org.globus.workspace.service.CoschedResource;
import org.globus.workspace.service.GroupResource;
import org.globus.workspace.service.InstanceResource;
import org.globus.workspace.service.binding.vm.CustomizationNeed;
import org.globus.workspace.service.binding.vm.VirtualMachine;
import org.globus.workspace.service.binding.vm.VirtualMachinePartition;
import org.nimbustools.api._repr._SpotPriceEntry;
import org.nimbustools.api.repr.ReprFactory;
import org.nimbustools.api.repr.SpotPriceEntry;
import org.nimbustools.api.services.rm.DoesNotExistException;
import org.nimbustools.api.services.rm.ManageException;

public class PersistenceAdapterImpl implements WorkspaceConstants,
                                               PersistenceAdapterConstants,
                                               PersistenceAdapter {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------
    
    private static final Log logger =
            LogFactory.getLog(PersistenceAdapterImpl.class.getName());

    private static final int[] EMPTY_INT_ARRAY = new int[0];
    

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private final Lager lager;
    private final DataSource dataSource;
    private final boolean dbTrace;
    private final ReprFactory repr;

    // caches, todo: ehcache
    private Hashtable associations;


    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    public PersistenceAdapterImpl(DataSource dataSourceImpl,
                                  Lager lagerImpl,
                                  DBLoader loader,
                                  ReprFactory reprImpl) throws Exception {
        
        if (dataSourceImpl == null) {
            throw new IllegalArgumentException("dataSource may not be null");
        }
        this.dataSource = dataSourceImpl;

        if (lagerImpl == null) {
            throw new IllegalArgumentException("lager may not be null");
        }
        this.lager = lagerImpl;
        this.dbTrace = lagerImpl.dbLog;

        if (reprImpl == null) {
            throw new IllegalArgumentException("reprImpl may not be null");
        }
        this.repr = reprImpl;        
        
        if (loader == null) {
            throw new IllegalArgumentException("loader may not be null");
        }
        if (!loader.isLoaded()) {
            throw new Exception("DBLoader reporting not loaded (?)");
        }

        try {
            this.prepareStatements();
        } catch (SQLException sql) {
            throw new Exception("Problem preparing DB statements: ", sql);
        }
    }


    // -------------------------------------------------------------------------
    // SETUP
    // -------------------------------------------------------------------------

    /**
     * This moves significant prepared statement setup times to service
     * initialization instead of the first time they're used.
     *
     * Documentation states that PreparedStatement caches are per pool
     * connection but testing indicates with Derby (in embedded mode),
     * this is OK using one connection.
     *
     * @throws SQLException problem
     */
    private void prepareStatements() throws SQLException {

        if (this.dbTrace) {
            logger.debug("prepareStatements()");
        }
        long mstart = 0;
        if (this.lager.perfLog) {
            mstart = System.currentTimeMillis();
        }
        
        //String[] ins =
        //        PersistenceAdapterConstants.INSENSITIVE_PREPARED_STATEMENTS;

        final String[] pstmts = PREPARED_STATEMENTS;

        Connection c = null;
        PreparedStatement pstmt = null;
        try {
            c = getConnection();

            //for (int i = 0; i < ins.length; i++) {
            //    pstmt = c.prepareStatement(ins[i],
            //                               ResultSet.TYPE_SCROLL_INSENSITIVE,
            //                               ResultSet.CONCUR_UPDATABLE);
            //    pstmt.close();
            //}

            for (int i = 0; i < pstmts.length; i++) {
                pstmt = c.prepareStatement(pstmts[i]);
                pstmt.close();
            }

            pstmt = null;

        } catch(SQLException e) {
            logger.error("",e);
            throw e;
        } finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
                if (c != null) {
                    returnConnection(c);
                }
            } catch (SQLException sql) {
                logger.error("SQLException in finally cleanup", sql);
            }

            if (this.lager.perfLog) {
                final long mstop = System.currentTimeMillis();
                logger.debug("_perf: prepareStatements() took " +
                            Long.toString(mstop - mstart) + " ms");
            }
        }
    }

    /* ********* */
    /* DB access */
    /* ********* */

    public void setState(int id, int state, Throwable t)
            throws WorkspaceDatabaseException{

        if (this.dbTrace) {
            logger.trace("setState(): " + Lager.id(id) + ", state = " + state);
        }

        final byte[] faultBytes;

        try {
            faultBytes = ErrorUtil.toByteArray(t);
        } catch (IOException e) {
            throw new WorkspaceDatabaseException(e);
        }

        Connection c = null;
        PreparedStatement pstmt = null;
        try {
            c = getConnection();
            pstmt = c.prepareStatement(SQL_SET_STATE);
            pstmt.setInt(1,state);
            if (faultBytes != null) {
                pstmt.setObject(2, faultBytes, Types.BLOB);
            } else {
                pstmt.setNull(2, Types.BLOB);
            }
            pstmt.setInt(3,id);
            final int updated = pstmt.executeUpdate();

            if (this.dbTrace) {
                logger.trace(Lager.id(id) + ": updated " + updated + " rows");
            }

        } catch(SQLException e) {
            logger.error("",e);
            throw new WorkspaceDatabaseException(e);
        } finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
                if (c != null) {
                    returnConnection(c);
                }
            } catch (SQLException sql) {
                logger.error("SQLException in finally cleanup", sql);
            }
        }
    }

    public void setTargetState(int id, int targetState)
            throws WorkspaceDatabaseException{

        if (this.dbTrace) {
            logger.trace("setTargetState(): " + Lager.id(id) +
                         ", targetState = " + targetState);
        }

        Connection c = null;
        PreparedStatement pstmt = null;
        try {
            c = getConnection();
            pstmt = c.prepareStatement(SQL_SET_TARGET_STATE);
            pstmt.setInt(1,targetState);
            pstmt.setInt(2,id);
            final int updated = pstmt.executeUpdate();

            if (this.dbTrace) {
                logger.trace(Lager.id(id) + ": updated " + updated + " rows");
            }

        } catch(SQLException e) {
            logger.error("",e);
            throw new WorkspaceDatabaseException(e);
        } finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
                if (c != null) {
                    returnConnection(c);
                }
            } catch (SQLException sql) {
                logger.error("SQLException in finally cleanup", sql);
            }
        }
    }

    public void setOpsEnabled(int id, boolean enabled)
            throws WorkspaceDatabaseException{

        if (this.dbTrace) {
            logger.trace("setOpsEnabled(): " + Lager.id(id) +
                                            ", enabled = " + enabled);
        }

        Connection c = null;
        PreparedStatement pstmt = null;
        try {
            c = getConnection();
            pstmt = c.prepareStatement(SQL_SET_OPS_ENABLED);
            if (enabled) {
                pstmt.setInt(1, 1);
            } else {
                pstmt.setInt(1, 0);
            }
            pstmt.setInt(2,id);
            final int updated = pstmt.executeUpdate();

            if (this.dbTrace) {
                logger.trace(Lager.id(id) + ": updated " + updated + " rows");
            }

        } catch(SQLException e) {
            logger.error("",e);
            throw new WorkspaceDatabaseException(e);
        } finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
                if (c != null) {
                    returnConnection(c);
                }
            } catch (SQLException sql) {
                logger.error("SQLException in finally cleanup", sql);
            }
        }
    }

    public void setNetwork(int id, String network)
            throws WorkspaceDatabaseException{

        if (this.dbTrace) {
            logger.trace("setNetwork(): " + Lager.id(id) +
                                            ", network = " + network);
        }

        Connection c = null;
        PreparedStatement pstmt = null;
        try {
            c = getConnection();
            pstmt = c.prepareStatement(SQL_SET_NETWORKING);
            if (network != null) {
                pstmt.setString(1, network);
            } else {
                pstmt.setNull(1, Types.VARCHAR);
            }
            pstmt.setInt(2,id);
            final int updated = pstmt.executeUpdate();

            if (this.dbTrace) {
                logger.trace(Lager.id(id) + ": updated " + updated + " rows");
            }

        } catch(SQLException e) {
            logger.error("",e);
            throw new WorkspaceDatabaseException(e);
        } finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
                if (c != null) {
                    returnConnection(c);
                }
            } catch (SQLException sql) {
                logger.error("SQLException in finally cleanup", sql);
            }
        }
    }

    public void setVMMaccessOK(int resourceID, boolean accessOK)

            throws WorkspaceDatabaseException {
        
        if (this.dbTrace) {
            logger.trace("setVMMaccessOK(): " + Lager.id(resourceID)
                                            + ", OK? " + accessOK);
        }

        Connection c = null;
        PreparedStatement pstmt = null;
        try {
            c = getConnection();
            pstmt = c.prepareStatement(SQL_SET_VMM_ACCESS_OK);
            if (accessOK) {
                pstmt.setInt(1, 1);
            } else {
                pstmt.setInt(1, 0);
            }
            pstmt.setInt(2, resourceID);
            final int updated = pstmt.executeUpdate();

            if (this.dbTrace) {
                logger.trace(Lager.id(resourceID) + ": updated " +
                        updated + " rows");
            }

        } catch(SQLException e) {
            logger.error("",e);
            throw new WorkspaceDatabaseException(e);
        } finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
                if (c != null) {
                    returnConnection(c);
                }
            } catch (SQLException sql) {
                logger.error("SQLException in finally cleanup", sql);
            }
        }
    }

    public void setHostname(int id, String hostname)
            throws WorkspaceDatabaseException{

        if (this.dbTrace) {
            logger.trace("setHostname(): " + Lager.id(id) +
                                            ", hostname = " + hostname);
        }

        Connection c = null;
        PreparedStatement pstmt = null;
        try {
            c = getConnection();
            pstmt = c.prepareStatement(SQL_SET_HOSTNAME);
            if (hostname != null) {
                pstmt.setString(1, hostname);
            } else {
                pstmt.setNull(1, Types.VARCHAR);
            }
            pstmt.setInt(2,id);
            final int updated = pstmt.executeUpdate();

            if (this.dbTrace) {
                logger.trace(Lager.id(id) + ": updated " + updated + " rows");
            }

        } catch(SQLException e) {
            logger.error("",e);
            throw new WorkspaceDatabaseException(e);
        } finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
                if (c != null) {
                    returnConnection(c);
                }
            } catch (SQLException sql) {
                logger.error("SQLException in finally cleanup", sql);
            }
        }
    }

    public void setRootUnpropTarget(int id, String path)

            throws WorkspaceDatabaseException {

        if (this.dbTrace) {
            logger.trace("setRootUnpropTarget(): " + Lager.id(id) +
                                            ", path = " + path);
        }

        Connection c = null;
        PreparedStatement pstmt = null;
        try {
            c = getConnection();

            // we need two SQL statements because unprop is known to be needed
            // if setting path, but unknown if clearing it
            if (path == null) {
                pstmt = c.prepareStatement(SQL_UNSET_ROOT_UNPROP_TARGET);
                pstmt.setNull(1, Types.VARCHAR);
                pstmt.setInt(2,id);
            } else {
                pstmt = c.prepareStatement(SQL_SET_ROOT_UNPROP_TARGET);
                pstmt.setString(1, path);
                pstmt.setInt(2,id);
            }

            final int updated = pstmt.executeUpdate();

            if (this.dbTrace) {
                logger.trace(Lager.id(id) + ": updated " + updated + " rows");
            }

        } catch(SQLException e) {
            logger.error("",e);
            throw new WorkspaceDatabaseException(e);
        } finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
                if (c != null) {
                    returnConnection(c);
                }
            } catch (SQLException sql) {
                logger.error("SQLException in finally cleanup", sql);
            }
        }
    }

    public void addCustomizationNeed(int id, CustomizationNeed need)
            throws WorkspaceDatabaseException {
        
        if (this.dbTrace) {
            logger.trace("addCustomizationNeed(): " + Lager.id(id));
        }

        if (need == null) {
            throw new IllegalArgumentException("need may not be null");
        }

        Connection c = null;
        PreparedStatement pstmt = null;
        try {
            c = getConnection();
            pstmt = c.prepareStatement(SQL_INSERT_VM_CUSTOMIZATION);

            pstmt.setInt(1, id);
            pstmt.setString(2, need.sourcePath);
            pstmt.setString(3, need.destPath);
            if (need.isSent()) {
                pstmt.setInt(4, 1);
            } else {
                pstmt.setInt(4, 0);
            }

            final int updated = pstmt.executeUpdate();

            if (this.dbTrace) {
                logger.trace(Lager.id(id) + ": updated " + updated + " rows");
            }

        } catch(SQLException e) {
            logger.error("",e);
            throw new WorkspaceDatabaseException(e);
        } finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
                if (c != null) {
                    returnConnection(c);
                }
            } catch (SQLException sql) {
                logger.error("SQLException in finally cleanup", sql);
            }
        }
    }
    
    public void setCustomizeTaskSent(int id, CustomizationNeed need)
            throws WorkspaceDatabaseException {

        if (this.dbTrace) {
            logger.trace("setCustomizeTaskSent(): " + Lager.id(id) +
                                            ", sent = " + need.isSent());
        }

        Connection c = null;
        PreparedStatement pstmt = null;
        try {
            c = getConnection();
            pstmt = c.prepareStatement(SQL_SET_VM_CUSTOMIZATION_SENT);
            if (need.isSent()) {
                pstmt.setInt(1, 1);
            } else {
                pstmt.setInt(1, 0);
            }
            pstmt.setInt(2, id);
            pstmt.setString(3, need.sourcePath);
            pstmt.setString(4, need.destPath);

            final int updated = pstmt.executeUpdate();

            if (this.dbTrace) {
                logger.trace(Lager.id(id) + ": updated " + updated + " rows");
            }

        } catch(SQLException e) {
            logger.error("",e);
            throw new WorkspaceDatabaseException(e);
        } finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
                if (c != null) {
                    returnConnection(c);
                }
            } catch (SQLException sql) {
                logger.error("SQLException in finally cleanup", sql);
            }
        }
    }
    
    public void setStartTime(int id, Calendar startTime)

            throws WorkspaceDatabaseException {

        if (this.dbTrace) {
            logger.trace("setStartTime(): " + Lager.id(id) +
                                            ", startTime = " + startTime);
        }

        Connection c = null;
        PreparedStatement pstmt = null;
        try {
            c = getConnection();
            pstmt = c.prepareStatement(SQL_SET_STARTTIME);

            if (startTime != null) {
                pstmt.setObject(1,
                    new Long(startTime.getTimeInMillis()));
            } else {
                pstmt.setInt(1, 0);
            }

            pstmt.setInt(2,id);
            final int updated = pstmt.executeUpdate();

            if (this.dbTrace) {
                logger.trace(Lager.id(id) + ": updated " + updated + " rows");
            }

        } catch(SQLException e) {
            logger.error("",e);
            throw new WorkspaceDatabaseException(e);
        } finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
                if (c != null) {
                    returnConnection(c);
                }
            } catch (SQLException sql) {
                logger.error("SQLException in finally cleanup", sql);
            }
        }
    }

    public void setTerminationTime(int id, Calendar termTime)

            throws WorkspaceDatabaseException {
        
        if (this.dbTrace) {
            logger.trace("setTerminationTime(): " + Lager.id(id) +
                                            ", startTime = " + termTime);
        }

        Connection c = null;
        PreparedStatement pstmt = null;
        try {
            c = getConnection();
            pstmt = c.prepareStatement(SQL_SET_TERMTIME);

            if (termTime != null) {
                pstmt.setObject(1,
                    new Long(termTime.getTimeInMillis()));
            } else {
                pstmt.setInt(1, 0);
            }

            pstmt.setInt(2,id);
            final int updated = pstmt.executeUpdate();

            if (this.dbTrace) {
                logger.trace(Lager.id(id) + ": updated " + updated + " rows");
            }

        } catch(SQLException e) {
            logger.error("",e);
            throw new WorkspaceDatabaseException(e);
        } finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
                if (c != null) {
                    returnConnection(c);
                }
            } catch (SQLException sql) {
                logger.error("SQLException in finally cleanup", sql);
            }
        }
    }

    public void remove(int id, InstanceResource resource)
            throws WorkspaceDatabaseException {

        if (this.dbTrace) {
            logger.trace("remove(): " + Lager.id(id)
                                            + ", resource = " + resource);
        }

        if (id < 0) {
            throw new WorkspaceDatabaseException("id is less than zero");
        }

        Connection c = null;
        PreparedStatement pstmt = null;
        PreparedStatement[] pstmts = null;
        try {
            c = getConnection();
            c.setAutoCommit(false);
            pstmt = c.prepareStatement(SQL_DELETE_RESOURCE);
            pstmt.setInt(1, id);
            int numMod = pstmt.executeUpdate();

            if (this.dbTrace) {
                logger.trace(Lager.id(id) + " resources: removed "
                                                    + numMod + " rows");
            }

            if (resource.getVM() != null) {
                pstmts = VirtualMachinePersistenceUtil.
                                        getRemoveVM(resource.getVM(), id, c);
            }

            if (pstmts != null) {
                for (int i = 0; i < pstmts.length; i++) {
                    numMod = pstmts[i].executeUpdate();

                    if (this.dbTrace) {
                        logger.trace(Lager.id(id) + " vm record: removed "
                                                        + numMod + " rows");
                    }
                }
            }

            c.commit();

        } catch(SQLException e) {
            logger.error("",e);
            throw new WorkspaceDatabaseException(e);
        } finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
                if (pstmts != null) {
                    for (int i = 0; i < pstmts.length; i++) {
                        pstmts[i].close();
                    }
                }
                if (c != null) {
                    c.setAutoCommit(true);
                    returnConnection(c);
                }
            } catch (SQLException sql) {
                logger.error("SQLException in finally cleanup", sql);
            }
        }
    }

    public void removeGroup(String id)

            throws WorkspaceDatabaseException {
                
        if (this.dbTrace) {
            logger.trace("removeGroup(): " + Lager.groupid(id));
        }
        this.removeGroupOrEnsemble(id);
    }

    public void removeEnsemble(String id)

            throws WorkspaceDatabaseException {

        if (this.dbTrace) {
            logger.trace("removeEnsemble(): " + Lager.ensembleid(id));
        }
        this.removeGroupOrEnsemble(id);
    }

    private void removeGroupOrEnsemble(String id)

            throws WorkspaceDatabaseException {

        Connection c = null;
        PreparedStatement pstmt = null;
        try {
            c = getConnection();
            pstmt = c.prepareStatement(SQL_DELETE_GROUP_RESOURCE);
            pstmt.setString(1, id);
            final int removed = pstmt.executeUpdate();

            if (this.dbTrace) {
                logger.trace(Lager.groupid(id) + " groupresources: removed "
                                                    + removed + " rows");
            }
        } catch(SQLException e) {
            logger.error("",e);
            throw new WorkspaceDatabaseException(e);
        } finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
                if (c != null) {
                    returnConnection(c);
                }
            } catch (SQLException sql) {
                logger.error("SQLException in finally cleanup", sql);
            }
        }
    }

    public int[] findActiveWorkspacesIDs() throws WorkspaceDatabaseException {

        if (this.dbTrace) {
            logger.trace("findActiveWorkspacesIDs()");
        }

        Connection c = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            c = getConnection();
            pstmt = c.prepareStatement(SQL_SELECT_RESOURCES);
            rs = pstmt.executeQuery();

            final ArrayList results = new ArrayList();
            if (rs == null || !rs.next()) {
                return null;
            } else {
                do {
                    final Integer id = new Integer(rs.getInt(1));
                    results.add(id);
                    if (this.dbTrace) {
                        logger.trace("found id: " + id);
                    }
                } while (rs.next());
            }

            // can't use toArray without converting to Integer[]
            final int[] ret = new int[results.size()];
            for (int i = 0; i < ret.length; i++) {
                ret[i] = ((Number) results.get(i)).intValue();
            }
            return ret;

        } catch(SQLException e) {
            logger.error("",e);
            throw new WorkspaceDatabaseException(e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (pstmt != null) {
                    pstmt.close();
                }
                if (c != null) {
                    returnConnection(c);
                }
            } catch (SQLException sql) {
                logger.error("SQLException in finally cleanup", sql);
            }
        }
    }

    public boolean isActiveWorkspaceID(int id)

            throws WorkspaceDatabaseException {


        if (this.dbTrace) {
            logger.trace("isActiveWorkspaceID(), id = " + id);
        }

        Connection c = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            c = getConnection();
            pstmt = c.prepareStatement(SQL_LOAD_RESOURCE_NAME);
            pstmt.setInt(1, id);
            rs = pstmt.executeQuery();

            if (rs == null || !rs.next()) {
                logger.debug("No workspace with id " + id);
                return false;
            } else {
                final String name = rs.getString(1);
                if (this.dbTrace) {
                    logger.trace("found id " + id + ", name = " + name);
                }
                return true;
            }
        } catch(SQLException e) {
            logger.error("",e);
            throw new WorkspaceDatabaseException(e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (pstmt != null) {
                    pstmt.close();
                }
                if (c != null) {
                    returnConnection(c);
                }
            } catch (SQLException sql) {
                logger.error("SQLException in finally cleanup", sql);
            }
        }
    }

    public int[] findVMsInGroup(String groupID)

            throws WorkspaceDatabaseException {
        
        if (groupID == null) {
            throw new WorkspaceDatabaseException("groupID is null");
        }

        return this.findVMsInGroupOrEnsemble(groupID,
                                             SQL_SELECT_ALL_VMS_IN_GROUP);
    }

    public int[] findVMsInEnsemble(String ensembleID)

            throws WorkspaceDatabaseException {

        if (ensembleID == null) {
            throw new WorkspaceDatabaseException("ensembleID is null");
        }

        return this.findVMsInGroupOrEnsemble(ensembleID,
                                             SQL_SELECT_ALL_VMS_IN_ENSEMBLE);
    }

    private int[] findVMsInGroupOrEnsemble(String ID,
                                           String pstmtString)

            throws WorkspaceDatabaseException {

        Connection c = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            c = getConnection();

            pstmt = c.prepareStatement(pstmtString);
            pstmt.setString(1, ID);

            rs = pstmt.executeQuery();

            if (rs == null || !rs.next()) {
                logger.debug("no VM ensemble/group found with ID = " + ID);
                return EMPTY_INT_ARRAY;
            }

            final ArrayList vmidsList = new ArrayList(64);
            do {
                vmidsList.add(new Integer(rs.getInt(1)));
            } while (rs.next());

            // can't use toArray without converting to Integer[]
            final int[] ret = new int[vmidsList.size()];
            for (int i = 0; i < ret.length; i++) {
                ret[i] = ((Number)vmidsList.get(i)).intValue();
            }
            return ret;

        } catch(SQLException e) {
            logger.error("",e);
            throw new WorkspaceDatabaseException(e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (pstmt != null) {
                    pstmt.close();
                }
                if (c != null) {
                    returnConnection(c);
                }
            } catch (SQLException sql) {
                logger.error("SQLException in finally cleanup", sql);
            }
        }
    }


    public int[] findVMsByOwner(String ownerID)

            throws WorkspaceDatabaseException {

        if (ownerID == null) {
            throw new WorkspaceDatabaseException("ownerID is missing");
        }

        Connection c = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            c = getConnection();

            pstmt = c.prepareStatement(SQL_SELECT_ALL_VMS_BY_OWNER);
            pstmt.setString(1, ownerID);

            rs = pstmt.executeQuery();

            if (rs == null || !rs.next()) {
                logger.debug("no VMs found with ID = " + ownerID);
                return EMPTY_INT_ARRAY;
            }

            final ArrayList vmidsList = new ArrayList(64);
            do {
                vmidsList.add(new Integer(rs.getInt(1)));
            } while (rs.next());

            // can't use toArray without converting to Integer[]
            final int[] ret = new int[vmidsList.size()];
            for (int i = 0; i < ret.length; i++) {
                ret[i] = ((Number)vmidsList.get(i)).intValue();
            }
            return ret;

        } catch(SQLException e) {
            logger.error("",e);
            throw new WorkspaceDatabaseException(e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (pstmt != null) {
                    pstmt.close();
                }
                if (c != null) {
                    returnConnection(c);
                }
            } catch (SQLException sql) {
                logger.error("SQLException in finally cleanup", sql);
            }
        }
    }

    public void add(InstanceResource resource)

            throws WorkspaceDatabaseException {

        if (resource == null) {
            throw new WorkspaceDatabaseException("resource is null");
        }

        final int id = resource.getID();

        if (id < 0) {
            throw new WorkspaceDatabaseException("id is less than zero");
        }

        if (this.dbTrace) {
            logger.trace("add(): " + Lager.id(id) +
                                ", WorkspaceResource = " + resource);
        }

        final byte[] faultBytes;

        try {
            faultBytes = ErrorUtil.toByteArray(resource.getStateThrowable());
        } catch (IOException e) {
            throw new WorkspaceDatabaseException(e);
        }

        Connection c = null;
        PreparedStatement pstmt = null;
        PreparedStatement[] pstmts = null;
        try {
            c = getConnection();
            c.setAutoCommit(false);
            pstmt = c.prepareStatement(SQL_INSERT_RESOURCE);

            pstmt.setInt(1, id);
            pstmt.setString(2,resource.getName());
            pstmt.setInt(3, resource.getState());
            pstmt.setInt(4, resource.getTargetState());

            if (resource.getTerminationTime() != null) {
                pstmt.setObject(5,
                    new Long(resource.getTerminationTime().getTimeInMillis()));
            } else {
                pstmt.setInt(5, 0);
            }

            if (resource.isOpsEnabled()) {
                pstmt.setInt(6, 1);
            } else {
                pstmt.setInt(6, 0);
            }

            if (resource.getCreatorID() != null) {
                pstmt.setString(7, resource.getCreatorID());
            } else {
                pstmt.setNull(7, Types.VARCHAR);
            }

            if (resource.getStartTime() != null) {
                pstmt.setObject(8,
                    new Long(resource.getStartTime().getTimeInMillis()));
            } else {
                pstmt.setInt(8, 0);
            }

            if (resource.isVMMaccessOK()) {
                pstmt.setInt(9, 1);
            } else {
                pstmt.setInt(9, 0);
            }

            if (resource.getEnsembleId() != null) {
                pstmt.setString(10, resource.getEnsembleId());
            } else {
                pstmt.setNull(10, Types.VARCHAR);
            }

            if (resource.getGroupId() != null) {
                pstmt.setString(11, resource.getGroupId());
            } else {
                pstmt.setNull(11, Types.VARCHAR);
            }

            pstmt.setInt(12, resource.getGroupSize());

            if (resource.isLastInGroup()) {
                pstmt.setInt(13, 1);
            } else {
                pstmt.setInt(13, 0);
            }

            pstmt.setInt(14, resource.getLaunchIndex());

            if (faultBytes != null) {
                pstmt.setObject(15, faultBytes, Types.BLOB);
            } else {
                pstmt.setNull(15, Types.BLOB);
            }

            pstmt.setString(16, resource.getClientToken());

            if (this.dbTrace) {
                logger.trace("creating WorkspaceResource db " +
                        "entry for " + Lager.id(id));
            }

            pstmt.executeUpdate();

            if (resource instanceof VMPersistence) {

                pstmts = VirtualMachinePersistenceUtil.
                                                  getInsertVM(resource, id, c);

                if (this.dbTrace) {
                    logger.trace("creating VirtualMachine db " +
                            "entry for " + Lager.id(id) + ": " +
                            pstmts.length + " inserts");
                }

                for (int i = 0; i < pstmts.length; i++) {
                    pstmts[i].executeUpdate();
                }
            }

            c.commit();

        } catch(SQLException e) {
            logger.error("",e);
            throw new WorkspaceDatabaseException(e);
        } catch (ManageException e) {
            logger.error("",e);
            throw new WorkspaceDatabaseException(e);
        } finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
                if (pstmts != null) {
                    for (int i = 0; i < pstmts.length; i++) {
                        pstmts[i].close();
                    }
                }
                if (c != null) {
                    c.setAutoCommit(true);
                    returnConnection(c);
                }
            } catch (SQLException sql) {
                logger.error("SQLException in finally cleanup", sql);
            }
        }
    }

    public void addGroup(GroupResource resource)

            throws WorkspaceDatabaseException {

        if (resource == null) {
            throw new IllegalArgumentException("resource is null");
        }
        
        final String id = resource.getID();

        if (this.dbTrace) {
            logger.trace("addGroup(): " + Lager.groupid(id));
        }

        this.addGroupOrEnsemble(id, resource.getCreatorID());
    }

    public void addEnsemble(CoschedResource resource)

            throws WorkspaceDatabaseException {
        
        if (resource == null) {
            throw new IllegalArgumentException("resource is null");
        }

        final String id = resource.getID();

        if (this.dbTrace) {
            logger.trace("addEnsemble(): " + Lager.ensembleid(id));
        }

        this.addGroupOrEnsemble(id, resource.getCreatorID());
    }

    private void addGroupOrEnsemble(String id, String creatorDN)

            throws WorkspaceDatabaseException {

        if (id == null) {
            throw new IllegalArgumentException("id is null");
        }

        Connection c = null;
        PreparedStatement pstmt = null;
        try {
            c = getConnection();
            pstmt = c.prepareStatement(SQL_INSERT_GROUP_RESOURCE);

            pstmt.setString(1, id);

            if (creatorDN != null) {
                pstmt.setString(2, creatorDN);
            } else {
                pstmt.setNull(2, Types.VARCHAR);
            }

            pstmt.executeUpdate();

        } catch(SQLException e) {
            logger.error("",e);
            throw new WorkspaceDatabaseException(e);
        } finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
                if (c != null) {
                    returnConnection(c);
                }
            } catch (SQLException sql) {
                logger.error("SQLException in finally cleanup", sql);
            }
        }
    }

    /**
     * @param id id
     * @param resource resource
     * @throws DoesNotExistException
     * @throws WorkspaceDatabaseException
     */
    public void load(int id, InstanceResource resource)
            throws DoesNotExistException, WorkspaceDatabaseException {

        if (this.dbTrace) {
            logger.trace("load(): " + Lager.id(id) +
                                ", WorkspaceResource = " + resource);
        }

        if (id < 0) {
            throw new DoesNotExistException("id is less than zero");
        }

        Connection c = null;
        PreparedStatement pstmt = null;
        PreparedStatement[] pstmts = null;
        ResultSet rs = null;
        try {
            c = getConnection();

            pstmt = c.prepareStatement(SQL_LOAD_RESOURCE);
            pstmt.setInt(1, id);
            rs = pstmt.executeQuery();
            if (rs == null || !rs.next()) {
                final String err = "resource with id = " + id + " not found";
                logger.debug(err);
                throw new DoesNotExistException(err);
            } else {
                final String name = rs.getString(1);
                resource.setName(name);
                final int state = rs.getInt(2);
                final int targetState = rs.getInt(3);
                resource.setInitialTargetState(targetState);
                final long t = rs.getLong(4);
                if (t == 0) {
                    resource.setTerminationTime(null);
                } else {
                    final Calendar term = Calendar.getInstance();
                    term.setTimeInMillis(t);
                    resource.setTerminationTime(term);
                }
                final boolean opsEnabled = rs.getBoolean(5);
                resource.setInitialOpsEnabled(opsEnabled);
                
                final String dn = rs.getString(6);
                resource.setCreatorID(dn);

                final long s = rs.getLong(7);
                if (s == 0) {
                    resource.setStartTime(null);
                } else {
                    final Calendar start = Calendar.getInstance();
                    start.setTimeInMillis(s);
                    resource.setStartTime(start);
                }
                
                final boolean vmmAccessOK = rs.getBoolean(8);
                resource.setInitialVMMaccessOK(vmmAccessOK);

                final String ensembleid = rs.getString(9);
                resource.setEnsembleId(ensembleid);

                final String groupid = rs.getString(10);
                resource.setGroupId(groupid);

                final int groupsize = rs.getInt(11);
                resource.setGroupSize(groupsize);

                final boolean isLastInGroup = rs.getBoolean(12);
                resource.setLastInGroup(isLastInGroup);

                final int launchIndex = rs.getInt(13);
                resource.setLaunchIndex(launchIndex);

                final Blob errBlob = rs.getBlob(14);
                if (errBlob != null) {
                    // getBytes requires int, cast from long
                    final int length = (int)errBlob.length();
                    final Throwable err =
                           ErrorUtil.getThrowable(errBlob.getBytes(1,length));
                    resource.setInitialState(state, err);
                } else {
                    resource.setInitialState(state, null);
                }

                final String clientToken = rs.getString(15);
                resource.setClientToken(clientToken);


                if (this.dbTrace) {
                    logger.trace("found " + Lager.id(id) +
                             ": name = " + name +
                             ", state = " + state +
                             ", targetState = " + targetState +
                             ", termination time = " + t +
                             ", opsEnabled = " + opsEnabled +
                             ", creator ID = " + dn +
                             ", start time = " + s +
                             ", vmmAccessOK = " + vmmAccessOK +
                             ", ensembleid = " + ensembleid +
                             ", groupid = " + groupid +
                             ", groupsize = " + groupsize +
                             ", isLastInGroup = " + isLastInGroup +
                             ", launchIndex = " + launchIndex +
                             ", clientToken = " + clientToken +
                             ", error present = " + (errBlob != null));
                }
                
                rs.close();

                if (resource instanceof VMPersistence) {
                    if (this.dbTrace) {
                        logger.trace(Lager.id(id) + ": load virtual machine");
                    }

                    pstmts = VirtualMachinePersistenceUtil.getVMQuery(id, c);

                    rs = pstmts[0].executeQuery();
                    if (rs == null || !rs.next()) {
                        logger.error("resource with id=" + id + " not found");
                        throw new DoesNotExistException();
                    }

                    final VirtualMachine vm =
                            VirtualMachinePersistenceUtil.newVM(id, rs);

                    if (this.dbTrace) {
                        logger.trace(Lager.id(id) +
                                        ", created vm:\n" + vm.toString());
                    }

                    rs.close();

                    rs = pstmts[1].executeQuery();
                    if (rs == null || !rs.next()) {
                        logger.debug("resource with id=" + id + " has no" +
                                " deployment information");
                    } else {
                        VirtualMachinePersistenceUtil.addDeployment(vm, rs);
                        if (this.dbTrace) {
                            logger.trace("added deployment info to vm object");
                        }
                        rs.close();
                    }

                    rs = pstmts[2].executeQuery();

                    if (rs == null || !rs.next()) {
                        logger.warn("resource with id=" + id + " has no" +
                                " partitions");
                    } else {
                        final ArrayList partitions = new ArrayList(8);
                        do {
                            partitions.add(VirtualMachinePersistenceUtil.
                                                             getPartition(rs));
                        } while (rs.next());

                        final VirtualMachinePartition[] parts =
                            (VirtualMachinePartition[]) partitions.toArray(
                               new VirtualMachinePartition[partitions.size()]);
                        vm.setPartitions(parts);
                    }

                    rs = pstmts[3].executeQuery();

                    if (rs == null || !rs.next()) {
                        if (this.lager.dbLog) {
                            logger.debug("resource with id=" + id + " has no" +
                                    " customization needs");
                        }
                    } else {
                        do {
                            vm.addCustomizationNeed(
                                    VirtualMachinePersistenceUtil.getNeed(rs));
                        } while (rs.next());
                    }

                    ((VMPersistence)resource).setWorkspace(vm);
                }
            }
        } catch(SQLException e) {
            logger.error("",e);
            throw new WorkspaceDatabaseException(e);
        } catch(DoesNotExistException e) {
            throw e;
        } catch (IOException e) {
            logger.error("",e);
            throw new WorkspaceDatabaseException(e);
        } catch (ClassNotFoundException e) {
            logger.error("",e);
            throw new WorkspaceDatabaseException(e);
        } finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
                if (pstmts != null) {
                    for (int i = 0; i < pstmts.length; i++) {
                        pstmts[i].close();
                    }
                }
                if (rs != null) {
                    rs.close();
                }
                if (c != null) {
                    returnConnection(c);
                }
            } catch (SQLException sql) {
                logger.error("SQLException in finally cleanup", sql);
            }
        }
    }

    public void loadGroup(String id, GroupResource resource)

            throws DoesNotExistException, WorkspaceDatabaseException {

        if (this.dbTrace) {
            logger.trace("loadGroup(): " + Lager.groupid(id) +
                                ", WorkspaceGroupResource = " + resource);
        }

        final String callerID = this.loadGroupOrEnsemble(id);
        resource.setCreatorID(callerID);
        resource.setID(id);

        if (this.dbTrace) {
            logger.trace("found " + Lager.groupid(id) +
                         ", creator ID = " + callerID);
        }
    }

    public void loadEnsemble(String id, CoschedResource resource)

            throws DoesNotExistException, WorkspaceDatabaseException {

        if (this.dbTrace) {
            logger.trace("loadEnsemble(): " + Lager.ensembleid(id) +
                                ", WorkspaceEnsembleResource = " + resource);
        }

        final String creatorID = this.loadGroupOrEnsemble(id);

        if (creatorID != null) {
            resource.setCreatorID(creatorID);
        }

        resource.setID(id);

        if (this.dbTrace) {
            logger.trace("found " + Lager.ensembleid(id) +
                         ", creator ID = " + creatorID);
        }
    }

    private String loadGroupOrEnsemble(String id)

            throws DoesNotExistException, WorkspaceDatabaseException {

        if (id == null) {
            throw new DoesNotExistException("id is null");
        }

        Connection c = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            c = getConnection();

            pstmt = c.prepareStatement(SQL_LOAD_GROUP_RESOURCE);
            pstmt.setString(1, id);
            rs = pstmt.executeQuery();
            if (rs == null || !rs.next()) {
                final String err = "resource with id = " + id + " not found";
                logger.error(err);
                throw new DoesNotExistException(err);
            } else {
                // could be null
                return rs.getString(1);
            }
        } catch(SQLException e) {
            logger.error("",e);
            throw new WorkspaceDatabaseException(e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (pstmt != null) {
                    pstmt.close();
                }
                if (c != null) {
                    returnConnection(c);
                }
            } catch (SQLException sql) {
                logger.error("SQLException in finally cleanup", sql);
            }
        }
    }

    /**
     * This is only called at factory initialization and not a priority
     * to switch to using PreparedStatements
     * 
     * @param assocs all associations (potentially merged with previous)
     * @throws WorkspaceDatabaseException
     */
    public void replaceAssocations(Hashtable assocs)
                                throws WorkspaceDatabaseException {

        if (this.dbTrace) {
            logger.trace("replaceAssocations()");
        }

        final String[] inserts =
            AssociationPersistenceUtil.insertAllAssociationsSQL(assocs);

        if (inserts == null || inserts.length == 0) {
            logger.debug("no networks to persist");
            this.associations = null;
            return;
        }

        Connection c = null;
        Statement st = null;
        PreparedStatement pstmt = null;
        PreparedStatement pstmt2 = null;
        try {
            c = getConnection();
            c.setAutoCommit(false);

            // first delete all current rows
            pstmt = c.prepareStatement(SQL_DELETE_ALL_ASSOCIATIONS);
            final int removedNum = pstmt.executeUpdate();
            if (this.dbTrace) {
                logger.trace("removed " + removedNum +
                             " entries from associations table");
            }

            pstmt2 = c.prepareStatement(SQL_DELETE_ALL_ASSOCIATION_ENTRIES);
            final int removedEntries = pstmt2.executeUpdate();
            if (this.dbTrace) {
                logger.trace("removed " + removedEntries + " entries from " +
                             "association entries table");
            }
            
            st = c.createStatement();
            for (int i = 0; i < inserts.length; i++) {
                if (this.dbTrace) {
                    logger.trace("executing insert: '" + inserts[i] + "'");
                }
                st.executeUpdate(inserts[i]);
            }

            c.commit();
            
        } catch(SQLException e) {
            logger.error("",e);
            throw new WorkspaceDatabaseException(e);
        } finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
                if (pstmt2 != null) {
                    pstmt2.close();
                }
                if (st != null) {
                    st.close();
                }
                if (c != null) {
                    c.setAutoCommit(true);
                    returnConnection(c);
                }
            } catch (SQLException sql) {
                logger.error("SQLException in finally cleanup", sql);
            }
        }

        this.associations = assocs;
    }

    /**
     * For now, only in-use flag is replaceable.
     * @param name name
     * @param entry assoc entry
     */
    public void replaceAssociationEntry(String name, AssociationEntry entry)
            throws WorkspaceDatabaseException {

        if (this.dbTrace) {
            logger.trace("replaceAssociationEntry()");
        }

        Connection c = null;
        PreparedStatement pstmt = null;
        try {
            c = getConnection();
            pstmt = AssociationPersistenceUtil.updateEntryInUse(name,entry,c);
            pstmt.executeUpdate();
        } catch(SQLException e) {
            logger.error("",e);
            throw new WorkspaceDatabaseException(e);
        } finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
                if (c != null) {
                    returnConnection(c);
                }
            } catch (SQLException sql) {
                logger.error("SQLException in finally cleanup", sql);
            }
        }

    }

    public Hashtable currentAssociations()
                            throws WorkspaceDatabaseException {
        return currentAssociations(true);
    }

    public synchronized Hashtable currentAssociations(boolean cachedIsFine)
                            throws WorkspaceDatabaseException {

        if (this.dbTrace) {
            logger.trace("currentAssociations()");
        }

        if (cachedIsFine) {
            if (this.associations != null) {
                return this.associations;
            }
        }

        Connection c = null;
        PreparedStatement pstmt = null;
        PreparedStatement pstmt2 = null;
        ResultSet rs = null;
        ResultSet rs2 = null;

        try {
            c = getConnection();
            pstmt = c.prepareStatement(SQL_SELECT_ALL_ASSOCIATIONS);
            rs = pstmt.executeQuery();

            if (rs == null || !rs.next()) {
                if (lager.traceLog) {
                    logger.debug("no previous networks (associations)");
                }
                return new Hashtable();
            }

            Hashtable assocs = new Hashtable();
            do {
                // rs was next'd above already
                String name = rs.getString(1);
                String dns = rs.getString(2);
                long fileTime = rs.getLong(3);
                Association assoc = new Association(dns);
                assoc.setFileTime(fileTime);

                pstmt2 = c.prepareStatement(SQL_SELECT_ASSOCIATION);
                pstmt2.setString(1, name);
                rs2 = pstmt2.executeQuery();

                ArrayList entries = new ArrayList();
                while (rs2.next()) {
                    AssociationEntry entry =
                                new AssociationEntry(rs2.getString(2),
                                                     rs2.getString(3),
                                                     rs2.getString(4),
                                                     rs2.getString(5),
                                                     rs2.getString(6),
                                                     rs2.getString(7));
                    entry.setInUse(rs2.getBoolean(8));

                    // Encoding that MAC is explicit in the MAC field itself.
                    // better to introduce a new field to schema?
                    String mac = entry.getMac();
                    if (mac.startsWith(AssociationPersistenceUtil.EXPLICIT_MAC_PREFIX)) {
                        mac = mac.substring(AssociationPersistenceUtil.EXPLICIT_MAC_PREFIX.length());
                        entry.setMac(mac);
                        entry.setExplicitMac(true);
                    }

                    entries.add(entry);
                }
                assoc.setEntries(entries);
                assocs.put(name,assoc);
                if (this.dbTrace) {
                    logger.trace("found previously stored network '" +
                            name + "':\n" + assoc);
                }

                pstmt2.close();
                rs2.close();

            } while (rs.next());

            rs = null;
            rs2 = null;
            pstmt = null;
            pstmt2 = null;

            this.associations = assocs;

            return assocs;

        } catch(SQLException e) {
            this.associations = null;
            logger.error("",e);
            throw new WorkspaceDatabaseException(e);
        } finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
                if (pstmt2 != null) {
                    pstmt2.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (rs2 != null) {
                    rs2.close();
                }
                if (c != null) {
                    returnConnection(c);
                }
            } catch (SQLException sql) {
                this.associations = null;
                logger.error("SQLException in finally cleanup", sql);
            }
        }

    }

    public void updateResourcepoolEntryAvailableMemory(String hostname,
                                                       int newAvailMemory,
                                                       int preemptibleMemory)
            throws WorkspaceDatabaseException {

        if (this.dbTrace) {
            logger.trace("updateResourcepoolEntryAvailableMemory()");
        }

        if (hostname == null) {
            throw new IllegalArgumentException("hostname may not be null");
        }

        if (newAvailMemory < 0) {
            throw new IllegalArgumentException("newAvailMemory must be non-negative");
        }

        if (preemptibleMemory < 0) {
            throw new IllegalArgumentException("preemptibleMemory must be non-negative");
        }

        Connection c = null;
        PreparedStatement pstmt = null;
        try {
            c = getConnection();

            pstmt =
                    c.prepareStatement(SQL_UPDATE_RESOURCE_POOL_ENTRY_MEMORY);

            pstmt.setInt(1, newAvailMemory);
            pstmt.setInt(2, preemptibleMemory);
            pstmt.setString(3, hostname);

            final int updated = pstmt.executeUpdate();
            if (updated != 1) {
                throw new WorkspaceDatabaseException("expected row update");
            }
        } catch(SQLException e) {
            logger.error("",e);
            throw new WorkspaceDatabaseException(e);
        } finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
                if (c != null) {
                    returnConnection(c);
                }
            } catch (SQLException sql) {
                logger.error("SQLException in finally cleanup", sql);
            }
        }

    }

    public boolean updateResourcepoolEntry(String hostname,
                                        String pool,
                                        String networks,
                                        Integer memoryMax,
                                        Integer memoryAvail,
                                        Boolean active)
            throws WorkspaceDatabaseException {

        if (this.dbTrace) {
            logger.trace("updateResourcepoolEntry()");
        }
        if (hostname == null) {
            throw new IllegalArgumentException("hostname may not be null");
        }

        final StringBuilder sb = new StringBuilder();
        final List<Object> params = new ArrayList<Object>(5);
        if (pool != null) {
            appendUpdatePair(sb, "resourcepool");
            params.add(pool);
        }
        if (networks != null) {
            appendUpdatePair(sb, "associations");
            params.add(networks);
        }
        if (memoryMax != null) {
            appendUpdatePair(sb, "maximum_memory");
            params.add(memoryMax);
        }
        if (memoryAvail != null) {
            appendUpdatePair(sb, "available_memory");
            params.add(memoryAvail);
        }

        if (active != null) {
            appendUpdatePair(sb, "active");
            params.add(active);
        }
        if (params.isEmpty()) {
            throw new IllegalArgumentException(
                    "at least one updated field must be specified");
        }

        Connection c = null;
        PreparedStatement pstmt = null;
        try {
            c = getConnection();

            final String q = String.format(
                    SQL_UPDATE_RESOURCE_POOL_ENTRY_SKELETAL, sb.toString());
            if (this.dbTrace) {
                logger.trace("resourcepool_entry update query: "+ q);
            }
            pstmt = c.prepareStatement(q);

            int paramIndex = 1;
            for (Object p : params) {
                pstmt.setObject(paramIndex, p);
                paramIndex++;
            }

            // add on the hostname param
            pstmt.setString(paramIndex, hostname);

            final int updated = pstmt.executeUpdate();
            return updated >= 1;
            
        } catch(SQLException e) {
            logger.error("",e);
            throw new WorkspaceDatabaseException(e);
        } finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
                if (c != null) {
                    returnConnection(c);
                }
            } catch (SQLException sql) {
                logger.error("SQLException in finally cleanup", sql);
            }
        }
    }

    private void appendUpdatePair(StringBuilder stringBuilder, String columnName) {
        if (stringBuilder.length() != 0) {
            stringBuilder.append(",");
        }
        stringBuilder.append(columnName).append("=?");
    }

    // one can only use result of this safely during service initialization
    public int memoryUsedOnPoolnode(String poolnode)

            throws WorkspaceDatabaseException {

        if (this.dbTrace) {
            logger.trace("memoryUsedOnPoolnode(): poolnode = " + poolnode);
        }

        Connection c = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            c = getConnection();

            pstmt = c.prepareStatement(SQL_JOIN_SELECT_RESOURCE_POOL_MEMORY);
            pstmt.setString(1, poolnode);
            rs = pstmt.executeQuery();

            if (rs == null) {
                if (this.dbTrace) {
                    logger.trace("memoryUsedOnPoolnode(): null result so " +
                                 "total is 0 MB");
                }
                return 0;
            }

            int total = 0;

            while (rs.next()) {
                int memory = rs.getInt(1);
                total += memory;
                if (this.dbTrace) {
                    logger.trace("memoryUsedOnPoolnode(): found " + memory +
                            " MB for one VM, new total is " + total);
                }
            }

            if (this.dbTrace) {
                logger.trace("memoryUsedOnPoolnode(): final total = " + total);
            }
            return total;
            
        } catch(SQLException e) {
            logger.error("",e);
            throw new WorkspaceDatabaseException(e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (pstmt != null) {
                    pstmt.close();
                }
                if (c != null) {
                    returnConnection(c);
                }
            } catch (SQLException sql) {
                logger.error("SQLException in finally cleanup", sql);
            }
        }
    }


    public List<ResourcepoolEntry> currentResourcepoolEntries()
            throws WorkspaceDatabaseException {
        Connection c = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            c = getConnection();
            pstmt = c.prepareStatement(SQL_SELECT_ALL_RESOURCE_POOL_ENTRIES);
            rs = pstmt.executeQuery();

            if (rs == null || !rs.next()) {
                return Collections.emptyList();
            }

            List<ResourcepoolEntry> list = new ArrayList<ResourcepoolEntry>();

            do {
                final ResourcepoolEntry entry = new ResourcepoolEntry(rs.getString(1),
                        rs.getString(2),
                        rs.getInt(4),
                        rs.getInt(5),
                        rs.getInt(7),
                        rs.getString(3),
                        rs.getBoolean(6));
                list.add(entry);
            }   while (rs.next());

            return list;

        } catch(SQLException e) {
            logger.error("",e);
            throw new WorkspaceDatabaseException(e);
        } finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (c != null) {
                    returnConnection(c);
                }
            } catch (SQLException sql) {
                logger.error("SQLException in finally cleanup", sql);
            }
        }

    }

    public ResourcepoolEntry getResourcepoolEntry(String hostname)
            throws WorkspaceDatabaseException {

        Connection c = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            c = getConnection();
            pstmt = c.prepareStatement(SQL_SELECT_RESOURCE_POOL_ENTRY);
            pstmt.setString(1, hostname);
            rs = pstmt.executeQuery();

            if (rs == null || !rs.next()) {
                return null;
            }

            return new ResourcepoolEntry(rs.getString(1),
                    rs.getString(2),
                    rs.getInt(4),
                    rs.getInt(5),
                    rs.getInt(7),
                    rs.getString(3),
                    rs.getBoolean(6));

        } catch(SQLException e) {
            logger.error("",e);
            throw new WorkspaceDatabaseException(e);
        } finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (c != null) {
                    returnConnection(c);
                }
            } catch (SQLException sql) {
                logger.error("SQLException in finally cleanup", sql);
            }
        }
    }

    public void addResourcepoolEntry(ResourcepoolEntry entry)
            throws WorkspaceDatabaseException {

        if (entry == null) {
            throw new IllegalArgumentException("entry may not be null");
        }

        if (this.dbTrace) {
            logger.trace("addResourcepoolEntry(): hostname = " + entry.getHostname());
        }

        Connection c = null;
        PreparedStatement pstmt = null;
        try {
            c = getConnection();

            pstmt = c.prepareStatement(SQL_INSERT_RESOURCE_POOL_ENTRY);

            pstmt.setString(1, entry.getResourcePool());
            pstmt.setString(2, entry.getHostname());
            pstmt.setString(3, entry.getSupportedAssociations());
            pstmt.setInt(4, entry.getMemMax());
            pstmt.setInt(5, entry.getMemCurrent());
            pstmt.setInt(6, entry.isActive() ? 1 : 0);

            final int updated = pstmt.executeUpdate();

            if (this.dbTrace) {
                logger.trace("Inserted " + updated + " row(s)");
            }

        } catch(SQLException e) {
            logger.error("",e);
            throw new WorkspaceDatabaseException(e);
        } finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
                if (c != null) {
                    returnConnection(c);
                }
            } catch (SQLException sql) {
                logger.error("SQLException in finally cleanup", sql);
            }
        }
    }

    public boolean removeResourcepoolEntry(String hostname)
            throws WorkspaceDatabaseException {
        if (hostname == null) {
            throw new IllegalArgumentException("hostname may not be null");
        }

        if (this.dbTrace) {
            logger.trace("removeResourcepoolEntry(): hostname = " + hostname);
        }

        Connection c = null;
        PreparedStatement pstmt = null;
        try {
            c = getConnection();
            pstmt = c.prepareStatement(SQL_DELETE_RESOURCE_POOL_ENTRY);

            pstmt.setString(1, hostname);

            final int updated = pstmt.executeUpdate();

            if (this.dbTrace) {
                logger.trace("Deleted " + updated + " row(s)");
            }

            return updated > 0;

        } catch(SQLException e) {
            logger.error("",e);
            throw new WorkspaceDatabaseException(e);
        } finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
                if (c != null) {
                    returnConnection(c);
                }
            } catch (SQLException sql) {
                logger.error("SQLException in finally cleanup", sql);
            }
        }
    }

    private Connection getConnection() throws SQLException {
        return this.dataSource.getConnection();
    }

    private static void returnConnection(Connection connection) {

        if(connection != null) {
            try {
                connection.close();
            } catch(SQLException e) {
                logger.error("",e);
            }
        }
    }

    /* ******************************************************************** */

    // TEMPORARY:  for propagation implementation

    public void updateCursorPosition(long filepos)
            throws WorkspaceDatabaseException {

        final String SQL_UPDATE_CURSOR_POSITION =
            "UPDATE notification_position SET position=? WHERE 1=1";
        
        Connection c = null;
        PreparedStatement pstmt = null;
        try {
            c = getConnection();
            pstmt = c.prepareStatement(SQL_UPDATE_CURSOR_POSITION);
            pstmt.setLong(1, filepos);
            pstmt.executeUpdate();
        } catch(SQLException e) {
            logger.error("",e);
            throw new WorkspaceDatabaseException(e);
        } finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
                if (c != null) {
                    returnConnection(c);
                }
            } catch (SQLException sql) {
                logger.error("SQLException in finally cleanup", sql);
            }
        }
    }

    public long currentCursorPosition() throws WorkspaceDatabaseException {

        final String SQL_INSERT_CURSOR_POSITION =
            "INSERT INTO notification_position VALUES(0)";

        final String SQL_SELECT_CURSOR_POSITION =
            "SELECT position FROM notification_position";

        Connection c = null;
        PreparedStatement pstmt = null;
        PreparedStatement pstmt2 = null;
        ResultSet rs = null;
        try {
            c = getConnection();
            pstmt = c.prepareStatement(SQL_SELECT_CURSOR_POSITION);

            rs = pstmt.executeQuery();

            if (rs == null || !rs.next()) {
                pstmt2 = c.prepareStatement(SQL_INSERT_CURSOR_POSITION);
                pstmt2.executeUpdate();
                return 0;
            }

            return rs.getLong(1);

        } catch(SQLException e) {
            logger.error("",e);
            throw new WorkspaceDatabaseException(e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (pstmt != null) {
                    pstmt.close();
                }
                if (pstmt2 != null) {
                    pstmt2.close();
                }
                if (c != null) {
                    returnConnection(c);
                }
            } catch (SQLException sql) {
                logger.error("SQLException in finally cleanup", sql);
            }
        }
    }

    public void updatePropagationCounter(int n)
                        throws WorkspaceDatabaseException {

        if (this.dbTrace) {
            logger.trace("updatePropagationCounter()");
        }

        final String SQL_SET_PROP_COUNTER =
                    "UPDATE counter SET pending=? WHERE id=1";

        this.updateCounter(n, SQL_SET_PROP_COUNTER);

    }

    private void updateCounter(int n, String prepd)
                    throws WorkspaceDatabaseException {

        Connection c = null;
        PreparedStatement pstmt = null;
        try {
            c = getConnection();
            pstmt = c.prepareStatement(prepd);
            pstmt.setInt(1, n);
            pstmt.executeUpdate();
        } catch(SQLException e) {
            logger.error("",e);
            throw new WorkspaceDatabaseException(e);
        } finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
                if (c != null) {
                    returnConnection(c);
                }
            } catch (SQLException sql) {
                logger.error("SQLException in finally cleanup", sql);
            }
        }
    }

    public int readPropagationCounter() throws WorkspaceDatabaseException {

        if (this.dbTrace) {
            logger.trace("readPropagationCounter()");
        }

        final String SQL_SELECT_PROP_COUNTER =
                        "SELECT pending FROM counter where id=1";

        return this.readCounter(1, SQL_SELECT_PROP_COUNTER);
    }

    private int readCounter(int n, String prepd)
                        throws WorkspaceDatabaseException {

        final String SQL_INSERT_COUNTER = "INSERT INTO counter VALUES(?,?)";

        Connection c = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            c = getConnection();
            pstmt = c.prepareStatement(prepd);
            rs = pstmt.executeQuery();

            if (rs == null || !rs.next()) {
                pstmt.close();
                pstmt = c.prepareStatement(SQL_INSERT_COUNTER);
                pstmt.setInt(1, n);
                pstmt.setInt(2, 0);
                pstmt.executeUpdate();
                return 0;
            } else {
                return rs.getInt(1);
            }
        } catch(SQLException e) {
            logger.error("",e);
            throw new WorkspaceDatabaseException(e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (pstmt != null) {
                    pstmt.close();
                }
                if (c != null) {
                    returnConnection(c);
                }
            } catch (SQLException sql) {
                logger.error("SQLException in finally cleanup", sql);
            }
        }
    }
    
    public List<ResourcepoolEntry> getAvailableEntriesSortedByFreeMemoryPercentage(int requestedMem)
            throws WorkspaceDatabaseException{

        Connection c = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        ArrayList<ResourcepoolEntry> entries = new ArrayList<ResourcepoolEntry>();

        try {
            c = getConnection();
            pstmt = c.prepareStatement(SQL_SELECT_AVAILABLE_ENTRIES);
            pstmt.setInt(1, requestedMem);
            rs = pstmt.executeQuery();

            if (rs == null || !rs.next()) {
                if (lager.traceLog) {
                    logger.debug("no available resource pool entries");
                }
            } else do {
                // rs was next'd above already
                String name = rs.getString(1);
                String hostname = rs.getString(2);
                String assocs = rs.getString(3);

                if (hostname == null) {
                    logger.error("hostname cannot be null for resource pool entry");
                    continue;
                }

                if (assocs == null) {
                    logger.error("assocs cannot be null for resource pool entry");
                    continue;
                }

                ResourcepoolEntry entry =
                    new ResourcepoolEntry(name,
                            hostname,
                            rs.getInt(4),
                            rs.getInt(5),
                            rs.getInt(7),
                            assocs,
                            rs.getBoolean(6));
                entries.add(entry);

            } while (rs.next());

            return entries;

        } catch(SQLException e) {
            logger.error("",e);
            throw new WorkspaceDatabaseException(e);
        } finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (c != null) {
                    returnConnection(c);
                }
            } catch (SQLException sql) {
                logger.error("SQLException in finally cleanup", sql);
            }
        }                
    }


    private synchronized Integer getTotalMemory(String sql) throws WorkspaceDatabaseException {

        Connection c = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            c = getConnection();

            pstmt = c.prepareStatement(sql);
            rs = pstmt.executeQuery();
            
            if (rs == null) {
                if (this.dbTrace) {
                    logger.trace("getTotalMemory(): null result so " +
                                 "total is 0 MB");
                }
                return 0;
            }

            Integer total = 0;

            if(rs.next()){
                total = rs.getInt(1);
            } 
            
            return total;
            
        } catch(SQLException e) {
            logger.error("",e);
            throw new WorkspaceDatabaseException(e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (pstmt != null) {
                    pstmt.close();
                }
                if (c != null) {
                    returnConnection(c);
                }
            } catch (SQLException e) {
                logger.error("SQLException in finally cleanup", e);
            }
        }
    }


    public Integer getTotalAvailableMemory(Integer multipleOf) throws WorkspaceDatabaseException {
        if (this.dbTrace) {
            logger.trace("getTotalAvailableMemory(" + multipleOf + ")");
        }
                
        Integer total = 0;
        
        Connection c = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            c = getConnection();

            pstmt = c.prepareStatement(SQL_SELECT_MULTIPLE_OF_AVAILABLE_MEMORY);
            pstmt.setInt(1, multipleOf);
            rs = pstmt.executeQuery();
            
            if (rs == null) {
                if (this.dbTrace) {
                    logger.trace("getTotalMemory(): null result so " +
                                 "total is 0 MB");
                }
                return 0;
            }

            if(rs.next()){
                total = rs.getInt(1);
            } 
                        
        } catch(SQLException e) {
            logger.error("",e);
            throw new WorkspaceDatabaseException(e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (pstmt != null) {
                    pstmt.close();
                }
                if (c != null) {
                    returnConnection(c);
                }
            } catch (SQLException e) {
                logger.error("SQLException in finally cleanup", e);
            }
        }        
        
        if (this.dbTrace) {
            logger.trace("getTotalAvailableMemory(" + multipleOf + "): total available memory = " + total);
        }

        return total;
    }

    public Integer getTotalAvailableMemory() throws WorkspaceDatabaseException {
        if (this.dbTrace) {
            logger.trace("getTotalAvailableMemory()");
        }
        
        Integer total = getTotalMemory(SQL_SELECT_TOTAL_AVAILABLE_MEMORY);
        
        if (this.dbTrace) {
            logger.trace("getTotalAvailableMemory(): total max memory = " + total);
        }

        return total;
    }
    
    public Integer getTotalMaxMemory() throws WorkspaceDatabaseException {
        if (this.dbTrace) {
            logger.trace("getTotalMaxMemory()");
        }
        
        Integer total = getTotalMemory(SQL_SELECT_TOTAL_MAX_MEMORY);
        
        if (this.dbTrace) {
            logger.trace("getTotalMaxMemory(): total max memory = " + total);
        }

        return total;
    }


    public Integer getTotalPreemptableMemory() throws WorkspaceDatabaseException {
        if (this.dbTrace) {
            logger.trace("getTotalPreemptableMemory()");
        }
        
        Integer total = getTotalMemory(SQL_SELECT_TOTAL_PREEMPTABLE_MEMORY);
        
        if (this.dbTrace) {
            logger.trace("getTotalPreemptableMemory(): total pre-emptable memory = " + total);
        }

        return total;
    }
    
    public Integer getUsedNonPreemptableMemory() throws WorkspaceDatabaseException {
        if (this.dbTrace) {
            logger.trace("getUsedNonPreemptableMemory()");
        }
        
        Integer total = getTotalMemory(SQL_SELECT_USED_NON_PREEMPTABLE_MEMORY);
        
        if (this.dbTrace) {
            logger.trace("getUsedNonPreemptableMemory(): used non pre-emptable memory = " + total);
        }

        return total;
    }

    public void addSpotPriceHistory(Calendar timeStamp, Double newPrice) throws WorkspaceDatabaseException{
        if (this.dbTrace) {
            logger.trace("addSpotPriceHistory(): timeStamp = " + timeStamp + ", spot price = " + newPrice);
        }

        Connection c = null;
        PreparedStatement pstmt = null;
        try {
            c = getConnection();
            pstmt = c.prepareStatement(SQL_INSERT_SPOT_PRICE);

            if (timeStamp != null) {
                pstmt.setLong(1,
                    new Long(timeStamp.getTimeInMillis()));
            } else {
                pstmt.setInt(1, 0);
            }

            pstmt.setDouble(2, newPrice);
            final int updated = pstmt.executeUpdate();

            if (this.dbTrace) {
                logger.trace("addSpotPriceHistory(): updated " + updated + " rows");
            }

        } catch(SQLException e) {
            logger.error("",e);
            throw new WorkspaceDatabaseException(e);
        } finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
                if (c != null) {
                    returnConnection(c);
                }
            } catch (SQLException sql) {
                logger.error("SQLException in finally cleanup", sql);
            }
        }
    }


    public List<SpotPriceEntry> getSpotPriceHistory(Calendar startDate,
            Calendar endDate) throws WorkspaceDatabaseException {
        if (this.dbTrace) {
            logger.trace("getSpotPriceHistory() startDate: " + startDate == null? null : startDate.getTime() 
                            + ". endDate: " + endDate == null? null : endDate.getTime());
        }

        Connection c = null;
        Statement st = null;
        ResultSet rs = null;

        try {
            c = getConnection();
            st = c.createStatement();
            
            String statement = SQL_SELECT_SPOT_PRICE;
            
            if(startDate != null || endDate != null){
                statement += " WHERE ";
                
                if(startDate != null){
                    statement += "tstamp >= " + startDate.getTimeInMillis(); 
                    if(endDate != null){
                        statement += " AND";
                    }
                }
                
                if(endDate != null){
                    statement += " tstamp <= " + endDate.getTimeInMillis();
                }
            }
            
            rs = st.executeQuery(statement);
            
            if (rs == null || !rs.next()) {
                if (lager.traceLog) {
                    logger.debug("no previous spot price history");
                }
                return new LinkedList<SpotPriceEntry>();
            }

            List<SpotPriceEntry> result = new LinkedList<SpotPriceEntry>();
            do {
                // rs was next'd above already
                Long timeMillis = rs.getLong(1);                
                Double spotPrice = rs.getDouble(2);
                _SpotPriceEntry spotPriceEntry = repr._newSpotPriceEntry();
                
                Calendar timeStamp = Calendar.getInstance();
                timeStamp.setTimeInMillis(timeMillis);

                spotPriceEntry.setTimeStamp(timeStamp);
                spotPriceEntry.setSpotPrice(spotPrice);
                
                result.add(spotPriceEntry);
                
                if (this.dbTrace) {
                    logger.trace("found spot price entry: '" +
                            timeStamp + " : " + spotPrice + "'");
                }
            } while (rs.next());

            return result;
            
        } catch(SQLException e) {
            logger.error("",e);
            throw new WorkspaceDatabaseException(e);
        } finally {
            try {
                if (st != null) {
                    st.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (c != null) {
                    returnConnection(c);
                }
            } catch (SQLException sql) {
                logger.error("SQLException in finally cleanup", sql);
            }
        }
    }
    
    public Double getLastSpotPrice() throws WorkspaceDatabaseException {
        if (this.dbTrace) {
            logger.trace("getLastSpotPrice()");
        }

        Connection c = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            c = getConnection();
            pstmt = c.prepareStatement(SQL_SELECT_LAST_SPOT_PRICE);
            
            rs = pstmt.executeQuery();
            
            if (rs == null || !rs.next()) {
                if (lager.traceLog) {
                    logger.debug("no previous spot price");
                }
                return null;
            }

            double result = rs.getDouble(1);
            
            if(rs.next()){
                logger.warn("Wrong behavior: multiple spot prices from last time stamp.");
            }
            
            return result;
            
        } catch(SQLException e) {
            logger.error("",e);
            throw new WorkspaceDatabaseException(e);
        } finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (c != null) {
                    returnConnection(c);
                }
            } catch (SQLException sql) {
                logger.error("SQLException in finally cleanup", sql);
            }
        }
    }

    public Backfill getStoredBackfill() throws WorkspaceDatabaseException {
        if (this.dbTrace) {
            logger.trace("getStoredBackfill()");
        }

        Connection c = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            c = getConnection();
            pstmt = c.prepareStatement(SQL_SELECT_BACKFILL);

            rs = pstmt.executeQuery();

            if (rs == null || !rs.next()) {
                if (lager.traceLog) {
                    logger.debug("no previous backfill");
                }
                return null;
            }

            Backfill bf = new Backfill(null, null);
            bf.setBackfillDisabled(rs.getBoolean(1));
            bf.setMaxInstances(rs.getInt(2));
            bf.setDiskImage(rs.getString(3));
            bf.setMemoryMB(rs.getInt(4));
            bf.setVcpus(rs.getInt(5));
            bf.setDurationSeconds(rs.getInt(6));
            bf.setNetwork(rs.getString(7));
            bf.setSiteCapacity(rs.getInt(8));
            return bf;

        } catch(SQLException e) {
            logger.error("",e);
            throw new WorkspaceDatabaseException(e);
        } finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (c != null) {
                    returnConnection(c);
                }
            } catch (SQLException sql) {
                logger.error("SQLException in finally cleanup", sql);
            }
        }
    }

    public synchronized void setBackfill(Backfill bf) throws WorkspaceDatabaseException {
        if (bf == null) {
            throw new IllegalArgumentException("backfill may not be null");
        }

        if (this.dbTrace) {
            logger.trace("setBackfill()");
        }

        Connection c = null;
        PreparedStatement pstmt = null;
        try {
            c = getConnection();

            // Need to determine whether to use insert or update
            Backfill previous = this.getStoredBackfill();
            if (previous == null) {
                pstmt = c.prepareStatement(SQL_INSERT_BACKFILL);
            } else {
                pstmt = c.prepareStatement(SQL_UPDATE_BACKFILL);
            }

            pstmt.setInt(1, bf.isBackfillDisabled() ? 1 : 0);
            pstmt.setInt(2, bf.getMaxInstances());
            pstmt.setString(3, bf.getDiskImage());
            pstmt.setInt(4, bf.getMemoryMB());
            pstmt.setInt(5, bf.getVcpus());
            pstmt.setInt(6, bf.getDurationSeconds());
            pstmt.setString(7, bf.getNetwork());
            pstmt.setInt(8, bf.getSiteCapacity());

            final int updated = pstmt.executeUpdate();
            if (this.dbTrace) {
                logger.trace("Updated/inserted backfill");
            }

        } catch(SQLException e) {
            logger.error("",e);
            throw new WorkspaceDatabaseException(e);
        } finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
                if (c != null) {
                    returnConnection(c);
                }
            } catch (SQLException sql) {
                logger.error("SQLException in finally cleanup", sql);
            }
        }
    }

    /**
     * Retrieves idempotency reservation
     *
     * @param creatorId   initiating client user
     * @param clientToken client-provided idempotency token
     * @return stored reservation, or null of not found
     * @throws WorkspaceDatabaseException
     *          DB error
     */
    public IdempotentReservation getIdempotentReservation(String creatorId, String clientToken)
            throws WorkspaceDatabaseException {

        if (creatorId == null) {
            throw new IllegalArgumentException("creatorId may not be null");
        }
        if (clientToken == null) {
            throw new IllegalArgumentException("clientToken may not be null");
        }

        if (this.dbTrace) {
            logger.trace("getIdempotentReservation()");
        }

        Connection c = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            c = getConnection();

            pstmt = c.prepareStatement(SQL_SELECT_IDEMPOTENT_CREATION);

            pstmt.setString(1, creatorId);
            pstmt.setString(2, clientToken);

            rs = pstmt.executeQuery();

            if (rs == null || !rs.next()) {
                if (lager.traceLog) {
                    logger.debug("no existing idempotency reservation");
                }
                return null;
            }

            ArrayList<IdempotentInstance> instances = new ArrayList<IdempotentInstance>();
            // rs was next'd above already
            final String groupId = rs.getString(2);
            do {
                final int vmId = rs.getInt(1);
                final String name = rs.getString(3);
                final int launchIndex = rs.getInt(4);

                instances.add(new IdempotentInstanceImpl(vmId, name, launchIndex));
            } while(rs.next());

            return new IdempotentReservationImpl(creatorId, clientToken, groupId, instances);


        } catch(SQLException e) {
            logger.error("",e);
            throw new WorkspaceDatabaseException(e);
        } finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }

                if (rs != null) {
                    rs.close();
                }

                if (c != null) {
                    returnConnection(c);
                }
            } catch (SQLException sql) {
                logger.error("SQLException in finally cleanup", sql);
            }
        }
    }

    /**
     * Stores idempotency reservation
     *
     * @param reservation the reservation to store
     * @throws WorkspaceDatabaseException
     *          DB error
     */
    public void addIdempotentReservation(IdempotentReservation reservation)
            throws WorkspaceDatabaseException {
        if (this.dbTrace) {
            logger.trace("addIdempotentReservation()");
        }

        Connection c = null;
        PreparedStatement[] pstmts = null;
        try {
            c = getConnection();
            c.setAutoCommit(false);

            pstmts = IdempotencyPersistenceUtil.getInsertReservation(reservation, c);

            for (PreparedStatement pstmt : pstmts) {
                pstmt.executeUpdate();
            }
            c.commit();

            if (this.dbTrace) {
                logger.trace("Inserted idempotency reservation");
            }

        } catch(SQLException e) {
            logger.error("",e);
            throw new WorkspaceDatabaseException(e);
        } finally {
            try {
                if (pstmts != null) {
                    for (PreparedStatement pstmt : pstmts) {
                        pstmt.close();
                    }
                }
                if (c != null) {
                    c.setAutoCommit(true);
                    returnConnection(c);
                }
            } catch (SQLException sql) {
                logger.error("SQLException in finally cleanup", sql);
            }
        }
    }

    /**
     * Removes existing idempotency reservation
     *
     * @param creatorId   initiating client user
     * @param clientToken client-provided idempotency token
     * @throws WorkspaceDatabaseException
     *          DB error
     */
    public void removeIdempotentReservation(String creatorId, String clientToken)
            throws WorkspaceDatabaseException {

        if (creatorId == null) {
            throw new IllegalArgumentException("creatorId may not be null");
        }
        if (clientToken == null) {
            throw new IllegalArgumentException("clientToken may not be null");
        }

        if (this.dbTrace) {
            logger.trace("removeIdempotentReservation()");
        }

        Connection c = null;
        PreparedStatement pstmt = null;
        try {
            c = getConnection();
            pstmt = c.prepareStatement(SQL_DELETE_IDEMPOTENT_CREATION);
            pstmt.setString(1, creatorId);
            pstmt.setString(2, clientToken);

            pstmt.executeUpdate();

        } catch(SQLException e) {
            logger.error("",e);
            throw new WorkspaceDatabaseException(e);
        } finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
                if (c != null) {
                    returnConnection(c);
                }
            } catch (SQLException sql) {
                logger.error("SQLException in finally cleanup", sql);
            }
        }
    }
}
