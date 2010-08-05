/*
 * Copyright 1999-2010 University of Chicago
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
import org.nimbustools.api._repr._CustomizationRequest;
import org.nimbustools.api._repr._SpotCreateRequest;
import org.nimbustools.api.brain.ModuleLocator;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.CannotTranslateException;
import org.nimbustools.api.repr.CreateRequest;
import org.nimbustools.api.repr.CustomizationRequest;
import org.nimbustools.api.repr.SpotCreateRequest;
import org.nimbustools.api.repr.SpotRequestInfo;
import org.nimbustools.api.repr.vm.NIC;
import org.nimbustools.api.repr.vm.RequiredVMM;
import org.nimbustools.api.repr.vm.ResourceAllocation;
import org.nimbustools.api.repr.vm.State;
import org.nimbustools.api.repr.vm.VMFile;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_06_15.*;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.general.Networks;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.general.ResourceAllocations;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.image.Repository;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.rm.ContainerInterface;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.rm.Describe;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.rm.IDMappings;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.rm.RequestSI;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.security.SSHKey;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.security.SSHKeys;

import java.rmi.RemoteException;

public class DefaultRequestSI extends DefaultRun implements RequestSI {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(DefaultRequestSI.class.getName());
    protected static final String PERSISTENT = "persistent";
    protected static final String ONE_TIME = "one-time";
        
    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    public DefaultRequestSI(ResourceAllocations rasImpl,
                            Networks networksImpl,
                            Repository repoImpl,
                            IDMappings idsImpl,
                            Describe describeImpl,
                            ContainerInterface containerImpl,
                            SSHKeys sshKeysImpl,
                            ModuleLocator locator) throws Exception {
        
        super(rasImpl, networksImpl, repoImpl, idsImpl, describeImpl, containerImpl, locator);
    }

    public DefaultRequestSI(ResourceAllocations rasImpl,
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

    public SpotCreateRequest translateReqSpotInstances(
            RequestSpotInstancesType req, Caller caller)
            throws RemoteException, CannotTranslateException {

        final String ownerID;
        try {
            ownerID = this.container.getOwnerID(caller);
        } catch (CannotTranslateException e) {
            throw new RemoteException(e.getMessage(), e);
        }

        LaunchSpecificationRequestType launchSpec = req.getLaunchSpecification();
        
        final String imageID = launchSpec.getImageId();
        if (imageID == null) {
            throw new RemoteException("Request is missing image ID");
        }

        // currently ignored: groupSet, placement, kernel, ramdiskid,
        // blockDeviceMapping

        final _CustomizationRequest cust;
        final String keyname = launchSpec.getKeyName();
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
        final String raType = launchSpec.getInstanceType();
        final ResourceAllocation ra = this.RAs.getMatchingRA(raType,
                                                             req.getInstanceCount().intValue(),
                                                             req.getInstanceCount().intValue(),
                                                             true);

        final RequiredVMM reqVMM = this.RAs.getRequiredVMM();

        String userData = null;
        final UserDataType t_userData = launchSpec.getUserData();
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

        final Double spotPrice;
        try{
            spotPrice = new Double(req.getSpotPrice());
        } catch (NumberFormatException e){
            throw new RemoteException("Error in spot price conversion.");
        }    
        
        boolean persistent = PERSISTENT.equals(req.getType());
        
        final _SpotCreateRequest screq = this.repr._newSpotCreateRequest();

        screq.setContext(null);
        screq.setCoScheduleDone(false);
        screq.setCoScheduleID(null);
        screq.setCoScheduleMember(false);
        screq.setCustomizationRequests(custRequests);
        screq.setInitialStateRequest(State.STATE_Running);
        screq.setName(imageID);
        screq.setRequestedKernel(null); // todo
        screq.setRequestedNics(nics);
        screq.setRequestedRA(ra);
        screq.setRequestedSchedule(null); // ask for default
        screq.setRequiredVMM(reqVMM);
        screq.setShutdownType(CreateRequest.SHUTDOWN_TYPE_TRASH);
        screq.setVMFiles(files);
        screq.setMdUserData(userData);
        screq.setSshKeyName(keyname);
        
        screq.setPersistent(persistent);
        screq.setSpotPrice(spotPrice);
        
        return screq;
        
    }    
    

    // -------------------------------------------------------------------------
    // RESULT
    // -------------------------------------------------------------------------

    public RequestSpotInstancesResponseType translateSpotInfo(
            SpotRequestInfo result, Caller caller) throws Exception {

        if (result == null) {
            throw new CannotTranslateException("spot request info is missing");
        }
        
        Integer instanceCount = result.getInstanceCount();
        if (instanceCount == null || instanceCount == 0) {
            throw new CannotTranslateException("instance count is empty?");
        }        

        final String groupid = result.getGroupID();

        if (groupid == null && instanceCount != 1) {
            throw new CannotTranslateException("expecting a groupID if " +
                    "more than one VM was created");
        }

        String vmidWhenJustOne = null;
        String resID = null;
        if (groupid == null && result.getVMIds().length == 1) {
            vmidWhenJustOne = result.getVMIds()[0];
            resID = this.ids.newGrouplessInstanceID(vmidWhenJustOne,
                                                    result.getSshKeyName());
            logger.info("New reservation ID '" + resID + "' for Single VM '" +
                         vmidWhenJustOne +"'.");            
        } else if (groupid != null && result.getVMIds().length > 0) {
            vmidWhenJustOne = null;
            resID = this.ids.newGroupReservationID(groupid);
            logger.info("New reservation ID '" + resID + "' for VM group '" +
                         groupid +"'.");
        }

        String instID = null;
        if (vmidWhenJustOne != null) {
            // mapping already created:
            instID = this.ids.managerInstanceToElasticInstance(vmidWhenJustOne);
            logger.info("id-" + vmidWhenJustOne + "='" + instID + "'.");            
        } else if(resID != null){
            String vmId = result.getVMIds()[0];
            instID = this.ids.newInstanceID(vmId, resID, result.getSshKeyName());
            logger.info("id-" + vmId + "='" + instID + "'.");
        }
        
        LaunchSpecificationResponseType launchSpec = getLaunchSpec(result);
        String type = result.isPersistent()? PERSISTENT : ONE_TIME;
        
        SpotInstanceRequestSetItemType sirsi = new SpotInstanceRequestSetItemType();

        sirsi.setLaunchSpecification(launchSpec);
        sirsi.setType(type);
        sirsi.setSpotPrice(result.getSpotPrice().toString());
        sirsi.setSpotInstanceRequestId(result.getRequestID());
        sirsi.setState(result.getState().getStateStr());
        sirsi.setCreateTime(result.getCreationTime());
        if(instID != null){
            sirsi.setInstanceId(instID);
        }
        
        SpotInstanceRequestSetType sirs = new SpotInstanceRequestSetType();
        sirs.setItem(0, sirsi);        

        final RequestSpotInstancesResponseType rsirt = new RequestSpotInstancesResponseType();
        rsirt.setSpotInstanceRequestSet(sirs);
        
        return rsirt;
    }

    protected LaunchSpecificationResponseType getLaunchSpec(SpotRequestInfo reqInfo)
    throws CannotTranslateException {

        if (reqInfo == null) {
            throw new IllegalArgumentException("instID may not be null");
        }

        LaunchSpecificationResponseType launchSpec = new LaunchSpecificationResponseType();

        launchSpec.setGroupSet(this.getGroupStub());
        launchSpec.setPlacement(this.describe.getPlacementReq());
        launchSpec.setImageId(this.describe.getImageID(reqInfo.getVMFiles()));
        launchSpec.setInstanceType(this.RAs.getSpotInstanceType());
        launchSpec.setKeyName(reqInfo.getSshKeyName());
        launchSpec.setKernelId("default"); // todo
        
        return launchSpec;
    }
    
}
