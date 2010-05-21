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

package org.globus.workspace.service.impls;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.workspace.Counter;
import org.globus.workspace.Lager;
import org.globus.workspace.WorkspaceConstants;
import org.globus.workspace.WorkspaceException;
import org.globus.workspace.accounting.AccountingEventAdapter;
import org.globus.workspace.persistence.DataConvert;
import org.globus.workspace.persistence.PersistenceAdapter;
import org.globus.workspace.service.InstanceResource;
import org.globus.workspace.service.binding.BindingAdapter;
import org.globus.workspace.service.binding.GlobalPolicies;
import org.globus.workspace.service.binding.authorization.CreationAuthorizationCallout;
import org.globus.workspace.service.binding.authorization.Decision;
import org.globus.workspace.service.binding.authorization.PostTaskAuthorization;
import org.globus.workspace.service.binding.vm.CustomizationNeed;
import org.globus.workspace.service.binding.vm.VirtualMachine;

import org.nimbustools.api.repr.ShutdownTasks;
import org.nimbustools.api.services.rm.AuthorizationException;
import org.nimbustools.api.services.rm.DestructionCallback;
import org.nimbustools.api.services.rm.DoesNotExistException;
import org.nimbustools.api.services.rm.ManageException;
import org.nimbustools.api.services.rm.OperationDisabledException;
import org.nimbustools.api.services.rm.StateChangeCallback;
import org.nimbustools.api.services.rm.CreationException;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

/**
 * More organization (and docs) coming.  This class is too tightly coupled.
 */
public abstract class InstanceResourceImpl implements InstanceResource {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
        LogFactory.getLog(InstanceResourceImpl.class.getName());

    private static DateFormat localFormat = DateFormat.getDateTimeInstance();


    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected final PersistenceAdapter persistence;
    protected final BindingAdapter binding;
    protected final GlobalPolicies globals;
    protected final DataConvert dataConvert;
    protected final Lager lager;
    protected final Counter pendingNotifications = new Counter(0, null);
    protected final List destructionListeners = new LinkedList();
    protected final List stateListeners = new LinkedList();
    
    // optionally set
    protected AccountingEventAdapter accounting;
    protected CreationAuthorizationCallout authzCallout;
    
    protected int id = -1;
    protected String name;
    protected VirtualMachine vm;

    protected String ensembleId;

    protected String groupId;
    protected boolean lastInGroup;
    protected int groupSize = 1;
    protected boolean partOfGroupRequest;
    protected int launchIndex;

    protected Calendar terminationTime;
    protected Calendar startTime;
    protected volatile int state = WorkspaceConstants.STATE_INVALID;
    protected volatile int targetState = WorkspaceConstants.STATE_INVALID;
    protected boolean opsEnabled;
    protected boolean vmmAccessOK = true;
    protected volatile Throwable throwableForState;

    // currently, mgmt policy limited to one entity, the create() caller
    protected String creatorID;

    private boolean removeTriggered;


    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    protected InstanceResourceImpl(PersistenceAdapter persistenceImpl,
                                   BindingAdapter bindingImpl,
                                   GlobalPolicies globalsImpl,
                                   DataConvert dataConvertImpl,
                                   Lager lagerImpl) {

        if (lagerImpl == null) {
            throw new IllegalArgumentException("lagerImpl may not be null");
        }
        this.lager = lagerImpl;

        if (persistenceImpl == null) {
            throw new IllegalArgumentException(
                    "persistenceImpl may not be null");
        }
        this.persistence = persistenceImpl;        

        if (bindingImpl == null) {
            throw new IllegalArgumentException("bindingImpl may not be null");
        }
        this.binding = bindingImpl;

        if (globalsImpl == null) {
            throw new IllegalArgumentException("globalsImpl may not be null");
        }
        this.globals = globalsImpl;

        if (dataConvertImpl == null) {
            throw new IllegalArgumentException("da may not be null");
        }
        this.dataConvert = dataConvertImpl;
    }


    // -------------------------------------------------------------------------
    // OPTIONAL MODULES SETTERS
    // -------------------------------------------------------------------------

    public void setAuthzCallout(CreationAuthorizationCallout callout) {
        this.authzCallout = callout;
    }

    public void setAccountingEventAdapter(AccountingEventAdapter events) {
        this.accounting = events;
    }


    // -------------------------------------------------------------------------
    // CREATION
    // -------------------------------------------------------------------------

