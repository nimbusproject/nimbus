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

package org.globus.workspace.persistence;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;

import org.globus.workspace.WorkspaceConstants;
import org.globus.workspace.accounting.ElapsedAndReservedMinutes;
import org.globus.workspace.async.AsyncRequest;
import org.globus.workspace.async.AsyncRequestStatus;
import org.globus.workspace.service.InstanceResource;
import org.globus.workspace.service.binding.vm.VirtualMachine;
import org.globus.workspace.service.binding.vm.VirtualMachineDeployment;
import org.globus.workspace.service.binding.vm.VirtualMachinePartition;
import org.globus.workspace.xen.XenUtil;
import org.nimbustools.api._repr._Caller;
import org.nimbustools.api._repr._RequestInfo;
import org.nimbustools.api._repr._SpotRequestInfo;
import org.nimbustools.api._repr._Usage;
import org.nimbustools.api._repr.si._SIRequestState;
import org.nimbustools.api._repr.vm._NIC;
import org.nimbustools.api._repr.vm._ResourceAllocation;
import org.nimbustools.api._repr.vm._Schedule;
import org.nimbustools.api._repr.vm._State;
import org.nimbustools.api._repr.vm._VM;
import org.nimbustools.api._repr.vm._VMFile;
import org.nimbustools.api.defaults.repr.si.DefaultSIRequestState;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.CannotTranslateException;
import org.nimbustools.api.repr.ReprFactory;
import org.nimbustools.api.repr.RequestInfo;
import org.nimbustools.api.repr.SpotRequestInfo;
import org.nimbustools.api.repr.Usage;
import org.nimbustools.api.repr.si.RequestState;
import org.nimbustools.api.repr.vm.NIC;
import org.nimbustools.api.repr.vm.ResourceAllocation;
import org.nimbustools.api.repr.vm.Schedule;
import org.nimbustools.api.repr.vm.State;
import org.nimbustools.api.repr.vm.VM;
import org.nimbustools.api.repr.vm.VMConstants;
import org.nimbustools.api.repr.vm.VMFile;

/**
 * Most translation should be able to go away as we use the new RM API's
 * objects internally in the service even more (the state related stuff
 * will not since the service keeps track of many more states than the
 * remote view).
 */
public class DataConvert implements WorkspaceConstants {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    protected static final Hashtable nameTable = new Hashtable(32);
    private static final Hashtable statusMap = new Hashtable(32);

