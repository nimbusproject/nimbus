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

package org.globus.workspace.scheduler.defaults;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.workspace.Lager;
import org.globus.workspace.WorkspaceConstants;
import org.globus.workspace.LockManager;
import org.globus.workspace.persistence.DataConvert;
import org.globus.workspace.persistence.WorkspaceDatabaseException;
import org.globus.workspace.scheduler.Reservation;
import org.globus.workspace.scheduler.Scheduler;
import org.globus.workspace.scheduler.IdHostnameTuple;
import org.globus.workspace.service.InstanceResource;
import org.globus.workspace.service.WorkspaceHome;
import org.globus.workspace.service.binding.GlobalPolicies;
import org.globus.workspace.LockAcquisitionFailure;
import org.nimbustools.api.services.rm.ResourceRequestDeniedException;
import org.nimbustools.api.services.rm.SchedulingException;
import org.nimbustools.api.services.rm.DoesNotExistException;
import org.nimbustools.api.services.rm.ManageException;

import javax.sql.DataSource;
import java.text.DateFormat;
import java.util.Calendar;

import commonj.timers.TimerManager;
import edu.emory.mathcs.backport.java.util.concurrent.locks.Lock;

public class DefaultSchedulerAdapter implements Scheduler {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------
    
    private static final Log logger =
        LogFactory.getLog(DefaultSchedulerAdapter.class.getName());


    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected final DataSource dataSource;
    protected final SlotManagement slotManager;
    protected final TimerManager timerManager;
    protected final GlobalPolicies globals;
    protected final DataConvert dataConvert;
    protected final Lager lager;

    // lock per coscheduling ID (see usage to know why)
    protected final LockManager lockManager;

    
    protected WorkspaceHome home; // see setHome
    protected boolean valid;
    
    protected DefaultSchedulerAdapterDB db;

    // optionally set via config
    protected long sweeperDelay = 2000;

    protected DefaultSchedulerSweeper sweeper;

    // see CreationPending class comment
    protected final CreationPending creationPending = new CreationPending();


    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    public DefaultSchedulerAdapter(LockManager lockManager,
                                   SlotManagement slotManager,
                                   DataSource dataSource,
                                   TimerManager timerManager,
                                   GlobalPolicies globalPolicies,
                                   DataConvert dataConvert,
                                   Lager lagerImpl) {

        if (lockManager == null) {
            throw new IllegalArgumentException("lockManager may not be null");
        }
        this.lockManager = lockManager;
        
        if (slotManager == null) {
            throw new IllegalArgumentException("slotManager may not be null");
        }
        this.slotManager = slotManager;

        if (dataSource == null) {
            throw new IllegalArgumentException("dataSource may not be null");
        }
        this.dataSource = dataSource;
        
        if (timerManager == null) {
            throw new IllegalArgumentException("timerManager may not be null");
        }
        this.timerManager = timerManager;
        
        if (globalPolicies == null) {
            throw new IllegalArgumentException("globalPolicies may not be null");
        }
        this.globals = globalPolicies;
        
        if (dataConvert == null) {
            throw new IllegalArgumentException("dataConvert may not be null");
        }
        this.dataConvert = dataConvert;

        if (lagerImpl == null) {
            throw new IllegalArgumentException("lagerImpl may not be null");
        }
        this.lager = lagerImpl;
    }


    // -------------------------------------------------------------------------
    // MODULE SET (avoids circular dependency problem)
    // -------------------------------------------------------------------------

    public void setHome(WorkspaceHome homeImpl) {
        if (homeImpl == null) {
            throw new IllegalArgumentException("homeImpl may not be null");
        }
        this.home = homeImpl;
    }
    

    // -------------------------------------------------------------------------
    // IoC INIT METHOD
    // -------------------------------------------------------------------------

