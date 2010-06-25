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

package org.globus.workspace.testing.suites.spotinstances;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import org.globus.workspace.spotinstances.PricingModelConstants;
import org.globus.workspace.spotinstances.SpotInstancesManagerImpl;
import org.globus.workspace.testing.NimbusTestBase;
import org.globus.workspace.testing.NimbusTestContextLoader;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.CreateRequest;
import org.nimbustools.api.repr.CreateResult;
import org.nimbustools.api.repr.RequestSI;
import org.nimbustools.api.repr.SpotRequest;
import org.nimbustools.api.repr.si.SIRequestState;
import org.nimbustools.api.services.rm.Manager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.Test;

@ContextConfiguration(
        locations={"file:./service/service/java/tests/suites/spotinstances/" +
                "singleresourcepool/home/services/etc/nimbus/workspace-service/other/main.xml"},
        loader=NimbusTestContextLoader.class)
public class SingleResourcePoolSISuite extends NimbusTestBase {

    // -----------------------------------------------------------------------------------------
    // extends NimbusTestBase
    // -----------------------------------------------------------------------------------------

    private static final String SPOTINSTANCES_MANAGER_BEAN_NAME = "nimbus-rm.spotinstances.manager";
    private static final int TASK_TIME = 10;
    private static final int BUFFER = 10000;

    @AfterSuite(alwaysRun=true)
    public void suiteTeardown() throws Exception {
        super.suiteTeardown();
    }

    /**
     * This is how coordinate your Java test suite code with the conf files to use.
     * @return absolute path to the value that should be set for $NIMBUS_HOME
     * @throws Exception if $NIMBUS_HOME cannot be determined
     */
    @Override
    protected String getNimbusHome() throws Exception {
        return this.determineSuitesPath() + "/spotinstances/singleresourcepool/home";
    }
    
    /**
     * This tests works as follows:
     * 
     *  * One non-persistent SI request is submitted
     *    * Check SI request result
     *    * Check if it was allocated
     *    * Check if spot price is equal to MINIMUM PRICE (since there are still available SI resources)
     *    
     *  * This request is canceled
     *    * Check if it was properly canceled
     *    
     * @throws Exception in case an error occurs
     */
    @Test
    @DirtiesContext
    public void singleRequest() throws Exception {
        logger.debug("singleRequest");

        //Objects and requests setup        
        
        Caller caller = this.populator().getCaller();
        Manager rm = this.locator.getManager();
        
        Double previousPrice = rm.getSpotPrice();
        
        final Double bid = previousPrice + 1;
        RequestSI requestSI = this.populator().getBasicRequestSI("suite:spotinstances:singleresourcepool:singleRequest", 1, bid, false);
        
        //Request spot instances
        SpotRequest result = rm.requestSpotInstances(requestSI, caller);
        
        //Check result
        //note: cannot check state at this point, because it can either be 
        //OPEN (not scheduler yet) or ACTIVE (already scheduled)
        assertEquals(bid, result.getSpotPrice());
        assertTrue(!result.isPersistent());
        
        // Spot Instances Snapshot
        // 
        // Total memory: 1280MB
        // Reserved free memory (for ordinary WS requests): 256MB
        // Available basic SI VMs: 8 (128MB each)         
        //
        // Current Requests___________________________________________________________
        // |   Request  | reqVMs | allocVMs |       Bid        | Status | Persistent |
        // | singleReq  |    1   |    1     |  previousPrice+1 | ACTIVE |    false   |
        // ---------------------------------------------------------------------------
        // Requested SI VMs: 1
        // Spot price: MINIUM_PRICE (since requestedVMs < availableVMs)                  
        
        //New spot price is equal to minimum price (since there are still available resources)
        assertEquals(PricingModelConstants.MINIMUM_PRICE,  rm.getSpotPrice());
        
        SpotRequest[] spotRequestByCaller = rm.getSpotRequestByCaller(caller);
        assertEquals(1, spotRequestByCaller.length);
        assertEquals(result, spotRequestByCaller[0]);
        
        //Let's assume the request was already scheduled, so the state should be ACTIVE
        SpotRequest request = rm.getSpotRequest(result.getRequestID(), caller);
        assertEquals(SIRequestState.STATE_Active, request.getState().getStateStr());
        
        //Cancel request
        SpotRequest[] cancelledReqs = rm.cancelSpotInstanceRequests(new String[]{result.getRequestID()}, caller);
        assertEquals(1, cancelledReqs.length);
        assertEquals(SIRequestState.STATE_Cancelled, cancelledReqs[0].getState().getStateStr());
        assertEquals(result.getRequestID(), cancelledReqs[0].getRequestID());
        
        // Spot Instances Snapshot
        // 
        // Total memory: 1280MB
        // Reserved free memory (for ordinary WS requests): 256MB
        // Available basic SI VMs: 8 (128MB each)         
        //
        // Current Requests______________________________________________________________
        // |   Request  | reqVMs | allocVMs |       Bid        |   Status  | Persistent |
        // | singleReq  |    1   |    0     |  previousPrice+1 | CANCELLED |    false   |
        // ------------------------------------------------------------------------------
        // Requested SI VMs: 1
        // Spot price: MINIUM_PRICE (since requestedVMs < availableVMs)              
        
        //Check if request was really cancelled
        request = rm.getSpotRequest(result.getRequestID(), caller);
        assertEquals(SIRequestState.STATE_Cancelled, request.getState().getStateStr());
        assertEquals(0, request.getVMIds().length);
    } 
    
