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

import commonj.timers.TimerManager;
import edu.emory.mathcs.backport.java.util.concurrent.ExecutorService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nimbustools.api.services.rm.DoesNotExistException;
import org.nimbustools.api.services.rm.ResourceRequestDeniedException;
import org.nimbustools.api.services.rm.ManageException;
import org.globus.workspace.Lager;
import org.globus.workspace.ReturnException;
import org.globus.workspace.WorkspaceConstants;
import org.globus.workspace.WorkspaceUtil;
import org.globus.workspace.WorkspaceException;
import org.globus.workspace.cmdutils.TorqueUtil;
import org.globus.workspace.persistence.WorkspaceDatabaseException;
import org.globus.workspace.scheduler.Reservation;
import org.globus.workspace.scheduler.Scheduler;
import org.globus.workspace.scheduler.defaults.NodeRequest;
import org.globus.workspace.scheduler.defaults.SlotManagement;
import org.globus.workspace.service.WorkspaceHome;
import org.globus.workspace.service.InstanceResource;
import org.globus.workspace.service.impls.site.HTTPListener;
import org.globus.workspace.service.impls.site.PilotPoll;
import org.globus.workspace.service.impls.site.SlotPollCallback;
import org.globus.workspace.xen.XenUtil;
import org.safehaus.uuid.UUIDGenerator;
import org.springframework.core.io.Resource;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;

