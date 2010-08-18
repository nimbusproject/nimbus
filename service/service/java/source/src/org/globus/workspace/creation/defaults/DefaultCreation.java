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

package org.globus.workspace.creation.defaults;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.globus.workspace.ErrorUtil;
import org.globus.workspace.Lager;
import org.globus.workspace.LockManager;
import org.globus.workspace.ProgrammingError;
import org.globus.workspace.WorkspaceConstants;
import org.globus.workspace.WorkspaceUtil;
import org.globus.workspace.network.AssociationAdapter;
import org.globus.workspace.accounting.AccountingEventAdapter;
import org.globus.workspace.creation.Creation;
import org.globus.workspace.persistence.DataConvert;
import org.globus.workspace.persistence.PersistenceAdapter;
import org.globus.workspace.persistence.WorkspaceDatabaseException;
import org.globus.workspace.scheduler.IdHostnameTuple;
import org.globus.workspace.scheduler.Reservation;
import org.globus.workspace.scheduler.Scheduler;
import org.globus.workspace.scheduler.Event;
import org.globus.workspace.service.InstanceResource;
import org.globus.workspace.service.WorkspaceHome;
import org.globus.workspace.service.WorkspaceGroupHome;
import org.globus.workspace.service.WorkspaceCoschedHome;
import org.globus.workspace.service.binding.Authorize;
import org.globus.workspace.service.binding.BindingAdapter;
import org.globus.workspace.service.binding.GlobalPolicies;
import org.globus.workspace.service.binding.vm.VirtualMachine;
import org.globus.workspace.service.binding.vm.VirtualMachineDeployment;
import org.globus.workspace.service.binding.vm.CustomizationNeed;

import org.nimbustools.api._repr._CreateResult;
import org.nimbustools.api._repr._Advertised;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.CreateRequest;
import org.nimbustools.api.repr.CreateResult;
import org.nimbustools.api.repr.ReprFactory;
import org.nimbustools.api.repr.CannotTranslateException;
import org.nimbustools.api.repr.Advertised;
import org.nimbustools.api.repr.ctx.Context;
import org.nimbustools.api.repr.vm.VM;
import org.nimbustools.api.repr.vm.ResourceAllocation;
import org.nimbustools.api.repr.vm.NIC;
import org.nimbustools.api.services.rm.AuthorizationException;
import org.nimbustools.api.services.rm.BasicLegality;
import org.nimbustools.api.services.rm.CoSchedulingException;
import org.nimbustools.api.services.rm.CreationException;
import org.nimbustools.api.services.rm.MetadataException;
import org.nimbustools.api.services.rm.ResourceRequestDeniedException;
import org.nimbustools.api.services.rm.SchedulingException;
import org.nimbustools.api.services.rm.ManageException;

import org.safehaus.uuid.UUIDGenerator;

import java.util.Calendar;
import java.text.DateFormat;

import commonj.timers.TimerManager;

/**
 * Initial intake, resource requests, resource grants, and setup of new
 * instance objects. 
 *
 * Includes rollbacks (looking into dedicated transaction technology for the
 * future).
 */
public class DefaultCreation implements Creation {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(DefaultCreation.class.getName());
    

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected final UUIDGenerator uuidGen = UUIDGenerator.getInstance();
    protected final LockManager lockManager;
    protected final BasicLegality legals;
    protected final BindingAdapter binding;
    protected final AssociationAdapter networks;
    protected final Authorize authorize;
    protected final Scheduler scheduler;
    protected final ReprFactory repr;
    protected final GlobalPolicies globals;
    protected final WorkspaceHome whome;
    protected final WorkspaceCoschedHome coschedHome;
    protected final WorkspaceGroupHome groupHome;
    protected final PersistenceAdapter persistence;
    protected final DataConvert dataConvert;
    protected final TimerManager timerManager;
    protected final Lager lager;
    protected final DateFormat localFormat = DateFormat.getDateTimeInstance();

