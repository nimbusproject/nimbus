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

import edu.emory.mathcs.backport.java.util.concurrent.locks.Lock;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.workspace.ErrorUtil;
import org.globus.workspace.Lager;
import org.globus.workspace.LockManager;
import org.globus.workspace.ProgrammingError;
import org.globus.workspace.WorkspaceConstants;
import org.globus.workspace.WorkspaceUtil;
import org.globus.workspace.accounting.AccountingEventAdapter;
import org.globus.workspace.async.AsyncRequest;
import org.globus.workspace.async.AsyncRequestManager;
import org.globus.workspace.creation.CreationManager;
import org.globus.workspace.creation.IdempotentCreationManager;
import org.globus.workspace.creation.IdempotentInstance;
import org.globus.workspace.creation.IdempotentReservation;
import org.globus.workspace.network.AssociationAdapter;
import org.globus.workspace.persistence.DataConvert;
import org.globus.workspace.persistence.PersistenceAdapter;
import org.globus.workspace.persistence.WorkspaceDatabaseException;
import org.globus.workspace.scheduler.IdHostnameTuple;
import org.globus.workspace.scheduler.Reservation;
import org.globus.workspace.scheduler.Scheduler;
import org.globus.workspace.scheduler.StateChangeEvent;
import org.globus.workspace.service.InstanceResource;
import org.globus.workspace.service.WorkspaceCoschedHome;
import org.globus.workspace.service.WorkspaceGroupHome;
import org.globus.workspace.service.WorkspaceHome;
import org.globus.workspace.service.binding.Authorize;
import org.globus.workspace.service.binding.BindNetwork;
import org.globus.workspace.service.binding.BindingAdapter;
import org.globus.workspace.service.binding.GlobalPolicies;
import org.globus.workspace.service.binding.vm.VirtualMachine;
import org.globus.workspace.service.binding.vm.VirtualMachineDeployment;
import org.globus.workspace.creation.InternalCreationManager;
import org.globus.workspace.service.binding.vm.FileCopyNeed;

import org.nimbustools.api._repr._Advertised;
import org.nimbustools.api.repr.Advertised;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.CannotTranslateException;
import org.nimbustools.api.repr.CreateRequest;
import org.nimbustools.api.repr.ReprFactory;
import org.nimbustools.api.repr.AsyncCreateRequest;
import org.nimbustools.api.repr.SpotCreateRequest;
import org.nimbustools.api.repr.ctx.Context;
import org.nimbustools.api.repr.si.SIConstants;
import org.nimbustools.api.repr.vm.NIC;
import org.nimbustools.api.repr.vm.ResourceAllocation;

import org.nimbustools.api.services.rm.*;
import org.safehaus.uuid.UUIDGenerator;

import java.util.Arrays;
import java.util.Calendar;
import java.text.DateFormat;
import java.util.List;

import commonj.timers.TimerManager;

/**
 * Initial intake, resource requests, resource grants, and setup of new
 * instance objects. 
 *
 * Includes rollbacks (looking into dedicated transaction technology for the
 * future).
 */
public class CreationManagerImpl implements CreationManager, InternalCreationManager {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(CreationManagerImpl.class.getName());
    

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
    protected final BindNetwork bindNetwork;

    protected AccountingEventAdapter accounting;
    
    protected AsyncRequestManager asyncManager;
    protected IdempotentCreationManager idemManager;


    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    public CreationManagerImpl(LockManager lockManagerImpl,
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
                           Lager lagerImpl,
                           BindNetwork bindNetworkImpl,
                           IdempotentCreationManager idempotentCreationManager) {

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
        
        if (bindNetworkImpl == null) {
            throw new IllegalArgumentException("bindNetworkImpl may not be null");
        }
        this.bindNetwork = bindNetworkImpl;

        if (idempotentCreationManager == null) {
            logger.warn("Idempotency creation manager is null!");
        }
        this.idemManager = idempotentCreationManager;
    }


    // -------------------------------------------------------------------------
    // OPTIONAL SETs
    // -------------------------------------------------------------------------

