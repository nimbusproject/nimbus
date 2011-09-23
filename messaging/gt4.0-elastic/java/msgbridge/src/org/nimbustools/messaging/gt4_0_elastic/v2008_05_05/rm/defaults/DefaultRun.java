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
import org.globus.util.Base64;
import org.nimbustools.api._repr._CreateRequest;
import org.nimbustools.api._repr._CustomizationRequest;
import org.nimbustools.api._repr.vm._NIC;
import org.nimbustools.api.brain.ModuleLocator;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.CannotTranslateException;
import org.nimbustools.api.repr.CreateRequest;
import org.nimbustools.api.repr.CreateResult;
import org.nimbustools.api.repr.CustomizationRequest;
import org.nimbustools.api.repr.ReprFactory;
import org.nimbustools.api.repr.vm.NIC;
import org.nimbustools.api.repr.vm.RequiredVMM;
import org.nimbustools.api.repr.vm.ResourceAllocation;
import org.nimbustools.api.repr.vm.State;
import org.nimbustools.api.repr.vm.VM;
import org.nimbustools.api.repr.vm.VMFile;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.*;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.general.Networks;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.general.ResourceAllocations;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.image.Repository;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.rm.ContainerInterface;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.rm.Describe;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.rm.IDMappings;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.rm.Run;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.security.SSHKey;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.security.SSHKeys;

import java.rmi.RemoteException;

public class DefaultRun implements Run {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(DefaultRun.class.getName());
    

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected final ReprFactory repr;
    protected final ResourceAllocations RAs;
    protected final Networks networks;
    protected final Repository repository;
    protected final IDMappings ids;
    protected final ContainerInterface container;
    protected final Describe describe;
    protected final SSHKeys sshKeys;
    

    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    public DefaultRun(ResourceAllocations rasImpl,
                      Networks networksImpl,
                      Repository repoImpl,
                      IDMappings idsImpl,
                      Describe describeImpl,
                      ContainerInterface containerImpl,
                      SSHKeys sshKeysImpl,
                      ModuleLocator locator) throws Exception {

        if (rasImpl == null) {
            throw new IllegalArgumentException("rasImpl may not be null");
        }
        this.RAs = rasImpl;

        if (networksImpl == null) {
            throw new IllegalArgumentException("networksImpl may not be null");
        }
        this.networks = networksImpl;

        if (repoImpl == null) {
            throw new IllegalArgumentException("repoImpl may not be null");
        }
        this.repository = repoImpl;

        if (idsImpl == null) {
            throw new IllegalArgumentException("idsImpl may not be null");
        }
        this.ids = idsImpl;

        if (describeImpl == null) {
            throw new IllegalArgumentException("describeImpl may not be null");
        }
        this.describe = describeImpl;

        if (containerImpl == null) {
            throw new IllegalArgumentException("containerImpl may not be null");
        }
        this.container = containerImpl;

        this.sshKeys = sshKeysImpl;

        if (locator == null) {
            throw new IllegalArgumentException("locator may not be null");
        }
        this.repr = locator.getReprFactory();
    }

    public DefaultRun(ResourceAllocations rasImpl,
                      Networks networksImpl,
                      Repository repoImpl,
                      IDMappings idsImpl,
                      Describe describeImpl,
                      ContainerInterface containerImpl,
                      ModuleLocator locator) throws Exception {
        this(rasImpl, networksImpl, repoImpl,
             idsImpl, describeImpl, containerImpl, null, locator);
    }

    // -------------------------------------------------------------------------
    // implements Run
    // -------------------------------------------------------------------------

