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
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.RequestSI;
import org.nimbustools.api.repr.SpotRequest;
import org.nimbustools.api.repr.si.SIRequestState;
import org.nimbustools.api.services.rm.Manager;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class NoResourcesSISuite extends NimbusTestBase {

    // -----------------------------------------------------------------------------------------
    // extends NimbusTestBase
    // -----------------------------------------------------------------------------------------

    @BeforeTest
    public void setup() throws Exception {
        super.suiteSetup();
    }

    @AfterTest(alwaysRun=true)
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
        return this.determineSuitesPath() + "/basic/home";
    }
    
    /**
     * Request a single spot instance from a single caller
     * @throws Exception problem
     */
    @Test
    public void simpleRequest() throws Exception {
        logger.debug("simpleRequest");

        Caller caller = this.populator().getCaller();
        Manager rm = this.locator.getManager();
        
        Double previousPrice = rm.getSpotPrice();
        
        final Double bid = previousPrice + 1;
        RequestSI requestSI = this.populator().getBasicRequestSI("suite:spotinstances:simpleRequest", 1, bid, false);
        
        SpotRequest result = rm.requestSpotInstances(requestSI, caller);

        //Check result
        assertEquals(SIRequestState.STATE_Open, result.getState().getState());
        assertEquals(bid, result.getSpotPrice());
        assertTrue(!result.isPersistent());
        
        //New spot price is equal largest bid + 0.1 (since there are 0 resources)
        assertEquals(bid + 0.1,  rm.getSpotPrice());
        
        SpotRequest[] spotRequestByCaller = rm.getSpotRequestByCaller(caller);
        assertEquals(1, spotRequestByCaller.length);
        assertEquals(result, spotRequestByCaller[0]);
        
        assertEquals(result, rm.getSpotRequest(result.getRequestID(), caller));
    }    
    
    /**
     * Request multiple spot instances from multiple callers
     * @throws Exception problem
     */
    @Test
    public void multipleRequests() throws Exception {
        logger.debug("multipleRequests");
        
        Caller caller1 = this.populator().getCaller("CALLER1");
        Caller caller2 = this.populator().getCaller("CALLER2");
        Manager rm = this.locator.getManager();
        
        Double previousPrice = rm.getSpotPrice();
        
        final Double bid1 = previousPrice + 3;
        final Double bid2 = previousPrice + 1;
        final Double bid3 = previousPrice + 1;
        RequestSI req1 = this.populator().getBasicRequestSI("req1", 1, bid1, false);
        RequestSI req2 = this.populator().getBasicRequestSI("req1", 3, bid2, false);
        RequestSI req3 = this.populator().getBasicRequestSI("req1", 4, bid3, false);
        
        SpotRequest result1 = rm.requestSpotInstances(req1, caller1);
        SpotRequest result2 = rm.requestSpotInstances(req2, caller2);
        SpotRequest result3 = rm.requestSpotInstances(req3, caller1);

        assertEquals(result1, rm.getSpotRequest(result1.getRequestID(), caller1));
        assertEquals(result2, rm.getSpotRequest(result2.getRequestID(), caller2));
        assertEquals(result3, rm.getSpotRequest(result3.getRequestID(), caller1));
        
        //New spot price is equal largest bid + 0.1 (since there are 0 resources)
        assertEquals(bid1 + 0.1,  rm.getSpotPrice());
        
        SpotRequest[] spotRequestByCaller1 = rm.getSpotRequestByCaller(caller1);
        assertEquals(2, spotRequestByCaller1.length);
        
        SpotRequest[] spotRequestByCaller2 = rm.getSpotRequestByCaller(caller2);
        assertEquals(1, spotRequestByCaller2.length);        
        
    }
}
