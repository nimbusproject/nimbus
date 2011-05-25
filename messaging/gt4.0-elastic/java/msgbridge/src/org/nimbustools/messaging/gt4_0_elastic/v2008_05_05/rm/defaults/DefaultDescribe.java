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

package org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.rm.defaults;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nimbustools.api.repr.CannotTranslateException;
import org.nimbustools.api.repr.vm.VM;
import org.nimbustools.api.repr.vm.NIC;
import org.nimbustools.api.repr.vm.VMFile;
import org.nimbustools.api.repr.vm.State;
import org.nimbustools.api.repr.vm.ResourceAllocation;
import org.nimbustools.api.repr.vm.Schedule;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.*;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.general.Validity;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.general.Networks;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.general.ResourceAllocations;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.general.StateMap;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.general.Kernels;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.general.AvailabilityZones;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.rm.Describe;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.rm.IDMappings;
import org.safehaus.uuid.UUIDGenerator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Calendar;
import java.net.URI;

public class DefaultDescribe implements Describe {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(DefaultDescribe.class.getName());

    private static final String[]
            EMPTY_STRING_ARRAY = new String[0];

    private static final ReservationInfoType[]
            EMPTY_RESERVATION_INFO_TYPE = new ReservationInfoType[0];

    
    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected final IDMappings ids;
    protected final Validity validity;
    protected final Networks networks;
    protected final Kernels kernels;
    protected final ResourceAllocations RAs;
    protected final AvailabilityZones zones;
    protected final UUIDGenerator uuidGen = UUIDGenerator.getInstance();

    
    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    public DefaultDescribe(IDMappings idMappings,
                           Validity validityImpl,
                           Networks networksImpl,
                           ResourceAllocations rasImpl,
                           Kernels kernelsImpl,
                           AvailabilityZones zonesImpl) {
        
        if (idMappings == null) {
            throw new IllegalArgumentException("idMappings may not be null");
        }
        this.ids = idMappings;

        if (validityImpl == null) {
            throw new IllegalArgumentException("validityImpl may not be null");
        }
        this.validity = validityImpl;

        if (networksImpl == null) {
            throw new IllegalArgumentException("networksImpl may not be null");
        }
        this.networks = networksImpl;

        if (rasImpl == null) {
            throw new IllegalArgumentException("rasImpl may not be null");
        }
        this.RAs = rasImpl;
        
        if (kernelsImpl == null) {
            throw new IllegalArgumentException("kernelsImpl may not be null");
        }
        this.kernels = kernelsImpl;
        
        if (zonesImpl == null) {
            throw new IllegalArgumentException("zonesImpl may not be null");
        }
        this.zones = zonesImpl;
    }


    // -------------------------------------------------------------------------
    // implements Describe
    // -------------------------------------------------------------------------

    /*
      From ec2 devguide 2008-02-01:
      
        "If you specify one or more instance IDs, Amazon EC2 returns information
         for those instances. If you do not specify instance IDs, Amazon EC2
         returns information for all relevant instances. If you specify an
         invalid instance ID, a fault is returned. If you specify an instance
         that you do not own, it will not be included in the returned results."
     */

    public DescribeInstancesResponseType translate(VM[] vms,
                                                   String[] instanceIDs,
                                                   String ownerID)
            throws CannotTranslateException {

        boolean scopedQuery = false;
        if (instanceIDs != null && instanceIDs.length > 0) {
            scopedQuery = true;
            for (int i = 0; i < instanceIDs.length; i++) {
                final String instanceID = instanceIDs[i];
                if (!this.validity.isValidInstanceID(instanceID)) {
                    throw new CannotTranslateException(
                            "invalid instance ID in query: " + instanceID);
                }
            }
        }

        final DescribeInstancesResponseType response =
                                new DescribeInstancesResponseType();
        final ReservationSetType reservationSet = new ReservationSetType();

        if (vms == null || vms.length == 0) {
            reservationSet.setItem(EMPTY_RESERVATION_INFO_TYPE);
        } else if (scopedQuery) {
            reservationSet.setItem(this.getSomeReservations(vms,
                                                            instanceIDs,
                                                            ownerID));
        } else {
            reservationSet.setItem(this.getAllReservations(vms,
                                                           ownerID));
        }
        response.setReservationSet(reservationSet);
        return response;
    }