    static {
        nameTable.put(new Integer(STATE_INVALID), "Invalid");
        nameTable.put(new Integer(STATE_SCHEDULED_ONLY), "ScheduledOnly");
        nameTable.put(new Integer(STATE_UNSTAGED), "Unstaged");
        nameTable.put(new Integer(STATE_STAGING_IN), "StagingIn");
        nameTable.put(new Integer(STATE_UNPROPAGATED), "Unpropagated");
        nameTable.put(new Integer(STATE_PROPAGATING), "Propagating");
        nameTable.put(new Integer(STATE_PROPAGATING_TO_START), "PropagatingToStart");
        nameTable.put(new Integer(STATE_PROPAGATING_TO_PAUSE), "PropagatingToPause");

        nameTable.put(new Integer(STATE_PROPAGATED), "Propagated");
        nameTable.put(new Integer(STATE_STARTING), "Starting");
        nameTable.put(new Integer(STATE_STARTED), "Started");
        nameTable.put(new Integer(STATE_SERIALIZING), "Serializing");
        nameTable.put(new Integer(STATE_SERIALIZED), "Serialized");
        nameTable.put(new Integer(STATE_PAUSING), "Pausing");
        nameTable.put(new Integer(STATE_PAUSED), "Paused");
        nameTable.put(new Integer(STATE_REBOOT), "Reboot");
        nameTable.put(new Integer(STATE_SHUTTING_DOWN), "ShuttingDown");

        nameTable.put(new Integer(STATE_READYING_FOR_TRANSPORT), "ReadyingForTransport");
        nameTable.put(new Integer(STATE_READY_FOR_TRANSPORT), "ReadyForTransport");
        nameTable.put(new Integer(STATE_STAGING_OUT), "StagingOut");
        nameTable.put(new Integer(STATE_STAGED_OUT), "StagedOut");

        nameTable.put(new Integer(STATE_CANCELLING_STAGING_IN), "CancellingStagingIn");
        nameTable.put(new Integer(STATE_CANCELLING_UNPROPAGATED), "CancellingUnpropagated");
        nameTable.put(new Integer(STATE_CANCELLING_PROPAGATING), "CancellingPropagating");
        nameTable.put(new Integer(STATE_CANCELLING_PROPAGATING_TO_START), "CancellingPropagatingToStart");
        nameTable.put(new Integer(STATE_CANCELLING_PROPAGATING_TO_PAUSE), "CancellingPropagatingToPause");
        nameTable.put(new Integer(STATE_CANCELLING_READYING_FOR_TRANSPORT), "CancellingTransportReadying");
        nameTable.put(new Integer(STATE_CANCELLING_AT_VMM), "CancellingAtVMM");
        nameTable.put(new Integer(STATE_CANCELLING_READYING_FOR_TRANSPORT), "CancellingReadingForTransport");
        nameTable.put(new Integer(STATE_CANCELLING_READY_FOR_TRANSPORT), "CancellingReadyForTransport");
        nameTable.put(new Integer(STATE_CANCELLING_STAGING_OUT), "CancellingStagingOut");

        nameTable.put(new Integer(STATE_DESTROYING), "Destroying");
        nameTable.put(new Integer(STATE_DESTROY_SUCCEEDED), "DestroySucceeded");
        nameTable.put(new Integer(STATE_DESTROY_FAILED), "DestroyFailed");
        nameTable.put(new Integer(STATE_CORRUPTED_GENERIC), "Corrupted");

        // --------------------------------------------------------------

        statusMap.put(new Integer(STATE_SCHEDULED_ONLY),
                State.STATE_Unpropagated);
        statusMap.put(new Integer(STATE_UNSTAGED),
                State.STATE_Unpropagated);
        statusMap.put(new Integer(STATE_STAGING_IN),
                State.STATE_Unpropagated);
        statusMap.put(new Integer(STATE_UNPROPAGATED),
                State.STATE_Unpropagated);
        statusMap.put(new Integer(STATE_PROPAGATING),
                State.STATE_Unpropagated);
        statusMap.put(new Integer(STATE_PROPAGATING_TO_START),
                State.STATE_Unpropagated);
        statusMap.put(new Integer(STATE_PROPAGATING_TO_PAUSE),
                State.STATE_Unpropagated);

        statusMap.put(new Integer(STATE_PROPAGATED),
                State.STATE_Propagated);
        statusMap.put(new Integer(STATE_STARTING),
                State.STATE_Propagated);  // need knowledge of past for good client visible match
        statusMap.put(new Integer(STATE_STARTED),
                State.STATE_Running);
        statusMap.put(new Integer(STATE_SERIALIZING),
                State.STATE_Running);
        statusMap.put(new Integer(STATE_SERIALIZED),
                State.STATE_Propagated);
        statusMap.put(new Integer(STATE_PAUSING),
                State.STATE_Running);
        statusMap.put(new Integer(STATE_PAUSED),
                State.STATE_Paused);
        statusMap.put(new Integer(STATE_REBOOT),
                State.STATE_Running);
        statusMap.put(new Integer(STATE_SHUTTING_DOWN),
                State.STATE_Running);  // need knowledge of past for good client visible match

        statusMap.put(new Integer(STATE_READYING_FOR_TRANSPORT),
                State.STATE_TowardsTransportReady);
        statusMap.put(new Integer(STATE_READY_FOR_TRANSPORT),
                State.STATE_TransportReady);
        statusMap.put(new Integer(STATE_STAGING_OUT),
                State.STATE_TransportReady);
        statusMap.put(new Integer(STATE_STAGED_OUT),
                State.STATE_TransportReady);

        statusMap.put(new Integer(STATE_CANCELLING_STAGING_IN),
                State.STATE_Cancelled);
        statusMap.put(new Integer(STATE_CANCELLING_UNPROPAGATED),
                State.STATE_Cancelled);
        statusMap.put(new Integer(STATE_CANCELLING_PROPAGATING),
                State.STATE_Cancelled);
        statusMap.put(new Integer(STATE_CANCELLING_PROPAGATING_TO_START),
                State.STATE_Cancelled);
        statusMap.put(new Integer(STATE_CANCELLING_PROPAGATING_TO_PAUSE),
                State.STATE_Cancelled);
        statusMap.put(new Integer(STATE_CANCELLING_AT_VMM),
                State.STATE_Cancelled);
        statusMap.put(new Integer(STATE_CANCELLING_READYING_FOR_TRANSPORT),
                State.STATE_Cancelled);
        statusMap.put(new Integer(STATE_CANCELLING_READY_FOR_TRANSPORT),
                State.STATE_Cancelled);
        statusMap.put(new Integer(STATE_CANCELLING_STAGING_OUT),
                State.STATE_Cancelled);

        statusMap.put(new Integer(STATE_DESTROYING),
                State.STATE_Cancelled);
        statusMap.put(new Integer(STATE_DESTROY_SUCCEEDED),
                State.STATE_Cancelled);
        statusMap.put(new Integer(STATE_DESTROY_FAILED),
                State.STATE_Cancelled);
    }


    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected final ReprFactory repr;
    private static final VMFile[] EMPTY_VM_FILES = new VMFile[0];

    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    public DataConvert(ReprFactory reprFactory) {
        if (reprFactory == null) {
            throw new IllegalArgumentException("reprFactory may not be null");
        }
        this.repr = reprFactory;
    }