    public CreateRequest translateRunInstances(RunInstancesType req,
                                               Caller caller)
            throws RemoteException, CannotTranslateException {

        final String ownerID;
        try {
            ownerID = this.container.getOwnerID(caller);
        } catch (CannotTranslateException e) {
            throw new RemoteException(e.getMessage(), e);
        }

        final String imageID = req.getImageId();
        if (imageID == null) {
            throw new RemoteException("Request is missing image ID");
        }

        // currently ignored: groupSet, placement, kernel, ramdiskid,
        // blockDeviceMapping

        final _CustomizationRequest cust;
        final String keyname = req.getKeyName();
        if (keyname != null && this.sshKeys != null) {
            cust = this.repr._newCustomizationRequest();
            final SSHKey key = this.sshKeys.findKey(ownerID, keyname);
            if (key == null) {
                throw new RemoteException("There is no key '" + keyname +
                        "' registered for you to use");
            }
            cust.setContent(key.getPubKeyValue());
            cust.setPathOnVM("/root/.ssh/authorized_keys");
        } else {
            cust = null;
        }

        final CustomizationRequest[] custRequests;
        if (cust != null) {
            custRequests = new CustomizationRequest[1];
            custRequests[0] = cust;
        } else {
            custRequests = null;
        }

        final NIC[] nics = this.getNICs();
        final String raType = req.getInstanceType();
        final ResourceAllocation ra = this.RAs.getMatchingRA(raType,
                                                             req.getMinCount(),
                                                             req.getMaxCount(),
                                                             false);

        final RequiredVMM reqVMM = this.RAs.getRequiredVMM();

        String userData = null;
        final UserDataType t_userData = req.getUserData();
        if (t_userData != null) {
            final String base64Encoded = t_userData.getData();
            if (base64Encoded != null) {
                if (!Base64.isBase64(base64Encoded)) {
                    throw new RemoteException("userdata does not appear to " +
                            "be base64 encoded?");
                }
                final byte[] bytes = Base64.decode(base64Encoded.getBytes());
                userData = new String(bytes);
            }
        }

        final VMFile[] files =
                this.repository.constructFileRequest(imageID, ra, caller);

        final String clientToken = req.getClientToken();

        String availabilityZone = null;
        if (req.getPlacement() != null) {
            availabilityZone = req.getPlacement().getAvailabilityZone();
        }


        final _CreateRequest creq = this.repr._newCreateRequest();

        creq.setContext(null);
        creq.setCoScheduleDone(false);
        creq.setCoScheduleID(null);
        creq.setCoScheduleMember(false);
        creq.setCustomizationRequests(custRequests);
        creq.setInitialStateRequest(State.STATE_Running);
        creq.setName(imageID);
        creq.setRequestedKernel(null); // todo
        creq.setRequestedNics(nics);
        creq.setRequestedRA(ra);
        creq.setRequestedSchedule(null); // ask for default
        creq.setRequiredVMM(reqVMM);
        creq.setShutdownType(CreateRequest.SHUTDOWN_TYPE_TRASH);
        creq.setVMFiles(files);
        creq.setMdUserData(userData);
        creq.setSshKeyName(keyname);
        creq.setClientToken(clientToken);
        creq.setRequestedResourcePool(availabilityZone);

        return creq;
    }

    public RunInstancesResponseType translateCreateResult(CreateResult result,
                                                     Caller caller,
                                                     String sshKeyName)
            throws Exception {

        if (result == null) {
            throw new CannotTranslateException("creation result is missing");
        }

        final VM[] vms = result.getVMs();
        if (vms == null || vms.length == 0) {
            throw new CannotTranslateException("creation result is empty?");
        }

        if (result.getCoscheduledID() != null) {
            throw new CannotTranslateException("not expecting " +
                    "coscheduling ID in any cases yet");
        }

        final String groupid = result.getGroupID();

        if (groupid == null && vms.length != 1) {
            throw new CannotTranslateException("expecting a groupID if " +
                    "more than one VM was created");
        }

        final String vmidWhenJustOne;
        final String resID;
        // these mappings may exist already, for secondary idempotent launches
        if (groupid == null) {
            vmidWhenJustOne = vms[0].getID();
            resID = this.ids.getOrNewInstanceReservationID(vmidWhenJustOne, sshKeyName);
        } else {
            vmidWhenJustOne = null;
            resID = this.ids.getOrNewGroupReservationID(groupid);
        }

        final RunningInstancesSetType rist = new RunningInstancesSetType();
        final RunningInstancesItemType[] riits =
                new RunningInstancesItemType[vms.length];

        final String msg = "New reservation ID '" + resID + "' for ";
        final StringBuffer buf = new StringBuffer(msg);
        if (vmidWhenJustOne == null) {
            buf.append("VM group '").append(groupid);
        } else {
            buf.append("single VM '").append(vmidWhenJustOne);
        }
        buf.append("'.  Members:");

        for (int i = 0; i < vms.length; i++) {
            final VM vm = vms[i];
            final String id = vm.getID();
            if (id == null) {
                throw new CannotTranslateException("VM has no ID");
            }
            final String instID;
            if (vmidWhenJustOne != null) {
                // mapping already created:
                instID = this.ids.managerInstanceToElasticInstance(vmidWhenJustOne);
            } else {
                // this mapping may exist already, for secondary idempotent launches
                instID = this.ids.getOrNewInstanceID(vm.getID(), resID, sshKeyName);
            }

            if (i != 0) {
                buf.append(",");
            }
            buf.append(" id-").append(id)
               .append("='").append(instID).append("'");

            riits[i] = this.getOneCreatedVM(vm, instID, resID, sshKeyName);
        }

        logger.info(buf.toString());

        final RunInstancesResponseType ret = new RunInstancesResponseType();
        ret.setGroupSet(getGroupStub());
        final String ownerID = this.container.getOwnerID(caller);
        if (ownerID == null) {
            throw new CannotTranslateException("Cannot find owner ID");
        }
        ret.setOwnerId(ownerID);
        ret.setReservationId(resID);
        rist.setItem(riits);
        ret.setInstancesSet(rist);
        ret.setRequesterId(ownerID);

        return ret;
    }


