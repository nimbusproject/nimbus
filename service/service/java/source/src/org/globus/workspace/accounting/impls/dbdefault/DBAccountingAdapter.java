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
import org.globus.workspace.accounting.AccountingEventAdapter;
import org.globus.workspace.accounting.AccountingReaderAdapter;
import org.globus.workspace.accounting.ElapsedAndReservedMinutes;
import org.globus.workspace.accounting.impls.Util;
import org.globus.workspace.persistence.WorkspaceDatabaseException;
import org.globus.workspace.persistence.DBLoader;
import org.safehaus.uuid.UUIDGenerator;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.Calendar;

import commonj.timers.TimerManager;
import org.springframework.core.io.Resource;

public class DBAccountingAdapter implements AccountingEventAdapter,
                                            AccountingReaderAdapter {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
        LogFactory.getLog(DBAccountingAdapter.class.getName());

    public static final int DEFAULT_WRITE_DELAY_MILLISECONDS = 10000;
    

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private final DataSource dataSource;
    private final Lager lager;
    private final TimerManager timerManager;

    private DBAccountingPersistence db;
    private final UUIDGenerator uuidGen = UUIDGenerator.getInstance();

    private String currentReservationsPath;
    private String eventsPath;
    private long writeDelayMilliseconds = -1;
    private int chargeGranularity;

    // see comment in initialize()
    private boolean initialized;

    private DelayedAccountingFileLogger fileLog;


    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------
    
    public DBAccountingAdapter(DataSource dataSource,
                               Lager lager,
                               TimerManager timerManager,
                               DBLoader loader) throws Exception {
        
        if (dataSource == null) {
            throw new IllegalArgumentException("dataSource may not be null");
        }
        this.dataSource = dataSource;

        if (lager == null) {
            throw new IllegalArgumentException("lager may not be null");
        }
        this.lager = lager;

        if (timerManager == null) {
            throw new IllegalArgumentException("timerManager may not be null");
        }
        this.timerManager = timerManager;

        if (loader == null) {
            throw new IllegalArgumentException("loader may not be null");
        }
        if (!loader.isLoaded()) {
            throw new Exception("DBLoader reporting not loaded (?)");
        }
    }


    // -------------------------------------------------------------------------
    // INITIALIZATION
    // -------------------------------------------------------------------------

    public void setCurrentReservationsResource(Resource currentReservationsResource)
            throws IOException {
        this.currentReservationsPath = currentReservationsResource.getFile().getAbsolutePath();
    }

    public void setEventsResource(Resource eventsResource) throws IOException {
        this.eventsPath = eventsResource.getFile().getAbsolutePath();
    }

    public void setWriteDelayMilliseconds(int writeDelayMilliseconds) {
        final int min =
                DelayedAccountingFileLogger.MIN_WRITE_DELAY_MILLISECONDS;
        if (writeDelayMilliseconds < min) {
            throw new RuntimeException("write delay can not be less " +
                    "than " + min + " ms");
        }
        this.writeDelayMilliseconds = writeDelayMilliseconds;
    }

    public void setChargeGranularity(int chargeGranularity) {
        if (chargeGranularity < 1) {
            throw new RuntimeException("chargeGranularity must" +
                    " be greater than zero");
        }
        this.chargeGranularity = chargeGranularity;
    }

    
    // -------------------------------------------------------------------------
    // IoC INIT METHOD
    // -------------------------------------------------------------------------

    public synchronized void initialize() throws Exception {

        logger.debug("validating/initializing");

        if (this.initialized) {
            throw new Exception("already initialized, illegal to initialize " +
                    "more than once");
        }

        if (this.chargeGranularity == 0) {
            throw new Exception("chargeGranularity was not set");
        }

        if (this.db == null) {
            this.db = new DBAccountingPersistence(this.dataSource, this.lager);
            this.db.initialize();
        }

        boolean res = false;
        boolean ev = false;
        if (this.currentReservationsPath != null &&
                this.currentReservationsPath.length() != 0) {
            logger.debug("Configuration found for accounting reservation " +
                    "file-logging");
            res = true;
        } else {
            logger.debug("No configuration found for accounting reservation " +
                    "file-logging");
            this.currentReservationsPath = null;
        }

        if (this.eventsPath != null && this.eventsPath.length() != 0) {
            logger.debug("Configuration found for accounting event " +
                    "file-logging");
            ev = true;
        } else {
            logger.debug("No configuration found for accounting event " +
                    "file-logging");
            this.eventsPath = null;
        }

        if (res || ev) {
            if (this.writeDelayMilliseconds <
                    DelayedAccountingFileLogger.MIN_WRITE_DELAY_MILLISECONDS) {
                
                // values via jndi below min are rejected: config was left out
                logger.debug("Delay for reservation and event file-logging " +
                        "not set, defaulting to " +
                        DEFAULT_WRITE_DELAY_MILLISECONDS + " ms");
                this.writeDelayMilliseconds = DEFAULT_WRITE_DELAY_MILLISECONDS;
            }


            // TODO: DelayedAccountingFileLogger should be via IoC too

            // exception flies, client did not disable everything and there
            // was a problem initializing
            this.fileLog = DelayedAccountingFileLogger.create(
                                            this.currentReservationsPath,
                                            this.eventsPath,
                                            this.writeDelayMilliseconds,
                                            this.db,
                                            this.lager,
                                            this.timerManager);
        }

        this.initialized = true;
        logger.debug("validated/initialized");
    }


    // -------------------------------------------------------------------------
    // implements AccountingEventAdapter
    // -------------------------------------------------------------------------

    public void create(int id, String ownerDN, long minutesRequested,
                       String network, String resource, String clientLaunchName,
                       int CPUCount, int memory) {

        String moreToLog = "";

        if (resource != null) {
            moreToLog += ", vmm='" + resource.trim() + '\'';
        }

        if (clientLaunchName != null) {
            moreToLog += ", clientLaunchName='" + clientLaunchName.trim() + '\'';
        }


        if (network != null) {
            moreToLog += ", network='" + network.trim() + '\'';
        }
        
        if (this.lager.accounting) {
            logger.trace("create(): id = " + id + ", ownerDN = '" +
                    ownerDN + "', minutesRequested = " + minutesRequested +
                         ", CPUCount = " + CPUCount + ", memory = " + memory + moreToLog);
        }

        if (!this.initialized) {
            logger.error("never initialized, can't do anything");
            return;
        }

        if (ownerDN == null) {
            logger.error("ownerDN is null, can't do anything");
            return;
        }

        // account for container recovery after a service state reset
        if (id == 1) {
            try {
                final int updated = this.db.forceAllInactive();

                if (updated < 0) {

                    logger.error("compound, unresolvable DB problem");
                    return;

                } else if (updated > 0) {

                    logger.fatal("Fresh service install or service state " +
                            "was wiped: ALL previous deployments tracked " +
                            "by accounting module were moved to inactive.  " +
                            "Destruction time in this case is undefined and " +
                            "not set!  Set " + updated + " deployments to " +
                            "inactive.");

                } else {
                    
                    logger.debug("fresh service install or service state " +
                            "was wiped, but all previous deployments were " +
                            "already inactive: all is well");
                }
                
            } catch (WorkspaceDatabaseException e) {
                logger.error(e.getMessage());
                return;
            }
        }

        try {

            final long charge = Util.positiveCeiling(minutesRequested,
                                                     this.chargeGranularity);

            final String uuid = uuidGen.generateRandomBasedUUID().toString();

            final Calendar now = Calendar.getInstance();

            this.db.add(uuid, id, ownerDN, charge, now, CPUCount, memory);
            
            if (this.lager.eventLog) {
                logger.info(Lager.ev(id) + "accounting: ownerDN = '" +
                    ownerDN + "', minutesRequested = " + minutesRequested +
                    ", minutes reserved = " + charge +
                    ", CPUCount = " + CPUCount + ", memory = " + memory +
                    ", uuid = '" + uuid + "'" + moreToLog);
            }

            if (this.fileLog != null) {
                try {
                    this.fileLog.logCreate(uuid, id, ownerDN,
                                           minutesRequested, charge, now,
                                           CPUCount, memory, moreToLog);
                } catch (WorkspaceException e) {
                    if (logger.isDebugEnabled()) {
                        logger.error(e.getMessage(), e);
                    } else {
                        logger.error(e.getMessage());
                    }
                }
            }


        } catch (WorkspaceDatabaseException e) {
            logger.error(e.getMessage());
        }

    }

    public void destroy(int id, String ownerDN, long minutesElapsed) {

        if (this.lager.accounting) {
            logger.trace("destroy(): id = " + id + ", ownerDN = '" +
                    ownerDN + "', minutesElapsed = " + minutesElapsed);
        }

        if (!this.initialized) {
            logger.error("never initialized, can't do anything");
            return;
        }

        final long charge = Util.positiveCeiling(minutesElapsed,
                                           this.chargeGranularity);

        try {
            
            final String uuid = this.db.end(id, ownerDN, charge);
            
            if (this.lager.eventLog) {
                logger.info(Lager.ev(id) + "accounting: ownerDN = '" +
                    ownerDN + "', minutesElapsed = " + charge +
                    ", real usage = " + minutesElapsed +
                    ", uuid = '" + uuid + '\'');
            }

            if (this.fileLog != null) {
                final Calendar now = Calendar.getInstance();
                try {
                    this.fileLog.logRemove(uuid, id, ownerDN, charge, now);
                } catch (WorkspaceException e) {
                    if (logger.isDebugEnabled()) {
                        logger.error(e.getMessage(), e);
                    } else {
                        logger.error(e.getMessage());
                    }
                }
            }

        } catch (WorkspaceDatabaseException e) {
            logger.error(e.getMessage());
        }
    }

    // getChargeGranularity below is also from AccountingEventAdapter

    
    // -------------------------------------------------------------------------
    // implements AccountingReaderAdapter
    // -------------------------------------------------------------------------

    public long totalElapsedMinutes(String ownerDN) throws WorkspaceException {
        
        if (this.lager.accounting) {
            logger.trace("totalElapsedMinutes(): ownerDN = '" + ownerDN + "'");
        }

        if (!this.initialized) {
            throw new WorkspaceException("never initialized, " +
                                         "can't do anything");
        }
        
        return this.db.totalElapsedMinutes(ownerDN);
    }

    public long currentReservedMinutes(String ownerDN)
            throws WorkspaceException {
        
        if (lager.accounting) {
            logger.trace("currentReservedMinutes(): ownerDN = '" +
                                                    ownerDN + "'");
        }

        if (!this.initialized) {
            throw new WorkspaceException("never initialized, " +
                                         "can't do anything");
        }

        return this.db.currentReservedMinutes(ownerDN);
    }

    public long totalElapsedAndReservedMinutes(String ownerDN)
            throws WorkspaceException {

        if (this.lager.accounting) {
            logger.trace("totalElapsedAndReservedMinutes(): ownerDN = '" +
                                                    ownerDN + "'");
        }

        if (!this.initialized) {
            throw new WorkspaceException("never initialized, " +
                                         "can't do anything");
        }

        final ElapsedAndReservedMinutes elapRes =
                this.db.totalElapsedAndReservedMinutesTuple(ownerDN);

        return elapRes.getElapsed() + elapRes.getReserved();
    }

    public ElapsedAndReservedMinutes totalElapsedAndReservedMinutesTuple(
            String ownerDN) throws WorkspaceException {

        if (this.lager.accounting) {
            logger.trace("totalElapsedAndReservedMinutesTuple(): ownerDN = '" +
                                                    ownerDN + "'");
        }

        if (!this.initialized) {
            throw new WorkspaceException("never initialized, " +
                                         "can't do anything");
        }

        return this.db.totalElapsedAndReservedMinutesTuple(ownerDN);
    }

    public int getChargeGranularity() {
        return this.chargeGranularity;
    }
}