    public Calendar getStartTime() {
        return this.startTime;
    }

    public void setStartTime(Calendar start) {
        this.startTime = start;
    }

    /**
     * Only called when this resource is created.
     *
     * @param id id#
     * @param vm instantiation details
     * @param startTime start, can be null
     * @param termTime resource destruction time, can be null which mean either
     *                 "never" or "no setting" (while using best-effort sched)
     * @param node assigned node, can be null
     * @throws CreationException problem
     */
    public void populate(int id,
                         VirtualMachine vm,
                         Calendar startTime,
                         Calendar termTime,
                         String node) throws CreationException {

        if (vm == null) {
            throw new CreationException("null vm");
        }

        if (vm.getDeployment() == null) {
            throw new CreationException("null deployment information");
        }

        this.id = id;
        this.vm = vm;

        // could be null if best effort, infinite for timebeing
        this.terminationTime = termTime;

        // could be null if best effort
        this.startTime = startTime;

        this.name = vm.getName();

        this.setInitialState(WorkspaceConstants.STATE_FIRST_LEGAL, null);

        this.setInitialTargetState(
                this.vm.getDeployment().getRequestedState());

        this.vm.setNode(node);

        if (lager.traceLog) {

            String msg = "populating " + Lager.id(this.id) + ", resource " +
                    "termination time = ";
            if (this.terminationTime != null) {
                msg += localFormat.format(this.terminationTime.getTime());
            } else {
                msg += "not set";
            }
            logger.trace(msg);
        }
    }


    // -------------------------------------------------------------------------
    // GENERAL INFORMATION
    // -------------------------------------------------------------------------

    public int getID() {
        return this.id;
    }

    public void setID(int idnum) {
        this.id = idnum;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String resourceName) {
        this.name = resourceName;
    }

    public String getEnsembleId() {
        return this.ensembleId;
    }

    public void setEnsembleId(String ensembleId) {
        this.ensembleId = ensembleId;
    }