    // -------------------------------------------------------------------------
    // NETWORK REQUEST
    // -------------------------------------------------------------------------

    protected NIC[] getNICs() throws CannotTranslateException {

        // if the network mappings are the same value, that currently means
        // only make one real NIC request

        final String pubNet = this.networks.getManagerPublicNetworkName();
        final String privNet = this.networks.getManagerPrivateNetworkName();

        if (pubNet == null || privNet == null) {
            throw new CannotTranslateException("Illegal Networks " +
                    "implementation, null network mapping");
        }

        final NIC[] nics;
        if (pubNet.equals(privNet)) {
            nics = new NIC[1];
            nics[0] = this.oneRequestedNIC(pubNet, "autoeth0");
        } else {
            nics = new NIC[2];
            nics[0] = this.oneRequestedNIC(pubNet, "autoeth0");
            nics[1] = this.oneRequestedNIC(privNet, "autoeth1");
        }
        return nics;
    }

    protected NIC oneRequestedNIC(String networkName, String nicName)
            throws CannotTranslateException {
        
        if (networkName == null) {
            throw new CannotTranslateException("networkName is missing");
        }
        final _NIC nic = this.repr._newNIC();
        nic.setAcquisitionMethod(NIC.ACQUISITION_AllocateAndConfigure);
        nic.setNetworkName(networkName);
        nic.setName(nicName);
        return nic;
    }


    // -------------------------------------------------------------------------
    // RESULT
    // -------------------------------------------------------------------------

    // todo: duped code; support groups
    public static GroupSetType getGroupStub() {
        final GroupItemType[] groupItemTypes = new GroupItemType[1];
        groupItemTypes[0] = new GroupItemType("default");
        return new GroupSetType(groupItemTypes);
    }

    protected RunningInstancesItemType getOneCreatedVM(VM vm,
                                                       String instID,
                                                       String resID,
                                                       String sshKeyName)
            throws CannotTranslateException {
        
        if (instID == null) {
            throw new IllegalArgumentException("instID may not be null");
        }
        if (resID == null) {
            throw new IllegalArgumentException("resID may not be null");
        }
        if (vm == null) {
            throw new IllegalArgumentException("vm may not be null");
        }

        final RunningInstancesItemType riit = new RunningInstancesItemType();

        riit.setInstanceState(this.describe.getState(vm));
        riit.setReason(this.describe.getReason(vm));
        riit.setPlacement(this.describe.getPlacement());
        riit.setImageId(this.describe.getImageID(vm.getVMFiles()));
        riit.setInstanceType(this.describe.getInstanceType(vm));
        riit.setLaunchTime(this.describe.getLaunchTime(vm));
        

        riit.setAmiLaunchIndex("0"); // todo: could generate this here with i
        this.handleNetworking(vm, riit);
        riit.setInstanceId(instID);
        riit.setKeyName(sshKeyName);

        riit.setClientToken(vm.getClientToken());

        riit.setKernelId("default"); // todo

        riit.setMonitoring(new InstanceMonitoringStateType("disabled"));
        riit.setProductCodes(new ProductCodesSetType(new ProductCodesSetItemType[]{}));

        //riit.setRamdiskId();
        //riit.setReason();

        return riit;
    }