    public String[] findQueryIDs(DescribeInstancesType describeInstancesRequestMsg)
            throws CannotTranslateException {

        if (describeInstancesRequestMsg == null) {
            return EMPTY_STRING_ARRAY; // *** EARLY RETURN ***
        }

        final DescribeInstancesInfoType instancesSet =
                describeInstancesRequestMsg.getInstancesSet();

        if (instancesSet == null) {
            return EMPTY_STRING_ARRAY; // *** EARLY RETURN ***
        }

        final DescribeInstancesItemType[] describeInstancesItemTypes =
                                                        instancesSet.getItem();

        if (describeInstancesItemTypes == null
                || describeInstancesItemTypes.length == 0) {
            return EMPTY_STRING_ARRAY; // *** EARLY RETURN ***
        }

        final ArrayList instanceIDs =
                new ArrayList(describeInstancesItemTypes.length);
        for (int i = 0; i < describeInstancesItemTypes.length; i++) {
            if (describeInstancesItemTypes[i] != null) {
                instanceIDs.add(describeInstancesItemTypes[i].getInstanceId());
            }
        }

        return (String[]) instanceIDs.toArray(new String[instanceIDs.size()]);
    }


    // -------------------------------------------------------------------------
    // GET RESERVATIONS
    // -------------------------------------------------------------------------

    protected ReservationInfoType[] getAllReservations(VM[] vms,
                                                       String ownerID)
            throws CannotTranslateException {

        if (vms == null) {
            throw new CannotTranslateException("vms may not be null");
        }

        final List riits = new LinkedList();
        // assuming sorting will resolve any orphans
        final Hashtable sorted = this.sort(vms);

        final Set keys = sorted.keySet();
        final Iterator iter = keys.iterator();
        while (iter.hasNext()) {
            final String key = (String) iter.next();
            final List list = (List) sorted.get(key);
            riits.add(this.getOneReservation(key, list, ownerID));
        }

        return (ReservationInfoType[]) riits.toArray(
                        new ReservationInfoType[riits.size()]);
    }

    protected ReservationInfoType[] getSomeReservations(VM[] vms,
                                                        String[] instanceIDs,
                                                        String ownerID)
            throws CannotTranslateException {

        if (instanceIDs == null || instanceIDs.length == 0) {
            throw new CannotTranslateException(
                    "instanceIDs is empty or missing");
        }

        // translate the requests elastic IDs into manager instance IDs
        final String[] mgrIDs = new String[instanceIDs.length];
        for (int i = 0; i < instanceIDs.length; i++) {
            // arg and result may be null, ignoring invalid or unknown
            mgrIDs[i] = this.ids.instanceToManager(instanceIDs[i]);
        }

        // add the VMs of interest
        final List newVMs = new LinkedList();
        for (int i = 0; i < vms.length; i++) {
            final VM vm = vms[i];
            if (vm == null) {
                logger.error("Manager implementation is invalid, received " +
                        "null VM in query response");
                continue; // *** GOTO NEXT VM ***
            }
            for (int j = 0; j < mgrIDs.length; j++) {
                final String id = mgrIDs[j];
                if (id != null && id.equals(vm.getID())) {
                    newVMs.add(vm);
                    break;
                }
            }
        }

        final VM[] filteredVMs = (VM[]) newVMs.toArray(new VM[newVMs.size()]);

        // we can use the get-all routine with this limited input
        return this.getAllReservations(filteredVMs, ownerID);
    }


    // -------------------------------------------------------------------------
    // TRANSLATE ONE RESERVATION
    // -------------------------------------------------------------------------

    protected GroupSetType getGroupStub() {
        final GroupItemType[] groupItemTypes = new GroupItemType[1];
        groupItemTypes[0] = new GroupItemType("default");
        return new GroupSetType(groupItemTypes);
    }