    public void setAccountingEventAdapter(AccountingEventAdapter accEvents) {
        this.accounting = accEvents;
    }

    
    // -------------------------------------------------------------------------
    // implements CreationManager
    // -------------------------------------------------------------------------

    public Advertised getAdvertised() {

        final _Advertised adv = this.repr._newAdvertised();
        adv.setCpuArchitectureNames(
                this.globals.getCpuArchitectureNames());
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
    
    
    /**
     * An asynchronous create request is not satisfied at the same time
     * it is submitted, but when the Asynchronous Request Manager
     * decides to fulfill that request based on policies.
     * 
     * Currently, asynchronous requests can be Spot Instance
     * requests or backfill requests.
     * 
     * @param req the asynchronous create request
     * @param caller the owner of the request
     * @return the added asynchronous request
     * @throws CreationException
     * @throws MetadataException
     * @throws ResourceRequestDeniedException
     * @throws SchedulingException
     */    
    public AsyncRequest addAsyncRequest(AsyncCreateRequest req, Caller caller)
            throws CreationException,
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

        //Check basic request errors
        this.legals.checkCreateRequest(req);
        
        //Check site policies and constraints
        final VirtualMachine[] bound = this.binding.processRequest(req);
        if (bound == null || bound.length == 0) {
            throw new CreationException("no binding result but no binding " +
                    "error: illegal binding implementation");
        }        

        final String creatorID = caller.getIdentity();
        if (creatorID == null || creatorID.trim().length() == 0) {
            throw new CreationException("Cannot determine identity");
        }       
        
        final String groupID = this.getGroupID(creatorID, bound.length);   
        
        final String reqiID = generateRequestID();
        
        AsyncRequest asyncReq;
                
        if(req instanceof SpotCreateRequest){
            SpotCreateRequest spotReq = (SpotCreateRequest)req;
            asyncReq = new AsyncRequest(reqiID, spotReq.getSpotPrice(), spotReq.isPersistent(), 
                                        caller, groupID, bound, req.getContext(), req.getRequestedNics(), 
                                        req.getSshKeyName(), Calendar.getInstance());   
        } else {
            asyncReq = new AsyncRequest(reqiID, caller, groupID, bound, 
                                        req.getContext(), req.getRequestedNics(), 
                                        Calendar.getInstance());               
        }
        
        asyncManager.addRequest(asyncReq); 
        
        return asyncReq;
    }    

    public InstanceResource[] create(CreateRequest req, Caller caller)

            throws CoSchedulingException,
                   CreationException,
                   MetadataException,
                   ResourceRequestDeniedException,
                   SchedulingException,
                   AuthorizationException {

        if (caller == null) {
            throw new CreationException("no caller");
        }

        if (this.lager.eventLog) {
            logger.info(Lager.ev(-1) + "Create request for " + this.getType(req) +
                                    " from '" + caller.getIdentity() + "'");
        }

        this.legals.checkCreateRequest(req);
        
        final VirtualMachine[] bound = this.binding.processRequest(req);
        if (bound == null || bound.length == 0) {
            throw new CreationException("no binding result but no binding " +
                    "error: illegal binding implementation");
        }         
        
        final String creatorID = caller.getIdentity();
        if (creatorID == null || creatorID.trim().length() == 0) {
            throw new CreationException("Cannot determine identity");
        }


        if (req.getClientToken() == null || req.getClientToken().length() == 0) {
            logger.debug("Non-idempotent instance creation");

            // non-idempotent creation
            return doCreation(req, caller, bound);

        } else if (this.idemManager == null) {
            logger.warn("Instance request has client token (" +
                    req.getClientToken() +
                    ") but idempotency is disabled! Proceeding with creation.");

            return doCreation(req, caller, bound);

        } else {

            logger.debug("Idempotent instance creation. clientToken='" +
                    req.getClientToken() + "'");
            return doIdempotentCreation(req, caller, bound);
        }
    }

    private InstanceResource[] doCreation(CreateRequest req, Caller caller, VirtualMachine[] bound)
            throws CreationException, CoSchedulingException, MetadataException,
            ResourceRequestDeniedException, SchedulingException, AuthorizationException {

        final String coschedID = this.getCoschedID(req, caller.getIdentity());
        final String groupID = this.getGroupID(caller.getIdentity(), bound.length);

        return this.createVMs(bound, req.getRequestedNics(), caller, req.getContext(),
                groupID, coschedID, req.getClientToken(), false, 1.0);
    }