    // -------------------------------------------------------------------------
    // ALL AT ONCE
    // -------------------------------------------------------------------------

    public VM getVM(InstanceResource resource) throws CannotTranslateException {

        if (resource == null) {
            throw new CannotTranslateException("no resource?");
        }

        final _VM vm = this.repr._newVM();
        vm.setID(String.valueOf(resource.getID()));
        vm.setGroupID(resource.getGroupId());
        vm.setCoschedID(resource.getEnsembleId());
        vm.setLaunchIndex(resource.getLaunchIndex());
        vm.setNics(this.getNICs(resource.getVM()));
        vm.setMdUserData(resource.getVM().getMdUserData());
        vm.setCredentialName(resource.getVM().getCredentialName());
        vm.setVMFiles(this.getStorage(resource.getVM()));
        vm.setResourceAllocation(this.getRA(resource.getVM()));
        vm.setSchedule(this.getSchedule(resource));
        vm.setState(this.getState(resource));
        vm.setCreator(this.getCreator(resource));
        vm.setClientToken(resource.getClientToken());
        vm.setDetails(this.getDetails(resource.getVM()));
        
        if(resource.getVM().isPreemptable()){
            vm.setLifeCycle(VMConstants.LIFE_CYCLE_SPOT);
        }
        
        return vm;
    }
    
    public RequestInfo getRequestInfo(AsyncRequest backfillRequest) throws CannotTranslateException {
        VirtualMachine[] bindings = backfillRequest.getBindings();
        
        if (bindings == null || bindings.length == 0) {
            throw new CannotTranslateException("no resource?");
        }        
        
        final _RequestInfo result = repr._newRequestInfo();
        
        populate(backfillRequest, result);
        
        return result;
    }    
    
    public SpotRequestInfo getSpotRequest(AsyncRequest siRequest) throws CannotTranslateException {

        VirtualMachine[] bindings = siRequest.getBindings();
        
        if (bindings == null || bindings.length == 0) {
            throw new CannotTranslateException("no resource?");
        }        
        
        final _SpotRequestInfo result = repr._newSpotRequestInfo();

        populate(siRequest, result);        
        
        result.setPersistent(siRequest.isPersistent());
        result.setSpotPrice(siRequest.getMaxBid());        
                
        return result;
    }