    public synchronized void validate() throws Exception {

        logger.debug("validating/initializing");

        if (this.home == null) {
            throw new Exception("instance home reference was not configured " +
                    "properly, cannot continue.");
        }

        this.db = new DefaultSchedulerAdapterDB(this.dataSource, this.lager);

        try {
            this.db.prepareStatements();
        } catch (WorkspaceDatabaseException e) {
            throw new Exception("Problem preparing DB statements: ", e);
        }

        this.sweeper =
                new DefaultSchedulerSweeper(this.timerManager,
                                            this.home,
                                            this.sweeperDelay,
                                            this.lager, 
                                            this);

        if (this.slotManager.isBestEffort()) {
            // there could otherwise be a loop, todo: reexamine with spring
            this.slotManager.setScheduler(this);
        }

        if (this.slotManager.isEvacuationStrict()) {
            this.globals.setUnpropagateAfterRunningTimeEnabled(false);
        } else {
            this.globals.setUnpropagateAfterRunningTimeEnabled(true);
        }

        this.valid = true;

        final StringBuffer buf = new StringBuffer(256);

        buf.append("SlotManagement adapter configured: ")
           .append(this.slotManager.getClass().getName())
           .append(".  Supports matching resources with ")
           .append("required networks: ")
           .append(this.slotManager.isNeededAssociationsSupported())
           .append(".  Finds space on a best-effort basis: ")
           .append(this.slotManager.isBestEffort())
           .append(".");

        logger.debug(buf.toString());

        logger.debug("validated/initialized");
    }

    // optional set in JNDI config, default is 2 seconds
    public void setSweeperDelay(long delay) {
        if (delay < 1000) {
            logger.error("cannot set sweeper delay to less than one" +
                    "second, default is 2 seconds");
        } else {
            this.sweeperDelay = delay;
        }
    }

    public long getSweeperDelay() {
        return this.sweeperDelay;
    }

    public Reservation schedule(int memory,
                                int duration,
                                String[] neededAssociations,
                                int numNodes,
                                String groupid,
                                String coschedid)

            throws SchedulingException,
                   ResourceRequestDeniedException {

        if (!this.valid) {
            throw new SchedulingException("scheduler was instantiated " +
                    "incorrectly"); // note for future IoC muckers 
        }

        if (coschedid != null && !this.slotManager.canCoSchedule()) {
            throw new ResourceRequestDeniedException("this " +
                    "scheduler can not coschedule, ensemble usage is not " +
                    "supported");
        }

        final int[] ids;
        try {
            ids = this.db.getNextTasktIds(numNodes);
        } catch (WorkspaceDatabaseException e) {
            throw new SchedulingException(e.getMessage(), e);
        }
        final String[] assocs;
        if (this.slotManager.isNeededAssociationsSupported()) {
            assocs = neededAssociations;
        } else {
            assocs = null;
        }

        // see CreationPending class comment
        this.creationPending.pending(ids);

        final NodeRequest req =
                new NodeRequest(ids, memory, duration, assocs, groupid);

        try {

            if (coschedid == null) {
                return this.scheduleImpl(req);
            } else {
                this.scheduleCoschedImpl(req, coschedid);
                return new Reservation(ids, null);
            }
            
        } catch (WorkspaceDatabaseException e) {
            this.creationPending.notpending(ids);
            throw new SchedulingException(e.getMessage(), e);
        } catch (ResourceRequestDeniedException e) {
            this.creationPending.notpending(ids);
            throw e;
        } catch (Throwable t) {
            this.creationPending.notpending(ids);
            throw new SchedulingException(t.getMessage(), t);
        }
    }

