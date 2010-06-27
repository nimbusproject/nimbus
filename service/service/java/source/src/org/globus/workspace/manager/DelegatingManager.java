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

package org.globus.workspace.manager;

import java.util.Calendar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.workspace.Lager;
import org.globus.workspace.PathConfigs;
import org.globus.workspace.accounting.AccountingReaderAdapter;
import org.globus.workspace.accounting.ElapsedAndReservedMinutes;
import org.globus.workspace.creation.CreationManager;
import org.globus.workspace.persistence.DataConvert;
import org.globus.workspace.service.InstanceResource;
import org.globus.workspace.service.WorkspaceCoschedHome;
import org.globus.workspace.service.WorkspaceGroupHome;
import org.globus.workspace.service.WorkspaceHome;
import org.globus.workspace.spotinstances.SIRequest;
import org.globus.workspace.spotinstances.SpotInstancesHome;
import org.nimbustools.api._repr._Caller;
import org.nimbustools.api._repr._CreateResult;
import org.nimbustools.api.repr.Advertised;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.CannotTranslateException;
import org.nimbustools.api.repr.CreateRequest;
import org.nimbustools.api.repr.CreateResult;
import org.nimbustools.api.repr.ReprFactory;
import org.nimbustools.api.repr.RequestSI;
import org.nimbustools.api.repr.ShutdownTasks;
import org.nimbustools.api.repr.SpotRequest;
import org.nimbustools.api.repr.Usage;
import org.nimbustools.api.repr.vm.VM;
import org.nimbustools.api.services.rm.AuthorizationException;
import org.nimbustools.api.services.rm.CoSchedulingException;
import org.nimbustools.api.services.rm.CreationException;
import org.nimbustools.api.services.rm.DestructionCallback;
import org.nimbustools.api.services.rm.DoesNotExistException;
import org.nimbustools.api.services.rm.ManageException;
import org.nimbustools.api.services.rm.Manager;
import org.nimbustools.api.services.rm.MetadataException;
import org.nimbustools.api.services.rm.OperationDisabledException;
import org.nimbustools.api.services.rm.ResourceRequestDeniedException;
import org.nimbustools.api.services.rm.SchedulingException;
import org.nimbustools.api.services.rm.StateChangeCallback;

/**
 * Link into the application from the messaging layer.
 *
 * Dispatches work to the appropriate place.
 *
 * Relying on messaging layer to use getAuthorizedManagers()
 *
 * @see Manager
 * @see org.nimbustools.api.brain.BreathOfLife
 */
public class DelegatingManager implements Manager {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(DelegatingManager.class.getName());

    protected static final VM[] EMPTY_VM_ARRAY = new VM[0];

    
    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected final CreationManager creation;
    protected final WorkspaceHome home;
    protected final WorkspaceGroupHome ghome;
    protected final WorkspaceCoschedHome cohome;
    protected final PathConfigs paths;
    protected final ReprFactory repr;
    protected final DataConvert dataConvert;
    protected final Lager lager;
    protected final SpotInstancesHome siHome;
    
