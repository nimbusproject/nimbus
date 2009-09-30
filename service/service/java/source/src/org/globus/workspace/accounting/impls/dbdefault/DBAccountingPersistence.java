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

package org.globus.workspace.accounting.impls.dbdefault;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.workspace.Lager;
import org.globus.workspace.WorkspaceException;
import org.globus.workspace.accounting.ElapsedAndReservedMinutes;
import org.globus.workspace.persistence.WorkspaceDatabaseException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * package-private class, all access is from DBAccountingAdapter
 */
class DBAccountingPersistence implements DBAccountingConstants {

    private static final Log logger =
        LogFactory.getLog(DBAccountingPersistence.class.getName());

    private boolean initialized;

    private final DateFormat localFormat = DateFormat.getDateTimeInstance();

    private final DataSource dataSource;

    private final Lager lager;

    DBAccountingPersistence(DataSource dataSourceImpl,
                            Lager lagerImpl) {
        
        if (dataSourceImpl == null) {
            throw new IllegalArgumentException("dataSourceImpl may not be null");
        }
        this.dataSource = dataSourceImpl;

        if (lagerImpl == null) {
            throw new IllegalArgumentException("lagerImpl may not be null");
        }
        this.lager = lagerImpl;
    }

    /* ******** */
    /* DB setup */
    /* ******** */


    public void initialize() throws Exception {
        
        try {
            this.prepareStatements();
        } catch (SQLException sql) {
            throw new Exception("Problem preparing DB statements: ", sql);
        }

        this.initialized = true;
    }

    public boolean isInitialized() {
        return this.initialized;
    }

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

        if (this.lager.accounting) {
            logger.debug("prepareStatements()");
        }

        // String[] ins = INSENSITIVE_PREPARED_STATEMENTS;