    // note: calls IDMgmt and assumes sorting took place
    protected ReservationInfoType getOneReservation(String resID,
                                                    List vms,
                                                    String ownerID)
            throws CannotTranslateException {

        if (resID == null) {
            throw new CannotTranslateException("resID is missing");
        }

        if (ownerID == null) {
            throw new CannotTranslateException("ownerID is missing");
        }
        
        if (vms == null) {
            throw new CannotTranslateException("vms are missing");
        }

        final RunningInstancesItemType[] riits = 
                new RunningInstancesItemType[vms.size()];

        final Iterator iter = vms.iterator();
        int idx = 0;
        while (iter.hasNext()) {
            final VM vm = (VM) iter.next();
            final String elasticID;
            try {
                elasticID = this.ids.managerInstanceToElasticInstance(vm.getID());
            } catch (Exception e) {
                throw new CannotTranslateException(e.getMessage(), e);
            }
            riits[idx] = this.getInstanceItemType(vm, elasticID);
            idx += 1;
        }

        final ReservationInfoType rit = new ReservationInfoType();
        rit.setReservationId(resID);
        rit.setOwnerId(ownerID);
        rit.setGroupSet(this.getGroupStub());
        rit.setInstancesSet(new RunningInstancesSetType(riits));
        return rit;
    }

    protected RunningInstancesItemType getInstanceItemType(VM vm,
                                                           String elasticID)
            throws CannotTranslateException {

        if (vm == null) {
            throw new CannotTranslateException("vm may not be null");
        }
        if (elasticID == null) {
            throw new IllegalArgumentException("elasticID may not be null");
        }

        final RunningInstancesItemType riit = new RunningInstancesItemType();

        riit.setInstanceId(elasticID);
        riit.setImageId(this.getImageID(vm.getVMFiles()));
        riit.setInstanceState(this.getState(vm));
        riit.setReason(this.getReason(vm));
        this.addNICs(vm.getNics(), riit);
        riit.setInstanceType(this.getInstanceType(vm));
        riit.setLaunchTime(this.getLaunchTime(vm));
        riit.setPlacement(this.getPlacement());
        riit.setMonitoring(new InstanceMonitoringStateType("disabled"));
        riit.setInstanceLifecycle(vm.getLifeCycle());
        if(vm.getSpotInstanceRequestID() != null){
            riit.setSpotInstanceRequestId(vm.getSpotInstanceRequestID());
        }
        riit.setClientToken(vm.getClientToken());
        
        final String[] availableKernels = this.kernels.getAvailableKernels();
        if (availableKernels == null || availableKernels.length == 0) {
            riit.setKernelId("UNKNOWN");
        } else {
            // TODO: get kernels into the services-api
            riit.setKernelId(availableKernels[0]); // todo
        }

        // needs to be fixed but it's ok for now, launch indexes are
        //       irrelevant until there is a data server
        riit.setAmiLaunchIndex("0"); // todo


        final String keyName = this.ids.getKeyName(elasticID);
        if (keyName == null || keyName.trim().length() == 0) {
            riit.setKeyName("[no key]");
        } else {
            riit.setKeyName(keyName);
        }
        riit.setProductCodes(new ProductCodesSetType(new ProductCodesSetItemType[]{}));

        return riit;
    }

    public PlacementResponseType getPlacement() {
        final PlacementResponseType prt = new PlacementResponseType();
        final String[] availabilityZones = this.zones.getAvailabilityZones();
        if (availabilityZones == null || availabilityZones.length == 0) {
            prt.setAvailabilityZone("UNKNOWN");
        } else {
            prt.setAvailabilityZone(availabilityZones[0]);
        }
        return prt;
    }

    public PlacementRequestType getPlacementReq() {
        final PlacementRequestType prt = new PlacementRequestType();
        final String[] availabilityZones = this.zones.getAvailabilityZones();
        if (availabilityZones == null || availabilityZones.length == 0) {
            prt.setAvailabilityZone("UNKNOWN");
        } else {
            prt.setAvailabilityZone(availabilityZones[0]);
        }
        return prt;
    }    
    
    public Calendar getLaunchTime(VM vm) throws CannotTranslateException {

        if (vm == null) {
            throw new CannotTranslateException("vm is missing");
        }

        final Schedule schedule = vm.getSchedule();
        if (schedule == null) {
            throw new CannotTranslateException("schedule is missing");
        }

        return schedule.getStartTime();
    }

    public String getInstanceType(VM vm) throws CannotTranslateException {

        if (vm == null) {
            throw new CannotTranslateException("vm is missing");
        }

        final ResourceAllocation ra = vm.getResourceAllocation();
        if (ra == null) {
            throw new CannotTranslateException("ra is missing");
        }

        return this.RAs.getMatchingName(ra);
    }