    protected AccountingReaderAdapter accounting;
    
    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    public DelegatingManager(CreationManager creationImpl,
                             PathConfigs pathConfigs,
                             WorkspaceHome instanceHome,
                             WorkspaceGroupHome groupHome,
                             WorkspaceCoschedHome coschedHome,
                             ReprFactory reprFactory,
                             DataConvert dataConvertImpl,
                             Lager lagerImpl,
                             SpotInstancesHome siHome) {
        
        if (creationImpl == null) {
            throw new IllegalArgumentException("creationImpl may not be null");
        }
        this.creation = creationImpl;

        if (pathConfigs == null) {
            throw new IllegalArgumentException("pathConfigs may not be null");
        }
        this.paths = pathConfigs;

        if (instanceHome == null) {
            throw new IllegalArgumentException("instanceHome may not be null");
        }
        this.home = instanceHome;

        if (groupHome == null) {
            throw new IllegalArgumentException("groupHome may not be null");
        }
        this.ghome = groupHome;

        if (coschedHome == null) {
            throw new IllegalArgumentException("coschedHome may not be null");
        }
        this.cohome = coschedHome;
        
        if (reprFactory == null) {
            throw new IllegalArgumentException("reprFactory may not be null");
        }
        this.repr = reprFactory;

        if (dataConvertImpl == null) {
            throw new IllegalArgumentException("dataConvertImpl may not be null");
        }
        this.dataConvert = dataConvertImpl;

        if (lagerImpl == null) {
            throw new IllegalArgumentException("lagerImpl may not be null");
        }
        this.lager = lagerImpl;
        
        if (siHome == null) {
            throw new IllegalArgumentException("siHome may not be null");
        }
        this.siHome = siHome;        
    }


    // -------------------------------------------------------------------------
    // SET
    // -------------------------------------------------------------------------

    public void setAccounting(AccountingReaderAdapter accountingReader) {
        this.accounting = accountingReader;
    }

    
    // -------------------------------------------------------------------------
    // implements NimbusModule
    // -------------------------------------------------------------------------

    public String report() {
        final StringBuffer buf = new StringBuffer("Class: ");
        buf.append(this.getClass().getName())
           .append("\n")
           .append("Workspace Service Manager");
        return buf.toString();
    }


    // -------------------------------------------------------------------------
    // implements Manager (several following sections)
    // -------------------------------------------------------------------------
    
    // -------------------------------------------------------------------------
    // LIFECYCLE
    // -------------------------------------------------------------------------

    /**
     * <p>This informs the implementation that its container (or whatever is
     * instantiating it) has just recovered from a failure or restart.</p>
     *
     * <p>If it is left up to normal initialization mechanisms, then there is
     * no chance for change/destruction listeners to be registered <i>before</i>
     * something happens.</p>
     *
     * @throws Exception problem, cannot continue
     */
    public void recover_initialize() throws Exception {
        this.home.recover_initialize();
    }

    public void shutdownImmediately() {
        this.home.shutdownImmediately();
    }

    
    // -------------------------------------------------------------------------
    // EVENTS CAUSED BY USER OPERATIONS - MUTATIVE
    // -------------------------------------------------------------------------

    public CreateResult create(CreateRequest req, Caller caller)
           throws CoSchedulingException,
                  CreationException,
                  MetadataException,
                  ResourceRequestDeniedException,
                  SchedulingException {
        
        InstanceResource[] resources = this.creation.create(req, caller);
        final _CreateResult result = this.repr._newCreateResult();        
        if(resources.length > 0){
            result.setCoscheduledID(resources[0].getEnsembleId());
            result.setGroupID(resources[0].getGroupId());
        }

        try {
            result.setVMs(getInstances(resources));
        } catch (CannotTranslateException e) {
            throw new MetadataException(e.getMessage(), e);
        }
        
        
        return result;
    }

    public void setDestructionTime(String id, int type, Calendar time)
            throws DoesNotExistException, ManageException {
        throw new ManageException("Not allowing remote termination changes");
    }

    public void trash(String id, int type, Caller caller)
            throws DoesNotExistException, ManageException {

        this.opIntake("TRASH", id, type, caller);
        
        switch (type) {
            case INSTANCE: this.home.destroy(id); break;
            case GROUP: this.ghome.destroy(id); break;
            case COSCHEDULED: this.cohome.destroy(id); break;
            default: throw new ManageException(
                                "Unknown/unhandled type: " + trType(type));
        }
    }

    public void start(String id, int type, Caller caller)
            throws DoesNotExistException, ManageException,
                   OperationDisabledException {

        this.opIntake("START", id, type, caller);

        switch (type) {
            case INSTANCE: this.home.find(id).start(); break;
            case GROUP: this.ghome.find(id).start(); break;
            default: throw new ManageException(
                                "Unknown/unhandled type: " + trType(type));
        }
    }