    protected AccountingEventAdapter accounting;


    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    public DefaultCreation(LockManager lockManagerImpl,
                           BasicLegality legalsImpl,
                           BindingAdapter bindingImpl,
                           AssociationAdapter networkImpl,
                           Authorize authorizeImpl,
                           Scheduler schedulerImpl,
                           ReprFactory reprFactory,
                           GlobalPolicies globalPolicies,
                           WorkspaceHome workspaceHome,
                           WorkspaceGroupHome groupHomeImpl,
                           WorkspaceCoschedHome coschedHomeImpl,
                           PersistenceAdapter persistenceAdapter,
                           DataConvert dataConvertImpl,
                           TimerManager timerManagerImpl,
                           Lager lagerImpl) {

        if (lockManagerImpl == null) {
            throw new IllegalArgumentException("lockManager may not be null");
        }
        this.lockManager = lockManagerImpl;

        if (legalsImpl == null) {
            throw new IllegalArgumentException("legals may not be null");
        }
        this.legals = legalsImpl;

        if (bindingImpl == null) {
            throw new IllegalArgumentException("binding may not be null");
        }
        this.binding = bindingImpl;

        if (networkImpl == null) {
            throw new IllegalArgumentException("networkImpl may not be null");
        }
        this.networks = networkImpl;
        
        if (authorizeImpl == null) {
            throw new IllegalArgumentException("authorize may not be null");
        }
        this.authorize = authorizeImpl;

        if (schedulerImpl == null) {
            throw new IllegalArgumentException("scheduler may not be null");
        }
        this.scheduler = schedulerImpl;

        if (reprFactory == null) {
            throw new IllegalArgumentException("reprFactory may not be null");
        }
        this.repr = reprFactory;

        if (globalPolicies == null) {
            throw new IllegalArgumentException("globalPolicies may not be null");
        }
        this.globals = globalPolicies;

        if (workspaceHome == null) {
            throw new IllegalArgumentException("workspaceHome may not be null");
        }
        this.whome = workspaceHome;

        if (groupHomeImpl == null) {
            throw new IllegalArgumentException("groupHomeImpl may not be null");
        }
        this.groupHome = groupHomeImpl;

        if (coschedHomeImpl == null) {
            throw new IllegalArgumentException("coschedHomeImpl may not be null");
        }
        this.coschedHome = coschedHomeImpl;
        
        if (persistenceAdapter == null) {
            throw new IllegalArgumentException("persistenceAdapter may not be null");
        }
        this.persistence = persistenceAdapter;
        
        if (dataConvertImpl == null) {
            throw new IllegalArgumentException("dataConvertImpl may not be null");
        }
        this.dataConvert = dataConvertImpl;

        if (timerManagerImpl == null) {
            throw new IllegalArgumentException("timerManagerImpl may not be null");
        }
        this.timerManager = timerManagerImpl;

        if (lagerImpl == null) {
            throw new IllegalArgumentException("lagerImpl may not be null");
        }
        this.lager = lagerImpl;
    }


    // -------------------------------------------------------------------------
    // OPTIONAL SETs
    // -------------------------------------------------------------------------

    public void setAccountingEventAdapter(AccountingEventAdapter accEvents) {
        this.accounting = accEvents;
    }

    
    // -------------------------------------------------------------------------
    // implements Creation
    // -------------------------------------------------------------------------

    public Advertised getAdvertised() {

        final _Advertised adv = this.repr._newAdvertised();
        adv.setCpuArchitectureName(
                this.globals.getCpuArchitectureName());
        adv.setDefaultRunningTimeSeconds(
                this.globals.getDefaultRunningTimeSeconds());
        adv.setMaxGroupSize(
                this.globals.getMaximumGroupSize());
        adv.setMaximumRunningTimeSeconds(
                this.globals.getMaximumRunningTimeSeconds());

        adv.setVmm(this.globals.getVmm());
        adv.setVmmVersions(this.globals.getVmmVersions());
        
        try {
            adv.setNetworkNames(this.networks.getAssociationNames());
        } catch (ManageException e) {
            logger.fatal(e.getMessage(), e);
            adv.setNetworkNames(null);
        }

        if (this.accounting != null) {
            adv.setChargeGranularity(this.accounting.getChargeGranularity());
        } else {
            adv.setChargeGranularity(-1);
        }

        return adv;
    }