    private void populate(AsyncRequest asyncReq, final _RequestInfo result) throws CannotTranslateException {
        
        VirtualMachine[] bindings = asyncReq.getBindings();
        
        result.setInstanceCount(asyncReq.getRequestedInstances());
        result.setCreationTime(asyncReq.getCreationTime());
        result.setGroupID(asyncReq.getGroupID());
        result.setRequestID(asyncReq.getId());
        result.setVMFiles(this.getStorage(bindings[0]));
        result.setSshKeyName(asyncReq.getSshKeyName());
        result.setVMIds(getVMIDs(asyncReq.getVMIds()));
        
        _SIRequestState state = new DefaultSIRequestState();
        state.setState(this.getSIRequestState(asyncReq.getStatus()));
        result.setState(state);
        
        //FIXME remove if not used
        //result.setMdUserData(bindings[0].getMdUserData());
        //result.setResourceAllocation(this.getRA(bindings[0]));  
        //result.setCreator(asyncReq.getCaller());        
    }


    private String[] getVMIDs(Collection<Integer> vmIds) {
        String[] ids = new String[vmIds.size()];
        Iterator<Integer> iterator = vmIds.iterator();
        for (int i = 0; i < vmIds.size(); i++) {
            ids[i] = String.valueOf(iterator.next());
        }
        return ids;
    }    
    
    
    // -------------------------------------------------------------------------
    // STATE RELATED
    // -------------------------------------------------------------------------

    private String getSIRequestState(AsyncRequestStatus status) {
        switch(status){
        case ACTIVE:
            return RequestState.STATE_Active;
        case CANCELLED:
            return RequestState.STATE_Canceled;
        case CLOSED:
            return RequestState.STATE_Closed;
        case FAILED:
            return RequestState.STATE_Failed;
        case OPEN:
            return RequestState.STATE_Open;
        }
        
        return null;
    }


    /**
     * @param state internal state position
     * @return state name string
     */
    public String stateName(int state) {

        if (state >= STATE_CORRUPTED && state <= STATE_LAST_LEGAL) {

            final int oldstate = state - STATE_CORRUPTED;
            final String oldname =
                    (String) nameTable.get(new Integer(oldstate));

            return "Corrupted-" + oldname;
        }

        return (String) nameTable.get(new Integer(state));
    }

    public State getState(InstanceResource resource)
            throws CannotTranslateException {

        if (resource == null) {
            throw new CannotTranslateException("no resource?");
        }

        final int num = resource.getState();

        final String stateStr;
        if (num >= STATE_CORRUPTED_GENERIC && num <= STATE_LAST_LEGAL) {
            stateStr = State.STATE_Corrupted;
        } else {
            stateStr = (String) statusMap.get(new Integer(num));
        }

        if (stateStr == null) {
            throw new CannotTranslateException("state string is required");
        }

        final _State state = this.repr._newState();
        state.setState(stateStr);
        state.setProblem(resource.getStateThrowable()); // can be null

        return state;
    }


    // -------------------------------------------------------------------------
    // NETWORK RELATED
    // -------------------------------------------------------------------------

    // TODO: move to sane network representation
    // need for this will go away as we migrate
    public NIC[] getNICs(VirtualMachine vm) throws CannotTranslateException {

        if (vm == null) {
            throw new CannotTranslateException("no vm?");
        }

        final String network = vm.getNetwork();

        if (network == null) {
            throw new CannotTranslateException("no network?");
        }

        return getNICs(network);
    }