    public void shutdown(String id, int type,
                         ShutdownTasks tasks, Caller caller)
            throws DoesNotExistException,
                   ManageException,
                   OperationDisabledException {

        this.opIntake("SHUTDOWN", id, type, caller);

        switch (type) {
            case INSTANCE: this.home.find(id).shutdown(tasks); break;
            case GROUP: this.ghome.find(id).shutdown(tasks); break;
            default: throw new ManageException(
                                "Unknown/unhandled type: " + trType(type));
        }

    }

    public void shutdownSave(String id, int type,
                             ShutdownTasks tasks, Caller caller)
            throws DoesNotExistException,
                   ManageException,
                   OperationDisabledException {

        this.opIntake("SHUTDOWN-SAVE", id, type, caller);

        switch (type) {
            case INSTANCE: this.home.find(id).shutdownSave(tasks); break;
            case GROUP: this.ghome.find(id).shutdownSave(tasks); break;
            default: throw new ManageException(
                                "Unknown/unhandled type: " + trType(type));
        }
    }

    public void pause(String id, int type,
                      ShutdownTasks tasks, Caller caller)
            throws DoesNotExistException,
                   ManageException,
                   OperationDisabledException {

        this.opIntake("PAUSE", id, type, caller);
        
        switch (type) {
            case INSTANCE: this.home.find(id).pause(tasks); break;
            case GROUP: this.ghome.find(id).pause(tasks); break;
            default: throw new ManageException(
                                "Unknown/unhandled type: " + trType(type));
        }
    }

    public void serialize(String id, int type,
                          ShutdownTasks tasks, Caller caller)
            throws DoesNotExistException,
                   ManageException,
                   OperationDisabledException {

        this.opIntake("SERIALIZE", id, type, caller);

        switch (type) {
            case INSTANCE: this.home.find(id).serialize(tasks); break;
            case GROUP: this.ghome.find(id).serialize(tasks); break;
            default: throw new ManageException(
                                "Unknown/unhandled type: " + trType(type));
        }
    }

    public void reboot(String id, int type,
                       ShutdownTasks tasks, Caller caller)
            throws DoesNotExistException,
                   ManageException,
                   OperationDisabledException {

        this.opIntake("REBOOT", id, type, caller);
        
        switch (type) {
            case INSTANCE: this.home.find(id).reboot(tasks); break;
            case GROUP: this.ghome.find(id).reboot(tasks); break;
            default: throw new ManageException(
                                "Unknown/unhandled type: " + trType(type));
        }
    }

    public void coscheduleDone(String id, Caller caller)
            
            throws DoesNotExistException,
                   ManageException,
                   CoSchedulingException {

        this.opIntake("DONE", id, COSCHEDULED, caller);
        this.cohome.find(id).done();
    }


    // -------------------------------------------------------------------------
    // INFORMATION QUERIES
    // -------------------------------------------------------------------------