    public String getGroupId() {
        return this.groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public boolean isLastInGroup() {
        return this.lastInGroup;
    }

    public void setLastInGroup(boolean lastInGroup) {
        this.lastInGroup = lastInGroup;
    }

    public int getGroupSize() {
        return this.groupSize;
    }

    public void setGroupSize(int groupSize) {
        this.groupSize = groupSize;
    }

    public String getCreatorID() {
        return this.creatorID;
    }

    public void setCreatorID(String ID) {
        this.creatorID = ID;
    }

    public VirtualMachine getVM() {
        return this.vm;
    }

    public boolean isPropagateRequired() {
        return this.vm.isPropagateRequired();
    }

    public boolean isUnPropagateRequired() {
        return this.vm.isUnPropagateRequired();
    }

    public boolean isPropagateStartOK() {
        return this.vm.isPropagateStartOK();
    }

    public boolean isVMMaccessOK() {
        return this.vmmAccessOK;
    }

    public void setVMMaccessOK(boolean accessOK) {
        this.vmmAccessOK = accessOK;
        try {
            this.persistence.setVMMaccessOK(this.id, accessOK);
        } catch (ManageException e) {
            logger.error("",e);
        }
    }

    public void setInitialVMMaccessOK(boolean accessOK) {
        this.vmmAccessOK = accessOK;
    }

    public synchronized void newCustomizationNeed(CustomizationNeed need) {
        if (this.vm == null) {
            throw new IllegalStateException("vm is null");
        }
        this.vm.addCustomizationNeed(need);
        try {
            this.persistence.addCustomizationNeed(this.id, need);
        } catch (ManageException e) {
            logger.error("", e);  // TODO
        }
    }

    public synchronized void newHostname(String hostname) {
        if (this.vm == null) {
            logger.fatal(Lager.id(this.id) +
                         ": could not set hostname, no vm");
            return;
        }
        this.vm.setNode(hostname);
        try {
            this.persistence.setHostname(this.id, hostname);
        } catch (ManageException e) {
            logger.error("", e);
        }
    }

    public synchronized void newStartTime(Calendar startTime) {
        this.setStartTime(startTime);
        try {
            this.persistence.setStartTime(this.id, startTime);
        } catch (ManageException e) {
            logger.error("", e);
        }
    }

    public synchronized void newStopTime(Calendar stopTime) {

        // this only in fact sets term time, scheduler is the only thing
        // that currently needs stop time, but todo: stop should be stored
        // in workspace resource for future querying from client (since
        // with best effort scheduler it is not always known at create time
        // and therefore returned to client)

        try {
            stopTime.add(Calendar.SECOND,
                         this.globals.getTerminationOffsetSeconds());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return;
        }

        this.terminationTime = stopTime;
        try {
            this.persistence.setTerminationTime(this.id, stopTime);
        } catch (ManageException e) {
            logger.error("", e);
        }
    }

    public synchronized void newNetwork(String network) {
        if (this.vm == null) {
            logger.fatal(Lager.id(this.id) +
                         ": could not set network, no vm");
            return;
        }
        this.vm.setNetwork(network);
        try {
            this.persistence.setNetwork(this.id, network);
        } catch (ManageException e) {
            logger.error("", e);
        }
    }

    public boolean isPartOfGroupRequest() {
        return this.partOfGroupRequest;
    }

    public void setPartOfGroupRequest(boolean isInGroupRequest) {
        this.partOfGroupRequest = isInGroupRequest;
    }

    public int getLaunchIndex() {
        return this.launchIndex;
    }

    public void setLaunchIndex(int launchIndex) {
        this.launchIndex = launchIndex;
    }

    // -------------------------------------------------------------------------
    // REMOTE OPERATION POLICY
    // -------------------------------------------------------------------------

    /**
     * @return true if client is permitted to invoke WS operations
     *         other than destroy
     */
    public boolean isOpsEnabled() {
        return this.opsEnabled;
    }

    /**
     * For creating the object in the first place; setOpsEnabled calls
     * persistenceAdapter.setOpsEnabled() - but entry for id does not
     * exist yet when setInitialOpsEnabled() is needed.
     *
     * @see PersistenceAdapter#load(int, org.globus.workspace.service.InstanceResource) )
     * @param enabled true if enabled
     */
    public void setInitialOpsEnabled(boolean enabled) {
        this.opsEnabled = enabled;
    }

    /**
     * For regular changes during the life of the workspace.
     *
     * @param enabled true if enabled
     */
    public synchronized void setOpsEnabled(boolean enabled) {
        
        if (enabled != this.opsEnabled) {
            this.opsEnabled = enabled;
            try {
                this.persistence.setOpsEnabled(this.id, enabled);
            } catch (ManageException e) {
                logger.error("",e);
            }

            if (lager.eventLog) {
                String msg = "disabled";
                if (enabled) {
                    msg = "enabled";
                }
                logger.info(Lager.ev(this.id) + " WS-operations " + msg);
            }
        }
    }


    // -------------------------------------------------------------------------
    // REMOTE CLIENT MANIPULATION
    // -------------------------------------------------------------------------

    public void start() throws ManageException, OperationDisabledException {
        if (this.isOpsEnabled()) {
            start(WorkspaceConstants.STATE_STARTED);
        } else {
            throw new OperationDisabledException(
                                    "start is currently disabled");
        }
    }

    private void start(int target) throws ManageException {

        if (lager.traceLog) {
            logger.trace("_start(): " + Lager.id(this.id) +
                    "current state is " +
                    this.dataConvert.stateName(this.state) +
                    ", current target is " +
                    this.dataConvert.stateName(this.targetState) +
                    ", target is " +
                    this.dataConvert.stateName(target));
        }

        this.setTargetState(target);
    }

    public void shutdown(ShutdownTasks tasks)

            throws ManageException, OperationDisabledException {

        if (this.isOpsEnabled()) {
            this.handlePostTasks(tasks, false);
            this._shutdown(WorkspaceConstants.STATE_PROPAGATED);
        } else {
            throw new OperationDisabledException(
                    "Shutdown is currently disabled");
        }
    }

    public void shutdownSave(ShutdownTasks tasks)

            throws ManageException, OperationDisabledException {

        if (this.isOpsEnabled()) {
            this.handlePostTasks(tasks, true);
            this._shutdown(WorkspaceConstants.STATE_READY_FOR_TRANSPORT);
        } else {
            throw new OperationDisabledException(
                    "ReadyForTransport (shutdown-save) is currently disabled");
        }
    }

    public void pause(ShutdownTasks tasks)

            throws ManageException, OperationDisabledException {

        if (this.isOpsEnabled()) {
            this.handlePostTasks(tasks, false);
            this._shutdown(WorkspaceConstants.STATE_PAUSED);
        } else {
            throw new OperationDisabledException(
                    "Pause is currently disabled");
        }
    }

    public void serialize(ShutdownTasks tasks)

            throws ManageException, OperationDisabledException {

        if (this.isOpsEnabled()) {
            this.handlePostTasks(tasks, false);
            this._shutdown(WorkspaceConstants.STATE_SERIALIZED);
        } else {
            throw new OperationDisabledException(
                    "Serialize is currently disabled");
        }
    }

    public void reboot(ShutdownTasks tasks)

            throws ManageException, OperationDisabledException {

        if (this.isOpsEnabled()) {
            this.handlePostTasks(tasks, false);
            this._shutdown(WorkspaceConstants.STATE_REBOOT);
        } else {
            throw new OperationDisabledException(
                    "Reboot is currently disabled");
        }
    }

    private void handlePostTasks(ShutdownTasks tasks,
                                 boolean isReadyForTransport)
            throws ManageException {

        if (tasks == null) {
            return; // *** EARLY RETURN ***
        }

        URI target = tasks.getBaseFileUnpropagationTarget();
        if (target == null) {
            return; // *** EARLY RETURN ***
        }

        // technically possible but only someone else's client implementation
        // would allow this to happen
        if (!isReadyForTransport) {
            final String err = "Post-shutdown tasks are only compatible " +
                    "with a ReadyForTransport request.";
            throw new ManageException(err);
        }

        if (this.authzCallout != null
                && this.authzCallout instanceof PostTaskAuthorization) {

            // shortcut: currently we know owner is the caller at this point
            final String dnToAuthorize = this.creatorID;

            final Integer decision;
            String newTargetName;
            try {
                decision = ((PostTaskAuthorization)this.authzCallout).
                    isRootPartitionUnpropTargetPermitted(target, dnToAuthorize);
            } catch (AuthorizationException e) {
                throw new ManageException(e.getMessage(), e);
            }

            if (!Decision.PERMIT.equals(decision)) {
                throw new ManageException(
                        "request denied, no message for client");
            }
        }

        final String trueTarget;
        if (tasks.isAppendID()) {
            
            trueTarget = target.toASCIIString() + "-" + this.id;

            // check new uri syntax:
            try {
                new URI(trueTarget);
            } catch (URISyntaxException e) {
                throw new ManageException(e.getMessage(), e);
            }
            
        } else {
            
            trueTarget = target.toASCIIString();
        }

        this.vm.overrideRootUnpropTarget(trueTarget, logger);

        if (!this.isUnPropagateRequired()) {
            this.vm.setUnPropagateRequired(true);
        }
        
        this.persistence.setRootUnpropTarget(this.id, trueTarget);
    }

    private void _shutdown(int target) throws ManageException {

        if (lager.traceLog) {
            logger.trace("shutdown(): " + Lager.id(this.id) + ", " +
                    "target state = " + target);
        }

        try {
            this.setTargetState(target);
        } catch (Exception e) {
            final String err = "problem shutting down " + Lager.id(this.id) +
                        ": " + e.getMessage();
            logger.error(err, e);
            // setting to corrupted and/or deciding to destroy depends on the
            // situation and has already happened via the state machine
            throw new ManageException(err, e);
        }
    }


    // -------------------------------------------------------------------------
    // LISTENERS
    // -------------------------------------------------------------------------

    public void registerStateChangeListener(StateChangeCallback listener) {
        if (listener == null) {
            logger.warn("Something sent null state change listener, ignoring");
            return; // *** EARLY RETURN ***
        }

        // re-registrations expected after a container crash
        synchronized(this.stateListeners) {
            this.stateListeners.add(listener);
        }
    }

    protected void expungeStateListeners() {
        this.stateListeners.clear();
    }
    
    public void registerDestructionListener(DestructionCallback listener) {
        if (listener == null) {
            logger.warn("Something sent null destruction listener, ignoring");
            return; // *** EARLY RETURN ***
        }

        // re-registrations expected after a container crash
        synchronized(this.destructionListeners) {
            this.destructionListeners.add(listener);
        }
    }

    protected void resourceDestroyed() {

        this.expungeStateListeners();
        
        // todo: assuming listeners will quickly return from the notification,
        // should not assume that about all future messaging layers, so in the
        // future launch a separate thread to then make the callbacks (there
        // are many articles about this to consult)
        synchronized(this.destructionListeners) {

            for (int i = 0; i < this.destructionListeners.size(); i++) {

                try {

                    final DestructionCallback dc =
                        (DestructionCallback) this.destructionListeners.get(i);

                    if (dc != null) {
                        dc.destroyed();
                    }

                } catch (Throwable t) {
                    final String err = "Problem with asynchronous " +
                            "destruction notification: " + t.getMessage();
                    logger.error(err, t);
                }

            }

            // never happens again
            this.destructionListeners.clear();
        }
    }


    // -------------------------------------------------------------------------
    // REMOVAL
    // -------------------------------------------------------------------------

    /**
     * Called when termination time passes or if client invoked destroy
     * operation.
     *
     * This invokes all it can to remove all traces of the workspace,
     * including cancelling any current transfers or calling shutdown +
     * trash on the backend for example.
     *
     * It currently blocks until finished.
     *
     * Don't call unless you are managing the instance cache (or not using
     * one, perhaps).
     *
     * @throws ManageException problem with removal
     */
    public synchronized void remove() throws ManageException {

        if (this.removeTriggered) {
            return; // *** EARLY RETURN ***
        } else {
            this.removeTriggered = true;
        }

        if (this.lager.eventLog || this.lager.traceLog) {
            if (this.lager.eventLog) {
                logger.info(Lager.ev(this.id) + "destroy begins");
            } else if (this.lager.traceLog) {
                logger.trace(Lager.id(this.id) + "destroy begins");
            }
        }

        this.setTargetState(WorkspaceConstants.STATE_DESTROYING);

        if (this.lager.eventLog) {
            logger.info(Lager.ev(this.id) +
                        "destroyed ('" + this.name + "')");
        } else if (this.lager.traceLog) {
            logger.trace(Lager.id(this.id) +
                         " destroyed ('" + this.name + "')");
        }

        // inform any destruction listeners:
        this.resourceDestroyed();
    }

    protected void do_remove() {

        // scheduler already notified by doStateChange()

        logger.debug("removing " + Lager.id(this.id));
        
        // notify accounting

        if (this.accounting == null) {
            if (lager.accounting) {
                logger.debug("accounting not available, " +
                        "no stop event generated");
            }
        } else {

            final String network;
            final String resource;
            if (this.vm != null) {
                network = this.vm.getNetwork();
                resource = this.vm.getNode();
            } else {
                network = null;
                resource = null;
            }

            if (this.startTime == null) {
                if (lager.accounting) {
                    logger.debug("no start time available (will happen with " +
                        "best effort scheduler with destruction coming " +
                        "before any resource assignment");
                }

                this.accounting.destroy(this.id,
                                        this.getCreatorID(),
                                        0L);
            } else {
                final long runningTimeMS =
                        Calendar.getInstance().getTimeInMillis() -
                                   this.startTime.getTimeInMillis();

                // convert milliseconds to minutes, take ceiling
                long runningTime = runningTimeMS / 60000L;
                if (runningTimeMS % 60000L > 0L) {
                    runningTime += 1L;
                }

                this.accounting.destroy(this.id,
                                        this.getCreatorID(),
                                        runningTime);
            }
        }

        try {
            logger.debug("backing out allocations for " + Lager.id(this.id));
            this.binding.backOutAllocations(this.vm);
        } catch (WorkspaceException e) {
            // candidate for admin log/trigger of severe issues
            final String err = "error retiring allocations, " +
                                                    Lager.id(this.id);
            logger.error(err, e);
        }

        try {
            this.persistence.remove(this.id, this);
        } catch (Throwable t) {
            // nothing more we can really do about this
            // candidate for admin log/trigger of severe issues
            logger.fatal("error removing " + Lager.id(this.id)
                            + " from persistence layer: " + t.getMessage(), t);
        }

        // After remove is done, home notifies subscribers of termination
    }


    public void load(String key) throws ManageException,
                                        DoesNotExistException {

        final int keyInt;
        try {
            keyInt = Integer.parseInt(key);
        } catch (Throwable t) {
            final String err = "invalid key: " + key;
            throw new ManageException(err, t);
        }

        this.id = keyInt;

        if (lager.traceLog) {
            logger.trace("load(): " + Lager.id(this.id));
        }

        this.persistence.load(keyInt, this);
    }

    public Calendar getTerminationTime() {
        return this.terminationTime;
    }

    public void setTerminationTime(Calendar time) {
        this.terminationTime = time;
    }
}
