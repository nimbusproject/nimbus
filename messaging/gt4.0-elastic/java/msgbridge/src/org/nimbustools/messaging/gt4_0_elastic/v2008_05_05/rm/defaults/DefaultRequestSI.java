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
import org.nimbustools.api._repr._CreateRequest;
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
import org.nimbustools.api.services.metadata.MetadataServer;
import org.nimbustools.api.services.rm.Manager;
import org.nimbustools.messaging.gt4_0.common.AddCustomizations;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.*;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.general.Networks;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.general.ResourceAllocations;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.image.Repository;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.rm.CancelSI;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.rm.ContainerInterface;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.rm.Describe;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.rm.DescribeSI;
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
        
    protected final MetadataServer mdServer;
    protected final DescribeSI describeSI;
    protected final CancelSI cancelSI;
    
    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    public DefaultRequestSI(ResourceAllocations rasImpl,
                            Networks networksImpl,
                            Repository repoImpl,
                            IDMappings idsImpl,
                            Describe describeImpl,
                            DescribeSI describeSIImpl,
                            CancelSI cancelSIIMpl,
                            ContainerInterface containerImpl,
                            SSHKeys sshKeysImpl,
                            ModuleLocator locator) throws Exception {
        
        super(rasImpl, networksImpl, repoImpl, idsImpl, describeImpl, containerImpl, locator);
        
        if (describeSIImpl == null) {
            throw new IllegalArgumentException("describeSIImpl may not be null");
        }
        this.describeSI = describeSIImpl;
        
        if (cancelSIIMpl == null) {
            throw new IllegalArgumentException("cancelSIIMpl may not be null");
        }
        this.cancelSI = cancelSIIMpl;
        
        if (locator == null) {
            throw new IllegalArgumentException("mdServerImpl may not be null");
        }
        this.mdServer = locator.getMetadataServer();        
    }

    // -------------------------------------------------------------------------
    // implements RequestSI
    // -------------------------------------------------------------------------

    public RequestSpotInstancesResponseType requestSpotInstances(
            RequestSpotInstancesType req, Caller caller, Manager manager)
            throws RemoteException {
        
        final SpotRequestInfo result;
        try {
            SpotCreateRequest creq =
                    this.translateReqSpotInstances(req, caller);
            AddCustomizations.addAll((_CreateRequest)creq,
                                     this.repr, this.mdServer);
            result = manager.requestSpotInstances(creq, caller);

        } catch (Exception e) {
            throw new RemoteException(e.getMessage(), e);
        }
        
        try {
            SpotInstanceRequestSetItemType sirsit = this.describeSI.translateSpotInfo(result);
            
            SpotInstanceRequestSetType sirs = new SpotInstanceRequestSetType();
            sirs.setItem(new SpotInstanceRequestSetItemType[]{sirsit});
            
            RequestSpotInstancesResponseType response = new RequestSpotInstancesResponseType();
            response.setSpotInstanceRequestSet(sirs);
            
            return response;
        } catch (Exception e) {
            final String err = "Problem translating valid request spot instances " +
                    "result into elastic protocol.  Backout required. " +
                    " Error: " + e.getMessage();
            logger.error(err, e);
            this.cancelSI.backOutRequestSpotInstances(result, caller, manager);
            // gets caught by Throwable hook:
            throw new RuntimeException(err, e);
        }
    }    
    
    /**
     * Translate request spot instances 
     * into something the Manager understands.
     * 
     * @param req given SI request
     * @param caller caller object
     * @return valid create request for manager
     * @throws RemoteException unexpected error
     * @throws CannotTranslateException invalid request or configuration
     */
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
        
        boolean persistent = DefaultDescribeSI.PERSISTENT.equalsIgnoreCase(req.getType());
        
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
}