    public boolean exists(String id, int type) {
        try {
            return this._exists(id, type);
        } catch (DoesNotExistException e) {
            return false;
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.error(e.getMessage(), e);
            } else {
                logger.error(e.getMessage());
            }
            return false;
        }
    }

    public Calendar getDestructionTime(String id, int type)
            throws DoesNotExistException, ManageException {
        
        switch (type) {
            case INSTANCE: return this.home.find(id).getTerminationTime();
            case GROUP: return null;
            case COSCHEDULED: return null;
            default: throw new ManageException(
                                "Unknown/unhandled type: " + trType(type));
        }
    }

    public Caller[] getAuthorizedManagers(String id, int type)
            throws DoesNotExistException, ManageException {

        final String creatorID = this._getManagerID(id, type);
        if (creatorID == null) {
            throw new ManageException("creatorID may not be null");
        }
        
        final _Caller caller = this.repr._newCaller();
        caller.setIdentity(creatorID);
        return new Caller[]{caller};
    }

    public VM getInstance(String id)
            throws DoesNotExistException, ManageException {

        try {
            return this.getVM(this.home.find(id));
        } catch (CannotTranslateException e) {
            throw new ManageException(e.getMessage(), e);
        }
    }

    public VM[] getAll(String id, int type)
            throws DoesNotExistException, ManageException {
        
        switch (type) {
            case INSTANCE:
                final VM vm = this.getInstance(id);
                return new VM[]{vm};
            
            case GROUP:
                return this.getGroup(id);

            case COSCHEDULED:
                return this.getCosched(id);

            default: throw new ManageException(
                                "Unknown/unhandled type: " + trType(type));
        }
    }

    public VM[] getAllByCaller(Caller caller)
            throws ManageException {

        this.opIntakeGeneralOp("ALL-OWNED-VMS", caller);

        try {
            final InstanceResource[] rsrcs =
                    this.home.findByCaller(caller.getIdentity());

            return this.getInstances(rsrcs);
            
        } catch (CannotTranslateException e) {
            throw new ManageException(e.getMessage(), e);
        }
    }

    public VM[] getAllByIPAddress(String ip) throws ManageException {

        if (ip == null) {
            throw new ManageException("invalid, ip may not be null");
        }
        
        try {
            final InstanceResource[] rsrcs = this.home.findByIP(ip);

            return this.getInstances(rsrcs);

        } catch (CannotTranslateException e) {
            throw new ManageException(e.getMessage(), e);
        }
    }

    public VM[] getGlobalAll() throws ManageException {
        try {
            final InstanceResource[] rsrcs = this.home.findAll();

            return this.getInstances(rsrcs);

        } catch (CannotTranslateException e) {
            throw new ManageException(e.getMessage(), e);
        }
    }

    public Usage getCallerUsage(Caller caller) throws ManageException {

        this.opIntakeGeneralOp("ALL-USAGE", caller);

        if (this.accounting == null) {
            throw new ManageException("not able to query usage, there is " +
                    "no accounting plugin configured");
        }

        final String id;

        // already checked in opIntake but guard against object extenders
        if (caller == null || caller.getIdentity() == null) {
            throw new ManageException("not able to query usage, there is " +
                    "no caller information");
        } else {
            id = caller.getIdentity();
        }

        try {
            final ElapsedAndReservedMinutes earm =
                    this.accounting.totalElapsedAndReservedMinutesTuple(id);
            return this.dataConvert.getUsage(earm);
        } catch (Exception e) {
            throw new ManageException(e.getMessage(), e);
        }
    }

    public Advertised getAdvertised() {
        return this.creation.getAdvertised();
    }

    public void registerStateChangeListener(String id,
                                            int type,
                                            StateChangeCallback listener)

            throws ManageException, DoesNotExistException {
        
        switch (type) {
            case INSTANCE:
                this.home.find(id).registerStateChangeListener(listener);
                break;
            default:
                throw new ManageException(
                        "Unknown/unhandled type: " + trType(type));
        }
    }

    public void registerDestructionListener(String id,
                                            int type,
                                            DestructionCallback listener)
            
            throws ManageException, DoesNotExistException {

        switch (type) {
            case INSTANCE:
                this.home.find(id).registerDestructionListener(listener);
                break;
            default:
                throw new ManageException(
                        "Unknown/unhandled type: " + trType(type));
        }
    }


    // -------------------------------------------------------------------------
    // OTHER (doesn't implement Manager)
    // -------------------------------------------------------------------------

    protected boolean _exists(String id, int type) throws Exception {

        if (this.lager.traceLog) {
            logger.trace(this.traceString(id,type) + " exists?");
        }

        // *home.find() will return DoesNotExistException or return
        switch (type) {
            case INSTANCE: this.home.find(id); return true;
            case GROUP: this.ghome.find(id); return true;
            case COSCHEDULED: this.cohome.find(id); return true;
            default: throw new ManageException(
                                "Unknown/unhandled type: " + trType(type));
        }
    }

    protected String traceString(String id, int type) {
        switch (type) {
            case INSTANCE:
                try {
                    return Lager.id(this.home.convertID(id));
                } catch (ManageException e) {
                    return "invalid-instance-id '" + id + "'?]";
                }
            case GROUP:
                return Lager.groupid(id);
            case COSCHEDULED:
                return Lager.ensembleid(id);
            default:
                return "[unknown-type " + type + "?]";
        }
    }
    
    /**
     * Currently only supporting one ID string (creator) for authorized
     * managers.
     *
     * @param id ID
     * @param type ID-type
     * @return creator ID
     * @throws ManageException problem
     * @throws DoesNotExistException unknown id
     */
    protected String _getManagerID(String id, int type)
            throws ManageException, DoesNotExistException {

        switch (type) {
            case INSTANCE: return this.home.find(id).getCreatorID();
            case GROUP: return this.ghome.find(id).getCreatorID();
            case COSCHEDULED: return this.cohome.find(id).getCreatorID();
            default: throw new ManageException(
                                "Unknown/unhandled type: " + trType(type));
        }
    }

    /**
     * @param type integer id-type
     * @return string representation of id-type for errors, etc.
     */
    protected static String trType(int type) {
        switch (type) {
            case INSTANCE: return "instance";
            case GROUP: return "group";
            case COSCHEDULED: return "coscheduled";
            default: return "UNKNOWN: " + type;
        }
    }

    protected void opIntakeGeneralOp(String opName, Caller caller)
            throws ManageException {
        this._opIntake(opName, null, -1, caller, true);
    }

    protected void opIntake(String opName, String id, int type, Caller caller)
            throws ManageException {
        this._opIntake(opName, id, type, caller, false);
    }

    protected void _opIntake(String opName,
                             String id,
                             int type,
                             Caller caller,
                             boolean generalOp)
            throws ManageException {

        // these things should all be present even if event log is off,
        // this method serves as initial validation as well as optionally
        // doing event log
        
        if (opName == null) {
            throw new ManageException("No operation name");
        }

        if (caller == null) {
            throw new ManageException("No caller information");
        }

        final String callerID = caller.getIdentity();

        if (callerID == null) {
            throw new ManageException("No caller identity information");
        }

        if (!this.lager.eventLog) {
            return; // *** EARLY RETURN ***
        }
        
        final String prefix;

        if (generalOp) {
            
            prefix = "";

        } else {

            switch (type) {

                case INSTANCE:
                    prefix = Lager.ev(this.home.convertID(id));
                    break;

                case GROUP:
                    prefix = Lager.groupev(id);
                    break;

                case COSCHEDULED:
                    prefix = Lager.ensembleev(id);
                    break;

                default: throw new ManageException(
                                    "Unknown/unhandled type: " + trType(type));
            }
        }

        logger.info(prefix + opName + " called by '" + callerID + "'");
    }



    // -------------------------------------------------------------------------
    // TYPE TRANSLATION
    // -------------------------------------------------------------------------

    /**
     * Translate internal objects to the main VM representation.
     *
     * This will go away.  We can start using VM structures internally, but
     * did not want to change too much at once in phase I.
     *
     * @param resource WorkspaceResource
     * @return VM
     * @throws CannotTranslateException invalid/unexpected
     */
    protected VM getVM(InstanceResource resource)
            throws CannotTranslateException {
        
        if (resource == null) {
            throw new CannotTranslateException("resource is missing");
        }

        return this.dataConvert.getVM(resource);
    }

    protected VM[] getInstances(InstanceResource[] resources)
            throws CannotTranslateException {

        if (resources == null) {
            return null;
        }

        if (resources.length == 0) {
            return EMPTY_VM_ARRAY;
        }

        final VM[] vms = new VM[resources.length];
        for (int i = 0; i < resources.length; i++) {
            vms[i] = this.getVM(resources[i]);
        }
        return vms;
    }

    protected VM[] getGroup(String groupid)
            throws ManageException,
                   DoesNotExistException {

        // null groupid is checked underneath
        final InstanceResource[] resources = this.ghome.findMembers(groupid);

        try {
            return this.getInstances(resources);
        } catch (CannotTranslateException e) {
            throw new ManageException(e.getMessage(), e);
        }
    }

    protected VM[] getCosched(String coschedid)
            throws ManageException,
                   DoesNotExistException {

        // null coschedid is checked underneath
        final InstanceResource[] resources = this.cohome.findMembers(coschedid);

        try {
            return this.getInstances(resources);
        } catch (CannotTranslateException e) {
            throw new ManageException(e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // SPOT INSTANCES OPERATIONS
    // -------------------------------------------------------------------------        

    public SpotRequest requestSpotInstances(RequestSI req, Caller caller)
            throws AuthorizationException, CoSchedulingException,
                   CreationException, MetadataException,
                   ResourceRequestDeniedException, SchedulingException {

        SIRequest siRequest = this.creation.requestSpotInstances(req, caller);
        
        try {
            return dataConvert.getSpotRequest(siRequest);
        } catch (CannotTranslateException e) {
            throw new MetadataException("Could not translate request from internal representation to RM API representation.", e);
        }
    }     
    
    public Double getSpotPrice() throws ManageException {
        return siHome.getSpotPrice();
    }


    @Override
    public SpotRequest getSpotRequest(String requestID, Caller caller)
            throws DoesNotExistException, ManageException, AuthorizationException {
        return this.getSpotRequests(new String[]{requestID}, caller)[0];
    }    
    
    public SpotRequest[] getSpotRequests(String[] ids, Caller caller)
            throws DoesNotExistException, ManageException, AuthorizationException {
        
        SpotRequest[] result = new SpotRequest[ids.length];
        
        for (int i = 0; i < ids.length; i++) {
            SIRequest siReq = siHome.getRequest(ids[i]);
            
            if(!caller.isSuperUser() && !siReq.getCaller().equals(caller)){
                throw new AuthorizationException("Caller is not authorized to get information about this request");
            }
            
            result[i] = getSpotRequest(siReq);
        }
        
        return result;
    }


    public SpotRequest[] getSpotRequestByCaller(Caller caller)
            throws ManageException {
        
        return this.getSpotRequests(siHome.getRequests(caller));
    }


    @Override
    public SpotRequest[] cancelSpotInstanceRequests(String[] ids, Caller caller) 
            throws DoesNotExistException, AuthorizationException, ManageException {
        SpotRequest[] result = new SpotRequest[ids.length];
        
        for (int i = 0; i < ids.length; i++) {
            SIRequest siReq = siHome.getRequest(ids[i]);
            
            if(!caller.isSuperUser() && !siReq.getCaller().equals(caller)){
                throw new AuthorizationException("Caller is not authorized to get information about this request");
            }
            
            result[i] = getSpotRequest(siHome.cancelRequest(ids[i]));
        }
        
        return result;
    }
    
    private SpotRequest getSpotRequest(SIRequest siReq) throws ManageException {
        try {
            return dataConvert.getSpotRequest(siReq);
        } catch (CannotTranslateException e) {
            throw new ManageException(e.getMessage(), e);
        }
    }    
    
    private SpotRequest[] getSpotRequests(SIRequest[] requests) throws ManageException{
        SpotRequest[] result = new SpotRequest[requests.length];
        
        for (int i = 0; i < requests.length; i++) {
            result[i] = getSpotRequest(requests[i]);
        }
        
        return result;
    }    

}
