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

package org.globus.workspace.scheduler.defaults;

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
import java.util.Calendar;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class DefaultSchedulerAdapterDB {

    private static final Log logger =
        LogFactory.getLog(DefaultSchedulerAdapterDB.class.getName());

    private final DataSource source;
    private final Lager lager;

    public DefaultSchedulerAdapterDB(DataSource dataSource, Lager lagerImpl) {
        if (dataSource == null) {
            throw new IllegalArgumentException("dataSource is null");
        }
        this.source = dataSource;

        if (lagerImpl == null) {
            throw new IllegalArgumentException("lagerImpl may not be null");
        }
        this.lager = lagerImpl;
    }

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
    void prepareStatements() throws WorkspaceDatabaseException {

        final String[] ins =
            DefaultSchedulerConstants.INSENSITIVE_PREPARED_STATEMENTS;

        final String[] pstmts =
            DefaultSchedulerConstants.PREPARED_STATEMENTS;

        Connection c = null;
        PreparedStatement pstmt = null;
        try {
            c = getConnection();

            for (int i = 0; i < ins.length; i++) {
                pstmt = c.prepareStatement(ins[i],
                                           ResultSet.TYPE_SCROLL_INSENSITIVE,
                                           ResultSet.CONCUR_UPDATABLE);
                pstmt.close();
            }

            for (int i = 0; i < pstmts.length; i++) {
                pstmt = c.prepareStatement(pstmts[i]);
                pstmt.close();
            }

            pstmt = null;

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

    void backOutTasks(int id) throws WorkspaceDatabaseException {

        Connection c = null;
        PreparedStatement pstmt = null;
        try {
            c = getConnection();
            pstmt = c.prepareStatement(
                    DefaultSchedulerConstants.SQL_DELETE_TASKS);
            pstmt.setInt(1, id);
            int deleted = pstmt.executeUpdate();

            // this will be zero deletes in the case where slot has not
            // been activated yet but client has called destroy (the only
            // legal operation it can call before slot is activated)

            if (lager.dbLog) {
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
    }

    void scheduleTasks(int id, Calendar stop)
                throws WorkspaceDatabaseException {

        if (lager.traceLog) {
            logger.trace("scheduleTasks(): " + Lager.id(id));
        }

        Connection c = null;
        PreparedStatement pstmt = null;
        try {
            c = getConnection();
            pstmt = c.prepareStatement(
                        DefaultSchedulerConstants.SQL_INSERT_TASK);
            pstmt.setInt(1, id);
            pstmt.setLong(2, stop.getTimeInMillis());
            pstmt.setInt(3, 0);

            int inserted = pstmt.executeUpdate();

            if (lager.dbLog) {
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

    /**
     * Database agnostic way to manage indexes
     *
     * What is stored in the row is the last ID that was USED already.
     *
     * This is O(1) now, not O(numNodes)
     *
     * @param numNodes number of IDs needed
     * @return next request IDs
     * @throws WorkspaceDatabaseException exc
     */
    synchronized int[] getNextTasktIds(int numNodes)
                            throws WorkspaceDatabaseException {

        PreparedStatement pstmt = null;
        PreparedStatement pstmt2 = null;
        ResultSet rs = null;
        Connection c = null;
        int lastTaskId = -1;
        int newLastTaskId = -1;
        try {
            c = getConnection();

            pstmt = c.prepareStatement(
                     DefaultSchedulerConstants.SQL_SELECT_DEFAULT_SCHED_REQ_ID,
                     ResultSet.TYPE_SCROLL_INSENSITIVE,
                     ResultSet.CONCUR_UPDATABLE);
            rs = pstmt.executeQuery();

            if (!rs.next()) {
                // if there is no row in database, this is first time an ID
                // is needed, insert the value do not update it
                lastTaskId = 0;
                newLastTaskId = numNodes;
                pstmt2 = c.prepareStatement(
                                DefaultSchedulerConstants.
                                        SQL_INSERT_DEFAULT_SCHED_REQ_ID);
                pstmt2.setInt(1, newLastTaskId);
                pstmt2.executeUpdate();
            } else {
                lastTaskId = rs.getInt(1);

                // Get the req Id and increment it
                newLastTaskId = lastTaskId + numNodes;

                pstmt2 = c.prepareStatement(
                                DefaultSchedulerConstants.
                                        SQL_UPDATE_DEFAULT_SCHED_REQ_ID);
                pstmt2.setInt(1, newLastTaskId);
                pstmt2.executeUpdate();
            }
        } catch (SQLException e) {
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
                if (c != null) {
                    returnConnection(c);
                }
            } catch (SQLException e) {
                 logger.error("SQLException in finally cleanup", e);
            }
        }

        if (lastTaskId < 0) {
            throw new WorkspaceDatabaseException("lastTaskId not expected " +
                                                 "to be negative here");
        }
        if (newLastTaskId < 0) {
            throw new WorkspaceDatabaseException("newLastTaskId not expected" +
                                                 " to be negative here");
        }

        if (newLastTaskId - lastTaskId != numNodes) {
            throw new WorkspaceDatabaseException("difference expected to be " +
                                                 "equal to numNodes here");
        }

        final int[] ret = new int[numNodes];
        for (int i = 0; i < numNodes; i++) {
            lastTaskId += 1;
            ret[i] = lastTaskId;
        }
        return ret;
    }

    int[] findWorkspacesToShutdown() throws WorkspaceDatabaseException {

        if (lager.schedLog) {
            logger.trace("findWorkspacesToShutdown()");
        }

        Connection c = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            c = getConnection();
            pstmt = c.prepareStatement(
                            DefaultSchedulerConstants.SQL_SELECT_SHUTDOWN);
            pstmt.setLong(1, Calendar.getInstance().getTimeInMillis());
            rs = pstmt.executeQuery();

            ArrayList results = new ArrayList();
            if (rs == null || !rs.next()) {
                if (lager.schedLog) {
                    logger.trace("no workspaces to shutdown");
                }
                return null;
            } else {
                do {
                    Integer id = new Integer(rs.getInt(1));
                    results.add(id);
                    if (lager.schedLog) {
                        logger.trace("found id: " + id);
                    }
                } while (rs.next());
            }

            // can't use toArray without converting to Integer[]
            int[] ret = new int[results.size()];
            for (int i = 0; i < ret.length; i++) {
                ret[i] = ((Number)results.get(i)).intValue();
            }
            return ret;


        } catch(SQLException e) {
            logger.error("",e);
            return null;
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

    int anyLeft() throws WorkspaceDatabaseException {
        Connection c = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            c = getConnection();
            pstmt = c.prepareStatement(
                            DefaultSchedulerConstants.SQL_SELECT_ANY_LEFT);
            rs = pstmt.executeQuery();

            Integer left = null;

            if (rs != null && rs.next()) {
                left = new Integer(rs.getInt(1));
            }

            if (left != null) {
                return left.intValue();
            } else {
                return 0;
            }

        } catch(SQLException e) {
            logger.error("",e);
            return 0;
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

    void markShutdown(int id) throws WorkspaceDatabaseException {

        Connection c = null;
        PreparedStatement pstmt = null;
        try {
            c = getConnection();
            pstmt = c.prepareStatement(
                            DefaultSchedulerConstants.SQL_UPDATE_SHUTDOWN);
            pstmt.setInt(1, id);
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

    /* Coscheduling related */

    void addNodeRequest(NodeRequest req, String coschedid)
                throws WorkspaceDatabaseException {

        if (lager.traceLog) {
            logger.trace("addNodeRequest(): " + Lager.ensembleid(coschedid));
        }

        String assocString = null;
        final String[] assocs = req.getNeededAssociations();
        if (assocs != null && assocs.length > 0) {
            StringBuffer buf = new StringBuffer(256);
            buf.append(assocs[0]);
            for (int i = 1; i < assocs.length; i++) {
                buf.append(",")
                   .append(assocs[i]);
            }
            assocString = buf.toString();
        }

        // this nastily loops because failure situation to scheduler will only
        // send vmids and scheduler needs to be able to back out ensemble
        // pieces one by one but keep others intact (unless entire ensemble is
        // destroyed).  won't have to loop like this in the future when inner
        // abstractions are themselves unlooped more.

        Connection c = null;
        PreparedStatement pstmt = null;
        try {
            c = getConnection();
            c.setAutoCommit(false);

            for (int i = 0; i < req.getIds().length; i++) {
            
                pstmt = c.prepareStatement(
                            DefaultSchedulerConstants.SQL_INSERT_NODE_REQUEST);

                pstmt.setString(1, coschedid);

                if (req.getGroupid() != null) {
                    pstmt.setString(2, req.getGroupid());
                } else {
                    pstmt.setNull(2, Types.VARCHAR);
                }

                pstmt.setInt(3, req.getIds()[i]);

                pstmt.setInt(4, req.getDuration());

                pstmt.setInt(5, req.getMemory());

                if (assocString != null) {
                    pstmt.setString(6, assocString);
                } else {
                    pstmt.setNull(6, Types.VARCHAR);
                }

                pstmt.executeUpdate();
                pstmt.close();
                pstmt = null;

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
                if (c != null) {
                    c.setAutoCommit(true);
                    returnConnection(c);
                }
            } catch (SQLException sql) {
                logger.error("SQLException in finally cleanup", sql);
            }
        }
    }

    NodeRequest[] getNodeRequests(String coschedid)
            throws WorkspaceDatabaseException {
        
        Connection c = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            c = getConnection();

            pstmt = c.prepareStatement(DefaultSchedulerConstants.
                                                SQL_SELECT_LOAD_NODE_REQUESTS);
            pstmt.setString(1, coschedid);
            rs = pstmt.executeQuery();

            final Map groupMap = new HashMap(16);
            final ArrayList reqs = new ArrayList(64);
            if (rs == null || !rs.next()) {
                logger.warn("no requests with coschedid '" + coschedid + "'?");
                return null;
            } else {

                // already next'd
                do {
                    String groupid = rs.getString(1);
                    int id = rs.getInt(2);
                    int duration = rs.getInt(3);
                    int memory = rs.getInt(4);
                    String assocStr = rs.getString(5);

                    final NodeRequest req;
                    if (groupid != null) {
                        if (groupMap.containsKey(groupid)) {
                            req = (NodeRequest)groupMap.get(groupid);
                            req.addId(id);
                            continue;
                        } else {
                            req = new NodeRequest(memory, duration);
                            req.setGroupid(groupid);
                        }
                    } else {
                        req = new NodeRequest(memory, duration);
                    }

                    req.addId(id);

                    if (assocStr != null) {
                        String[] assocs = assocStr.split(",");
                        req.setNeededAssociations(assocs);
                    }

                    reqs.add(req);

                } while (rs.next());
            }

            if (reqs.isEmpty()) {
                return null;
            }

            return (NodeRequest[]) reqs.toArray(new NodeRequest[reqs.size()]);
            
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

    // transaction that removes node requests and sets a done flag
    void deleteNodeRequestsAndBeDone(String coschedid)
                    throws WorkspaceDatabaseException {

        if (lager.dbLog) {
            logger.trace("deleteNodeRequestsAndBeDone(): " +
                                Lager.ensembleid(coschedid));
        }

        Connection c = null;
        PreparedStatement pstmt = null;
        PreparedStatement pstmt2 = null;
        try {
            c = getConnection();
            c.setAutoCommit(false);
            
            pstmt = c.prepareStatement(
                    DefaultSchedulerConstants.SQL_DELETE_NODE_REQUESTS);
            pstmt.setString(1, coschedid);
            int deleted = pstmt.executeUpdate();

            pstmt2 = c.prepareStatement(
                    DefaultSchedulerConstants.SQL_INSERT_NODE_REQUESTS_SENT);
            pstmt2.setString(1, coschedid);
            pstmt2.executeUpdate();

            c.commit();

            if (lager.dbLog) {
                logger.debug("deleted " + deleted + " rows, " +
                        Lager.ensembleid(coschedid));
            }

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
                if (c != null) {
                    c.setAutoCommit(true);
                    returnConnection(c);
                }
            } catch (SQLException sql) {
                logger.error("SQLException in finally cleanup", sql);
            }
        }
    }

    // deletes just one node request
    void deleteNodeRequest(int vmid)
                    throws WorkspaceDatabaseException {

        if (lager.dbLog) {
            logger.trace("deleteNodeRequest(): " + Lager.id(vmid));
        }

        Connection c = null;
        PreparedStatement pstmt = null;
        try {
            c = getConnection();

            pstmt = c.prepareStatement(
                    DefaultSchedulerConstants.SQL_DELETE_ONE_NODE_REQUEST);
            pstmt.setInt(1, vmid);
            int deleted = pstmt.executeUpdate();

            if (lager.dbLog) {
                logger.debug("deleted " + deleted + " rows, " +
                             Lager.id(vmid));
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

    void coschedDone(String coschedid)
                throws WorkspaceDatabaseException {

        Connection c = null;
        PreparedStatement pstmt = null;
        try {
            c = getConnection();
            pstmt = c.prepareStatement(DefaultSchedulerConstants.
                                                SQL_INSERT_NODE_REQUESTS_SENT);

            pstmt.setString(1, coschedid);
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

    boolean isCoschedDone(String coschedid)
                throws WorkspaceDatabaseException {

        Connection c = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            c = getConnection();
            pstmt = c.prepareStatement(DefaultSchedulerConstants.
                                           SQL_SELECT_CHECK_NODEREQS_SENT);
            pstmt.setString(1, coschedid);
            rs = pstmt.executeQuery();

            int count = -1;

            if (rs != null && rs.next()) {
                count = rs.getInt(1);
            }

            if (count < 0) {
                throw new WorkspaceDatabaseException("no count (?)");
            }

            return count > 0;

        } catch(SQLException e) {
            throw new WorkspaceDatabaseException(e.getMessage(), e);
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

    /* Connection methods */

    /**
     * @return connection
     * @throws WorkspaceDatabaseException exc
     */
    private Connection getConnection() throws WorkspaceDatabaseException {
        try {
            return this.source.getConnection();
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
