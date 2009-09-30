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

package org.globus.workspace.scheduler.defaults.pilot;

import org.globus.workspace.persistence.WorkspaceDatabaseException;
import org.globus.workspace.Lager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;

public class PilotSlotManagementDB implements PilotSlotManagementConstants {

    private static final Log logger =
        LogFactory.getLog(PilotSlotManagementDB.class.getName());

    private final DataSource dataSource;
    private final Lager lager;

    private static final int[] zeroLen = new int[0];

    private int numActiveSlotsCache;

    /* ************ */
    /* Construction */
    /* ************ */

    PilotSlotManagementDB(DataSource dsrc, Lager lagerImpl) throws Exception {
        if (dsrc == null) {
            throw new IllegalArgumentException("dsrc may not be null");
        }
        this.dataSource = dsrc;

        if (lagerImpl == null) {
            throw new IllegalArgumentException("lagerImpl may not be null");
        }
        this.lager = lagerImpl;

        try {
            this.prepareStatements();
        } catch (WorkspaceDatabaseException e) {
            throw new Exception("Problem preparing DB statements: ", e);
        }

        this.numActiveSlotsCache = this.countSlots();
    }

    /* ****** */
    /* Caches */
    /* ****** */

    /**
     * @param adjusted if true, we are telling the cache to update itself
     * @return number of current slots
     * @throws org.globus.workspace.persistence.WorkspaceDatabaseException problem updating cache
     */
    synchronized int numSlotsCached(boolean adjusted)
            throws WorkspaceDatabaseException {
        if (adjusted) {
            this.numActiveSlotsCache = this.countSlots();
        }
        return this.numActiveSlotsCache;
    }

    /* **************** */
    /* Database methods */
    /* **************** */
        