    /**
     * SI Capacity (A) is given by this formula:
     * 
     * A + MAX(256, R) + B = 1280 (pool capacity) 
     * 
     * Where R is the reserved free capacity for WS requests:
     * 
     * R = B*0.3/0.7 (70% maximum utilization constraint - defined in configuration) 
     * 
     * Where B is the memory allocated by ordinary WS VMs.
     * 
     * In this test, B is always 0, so, the SI capacity for this test 
     * is fixed in 8 basic SI VMs (128MB each):
     * 
     * 1024(SI VMs) + 256(minimum free space reserved for WS requests) = 1280(pool capacity)
     * 
     * There are 3 classes of SI requests:
     *    1 - Low bid (lowReq1, lowReq2, lowReq3)
     *    2 - Medium bid (mediumReq1, mediumReq2)
     *    3 - High bid (highReq)
     *    
     * Where, obviously, low bid < medium bid < high bid
     * 
     * This tests works as follows:
     * 
     *  * lowReq1(1 VM), lowReq2(3 VMs) and mediumReq1(3 VMs) are submitted
     *    * Check if they were allocated
     *    * Check if spot price is equal to MINIMUM PRICE (since not all resources are taken)
     *    
     *  * lowReq3(1 VM) is submitted
     *    * Check if it was allocated
     *    * Check if spot price is equal to lowBid (since all resources are taken, and all requests 
     *                                           satisfied)
     *  * mediumReq2(5 VMs) is submitted
     *    * Check if it was allocated
     *    * Check if spot price has raised to mediumBid (since all resources are taken, and just 
     *                                                Medium bid requests can be satisfied)
     *    * Check if low bid requests were pre-empted
     *    * Check if persistent low bid requests are OPEN and non-persistent are CLOSED 
     *    
     *  * highReq(8 VMs) is submitted
     *    * Check if it was allocated
     *    * Check if spot price has raised to highBid (since all resources are taken, and just 
     *                                                High bid requests can be satisfied)
     *    * Check if medium bid requests were pre-empted
     *    * Check if persistent medium bid requests are OPEN and non-persistent are CLOSED                                                                                        
     *    
     * @throws Exception in case an error occurs
     */
    @Test
    @DirtiesContext
    public void multipleSIRequestsOnly() throws Exception {
        logger.debug("multipleRequestsSIOnly");

        //Objects and requests setup
        
        Manager rm = this.locator.getManager();
        
        Caller caller1 = this.populator().getCaller();
        Caller caller2 = this.populator().getCaller();
        Caller caller3 = this.populator().getCaller();
        
        Double previousPrice = rm.getSpotPrice();
        
        final Double lowBid = previousPrice + 1;
        final Double mediumBid = previousPrice + 2;
        final Double highBid = previousPrice + 3;
        
        RequestSI lowReq1 = this.populator().getBasicRequestSI("lowReq1", 1, lowBid, false, 500);
        RequestSI lowReq2 = this.populator().getBasicRequestSI("lowReq2", 3, lowBid, true, 500);
        RequestSI lowReq3 = this.populator().getBasicRequestSI("lowReq3", 1, lowBid, false, 500);
        RequestSI mediumReq1 = this.populator().getBasicRequestSI("mediumReq1", 3, mediumBid, false, 500);
        RequestSI mediumReq2 = this.populator().getBasicRequestSI("mediumReq2", 5, mediumBid, true, 500);
        RequestSI highReq = this.populator().getBasicRequestSI("highReq", 10, highBid, false, 60);
        
        //Submit 3 SI Requests
        String lowReq1Id = rm.requestSpotInstances(lowReq1, caller1).getRequestID();
        String lowReq2Id = rm.requestSpotInstances(lowReq2, caller1).getRequestID();
        String medReq1Id = rm.requestSpotInstances(mediumReq1, caller2).getRequestID();

        // Spot Instances Snapshot
        // 
        // Total memory: 1280MB
        // Reserved free memory (for ordinary WS requests): 256MB (mininum reserved capacity)
        // Available basic SI VMs: 8 (128MB each)         
        //
        // Current Requests___________________________________________________________
        // |   Request  | reqVMs | allocVMs |       Bid        | Status | Persistent |
        // | lowReq1    |   1    |    1     |  previousPrice+1 | ACTIVE |    false   |
        // | lowReq2    |   3    |    3     |  previousPrice+1 | ACTIVE |    true    |
        // | mediumReq1 |   3    |    3     |  previousPrice+2 | ACTIVE |    false   |
        // ----------------------------------------------------------------------------
        // Requested SI VMs: 7
        // Spot price: MINIUM_PRICE (since requestedVMs < availableVMs)           
        
        //New spot price is equal to minimum price
        assertEquals(PricingModelConstants.MINIMUM_PRICE,  rm.getSpotPrice());
        
        //Check if all submitted requests are active
        SpotRequest[] caller1Reqs = rm.getSpotRequestByCaller(caller1);
        for (SpotRequest caller1Req : caller1Reqs) {
            assertEquals(SIRequestState.STATE_Active, caller1Req.getState().getStateStr());
        }        
        SpotRequest[] caller2Reqs = rm.getSpotRequestByCaller(caller2);
        assertEquals(SIRequestState.STATE_Active, caller2Reqs[0].getState().getStateStr());
        
        //Submit another SI Request 
        String lowReq3Id = rm.requestSpotInstances(lowReq3, caller2).getRequestID();
        
        // Spot Instances Snapshot
        // 
        // Total memory: 1280MB
        // Reserved free memory (for ordinary WS requests): 256MB (mininum reserved capacity)
        // Available basic SI VMs: 8 (128MB each)         
        //
        // Current Requests___________________________________________________________
        // |   Request  | reqVMs | allocVMs |       Bid        | Status | Persistent |
        // | lowReq1    |   1    |    1     |  previousPrice+1 | ACTIVE |    false   |
        // | lowReq2    |   3    |    3     |  previousPrice+1 | ACTIVE |    true    |
        // | mediumReq1 |   3    |    3     |  previousPrice+2 | ACTIVE |    false   |
        // | lowReq3    |   1    |    1     |  previousPrice+1 | ACTIVE |    false   |        
        // ---------------------------------------------------------------------------      
        // Requested SI VMs: 8
        // Spot price: lowBid (previousPrice+1) 
        
        //New spot price is equal to lower bid
        assertEquals(lowBid,  rm.getSpotPrice());
        
        //Check if submitted request is active
        assertEquals(SIRequestState.STATE_Active, rm.getSpotRequest(lowReq3Id, caller2).getState().getStateStr());
        
        //Submit another medium-bid SI Request 
        String medReq2Id = rm.requestSpotInstances(mediumReq2, caller2).getRequestID();
        
        // Spot Instances Snapshot
        // 
        // Total memory: 1280MB
        // Reserved free memory (for ordinary WS requests): 256MB (mininum reserved capacity)
        // Available basic SI VMs: 8 (128MB each)         
        // 
        // Current Requests___________________________________________________________
        // |   Request  | reqVMs | allocVMs |       Bid        | Status | Persistent |
        // | lowReq1    |   1    |    0     |  previousPrice+1 | CLOSED |    false   |
        // | lowReq2    |   3    |    0     |  previousPrice+1 | OPEN   |    true    |
        // | mediumReq1 |   3    |    3     |  previousPrice+2 | ACTIVE |    false   |
        // | lowReq3    |   1    |    0     |  previousPrice+1 | CLOSED |    false   |
        // | mediumReq2 |   5    |    5     |  previousPrice+2 | ACTIVE |    true    |
        // ---------------------------------------------------------------------------
        // Requested SI VMs: 13
        // Spot price: mediumBid (previousPrice+2)             
        
        //New spot price is equal to medium bid
        assertEquals(mediumBid,  rm.getSpotPrice());   
        
        //Check if submitted request is active
        assertEquals(SIRequestState.STATE_Active, rm.getSpotRequest(medReq2Id, caller2).getState().getStateStr());
        
        //Check if previous medium-bid request is still active
        assertEquals(SIRequestState.STATE_Active, rm.getSpotRequest(medReq1Id, caller2).getState().getStateStr());
        
        //Check if persistent lower-bid requests are open
        assertEquals(SIRequestState.STATE_Open, rm.getSpotRequest(lowReq2Id, caller1).getState().getStateStr());
        
        //Check if non-persistent lower-bid requests are closed
        assertEquals(SIRequestState.STATE_Closed, rm.getSpotRequest(lowReq1Id, caller1).getState().getStateStr());
        assertEquals(SIRequestState.STATE_Closed, rm.getSpotRequest(lowReq3Id, caller2).getState().getStateStr());
        
        //Submit a higher-bid SI Request 
        String highReqId = rm.requestSpotInstances(highReq, caller3).getRequestID();
        
        // Spot Instances Snapshot
        // 
        // Total memory: 1280MB
        // Reserved free memory (for ordinary WS requests): 256MB (mininum reserved capacity)
        // Available basic SI VMs: 8 (128MB each)         
        // 
        // Current Requests___________________________________________________________
        // |   Request  | reqVMs | allocVMs |       Bid        | Status | Persistent |
        // | lowReq1    |   1    |    0     |  previousPrice+1 | CLOSED |    false   |
        // | lowReq2    |   3    |    0     |  previousPrice+1 | OPEN   |    true    |
        // | mediumReq1 |   3    |    0     |  previousPrice+2 | CLOSED |    false   |
        // | lowReq3    |   1    |    0     |  previousPrice+1 | CLOSED |    false   |
        // | mediumReq2 |   5    |    0     |  previousPrice+2 | OPEN   |    true    |
        // | highReq    |  10    |    8     |  previousPrice+3 | ACTIVE |    false   |            
        // ---------------------------------------------------------------------------              
        // Requested SI VMs: 23
        // Spot price: highBid (previousPrice+3)              
        
        //New spot price is equal to high bid
        assertEquals(highBid,  rm.getSpotPrice());
        
        //Check if submitted request is active
        assertEquals(SIRequestState.STATE_Active, rm.getSpotRequest(highReqId, caller3).getState().getStateStr());
        
        //Check if persistent requests are open
        assertEquals(SIRequestState.STATE_Open, rm.getSpotRequest(lowReq2Id, caller1).getState().getStateStr());
        assertEquals(SIRequestState.STATE_Open, rm.getSpotRequest(medReq2Id, caller2).getState().getStateStr());
        
        //Check if non-persistent requests are closed
        assertEquals(SIRequestState.STATE_Closed, rm.getSpotRequest(lowReq1Id, caller1).getState().getStateStr());
        assertEquals(SIRequestState.STATE_Closed, rm.getSpotRequest(lowReq3Id, caller2).getState().getStateStr());
        assertEquals(SIRequestState.STATE_Closed, rm.getSpotRequest(medReq1Id, caller2).getState().getStateStr());        
    }
    