    private InstanceResource[] doIdempotentCreation(CreateRequest req, Caller caller, VirtualMachine[] bound)
            throws CreationException,
            CoSchedulingException,
            MetadataException,
            ResourceRequestDeniedException,
            SchedulingException,
            AuthorizationException {

        String creatorID = caller.getIdentity();
        final String clientToken = req.getClientToken();
        IdempotentReservation res;
        final Lock idemLock = idemManager.getLock(creatorID, clientToken);

        try {
            idemLock.lockInterruptibly();
        } catch (InterruptedException e) {
            throw new CreationException(e.getMessage(), e);
        }

        try {

            res = this.idemManager.getReservation(creatorID, clientToken);
            if (res != null) {

                if (logger.isDebugEnabled()) {
                    logger.debug("Found existing idempotent reservation: " +
                            res.toString());
                }

                // the reservation already exists. check its validity
                return resolveIdempotentReservation(res, req);

            } else {

                final InstanceResource[] resources =
                        this.doCreation(req, caller, bound);

                this.idemManager.addReservation(creatorID, clientToken,
                        Arrays.asList(resources));
                return resources;
            }

        } catch (ManageException e) {
            throw new CreationException(e.getMessage(), e);
        } finally {
            idemLock.unlock();
        }
    }

    private InstanceResource[] resolveIdempotentReservation(IdempotentReservation res,
                                                            CreateRequest req)
            throws ManageException, CreationException {


        final List<IdempotentInstance> instances = res.getInstances();
        if (instances == null || instances.isEmpty()) {
            throw new CreationException("Idempotent reservation exists but has no instances");
        }

        if (res.getGroupId() == null && instances.size() > 1) {
            throw new CreationException("Idempotent reservation is not a group but has more than 1 instance");
        }

        if (instances.size() != req.getRequestedRA().getNodeNumber()) {
            throw new IdempotentCreationMismatchException("instance count mismatch");
        }

        final String logId;
        if (res.getGroupId() != null) {
            logId = Lager.groupid(res.getGroupId());
        } else {
            final IdempotentInstance instance = instances.get(0);
            logId = Lager.id(instance.getID());
        }
        logger.info(logId + " idempotent creation request already fulfilled");

        InstanceResource[] resources = new InstanceResource[instances.size()];
        int index = 0;
        for (IdempotentInstance instance : instances) {
            if (instance == null) {
                throw new CreationException("Idempotent reservation has null instance");
            }

            InstanceResource resource;
            try {
                resource = this.whome.find(instance.getID());

                if (resource == null) {
                    throw new CreationException("Existing idempotent instance was null (?)");
                }

                // these parameter checks can only be performed against a running
                // instance.

                this.checkIdempotentInstanceForMismatch(resource, req, logId);

            } catch (DoesNotExistException e) {
                logger.debug("Idempotent reservation request has a terminated instance");

                final VirtualMachine vm = new VirtualMachine();
                vm.setNetwork("NONE");
                final VirtualMachineDeployment deployment = new VirtualMachineDeployment();
                vm.setDeployment(deployment);

                resource = new IdempotentInstanceResource(instance.getID(),
                        instance.getName(), null, res.getGroupId(), instances.size(),
                        instance.getLaunchIndex(), res.getCreatorId(), vm,
                        WorkspaceConstants.STATE_DESTROYING, res.getClientToken());
            }

            if (resource.getName() == null) {
                if (req.getName() != null) {
                    throw new IdempotentCreationMismatchException("instance name mismatch");
                }
            } else {
                if (!resource.getName().equals(req.getName())) {
                    throw new IdempotentCreationMismatchException("instance name mismatch");
                }
            }
            resources[index] = resource;

            index++;
        }

        return resources;
    }

