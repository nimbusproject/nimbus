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

import commonj.timers.Timer;
import commonj.timers.TimerListener;
import commonj.timers.TimerManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.workspace.Lager;
import org.globus.workspace.WorkspaceException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * Pretty prints DBAccountingAdapter's information to files.
 * Writes are queued and only written out occasionally.
 */
public class DelayedAccountingFileLogger implements TimerListener {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------
    
    private static final Log logger =
        LogFactory.getLog(DelayedAccountingFileLogger.class.getName());

    public static final int MIN_WRITE_DELAY_MILLISECONDS = 500;

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private final DBAccountingPersistence db;
    private final Lager lager;
    private final TimerManager timerManager;
    private final long delay;

    private final DateFormat localFormat = DateFormat.getDateTimeInstance();
    
    private String currentReservationsPath;
    private File curresFile;
    private boolean resEnabled;
    
    private String eventLogPath;
    private File eventLogFile;
    private boolean evEnabled;

    private Timer timer;

    // Stores Strings, each a line to be appended to file.  One \n will be
    // added to each for you when being written to file.
    private final ArrayList unwrittenEvents = new ArrayList(256);
    

    // -------------------------------------------------------------------------
    // SETUP
    // -------------------------------------------------------------------------

    private DelayedAccountingFileLogger(String currentReservationsPath,
                                        String eventLogPath,
                                        long writeDelayMilliseconds,
                                        DBAccountingPersistence db,
                                        Lager lager,
                                        TimerManager timerManager) {
        
        this.currentReservationsPath = currentReservationsPath;
        this.eventLogPath = eventLogPath;
        this.delay = writeDelayMilliseconds;
        this.db = db;
        this.lager = lager;
        this.timerManager = timerManager;
    }
    
    /**
     * If one path is null, that functionality is disabled.  
     * If both paths are null, no instance is created (exception).
     * 
     * @param currentReservationsPath path to file for printing current reservation view
     * @param eventLogPath path to file for printing events
     * @param writeDelayMilliseconds how long to wait until queued writes are sent to filesystem
     * @param db for database access
     * @param lager lager
     * @param timerManager delay mechanism
     * @throws Exception problem setting up
     * @return created and initialized instance of DelayedAccountingFileLogger
     */
    public static DelayedAccountingFileLogger create(
                                String currentReservationsPath,
                                String eventLogPath,
                                long writeDelayMilliseconds,
                                DBAccountingPersistence db,
                                Lager lager,
                                TimerManager timerManager)
            throws Exception {

        if (db == null) {
            throw new WorkspaceException("db can not be null");
        } else if (!db.isInitialized()) {
            throw new WorkspaceException("DBAccountingPersistence not " +
                    "initialized");
        }

        if (lager == null) {
            throw new IllegalArgumentException("lager may not be null");
        }

        if (timerManager == null) {
            throw new IllegalArgumentException("timerManager may not be null");
        }

        if (writeDelayMilliseconds < MIN_WRITE_DELAY_MILLISECONDS) {
            throw new WorkspaceException("write delay can not be less " +
                    "than " + MIN_WRITE_DELAY_MILLISECONDS + " ms");
        }

        final DelayedAccountingFileLogger fileLog =
                   new DelayedAccountingFileLogger(currentReservationsPath,
                                                   eventLogPath,
                                                   writeDelayMilliseconds,
                                                   db,
                                                   lager,
                                                   timerManager);

        fileLog.initialize();

        return fileLog;
    }

    private void initialize() throws Exception {

        if (this.eventLogPath == null
                && this.currentReservationsPath == null) {
            throw new WorkspaceException("no paths configured");
        }

        if (this.eventLogPath != null) {

            this.eventLogFile = new File(this.eventLogPath);

            initFile(this.eventLogFile, "accounting event log file");

            this.evEnabled = true;
        }

        if (this.currentReservationsPath != null) {
            
            this.curresFile = new File(this.currentReservationsPath);
            this.initFile(this.curresFile, "accounting current-reservations file");
            this.resEnabled = true;
        }
    }

    private void initFile(File file, String name)
            throws WorkspaceException {

        try {
            if (file.createNewFile()) {
                if (this.lager.eventLog) {
                    logger.info(name + " created: '" +
                            file.getAbsolutePath() + "'");
                }
            } else {
                if (this.lager.eventLog) {
                    logger.info("Using pre-existing " + name + ": '" +
                            file.getAbsolutePath() + "'");
                }
            }

            // test the one we just created as well in case there was
            // some odd umask setting etc.

            if (file.canWrite()) {
                if (this.lager.accounting) {
                    logger.debug("can write to " + file.getAbsolutePath());
                }
            } else {
                throw new WorkspaceException("can not write to " +
                        name + ": '" + file.getAbsolutePath() + "'");
            }

            if (setFilePermissions(file.getAbsolutePath(), 600)) {
                if (this.lager.accounting) {
                    logger.debug("able to set owner only: " +
                            file.getAbsolutePath());
                }
            } else {
                throw new WorkspaceException("can not set " +
                    name + " permissions to owner-only: '" +
                        file.getAbsolutePath() + "'");
            }

        } catch (IOException e) {
            throw new WorkspaceException(name + " ('" +
                    file.getAbsolutePath() + "') is not set up properly:", e);
        }
    }