        String[] pstmts = PREPARED_STATEMENTS;

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
        }
    }

    public int forceAllInactive() throws WorkspaceDatabaseException {
        
        if (this.lager.accounting) {
            logger.trace("forceAllInactive()");
        }

        int updated = -1;

        Connection c = null;
        PreparedStatement pstmt = null;
        try {
            c = getConnection();
            pstmt = c.prepareStatement(SQL_FORCE_ALL_INACTIVE);
            updated = pstmt.executeUpdate();

            if (this.lager.accounting) {
                logger.trace("updated " + updated + " rows");
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

        return updated;
    }

    /* ****** */
    /* Common */
    /* ****** */

    /**
     * @return Connection conn
     * @throws SQLException problem
     */
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


    /* ****************** */
    /* Add/End Operations */
    /* ****************** */

    public synchronized void add(String uuid,
                                 int id,
                                 String ownerDN,
                                 long minutesRequested,
                                 Calendar creationTime)
            throws WorkspaceDatabaseException {

        if (this.lager.accounting) {
            logger.trace("add(): uuid = '" + uuid + "', id = " + id + ", " +
                    "ownerDN = '" + ownerDN + "', minutesRequest = " +
                    minutesRequested + ", creationTime = " +
                    creationTime.getTimeInMillis());
        }

        Connection c = null;
        PreparedStatement pstmt = null;
        try {
            c = getConnection();
            pstmt = c.prepareStatement(SQL_INSERT_DEPLOYMENT);

            pstmt.setString(1, uuid);
            pstmt.setInt(2, id);
            pstmt.setString(3, ownerDN);
            pstmt.setObject(4, new Long(creationTime.getTimeInMillis()));
            pstmt.setObject(5, new Long(minutesRequested));
            pstmt.setInt(6, 1);
            pstmt.setNull(7, Types.INTEGER);
            
            int inserted = pstmt.executeUpdate();
            if (this.lager.accounting) {
                logger.trace(Lager.id(id) + ": inserted " + inserted + " rows");   
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

        if (this.lager.accounting) {
            logger.trace(Lager.id(id) + ": add() done (uuid: " + uuid + ")");
        }
        
    }

    public synchronized String end(int id,
                                 String ownerDN,
                                 long minutesElapsed)
            throws WorkspaceDatabaseException {

        if (this.lager.accounting) {
            logger.trace("end(): " + Lager.id(id) + ", ownerDN = '" + ownerDN +
                    "', minutesElapsed = " + minutesElapsed);
        }

        Connection c = null;
        PreparedStatement pstmt = null;
        PreparedStatement pstmt2 = null;
        ResultSet rs = null;
        String uuid;
        try {
            c = getConnection();
            
            // begin transaction
            c.setAutoCommit(false);

            // check consistency of destroy request
            // & retrieve necessary items for subsequent sql
            
            String creatorDN;
            Calendar creationTime;
            long t;
            int requestedDuration;

            pstmt = c.prepareStatement(SQL_LOAD_DEPLOYMENT);
            pstmt.setInt(1, id);
            rs = pstmt.executeQuery();

            if (rs == null || !rs.next()) {
                String err = "active deployment with id " + id + " not found";
                logger.error(err);
                throw new WorkspaceDatabaseException(err);
            } else {
                uuid = rs.getString(1);
                creatorDN = rs.getString(2);
                t = rs.getLong(3);
                creationTime = Calendar.getInstance();
                creationTime.setTimeInMillis(t);
                requestedDuration = rs.getInt(4);
            }

            if (this.lager.accounting) {
                logger.trace("end(): found " + Lager.id(id) +
                             ": uuid = " + uuid +
                             ", creation time = " + t +
                             ", requestedDuration = " + requestedDuration +
                             ", creator DN = " + creatorDN);
            }

            rs.close();
            rs = null;

            if (ownerDN.equals(creatorDN)) {
                if (this.lager.accounting) {
                    logger.trace(Lager.id(id) + ": creatorDN in DB matches " +
                            "destroy request");
                }
            } else {
                String err = "active deployment with id " + id + " had " +
                        "non-matching creatorDN.  Expected '" + ownerDN + "'," +
                        " stored was '" + creatorDN + "'";
                logger.error(err);
                throw new WorkspaceDatabaseException(err);
            }

            // log elapsed time

            pstmt2 = c.prepareStatement(SQL_UPDATE_END_DEPLOYMENT);
            pstmt2.setObject(1, new Long(minutesElapsed));
            pstmt2.setString(2, uuid);
            
            int updated = pstmt2.executeUpdate();

            c.commit();

            if (this.lager.accounting) {
                logger.trace(Lager.id(id) + ": updated " + updated + " rows");
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

        return uuid;
    }

    /* **************** */
    /* Query Operations */
    /* **************** */

    public long totalElapsedMinutes(String ownerDN)
            throws WorkspaceException {

        Connection c = null;
        try {
            c = getConnection();
            return totalElapsedMinutesImpl(ownerDN, c);
        } catch(SQLException e) {
            logger.error("",e);
            throw new WorkspaceException(e);
        } finally {
            if (c != null) {
                returnConnection(c);
            }
        }
    }

    public long currentReservedMinutes(String ownerDN)
            throws WorkspaceException {
        
        Connection c = null;
        try {
            c = getConnection();
            return this.currentReservedMinutesImpl(ownerDN, c);
        } catch(SQLException e) {
            logger.error("",e);
            throw new WorkspaceException(e);
        } finally {
            if (c != null) {
                returnConnection(c);
            }
        }
    }

    public ElapsedAndReservedMinutes totalElapsedAndReservedMinutesTuple(
                                                        String ownerDN)
            throws WorkspaceException {

        Connection c = null;
        try {
            c = getConnection();
            long reserved = this.currentReservedMinutesImpl(ownerDN, c);
            long elapsed = totalElapsedMinutesImpl(ownerDN, c);
            return new ElapsedAndReservedMinutes(elapsed, reserved);
        } catch(SQLException e) {
            logger.error("",e);
            throw new WorkspaceException(e);
        } finally {
            if (c != null) {
                returnConnection(c);
            }
        }
    }

    /**
     * Returns a list of lines to log, one for each current reservation.
     * Embedding line formatting in this method as a shortcut even though
     * formatting is more appropriately the caller's concern.
     *  
     * @return list of strings to log
     * @throws WorkspaceException problem
     */
    public ArrayList allActiveReservations() throws WorkspaceException {

        Connection c = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            c = getConnection();
            pstmt = c.prepareStatement(SQL_ALL_CURRENT_RESERVATIONS);
            rs = pstmt.executeQuery();

            ArrayList reservations = new ArrayList();

            if (rs == null) {
                if (this.lager.accounting) {
                    logger.trace("null result for current reservations query");
                }
                return reservations;
            }

            while (rs.next()) {
                String uuid = rs.getString(1);
                int id = rs.getInt(2);
                String dn = rs.getString(3);
                long t = rs.getLong(4);
                Calendar creationTime = Calendar.getInstance();
                creationTime.setTimeInMillis(t);
                int duration = rs.getInt(5);

                StringBuffer buf = new StringBuffer();
                buf.append("dn=\"")
                   .append(dn)
                   .append("\", minutes=")
                   .append(duration)
                   .append(", uuid=\"")
                   .append(uuid)
                   .append("\", eprkey=")
                   .append(id)
                   .append(", creation=\"")
                   .append(this.localFormat.format(creationTime.getTime()))
                   .append('"');

                reservations.add(buf.toString());
            }

            if (this.lager.accounting) {
                logger.trace("sizeof(reservations) = " + reservations.size());
            }

            return reservations;

        } catch(SQLException e) {
            logger.error("",e);
            throw new WorkspaceException(e);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    logger.error(e.getMessage(), e);
                }
            }
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException e) {
                    logger.error(e.getMessage(), e);
                }
            }
            if (c != null) {
                returnConnection(c);
            }
        }
    }

    private static long totalElapsedMinutesImpl(String ownerDN, Connection c)
            throws WorkspaceException {

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        long time = 0;
        
        try {

            pstmt = c.prepareStatement(SQL_SUM_ELAPSED);
            pstmt.setString(1, ownerDN);
            rs = pstmt.executeQuery();

            if (rs == null || !rs.next()) {
                logger.debug("no result from sum (DN not seen before)");
                return time;
            } else {
                time = rs.getLong(1);
            }

        } catch(SQLException e) {
            logger.error("",e);
            throw new WorkspaceException(e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (pstmt != null) {
                    pstmt.close();
                }
            } catch (SQLException sql) {
                logger.error("SQLException in finally cleanup", sql);
            }
        }

        return time;
    }

    private long currentReservedMinutesImpl(String ownerDN, Connection c)
            throws WorkspaceException {

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        long time = 0;

        try {

            pstmt = c.prepareStatement(SQL_SUM_RESERVED);
            pstmt.setString(1, ownerDN);
            rs = pstmt.executeQuery();

            if (rs == null || !rs.next()) {
                logger.debug("no result from sum (DN not seen before)");
                return time;
            } else {
                time = rs.getLong(1);
            }
            
        } catch(SQLException e) {
            logger.error("",e);
            throw new WorkspaceException(e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (pstmt != null) {
                    pstmt.close();
                }
            } catch (SQLException sql) {
                logger.error("SQLException in finally cleanup", sql);
            }
        }

        return time;
    }
}