    protected void handleNetworking(VM vm,
                                    RunningInstancesItemType riit)
            throws CannotTranslateException {


        // ec2 only necessarily has networking information on a running
        // instance. we can loosen up requirements here.

        // this is motivated by idempotent instance support. In cases where
        // an idempotent launch maps to an already-terminated instance,
        // the VM object here will be in the terminated state and have no
        // NICs information

        final boolean isTerminated =
                vm.getState().getState().equals(State.STATE_Cancelled);

        if (isTerminated && (vm.getNics() == null || vm.getNics().length == 0)) {
            return;
        }

        final NIC[] nics = vm.getNics();
        if (nics == null || nics.length == 0) {
            // zero NICs not supported by this interface
            throw new CannotTranslateException("NICs are missing");
        }

        if (nics.length != 1 && nics.length != 2) {
            throw new CannotTranslateException("Can only handle one or two " +
                    "assigned NICs, but were given " + nics.length +
                    " for vm id-" + vm.getID());
        }

        if (nics.length == 1 && nics[0] == null) {
            throw new CannotTranslateException("NIC[] value is missing");
        }

        if (nics.length == 2 && nics[1] == null) {
            throw new CannotTranslateException("NIC[] value is missing");
        }

        final String netName = nics[0].getNetworkName();
        if (netName == null) {
            throw new CannotTranslateException("NIC in vm id-" +
                    vm.getID() + " is missing network name");
        }

        final String hostname = nics[0].getHostname();
        if (hostname == null) {
            throw new CannotTranslateException("NIC in vm id-" +
                    vm.getID() + " is missing hostname");
        }

        final String ipAddress = nics[0].getIpAddress();
        if (ipAddress == null) {
            throw new CannotTranslateException("NIC in vm id-" +
                    vm.getID() + " is missing IP address");
        }

        final String netName2;
        final String hostname2;
        final String ipAddress2;
        if (nics.length == 2) {
            netName2 = nics[1].getNetworkName();
            if (netName2 == null) {
                throw new CannotTranslateException("NIC in vm id-" +
                        vm.getID() + " is missing network name");
            }

            hostname2 = nics[1].getHostname();
            if (hostname2 == null) {
                throw new CannotTranslateException("NIC in vm id-" +
                        vm.getID() + " is missing hostname");
            }

            ipAddress2 = nics[1].getIpAddress();
            if (ipAddress2 == null) {
                throw new CannotTranslateException("NIC in vm id-" +
                        vm.getID() + " is missing IP address");
            }
        } else {
            netName2 = null;
            hostname2 = null;
            ipAddress2 = null;
        }

        String privateAssignedHostname = null;
        String publicAssignedHostname = null;
        String privateAssignedIp = null;
        String publicAssignedIp = null;

        if (this.networks.isPrivateNetwork(netName)) {
            riit.setPrivateDnsName(hostname);
            riit.setPrivateIpAddress(ipAddress);
            privateAssignedHostname = hostname;
            privateAssignedIp = ipAddress;
        } else if (this.networks.isPublicNetwork(netName)) {
            riit.setDnsName(hostname);
            riit.setIpAddress(ipAddress);
            publicAssignedHostname = hostname;
            publicAssignedIp = ipAddress;
        } else {
            throw new CannotTranslateException("Unknown network was " +
                    "assigned: '" + netName + "'");
        }

        if (nics.length == 2) {
            if (this.networks.isPrivateNetwork(netName2)) {
                if (privateAssignedHostname != null) {
                    // if public and private are set to be the same, that means
                    // request ONE nic and make it appear to be two in remote
                    // interface
                    throw new CannotTranslateException("Won't support " +
                            "real NICs from duplicate networks yet");
                }
                riit.setPrivateDnsName(hostname2);
                riit.setPrivateIpAddress(ipAddress2);
                privateAssignedHostname = hostname2;
                privateAssignedIp = ipAddress2;
            } else if (this.networks.isPublicNetwork(netName2)) {
                if (publicAssignedHostname != null) {
                    // if public and private are set to be the same, that means
                    // request ONE nic and make it appear to be two in remote
                    // interface
                    throw new CannotTranslateException("Won't support " +
                            "real NICs from duplicate networks yet");
                }
                riit.setDnsName(hostname2);
                riit.setIpAddress(ipAddress2);
                publicAssignedHostname = hostname2;
                publicAssignedIp = ipAddress2;
            } else {
                throw new CannotTranslateException("Unknown network was " +
                        "assigned: '" + netName2 + "'");
            }
        }

        if (this.networks.getManagerPublicNetworkName().equals(
                this.networks.getManagerPrivateNetworkName())) {

            if (publicAssignedHostname != null && privateAssignedHostname != null) {
                // if public and private are set to be the same, that means
                // request ONE nic and make it appear to be two in remote
                // interface
                throw new CannotTranslateException("Won't support " +
                            "real NICs from duplicate networks yet");
            }

            if (publicAssignedHostname != null) {
                riit.setPrivateDnsName(publicAssignedHostname);
                riit.setPrivateIpAddress(publicAssignedIp);
            } else {
                riit.setDnsName(privateAssignedHostname);
                riit.setIpAddress(privateAssignedIp);
            }
        }
    }
}