    public NIC[] getNICs(String network) throws CannotTranslateException {

        // Create objects

        if (network.equalsIgnoreCase("NONE")) {
            return null;
        }

        // Parse network string. Each NIC is separated by ";;"
        final String[] nicsStr = network.split(XenUtil.WC_GROUP_SEPARATOR);

        // this cast is safe
        final int numNics = nicsStr.length;

        // Create Nic_Type object for each NIC
        final _NIC[] nics = new _NIC[numNics];

        for(int i = 0; i < numNics; i++) {
            
            final _NIC nic = this.repr._newNIC();
            nics[i] = nic;
            
            final String[] nicPropertiesStr =
                    nicsStr[i].split(XenUtil.WC_FIELD_SEPARATOR);
            final int numProps = nicPropertiesStr.length;

            // NIC string format:
            // Name;Assocaition;MAC;Network Mode;IP method
            //      ;IP address;gateway;broadcast;subnetmask;dns;hostname
            //      ;null;null;null;null  (maintain old protocol)
            // First five are assumed to exist

            // Name
            nic.setName(nicPart(nicPropertiesStr[0]));

            // MAC
            nic.setMAC(nicPart(nicPropertiesStr[2]));

            // IP config
            nic.setAcquisitionMethod(nicPropertiesStr[4]);
            
            if (numProps > 4) {

                nic.setIpAddress(nicPart(nicPropertiesStr[5]));
                nic.setGateway(nicPart(nicPropertiesStr[6]));
                nic.setBroadcast(nicPart(nicPropertiesStr[7]));
                nic.setNetmask(nicPart(nicPropertiesStr[8]));
                nic.setHostname(nicPart(nicPropertiesStr[10]));

            } else {
                
                nic.setIpAddress(null);
                nic.setGateway(null);
                nic.setBroadcast(null);
                nic.setNetmask(null);
                nic.setHostname(null);
            }
            // Currently, no more information is
            // bound internally, setting to null..
            nic.setNetwork(null);

            // Network (old name: Association)
            nic.setNetworkName(nicPart(nicPropertiesStr[1]));
        }

        return nics;
    }
    
    private static String nicPart(String part) {
        final String notset = "null";
        if (part == null) {
            return null;
        }
        if (part.equals(notset)) {
            return null;
        }
        return part;
    }


    public String nicsAsString(NIC[] nics) {

        // NIC string format:
        // Name;Assocaition;MAC;Network Mode;IP method
        //      ;IP address;gateway;broadcast;subnetmask;dns;hostname
        //      ;null;null;null;null  (maintain old protocol)

        String nicString = "";
        for (NIC nic : nics) {
            nicString += nic.getName() + ";";
            nicString += nic.getNetworkName() + ";";
            nicString += nic.getMAC() + ";";
            nicString += "null" + ";"; //No network mode
            nicString += nic.getAcquisitionMethod() + ";";
            nicString += nic.getIpAddress() + ";";
            nicString += nic.getGateway() + ";";
            nicString += nic.getBroadcast() + ";";
            nicString += nic.getNetmask() + ";";
            nicString += "null" + ";"; //No dns
            nicString += nic.getHostname() + ";";
            nicString += "null;null;null";

            if (nic != nics[nics.length-1]) {
                nicString += ";;";
            }

        }
        return nicString;
    }


    // -------------------------------------------------------------------------
    // ALLOCATION RELATED
    // -------------------------------------------------------------------------

    // need for this will go away as we migrate
    public ResourceAllocation getRA(VirtualMachine vm)

            throws CannotTranslateException {
        
        if (vm == null) {
            throw new CannotTranslateException("null VirtualMachine?");
        }

        final _ResourceAllocation ra = this.repr._newResourceAllocation();

        final VirtualMachineDeployment dep = vm.getDeployment();
        
        if (dep == null) {
            return ra; // *** EARLY RETURN ***
        }

        ra.setSpotInstance(vm.isPreemptable());
        ra.setArchitecture(dep.getCPUArchitecture());
        ra.setCpuPercentage(dep.getCPUPercentage());
        ra.setIndCpuSpeed(dep.getIndividualCPUSpeed());
        ra.setIndCpuCount(dep.getIndividualCPUCount());
        ra.setMemory(dep.getIndividualPhysicalMemory());
        return ra;
    }

