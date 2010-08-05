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

import java.util.Calendar;

import org.globus.workspace.async.AsyncRequestManagerImpl;
import org.globus.workspace.testing.NimbusTestBase;
import org.globus.workspace.testing.NimbusTestContextLoader;
import org.nimbustools.api.repr.AsyncCreateRequest;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.CreateRequest;
import org.nimbustools.api.repr.CreateResult;
import org.nimbustools.api.repr.RequestInfo;
import org.nimbustools.api.repr.SpotCreateRequest;
import org.nimbustools.api.repr.SpotPriceEntry;
import org.nimbustools.api.repr.SpotRequestInfo;
import org.nimbustools.api.repr.si.RequestState;
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

    private static final String SPOTINSTANCES_MANAGER_BEAN_NAME = "nimbus-rm.async.manager";
    private static final int TASK_TIME = 10;
    private static final int BUFFER = 10000;
    
    private static final Double MINIMUM_PRICE = 0.1;

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
     *    * Check if request was allocated
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

        Calendar ts1 = Calendar.getInstance();
        
        //Objects and requests setup        
        Caller caller = this.populator().getCaller();
        Caller superuser = this.populator().getSuperuserCaller();
        Manager rm = this.locator.getManager();
        
        Double previousPrice = rm.getSpotPrice();
        
        logger.debug("Submitting backfill request..");
        
        AsyncCreateRequest backfillRequest = this.populator().getBackfillRequest("backfill", 10);
        RequestInfo backfillResult = rm.addBackfillRequest(backfillRequest, superuser);
        
        // Spot Instances Snapshot
        // 
        // Total memory: 1280MB
        // Reserved free memory (for ordinary WS requests): 256MB  (minimum reserved capacity)
        // Available basic SI VMs: 8 (128MB x 8 = 1024)
        //
        // Current SI Requests________________________________________________________
        // |   Request  | reqVMs | allocVMs |       Bid        | Status | Persistent |
        // ---------------------------------------------------------------------------
        // Backfill Requests__________________________
        // |   Request  | reqVMs | allocVMs | Status |
        // | backfill   |   10   |    8     | ACTIVE |
        // -------------------------------------------
        // Requested SI VMs (alive requests): 0
        // Spot price: MINIUM_PRICE (since requestedVMs < availableVMs)           

        logger.debug("Waiting 2 seconds for resources to be allocated.");
        Thread.sleep(2000);         
        
        //Verify there are no spot instance requests
        SpotRequestInfo[] spotRequestByCaller = rm.getSpotRequestsByCaller(superuser);
        assertEquals(0, spotRequestByCaller.length);
        spotRequestByCaller = rm.getSpotRequestsByCaller(caller);
        assertEquals(0, spotRequestByCaller.length);          
        
        //Check backfill request state
        RequestInfo[] backfillRequestsByCaller = rm.getBackfillRequestsByCaller(superuser);
        assertEquals(1, backfillRequestsByCaller.length);          
        String[] vmIdsBefore = backfillRequestsByCaller[0].getVMIds();
        assertEquals(8, vmIdsBefore.length);
        assertEquals(RequestState.STATE_Active, backfillRequestsByCaller[0].getState().getStateStr());
        
        //New spot price is equal to minimum price (since there are still available resources)
        assertEquals(MINIMUM_PRICE,  rm.getSpotPrice());    
        
        final Double bid = previousPrice + 1;
        SpotCreateRequest requestSI = this.populator().getBasicRequestSI("singleRequest", 1, bid, false);
        
        logger.debug("Submitting basic SI request: singleRequest");        
        
        //Request spot instances
        SpotRequestInfo result = rm.requestSpotInstances(requestSI, caller);
        
        //Check result
        //note: cannot check state at this point, because it can either be 
        //OPEN (not scheduled yet) or ACTIVE (already scheduled)
        assertEquals(bid, result.getSpotPrice());
        assertTrue(!result.isPersistent());
        
        // Spot Instances Snapshot
        // 
        // Total memory: 1280MB
        // Reserved free memory (for ordinary WS requests): 256MB  (minimum reserved capacity)
        // Available basic SI VMs: 8 (128MB x 8 = 1024)
        //
        // Current SI Requests___________________________________________________________
        // |   Request  | reqVMs | allocVMs |       Bid        | Status | Persistent |
        // | singleReq  |    1   |    1     |  previousPrice+1 | ACTIVE |    false   |
        // ---------------------------------------------------------------------------
        // Backfill Requests__________________________
        // |   Request  | reqVMs | allocVMs | Status |
        // | backfill   |   10   |    7     | ACTIVE |
        // -------------------------------------------        
        // Requested SI VMs (alive requests): 1
        // Spot price: MINIUM_PRICE (since requestedVMs < availableVMs)                  
        
        //Check backfill request state
        backfillRequestsByCaller = rm.getBackfillRequestsByCaller(superuser);
        assertEquals(1, backfillRequestsByCaller.length);          
        String[] vmIdsAfter = backfillRequestsByCaller[0].getVMIds();
        assertEquals(7, vmIdsAfter.length);
        assertEquals(RequestState.STATE_Active, backfillRequestsByCaller[0].getState().getStateStr());  
        
        //This check is to ensure that the VMs now are the same as before (ie. VMs were not restarted)
        Integer equalVms = 0;
        for (String vmIdBefore : vmIdsBefore) {
            for (String vmIdAfter : vmIdsAfter) {
                if(vmIdAfter.equals(vmIdBefore)){
                    equalVms++;
                    break;
                }
            }
        }
        assertEquals(new Integer(7), equalVms);
        
        //New spot price is equal to minimum price (since there are still available resources)
        assertEquals(MINIMUM_PRICE,  rm.getSpotPrice());
        
        spotRequestByCaller = rm.getSpotRequestsByCaller(caller);
        assertEquals(1, spotRequestByCaller.length);
        
        //Let's assume the request was already scheduled, so the state should be ACTIVE
        SpotRequestInfo request = rm.getSpotRequest(result.getRequestID(), caller);
        assertEquals(RequestState.STATE_Active, request.getState().getStateStr());
        assertEquals(1, request.getVMIds().length);
                
        logger.debug("Cancelling basic SI request: singleRequest");        
        
        SpotRequestInfo[] cancelledReqs = rm.cancelSpotInstanceRequests(new String[]{result.getRequestID()}, caller);
        assertEquals(1, cancelledReqs.length);
        assertEquals(RequestState.STATE_Canceled, cancelledReqs[0].getState().getStateStr());
        assertEquals(result.getRequestID(), cancelledReqs[0].getRequestID());
        
        // Spot Instances Snapshot
        // 
        // Total memory: 1280MB
        // Reserved free memory (for ordinary WS requests): 256MB
        // Available basic SI VMs: 8 (128MB x 8 = 1024)         
        //
        // Current Requests______________________________________________________________
        // |   Request  | reqVMs | allocVMs |       Bid        |   Status  | Persistent |
        // | singleReq  |    1   |    0     |  previousPrice+1 | CANCELLED |    false   |
        // ------------------------------------------------------------------------------
        // Backfill Requests__________________________
        // |   Request  | reqVMs | allocVMs | Status |
        // | backfill   |   10   |    8     | ACTIVE |
        // -------------------------------------------           
        // Requested SI VMs (alive requests): 0
        // Spot price: MINIUM_PRICE (since requestedVMs < availableVMs)        
        
        logger.debug("Waiting 2 seconds for resources to be pre-empted.");        
        Thread.sleep(2000);
        
        //Check backfill request state
        backfillRequestsByCaller = rm.getBackfillRequestsByCaller(superuser);
        assertEquals(1, backfillRequestsByCaller.length);          
        assertEquals(8, backfillRequestsByCaller[0].getVMIds().length);
        assertEquals(RequestState.STATE_Active, backfillRequestsByCaller[0].getState().getStateStr());             
        
        //Check if request was really cancelled
        request = rm.getSpotRequest(result.getRequestID(), caller);
        assertEquals(RequestState.STATE_Canceled, request.getState().getStateStr());
        assertEquals(0, request.getVMIds().length);
        
        logger.debug("Cancelling backfill request.");        
        
        RequestInfo[] cancelledBackfillReqs = rm.cancelBackfillRequests(new String[]{backfillResult.getRequestID()}, superuser);
        assertEquals(1, cancelledBackfillReqs.length);
        assertEquals(RequestState.STATE_Canceled, cancelledBackfillReqs[0].getState().getStateStr());
        assertEquals(backfillResult.getRequestID(), cancelledBackfillReqs[0].getRequestID());        
        
        //Check backfill request state
        RequestInfo backfillReq = rm.getBackfillRequest(backfillResult.getRequestID(), superuser);
        assertEquals(0, backfillReq.getVMIds().length);
        assertEquals(RequestState.STATE_Canceled, backfillReq.getState().getStateStr());         
        
        Double[] prices = {MINIMUM_PRICE};
        
        //Check spot price history
        SpotPriceEntry[] history = rm.getSpotPriceHistory();
        
        assertEquals(prices.length, history.length);
        
        for (int i = 0; i < history.length; i++) {
            assertEquals(prices[i], history[i].getSpotPrice());
        }
        
        history = rm.getSpotPriceHistory(ts1, null);
        
        assertEquals(history.length, 0);
        
        history = rm.getSpotPriceHistory(null, ts1);
        
        assertEquals(prices.length, history.length);
        
        for (int i = 0; i < history.length; i++) {
            assertEquals(prices[i], history[i].getSpotPrice());
        }        
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
     * For this test, there are 3 classes of SI requests:
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
     *    * Check if spot price is equal to MINIMUM PRICE (since there are still SI VMs available)
     *    
     *  * lowReq3(1 VM) is submitted
     *    * Check if it was allocated
     *    * Check if spot price is equal to lowBid (since all available SI VMs are allocated)
     *    
     *  * mediumReq2(5 VMs) is submitted
     *    * Check if it was allocated
     *    * Check if spot price has raised to mediumBid (since all available SI VMs are allocated, 
     *                                                  and just medium bid requests can be satisfied)
     *    * Check if low bid requests were pre-empted
     *    * Check if persistent low bid requests are OPEN and non-persistent are CLOSED 
     *    
     *  * highReq(8 VMs) is submitted
     *    * Check if it was allocated
     *    * Check if spot price has raised to highBid (since all available SI VMs are allocated, 
     *                                                  and just high bid requests can be satisfied)
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
        Caller superuser = this.populator().getSuperuserCaller();
        
        logger.debug("Submitting backfill requests..");
        
        AsyncCreateRequest backfill1 = this.populator().getBackfillRequest("backfill1", 3);
        RequestInfo backfill1Result = rm.addBackfillRequest(backfill1, superuser);        
        AsyncCreateRequest backfill2 = this.populator().getBackfillRequest("backfill2", 5);
        RequestInfo backfill2Result = rm.addBackfillRequest(backfill2, superuser);            
        
        // Spot Instances Snapshot
        // 
        // Total memory: 1280MB
        // Reserved free memory (for ordinary WS requests): 256MB  (minimum reserved capacity)
        // Available basic SI VMs: 8 (128MB x 8 = 1024)
        //
        // Current SI Requests________________________________________________________
        // |   Request  | reqVMs | allocVMs |       Bid        | Status | Persistent |
        // ---------------------------------------------------------------------------
        // Backfill Requests__________________________
        // |   Request   | reqVMs | allocVMs | Status |
        // |  backfill1  |   3    |    3     | ACTIVE |
        // |  backfill2  |   5    |    5     | ACTIVE |      
        // --------------------------------------------
        // Requested SI VMs (alive requests): 0
        // Spot price: MINIUM_PRICE (since requestedVMs < availableVMs)           

        logger.debug("Waiting 2 seconds for resources to be allocated.");
        Thread.sleep(2000);              
        
        //Verify there are no spot instance requests
        SpotRequestInfo[] spotRequestByCaller = rm.getSpotRequestsByCaller(superuser);
        assertEquals(0, spotRequestByCaller.length);
        spotRequestByCaller = rm.getSpotRequestsByCaller(caller1);
        assertEquals(0, spotRequestByCaller.length); 
        spotRequestByCaller = rm.getSpotRequestsByCaller(caller2);
        assertEquals(0, spotRequestByCaller.length);
        spotRequestByCaller = rm.getSpotRequestsByCaller(caller3);
        assertEquals(0, spotRequestByCaller.length);        
        
        //Check backfill request state
        RequestInfo[] backfillRequestsByCaller = rm.getBackfillRequestsByCaller(superuser);
        assertEquals(2, backfillRequestsByCaller.length);
        
        backfill1Result = rm.getBackfillRequest(backfill1Result.getRequestID(), superuser);
        assertEquals(3, backfill1Result.getVMIds().length);
        assertEquals(RequestState.STATE_Active, backfill1Result.getState().getStateStr());
        
        backfill2Result = rm.getBackfillRequest(backfill2Result.getRequestID(), superuser);        
        assertEquals(5, backfill2Result.getVMIds().length);
        assertEquals(RequestState.STATE_Active, backfill2Result.getState().getStateStr());        
        
        //New spot price is equal to minimum price (since there are still available resources)
        assertEquals(MINIMUM_PRICE,  rm.getSpotPrice());  
        
        Double previousPrice = rm.getSpotPrice();
        
        final Double lowBid = previousPrice + 1;
        final Double mediumBid = previousPrice + 2;
        final Double highBid = previousPrice + 3;
        
        SpotCreateRequest lowReq1 = this.populator().getBasicRequestSI("lowReq1", 1, lowBid, false);
        SpotCreateRequest lowReq2 = this.populator().getBasicRequestSI("lowReq2", 3, lowBid, true);
        SpotCreateRequest lowReq3 = this.populator().getBasicRequestSI("lowReq3", 1, lowBid, false);
        SpotCreateRequest mediumReq1 = this.populator().getBasicRequestSI("mediumReq1", 3, mediumBid, false);
        SpotCreateRequest mediumReq2 = this.populator().getBasicRequestSI("mediumReq2", 5, mediumBid, true);
        SpotCreateRequest highReq = this.populator().getBasicRequestSI("highReq", 10, highBid, false);
        
        logger.debug("Submitting SI requests: lowReq1, lowReq2, mediumReq1");         
        
        //Submit 3 SI Requests
        String lowReq1Id = rm.requestSpotInstances(lowReq1, caller1).getRequestID();
        String lowReq2Id = rm.requestSpotInstances(lowReq2, caller1).getRequestID();
        String medReq1Id = rm.requestSpotInstances(mediumReq1, caller2).getRequestID();

        // Spot Instances Snapshot
        // 
        // Total memory: 1280MB
        // Reserved free memory (for ordinary WS requests): 256MB (minimum reserved capacity)
        // Available basic SI VMs: 8 (128MB x 8 = 1024MB)         
        //
        // Current Requests___________________________________________________________
        // |   Request  | reqVMs | allocVMs |       Bid        | Status | Persistent |
        // | lowReq1    |   1    |    1     |  previousPrice+1 | ACTIVE |    false   |
        // | lowReq2    |   3    |    3     |  previousPrice+1 | ACTIVE |    true    |
        // | mediumReq1 |   3    |    3     |  previousPrice+2 | ACTIVE |    false   |
        // ---------------------------------------------------------------------------
        // Backfill Requests__________________________
        // |   Request   | reqVMs | allocVMs | Status |
        // |  backfill1  |   3    |    1     | ACTIVE |
        // |  backfill2  |   5    |    0     | OPEN   |      
        // --------------------------------------------        
        // Requested SI VMs (alive requests): 7
        // Spot price: MINIMUM_PRICE (since requestedVMs < availableVMs)           
        
        Calendar ts2 = Calendar.getInstance();        
        
        //logger.debug("Waiting 2 seconds for resources to be pre-empted.");
        //Thread.sleep(2000);        
        
        //New spot price is equal to minimum price
        assertEquals(MINIMUM_PRICE,  rm.getSpotPrice());
        
        //Check if all submitted requests are active
        SpotRequestInfo[] caller1Reqs = rm.getSpotRequestsByCaller(caller1);
        for (SpotRequestInfo caller1Req : caller1Reqs) {
            assertEquals(RequestState.STATE_Active, caller1Req.getState().getStateStr());
        }        
        SpotRequestInfo[] caller2Reqs = rm.getSpotRequestsByCaller(caller2);
        assertEquals(RequestState.STATE_Active, caller2Reqs[0].getState().getStateStr());
        
        //Check backfill request state
        backfillRequestsByCaller = rm.getBackfillRequestsByCaller(superuser);
        assertEquals(2, backfillRequestsByCaller.length);
        
        backfill1Result = rm.getBackfillRequest(backfill1Result.getRequestID(), superuser);
        assertEquals(1, backfill1Result.getVMIds().length);
        assertEquals(RequestState.STATE_Active, backfill1Result.getState().getStateStr());
        
        backfill2Result = rm.getBackfillRequest(backfill2Result.getRequestID(), superuser);        
        assertEquals(0, backfill2Result.getVMIds().length);
        assertEquals(RequestState.STATE_Open, backfill2Result.getState().getStateStr());      
        
        logger.debug("Submitting SI request: lowReq3");                 
        
        //Submit another SI Request 
        String lowReq3Id = rm.requestSpotInstances(lowReq3, caller2).getRequestID();
        
        // Spot Instances Snapshot
        // 
        // Total memory: 1280MB
        // Reserved free memory (for ordinary WS requests): 256MB (minimum reserved capacity)
        // Available basic SI VMs: 8 (128MB x 8 = 1024MB)         
        //
        // Current Requests___________________________________________________________
        // |   Request  | reqVMs | allocVMs |       Bid        | Status | Persistent |
        // | lowReq1    |   1    |    1     |  previousPrice+1 | ACTIVE |    false   |
        // | lowReq2    |   3    |    3     |  previousPrice+1 | ACTIVE |    true    |
        // | mediumReq1 |   3    |    3     |  previousPrice+2 | ACTIVE |    false   |
        // | lowReq3    |   1    |    1     |  previousPrice+1 | ACTIVE |    false   |        
        // ---------------------------------------------------------------------------      
        // Backfill Requests__________________________
        // |   Request   | reqVMs | allocVMs | Status |
        // |  backfill1  |   3    |    0     | OPEN   |
        // |  backfill2  |   5    |    0     | OPEN   |      
        // --------------------------------------------        
        // Requested SI VMs (alive requests): 8
        // Spot price: lowBid (previousPrice+1) 
                
        //New spot price is equal to lower bid
        assertEquals(lowBid,  rm.getSpotPrice());
        
        //Check if submitted request is active
        assertEquals(RequestState.STATE_Active, rm.getSpotRequest(lowReq3Id, caller2).getState().getStateStr());
        
        //Check backfill request state
        backfillRequestsByCaller = rm.getBackfillRequestsByCaller(superuser);
        assertEquals(2, backfillRequestsByCaller.length);
        assertEquals(0, backfillRequestsByCaller[0].getVMIds().length);
        assertEquals(RequestState.STATE_Open, backfillRequestsByCaller[0].getState().getStateStr());
        assertEquals(0, backfillRequestsByCaller[1].getVMIds().length);
        assertEquals(RequestState.STATE_Open, backfillRequestsByCaller[1].getState().getStateStr());        
        
        logger.debug("Submitting SI request: mediumReq2");
        
        //Submit another medium-bid SI Request 
        String medReq2Id = rm.requestSpotInstances(mediumReq2, caller2).getRequestID();
        
        // Spot Instances Snapshot
        // 
        // Total memory: 1280MB
        // Reserved free memory (for ordinary WS requests): 256MB (minimum reserved capacity)
        // Available basic SI VMs: 8 (128MB x 8 = 1024MB)         
        // 
        // Current Requests___________________________________________________________
        // |   Request  | reqVMs | allocVMs |       Bid        | Status | Persistent |
        // | lowReq1    |   1    |    0     |  previousPrice+1 | CLOSED |    false   |
        // | lowReq2    |   3    |    0     |  previousPrice+1 | OPEN   |    true    |
        // | mediumReq1 |   3    |    3     |  previousPrice+2 | ACTIVE |    false   |
        // | lowReq3    |   1    |    0     |  previousPrice+1 | CLOSED |    false   |
        // | mediumReq2 |   5    |    5     |  previousPrice+2 | ACTIVE |    true    |
        // ---------------------------------------------------------------------------
        // Backfill Requests__________________________
        // |   Request   | reqVMs | allocVMs | Status |
        // |  backfill1  |   3    |    0     | OPEN   |
        // |  backfill2  |   5    |    0     | OPEN   |      
        // --------------------------------------------        
        // Requested SI VMs (alive requests): 11
        // Spot price: mediumBid (previousPrice+2)             

        Calendar ts4 = Calendar.getInstance();        
        
        logger.debug("Waiting 2 seconds for resources to be pre-empted.");
        Thread.sleep(2000);         
        
        //New spot price is equal to medium bid
        assertEquals(mediumBid,  rm.getSpotPrice());   
        
        //Check if submitted request is active
        assertEquals(RequestState.STATE_Active, rm.getSpotRequest(medReq2Id, caller2).getState().getStateStr());
        
        //Check if previous medium-bid request is still active
        assertEquals(RequestState.STATE_Active, rm.getSpotRequest(medReq1Id, caller2).getState().getStateStr());
        
        //Check if persistent lower-bid requests are open
        assertEquals(RequestState.STATE_Open, rm.getSpotRequest(lowReq2Id, caller1).getState().getStateStr());
        
        //Check if non-persistent lower-bid requests are closed
        assertEquals(RequestState.STATE_Closed, rm.getSpotRequest(lowReq1Id, caller1).getState().getStateStr());
        assertEquals(RequestState.STATE_Closed, rm.getSpotRequest(lowReq3Id, caller2).getState().getStateStr());
        
        //Check backfill request state
        backfillRequestsByCaller = rm.getBackfillRequestsByCaller(superuser);
        assertEquals(2, backfillRequestsByCaller.length);
        assertEquals(0, backfillRequestsByCaller[0].getVMIds().length);
        assertEquals(RequestState.STATE_Open, backfillRequestsByCaller[0].getState().getStateStr());
        assertEquals(0, backfillRequestsByCaller[1].getVMIds().length);
        assertEquals(RequestState.STATE_Open, backfillRequestsByCaller[1].getState().getStateStr());        
        
        logger.debug("Submitting SI request: highReq");   
        
        //Submit a higher-bid SI Request 
        String highReqId = rm.requestSpotInstances(highReq, caller3).getRequestID();
        
        // Spot Instances Snapshot
        // 
        // Total memory: 1280MB
        // Reserved free memory (for ordinary WS requests): 256MB (minimum reserved capacity)
        // Available basic SI VMs: 8 (128MB x 8 = 1024MB)         
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
        // Backfill Requests__________________________
        // |   Request   | reqVMs | allocVMs | Status |
        // |  backfill1  |   3    |    0     | OPEN   |
        // |  backfill2  |   5    |    0     | OPEN   |      
        // --------------------------------------------        
        // Requested SI VMs (alive requests): 18
        // Spot price: highBid (previousPrice+3)              
        
        Calendar ts5 = Calendar.getInstance();        
        
        logger.debug("Waiting 2 seconds for resources to be pre-empted.");
        Thread.sleep(2000);        
        
        //New spot price is equal to high bid
        assertEquals(highBid,  rm.getSpotPrice());
        
        //Check if submitted request is active
        assertEquals(RequestState.STATE_Active, rm.getSpotRequest(highReqId, caller3).getState().getStateStr());
        
        //Check if persistent requests are open
        assertEquals(RequestState.STATE_Open, rm.getSpotRequest(lowReq2Id, caller1).getState().getStateStr());
        assertEquals(RequestState.STATE_Open, rm.getSpotRequest(medReq2Id, caller2).getState().getStateStr());
        
        //Check if non-persistent requests are closed
        assertEquals(RequestState.STATE_Closed, rm.getSpotRequest(lowReq1Id, caller1).getState().getStateStr());
        assertEquals(RequestState.STATE_Closed, rm.getSpotRequest(lowReq3Id, caller2).getState().getStateStr());
        assertEquals(RequestState.STATE_Closed, rm.getSpotRequest(medReq1Id, caller2).getState().getStateStr());
        
        //Check backfill request state
        backfillRequestsByCaller = rm.getBackfillRequestsByCaller(superuser);
        assertEquals(2, backfillRequestsByCaller.length);
        assertEquals(0, backfillRequestsByCaller[0].getVMIds().length);
        assertEquals(RequestState.STATE_Open, backfillRequestsByCaller[0].getState().getStateStr());
        assertEquals(0, backfillRequestsByCaller[1].getVMIds().length);
        assertEquals(RequestState.STATE_Open, backfillRequestsByCaller[1].getState().getStateStr());
        
        rm.cancelSpotInstanceRequests(new String[]{lowReq2Id}, caller1);
        rm.cancelSpotInstanceRequests(new String[]{highReqId}, caller3);
        
        // Spot Instances Snapshot
        // 
        // Total memory: 1280MB
        // Reserved free memory (for ordinary WS requests): 256MB (minimum reserved capacity)
        // Available basic SI VMs: 8 (128MB x 8 = 1024MB)         
        // 
        // Current Requests_______________________________________________________________
        // |   Request  | reqVMs | allocVMs |       Bid        | Status    | Persistent |
        // | lowReq1    |   1    |    0     |  previousPrice+1 | CLOSED    |    false   |
        // | lowReq2    |   3    |    0     |  previousPrice+1 | CANCELLED |    true    |
        // | mediumReq1 |   3    |    0     |  previousPrice+2 | CLOSED    |    false   |
        // | lowReq3    |   1    |    0     |  previousPrice+1 | CLOSED    |    false   |
        // | mediumReq2 |   5    |    5     |  previousPrice+2 | ACTIVE    |    true    |
        // | highReq    |  10    |    0     |  previousPrice+3 | CANCELLED |    false   |            
        // ---------------------------------------------------------------------------
        // Backfill Requests__________________________
        // |   Request   | reqVMs | allocVMs | Status |
        // |  backfill1  |   3    |    2     | ACTIVE |
        // |  backfill2  |   5    |    1     | ACTIVE |      
        // --------------------------------------------        
        // Requested SI VMs (alive requests): 5
        // Spot price: MINIMUM_PRICE        
        
        logger.debug("Waiting 2 seconds for resources to be pre-empted.");
        Thread.sleep(2000);          
        
        //New spot price is equal to minimum price
        assertEquals(MINIMUM_PRICE,  rm.getSpotPrice());
        
        //Check if requests were cancelled
        assertEquals(RequestState.STATE_Canceled, rm.getSpotRequest(highReqId, caller3).getState().getStateStr());
        assertEquals(RequestState.STATE_Canceled, rm.getSpotRequest(lowReq2Id, caller1).getState().getStateStr());
        
        //Check if persistent request is active
        SpotRequestInfo medReq2 = rm.getSpotRequest(medReq2Id, caller2);
        assertEquals(RequestState.STATE_Active, medReq2.getState().getStateStr());
        assertEquals(5, medReq2.getVMIds().length);
        
        //Check if non-persistent requests are closed
        assertEquals(RequestState.STATE_Closed, rm.getSpotRequest(lowReq1Id, caller1).getState().getStateStr());
        assertEquals(RequestState.STATE_Closed, rm.getSpotRequest(lowReq3Id, caller2).getState().getStateStr());
        assertEquals(RequestState.STATE_Closed, rm.getSpotRequest(medReq1Id, caller2).getState().getStateStr());
        
        //Check backfill request state
        backfillRequestsByCaller = rm.getBackfillRequestsByCaller(superuser);
        assertEquals(2, backfillRequestsByCaller.length);
        
        backfill1Result = rm.getBackfillRequest(backfill1Result.getRequestID(), superuser);
        assertEquals(2, backfill1Result.getVMIds().length);
        assertEquals(RequestState.STATE_Active, backfill1Result.getState().getStateStr());
        
        backfill2Result = rm.getBackfillRequest(backfill2Result.getRequestID(), superuser);        
        assertEquals(1, backfill2Result.getVMIds().length);
        assertEquals(RequestState.STATE_Active, backfill2Result.getState().getStateStr());        
        
        rm.cancelBackfillRequests(new String[]{backfill1Result.getRequestID()}, superuser);
        
        // Spot Instances Snapshot
        // 
        // Total memory: 1280MB
        // Reserved free memory (for ordinary WS requests): 256MB (minimum reserved capacity)
        // Available basic SI VMs: 8 (128MB x 8 = 1024MB)         
        // 
        // Current Requests_______________________________________________________________
        // |   Request  | reqVMs | allocVMs |       Bid        | Status    | Persistent |
        // | lowReq1    |   1    |    0     |  previousPrice+1 | CLOSED    |    false   |
        // | lowReq2    |   3    |    0     |  previousPrice+1 | CANCELLED |    true    |
        // | mediumReq1 |   3    |    0     |  previousPrice+2 | CLOSED    |    false   |
        // | lowReq3    |   1    |    0     |  previousPrice+1 | CLOSED    |    false   |
        // | mediumReq2 |   5    |    5     |  previousPrice+2 | ACTIVE    |    true    |
        // | highReq    |  10    |    0     |  previousPrice+3 | CANCELLED |    false   |            
        // ---------------------------------------------------------------------------
        // Backfill Requests______________________________
        // |   Request   | reqVMs | allocVMs | Status    |
        // |  backfill1  |   3    |    0     | CANCELLED |
        // |  backfill2  |   5    |    3     | ACTIVE    |      
        // -----------------------------------------------        
        // Requested SI VMs (alive requests): 5
        // Spot price: MINIMUM_PRICE        
        
        logger.debug("Waiting 2 seconds for resources to be pre-empted.");
        Thread.sleep(2000);               
        
        //Check backfill request state
        backfillRequestsByCaller = rm.getBackfillRequestsByCaller(superuser);
        assertEquals(2, backfillRequestsByCaller.length);
        
        backfill1Result = rm.getBackfillRequest(backfill1Result.getRequestID(), superuser);
        assertEquals(0, backfill1Result.getVMIds().length);
        assertEquals(RequestState.STATE_Canceled, backfill1Result.getState().getStateStr());
        
        backfill2Result = rm.getBackfillRequest(backfill2Result.getRequestID(), superuser);        
        assertEquals(3, backfill2Result.getVMIds().length);
        assertEquals(RequestState.STATE_Active, backfill2Result.getState().getStateStr());        
        
        rm.cancelSpotInstanceRequests(new String[]{medReq2Id}, caller2);
        
        // Spot Instances Snapshot
        // 
        // Total memory: 1280MB
        // Reserved free memory (for ordinary WS requests): 256MB (minimum reserved capacity)
        // Available basic SI VMs: 8 (128MB x 8 = 1024MB)         
        // 
        // Current Requests_______________________________________________________________
        // |   Request  | reqVMs | allocVMs |       Bid        | Status    | Persistent |
        // | lowReq1    |   1    |    0     |  previousPrice+1 | CLOSED    |    false   |
        // | lowReq2    |   3    |    0     |  previousPrice+1 | CANCELLED |    true    |
        // | mediumReq1 |   3    |    0     |  previousPrice+2 | CLOSED    |    false   |
        // | lowReq3    |   1    |    0     |  previousPrice+1 | CLOSED    |    false   |
        // | mediumReq2 |   5    |    0     |  previousPrice+2 | CANCELLED |    true    |
        // | highReq    |  10    |    0     |  previousPrice+3 | CANCELLED |    false   |            
        // ---------------------------------------------------------------------------
        // Backfill Requests______________________________
        // |   Request   | reqVMs | allocVMs | Status    |
        // |  backfill1  |   3    |    0     | CANCELLED |
        // |  backfill2  |   5    |    5     | ACTIVE    |      
        // -----------------------------------------------        
        // Requested SI VMs (alive requests): 0
        // Spot price: MINIMUM_PRICE        
        
        logger.debug("Waiting 2 seconds for resources to be pre-empted.");
        Thread.sleep(2000);        
        
        //Check if persistent request was cancelled
        medReq2 = rm.getSpotRequest(medReq2Id, caller2);
        assertEquals(RequestState.STATE_Canceled, medReq2.getState().getStateStr());
        assertEquals(0, medReq2.getVMIds().length);         
        
        //Check backfill request state
        backfillRequestsByCaller = rm.getBackfillRequestsByCaller(superuser);
        assertEquals(2, backfillRequestsByCaller.length);
        
        backfill1Result = rm.getBackfillRequest(backfill1Result.getRequestID(), superuser);
        assertEquals(0, backfill1Result.getVMIds().length);
        assertEquals(RequestState.STATE_Canceled, backfill1Result.getState().getStateStr());
        
        backfill2Result = rm.getBackfillRequest(backfill2Result.getRequestID(), superuser);        
        assertEquals(5, backfill2Result.getVMIds().length);
        assertEquals(RequestState.STATE_Active, backfill2Result.getState().getStateStr());         
        
        //Expected prices from full history
        
        Double[] prices = new Double[]{MINIMUM_PRICE, lowBid, mediumBid, highBid, MINIMUM_PRICE};
        
        //Check spot price history
        SpotPriceEntry[] history = rm.getSpotPriceHistory();
        
        assertEquals(prices.length, history.length);
        
        for (int i = 0; i < history.length; i++) {
            assertEquals(prices[i], history[i].getSpotPrice());
        }
        
        //Expected prices before ts2
        
        prices = new Double[]{MINIMUM_PRICE};
        
        //Check spot price history
        history = rm.getSpotPriceHistory(null, ts2);
        
        assertEquals(prices.length, history.length);
        
        for (int i = 0; i < history.length; i++) {
            assertEquals(prices[i], history[i].getSpotPrice());
        }
        
        //Expected prices from ts2 to ts4
        
        prices = new Double[]{lowBid, mediumBid};
        
        //Check spot price history
        history = rm.getSpotPriceHistory(ts2, ts4);
        
        assertEquals(prices.length, history.length);
        
        for (int i = 0; i < history.length; i++) {
            assertEquals(prices[i], history[i].getSpotPrice());
        }           
         
        //Expected prices from ts4 to ts5
        
        prices = new Double[]{highBid};
        
        //Check spot price history
        history = rm.getSpotPriceHistory(ts4, ts5);
        
        assertEquals(prices.length, history.length);
        
        for (int i = 0; i < history.length; i++) {
            assertEquals(prices[i], history[i].getSpotPrice());
        }            
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
        
        SpotCreateRequest lowReq1 = this.populator().getBasicRequestSI("lowReq2", 3, lowBid, false);
        SpotCreateRequest lowReq2 = this.populator().getBasicRequestSI("lowReq3", 2, lowBid, true);
        SpotCreateRequest mediumReq = this.populator().getBasicRequestSI("mediumReq1", 3, mediumBid, false);

        logger.debug("Submitting SI requests: lowReq1, lowReq2, mediumReq1");         
        
        //Submit 3 SI Requests
        String lowReq1Id = rm.requestSpotInstances(lowReq1, siCaller).getRequestID();
        String lowReq2Id = rm.requestSpotInstances(lowReq2, siCaller).getRequestID();
        String medReqId = rm.requestSpotInstances(mediumReq, siCaller).getRequestID();

        // Spot Instances Snapshot
        // 
        // Total memory: 1280MB
        // Used WS memory: 0MB
        // Reserved free memory (for ordinary WS requests): 256MB (minimum reserved capacity)
        // Available basic SI VMs: 8 (128MB x 8 = 1024MB)         
        //
        // Current Requests___________________________________________________________
        // |   Request  | reqVMs | allocVMs |       Bid        | Status | Persistent |
        // | lowReq1    |   3    |     3    | previousPrice+1  | ACTIVE |    false   |
        // | lowReq2    |   2    |     2    | previousPrice+1  | ACTIVE |    true    |
        // | mediumReq1 |   3    |     3    | previousPrice+3  | ACTIVE |    false   |
        // ---------------------------------------------------------------------------
        // Requested SI VMs (alive requests): 8
        // Spot price: lowBid (previousPrice+1)          
        
        //Check available SI VMs
        assertEquals(8,  getAvailableResources());
        
        //New spot price is equal to lower bid
        assertEquals(lowBid,  rm.getSpotPrice());
        
        //Check if all submitted requests are active and fulfilled
        SpotRequestInfo lowReq1SR = rm.getSpotRequest(lowReq1Id, siCaller);
        assertEquals(RequestState.STATE_Active, lowReq1SR.getState().getStateStr());
        assertEquals(3, lowReq1SR.getVMIds().length);        
        
        SpotRequestInfo lowReq2SR = rm.getSpotRequest(lowReq2Id, siCaller);
        assertEquals(RequestState.STATE_Active, lowReq2SR.getState().getStateStr());
        assertEquals(2, lowReq2SR.getVMIds().length);
        
        SpotRequestInfo medReqSR = rm.getSpotRequest(medReqId, siCaller);
        assertEquals(RequestState.STATE_Active, medReqSR.getState().getStateStr());
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
        // Available basic SI VMs: 6 (128MB x 6 = 768)         
        //
        // Current Requests___________________________________________________________
        // |   Request  | reqVMs | allocVMs |       Bid        | Status | Persistent |
        // | lowReq1    |   3    |     2    | previousPrice+1  | ACTIVE |    false   |
        // | lowReq2    |   2    |     1    | previousPrice+1  | ACTIVE |    true    |
        // | mediumReq1 |   3    |     3    | previousPrice+3  | ACTIVE |    false   |
        // ---------------------------------------------------------------------------
        // Requested SI VMs (alive requests): 7 (1 from lowReq1 was closed)
        // Spot price: lowBid (previousPrice+1)       

        logger.debug("Waiting 2 seconds for resources to be pre-empted.");
        Thread.sleep(2000);        
        
        //Check available SI VMs
        assertEquals(6,  getAvailableResources());        
        
        //New spot price is equal to lower bid
        assertEquals(lowBid,  rm.getSpotPrice());        
        
        lowReq1SR = rm.getSpotRequest(lowReq1Id, siCaller);
        assertEquals(RequestState.STATE_Active, lowReq1SR.getState().getStateStr());
        assertEquals(2, lowReq1SR.getVMIds().length);        
        
        lowReq2SR = rm.getSpotRequest(lowReq2Id, siCaller);
        assertEquals(RequestState.STATE_Active, lowReq2SR.getState().getStateStr());
        assertEquals(1, lowReq2SR.getVMIds().length);
        
        medReqSR = rm.getSpotRequest(medReqId, siCaller);
        assertEquals(RequestState.STATE_Active, medReqSR.getState().getStateStr());
        assertEquals(3, medReqSR.getVMIds().length); 
        
        logger.debug("Submitting WS request: wsReq2 (2 VMs, 110MB RAM each)");        
        
        CreateRequest wsReq2 = this.populator().getCreateRequest("wsReq2", TASK_TIME, 110, 2);
        rm.create(wsReq2, wsCaller); 
        
        // Spot Instances Snapshot
        // 
        // Total memory: 1280MB
        // Used WS memory: 460MB
        // Reserved free memory (for ordinary WS requests): 256MB (minimum reserved capacity)
        // Available basic SI VMs: 4 (128MB x 4 = 512MB)         
        //
        // Current Requests___________________________________________________________
        // |   Request  | reqVMs | allocVMs |       Bid        | Status | Persistent |
        // | lowReq1    |   3    |     1    | previousPrice+1  | ACTIVE |    false   |
        // | lowReq2    |   2    |     0    | previousPrice+1  | OPEN   |    true    |
        // | mediumReq1 |   3    |     3    | previousPrice+3  | ACTIVE |    false   |
        // ---------------------------------------------------------------------------
        // Requested SI VMs (alive requests): 6 (2 from lowReq1 were closed)
        // Spot price: lowBid (previousPrice+1)
        
        logger.debug("Waiting 2 seconds for SI VMs to be pre-empted.");
        Thread.sleep(2000);        
        
        //Check available SI VMs
        assertEquals(4,  getAvailableResources());            
        
        //New spot price is equal to lower bid
        assertEquals(lowBid,  rm.getSpotPrice());        
        
        lowReq1SR = rm.getSpotRequest(lowReq1Id, siCaller);
        assertEquals(RequestState.STATE_Active, lowReq1SR.getState().getStateStr());
        assertEquals(1, lowReq1SR.getVMIds().length);        
        
        lowReq2SR = rm.getSpotRequest(lowReq2Id, siCaller);
        assertEquals(RequestState.STATE_Open, lowReq2SR.getState().getStateStr());
        assertEquals(0, lowReq2SR.getVMIds().length);
        
        medReqSR = rm.getSpotRequest(medReqId, siCaller);
        assertEquals(RequestState.STATE_Active, medReqSR.getState().getStateStr());
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
        // Available basic SI VMs: 6 (128MB x 6 = 768MB)        
        //
        // Current Requests___________________________________________________________
        // |   Request  | reqVMs | allocVMs |       Bid        | Status | Persistent |
        // | lowReq1    |   3    |     1    | previousPrice+1  | ACTIVE |    false   |**
        // | lowReq2    |   2    |     2    | previousPrice+1  | ACTIVE |    true    |
        // | mediumReq1 |   3    |     3    | previousPrice+3  | ACTIVE |    false   |
        // ---------------------------------------------------------------------------
        // Requested SI VMs (alive requests): 6 (2 from lowReq1 were closed)
        // Spot price: lowBid (previousPrice+1)        
        //
        // ** lowReq1 is not persistent, so no more VMs are allocated for this
        //    request (since 2 VMs were already finished (pre-empted))
       
        //Check available SI VMs
        assertEquals(6,  getAvailableResources());            
        
        //Spot price is equal to lower bid
        assertEquals(lowBid,  rm.getSpotPrice());        
        
        lowReq1SR = rm.getSpotRequest(lowReq1Id, siCaller);
        assertEquals(RequestState.STATE_Active, lowReq1SR.getState().getStateStr());
        assertEquals(1, lowReq1SR.getVMIds().length);        
        
        lowReq2SR = rm.getSpotRequest(lowReq2Id, siCaller);
        assertEquals(RequestState.STATE_Active, lowReq2SR.getState().getStateStr());
        assertEquals(2, lowReq2SR.getVMIds().length);
        
        medReqSR = rm.getSpotRequest(medReqId, siCaller);
        assertEquals(RequestState.STATE_Active, medReqSR.getState().getStateStr());
        assertEquals(3, medReqSR.getVMIds().length);        
        
        logger.debug("Submitting WS request: wsReq3 (2 VMs, 256MB RAM each)");               
        
        CreateRequest wsReq3 = this.populator().getCreateRequest("wsReq3", 500, 256, 2);
        CreateResult wsReq3Result = rm.create(wsReq3, wsCaller);  

        // Spot Instances Snapshot
        // 
        // Total memory: 1280MB
        // Used WS memory: 752MB
        // Reserved capacity (for ordinary WS requests): 322MB (to ensure 70% max utilization)
        // Available basic SI VMs: 1 (128MB)         
        //
        // Current Requests___________________________________________________________
        // |   Request  | reqVMs | allocVMs |       Bid        | Status | Persistent |
        // | lowReq1    |   3    |     0    | previousPrice+1  | CLOSED |    false   |
        // | lowReq2    |   2    |     0    | previousPrice+1  | OPEN   |    true    |
        // | mediumReq1 |   3    |     1    | previousPrice+3  | ACTIVE |    false   |
        // ---------------------------------------------------------------------------
        // Requested SI VMs (alive requests): 3
        // Spot price: lowBid (previousPrice+1)
        
        logger.debug("Waiting 2 seconds for SI VMs to be pre-empted.");
        Thread.sleep(2000);        
        
        //Check available SI VMs
        assertEquals(1,  getAvailableResources());            
        
        //New spot price is equal to medium bid
        assertEquals(mediumBid,  rm.getSpotPrice());        
        
        lowReq1SR = rm.getSpotRequest(lowReq1Id, siCaller);
        assertEquals(RequestState.STATE_Closed, lowReq1SR.getState().getStateStr());
        assertEquals(0, lowReq1SR.getVMIds().length);        
        
        lowReq2SR = rm.getSpotRequest(lowReq2Id, siCaller);
        assertEquals(RequestState.STATE_Open, lowReq2SR.getState().getStateStr());
        assertEquals(0, lowReq2SR.getVMIds().length);
        
        medReqSR = rm.getSpotRequest(medReqId, siCaller);
        assertEquals(RequestState.STATE_Active, medReqSR.getState().getStateStr());
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
        // Requested SI VMs (alive requests): 2
        // Spot price: lowBid + 1
        
        logger.debug("Waiting 2 seconds for SI VMs to be pre-empted.");
        Thread.sleep(2000);        
        
        //Check available SI VMs
        assertEquals(0,  getAvailableResources());             
        
        //New spot price is equal to lower bid + 0.1 (since lower bid is the highest ALIVE bid)
        assertEquals(lowBid+0.1,  rm.getSpotPrice());       
        
        lowReq1SR = rm.getSpotRequest(lowReq1Id, siCaller);
        assertEquals(RequestState.STATE_Closed, lowReq1SR.getState().getStateStr());
        assertEquals(0, lowReq1SR.getVMIds().length);        
        
        lowReq2SR = rm.getSpotRequest(lowReq2Id, siCaller);
        assertEquals(RequestState.STATE_Open, lowReq2SR.getState().getStateStr());
        assertEquals(0, lowReq2SR.getVMIds().length);
        
        medReqSR = rm.getSpotRequest(medReqId, siCaller);
        assertEquals(RequestState.STATE_Closed, medReqSR.getState().getStateStr());
        assertEquals(0, medReqSR.getVMIds().length);   
        
        logger.debug("Destroying wsReq3 VMs: 2 VMs, 256MB RAM each");                       
        
        rm.trash(wsReq3Result.getGroupID(), Manager.GROUP, wsCaller);
        
        // Spot Instances Snapshot
        // 
        // Total memory: 1280MB
        // Used WS memory: 496MB
        // Reserved capacity (for ordinary WS requests): 256MB (minimum reserved capacity)
        // Available basic SI VMs: 4 (128MB x 4 = 512MB)
        //
        // Current Requests___________________________________________________________
        // |   Request  | reqVMs | allocVMs |       Bid        | Status | Persistent |
        // | lowReq1    |   3    |     0    | previousPrice+1  | CLOSED |    false   |
        // | lowReq2    |   2    |     2    | previousPrice+1  | ACTIVE |    true    |
        // | mediumReq1 |   3    |     0    | previousPrice+3  | CLOSED |    false   |
        // ---------------------------------------------------------------------------
        // Requested SI VMs (alive requests): 2
        // Spot price: MINIMUM_PRICE (since requestedVMs < availableVMs)           
        
        logger.debug("Waiting 2 seconds for resources to be allocated.");
        Thread.sleep(2000);
        
        //Check available SI VMs
        assertEquals(4,  getAvailableResources());             
        
        //New spot price is equal to minimum price
        assertEquals(MINIMUM_PRICE,  rm.getSpotPrice());
        
        lowReq1SR = rm.getSpotRequest(lowReq1Id, siCaller);
        assertEquals(RequestState.STATE_Closed, lowReq1SR.getState().getStateStr());
        assertEquals(0, lowReq1SR.getVMIds().length);        
        
        lowReq2SR = rm.getSpotRequest(lowReq2Id, siCaller);
        assertEquals(RequestState.STATE_Active, lowReq2SR.getState().getStateStr());
        assertEquals(2, lowReq2SR.getVMIds().length);
        
        medReqSR = rm.getSpotRequest(medReqId, siCaller);
        assertEquals(RequestState.STATE_Closed, medReqSR.getState().getStateStr());
        assertEquals(0, medReqSR.getVMIds().length);           
        
        logger.debug("Submitting basic SI request request: mediumReq2 (1 VM)");                 
        
        SpotCreateRequest mediumReq2 = this.populator().getBasicRequestSI("mediumReq2", 1, mediumBid, false);
        String medReq2Id = rm.requestSpotInstances(mediumReq2, siCaller).getRequestID();
        
        // Spot Instances Snapshot
        // 
        // Total memory: 1280MB
        // Used WS memory: 496MB
        // Reserved capacity (for ordinary WS requests): 256MB (minimum reserved capacity)
        // Available basic SI VMs: 4 (128MB x 4 = 512MB)
        //
        // Current Requests___________________________________________________________
        // |   Request  | reqVMs | allocVMs |       Bid        | Status | Persistent |
        // | lowReq1    |   3    |     0    | previousPrice+1  | CLOSED |    false   |
        // | lowReq2    |   2    |     2    | previousPrice+1  | ACTIVE |    true    |
        // | mediumReq1 |   3    |     0    | previousPrice+3  | CLOSED |    false   |
        // | mediumReq2 |   1    |     1    | previousPrice+3  | ACTIVE |    false   |        
        // ---------------------------------------------------------------------------
        // Requested SI VMs (alive requests): 3
        // Spot price: MINIMUM_PRICE (since requestedVMs < availableVMs)           
        
        logger.debug("Waiting 2 seconds for resources to be allocated.");
        Thread.sleep(2000);        
        
        //Check available SI VMs
        assertEquals(4,  getAvailableResources());             
        
        //New spot price is equal to minimum price
        assertEquals(MINIMUM_PRICE,  rm.getSpotPrice());
        
        lowReq1SR = rm.getSpotRequest(lowReq1Id, siCaller);
        assertEquals(RequestState.STATE_Closed, lowReq1SR.getState().getStateStr());
        assertEquals(0, lowReq1SR.getVMIds().length);        
        
        lowReq2SR = rm.getSpotRequest(lowReq2Id, siCaller);
        assertEquals(RequestState.STATE_Active, lowReq2SR.getState().getStateStr());
        assertEquals(2, lowReq2SR.getVMIds().length);
        
        medReqSR = rm.getSpotRequest(medReqId, siCaller);
        assertEquals(RequestState.STATE_Closed, medReqSR.getState().getStateStr());
        assertEquals(0, medReqSR.getVMIds().length);     
        
        SpotRequestInfo medReq2SR = rm.getSpotRequest(medReq2Id, siCaller);
        assertEquals(RequestState.STATE_Active, medReq2SR.getState().getStateStr());
        assertEquals(1, medReq2SR.getVMIds().length);
        
        logger.debug("Shutting down VM from mediumReq2");            
        
        rm.trash(medReq2SR.getVMIds()[0], Manager.INSTANCE, siCaller);
        
        // Spot Instances Snapshot
        // 
        // Total memory: 1280MB
        // Used WS memory: 496MB
        // Reserved capacity (for ordinary WS requests): 256MB (minimum reserved capacity)
        // Available basic SI VMs: 4 (128MB x 4 = 512MB)
        //
        // Current Requests___________________________________________________________
        // |   Request  | reqVMs | allocVMs |       Bid        | Status | Persistent |
        // | lowReq1    |   3    |     0    | previousPrice+1  | CLOSED |    false   |
        // | lowReq2    |   2    |     2    | previousPrice+1  | ACTIVE |    true    |
        // | mediumReq1 |   3    |     0    | previousPrice+3  | CLOSED |    false   |
        // | mediumReq2 |   1    |     0    | previousPrice+3  | CLOSED |    false   |        
        // ---------------------------------------------------------------------------
        // Requested SI VMs (alive requests): 2
        // Spot price: MINIMUM_PRICE (since requestedVMs < availableVMs)           
        
        logger.debug("Waiting 2 seconds for VM to shutdown.");
        Thread.sleep(2000);        
        
        //Check available SI VMs
        assertEquals(4,  getAvailableResources());             
        
        //New spot price is equal to minimum price
        assertEquals(MINIMUM_PRICE,  rm.getSpotPrice());
        
        lowReq1SR = rm.getSpotRequest(lowReq1Id, siCaller);
        assertEquals(RequestState.STATE_Closed, lowReq1SR.getState().getStateStr());
        assertEquals(0, lowReq1SR.getVMIds().length);        
        
        lowReq2SR = rm.getSpotRequest(lowReq2Id, siCaller);
        assertEquals(RequestState.STATE_Active, lowReq2SR.getState().getStateStr());
        assertEquals(2, lowReq2SR.getVMIds().length);
        
        medReqSR = rm.getSpotRequest(medReqId, siCaller);
        assertEquals(RequestState.STATE_Closed, medReqSR.getState().getStateStr());
        assertEquals(0, medReqSR.getVMIds().length);     
        
        medReq2SR = rm.getSpotRequest(medReq2Id, siCaller);
        assertEquals(RequestState.STATE_Closed, medReq2SR.getState().getStateStr());
        assertEquals(0, medReq2SR.getVMIds().length);
        
        logger.debug("Submitting basic SI request request: mediumReq2 (1 VM)");                 
        
        Double highBid = previousPrice + 4;
        SpotCreateRequest highReq1 = this.populator().getBasicRequestSI("highBid1", 4, highBid, false);
        String highReq1Id = rm.requestSpotInstances(highReq1, siCaller).getRequestID();
        
        // Spot Instances Snapshot
        // 
        // Total memory: 1280MB
        // Used WS memory: 496MB
        // Reserved capacity (for ordinary WS requests): 256MB (minimum reserved capacity)
        // Available basic SI VMs: 4 (128MB x 4 = 512MB)
        //
        // Current Requests___________________________________________________________
        // |   Request  | reqVMs | allocVMs |       Bid        | Status | Persistent |
        // | lowReq1    |   3    |     0    | previousPrice+1  | CLOSED |    false   |
        // | lowReq2    |   2    |     0    | previousPrice+1  | OPEN   |    true    |
        // | mediumReq1 |   3    |     0    | previousPrice+3  | CLOSED |    false   |
        // | mediumReq2 |   1    |     0    | previousPrice+3  | CLOSED |    false   |     
        // | highReq1   |   4    |     4    | previousPrice+4  | ACTIVE |    false   |           
        // ---------------------------------------------------------------------------
        // Requested SI VMs (alive requests): 4
        // Spot price: MINIMUM_PRICE (since requestedVMs < availableVMs)           
        
        logger.debug("Waiting 2 seconds for resources to be allocated.");
        Thread.sleep(2000);        
        
        //Check available SI VMs
        assertEquals(4,  getAvailableResources());             
        
        //New spot price is equal to high bid
        assertEquals(highBid,  rm.getSpotPrice());
        
        lowReq1SR = rm.getSpotRequest(lowReq1Id, siCaller);
        assertEquals(RequestState.STATE_Closed, lowReq1SR.getState().getStateStr());
        assertEquals(0, lowReq1SR.getVMIds().length);        
        
        lowReq2SR = rm.getSpotRequest(lowReq2Id, siCaller);
        assertEquals(RequestState.STATE_Open, lowReq2SR.getState().getStateStr());
        assertEquals(0, lowReq2SR.getVMIds().length);
        
        medReqSR = rm.getSpotRequest(medReqId, siCaller);
        assertEquals(RequestState.STATE_Closed, medReqSR.getState().getStateStr());
        assertEquals(0, medReqSR.getVMIds().length);     
        
        medReq2SR = rm.getSpotRequest(medReq2Id, siCaller);
        assertEquals(RequestState.STATE_Closed, medReq2SR.getState().getStateStr());
        assertEquals(0, medReq2SR.getVMIds().length);        
        
        SpotRequestInfo highReqSR = rm.getSpotRequest(highReq1Id, siCaller);
        assertEquals(RequestState.STATE_Active, highReqSR.getState().getStateStr());
        assertEquals(4, highReqSR.getVMIds().length);        
        
        logger.debug("Cancelling request: highReq1");            
        
        rm.cancelSpotInstanceRequests(new String[]{highReq1Id}, siCaller);
        
        // Spot Instances Snapshot
        // 
        // Total memory: 1280MB
        // Used WS memory: 496MB
        // Reserved capacity (for ordinary WS requests): 256MB (minimum reserved capacity)
        // Available basic SI VMs: 4 (128MB x 4 = 512MB)
        //
        // Current Requests______________________________________________________________
        // |   Request  | reqVMs | allocVMs |       Bid        | Status    | Persistent |
        // | lowReq1    |   3    |     0    | previousPrice+1  | CLOSED    |    false   |
        // | lowReq2    |   2    |     2    | previousPrice+1  | ACTIVE    |    true    |
        // | mediumReq1 |   3    |     0    | previousPrice+3  | CLOSED    |    false   |
        // | mediumReq2 |   1    |     0    | previousPrice+3  | CLOSED    |    false   | 
        // | highReq1   |   4    |     4    | previousPrice+4  | CANCELLED |    false   |                   
        // ------------------------------------------------------------------------------
        // Requested SI VMs (alive requests): 2
        // Spot price: MINIMUM_PRICE (since requestedVMs < availableVMs)           
        
        logger.debug("Waiting 2 seconds for VM to shutdown.");
        Thread.sleep(2000);
        
        //Check available SI VMs
        assertEquals(4,  getAvailableResources());             
        
        //New spot price is equal to minimum price
        assertEquals(MINIMUM_PRICE,  rm.getSpotPrice());
        
        lowReq1SR = rm.getSpotRequest(lowReq1Id, siCaller);
        assertEquals(RequestState.STATE_Closed, lowReq1SR.getState().getStateStr());
        assertEquals(0, lowReq1SR.getVMIds().length);        
        
        lowReq2SR = rm.getSpotRequest(lowReq2Id, siCaller);
        assertEquals(RequestState.STATE_Active, lowReq2SR.getState().getStateStr());
        assertEquals(2, lowReq2SR.getVMIds().length);
        
        medReqSR = rm.getSpotRequest(medReqId, siCaller);
        assertEquals(RequestState.STATE_Closed, medReqSR.getState().getStateStr());
        assertEquals(0, medReqSR.getVMIds().length);     
        
        medReq2SR = rm.getSpotRequest(medReq2Id, siCaller);
        assertEquals(RequestState.STATE_Closed, medReq2SR.getState().getStateStr());
        assertEquals(0, medReq2SR.getVMIds().length);        
        
        highReqSR = rm.getSpotRequest(highReq1Id, siCaller);
        assertEquals(RequestState.STATE_Canceled, highReqSR.getState().getStateStr());
        assertEquals(0, highReqSR.getVMIds().length);           
        
        logger.debug("Destroying remaining WS VMs.");     
        
        rm.trash(wsReq1Result.getGroupID(), Manager.GROUP, wsCaller);
        rm.trash(wsReq4Result.getVMs()[0].getID(), Manager.INSTANCE, wsCaller);
    }    
    
    public int getAvailableResources(){
        AsyncRequestManagerImpl siManager = (AsyncRequestManagerImpl) applicationContext.getBean(SPOTINSTANCES_MANAGER_BEAN_NAME);
        return siManager.getMaxVMs();
    }
    
}