    PilotSlot getSlotAndAssignVMImpl(String uuid,
                                             String hostname)
            throws WorkspaceDatabaseException, SlotNotFoundException {

        Connection c = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            c = getConnection();
            pstmt = c.prepareStatement(SQL_LOAD_SLOTS_NOHOSTNAME);
            pstmt.setString(1, uuid);

            rs = pstmt.executeQuery();

            if (rs == null || !rs.next()) {
                throw new SlotNotFoundException("There are no slot " +
                        "entries with this UUID and no hostname yet. " +
                        "UUID='" + uuid + "', hostname='" + hostname + "'");
            }

            int vmid = rs.getInt(1);
            boolean terminal = rs.getBoolean(2);
            String lrmhandle = rs.getString(3);
            int duration = rs.getInt(4);
            boolean partofgroup = rs.getBoolean(5);
            boolean pendingRemove = rs.getBoolean(6);

            pstmt.close();
            pstmt = null;

            pstmt = c.prepareStatement(SQL_UPDATE_SLOT_NODENAME);
            pstmt.setString(1, hostname);
            pstmt.setString(2, uuid);
            pstmt.setInt(3, vmid);

            pstmt.executeUpdate();

            logger.debug("Assigned vm id " + vmid + " to hostname = '" +
                         hostname + "', uuid = '" + uuid + "'");

            return new PilotSlot(uuid, vmid, false,
                                 terminal, lrmhandle,
                                 duration, hostname,
                                 partofgroup, pendingRemove);

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

    PilotSlot getSlot(String uuid, String hostname)
            throws WorkspaceDatabaseException, SlotNotFoundException {

        Connection c = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            c = getConnection();
            pstmt = c.prepareStatement(SQL_LOAD_SLOT);
            pstmt.setString(1, uuid);
            pstmt.setString(2, hostname);

            rs = pstmt.executeQuery();

            if (rs == null || !rs.next()) {
                throw new SlotNotFoundException(uuid, hostname);
            }

            int vmid = rs.getInt(1);
            boolean pending = rs.getBoolean(2);
            boolean terminal = rs.getBoolean(3);
            String lrmhandle = rs.getString(4);
            int duration = rs.getInt(5);
            boolean partofgroup = rs.getBoolean(6);
            boolean pendingRemove = rs.getBoolean(7);

            return new PilotSlot(uuid, vmid, pending,
                                 terminal, lrmhandle,
                                 duration, hostname,
                                 partofgroup, pendingRemove);

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

    // assumes no slotid-vmid one-to-many relation yet, just one to one
    PilotSlot getSlot(int vmid)
            throws WorkspaceDatabaseException, SlotNotFoundException {

        Connection c = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            c = getConnection();
            pstmt = c.prepareStatement(SQL_LOAD_SLOT_BY_VM);
            pstmt.setInt(1, vmid);

            rs = pstmt.executeQuery();

            if (rs == null || !rs.next()) {
                throw new SlotNotFoundException(vmid);
            }

            String uuid = rs.getString(1);
            boolean pending = rs.getBoolean(2);
            boolean terminal = rs.getBoolean(3);
            String lrmhandle = rs.getString(4);
            int duration = rs.getInt(5);
            boolean partOfGroup = rs.getBoolean(6);
            boolean pendingRemove = rs.getBoolean(7);
            String nodename = rs.getString(8);

            return new PilotSlot(uuid, vmid, pending, terminal,
                                 lrmhandle, duration, nodename,
                                 partOfGroup, pendingRemove);

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

    int countSlots() throws WorkspaceDatabaseException {

        Connection c = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            c = getConnection();
            pstmt = c.prepareStatement(SQL_SELECT_NUM_SLOTS);

            rs = pstmt.executeQuery();

            if (rs == null || !rs.next()) {
                return 0;
            }

            return rs.getInt(1);

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

    void setSlotPendingRemove(PilotSlot slot)
            throws WorkspaceDatabaseException, SlotNotFoundException {

        Connection c = null;
        PreparedStatement pstmt = null;
        try {
            c = getConnection();
            pstmt = c.prepareStatement(SQL_UPDATE_SLOT_PENDING_REMOVE_SET_TRUE);
            pstmt.setString(1, slot.uuid);
            pstmt.setString(2, slot.nodename);

            final int updated = pstmt.executeUpdate();

            if (this.lager.dbLog) {
                logger.trace("updated " + updated + " rows");
            }

            if (updated == 0) {
                throw new SlotNotFoundException(slot.uuid, slot.nodename);
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

    void setSlotTerminal(PilotSlot slot)
            throws WorkspaceDatabaseException, SlotNotFoundException {

        Connection c = null;
        PreparedStatement pstmt = null;
        try {
            c = getConnection();
            pstmt = c.prepareStatement(SQL_UPDATE_SLOT_TERMINAL_SET_TRUE);
            pstmt.setString(1, slot.uuid);
            pstmt.setString(2, slot.nodename);

            final int updated = pstmt.executeUpdate();

            if (this.lager.dbLog) {
                logger.trace("updated " + updated + " rows");
            }

            if (updated == 0) {
                throw new SlotNotFoundException(slot.uuid, slot.nodename);
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

    void newGroupMember(String groupid, int vmid)
            throws WorkspaceDatabaseException {

        Connection c = null;
        PreparedStatement pstmt = null;
        try {
            c = getConnection();
            pstmt = c.prepareStatement(SQL_INSERT_GROUP_MEMBER);
            pstmt.setString(1, groupid);
            pstmt.setInt(2, vmid);

            final int inserted = pstmt.executeUpdate();

            if (this.lager.dbLog) {
                logger.trace("inserted " + inserted + " rows");
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

    int[] findVMsInGroup(String groupID)

            throws WorkspaceDatabaseException {

        if (groupID == null) {
            throw new WorkspaceDatabaseException("groupID is null");
        }

        Connection c = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            c = getConnection();

            pstmt = c.prepareStatement(SQL_SELECT_ALL_VMS_IN_GROUP);
            pstmt.setString(1, groupID);

            rs = pstmt.executeQuery();

            if (rs == null || !rs.next()) {
                logger.debug("no VMs found with groupID = " + groupID);
                return zeroLen;
            }

            final ArrayList vmidsList = new ArrayList(64);
            do {
                vmidsList.add(new Integer(rs.getInt(1)));
            } while (rs.next());

            // can't use toArray without converting to Integer[]
            final int[] ret = new int[vmidsList.size()];
            for (int i = 0; i < ret.length; i++) {
                ret[i] = ((Number) vmidsList.get(i)).intValue();
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

    void newSlot(String uuid, int vmid, String lrmid, long duration)
            throws WorkspaceDatabaseException {

        Connection c = null;
        PreparedStatement pstmt = null;
        try {
            c = getConnection();
            pstmt = c.prepareStatement(SQL_INSERT_SLOT);
            pstmt.setString(1, uuid);
            pstmt.setInt(2, vmid);
            pstmt.setString(3, lrmid);
            pstmt.setLong(4, duration);
            pstmt.setNull(5, Types.VARCHAR);

            final int inserted = pstmt.executeUpdate();

            if (this.lager.dbLog) {
                logger.trace("inserted " + inserted + " rows");
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

        // newSlot affects slotnum
        this.numSlotsCached(true);
    }

    void newSlotGroup(String uuid, int[] vmids,
                              String lrmid, long duration)
            throws WorkspaceDatabaseException {

        Connection c = null;
        PreparedStatement pstmt = null;
        try {
            c = getConnection();
            for (int i = 0; i < vmids.length; i++) {
                pstmt = c.prepareStatement(SQL_INSERT_GROUP_SLOT);
                pstmt.setString(1, uuid);
                pstmt.setInt(2, vmids[i]);
                pstmt.setString(3, lrmid);
                pstmt.setLong(4, duration);
                pstmt.setNull(5, Types.VARCHAR);

                pstmt.executeUpdate();
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

        // newSlot affects slotnum
        this.numSlotsCached(true);
    }

    void removeSlot(String uuid) throws WorkspaceDatabaseException {

        Connection c = null;
        PreparedStatement pstmt = null;
        try {
            c = getConnection();
            pstmt = c.prepareStatement(SQL_DELETE_SLOT);
            pstmt.setString(1, uuid);

            final int deleted = pstmt.executeUpdate();

            if (this.lager.dbLog) {
                logger.trace("deleted " + deleted + " rows");
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

        // removeSlot affects slotnum
        this.numSlotsCached(true);
    }

    void updateCursorPosition(long filepos)
            throws WorkspaceDatabaseException {

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

    long currentCursorPosition() throws WorkspaceDatabaseException {

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

    // DB utility methods:

    /**
     * This moves significant prepared statement setup times to service
     * initialization instead of the first time they're used.
     *
     * Documentation states that PreparedStatement caches are per pool
     * connection but preliminary testing indicates it is effective to
     * just use the first one from the pool.
     *
     * @throws org.globus.workspace.persistence.WorkspaceDatabaseException exc
     */
    private void prepareStatements() throws WorkspaceDatabaseException {

        //String[] ins =
        //    PilotSlotManagementConstants.INSENSITIVE_PREPARED_STATEMENTS;

        final String[] pstmts = PREPARED_STATEMENTS;

        Connection c = null;
        PreparedStatement pstmt = null;
        try {
            c = getConnection();

            //for (int i = 0; i < ins.length; i++) {
            //  pstmt = c.prepareStatement(ins[i],
            //                             ResultSet.TYPE_SCROLL_INSENSITIVE,
            //                             ResultSet.CONCUR_UPDATABLE);
            //  pstmt.close();
            //}

            for (int i = 0; i < pstmts.length; i++) {
                pstmt = c.prepareStatement(pstmts[i]);
                pstmt.close();
                pstmt = null;
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
            } catch (SQLException e) {
                logger.error("SQLException in finally cleanup",e);
            }
        }
    }

    private Connection getConnection() throws WorkspaceDatabaseException {
        try {
            return this.dataSource.getConnection();
        } catch (SQLException e) {
            throw new WorkspaceDatabaseException(e);
        }
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
    
}