    public CreateResult create(CreateRequest req, Caller caller)

            throws CoSchedulingException,
                   CreationException,
                   MetadataException,
                   ResourceRequestDeniedException,
                   SchedulingException {

        if (caller == null) {
            throw new CreationException("no caller");
        }

        if (this.lager.eventLog) {
            logger.info(Lager.ev(-1) + "Create request for " + this.getType(req) +
                                    " from '" + caller.getIdentity() + "'");
        }

        this.legals.checkCreateRequest(req);

        return this.create1(req, caller);
    }

    protected String getType(CreateRequest req) {
        if (req == null) {
            return "UNKNOWN";
        }
        final ResourceAllocation requestedRA = req.getRequestedRA();
        if (requestedRA == null) {
            return "UNKNOWN";
        }

        final String suffix;
        if (req.isCoScheduleMember()) {
            suffix = " (coscheduled)";
        } else {
            suffix = "";
        }

        final int numNodes = req.getRequestedRA().getNodeNumber();
        if (numNodes == 1) {
            return "instance" + suffix;
        } else {
            return "group" + suffix;
        }
    }

    
    // -------------------------------------------------------------------------
    // CREATE I
    // -------------------------------------------------------------------------

    protected CreateResult create1(CreateRequest req, Caller caller)

            throws CoSchedulingException,
                   CreationException,
                   MetadataException,
                   ResourceRequestDeniedException,
                   SchedulingException {

        final VirtualMachine[] bound = this.binding.processRequest(req);
        if (bound == null || bound.length == 0) {
            throw new CreationException("no binding result but no binding " +
                    "error: illegal binding implementation");
        }

        final String creatorID = caller.getIdentity();
        if (creatorID == null || creatorID.trim().length() == 0) {
            throw new CreationException("Cannot determine identity");
        }

        final Context context = req.getContext();
        final String groupID = this.getGroupID(creatorID, bound.length);
        final String coschedID = this.getCoschedID(req, creatorID);

        // From this point forward an error requires backOutAllocations
        try {
            return this.create2(bound, caller, context, groupID, coschedID);
        } catch (CoSchedulingException e) {
            this.backoutBound(bound);
            throw e;
        } catch (CreationException e) {
            this.backoutBound(bound);
            throw e;
        } catch (MetadataException e) {
            this.backoutBound(bound);
            throw e;
        } catch (ResourceRequestDeniedException e) {
            this.backoutBound(bound);
            throw e;
        } catch (SchedulingException e) {
            this.backoutBound(bound);
            throw e;                    
        } catch (Throwable t) {
            this.backoutBound(bound);
            throw new CreationException("Unknown problem occured: " +
                    "'" + ErrorUtil.excString(t) + "'", t);
        }
    }

    protected String getGroupID(String creatorID, int number)
            throws CreationException {

        if (number < 2) {
            return null;
        }

        try {
            return this.groupHome.newGroup(creatorID).getID();
        } catch (ManageException e) {
            throw new CreationException(e.getMessage(), e);
        }
    }

    protected String getCoschedID(CreateRequest req, String creatorID)
            throws CreationException {

        if (!req.isCoScheduleMember()) {
            return null; // *** EARLY RETURN ***
        }

        final String coschedID = req.getCoScheduleID();
        if (coschedID != null) {
            return coschedID; // *** EARLY RETURN ***
        }

        /*
           This is a cosched member but there is no ID string -- this triggers
           creation of a new cosched resource.
         */
        try {
            return this.coschedHome.newCosched(creatorID).getID();
        } catch (ManageException e) {
            throw new CreationException(e.getMessage(), e);
        }

    }

