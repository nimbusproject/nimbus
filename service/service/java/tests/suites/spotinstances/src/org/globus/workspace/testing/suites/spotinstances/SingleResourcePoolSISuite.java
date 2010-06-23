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
import org.globus.workspace.testing.NimbusTestBase;
import org.globus.workspace.testing.NimbusTestContextLoader;
import org.nimbustools.api.repr.Caller;
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
     * Request a single spot instance from a single caller
     * @throws Exception problem
     */
    @Test
    @DirtiesContext
    public void singleRequest() throws Exception {
        logger.debug("singleRequest");

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
        
        //New spot price is equal to minimum price (since there are still available resources)
        assertEquals(PricingModelConstants.MINIMUM_PRICE,  rm.getSpotPrice());
        
        SpotRequest[] spotRequestByCaller = rm.getSpotRequestByCaller(caller);
        assertEquals(1, spotRequestByCaller.length);
        assertEquals(result, spotRequestByCaller[0]);
        
        //Let's assume the request was already scheduled, so the state should be ACTIVE
        SpotRequest request = rm.getSpotRequest(result.getRequestID(), caller);
        assertEquals(SIRequestState.STATE_Active, request.getState().getState());
        
        //Cancel request
        SpotRequest[] cancelledReqs = rm.cancelSpotInstanceRequests(new String[]{result.getRequestID()}, caller);
        assertEquals(1, cancelledReqs.length);
        assertEquals(SIRequestState.STATE_Cancelled, cancelledReqs[0].getState().getState());
        assertEquals(result.getRequestID(), cancelledReqs[0].getRequestID());
        
        //Check if request was really cancelled
        request = rm.getSpotRequest(result.getRequestID(), caller);
        assertEquals(SIRequestState.STATE_Cancelled, request.getState().getState());           
    } 
    
    /**
     * Requests multiple spot instances from multiple callers
     * @throws Exception problem
     */
    @Test
    @DirtiesContext
    public void multipleRequestsSIOnly() throws Exception {
        logger.debug("multipleRequestsSIOnly");

        Manager rm = this.locator.getManager();
        
        Caller caller1 = this.populator().getCaller();
        Caller caller2 = this.populator().getCaller();
        Caller caller3 = this.populator().getCaller();
        
        Double previousPrice = rm.getSpotPrice();
        
        final Double lowBid = previousPrice + 1;
        final Double mediumBid = previousPrice + 2;
        final Double highBid = previousPrice + 3;
        
        RequestSI lowReq1 = this.populator().getBasicRequestSI("lowReq1", 1, lowBid, true);
        RequestSI lowReq2 = this.populator().getBasicRequestSI("lowReq2", 3, lowBid, false);
        RequestSI lowReq3 = this.populator().getBasicRequestSI("lowReq3", 1, lowBid, false);
        RequestSI mediumReq1 = this.populator().getBasicRequestSI("mediumReq1", 3, mediumBid, false);
        RequestSI mediumReq2 = this.populator().getBasicRequestSI("mediumReq2", 5, mediumBid, false);
        RequestSI highReq = this.populator().getBasicRequestSI("highReq", 8, highBid, false);
        
        //Submit 3 SI Requests
        SpotRequest lowReq1Result = rm.requestSpotInstances(lowReq1, caller1);
        SpotRequest lowReq2Result = rm.requestSpotInstances(lowReq2, caller2);
        SpotRequest medReq1Result = rm.requestSpotInstances(mediumReq1, caller2);

        //New spot price is equal to minimum price (since just 7 out of 8 resources are being consumed)
        assertEquals(PricingModelConstants.MINIMUM_PRICE,  rm.getSpotPrice());
        
        //Check if submitted requests are active
        SpotRequest[] caller1Reqs = rm.getSpotRequestByCaller(caller1);
        assertEquals(SIRequestState.STATE_Active, caller1Reqs[0].getState().getState());
        SpotRequest[] caller2Reqs = rm.getSpotRequestByCaller(caller2);
        for (SpotRequest caller2Req : caller2Reqs) {
            assertEquals(SIRequestState.STATE_Active, caller2Req.getState().getState());
        }
        
        //Submit another SI Request 
        SpotRequest lowReq3Result = rm.requestSpotInstances(lowReq3, caller2);
        
        //New spot price is equal to lower bid (since all resources are being consumed)
        assertEquals(lowBid,  rm.getSpotPrice());
        
        //Check if submitted request is active
        assertEquals(SIRequestState.STATE_Active, rm.getSpotRequest(lowReq3Result.getRequestID(), caller2).getState().getState());
                 
    }     
}