    private Reservation scheduleImpl(NodeRequest req)
                throws WorkspaceDatabaseException,
                       ResourceRequestDeniedException {

        final String invalidResponse = "Implementation problem: slot " +
                "manager returned invalid response";

        final Reservation res = this.slotManager.reserveSpace(req);

        if (res == null) {
            throw new ResourceRequestDeniedException(
                    invalidResponse + ": null response");
        }

        if (res.getResponseLength() == 0) {

            // Because reserveSpace will throw a request denied exception
            // if there was a problem asking for space, no node assignments
            // here we assume means best effort behavior is being used.
            // Check that assumption:

            if (!this.slotManager.isBestEffort()) {
                throw new ResourceRequestDeniedException(
                        invalidResponse + ": no address(es) were returned " +
                                "but not using a best-effort slot manager");
            }

            return res;
        }

        // otherwise, this will be a 'concrete' reservation

        if (res.getResponseLength() != req.getIds().length) {

            logger.fatal("node selection response is length " +
                         res.getResponseLength() +
                         " which does not match requested length " +
                         req.getIds().length + " -- attempting backout.");
            
            for (int i = 0; i < req.getIds().length; i++) {
                try {
                    this.slotManager.releaseSpace(req.getIds()[i]);
                } catch (ManageException e) {
                    if (logger.isDebugEnabled()) {
                        logger.error(e.getMessage(), e);
                    } else {
                        logger.error(e.getMessage());
                    }
                }
            }
            throw new ResourceRequestDeniedException(
                                "internal service error when reserving space");
        }

        final Calendar start = Calendar.getInstance();
        final Calendar stop = Calendar.getInstance();
        stop.add(Calendar.SECOND, req.getDuration());

        res.setStartTime(start);
        res.setStopTime(stop);

        for (int i = 0; i < req.getIds().length; i++) {
            this.db.scheduleTasks(req.getIds()[i], stop);
        }

        return res;
    }

    private void scheduleCoschedImpl(NodeRequest req,
                                     String coschedid)

                throws WorkspaceDatabaseException,
                       ResourceRequestDeniedException {

        final Lock lock = this.lockManager.getLock(coschedid);

        try {
            lock.lockInterruptibly();
        } catch (InterruptedException e) {
            throw new WorkspaceDatabaseException(
                        new LockAcquisitionFailure(e));
        }

        try {

            if (this.db.isCoschedDone(coschedid)) {
                throw new ResourceRequestDeniedException(
                        "co-scheduling group " + Lager.ensembleid(coschedid) +
                        "has already had a successful done operation " +
                        "invocation.  No additions may be made.");
            }

            this.db.addNodeRequest(req, coschedid);

        } finally {
            lock.unlock();
        }
    }

    /**
     * @param coschedid coschedid
     * @throws ResourceRequestDeniedException exc
     */
    public void proceedCoschedule(String coschedid)