    protected void backoutBound(VirtualMachine[] bound) {
        try {
            this.binding.backOutAllocations(bound);
        } catch (Throwable t) {
            final String err =
                    "Error during bindings backout: " + t.getMessage();
            logger.error(err, t);
        }
    }
    
    
    // -------------------------------------------------------------------------
    // CREATE II
    // -------------------------------------------------------------------------

    // create #2 (wrapped, backOutAllocations required for failures)
    protected CreateResult create2(VirtualMachine[] bindings,
                                   Caller caller,
                                   Context context,
                                   String groupID,
                                   String coschedID)

            throws AuthorizationException,
                   CoSchedulingException,
                   CreationException,
                   MetadataException,
                   ResourceRequestDeniedException,
                   SchedulingException {

        // msg for future extenders
        if (bindings == null) {
            throw new IllegalArgumentException("bindings may not be null");
        }
        if (caller == null) {
            throw new IllegalArgumentException("caller may not be null");
        }

        // TODO: Would like to be able to get defaults (especially for running
        //       time request) on a per-group basis when using the group authz
        //       plugin.
        if (!caller.isSuperUser()) {
            this.authorize.authz(bindings,
                                 caller.getIdentity(),
                                 caller.getSubject());
        }

        final Reservation res = this.scheduleImpl(bindings[0],
                                                  bindings.length,
                                                  groupID,
                                                  coschedID,
                                                  caller.getIdentity());

        if (res == null) {
            throw new SchedulingException("reservation is missing, illegal " +
                    "scheduling implementation");
        }
        
        final int[] ids = res.getIds();

        // From this point forward an error requires attempt to
        // remove from scheduler

        try {

            if (bindings.length != ids.length) {
                throw new ProgrammingError(
                        "different number of IDs and requests?");
            }

            // the binding process happened before the scheduler is
            // invoked, add anything ID related now
            for (int i = 0; i < bindings.length; i++) {
                bindings[i].setID(ids[i]);
                if (bindings.length > 1) {
                    bindings[i].addUnpropTargetSuffixes();
                }
            }

            return create3(res,
                           bindings,
                           caller.getIdentity(),
                           context,
                           groupID,
                           coschedID);

        } catch (CoSchedulingException e) {
            this.backoutScheduling(ids, groupID);
            throw e;
        } catch (CreationException e) {
            this.backoutScheduling(ids, groupID);
            throw e;
        } catch (MetadataException e) {
            this.backoutScheduling(ids, groupID);
            throw e;
        } catch (ResourceRequestDeniedException e) {
            this.backoutScheduling(ids, groupID);
            throw e;
        } catch (SchedulingException e) {
            this.backoutScheduling(ids, groupID);
            throw e;
        } catch (Throwable t) {
            this.backoutScheduling(ids, groupID);
            throw new CreationException("Unknown problem occured: " +
                    "'" + ErrorUtil.excString(t) + "'", t);
        }
        
    }

    protected Reservation scheduleImpl(VirtualMachine vm,
                                       int numNodes,
                                       String groupid,
                                       String coschedid,
                                       String callerID)
            
            throws SchedulingException,
                   ResourceRequestDeniedException {

        final VirtualMachineDeployment dep = vm.getDeployment();
        if (dep == null) {
            throw new SchedulingException("no deployment request");
        }

        final int memory = dep.getIndividualPhysicalMemory();
        final int duration = dep.getMinDuration();

        // list of associations should be in the DB, perpetuation of
        // shortcut here.
        String[] assocs = null;
        final String assocStr = vm.getAssociationsNeeded();
        if (assocStr != null) {
            assocs = assocStr.split(",");
        }

        return this.scheduler.schedule(memory, duration, assocs, numNodes,
                                       groupid, coschedid, callerID);
    }