    /** 
     * SI Capacity (A) is given by this formula:
     * 
     * A + MAX(256, R) + B = 1280 (pool capacity) 
     * 
     * Where R is the reserved free capacity for WS requests:
     * 
     * R = B*0.3/0.7 (70% maximum utilization constraint - defined in configuration) 
     * 
     * Where B is the memory allocated by ordinary WS VMs.
     * 
     * The maximum SI capacity is 8 basic SI VMs (128MB each), when B is 0:
     * 
     * 1024(SI VMs) + 256(minimum free space reserved for WS requests) = 1280(pool capacity)
     * 
     * In this test, the value of B varies as WS requests come and go.
     * 
     * This tests works as follows:
     * 
     * * 3 heterogeneous SI requests are submitted
     *    * Check if they were allocated
     *    * Check spot price
     * * 1 WS request is submitted (wsReq1)
     *    * Check if SI capacity has decreased
     *    * Check if SI VMs were pre-empted
     *    * Check spot price   
     * * Another WS request is submitted (wsReq2)
     *    * Check if SI capacity has decreased
     *    * Check if SI VMs were pre-empted
     *    * Check spot price
     * * Wait for wsReq2 to finish
     *    * Check if SI capacity has raised
     *    * Check if SI VMs were allocated
     *    * Check spot price     
     * * Another WS request is submitted (wsReq3)
     *    * Check if SI capacity has decreased
     *    * Check if SI VMs were pre-empted
     *    * Check spot price
     * * Another WS request is submitted (wsReq4) - SI capacity drops to zero
     *    * Check if SI capacity is zero
     *    * Check if all SI VMs were pre-empted
     *    * Check if spot price is higher than highest bid           
     * * Destroy wsReq3 to finish
     *    * Check if SI capacity has raised
     *    * Check if SI VMs were allocated
     *    * Check spot price 
     *    
     * @throws Exception in case an error occurs
     */
    @Test
    @DirtiesContext
    public void mixedSIandWSrequests() throws Exception {
        logger.debug("mixedSIandWSrequests");

        //Objects and requests setup        
        
        Manager rm = this.locator.getManager();
        
        Caller siCaller = this.populator().getCaller();
        Caller wsCaller = this.populator().getCaller();
        
        Double previousPrice = rm.getSpotPrice();
        
        final Double lowBid = previousPrice + 1;
        final Double mediumBid = previousPrice + 3;
        
        RequestSI lowReq1 = this.populator().getBasicRequestSI("lowReq2", 3, lowBid, false, 500);
        RequestSI lowReq2 = this.populator().getBasicRequestSI("lowReq3", 2, lowBid, true, 500);
        RequestSI mediumReq = this.populator().getBasicRequestSI("mediumReq1", 3, mediumBid, false, 500);

        logger.debug("Submitting SI requests: lowReq1, lowReq2, mediumReq1");         
        
        //Submit 3 SI Requests
        String lowReq1Id = rm.requestSpotInstances(lowReq1, siCaller).getRequestID();
        String lowReq2Id = rm.requestSpotInstances(lowReq2, siCaller).getRequestID();
        String medReqId = rm.requestSpotInstances(mediumReq, siCaller).getRequestID();

        // Spot Instances Snapshot
        // 
        // Total memory: 1280MB
        // Reserved free memory (for ordinary WS requests): 256MB (mininum reserved capacity)
        // Available basic SI VMs: 8 (128MB each)         
        //
        // Current Requests___________________________________________________________
        // |   Request  | reqVMs | allocVMs |       Bid        | Status | Persistent |
        // | lowReq1    |   3    |     3    | previousPrice+1  | ACTIVE |    false   |
        // | lowReq2    |   2    |     2    | previousPrice+1  | ACTIVE |    true    |
        // | mediumReq1 |   3    |     3    | previousPrice+3  | ACTIVE |    false   |
        // ---------------------------------------------------------------------------
        // Requested SI VMs (ALIVE requests): 8
        // Spot price: lowBid (previousPrice+1)          
        
        //Check available SI VMs
        assertEquals(8,  getAvailableResources());
        
        //New spot price is equal to lower bid
        assertEquals(lowBid,  rm.getSpotPrice());
        
        //Check if all submitted requests are active and fulfilled
        SpotRequest lowReq1SR = rm.getSpotRequest(lowReq1Id, siCaller);
        assertEquals(SIRequestState.STATE_Active, lowReq1SR.getState().getStateStr());
        assertEquals(3, lowReq1SR.getVMIds().length);        
        
        SpotRequest lowReq2SR = rm.getSpotRequest(lowReq2Id, siCaller);
        assertEquals(SIRequestState.STATE_Active, lowReq2SR.getState().getStateStr());
        assertEquals(2, lowReq2SR.getVMIds().length);
        
        SpotRequest medReqSR = rm.getSpotRequest(medReqId, siCaller);
        assertEquals(SIRequestState.STATE_Active, medReqSR.getState().getStateStr());
        assertEquals(3, medReqSR.getVMIds().length); 
        
        logger.debug("Submitting WS request: wsReq1 (2 VMs, 120MB RAM each)");        
        
        CreateRequest wsReq1 = this.populator().getCreateRequest("wsReq1", 1000, 120, 2);
        CreateResult wsReq1Result = rm.create(wsReq1, wsCaller);
        
        Long mark = System.currentTimeMillis();
        
        // Spot Instances Snapshot
        // 
        // Total memory: 1280MB
        // Used WS memory: 240MB
        // Reserved free memory (for ordinary WS requests): 256MB (minimum reserved capacity)
        // Available basic SI VMs: 6 (128MB each)         
        //
        // Current Requests___________________________________________________________
        // |   Request  | reqVMs | allocVMs |       Bid        | Status | Persistent |
        // | lowReq1    |   3    |     2    | previousPrice+1  | ACTIVE |    false   |
        // | lowReq2    |   2    |     1    | previousPrice+1  | ACTIVE |    true    |
        // | mediumReq1 |   3    |     3    | previousPrice+3  | ACTIVE |    false   |
        // ---------------------------------------------------------------------------
        // Requested SI VMs (ALIVE requests): 7 (1 from lowReq1 was closed)
        // Spot price: lowBid (previousPrice+1)       

        logger.debug("Waiting 2 seconds for resources to be pre-empted.");
        Thread.sleep(2000);        
        
        //Check available SI VMs
        assertEquals(6,  getAvailableResources());        
        
        //New spot price is equal to lower bid
        assertEquals(lowBid,  rm.getSpotPrice());        
        
        lowReq1SR = rm.getSpotRequest(lowReq1Id, siCaller);
        assertEquals(SIRequestState.STATE_Active, lowReq1SR.getState().getStateStr());
        assertEquals(2, lowReq1SR.getVMIds().length);        
        
        lowReq2SR = rm.getSpotRequest(lowReq2Id, siCaller);
        assertEquals(SIRequestState.STATE_Active, lowReq2SR.getState().getStateStr());
        assertEquals(1, lowReq2SR.getVMIds().length);
        
        medReqSR = rm.getSpotRequest(medReqId, siCaller);
        assertEquals(SIRequestState.STATE_Active, medReqSR.getState().getStateStr());
        assertEquals(3, medReqSR.getVMIds().length); 
        
        logger.debug("Submitting WS request: wsReq2 (2 VMs, 110MB RAM each)");        
        
        CreateRequest wsReq2 = this.populator().getCreateRequest("wsReq2", TASK_TIME, 110, 2);
        rm.create(wsReq2, wsCaller); 
        
        // Spot Instances Snapshot
        // 
        // Total memory: 1280MB
        // Used WS memory: 460MB
        // Reserved free memory (for ordinary WS requests): 256MB (minimum reserved capacity)
        // Available basic SI VMs: 4 (128MB each)         
        //
        // Current Requests___________________________________________________________
        // |   Request  | reqVMs | allocVMs |       Bid        | Status | Persistent |
        // | lowReq1    |   3    |     1    | previousPrice+1  | ACTIVE |    false   |
        // | lowReq2    |   2    |     0    | previousPrice+1  | OPEN   |    true    |
        // | mediumReq1 |   3    |     3    | previousPrice+3  | ACTIVE |    false   |
        // ---------------------------------------------------------------------------
        // Requested SI VMs (ALIVE requests): 6 (2 from lowReq1 were closed)
        // Spot price: lowBid (previousPrice+1)
        
        logger.debug("Waiting 2 seconds for SI VMs to be pre-empted.");
        Thread.sleep(2000);        
        
        //Check available SI VMs
        assertEquals(4,  getAvailableResources());            
        
        //New spot price is equal to lower bid
        assertEquals(lowBid,  rm.getSpotPrice());        
        
        lowReq1SR = rm.getSpotRequest(lowReq1Id, siCaller);
        assertEquals(SIRequestState.STATE_Active, lowReq1SR.getState().getStateStr());
        assertEquals(1, lowReq1SR.getVMIds().length);        
        
        lowReq2SR = rm.getSpotRequest(lowReq2Id, siCaller);
        assertEquals(SIRequestState.STATE_Open, lowReq2SR.getState().getStateStr());
        assertEquals(0, lowReq2SR.getVMIds().length);
        
        medReqSR = rm.getSpotRequest(medReqId, siCaller);
        assertEquals(SIRequestState.STATE_Active, medReqSR.getState().getStateStr());
        assertEquals(3, medReqSR.getVMIds().length);
        
        //Wait for wsReq2 to finish
                
        long elapsed = System.currentTimeMillis()-mark;
        long sleepTime = (TASK_TIME*1000 - elapsed) + BUFFER;
        if(sleepTime > 0){
            logger.debug("Waiting " + sleepTime + " milliseconds for wsReq2 to finish.");
            Thread.sleep(sleepTime);
        }
        
        // Spot Instances Snapshot
        // 
        // Total memory: 1280MB
        // Used WS memory: 240MB
        // Reserved free memory (for ordinary WS requests): 256MB (minimum reserved capacity)
        // Available basic SI VMs: 6 (128MB each)         
        //
        // Current Requests___________________________________________________________
        // |   Request  | reqVMs | allocVMs |       Bid        | Status | Persistent |
        // | lowReq1    |   3    |     1    | previousPrice+1  | ACTIVE |    false   |**
        // | lowReq2    |   2    |     2    | previousPrice+1  | ACTIVE |    true    |
        // | mediumReq1 |   3    |     3    | previousPrice+3  | ACTIVE |    false   |
        // ---------------------------------------------------------------------------
        // Requested SI VMs (ALIVE requests): 6 (2 from lowReq1 were closed)
        // Spot price: lowBid (previousPrice+1)        
        //
        // ** lowReq1 is not persistent, so no more VMs are allocated for this
        //    request (since 2 VMs were already finished (pre-empted))
       
        //Check available SI VMs
        assertEquals(6,  getAvailableResources());            
        
        //Spot price is equal to lower bid
        assertEquals(lowBid,  rm.getSpotPrice());        
        
        lowReq1SR = rm.getSpotRequest(lowReq1Id, siCaller);
        assertEquals(SIRequestState.STATE_Active, lowReq1SR.getState().getStateStr());
        assertEquals(1, lowReq1SR.getVMIds().length);        
        
        lowReq2SR = rm.getSpotRequest(lowReq2Id, siCaller);
        assertEquals(SIRequestState.STATE_Active, lowReq2SR.getState().getStateStr());
        assertEquals(2, lowReq2SR.getVMIds().length);
        
        medReqSR = rm.getSpotRequest(medReqId, siCaller);
        assertEquals(SIRequestState.STATE_Active, medReqSR.getState().getStateStr());
        assertEquals(3, medReqSR.getVMIds().length);        
        
        logger.debug("Submitting WS request: wsReq3 (2 VMs, 256MB RAM each)");               
        
        CreateRequest wsReq3 = this.populator().getCreateRequest("wsReq3", 500, 256, 2);
        CreateResult wsReq3Result = rm.create(wsReq3, wsCaller);  

        // Spot Instances Snapshot
        // 
        // Total memory: 1280MB
        // Used WS memory: 752MB
        // Reserved capacity (for ordinary WS requests): 322MB (to ensure 70% max utilization)
        // Available basic SI VMs: 1 (128MB each)         
        //
        // Current Requests___________________________________________________________
        // |   Request  | reqVMs | allocVMs |       Bid        | Status | Persistent |
        // | lowReq1    |   3    |     0    | previousPrice+1  | CLOSED |    false   |
        // | lowReq2    |   2    |     0    | previousPrice+1  | OPEN   |    true    |
        // | mediumReq1 |   3    |     1    | previousPrice+3  | ACTIVE |    false   |
        // ---------------------------------------------------------------------------
        // Requested SI VMs (ALIVE requests): 3
        // Spot price: lowBid (previousPrice+1)
        
        logger.debug("Waiting 2 seconds for SI VMs to be pre-empted.");
        Thread.sleep(2000);        
        
        //Check available SI VMs
        assertEquals(1,  getAvailableResources());            
        
        //New spot price is equal to medium bid
        assertEquals(mediumBid,  rm.getSpotPrice());        
        
        lowReq1SR = rm.getSpotRequest(lowReq1Id, siCaller);
        assertEquals(SIRequestState.STATE_Closed, lowReq1SR.getState().getStateStr());
        assertEquals(0, lowReq1SR.getVMIds().length);        
        
        lowReq2SR = rm.getSpotRequest(lowReq2Id, siCaller);
        assertEquals(SIRequestState.STATE_Open, lowReq2SR.getState().getStateStr());
        assertEquals(0, lowReq2SR.getVMIds().length);
        
        medReqSR = rm.getSpotRequest(medReqId, siCaller);
        assertEquals(SIRequestState.STATE_Active, medReqSR.getState().getStateStr());
        assertEquals(1, medReqSR.getVMIds().length);
                
        logger.debug("Submitting WS request: wsReq4 (1 VM with 256MB RAM)");                       
        
        CreateRequest wsReq4 = this.populator().getCreateRequest("wsReq4", 500, 256, 1);
        CreateResult wsReq4Result = rm.create(wsReq4, wsCaller);
        
        // Spot Instances Snapshot
        // 
        // Total memory: 1280MB
        // Used WS memory: 1008MB
        // Reserved capacity (for ordinary WS requests): 432MB (to ensure 70% max utilization)
        // Available basic SI VMs: 0        
        //
        // Current Requests___________________________________________________________
        // |   Request  | reqVMs | allocVMs |       Bid        | Status | Persistent |
        // | lowReq1    |   3    |     0    | previousPrice+1  | CLOSED |    false   |
        // | lowReq2    |   2    |     0    | previousPrice+1  | OPEN   |    true    |
        // | mediumReq1 |   3    |     0    | previousPrice+3  | CLOSED |    false   |
        // ---------------------------------------------------------------------------
        // Requested SI VMs (ALIVE requests): 2
        // Spot price: lowBid + 1
        
        logger.debug("Waiting 2 seconds for SI VMs to be pre-empted.");
        Thread.sleep(2000);        
        
        //Check available SI VMs
        assertEquals(0,  getAvailableResources());             
        
        //New spot price is equal to lower bid + 0.1 (since lower bid is the highest ALIVE bid)
        assertEquals(lowBid+0.1,  rm.getSpotPrice());       
        
        lowReq1SR = rm.getSpotRequest(lowReq1Id, siCaller);
        assertEquals(SIRequestState.STATE_Closed, lowReq1SR.getState().getStateStr());
        assertEquals(0, lowReq1SR.getVMIds().length);        
        
        lowReq2SR = rm.getSpotRequest(lowReq2Id, siCaller);
        assertEquals(SIRequestState.STATE_Open, lowReq2SR.getState().getStateStr());
        assertEquals(0, lowReq2SR.getVMIds().length);
        
        medReqSR = rm.getSpotRequest(medReqId, siCaller);
        assertEquals(SIRequestState.STATE_Closed, medReqSR.getState().getStateStr());
        assertEquals(0, medReqSR.getVMIds().length);   
        
        logger.debug("Destroying wsReq3 VMs: 2 VMs, 256MB RAM each");                       
        
        rm.trash(wsReq3Result.getGroupID(), Manager.GROUP, wsCaller);
        
        // Spot Instances Snapshot
        // 
        // Total memory: 1280MB
        // Used WS memory: 496MB
        // Reserved capacity (for ordinary WS requests): 256MB (minimum reserved capacity)
        // Available basic SI VMs: 4
        //
        // Current Requests___________________________________________________________
        // |   Request  | reqVMs | allocVMs |       Bid        | Status | Persistent |
        // | lowReq1    |   3    |     0    | previousPrice+1  | CLOSED |    false   |
        // | lowReq2    |   2    |     2    | previousPrice+1  | ACTIVE |    true    |
        // | mediumReq1 |   3    |     0    | previousPrice+3  | CLOSED |    false   |
        // ---------------------------------------------------------------------------
        // Requested SI VMs (ALIVE requests): 2
        // Spot price: lowBid
        
        logger.debug("Waiting 2 seconds for resources to be allocated.");
        Thread.sleep(2000);        
        
        //Check available SI VMs
        assertEquals(4,  getAvailableResources());             
        
        //New spot price is equal to minimum price
        assertEquals(PricingModelConstants.MINIMUM_PRICE,  rm.getSpotPrice());
        
        lowReq1SR = rm.getSpotRequest(lowReq1Id, siCaller);
        assertEquals(SIRequestState.STATE_Closed, lowReq1SR.getState().getStateStr());
        assertEquals(0, lowReq1SR.getVMIds().length);        
        
        lowReq2SR = rm.getSpotRequest(lowReq2Id, siCaller);
        assertEquals(SIRequestState.STATE_Active, lowReq2SR.getState().getStateStr());
        assertEquals(2, lowReq2SR.getVMIds().length);
        
        medReqSR = rm.getSpotRequest(medReqId, siCaller);
        assertEquals(SIRequestState.STATE_Closed, medReqSR.getState().getStateStr());
        assertEquals(0, medReqSR.getVMIds().length);           
        
        logger.debug("Destroying remaining WS VMs.");     
        
        rm.trash(wsReq1Result.getGroupID(), Manager.GROUP, wsCaller);
        rm.trash(wsReq4Result.getVMs()[0].getID(), Manager.INSTANCE, wsCaller);
    }    
    
    public int getAvailableResources(){
        SpotInstancesManagerImpl siManager = (SpotInstancesManagerImpl) applicationContext.getBean(SPOTINSTANCES_MANAGER_BEAN_NAME);
        return siManager.getAvailableResources();
    }
    
}