    private void checkIdempotentInstanceForMismatch(InstanceResource resource,
                                                    CreateRequest request,
                                                    String logId)
            throws IdempotentCreationMismatchException {

        // could check a lot more things here, but it is not critical, more an aid to users with
        // buggy apps

        Integer instanceDuration = null;
        Integer requestDuration = null;

        Integer instanceMemory = null;
        Integer requestMemory = null;

        Integer instanceCpuCount = null;
        Integer requestCpuCount = null;

        final VirtualMachine vm = resource.getVM();
        if (vm != null) {
            final VirtualMachineDeployment deployment = vm.getDeployment();
            if (deployment != null) {
                instanceDuration = deployment.getMinDuration();
                instanceMemory = deployment.getIndividualPhysicalMemory();
                instanceCpuCount = deployment.getIndividualCPUCount();
            }
        }

        if (request.getRequestedSchedule() != null) {
            requestDuration = request.getRequestedSchedule().getDurationSeconds();
        }

        final ResourceAllocation ra = request.getRequestedRA();
        if (ra != null) {
            requestMemory = ra.getMemory();
            requestCpuCount = ra.getIndCpuCount();
        }

        final String msg = logId + "Idempotent request failed because of a parameter mismatch: ";
        if (instanceDuration != null && requestDuration != null) {
            if (!instanceDuration.equals(requestDuration)) {
                logger.info(msg + "duration");
                throw new IdempotentCreationMismatchException("duration mismatch");
            }
        }

        if (instanceMemory != null && requestMemory != null) {
            if (!instanceMemory.equals(requestMemory)) {
                logger.info(msg + "memory");
                throw new IdempotentCreationMismatchException("memory mismatch");
            }
        }

        if (instanceCpuCount != null && requestCpuCount != null) {
            if (!instanceCpuCount.equals(requestCpuCount)) {
                logger.info(msg + "CPU count");
                throw new IdempotentCreationMismatchException("CPU count mismatch");
            }
        }
    }


    /**
     * Generates a random Spot Instance request ID
     * @return the generated ID
     */
    private String generateRequestID() {
        
        return SIConstants.SI_REQUEST_PREFIX + this.uuidGen.generateRandomBasedUUID().toString(); 
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
    
    // -------------------------------------------------------------------------
    // implements InternalCreationManager
    // -------------------------------------------------------------------------

//    public InstanceResource[] createVMs2(VirtualMachine[] bindings,
//                                   NIC[] nics,
//                                   Caller caller,
//                                   Context context,
//                                   String groupId,
//                                   String coschedID,
//                                   boolean spotInstances)
//
//            throws CoSchedulingException,
//                   CreationException,
//                   MetadataException,
//                   ResourceRequestDeniedException,
//                   SchedulingException {
//        
//        this.bindNetwork.consume(bindings, nics);
//
//        // From this point forward an error requires backOutIPAllocations
//        try {
//            return this.create1(bindings, caller, context, groupId, coschedID, spotInstances);
//        } catch (CoSchedulingException e) {
//            this.backoutBound(bindings);
//            throw e;
//        } catch (CreationException e) {
//            this.backoutBound(bindings);
//            throw e;
//        } catch (MetadataException e) {
//            this.backoutBound(bindings);
//            throw e;
//        } catch (ResourceRequestDeniedException e) {
//            this.backoutBound(bindings);
//            throw e;
//        } catch (SchedulingException e) {
//            this.backoutBound(bindings);
//            throw e;                    
//        } catch (Throwable t) {
//            this.backoutBound(bindings);
//            throw new CreationException("Unknown problem occured: " +
//                    "'" + ErrorUtil.excString(t) + "'", t);
//        }
//    }

    public InstanceResource[] createVMs(VirtualMachine[] bindings,
                                        NIC[] nics,
                                        Caller caller,
                                        Context context,
                                        String groupID,
                                        String coschedID,
                                        String clientToken,
                                        boolean spotInstances,
                                        double chargeRatio)

            throws CoSchedulingException,
                   CreationException,
                   MetadataException,
                   ResourceRequestDeniedException,
                   SchedulingException,
                   AuthorizationException {

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
        if (!spotInstances &&!caller.isSuperUser()) {
            this.authorize.authz(bindings,
                                 caller.getIdentity(),
                                 caller.getSubject(),
                                 chargeRatio);
        }

        final Reservation res = this.scheduleImpl(bindings[0],
                                                  bindings.length,
                                                  groupID,
                                                  coschedID,
                                                  caller.getIdentity());

        // From this point forward an error requires attempt to
        // remove from scheduler        
        
        if (res == null) {
            throw new SchedulingException("reservation is missing, illegal " +
                    "scheduling implementation");
        }
        
        this.bindNetwork.consume(bindings, nics);        
        
        // From this point forward an error requires backOutIPAllocations
        
        
        final int[] ids = res.getIds();

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

            return create1(res,
                           bindings,
                           caller.getIdentity(),
                           context,
                           groupID,
                           coschedID,
                           clientToken,
                           spotInstances,
                           chargeRatio);

        } catch (CoSchedulingException e) {
            this.backoutScheduling(ids, groupID);
            this.backoutBound(bindings);            
            throw e;
        } catch (CreationException e) {
            this.backoutScheduling(ids, groupID);
            this.backoutBound(bindings);            
            throw e;
        } catch (MetadataException e) {
            this.backoutScheduling(ids, groupID);
            this.backoutBound(bindings);
            throw e;
        } catch (ResourceRequestDeniedException e) {
            this.backoutScheduling(ids, groupID);
            this.backoutBound(bindings);            
            throw e;
        } catch (SchedulingException e) {
            this.backoutScheduling(ids, groupID);
            this.backoutBound(bindings);            
            throw e;
        } catch (Throwable t) {
            this.backoutScheduling(ids, groupID);
            this.backoutBound(bindings);            
            throw new CreationException("Unknown problem occurred: " +
                    "'" + ErrorUtil.excString(t) + "'", t);
        }
        
    }
    