    protected void backoutScheduling(int[] ids, String groupid) {

        logger.debug("Problem encountered mid-creation, attempting to remove " +
                     Lager.oneormanyid(ids, groupid) + " from scheduler.");

        for (int i = 0; i < ids.length; i++) {
            try {
                this.scheduler.stateNotification(
                            ids[i], WorkspaceConstants.STATE_DESTROYING);

            } catch (Throwable t) {
                logger.error("Problem with removing " + Lager.id(ids[i]) +
                             " from scheduler: " + t.getMessage(), t);
            }
        }
    }

    
    // -------------------------------------------------------------------------
    // CREATE III
    // -------------------------------------------------------------------------

    // create #3 (wrapped, scheduler notification required for failure)
    protected CreateResult create3(Reservation res,
                                   VirtualMachine[] bindings,
                                   String callerID,
                                   Context context,
                                   String groupID,
                                   String coschedID)
            
            throws AuthorizationException,
                   CoSchedulingException,
                   CreationException,
                   MetadataException,
                   ResourceRequestDeniedException,
                   SchedulingException {

        // todo: check assumptions (guard against misuse by object extenders)

        final int ids[] = res.getIds();

        if (!res.isConcrete()) {
            logger.debug("pending -- best effort or part of " +
                                                    "coscheduling group");
        }

        // accounting:
        // give check then act problem as small a window as possible
        if (this.accounting != null) {

            final long requestSeconds;
            if (res.isConcrete()) {
                final long ms = res.getStopTime().getTimeInMillis() -
                                   Calendar.getInstance().getTimeInMillis();
                requestSeconds = ms/1000;
            } else {
                requestSeconds = bindings[0].getDeployment().getMinDuration();
            }
            final long requestedMinutes =
                    WorkspaceUtil.secondsToMinutes(requestSeconds);

            final int requestedCPUCount = bindings[0].getDeployment().getIndividualCPUCount();
            final int requestedMemory = bindings[0].getDeployment().getIndividualPhysicalMemory();


            for (int i = 0; i < ids.length; i++) {
                final String resource;
                if (res.isConcrete()) {
                    final IdHostnameTuple tup = res.getIdHostnamePair(i);
                    resource = tup.hostname;
                } else {
                    resource = null;
                }

                final String network = bindings[i].getNetwork();
                final String name = bindings[i].getName();
                
                this.accounting.create(ids[i], callerID, requestedMinutes,
                                       network, resource, name,
                                       requestedCPUCount, requestedMemory);
            }
        }

        try {
            return create4(res,
                           bindings,
                           callerID,
                           context,
                           groupID,
                           coschedID);

        } catch (CoSchedulingException e) {
            this.backoutAccounting(ids, callerID);
            throw e;
        } catch (CreationException e) {
            this.backoutAccounting(ids, callerID);
            throw e;
        } catch (MetadataException e) {
            this.backoutAccounting(ids, callerID);
            throw e;
        } catch (ResourceRequestDeniedException e) {
            this.backoutAccounting(ids, callerID);
            throw e;
        } catch (SchedulingException e) {
            this.backoutAccounting(ids, callerID);
            throw e;
        } catch (Throwable t) {
            this.backoutAccounting(ids, callerID);
            throw new CreationException("Unknown problem occured: " +
                    "'" + ErrorUtil.excString(t) + "'", t);
        }
    }

    protected void backoutAccounting(int ids[], String callerID) {
        
        // todo: check assumptions (guard against misuse by object extenders)

        if (this.accounting == null) {
            return; // *** EARLY RETURN ***
        }

        for (int i = 0; i < ids.length; i++) {
            try {
                this.accounting.destroy(ids[i], callerID, 0);
            } catch (Throwable t) {
                logger.error("Problem with destroying " + Lager.id(ids[i]) +
                         " accounting record: " + t.getMessage(), t);
            }
        }
    }



    // -------------------------------------------------------------------------
    // CREATE IV
    // -------------------------------------------------------------------------

    // create #4 (wrapped, accounting destruction might be required for failure)
    protected CreateResult create4(Reservation res,
                                   VirtualMachine[] bindings,
                                   String callerID,
                                   Context context,
                                   String groupID,
                                   String coschedID)
            