    public InstanceStateType getState(VM vm)
            throws CannotTranslateException {

        if (vm == null) {
            throw new CannotTranslateException("vm is missing");
        }

        final State state = vm.getState();

        if (state == null) {
            throw new CannotTranslateException("state is missing");
        }

        final String mgrState = state.getState();
        if (mgrState == null) {
            throw new CannotTranslateException("state string is missing");
        }

        final InstanceStateType ist = new InstanceStateType();
        ist.setName(StateMap.managerStringToElasticString(mgrState));
        ist.setCode(StateMap.managerStringToElasticInt(mgrState));
        return ist;
    }

    public String getReason(VM vm) throws CannotTranslateException {

        if (vm == null) {
            throw new CannotTranslateException("vm is missing");
        }

        final State state = vm.getState();
        if (state == null) {
            throw new CannotTranslateException("vm.state is missing");
        }

        final Throwable t = state.getProblem();
        if (t == null) {
            return null;
        }
        
        final String msg = recurseForSomeString(t);
        if (msg == null) {
            return "[no problem cause message found, error type: '" +
                            t.getClass().toString() + "']";
        } else {
            return "'" + msg + "'";
        }
    }
    
    private static String recurseForSomeString(Throwable thr) {
        Throwable t = thr;
        while (true) {
            if (t == null) {
                return null;
            }
            final String msg = t.getMessage();
            if (msg != null) {
                return msg;
            }
            t = t.getCause();
        }
    }

    public String getImageID(VMFile[] vmFiles) throws CannotTranslateException {

        final String UNKNOWN = "UNKNOWN";

        if (vmFiles == null || vmFiles.length == 0) {
            return UNKNOWN; // *** EARLY RETURN ***
        }

        for (int i = 0; i < vmFiles.length; i++) {
            final VMFile vmFile = vmFiles[i];
            if (vmFile.isRootFile()) {
                final URI uri = vmFile.getURI();
                if (uri == null) {
                    return UNKNOWN; // *** EARLY RETURN ***
                }
                final String path = uri.getPath();
                final String parts[] = path.split("/");
                if (parts == null) {
                    return UNKNOWN; // *** EARLY RETURN ***
                } else {
                    // assuming some kind of cloud convention, could make this
                    // behavior configurable in the future
                    return parts[parts.length-1];
                }
            }
        }

        return UNKNOWN;
    }

    // this is slightly different than what the Run section will interpret for
    // VM[] response because these might be VMs that were not created via the
    // elastic interface
    protected void addNICs(NIC[] nics, RunningInstancesItemType riit) {

        if (riit == null) {
            throw new IllegalArgumentException("riit may not be null");
        }

        if (nics == null || nics.length == 0) {
            riit.setDnsName(this.networks.getNoPublicNetwork());
            riit.setPrivateDnsName(this.networks.getNoPrivateNetwork());
            return; // *** EARLY RETURN ***
        }

        String privateAssignedHostname = null;
        String publicAssignedHostname = null;
        String privateAssignedIp = null;
        String publicAssignedIp = null;

        for (int i = 0; i < nics.length; i++) {

            final NIC nic = nics[i];
            if (nic == null) {
                logger.error("Invalid Manager implementation, " +
                        "missing NIC in NICS array");
                continue; // *** GOTO NEXT VM ***
            }

            final String hostname = nic.getHostname();
            if (hostname == null) {
                logger.error("Invalid Manager implementation, " +
                    "missing hostname in NIC");
                continue; // *** GOTO NEXT VM ***
            }
            final String ipAddress = nic.getIpAddress();
            if (ipAddress == null) {
                logger.error("Invalid Manager implementation, " +
                    "missing IP address in NIC");
                continue; // *** GOTO NEXT VM ***
            }

            final String networkName = nic.getNetworkName();
            if (this.networks.isPublicNetwork(networkName)) {
                if (publicAssignedHostname != null) {
                    logger.warn("Can't understand real NICs from duplicate " +
                            "networks yet, treating second one as unknown NIC");
                } else {
                    riit.setDnsName(hostname);
                    publicAssignedHostname = hostname;
                    riit.setIpAddress(ipAddress);
                    publicAssignedIp = ipAddress;
                }
            } else if (this.networks.isPrivateNetwork(networkName)) {
                if (privateAssignedHostname != null) {
                    logger.warn("Can't understand real NICs from duplicate " +
                            "networks yet, treating second one as unknown NIC");
                } else {
                    riit.setPrivateDnsName(hostname);
                    privateAssignedHostname = hostname;
                    riit.setPrivateIpAddress(ipAddress);
                    privateAssignedIp = ipAddress;
                }
            } else {
                logger.warn("Hostname is neither private nor public " +
                        "(as far as elastic interface is " +
                        "concerned): " + hostname);
            }
        }

        if (this.networks.getManagerPublicNetworkName().equals(
                this.networks.getManagerPrivateNetworkName())) {
            if (publicAssignedHostname != null && privateAssignedHostname == null) {
                riit.setPrivateDnsName(publicAssignedHostname);
                riit.setPrivateIpAddress(publicAssignedIp);
            } else if (publicAssignedHostname == null && privateAssignedHostname != null) {
                riit.setDnsName(privateAssignedHostname);
                riit.setIpAddress(privateAssignedIp);
            }
        }

        if (riit.getDnsName() == null) {
            riit.setDnsName(this.networks.getNoPublicNetwork());
        }

        if (riit.getPrivateDnsName() == null) {
            riit.setPrivateDnsName(this.networks.getNoPrivateNetwork());
        }
    }
    