    protected void backoutBound(VirtualMachine[] bound) {
        try {
            this.bindNetwork.backOutIPAllocations(bound);
            //this.binding.backOutAllocations(bound);
        } catch (Throwable t) {
            final String err =
                    "Error during bindings backout: " + t.getMessage();
            logger.error(err, t);
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
        final int cores = dep.getIndividualCPUCount();
        final int duration = dep.getMinDuration();

        // list of associations should be in the DB, perpetuation of
        // shortcut here.
        String[] assocs = null;
        final String assocStr = vm.getAssociationsNeeded();
        if (assocStr != null) {
            assocs = assocStr.split(",");
        }

        return this.scheduler.schedule(memory, cores, duration, assocs, numNodes,
                                       groupid, coschedid, vm.isPreemptable(), callerID);
    }


    protected void backoutScheduling(int[] ids, String groupid) {

        logger.debug("Problem encountered mid-creation, attempting to remove " +
                     Lager.oneormanyid(ids, groupid) + " from scheduler.");

        for (int i = 0; i < ids.length; i++) {
            try {
                this.scheduler.removeScheduling(ids[i]);

            } catch (Throwable t) {
                logger.error("Problem with removing " + Lager.id(ids[i]) +
                             " from scheduler: " + t.getMessage(), t);
            }
        }
    }

    
    // -------------------------------------------------------------------------
    // CREATE I
    // -------------------------------------------------------------------------

    // create #1 (wrapped, backOutAllocations required for failures)    
    // create #1 (wrapped, scheduler notification required for failure)
    protected InstanceResource[] create1(Reservation res,
                                         VirtualMachine[] bindings,
                                         String callerID,
                                         Context context,
                                         String groupID,
                                         String coschedID,
                                         String clientToken,
                                         boolean spotInstances,
                                         double chargeRatio)
            
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
                                       requestedCPUCount, requestedMemory, chargeRatio);
            }
        }

        try {
            return create2(res,
                           bindings,
                           callerID,
                           context,
                           groupID,
                           coschedID,
                           clientToken,
                           spotInstances,
                           chargeRatio);

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
            throw new CreationException("Unknown problem occurred: " +
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
                // charge ratio does not matter here since charge is zero
                this.accounting.destroy(ids[i], callerID, 0, 1.0);
            } catch (Throwable t) {
                logger.error("Problem with destroying " + Lager.id(ids[i]) +
                         " accounting record: " + t.getMessage(), t);
            }
        }
    }



    // -------------------------------------------------------------------------
    // CREATE II
    // -------------------------------------------------------------------------

    // create #2 (wrapped, accounting destruction might be required for failure)
    protected InstanceResource[] create2(Reservation res,
                                         VirtualMachine[] bindings,
                                         String callerID,
                                         Context context,
                                         String groupID,
                                         String coschedID,
                                         String clientToken,
                                         boolean spotInstances,
                                         double chargeRatio)
            
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

        final InstanceResource[] createdResources = new InstanceResource[ids.length];

        int bailed = -1;
        Throwable failure = null;
        for (int i = 0; i < ids.length; i++) {

            try {
                createdResources[i] =
                        this.createOne(i, ids, res, bindings[i],
                                       callerID, context, coschedID, groupID,
                                       clientToken, startTime, termTime, chargeRatio);
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
            this.schedulerCreatedNotification(ids, spotInstances);

            return createdResources;
        }
    }

    protected void schedulerCreatedNotification(int[] ids, boolean spotInstances) {
        
        // From here on, scheduler can do whatever it likes.  Instead
        // of letting the scheduler hijack this thread, launching this
        // notification in a new one.
        //
        // In the default scheduler's case, since it handles the request
        // immediately, not doing this could significantly delay the op
        // return to the client.

        final StateChangeEvent schedulerEvent = new StateChangeEvent(ids,
                                      WorkspaceConstants.STATE_FIRST_LEGAL,
                                      this.scheduler);

        this.timerManager.schedule(schedulerEvent, 20);
        
        if(!spotInstances){
            final StateChangeEvent simEvent = new StateChangeEvent(ids,
                    WorkspaceConstants.STATE_FIRST_LEGAL,
                    this.asyncManager);

            this.timerManager.schedule(simEvent, 50);            
        }
    }


    // -------------------------------------------------------------------------
    // INSTANCE CREATION/BACKOUT (called from CREATE II)
    // -------------------------------------------------------------------------

    // always throws an exception, return is to satisfy compiler
    protected InstanceResource[] failure(int[] ids, int bailed, Throwable failure)
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

    protected InstanceResource createOne(int idx,
                                         int[] ids,
                                         Reservation res,
                                         VirtualMachine vm,
                                         String callerID,
                                         Context context,
                                         String coschedID,
                                         String groupID,
                                         String clientToken,
                                         Calendar startTime,
                                         Calendar termTime,
                                         double chargeRatio)

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
                              callerID, coschedID, groupID, clientToken,
                              startTime, termTime, node,
                              ids.length, last, idx, chargeRatio);

        this.persistence.add(resource);

        if (context != null) {
            // todo: adding IPs to text here isn't necessary, this was added
            // as a shortcut for a release but needs to go back to IaaS-agnostic
            // text that works like a blob. Clean up w/ next interface changes
            // to the ctx broker
            try {
                final String newContent =
                        addIPs(context.getBootstrapText(), vm);
                final FileCopyNeed need =
                        this.binding.newFileCopyNeed(
                                newContent, context.getBootstrapPath());
                resource.newFileCopyNeed(need);
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }

        return resource;
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
            ifaceTwo = nics[1].getName();
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
                                    String clientToken,
                                    Calendar startTime,
                                    Calendar termTime,
                                    String node,
                                    int nodeNum,
                                    boolean lastInGroup,
                                    int launchIndex,
                                    double chargeRatio)
            throws CreationException {

        // todo: check assumptions (guard against misuse by object extenders)

        resource.populate(id, vm, startTime, termTime, node, chargeRatio);

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

        // OK if null:
        resource.setClientToken(clientToken);

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

    // -------------------------------------------------------------------------
    // MODULE SET (avoids circular dependency problem)
    // -------------------------------------------------------------------------
    public void setSiManager(AsyncRequestManager siManagerImpl) {
        if (siManagerImpl == null) {
            throw new IllegalArgumentException("siManagerImpl may not be null");
        }
        this.asyncManager = siManagerImpl;
    }
}
