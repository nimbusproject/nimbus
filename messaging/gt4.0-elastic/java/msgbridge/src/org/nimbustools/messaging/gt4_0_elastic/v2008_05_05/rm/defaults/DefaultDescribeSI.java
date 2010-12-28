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
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.CannotTranslateException;
import org.nimbustools.api.repr.SpotRequestInfo;
import org.nimbustools.api.services.rm.Manager;
import org.nimbustools.api.services.rm.ManageException;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.DescribeSpotInstanceRequestsResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.DescribeSpotInstanceRequestsType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.LaunchSpecificationResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.SpotInstanceRequestIdSetItemType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.SpotInstanceRequestSetItemType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.SpotInstanceRequestSetType;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.general.ResourceAllocations;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.rm.Describe;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.rm.DescribeSI;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.rm.IDMappings;

import java.rmi.RemoteException;
import java.util.Arrays;

public class DefaultDescribeSI implements DescribeSI {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(DefaultDescribeSI.class.getName());    
    protected static final String PERSISTENT = "persistent";
    protected static final String ONE_TIME = "one-time";
    
    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected final IDMappings ids;
    protected final Describe describe;
    protected final ResourceAllocations RAs;    

    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    public DefaultDescribeSI(Describe describeImpl, IDMappings idsImpl, ResourceAllocations RAsImpl) {

        if (idsImpl == null) {
            throw new IllegalArgumentException("idsImpl may not be null");
        }
        this.ids = idsImpl;
        
        if (describeImpl == null) {
            throw new IllegalArgumentException("describeImpl may not be null");
        }
        this.describe = describeImpl;   
        
        if (RAsImpl == null) {
            throw new IllegalArgumentException("RAsImpl may not be null");
        }
        this.RAs = RAsImpl;             
    }

    // -------------------------------------------------------------------------
    // DESCRIBE OPERATION
    // -------------------------------------------------------------------------
    
    public DescribeSpotInstanceRequestsResponseType describeSIRequests(
            DescribeSpotInstanceRequestsType req, Caller caller, Manager manager)
            throws RemoteException {

        if (manager == null) {
            throw new IllegalArgumentException("manager may not be null");
        }
        if (caller == null) {
            throw new IllegalArgumentException("caller may not be null");
        }
        if (req == null) {
            throw new IllegalArgumentException("req may not be null");
        }
        
        SpotInstanceRequestIdSetItemType[] sirisi = req.getSpotInstanceRequestIdSet().getItem();
        
        SpotRequestInfo[] spotRequests = null;
        
        if(sirisi != null) {
            String[] reqIds = new String[sirisi.length];
            
            for (int i = 0; i < sirisi.length; i++) {
                reqIds[i] = sirisi[i].getSpotInstanceRequestId().trim();
            }
            
            try {
                if(reqIds.length > 0){
                    spotRequests = manager.getSpotRequests(reqIds, caller);                
                } else {
                    spotRequests = manager.getSpotRequestsByCaller(caller);
                }
            } catch (ManageException e) {
                final String msg = "Problem retrieving SI request: " + Arrays.toString(reqIds) + ": ";
                if (logger.isDebugEnabled()) {
                    logger.error(msg + e.getMessage(), e);
                } else {
                    logger.error(msg + e.getMessage());
                }
            } catch (Exception e) {
                logger.warn(e.getMessage(), e);
                throw new RemoteException("Invalid request ID.");
            }            
        }
        
        DescribeSpotInstanceRequestsResponseType dsirrt = new DescribeSpotInstanceRequestsResponseType();
        
        if(spotRequests != null){            
            SpotInstanceRequestSetItemType[] sirsti = new SpotInstanceRequestSetItemType[spotRequests.length];
            
            for (int i = 0; i < spotRequests.length; i++) {
                try {
                    sirsti[i] = translateSpotInfo(spotRequests[i]);
                } catch (Exception e) {
                    final String err = "Problem translating valid spot instance " +
                    "request into elastic protocol." +
                    " Error: " + e.getMessage();                    
                    logger.warn(err, e);
                }
            }
            
            SpotInstanceRequestSetType sirst = new SpotInstanceRequestSetType();
            sirst.setItem(sirsti);
            
            dsirrt.setSpotInstanceRequestSet(sirst);
        }
        
        return dsirrt;
    }

    // -------------------------------------------------------------------------
    // TRANSLATE
    // -------------------------------------------------------------------------    
    
    public SpotInstanceRequestSetItemType translateSpotInfo(
                                    SpotRequestInfo result) throws Exception {

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
        
        String instID = null;
        String resID = null;
        if (groupid == null && result.getVMIds().length == 1) {
            String vmId = result.getVMIds()[0];
            resID = this.ids.getOrNewInstanceReservationID(vmId,
                                                    result.getSshKeyName());
            logger.info("New reservation ID '" + resID + "' for Single VM '" +
                    vmId +"'.");            
            instID = this.ids.managerInstanceToElasticInstance(vmId);
            logger.info("id-" + vmId + "='" + instID + "'.");            
        } else if (groupid != null && result.getVMIds().length > 0) {
            String vmId = result.getVMIds()[0];
            resID = this.ids.getOrNewGroupReservationID(groupid);
            logger.info("New reservation ID '" + resID + "' for VM group '" +
                         groupid +"'.");
            instID = this.ids.managerInstanceToElasticInstance(vmId); 
            if(instID == null){
                instID = this.ids.newInstanceID(vmId, resID, result.getSshKeyName());
            }
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

        return sirsi;
    }    

    protected LaunchSpecificationResponseType getLaunchSpec(SpotRequestInfo reqInfo)
    throws CannotTranslateException {

        if (reqInfo == null) {
            throw new IllegalArgumentException("instID may not be null");
        }

        LaunchSpecificationResponseType launchSpec = new LaunchSpecificationResponseType();

        launchSpec.setGroupSet(DefaultRun.getGroupStub());
        launchSpec.setPlacement(this.describe.getPlacementReq());
        launchSpec.setImageId(this.describe.getImageID(reqInfo.getVMFiles()));
        launchSpec.setInstanceType(this.RAs.getSpotInstanceType());
        launchSpec.setKeyName(reqInfo.getSshKeyName());
        launchSpec.setKernelId("default"); // todo
        
        return launchSpec;
    }    
}