    // nasty routine, lifted from jglobus
    private static boolean setFilePermissions(String file, int mode) {

        final Runtime runtime = Runtime.getRuntime();
        final String [] cmd = new String[] { "chmod",
                                             String.valueOf(mode),
                                             file };
        Process process = null;
        try {
            process = runtime.exec(cmd, null);
            return process.waitFor() == 0 ? true : false;
        } catch(Exception e) {
            if (logger.isDebugEnabled()) {
                logger.error(e.getMessage(), e);
            } else {
                logger.error(e.getMessage());
            }
            return false;
        } finally {
            if (process != null) {
                try {
                    process.getErrorStream().close();
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
                try {
                    process.getInputStream().close();
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
                try {
                    process.getOutputStream().close();
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // WORK METHODS
    // -------------------------------------------------------------------------

    public synchronized void logCreate(String uuid,
                                       int id,
                                       String ownerDN,
                                       long minutesRequested,
                                       long charge,
                                       Calendar now,
                                       int CPUCount,
                                       int memory,
                                       String moreToLog) throws WorkspaceException {

        try {
            this.create(uuid, id, ownerDN, minutesRequested, charge,
                        now, CPUCount, memory, moreToLog);
            this.schedule();
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
            throw new WorkspaceException(t.getMessage());
        }
    }

    private void create(String uuid,
                        int id,
                        String ownerDN,
                        long minutesRequested,
                        long charge,
                        Calendar now,
                        int CPUCount,
                        int memory,
                        String moreToLog) {

        final StringBuffer buf = new StringBuffer(128);
        buf.append("CREATED: time=\"")
           .append(this.localFormat.format(now.getTime()))
           .append("\", uuid=\"")
           .append(uuid)
           .append("\", eprkey=")
           .append(id)
           .append(", dn=\"")
           .append(ownerDN)
           .append("\", requestMinutes=")
           .append(minutesRequested)
           .append(", charge=")
           .append(charge)
           .append(", CPUCount=")
           .append(CPUCount)
           .append(", memory=")
           .append(memory);

        if (moreToLog != null) {
            buf.append(moreToLog);
        }
        

        this.unwrittenEvents.add(buf.toString());
    }

    public synchronized void logRemove(String uuid,
                                       int id,
                                       String ownerDN,
                                       long charge,
                                       Calendar now) throws WorkspaceException {
        
        try {
            this.remove(uuid, id, ownerDN, charge, now);
            this.schedule();
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
            throw new WorkspaceException(t.getMessage());
        }
    }

    private void remove(String uuid,
                        int id,
                        String ownerDN,
                        long charge,
                        Calendar now) {
        

        final StringBuffer buf = new StringBuffer(128);
        buf.append("REMOVED: time=\"")
           .append(this.localFormat.format(now.getTime()))
           .append("\", uuid=\"")
           .append(uuid)
           .append("\", eprkey=")
           .append(id)
           .append(", dn=\"")
           .append(ownerDN)
           .append("\", charge=")
           .append(charge);

        this.unwrittenEvents.add(buf.toString());
    }

    private void write() throws WorkspaceException {

        if (this.evEnabled) {

            if (lager.accounting) {
                logger.trace("evEnabled: write");
            }

            if (this.eventLogFile == null) {
                throw new WorkspaceException("eventLogFile null but" +
                        " evEnabled is true");
            }

            if (this.unwrittenEvents.isEmpty()) {
                if (lager.accounting) {
                    logger.trace("unwrittenEvents is empty");
                }
            } else {
                append(this.eventLogFile, this.unwrittenEvents);
                this.unwrittenEvents.clear();
                logger.debug("wrote to " + this.eventLogPath);
            }
        }

        if (this.resEnabled) {

            if (lager.accounting) {
                logger.trace("resEnabled: write");
            }

            if (this.curresFile == null) {
                throw new WorkspaceException("curresFile null but" +
                        " resEnabled is true");
            }

            final ArrayList reservations = this.db.allActiveReservations();
            replace(this.curresFile, reservations);
            logger.debug("wrote to " + this.currentReservationsPath);
        }
        
    }

    private static boolean replace(File file, ArrayList list) {
        return write(file, list, false);
    }    

    private static boolean append(File file, ArrayList list) {
        return write(file, list, true);
    }
    
    private static boolean write(File file, ArrayList list, boolean append) {

        boolean done = false;

        PrintWriter out = null;
        try {

            out = new PrintWriter(
                    new BufferedWriter(
                            new FileWriter(file, append)));

            for(int i = 0; i < list.size(); i++) {
                out.println(list.get(i));
            }

            done = true;

        } catch (IOException e) {
            if (logger.isDebugEnabled()) {
                logger.error(e.getMessage(), e);
            } else {
                logger.error(e.getMessage());
            }
        } finally {
            if (out != null) {
                out.close();
            }
        }

        return done;
    }
    
    /* *************************** */
    /* DELAY AND LOCKING MECHANICS */
    /* *************************** */

    private void schedule() {
        if (this.timer == null) {
            this.timer = this.timerManager.schedule(this,
                                                    this.delay);
        }
    }

    private void reset() {
        this.timer = null;
    }

    public synchronized void timerExpired(Timer aTimer) {
        try {
            this.write();
            // Because there is a lock around any incoming events when this
            // method is fired, no need to conditionally reset the timer,
            // just un-schedule
            this.reset();
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
        }
    }
}