    // -------------------------------------------------------------------------
    // SORT BY TYPE
    // -------------------------------------------------------------------------

    /**
     * @param vms vms to sort
     * @return Hashtable with key: reservationID (String)
     *                   and value: VMs in the reservation (List)
     * @throws CannotTranslateException problem
     */
    protected Hashtable sort(VM[] vms) throws CannotTranslateException {
        
        if (vms == null) {
            throw new IllegalArgumentException("vms may not be null");
        }

        final Set seenKeys = new HashSet();
        final Hashtable dict = new Hashtable();

        for (int i = 0; i < vms.length; i++) {

            final VM vm = vms[i];
            if (vm == null) {
                logger.error("Manager implementation is invalid, received " +
                        "null VM in query response");
                continue; // *** GOTO NEXT VM ***
            }
            
            final String groupid = vm.getGroupID();

            if (groupid == null) {
                this._newNoGroupID(vm, seenKeys, dict);
            } else {
                this._newGroupReservation(vm, groupid, seenKeys, dict);
            }
        }

        // look at all VMs to make sure they have elastic instance IDs,
        // they will not if they were created via other protocols and have
        // never shown up to this messaging layer before

        final Iterator iter = dict.keySet().iterator();
        while (iter.hasNext()) {
            final String elasticReservationID = (String) iter.next();
            final List group = (List) dict.get(elasticReservationID);
            final Iterator groupiter = group.iterator();
            while (groupiter.hasNext()) {
                final VM vm = (VM) groupiter.next();
                try {
                    this.ids.checkInstanceAndReservation(vm.getID(),
                                                         elasticReservationID);
                } catch (Exception e) {

                    final String elasticSingleID =
                            this.ids.managerInstanceToElasticReservation(vm.getID());
                    
                    if (elasticSingleID == null) {
                        throw new CannotTranslateException(e.getMessage(), e);
                    }
                }
            }
        }

        return dict;
    }

    private void _newNoGroupID(VM vm, Set seenKeys, Hashtable dict)
            throws CannotTranslateException {

        final String reservationID;
        try {
            // Only OK to call if there is no associated group or cosched ID.
            // Can call with null for sshkey because if it has not been seen
            // before than this means it did not get created via the elastic
            // interface and there is no way it will have a key nickname.
            reservationID =
                    this.ids.getOrNewInstanceReservationID(vm.getID(), null);
        } catch (Exception e) {
            throw new CannotTranslateException(e.getMessage(), e);
        }

        if (seenKeys.add(reservationID)) {
            final List thisGroup = new LinkedList();
            thisGroup.add(vm);
            dict.put(reservationID, thisGroup);
        } else {
            throw new CannotTranslateException("should not have " +
                    "seen this reservation ID before: " + reservationID);
        }
    }

    private void _newGroupReservation(VM vm, String groupid,
                                      Set seenKeys, Hashtable dict)
            throws CannotTranslateException {

        final String reservationID;
        try {
            reservationID = this.ids.getOrNewGroupReservationID(groupid);
        } catch (Exception e) {
            throw new CannotTranslateException(e.getMessage(), e);
        }

        final List thisGroup;
        if (seenKeys.add(reservationID)) {
            thisGroup = new LinkedList();
            dict.put(reservationID, thisGroup);
        } else {
            thisGroup = (List) dict.get(reservationID);
        }
        thisGroup.add(vm);
    }
    
}