            throws AuthorizationException,
                   CoSchedulingException,
                   CreationException,
                   MetadataException,
                   ResourceRequestDeniedException,
                   SchedulingException {

        // todo: check assumptions (guard against misuse by object extenders)

        // shutdown time is separate from termination time
        // best effort gets null (infinite) termination time: when it
        // starts, the termination time will be adjusted

        Calendar startTime = null;
        Calendar stopTime = null;
        Calendar termTime = null;

        if (res.isConcrete()) {

            startTime = res.getStartTime();
            if (startTime == null) {
                throw new SchedulingException(
                        "expecting start time if reservation is 'concrete'");
            }

            stopTime = res.getStopTime();
            if (stopTime == null) {
                throw new SchedulingException(
                        "expecting start time if reservation is 'concrete'");
            }

            termTime = (Calendar) stopTime.clone();
            termTime.add(Calendar.SECOND,
                         this.globals.getTerminationOffsetSeconds());
        }

        final int[] ids = res.getIds();
        if (ids == null) {
            throw new SchedulingException(
                        "expecting ID assignments from reservation");
        }

        final _CreateResult result = this.repr._newCreateResult();
        result.setCoscheduledID(coschedID);
        result.setGroupID(groupID);
        final VM[] createdVMs = new VM[ids.length];

        // make sure all group information is set on the VMs
        if (bindings.length > 1 && bindings[0].isPropagateRequired()) {
            for (int i = 0; i < bindings.length; i++) {
                bindings[i].setGroupTransferID(groupID);
                bindings[i].setGroupCount(bindings.length);
            }
        }

        int bailed = -1;
        Throwable failure = null;
        for (int i = 0; i < ids.length; i++) {

            try {
                createdVMs[i] =
                        this.createOne(i, ids, res, bindings[i],
                                       callerID, context, coschedID, groupID,
                                       startTime, termTime);
            } catch (Throwable t) {
                bailed = i;
                failure = t;
                break;
            }
            
        }

        if (failure != null) {

            // always throws an exception, return is to satisfy compiler
            return this.failure(ids, bailed, failure);

        } else {

            result.setVMs(createdVMs);

            final boolean eventLog = this.lager.eventLog;
            final boolean debugLog = logger.isDebugEnabled();

            if (eventLog || debugLog) {
                this.successPrint(eventLog, debugLog,
                                  startTime, stopTime, termTime,
                                  bindings[0].getName(), callerID,
                                  ids.length, ids,
                                  groupID, coschedID, res);
            }

            // go:
            this.schedulerCreatedNotification(ids);

            return result;
        }
    }

    protected void schedulerCreatedNotification(int[] ids) {
        
        // From here on, scheduler can do whatever it likes.  Instead
        // of letting the scheduler hijack this thread, launching this
        // notification in a new one.
        //
        // In the default scheduler's case, since it handles the request
        // immediately, not doing this could significantly delay the op
        // return to the client.

        final Event event = new Event(ids,
                                      WorkspaceConstants.STATE_FIRST_LEGAL,
                                      this.scheduler);

        this.timerManager.schedule(event, 20);
    }


    // -------------------------------------------------------------------------
    // INSTANCE CREATION/BACKOUT (called from CREATE IV)
    // -------------------------------------------------------------------------

    // always throws an exception, return is to satisfy compiler
    protected CreateResult failure(int[] ids, int bailed, Throwable failure)
            throws CreationException {

        if (ids == null) {
            throw new IllegalArgumentException("ids may not be null");
        }

        if (bailed > ids.length-1) {
            throw new IllegalArgumentException(
                    "bailed index would be index out of bounds");
        }

        if (failure == null) {
            throw new IllegalArgumentException("failure may not be null");
        }

        final String err = "Internal problem finalizing creation " +
                "request: '" + failure.getMessage() + "'";

        if (bailed < 1) {
            throw new CreationException(err, failure);
        }

        logger.error(err + ", backing out the already created instances.");

        for (int i = 0; i < bailed; i++) {
            try {

                this.whome.destroy(ids[i]);

            } catch (Throwable t) {

                String msg = t.getMessage();
                if (msg == null) {
                    msg = t.getClass().getName();
                }

                if (logger.isDebugEnabled()) {
                    logger.error("Error with one instance backout: " + msg, t);
                } else {
                    logger.error("Error with one instance backout: " + msg);
                }

                // continue trying anyhow
            }
        }

        throw new CreationException(err, failure);
    }