            throws SchedulingException,
                   ResourceRequestDeniedException {

        if (!this.valid) {
            throw new SchedulingException("scheduler was instantiated " +
                    "incorrectly"); // note for future IoC muckers
        }

        final Lock lock = lockManager.getLock(coschedid);

        try {
            lock.lockInterruptibly();
        } catch (InterruptedException e) {
            throw new SchedulingException(
                        new LockAcquisitionFailure(e));
        }

        try {
            this.proceedCoscheduleImpl(coschedid);
        } catch (WorkspaceDatabaseException e) {
            throw new SchedulingException(e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }

    // method is under lock on coschedid
    private void proceedCoscheduleImpl(String coschedid)

            throws WorkspaceDatabaseException,
                   ResourceRequestDeniedException {

        if (this.db.isCoschedDone(coschedid)) {
            throw new ResourceRequestDeniedException(
                    "co-scheduling group " + Lager.ensembleid(coschedid) +
                    " has already had a successful done-operation" +
                    " invocation.");
        }

        final NodeRequest[] reqs = this.db.getNodeRequests(coschedid);

        if (reqs == null || reqs.length == 0) {
            throw new ResourceRequestDeniedException(
                    "co-scheduling group " + Lager.ensembleid(coschedid) +
                    " has no pending requests stored for it but done is" +
                    " being called?");
        }

        final Reservation res =
                this.slotManager.reserveCoscheduledSpace(reqs, coschedid);

        final String invalidResponse = "Implementation problem: slot " +
                "manager returned invalid response";

        if (res == null) {
            throw new ResourceRequestDeniedException(
                    invalidResponse + ": null response");
        }

        if (res.getResponseLength() == 0) {
            
            // Because reserveCoscheduledSpace should throw a request denied
            // exception if there was a problem asking for space, no node
            // assignments here we assume means best effort behavior is being
            // used. Check that assumption:

            if (!this.slotManager.isBestEffort()) {
                this.fatalityBackoutReservation(res);
                throw new ResourceRequestDeniedException(
                        invalidResponse + ": no address(es) but not a best " +
                                "effort manager");
            }
        }

        // If reserveCoscheduledSpace generated an exception we let it
        // fly and leave the option open to call done again, but from here
        // on it is now done for good.
        try {
            this.db.deleteNodeRequestsAndBeDone(coschedid);
        } catch (Throwable t) {
            String msg = "Problem removing node requests from co-scheduling " +
                    "tracking.  This is severe, should never happen.  " +
                    "Attempting backout of slot reservations, this will " +
                    "probably fail as well.  Problem: \"" + t.getMessage();

            logger.fatal(msg, t);

            final String fullResponse = msg + "\", Backout result: " +
                                  this.fatalityBackoutReservation(res);
            throw new WorkspaceDatabaseException(fullResponse);
        }

        if (!this.slotManager.isBestEffort()) {
            
            // NOTE: coscheduling creates a situation where otherwise
            // not-best-effort slot managers make the DefaultSchedulerAdapter
            // *appear* to the service as a best effort scheduler.  That is OK
            // and expected (because of the add, add, ..."done now" ensemble
            // mechanism).  But this in no way means that the particular slot
            // manager *plugin implementation* as plugin to the
            // DefaultSchedulerAdapter should behave any differently than we
            // expect it to.

            if (!res.hasDurationList()) {
                throw new ResourceRequestDeniedException(
                        invalidResponse + ": no durations");
            }

            final int len = res.getResponseLength();
            for (int i = 0; i < len; i++) {
                final IdHostnameTuple idhost = res.getIdHostnamePair(i);
                final Calendar start = Calendar.getInstance();
                final Calendar stop = Calendar.getInstance();
                stop.add(Calendar.SECOND, res.getDurationByIndex(i));

                try {
                    this.slotReserved(idhost.id,
                                      start,
                                      stop,
                                      idhost.hostname);
                } catch (ManageException e) {
                    // log and let user decide how to cleanup
                    logger.error(e.getMessage());
                }
            }
        }

        // if it is a best-effort, there is nothing to do
    }

    // backout caused by severe issue
    private String fatalityBackoutReservation(Reservation res) {
        if (res == null) {
            return "Illegal Argument, null reservation";
        }
        final StringBuffer buf = new StringBuffer(1024);
        try {
            final int[] ids = res.getIds();
            this.slotManager.releaseSpace(ids[0]);
            buf.append(ids[0]).append(" OK");
            for (int i = 0; i < ids.length; i++) {
                buf.append(", ");
                this.slotManager.releaseSpace(ids[i]);
                buf.append(ids[i]).append(" OK");
            }
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
            buf.append("Problem: \"")
               .append(t.getMessage())
               .append("\"");
        }
        return buf.toString();
    }

    /**
     * Called by the slot manager module when space has been reserved for
     * a workspace.  If the slot manager is best effort the scheduler will
     * not invoke anything until it hears this notification.
     *
     * Stop is necessary for scheduler, start and hostname are passed in
     * for convenience, better model is to adjust the resource in the slot
     * manager with the new information, but scheduler needs to retrieve
     * the resource anyhow.
     * TODO: refactor in future when there are more schedulers and slot mgrs
     *  
     * @param vmid id
     * @param start time slot started
     * @param stop time to shut down
     * @param hostname node VM is running on
     * @throws ManageException exc
     */
    public void slotReserved(int vmid,
                             Calendar start,
                             Calendar stop,
                             String hostname) throws ManageException {

        if (!this.valid) {
            throw new ManageException("scheduler was instantiated " +
                    "incorrectly"); // note for future IoC muckers
        }
        
        this.stateNotification(vmid,
                               WorkspaceConstants.STATE_FIRST_LEGAL,
                               start,
                               stop,
                               hostname);
    }

    /**
     * Called by the workspace service whenever a workspace changes state.
     *
     * Notification of STATE_FIRST_LEGAL, is the signal that the scheduler
     * may now act on this id as it pleases.
     *
     * @param id the id
     * @param state the new state
     * @throws ManageException
     */
    public void stateNotification(int id, int state)
                                    throws ManageException {
        if (!this.valid) {
            throw new ManageException("scheduler was instantiated " +
                    "incorrectly"); // note for future IoC muckers
        }
        this.stateNotification(id, state, null, null, null);
    }

    public void stateNotification(int id,
                                  int state,
                                  Calendar start,
                                  Calendar stop,
                                  String hostname) throws ManageException {

        if (id < 0) {
            logger.fatal("invalid id: " + id);
            return;
        }

        if (lager.traceLog) {
            String msg = "stateNotification(): " + Lager.id(id) +
                          ", state = " + this.dataConvert.stateName(state) +
                          ", stop = ";
            if (stop != null) {
                DateFormat localFormat = DateFormat.getDateTimeInstance();
                msg += "'" + localFormat.format(stop.getTime()) + "'";
            } else {
                msg += "null";
            }
            logger.trace(msg);
        }

        if (state == WorkspaceConstants.STATE_DESTROYING) {
            
            // could be from scheduler backout during create, remember pending
            // just means "between the time the scheduler conjures the id
            // numbers and the time when the service creates and finalizes
            // the new resources"
            this.creationPending.notpending(id);
            
            remove(id);
            return;
        }

        // This simple scheduler implementation only decides what to do at
        // creation and when to shutdown (with lockdown of web services start
        // and shutdown operations).

        InstanceResource resource = null;

        // In the case of a slot obtained in a delayed manner (e.g. using
        // the workspace-pilot and/or coscheduled), we cannot act on
        // STATE_FIRST_LEGAL unless this.slotReserved is called (where
        // stop is !null).
        // In the future, stop time should not be overloaded like this, make
        // a more explicit parameter.

        boolean noActivateSituation = false;
        boolean populateAndSchedule = false;

        if (state == WorkspaceConstants.STATE_FIRST_LEGAL) {

            if (this.slotManager.isBestEffort() && stop == null) {

                noActivateSituation = true;

            } else if (this.slotManager.isBestEffort()) {

                populateAndSchedule = true;

            } else {

                resource = this.fetchResource(id, state);
                if (resource == null) {
                    return;
                }

                if (resource.getEnsembleId() != null) {
                    if (stop == null) {
                        noActivateSituation = true;
                    } else {
                        populateAndSchedule = true;
                    }
                }
            }


        }

        if (state == WorkspaceConstants.STATE_FIRST_LEGAL) {
            this.creationPending.notpending(id);
            if (noActivateSituation) {
                return;
            }
        }

        // Since it is immediate, nothing needs to be decided.
        if (state == WorkspaceConstants.STATE_FIRST_LEGAL) {

            if (resource == null) {
                resource = this.fetchResource(id, state);
                if (resource == null) {
                    return;
                }
            }

            if (populateAndSchedule) {

                // note that in the pilot case running time duration currently
                // equals requested running time, leaving no time for
                // unpropagation (need B scheduler), client will need to call
                // shutdown + ready-for-transport to get unpropagation
                this.db.scheduleTasks(id, stop);
                
                if (hostname == null) {
                    logger.error(Lager.id(id) + "scheduler received " +
                            "slot-reserved notification without a hostname");
                    return;
                } else {
                    resource.newHostname(hostname);
                }

                if (start == null) {
                    logger.error(Lager.id(id) + "scheduler received " +
                            "slot-reserved notification without a start time");
                    return;
                } else {
                    resource.newStartTime(start);
                }

                if (stop == null) {
                    // this is actually impossible to reach, leaving in
                    // for future developers
                    logger.error(Lager.id(id) + "scheduler received " +
                            "slot-reserved notification without a stop time");
                    return;
                } else {
                    resource.newStopTime(stop);
                }
            }

            resource.setOpsEnabled(true);
            try {
                resource.activate();
                this.sweeper.scheduleSweeper();
            } catch (ManageException e) {
                logger.error("", e);
            }
        }


        /* Once transport-readying is hit for any reason, no turning back */
        else if (state == WorkspaceConstants.STATE_READYING_FOR_TRANSPORT) {
            try {
                resource = this.home.find(id);
            } catch (DoesNotExistException e) {
                logger.error("scheduler received state notification (" +
                        this.dataConvert.stateName(state) + ") about " +
                        Lager.id(id) + ", but it seems to be gone now", e);
                return;
            }

            // find will not return null
            resource.setOpsEnabled(false);
            try {
                this.db.markShutdown(id);
            } catch (WorkspaceDatabaseException e) {
                logger.error("", e);
            }
        }
    }

    private InstanceResource fetchResource(int id, int state)
                    throws ManageException {

        InstanceResource resource = null;
        try {
            // find will not return null
            resource = this.home.find(id);
            return resource;
            
        } catch (DoesNotExistException e) {

            String msg = "scheduler received state notification (" +
                this.dataConvert.stateName(state) + ") about " + Lager.id(id)
                + ", but it seems to be gone now.";

            if (this.creationPending.isPending(id)) {

                // object creation pending race condition, see CreationPending
                // class comments

                logger.debug("WorkspaceHome can not find " + Lager.id(id) +
                    " and this is a object create pending race condition");

                // Over 10 seconds on an otherwise unloaded system would
                // mean an *extremely* slow processor or perhaps 100k's
                // of concurrent creations.
                // "We'll never see either of these things"
                short count = 0;
                while (count < 50 && resource == null) {
                    count += 1;
                    try {
                        Thread.sleep(200);
                        if (!this.creationPending.isPending(id)) {
                            resource = this.home.find(id);
                        }
                    } catch (InterruptedException e2) {
                        logger.error(e2.getMessage());
                    } catch (DoesNotExistException e2) {
                        logger.debug(e2.getMessage());
                    }
                }

                try {
                    resource = this.home.find(id);
                } catch (DoesNotExistException e2) {
                    logger.debug(e2.getMessage());
                }

                if (resource == null) {
                    msg = msg + " This was because of a known race " +
                            "condition that the service cases for.  The " +
                            "service re-tried to find this workspace " +
                            "many times but failed.";
                    if (logger.isDebugEnabled()) {
                        msg = msg + " [[DEBUG stack trace is of first " +
                                "lookup exception]]";
                        logger.error(msg, e);
                    } else {
                        logger.error(msg);
                    }
                }

                return resource;
            }

            logger.debug("WorkspaceHome can not find " + Lager.id(id) +
                    " and this is NOT an object creation pending race " +
                    "condition.  Looking up " + Lager.id(id) + " again " +
                    "in case there was just a check-then-check issue");

            try {
                // find will not return null
                resource = this.home.find(id);
                return resource;
            } catch (DoesNotExistException e2) {

                msg = msg + " This is NOT because of a known race" +
                    " condition. Looked up " + Lager.id(id) +
                    " one more time in case there was just a" +
                    " check-then-check issue, but that failed.  It" +
                    " is very likely this is a true inconsistency" +
                    " situation.";

                if (logger.isDebugEnabled()) {
                    msg = msg + " [[DEBUG stack trace is of first " +
                            "lookup exception]]";
                    logger.error(msg, e);
                } else {
                    logger.error(msg);
                }
                return null;
            }
        }
    }

    public void recover(int recovered) {
        if (recovered > 0) {
            this.sweeper.scheduleSweeper();
        }
    }

    private void remove(int vmid) throws ManageException {

        if (lager.traceLog) {
            logger.trace("remove(): reservation " + Lager.id(vmid));
        }

        this.db.backOutTasks(vmid);
        this.slotManager.releaseSpace(vmid);
        this.db.deleteNodeRequest(vmid);
    }

    protected void markShutdown(int id) throws WorkspaceDatabaseException {
        
    }

    protected int anyLeft() throws WorkspaceDatabaseException {
        return this.db.anyLeft();
    }

    protected int[] findWorkspacesToShutdown()
                            throws WorkspaceDatabaseException {
        return this.db.findWorkspacesToShutdown();
    }
}