    private String getDetails(VirtualMachine vm)
            throws CannotTranslateException {

        if (vm == null) {
            throw new CannotTranslateException("null VirtualMachine?");
        }
        return vm.getNode();
    }

    // -------------------------------------------------------------------------
    // SCHEDULE RELATED
    // -------------------------------------------------------------------------

    // need for this will go away as we migrate
    public Schedule getSchedule(InstanceResource resource)

            throws CannotTranslateException {

        if (resource == null) {
            throw new CannotTranslateException("no resource?");
        }

        final _Schedule schedule = this.repr._newSchedule();

        schedule.setDestructionTime(resource.getTerminationTime());
        schedule.setStartTime(resource.getStartTime());

        final VirtualMachine vm = resource.getVM();
        if (vm == null) {
            throw new CannotTranslateException("null VirtualMachine?");
        }
        final VirtualMachineDeployment dep = vm.getDeployment();
        if (dep == null) {
            throw new CannotTranslateException("null deployment information?");
        }
        schedule.setDurationSeconds(dep.getMinDuration());

        return schedule;
    }


    // -------------------------------------------------------------------------
    // STORAGE
    // -------------------------------------------------------------------------

    public VMFile[] getStorage(VirtualMachine vm)
            throws CannotTranslateException {

        if (vm == null) {
            throw new CannotTranslateException("null VirtualMachine?");
        }

        final VirtualMachinePartition[] partitions = vm.getPartitions();
        if (partitions == null || partitions.length == 0) {
            return EMPTY_VM_FILES;
        }

        final VMFile[] files = new _VMFile[partitions.length];
        for (int i = 0; i < partitions.length; i++) {
            files[i] = this.getOneFile(partitions[i]);
        }
        return files;
    }

    // TODO:
    // VMFile <--> VirtualMachinePartition is one of the first things we
    // will switch over to using natively, they are basically identical.
    public VMFile getOneFile(VirtualMachinePartition part)
            throws CannotTranslateException {

        if (part == null) {
            throw new CannotTranslateException("null partition");
        }

        final _VMFile vmFile = this.repr._newVMFile();

        vmFile.setRootFile(part.isRootdisk());
        if (part.isReadwrite()) {
            vmFile.setDiskPerms(VMFile.DISKPERMS_ReadWrite);
        } else {
            vmFile.setDiskPerms(VMFile.DISKPERMS_ReadOnly);
        }
        vmFile.setMountAs(part.getImagemount());

        final int size = part.getBlankspace();
        if (size > 0) {
            vmFile.setBlankSpaceSize(size);
            vmFile.setBlankSpaceName(null); // unknown
        }

        try {
            final String unpropTargetStr = part.getAlternateUnpropTarget();
            if (unpropTargetStr != null) {
                vmFile.setUnpropURI(new URI(unpropTargetStr));
            }
            vmFile.setURI(new URI(part.getImage()));
        } catch (URISyntaxException e) {
            throw new CannotTranslateException(e.getMessage(), e);
        }

        return vmFile;
    }


    // -------------------------------------------------------------------------
    // USAGE
    // -------------------------------------------------------------------------

    public Usage getUsage(ElapsedAndReservedMinutes earm)

            throws CannotTranslateException {

        if (earm == null) {
            throw new CannotTranslateException("no usage information");
        }

        final _Usage usage = this.repr._newUsage();
        usage.setElapsedMinutes(earm.getElapsed());
        usage.setReservedMinutes(earm.getReserved());
        return usage;
    }


    // -------------------------------------------------------------------------
    // CREATOR
    // -------------------------------------------------------------------------

    public Caller getCreator(InstanceResource resource)

            throws CannotTranslateException {
        
        if (resource == null) {
            throw new CannotTranslateException("no resource?");
        }

        final String creatorID = resource.getCreatorID();

        if (creatorID == null) {
            return null;
        }

        final _Caller creator = this.repr._newCaller();
        creator.setIdentity(creatorID);
        return creator;
    }
}