    protected VM createOne(int idx,
                           int[] ids,
                           Reservation res,
                           VirtualMachine vm,
                           String callerID,
                           Context context,
                           String coschedID,
                           String groupID,
                           Calendar startTime,
                           Calendar termTime)

            throws CreationException,
                   WorkspaceDatabaseException,
                   CannotTranslateException {

        // todo: check assumptions (guard against misuse by object extenders)

        final int id;
        final String node;
        if (res.isConcrete()) {
            final IdHostnameTuple idhost = res.getIdHostnamePair(idx);
            id = idhost.id;
            node = idhost.hostname;
        } else {
            id = ids[idx];
            node = null;
        }

        boolean last = false;
        if (idx == ids.length-1) {
            last = true;
        }

        final InstanceResource resource = this.whome.newInstance(id);

        this.populateResource(id, resource, vm,
                              callerID, coschedID, groupID,
                              startTime, termTime, node,
                              ids.length, last, idx);

        this.persistence.add(resource);

        if (context != null) {
            // todo: adding IPs to text here isn't necessary, this was added
            // as a shortcut for a release but needs to go back to IaaS-agnostic
            // text that works like a blob. Clean up w/ next interface changes
            // to the ctx broker
            try {
                final String newContent =
                        addIPs(context.getBootstrapText(), vm);
                final CustomizationNeed need =
                        this.binding.newCustomizationNeed(
                                newContent, context.getBootstrapPath());
                resource.newCustomizationNeed(need);
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }

        return this.dataConvert.getVM(resource);
    }

    protected String addIPs(String bootstrapText, VirtualMachine vm)
            throws CannotTranslateException {

        if (bootstrapText == null) {
            logger.warn("context document had null text?");
            return null;
        }
        
        final String FIELD_SEPARATOR =
              "\n\n=======================================================\n\n";

        final NIC[] nics = this.dataConvert.getNICs(vm);

        if (nics == null || nics.length == 0 || nics.length > 2) {
            logger.error("Cannot handle zero or more than two NICs w/ context");
            return null;
        }

        final String ifaceOne = nics[0].getName();
        final String ipOne = nics[0].getIpAddress();
        final String hostOne = nics[0].getHostname();
        String ifaceTwo = "NONE";
        String ipTwo = "NONE";
        String hostTwo = "NONE";

        if (nics.length == 2) {
            ifaceTwo = nics[1].getName();;
            ipTwo = nics[1].getIpAddress();
            hostTwo = nics[1].getHostname();
        }

        final StringBuffer buf = new StringBuffer(bootstrapText);
        buf.append(ifaceOne)
           .append(FIELD_SEPARATOR)
           .append(ipOne)
           .append(FIELD_SEPARATOR)
           .append(hostOne)
           .append(FIELD_SEPARATOR)
           .append(ifaceTwo)
           .append(FIELD_SEPARATOR)
           .append(ipTwo)
           .append(FIELD_SEPARATOR)
           .append(hostTwo)
           .append(FIELD_SEPARATOR);
        return buf.toString();
    }

    protected void populateResource(int id,
                                    InstanceResource resource,
                                    VirtualMachine vm,
                                    String callerID,
                                    String coschedID,
                                    String groupID,
                                    Calendar startTime,
                                    Calendar termTime,
                                    String node,
                                    int nodeNum,
                                    boolean lastInGroup,
                                    int launchIndex)
            throws CreationException {

        // todo: check assumptions (guard against misuse by object extenders)

        resource.populate(id, vm, startTime, termTime, node);

        // in the future, policy should come from elsewhere
        resource.setCreatorID(callerID);

        resource.setLaunchIndex(launchIndex);

        // client operations are not enabled (besides destroy, getRP, etc)
        // until the scheduler enables them.
        resource.setInitialOpsEnabled(false);

        resource.setGroupSize(nodeNum);
        resource.setLastInGroup(lastInGroup);
        // OK if null:
        resource.setGroupId(groupID);

        // OK if null:
        resource.setEnsembleId(coschedID);

        if (nodeNum > 1) {
            resource.setPartOfGroupRequest(true);
        }
    }

    
    // -------------------------------------------------------------------------
    // SUCCESS LOGGING
    // -------------------------------------------------------------------------

    protected void successPrint(boolean eventLog,
                                boolean debugLog,
                                Calendar startTime,
                                Calendar stopTime,
                                Calendar termTime,
                                String name,
                                String creatorID,
                                int nodeNum,
                                int[] ids,
                                String groupID,
                                String coschedID,
                                Reservation res) {

        // todo: check assumptions (guard against misuse by object extenders)

        final String start;
        if (startTime == null) {
            start = "not set yet";
        } else {
            start = this.localFormat.format(startTime.getTime());
        }

        final String stop;
        if (stopTime == null) {
            stop = "not set yet";
        } else {
            stop = this.localFormat.format(stopTime.getTime());
        }

        final String term;
        if (termTime == null) {
            term = "not set yet";
        } else {
            term = this.localFormat.format(termTime.getTime());
        }

        final StringBuffer buf = new StringBuffer(512);

        if (nodeNum == 1) {
            if (eventLog) {
                buf.append(Lager.ev(ids[0]));
            }
            buf.append("\n\nWORKSPACE INSTANCE CREATED:\n");
        } else {
            if (eventLog) {
                buf.append(Lager.groupev(groupID));
            }
            buf.append("\n\n").
                append(nodeNum).
                append(" WORKSPACE GROUP CREATED:\n");
        }

        buf.append("    - Name: '").
            append(name).
            append("'\n").
            append("    - Start time:                ").
            append(start).
            append("\n").
            append("    - Shutdown time:             ").
            append(stop).
            append("\n").
            append("    - Resource termination time: ").
            append(term).
            append("\n").
            append("    - Creator: ").
            append(creatorID).
            append("\n");

        buf.append(this.nodeReport(res));

        if (groupID != null) {
            buf.append("\n    - group ID: ")
               .append(groupID);
        }

        if (coschedID != null) {
            buf.append("\n    - coscheduled ID: ")
               .append(coschedID);
        }

        if (eventLog) {
            logger.info(buf.toString());
        } else if (debugLog) {
            logger.debug(buf.toString());
        }

    }

    protected String nodeReport(Reservation res) {

        // todo: check assumptions (guard against misuse by object extenders)

        final int[] ids = res.getIds();
        if (ids.length == 1) {
            String msg = "    - ID: " + ids[0];
            if (res.getResponseLength() > 0) {
                msg += ", VMM: " + res.getIdHostnamePair(0).hostname;
            }
            return msg;
        }

        final StringBuffer buf;
        if (ids.length > 25) {
            buf = new StringBuffer(ids.length * 32);
        } else {
            buf = new StringBuffer(512);
        }

        if (res.getResponseLength() == 0) {
            buf.append("    - IDs: ");
            buf.append(ids[0]);
            for (int i = 1; i < ids.length; i++) {
                buf.append(", ").
                    append(ids[i]);
            }
            buf.append("\n");
            return buf.toString();
        }

        IdHostnameTuple idhost = res.getIdHostnamePair(0);
        buf.append("    - ID(VMM): ");
        buf.append(idhost.id).
            append("(").
            append(idhost.hostname).
            append(")");

        for (int i = 1; i < ids.length; i++) {
            idhost = res.getIdHostnamePair(i);
            buf.append(", ").
                append(idhost.id).
                append("(").
                append(idhost.hostname).
                append(")");
        }
        buf.append("\n");
        return buf.toString();
    }
}