public class PilotSlotManagement implements SlotManagement,
                                            SlotPollCallback {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
        LogFactory.getLog(PilotSlotManagement.class.getName());

    private static final String SEVERE_PILOT_ISSUE = "There was a severe " +
            "issue with the workspace site scheduler interaction (worksapce " +
            "pilot).  Please contact your administrator with the time of " +
            "this problem and any relevant information";

    private static final Exception SEVERE_PILOT_FAULT =
            new Exception(SEVERE_PILOT_ISSUE);

    private static final UUIDGenerator uuidGen = UUIDGenerator.getInstance();

    private static final DateFormat localFormat = DateFormat.getDateTimeInstance();


    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private final Lager lager;
    private final TimerManager timerManager;
    private final DataSource dataSource;
    private final ExecutorService sharedExecutor;
    private final Object groupLock = new Object();

    private WorkspaceHome instHome;
    private Scheduler schedulerAdapter;

    private PilotSlotManagementDB db;
    
    private CursorPersistence cp = null;

    private String sshNotifyString;

    // keep reference for clean shutdown
    private HTTPListener httpListener = null;

    private String httpNotifyString;
    
    private PilotPoll watcher = null;

    private Pilot pilot = null;

    private String logdirPath = null;

    private TorqueUtil torque;


    // set from config
    private String contactPort;
    private String accountsPath;
    private String sshNotificationInfo;
    private String pilotPath;
    private String pollScript;
    private int grace = -1; // seconds
    private int padding = -1; // seconds
    private long watcherDelay = 200; // ms
    private String LRM;
    private String submitPath;
    private String deletePath;
    private String pilotVersion;
    private int maxMB; // assumes homogenous nodes for now
    private int ppn = -1; // assumes homogenous nodes for now

    private String destination = null; // only one for now
    private String extraProperties = null;
    private String multiJobPrefix = null;

    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    public PilotSlotManagement(WorkspaceHome home,
                               Lager lager,
                               DataSource dataSource,
                               TimerManager timerManager) {

        if (home == null) {
            throw new IllegalArgumentException("home may not be null");
        }
        this.instHome = home;
        this.sharedExecutor = home.getSharedExecutor();

        if (dataSource == null) {
            throw new IllegalArgumentException("dataSource may not be null");
        }
        this.dataSource = dataSource;
        
        if (timerManager == null) {
            throw new IllegalArgumentException("timerManager may not be null");
        }
        this.timerManager = timerManager;

        if (lager == null) {
            throw new IllegalArgumentException("lager may not be null");
        }
        this.lager = lager;
    }


    // -------------------------------------------------------------------------
    // MODULE SET (avoids circular dependency problem)
    // -------------------------------------------------------------------------

    public void setInstHome(WorkspaceHome homeImpl) {
        if (homeImpl == null) {
            throw new IllegalArgumentException("homeImpl may not be null");
        }
        this.instHome = homeImpl;
    }
    
    
    // -------------------------------------------------------------------------
    // SET FROM CONFIG
    // -------------------------------------------------------------------------

    public void setContactPort(String contactPort) {
        this.contactPort = contactPort;
    }

    public void setAccountsResource(Resource accountsResource) throws IOException {
        this.accountsPath = accountsResource.getFile().getAbsolutePath();
    }

    public void setSshNotificationInfo(String info) {
        if (info != null && info.trim().length() != 0) {
            this.sshNotificationInfo = info;
        }
    }

    public void setPollScriptResource(Resource pollScriptResource) throws IOException {
        this.pollScript = pollScriptResource.getFile().getAbsolutePath();
    }

    // default is 200ms
    public void setWatcherDelay(long delay) {
        this.watcherDelay = delay;
    }

    public void setPilotPath(String pilotPath) {
        this.pilotPath = pilotPath;
    }

    public void setGrace(int grace) {
        this.grace = grace;
    }

    public void setPadding(int padding) {
        this.padding = padding;
    }

    public void setLRM(String LRM) {
        this.LRM = LRM;
    }

    public void setPpn(int ppn) {
        this.ppn = ppn;
    }

    public void setSubmitPath(String submitPath) {
        this.submitPath = submitPath;
    }

    public void setDeletePath(String deletePath) {
        this.deletePath = deletePath;
    }

    public void setPilotVersion(String pilotVersion) {
        this.pilotVersion = pilotVersion;
    }

    public void setMaxMB(int maxMB) {
        this.maxMB = maxMB;
    }

    public void setDestination(String destination) {
        if (destination != null && destination.trim().length() != 0) {
            this.destination = destination;
        }
    }

    public void setExtraProperties(String extraProperties) {
        if (extraProperties != null && extraProperties.trim().length() != 0) {
            this.extraProperties = extraProperties;
        }
    }

    public void setMultiJobPrefix(String multiJobPrefix) {
        if (multiJobPrefix != null && multiJobPrefix.trim().length() != 0) {
            this.multiJobPrefix = multiJobPrefix;
        }
    }

    public void setLogdirResource(Resource logdirResource) throws IOException {
        this.logdirPath = logdirResource.getFile().getAbsolutePath();
    }

    // -------------------------------------------------------------------------
    // IoC INIT METHOD
    // -------------------------------------------------------------------------

    public synchronized void validate() throws Exception {

        boolean httpNotificationEnabled = true;
        if (this.contactPort == null) {
            httpNotificationEnabled = false;
            logger.info("pilot http-based notification information " +
                        "is not set therefore it is disabled");
        }

        boolean sshNotificationEnabled = true;
        
        if (this.sshNotificationInfo == null) {
            sshNotificationEnabled = false;
            logger.info("pilot ssh-based (backup) notification information " +
                        "is not set therefore it is disabled");
        }

        if (sshNotificationEnabled) {
            if (this.pollScript != null) {

                final File pollScriptFile = new File(this.pollScript);
                if (!pollScriptFile.exists() || !pollScriptFile.isFile()) {
                    throw new FileNotFoundException(
                            "Not found or not a file: '" + this.pollScript);
                }
            } else {
                throw new Exception("pollScript setting is missing from" +
                        " pilot slot management configuration");
            }

            if (this.watcherDelay < 1) {
                throw new Exception("pilot sweeper delay is less than 1, " +
                                    "invalid");
            }

            if (this.watcherDelay < 50) {
                logger.warn("you should probably not set sweeper delay to " +
                            "less than 50ms");
            }
        }

        if (!httpNotificationEnabled && !sshNotificationEnabled) {
            throw new Exception("no pilot-->container notification " +
                                "mechanism is set");
        }

        if (this.submitPath == null) {
            throw new Exception("pilot LRM submit path is not set");
        }

        if (this.deletePath == null) {
            throw new Exception("pilot LRM delete path is not set");
        }

        if (this.LRM == null) {
            throw new Exception("pilot LRM is not set");
        } else if (!this.LRM.equalsIgnoreCase("torque")) {
            String x = "pilot LRM is not set to torque, the only current impl";
            throw new Exception(x);
        }

        if (this.LRM.equalsIgnoreCase("torque")) {
            this.torque = new TorqueUtil(this.submitPath,
                                         this.deletePath);
        }

        if (this.maxMB <= 0) {
            throw new Exception("Max guest memory is <= 0 MB.  Is the " +
                    "configuration present (maxMB)?");
        }

        if (this.pilotPath == null) {
            throw new Exception("path to pilot on remote nodes is not set");
        } else {
            if (!new File(this.pilotPath).isAbsolute()) {
                throw new Exception("currently expecting path to pilot on " +
                        "remote nodes (pilotPath) to be an absolute path");
            }
        }

        if (this.destination != null) {
            logger.debug("Found destination: " + this.destination);
        } else {
            logger.debug("No destination configured.");
        }

        if (this.extraProperties != null) {
            logger.debug("Found extra properties: " + this.extraProperties);
        } else {
            logger.debug("No extra properties configured.");
        }

        if (this.grace < 0) {
            throw new Exception("grace period is less than zero, invalid. " +
                    "Is the configuration present?");
        }

        if (this.ppn < 1) {
            throw new Exception("processors per node (ppn) is less than one, " +
                    "invalid.  Is the configuration present?");
        }

        if (this.padding < 0) {
            throw new Exception("padding is less than zero, invalid. " +
                    "Is the configuration present?");
        }

        if (this.pilotVersion == null) {
            throw new Exception("pilot version not set, there is no default");
        }

        // Only 0.2 is supported right now and it is never cased for
        // anywhere else in this class -- will add casing in places it is
        // necessary as the implementations out there diverge.  At some point
        // we may even remove support for old pilot versions if they cause too
        // much casing to happen here or do not enable enough features such
        // that too much casing or lack of important service features would
        // have to happen outside this class (especially if remote client
        // semantics would not be the same for all supported versions of the
        // pilot).
        if (this.pilotVersion.equals("0.2")) {
            this.pilot = new Pilot_0_2();
        } else {
            throw new Exception("pilot version '" + this.pilotVersion +
                                "' is not supported");
        }

        if (sshNotificationEnabled) {
            
            final String[] cmd = {this.pollScript};
            try {
                WorkspaceUtil.runCommand(cmd,
                                         this.lager.eventLog,
                                         this.lager.traceLog);
            } catch (Exception e) {
                final String err = "error testing pilot notification script: ";
                // passing e to error gives very long stacktrace to user
                // logger.error(err, e);
                throw new Exception(err + e.getMessage());
            }

            this.sshNotifyString = this.sshNotificationInfo + this.pollScript;

            logger.debug("tests run of pilot notification script '" +
                                            this.pollScript + "' succeeded");

        }

        if (this.logdirPath != null) {

            final File logdirFile = new File(this.logdirPath);
            if (!logdirFile.exists()) {
                throw new Exception("configured pilot log directory " +
                        "does not exist: " + this.logdirPath);
            }

            if (!logdirFile.isDirectory()) {
                throw new Exception("configured pilot log directory is " +
                        "not a directory: " + this.logdirPath);
            }

            if (!logdirFile.canWrite()) {
                throw new Exception("configured pilot log directory is " +
                        "not a directory that is writeable for this " +
                        "user: " + this.logdirPath);
            }
        } else {
            throw new Exception("logdirPath setting is missing from" +
                        " pilot slot management configuration");
        }

        this.db = new PilotSlotManagementDB(this.dataSource, this.lager);

        if (sshNotificationEnabled) {
            this.cp = new CursorPersistence(this.db, this.timerManager, 5000);

            final String eventsPath = this.pollScript + ".txt";
            logger.debug("Setting events file to '" + eventsPath + "'");

            this.watcher = new PilotPoll(this.timerManager,
                                         this.lager,
                                         this.watcherDelay,
                                         eventsPath,
                                         this.db.currentCursorPosition(),
                                         this);

            this.watcher.scheduleNotificationWatcher();

            // this will consume pilot notifications sent during the time
            // the container was down
        }

        if (httpNotificationEnabled) {
            this.httpListener = new HTTPListener(this.contactPort,
                                                 this.accountsPath,
                                                 this,
                                                 this.sharedExecutor,
                                                 this.lager);
            this.httpNotifyString = this.httpListener.getContactURL();
            this.httpListener.start();
        }


    }

    /* ************************ */
    /* SlotManagement interface */
    /* ************************ */

    /**
     * @param request a single workspace or homogenous group-workspace request
     *
     * @return Reservation res
     * @throws ResourceRequestDeniedException exc
     */
    public Reservation reserveSpace(NodeRequest request)
            throws ResourceRequestDeniedException {

        this.reserveSpace(request.getIds(),
                          request.getMemory(),
                          request.getDuration(),
                          request.getGroupid(),
                          request.getCreatorDN());

        return new Reservation(request.getIds());
    }

    /**
     * @param requests  an array of single workspace or homogenous
     *                  group-workspace requests
     * @param coschedid coscheduling (ensemble) ID
     *
     * @return Reservation res
     * @throws ResourceRequestDeniedException exc
     */
    public Reservation reserveCoscheduledSpace(NodeRequest[] requests,
                                               String coschedid)
            throws ResourceRequestDeniedException {

        if (requests == null || requests.length == 0) {
            throw new IllegalArgumentException("requests null or length 0?");
        }

        // the LRM request will be for the highest memory and duration (the
        // lesser workspaces will not get this extra memory or time -- this is
        // capacity vs. mapping and we will get more sophisticated here later)

        int highestMemory = 0;
        int highestDuration = 0;

        final ArrayList idInts = new ArrayList(64);
        final ArrayList allDurations = new ArrayList(64);
        
        for (int i = 0; i < requests.length; i++) {

            final int thisMemory = requests[i].getMemory();

            if (highestMemory < thisMemory) {
                highestMemory = thisMemory;
            }

            final int thisDuration = requests[i].getDuration();

            if (highestDuration < thisDuration) {
                highestDuration = thisDuration;
            }

            final int[] ids = requests[i].getIds();
            if (ids == null) {
                throw new ResourceRequestDeniedException(
                        "Cannot proceed, no ids in NodeRequest parameter (?)");
            }

            for (int j = 0; j < ids.length; j++) {
                idInts.add(new Integer(ids[j]));
                allDurations.add(new Integer(thisDuration));
            }
        }

        final int length = idInts.size();
        final int[] all_ids = new int[length];
        final int[] all_durations = new int[length];

        for (int i = 0; i < length; i++) {
            all_ids[i] = ((Number)idInts.get(i)).intValue();
            all_durations[i] = ((Number)allDurations.get(i)).intValue();
        }

        // Assume that the creator's DN is the same for each node
        final String creatorDN = requests[0].getCreatorDN();

        this.reserveSpace(all_ids, highestMemory, highestDuration, coschedid, creatorDN);
        return new Reservation(all_ids, null, all_durations);
    }

    /**
     * Only handling one slot per VM for now, will change in the future
     * (multiple layers).
     *
     * Only handling homogenous requests for now.
     *
     * @param vmids array of IDs.  If array length is greater than one, it is
     *        up to the implementation (and its configuration etc) to decide
     *        if each must map to its own node or not.  In the case where more
     *        than one VM is mapped to the same node, the returned node
     *        assignment array will include duplicates.
     * @param memory megabytes needed
     * @param duration seconds needed
     * @param uuid group ID, can not be null if vmids is length > 1
     * @param creatorDN the DN of the user who requested creation of the VM
     *
     *  @throws ResourceRequestDeniedException can not fulfill request
     */
    private void reserveSpace(final int[] vmids,
                              final int memory,
                              final int duration,
                              final String uuid,
                              final String creatorDN)
                  throws ResourceRequestDeniedException {

        if (vmids == null) {
            throw new IllegalArgumentException("no vmids");
        }


        if (memory > this.maxMB) {
            String msg = "Memory request (" + memory + " MB) cannot be " +
                    "fulfilled by any VMM node (maximum: " + this.maxMB +
                    " MB).";
            throw new ResourceRequestDeniedException(msg);
        }

        if (vmids.length > 1 && uuid == null) {
            logger.error("cannot make group space request without group ID");
            throw new ResourceRequestDeniedException("internal " +
                    "pilot management error");
        }

        final String slotid;
        if (uuid == null) {
            slotid = uuidGen.generateRandomBasedUUID().toString();
        } else {
            slotid = uuid;
            try {
                for (int i = 0; i < vmids.length; i++) {
                    // add to our own group register, encapsulated from
                    // main service group/coscheduling management
                    this.db.newGroupMember(uuid, vmids[i]);
                }
            } catch (WorkspaceDatabaseException e) {
                logger.error(e.getMessage(), e);
                throw new ResourceRequestDeniedException("internal " +
                        "pilot management error");
            }
        }

        this.reserveSpaceImpl(memory, duration, slotid, vmids, creatorDN);

        // pilot reports hostname when it starts running, not returning an
        // exception to signal successful best effort pending slot
    }

    private void reserveSpaceImpl(final int memory,
                                  final int duration,
                                  final String uuid,
                                  final int[] vmids,
                                  final String creatorDN)
            throws ResourceRequestDeniedException {

        final String outputFile = this.logdirPath + File.separator + uuid;

        final int dur = duration + this.padding;
        final long wallTime = duration + this.padding;

        // we know it's torque for now, no casing
        final ArrayList torquecmd;
        try {
            torquecmd = this.torque.constructQsub(this.destination,
                                                  memory,
                                                  vmids.length,
                                                  this.ppn,
                                                  wallTime,
                                                  this.extraProperties,
                                                  outputFile,
                                                  false,
                                                  false,
                                                  creatorDN);
            
        } catch (WorkspaceException e) {
            final String msg = "Problem with Torque argument construction";
            if (logger.isDebugEnabled()) {
                logger.error(msg + ": " + e.getMessage(), e);
            } else {
                logger.error(msg + ": " + e.getMessage());
            }
            // scrubbing what client sees
            throw new ResourceRequestDeniedException(msg);
        }

        // no casing necessary yet for pilot, only 0.2 supported now
        final ArrayList pilotCommon;
        try {
            pilotCommon = this.pilot.constructCommon(false, false, true, null);
        } catch (ManageException e) {
            final String msg = "Problem with pilot argument construction";
            if (logger.isDebugEnabled()) {
                logger.error(msg + ": " + e.getMessage(), e);
            } else {
                logger.error(msg + ": " + e.getMessage());
            }
            // scrubbing what client sees
            throw new ResourceRequestDeniedException(msg);
        }

        // same params sent to each pilot in a group job
        final ArrayList pilotReserveSlot;
        try {

            final String notifyString;
            if (this.httpNotifyString != null
                    && this.sshNotifyString != null) {

                notifyString = this.httpNotifyString + "+++" +
                               this.sshNotifyString;
            } else if (this.httpNotifyString != null) {
                notifyString = this.httpNotifyString;
            } else if (this.sshNotifyString != null) {
                notifyString = this.sshNotifyString;
            } else {
                final String msg = "No pilot-->service notification mechanism";
                throw new ResourceRequestDeniedException(msg);
            }

            pilotReserveSlot = this.pilot.constructReserveSlot(memory,
                                                               dur,
                                                               this.grace,
                                                               uuid,
                                                               notifyString);
        } catch (ManageException e) {
            final String msg = "Problem with pilot argument construction";
            if (logger.isDebugEnabled()) {
                logger.error(msg + ": " + e.getMessage(), e);
            } else {
                logger.error(msg + ": " + e.getMessage());
            }
            // scrubbing what client sees
            throw new ResourceRequestDeniedException(msg);
        }

        final StringBuffer pilotcmdbuf = new StringBuffer(256);

        if (this.multiJobPrefix != null && vmids.length > 1) {
            pilotcmdbuf.append(this.multiJobPrefix);
            pilotcmdbuf.append(" ");
        }

        pilotcmdbuf.append(this.pilotPath);
        Iterator iter = pilotCommon.iterator();
        while (iter.hasNext()) {
            pilotcmdbuf.append(" ");
            pilotcmdbuf.append(iter.next());
        }
        iter = pilotReserveSlot.iterator();
        while (iter.hasNext()) {
            pilotcmdbuf.append(" ");
            pilotcmdbuf.append(iter.next());
        }
        final String pilotcmd = pilotcmdbuf.toString();
        logger.info("pilot command = " + pilotcmd);

        final String[] cmd = (String[]) torquecmd.toArray(
                                            new String[torquecmd.size()]);
        String stdout;
        try {
            stdout = WorkspaceUtil.runCommand(cmd, true, pilotcmd,
                                              this.lager.eventLog,
                                              this.lager.traceLog);
        } catch (WorkspaceException e) {
            final String msg = "Problem calling Torque";
            if (logger.isDebugEnabled()) {
                logger.error(msg + ": " + e.getMessage(), e);
            } else {
                logger.error(msg + ": " + e.getMessage());
            }
            // scrubbing what client sees
            throw new ResourceRequestDeniedException(msg);
        } catch (ReturnException e) {
            final String msg = "Problem calling Torque";

            final StringBuffer buf = new StringBuffer(msg);
            buf.append(": return code = ")
               .append(e.retval);

            if (e.stderr != null) {
                buf.append(", stderr = '")
                   .append(e.stderr)
                   .append("'");
            } else {
                buf.append(", no stderr");
            }

            if (e.stdout != null) {
                buf.append(", stdout = '")
                   .append(e.stdout)
                   .append("'");
            } else {
                buf.append(", no stdout");
            }

            logger.error(msg + ": " + buf.toString());

            // scrubbing what client sees
            throw new ResourceRequestDeniedException(msg);
        }

        if (stdout == null || stdout.length() == 0) {
            final String msg = "Inexplicable problem receiving job ID from" +
                    " Torque (return == 0, but no stdout), aborting.";
            logger.error(msg);
            throw new ResourceRequestDeniedException(msg);
        }

        // TODO: analyze stdout here, should have no newlines or spaces
        logger.debug("torque stdout = " + stdout);

        stdout = stdout.trim();
        if (stdout.indexOf('\n') >= 0) {
            logger.warn("torque stdout has a new line");
            // todo: throw exc? strip it?
        }

        try {
            if (vmids.length == 1) {
                this.db.newSlot(uuid, vmids[0], stdout, dur);
            } else {
                this.db.newSlotGroup(uuid, vmids, stdout, dur);
            }
        } catch (WorkspaceDatabaseException e) {
            String msg = "Problem with service database, aborting pilot job.";

            if (logger.isDebugEnabled()) {
                logger.error(msg + ": " + e.getMessage(), e);
            } else {
                logger.error(msg + ": " + e.getMessage());
            }

            // we know it's torque for now, no casing
            try {
                WorkspaceUtil.runCommand(this.torque.constructQdel(stdout),
                                         this.lager.eventLog,
                                         this.lager.traceLog);

            } catch (WorkspaceException e2) {
                String msg2 = " (and problem with Torque qdel)";
                msg += msg2;

                if (logger.isDebugEnabled()) {
                    logger.error(msg2 + ": " + e2.getMessage(), e2);
                } else {
                    logger.error(msg2 + ": " + e2.getMessage());
                }
            } catch (ReturnException e2) {
                String msg2 = " (and problem calling Torque qdel)";
                msg += msg2;

                StringBuffer buf = new StringBuffer(msg2);
                buf.append(": return code = ")
                   .append(e2.retval);

                if (e2.stderr != null) {
                    buf.append(", stderr = '")
                       .append(e2.stderr)
                       .append("'");
                } else {
                    buf.append(", no stderr");
                }

                if (e2.stdout != null) {
                    buf.append(", stdout = '")
                       .append(e2.stdout)
                       .append("'");
                } else {
                    buf.append(", no stdout");
                }

                logger.error(buf.toString());
            }
            throw new ResourceRequestDeniedException(msg);
        }

        if (this.watcher != null) {
            this.watcher.scheduleNotificationWatcher();
        }
    }

    public boolean canCoSchedule() {
        return true;
    }

    public void releaseSpace(int reservationID) throws ManageException {

        if (lager.traceLog) {
            logger.trace("releaseSpace(), id = " + reservationID);
        }
        PilotSlot slot;
        try {
            slot = this.db.getSlot(reservationID);
        } catch (SlotNotFoundException e) {
            // fine in several cases
            logger.debug("slot with vmid " + reservationID + " not found");
            return;
        }

        if (slot.partOfGroup) {
            // todo: move to lock per uuid, LockManager etc.
            synchronized (this.groupLock) {

                // Need to retrieve again after being under lock because others
                // in the group will in many cases be destroyed at once and
                // there can now be situations where okToReleaseBlock can
                // succeed more than once.  For example this will happen in
                // the situation where a pilot job is still pending with the
                // LRM when a group/ensemble is destroyed.

                PilotSlot aslot;
                try {
                    aslot = this.db.getSlot(reservationID);
                } catch (SlotNotFoundException e) {
                    return;
                }


                // one slot in the block has enough information to be used
                // in impl of okToReleaseBlock() and releaseSpaceImpl()
                if (okToReleaseBlock(aslot)) {
                    // force qdel for block release
                    this.releaseSpaceImpl(aslot, false);
                } else {
                    logger.debug("Slot is part of block and it is not " +
                            "OK to release entire block yet");
                }
            }
        } else {
            this.releaseSpaceImpl(slot, slot.terminal);
        }
    }

    // add release-pending and check if all other VMs in block's pilots
    // have a release-pending or not
    private boolean okToReleaseBlock(PilotSlot slot) throws ManageException {
        try {
            if (slot.nodename != null) {
                this.db.setSlotPendingRemove(slot);
            }
        } catch (SlotNotFoundException e) {
            // this should never happen because of groupLock
            logger.error("inexplcable problem, block slot found but " +
                         "then not updateable");
        }

        int[] vmids = this.db.findVMsInGroup(slot.uuid);
        for (int i = 0; i < vmids.length; i++) {
            try {
                PilotSlot aSlot = this.db.getSlot(vmids[i]);
                if (aSlot.nodename == null) {
                    continue;
                }
                if (!aSlot.pendingRemove) {
                    return false;
                }
            } catch (SlotNotFoundException e) {
                // again, should not happen because of groupLock
                logger.error("slot for vm #" + vmids[i] + " not found?");
            }
        }
        return true;
    }

    private void releaseSpaceImpl(PilotSlot slot,
                                  boolean terminal) throws ManageException {

        // If we've already received word from the pilot that it is in a
        // terminal situation, no need to invoke qdel because the pilot's exit
        // will end the LRM job -- unless the slot is still pending and
        // therefore the pilot has not been run yet
        
        if (!terminal || slot.pending) {
            try {

                WorkspaceUtil.runCommand(
                                this.torque.constructQdel(slot.lrmhandle),
                                this.lager.eventLog,
                                this.lager.traceLog,
                                slot.vmid);
                
            } catch (WorkspaceException e) {
                String msg = "Problem with Torque qdel";

                if (logger.isDebugEnabled()) {
                    logger.error(msg + ": " + e.getMessage(), e);
                } else {
                    logger.error(msg + ": " + e.getMessage());
                }
            } catch (ReturnException e) {
                String msg = "Problem calling Torque qdel";

                StringBuffer buf = new StringBuffer(msg);
                buf.append(": return code = ")
                   .append(e.retval);

                if (e.stderr != null) {
                    buf.append(", stderr = '")
                       .append(e.stderr)
                       .append("'");
                } else {
                    buf.append(", no stderr");
                }

                if (e.stdout != null) {
                    buf.append(", stdout = '")
                       .append(e.stdout)
                       .append("'");
                } else {
                    buf.append(", no stdout");
                }

                logger.error(buf.toString());
            }
        }

        // In most situations we will hear from the pilot again about this
        // slot/block (it won't be here which is expected)

        this.db.removeSlot(slot.uuid);
    }

    public boolean isBestEffort() {
        return true;
    }

    /**
     * Strict evacuation means that the scheduler should not allow any time
     * consuming action on the slot after the running duration expires (actions
     * such as unpropagate).
     *
     * @return true if implementation requires strict evacuation
     */
    public boolean isEvacuationStrict() {
        return true;
    }

    public void setScheduler(Scheduler adapter) {
        this.schedulerAdapter = adapter;
    }

    public boolean isNeededAssociationsSupported() {
        return false;
    }

    /* ********************************** */
    /* NotificationPollCallback interface */
    /* ********************************** */

    public int numPendingNotifications() throws Exception {
        return this.db.numSlotsCached(false);
    }

    public void decreaseNumPending(int n) throws Exception {
        // ignored
    }

    public void cursorPosition(long pos) {
        if (this.cp != null) {
            this.cp.cursorPosition(pos);
        }
    }

    /* ************************** */
    /* SlotPollCallback interface */
    /* ************************** */

    /**
     * The pilot reports the slot has been successfully reserved and what host
     * it's ended up running on.
     *
     * @param slotid   uuid
     * @param hostname slot node
     * @param timestamp time of reservation
     */
    public void reserved(String slotid, String hostname, Calendar timestamp) {
        
        if (this.schedulerAdapter == null) {
            logger.error("Severe problem, slot manager has received word " +
                    "that the slot '" + slotid + "' is reserved (hostname '" +
                    hostname + "') but the manager has " +
                    "not been configured with a way to inform service " +
                    "scheduler to proceed.");
            try {
                PilotSlot slot = this.getSlotAndAssignVM(slotid, hostname);
                // this eventually causes this.releaseSpace() to be called
                // unless there was a race
                this.cancelWorkspace(slot.vmid, SEVERE_PILOT_FAULT);
            } catch (SlotNotFoundException e) {
                logger.error(e.getMessage());
            } catch (WorkspaceDatabaseException e) {
                logger.error(e.getMessage());
            }
            return;
        }

        try {

            final PilotSlot slot = this.getSlotAndAssignVM(slotid, hostname);

            if (hostname == null) {

                logger.error("Pilot '" + slotid + "' sent reserved message " +
                        "without hostname (?). Cancelling vm #" + slot.vmid +
                             " and running trash.");

                // this eventually causes this.releaseSpace() to be called
                // unless there was a race
                this.cancelWorkspace(slot.vmid, SEVERE_PILOT_FAULT);
                return;
            }

            if (timestamp == null) {

                logger.error("Pilot '" + slotid + "' sent reserved message " +
                        "without timestamp (?). Cancelling vm #" + slot.vmid +
                             " and running trash.");

                // this eventually causes this.releaseSpace() to be called
                // unless there was a race
                this.cancelWorkspace(slot.vmid, SEVERE_PILOT_FAULT);
                return;
            }

            final InstanceResource resource;
            try {
                resource = this.instHome.find(slot.vmid);
            } catch (DoesNotExistException e) {
                final String msg = "workspace #" + slot.vmid + " is unknown " +
                        "to the service but the pilot tracker has receieved " +
                        "space for it to run?  pilot ID: '" + slotid + "' " +
                        "There is nothing we can do about this.";
                logger.error(e.getMessage());
                return;
            }

            final int runningTime =
                    resource.getVM().getDeployment().getMinDuration();

            // double-checking assumptions
            if (runningTime > slot.duration - this.padding) {
                logger.error("The running time stored for workspace #" +
                        slot.vmid + " is greater than slot duration (?). " +
                        "Implementation error, backing out.");
                this.cancelWorkspace(slot.vmid, SEVERE_PILOT_FAULT);
                return;
            }

            logger.debug("reserved: running time = " + runningTime +
                         " (slot duration = " + slot.duration + ")");
            Calendar stop = (Calendar) timestamp.clone();
            stop.add(Calendar.SECOND, runningTime);

            Calendar slotstop = (Calendar) timestamp.clone();
            slotstop.add(Calendar.SECOND, slot.duration);

            String msg = Lager.ev(slot.vmid) +
                    "Pilot '" + slot.uuid + "' reserved for VM " +
                    slot.vmid + " @ host '" + hostname + "'.  Started at: " +
                    localFormat.format(timestamp.getTime()) + ".  VM " +
                    "running time ends at: " +
                    localFormat.format(stop.getTime()) + ".  Slot will " +
                    "end itself at approximately: " +
                    localFormat.format(slotstop.getTime());
            if (lager.eventLog) {
                logger.info(msg);
            } else {
                logger.debug(msg);
            }

            this.schedulerAdapter.slotReserved(slot.vmid,
                                               timestamp, 
                                               stop,
                                               hostname);
            
        } catch (ManageException e) {
            if (logger.isDebugEnabled()) {
                logger.error(e.getMessage(), e);
            } else {
                logger.error(e.getMessage());
            }
        } catch (SlotNotFoundException e) {

            String msg = "Severe problem, hearing about a slot being " +
                    "reserved but service has no record of it.  Slotid: " +
                    slotid + ", hostname: " + hostname + " (can't qdel or " +
                    "cancel it, we don't know the LRM handle or workspace ID)";
            logger.error(msg);
        }
    }

    /**
     * The pilot reports it started running but the slot was not successfully
     * reserved beacuse of some problem.
     *
     * @param slotid uuid
     * @param hostname hostname
     * @param error  error message
     */
    public void errorReserving(String slotid, String hostname, String error) {
        try {

            PilotSlot slot = this.getSlotAndAssignVM(slotid, hostname);

            String id = "Pilot '" + slotid + "'";
            if (hostname != null) {
                id += " @ host '" + hostname + "'";
            }

            if (slot.terminal) {
                logger.error(id + " had an error " +
                             "reserving: '" + error + "', nothing to do " +
                             "slot was already terminal -- unexpected this " +
                             "would be the case because this is the first " +
                             "we've heard from slot/sub-slot (?)");
            } else {

                logger.error(id + " had an error reserving: '" + error +
                             "' (cancelling vm #" + slot.vmid + " without " +
                             "running trash)");

                // this eventually causes this.releaseSpace() to be called
                // unless there was a race
                this.cancelWorkspaceNoTrash(slot.vmid, new Exception(error));

                this.db.setSlotTerminal(slot);
            }

        } catch (WorkspaceDatabaseException e) {
            logger.error(
                   getDBError("problem reserving",
                              slotid,
                              hostname,
                              e.getMessage()),
                   e);
        } catch (SlotNotFoundException e) {
            logger.error(getNoSlotError("problem reserving", slotid, hostname));
        }
    }

    /**
     *
     * The pilot reports that it has been interrupted and has determined the
     * signal was unexpected.  This can happen in three situations:
     *
     * 1. The LRM or administrator has decided to preempt the pilot for
     * whatever reason.
     *
     * 2. The node has been rebooted or shutdown.
     *
     * 3. The LRM job was cancelled by the slot manager (this class).
     * 
     * In each situation the pilot attempts to wait a specific (configurable)
     * ratio of the provided grace period.  In cases #1 and #2 this gives the
     * slot manager time to handle the problem (currently this involves running
     * shutdown-trash on all VMs in the slot).  In case #3 the slot manager can
     * just ignore this notification since it is already done with the slot
     * (which is why it cancelled the LRM job).
     *
     * @param slotid uuid
     * @param hostname hostname
     * @param timestamp the time that pilot sent this (second resolution only)
     *                  used to compute if we should act on it
     */
    public void earlyUnreserving(String slotid,
                                 String hostname,
                                 Calendar timestamp) {
        try {
            // SlotNotFoundException expected if we called qdel
            // (which is the usual situation)
            PilotSlot slot = this.db.getSlot(slotid, hostname);

            StringBuffer buf = new StringBuffer();
            buf.append("Pilot '")
               .append(slotid);

            if (hostname != null) {
                buf.append("' @ host '")
                   .append(hostname);
            }
            buf.append("' is being preempted early. Cancelling vm #")
               .append(slot.vmid);

            // Just before the notification was sent
            long timestampLong = timestamp.getTimeInMillis();

            // Now, which could be at any arbitrary time.
            Calendar now = Calendar.getInstance();
            long nowLong = Calendar.getInstance().getTimeInMillis();

            long difference = nowLong - timestampLong;
            difference = difference / 1000; //convert to seconds

            // Because this is an unusual situation, do a lot of logging
            localFormat.setCalendar(timestamp);
            int timestampGMTOffset = timestamp.getTimeZone().getRawOffset();
            buf.append(" || Notification sent @ ")
               .append(localFormat.format(timestamp.getTime()))
               .append(" -- GMT offset ")
               .append(timestampGMTOffset);

            localFormat.setCalendar(now);
            int nowGMTOffset = now.getTimeZone().getRawOffset();
            buf.append(" || Now: ")
               .append(localFormat.format(now.getTime()))
               .append(" -- GMT offset ")
               .append(nowGMTOffset);

            // This check is mostly here if the service was down and the
            // notification consumer is only now getting to the notifications.
            // We allow for a certain amount of inaccuracy on top of the grace
            // period (e.g. only using second resolution on the timestamp).

            int horizon = this.grace + 2;
            boolean trash = true;
            if (difference > horizon) {
                trash = false;
            }

            buf.append(" || Difference in seconds is ")
               .append(difference)
               .append(" which means we will ");

            if (!trash) {
                buf.append("not ");
            }
            buf.append("run shutdown-trash now (difference ");
            if (trash) {
                buf.append(" < ");
            } else {
                buf.append(" > ");
            }
            buf.append("than now + ")
               .append(horizon)
               .append(" seconds.");

            final String msg = buf.toString();
            if (lager.eventLog) {
                logger.info(Lager.ev(slot.vmid) + msg);
            } else {
                logger.debug(Lager.ev(slot.vmid) + msg);
            }

            final Exception e = new Exception("early LRMS preemption");
            if (trash) {
                this.cancelWorkspace(slot.vmid, e);
            } else {
                this.cancelWorkspaceNoTrash(slot.vmid, e);
            }

            this.db.setSlotTerminal(slot);

        } catch (WorkspaceDatabaseException e) {
            final String msg = getDBError("starting early-unreserving",
                                          slotid,
                                          hostname,
                                          e.getMessage());
            logger.error(msg, e);
        } catch (SlotNotFoundException e) {

            String id = "'" + slotid + "'";
            if (hostname != null) {
                id += " @ host '" + hostname + "'";
            }

            logger.debug("Pilot " + id + " is being preempted and " +
                    "we do not have a record of this slot anymore.  This " +
                    "is the expected situation if we have called qdel (this " +
                    "could also have happened if the service just " +
                    "recovered from being down).");
        }
    }

    /**
     * The pilot reports that there was a problem early unreserving, there is
     * no action to take. An error message will usually accompany this (for
     * logging to service logs).
     *
     * @param slotid uuid
     * @param hostname hostname
     * @param error  error message
     */
    public void errorEarlyUnreserving(String slotid,
                                      String hostname,
                                      String error) {
        logger.error("Pilot '" + slotid + "' had an error " +
                         "early-unreserving: '" + error);
        try {
            // SlotNotFoundException expected
            PilotSlot slot = this.db.getSlot(slotid, hostname);

            String id = "Pilot '" + slotid + "'";
            if (hostname != null) {
                id += " @ host '" + hostname + "'";
            }

            logger.warn(id + " had an error early-unreserving. Antecedent " +
                         "unreserving notification either never have made " +
                         "it or service's clean up crossed paths with the " +
                         "pilot's grace period expiring" +
                         ": (cancelling vm " + slot.vmid + "without running " +
                         "trash, a resource-not-found error is likely)");

            // this eventually causes this.releaseSpace() to be called
            // unless there was a race
            final Exception e = new Exception("LRMS preemption with error");
            this.cancelWorkspaceNoTrash(slot.vmid, e);

            this.db.setSlotTerminal(slot);

        } catch (WorkspaceDatabaseException e) {
            final String msg = getDBError("problem early-unreserving",
                                          slotid,
                                          hostname,
                                          e.getMessage());
            logger.error(msg, e);
        } catch (SlotNotFoundException e) {
            // expected
        }
    }

    /**
     * The pilot reports that it has begun unreserving the slot, there is
     * nothing to be done now, this is the end (whether it passes or fails). If
     * there was something the manager was expected to do, earlyUnreserving
     * would have been called.
     *
     * @param slotid uuid
     * @param hostname hostname
     */
    public void unreserving(String slotid, String hostname) {

        try {
            PilotSlot slot = this.db.getSlot(slotid, hostname);

            String id = "Pilot '" + slotid + "'";
            if (hostname != null) {
                id += " @ host '" + hostname + "'";
            }

            logger.error(id + " is being unreserved: " +
                         "Cancelling vm #" + slot.vmid +
                         " (which should be gone already).");

            // this eventually causes this.releaseSpace() to be called
            // unless there was a race
            this.cancelWorkspaceNoTrash(slot.vmid, null);
            
            this.db.setSlotTerminal(slot);

        } catch (WorkspaceDatabaseException e) {
            String msg = getDBError("starting unreserving",
                                        slotid,
                                        hostname,
                                        e.getMessage());
            logger.error(msg, e);
        } catch (SlotNotFoundException e) {
            String id = "Pilot '" + slotid + "'";
            if (hostname != null) {
                id += " @ host '" + hostname + "'";
            }
            logger.debug(id + " is shutting down and " +
                    "we do not have a record of this slot anymore.  This " +
                    "is the expected situation.");
        }
    }

    /**
     * The pilot reports it has killed VMs.
     *
     * @param slotid uuid
     * @param hostname hostname
     * @param killed array of killed VM IDs
     */
    public void kills(String slotid, String hostname, String[] killed) {

        if (killed == null) {
            logger.error("erroneous notification, killed but null VM list");
            return;
        }
        if (killed.length == 0) {
            logger.error("erroneous notification, killed but empty VM list");
            return;
        }
        
        StringBuffer buf = new StringBuffer();
        buf.append("'")
           .append(killed[0])
           .append("'");
        for (int i = 1; i < killed.length; i++) {
            buf.append(", '")
               .append(killed[i])
               .append("'");
        }
        String id = "Pilot '" + slotid + "'";
        if (hostname != null) {
            id += " @ host '" + hostname + "'";
        }
        logger.error(id + "' had to kill these VMS: " + buf.toString());

        try {
            this.db.setSlotTerminal(this.db.getSlot(slotid, hostname));
        } catch (WorkspaceDatabaseException e) {
            logger.error(e.getMessage(), e);
        } catch (SlotNotFoundException e) {
            logger.error(getNoSlotRaceError("killing", slotid, hostname), e);
        }

        for (int i = 0; i < killed.length; i++) {
            // todo: String -> id implementation should be discovered
            final int vmid = XenUtil.xenNameToId(killed[i]);
            if (vmid < 0) {
                logger.error("We don't know about this killed VM '" +
                             killed[i] + "', nothing to do.");
            } else {
                final Exception e =
                        new Exception("LRMS preemption with error (kills)");
                this.cancelWorkspaceNoTrash(vmid, e);
            }
        }
    }

    /**
     * The pilot reports that it can not successfully unreserve the slot, this
     * is no action to take. An error message will usually accompany this (for
     * logging to service logs).
     *
     * @param slotid uuid
     * @param hostname hostname
     * @param error  error message
     */
    public void errorUnreserving(String slotid, String hostname, String error) {
        logger.error("Pilot '" + slotid + "' had an error " +
                         "unreserving: '" + error);
        try {
            // SlotNotFoundException expected
            PilotSlot slot = this.db.getSlot(slotid, hostname);

            String id = "Pilot '" + slotid + "'";
            if (hostname != null) {
                id += " @ host '" + hostname + "'";
            }

            logger.error(id + " had an error " +
                         "unreserving but the previously expected " +
                         "unreserving notification seems to never have made " +
                         "it: (cancelling vm without running trash: " +
                         slot.vmid + ")");

            // this eventually causes this.releaseSpace() to be called
            // unless there was a race
            this.cancelWorkspaceNoTrash(slot.vmid, new Exception(error));

            this.db.setSlotTerminal(slot);

        } catch (WorkspaceDatabaseException e) {
            String msg = getDBError("problem unreserving",
                                        slotid,
                                        hostname,
                                        e.getMessage());
            logger.error(msg, e);
        } catch (SlotNotFoundException e) {
            // expected
        }
    }

    private static String getDBError(String state,
                                     String slotid,
                                     String hostname,
                                     String err) {
        return "Severe problem, hearing about a pilot " +
                state + " but service has a DB problem. " +
                "Slot: " +  slotid + ", hostname: " + hostname +
                " (would have cancelled workspace" +
                " instance if we knew what it was). DB problem: " + err;
    }

    private static String getNoSlotError(String state,
                                         String slotid,
                                         String hostname) {
        return "Problem, hearing about a pilot " +
                state + " but service has no record of it. " +
                "Slotid: " +  slotid + ", hostname: " + hostname +
                " (would have cancelled workspace" +
                " instance if we knew what it was)";
    }

    private static String getNoSlotRaceError(String state,
                                             String slotid,
                                             String hostname) {
        return "Probably OK race condition because of external, " +
                "asynchronous notifications. Hearing about a pilot " +
                state + " but service has no record of it.  Slotid: " +
                slotid + ", hostname = " + hostname;
    }

    // see reserved() and errorReserving()
    PilotSlot getSlotAndAssignVM(String uuid, String hostname)
            throws WorkspaceDatabaseException, SlotNotFoundException {

        synchronized (this.groupLock) {
            return this.db.getSlotAndAssignVMImpl(uuid, hostname);
        }
    }

    /* ******************** */
    /* Service interactions */
    /* ******************** */

    private void cancelWorkspace(int vmid, Exception e) {

        this.cancelWorkspace(vmid, true, e);
    }

    private void cancelWorkspaceNoTrash(int vmid, Exception e) {
        
        this.cancelWorkspace(vmid, false, e);
    }

    private void cancelWorkspace(int vmid,
                                 boolean trash,
                                 Throwable t) {

        final InstanceResource resource;
        try {
            resource = this.instHome.find(vmid);
        } catch (Exception e) {
            final String msg = "Couldn't find workspace " + Lager.id(vmid) +
                    " to cancel, already gone.";
            logger.error(msg);
            return;
        }

        if (!trash) {
            resource.setVMMaccessOK(false);
        }
        try {
            final int curr = resource.getState();
            // no need to set a new state if it is already corrupted
            // (this is a check then act problem)
            if (curr < WorkspaceConstants.STATE_CORRUPTED) {
                resource.setState(curr + WorkspaceConstants.STATE_CORRUPTED,
                                  null);
            }
        } catch (ManageException e) {
            final String msg = "Couldn't set corrupted state on workspace "
                    + Lager.id(vmid) +
                    ", already gone.";
            if (logger.isDebugEnabled()) {
                logger.error(msg, e);
            } else {
                logger.error(msg);
            }
        }
    }

    /* None of the below are used in the context of the pilot service at this time */
    public int getBackfillVMID() {
        int tmp=-1;
        return tmp;
    }

    public boolean isOldBackfillID(int vmid) {
        return false;
    }

    public boolean isCurrentBackfillID(int vmid) {
        return false;
    }
}
