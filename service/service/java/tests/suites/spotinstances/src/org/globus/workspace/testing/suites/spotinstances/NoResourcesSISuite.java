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

import org.globus.workspace.testing.NimbusTestBase;
import org.globus.workspace.testing.NimbusTestContextLoader;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.SpotCreateRequest;
import org.nimbustools.api.repr.SpotRequestInfo;
import org.nimbustools.api.repr.si.RequestState;
import org.nimbustools.api.services.rm.Manager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.rmi.RemoteException;

@Listeners({ org.globus.workspace.testing.suites.spotinstances.TestListener.class })
@ContextConfiguration(
        locations={"file:./service/service/java/tests/suites/spotinstances/" +
        		"home/services/etc/nimbus/workspace-service/other/main.xml"},
        loader=NimbusTestContextLoader.class)
public class NoResourcesSISuite extends NimbusTestBase {

    // -----------------------------------------------------------------------------------------
    // extends NimbusTestBase
    // -----------------------------------------------------------------------------------------

    @AfterSuite(alwaysRun=true)
    public void suiteTeardown() throws Exception {
        super.suiteTeardown();
    }

    protected void setUpVmms() throws RemoteException {
        logger.info("Before test method: overriden setUpVmms(), do nothing");
    }    

    /**
     * This is how coordinate your Java test suite code with the conf files to use.
     * @return absolute path to the value that should be set for $NIMBUS_HOME
     * @throws Exception if $NIMBUS_HOME cannot be determined
     */
    protected String getNimbusHome() throws Exception {
        return this.determineSuitesPath() + "/spotinstances/home";
    }
    
    /**
     * Request a single spot instance from a single caller
     *      * Check if spot price is higher than highest bid 
     *      * Since there are no available resources, the
     *        request is not allocated
     * Cancel the request
     * 
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
        SpotCreateRequest requestSI = this.populator().getBasicRequestSI("suite:spotinstances:noresources:singleRequest", 1, bid, false);
        
        SpotRequestInfo result = rm.requestSpotInstances(requestSI, caller);

        //Check result
        assertEquals(RequestState.STATE_Open, result.getState().getStateStr());
        assertEquals(bid, result.getSpotPrice());
        assertTrue(!result.isPersistent());
        
        //New spot price is equal largest bid + 0.1 (since there are 0 resources)
        assertEquals(bid + 0.1,  rm.getSpotPrice());
        
        SpotRequestInfo[] spotRequestByCaller = rm.getSpotRequestsByCaller(caller);
        assertEquals(1, spotRequestByCaller.length);
        assertEquals(result, spotRequestByCaller[0]);
        
        assertEquals(result, rm.getSpotRequest(result.getRequestID(), caller));
        
        //Cancel request
        SpotRequestInfo[] cancelledReqs = rm.cancelSpotInstanceRequests(new String[]{result.getRequestID()}, caller);
        assertEquals(1, cancelledReqs.length);
        assertEquals(RequestState.STATE_Canceled, cancelledReqs[0].getState().getStateStr());
        assertEquals(result.getRequestID(), cancelledReqs[0].getRequestID());
        
        //Check if request was really cancelled
        SpotRequestInfo request = rm.getSpotRequest(result.getRequestID(), caller);
        assertEquals(RequestState.STATE_Canceled, request.getState().getStateStr());           
    }    
    
    /**
     * Request multiple spot instances from multiple callers
     *      * Check if spot price is higher than highest bid 
     *      * Since there are no available resources, the
     *        requests are not allocated
     * Cancel the request      
     *      
     * @throws Exception problem
     */
    @Test
    @DirtiesContext
    public void multipleRequests() throws Exception {
        logger.debug("multipleRequests");
        
        Caller caller1 = this.populator().getCaller("CALLER1");
        Caller caller2 = this.populator().getCaller("CALLER2");
        Manager rm = this.locator.getManager();
        
        Double previousPrice = rm.getSpotPrice();
        
        final Double bid1 = previousPrice + 3;
        final Double bid2 = previousPrice + 1;
        final Double bid3 = previousPrice + 1;
        SpotCreateRequest req1 = this.populator().getBasicRequestSI("req1", 1, bid1, false);
        SpotCreateRequest req2 = this.populator().getBasicRequestSI("req1", 3, bid2, false);
        SpotCreateRequest req3 = this.populator().getBasicRequestSI("req1", 4, bid3, false);
        
        SpotRequestInfo result1 = rm.requestSpotInstances(req1, caller1);
        SpotRequestInfo result2 = rm.requestSpotInstances(req2, caller2);
        SpotRequestInfo result3 = rm.requestSpotInstances(req3, caller1);

        assertEquals(result1, rm.getSpotRequest(result1.getRequestID(), caller1));
        assertEquals(result2, rm.getSpotRequest(result2.getRequestID(), caller2));
        assertEquals(result3, rm.getSpotRequest(result3.getRequestID(), caller1));
        
        //New spot price is equal largest bid + 0.1 (since there are 0 resources)
        assertEquals(bid1 + 0.1,  rm.getSpotPrice());
        
        SpotRequestInfo[] spotRequestByCaller1 = rm.getSpotRequestsByCaller(caller1);
        assertEquals(2, spotRequestByCaller1.length);
        
        SpotRequestInfo[] spotRequestByCaller2 = rm.getSpotRequestsByCaller(caller2);
        assertEquals(1, spotRequestByCaller2.length);        
        
        //Cancel requests
        rm.cancelSpotInstanceRequests(new String[]{result1.getRequestID(), result3.getRequestID()}, caller1);
        SpotRequestInfo[] cancelledReqs = rm.cancelSpotInstanceRequests(new String[]{result2.getRequestID()}, caller2);
        
        //Check if requests were really cancelled
        assertEquals(RequestState.STATE_Canceled, cancelledReqs[0].getState().getStateStr());
        spotRequestByCaller1 = rm.getSpotRequestsByCaller(caller1);
        assertEquals(RequestState.STATE_Canceled, spotRequestByCaller1[0].getState().getStateStr());
        assertEquals(RequestState.STATE_Canceled, spotRequestByCaller1[1].getState().getStateStr());
    }
}
