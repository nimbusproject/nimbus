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

import com.google.gson.Gson;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.workspace.remoting.admin.VmmNode;
import org.globus.workspace.testing.NimbusTestBase;
import org.globus.workspace.testing.NimbusTestContextLoader;
import org.nimbustools.api.repr.AsyncCreateRequest;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.CreateRequest;
import org.nimbustools.api.repr.RequestInfo;
import org.nimbustools.api.services.admin.RemoteNodeManagement;
import org.nimbustools.api.services.rm.Manager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;

@Listeners({ org.globus.workspace.testing.suites.spotinstances.TestListener.class })
@ContextConfiguration(locations={"file:./service/service/java/tests/suites/spotinstances/" +
                                 "home/services/etc/nimbus/workspace-service/other/main.xml"},
                      loader= NimbusTestContextLoader.class)
public class SimpleSISuite extends NimbusTestBase {

    // -----------------------------------------------------------------------------------------
    // STATIC VARIABLES
    // -----------------------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(SimpleSISuite.class.getName());
    
    
    // -----------------------------------------------------------------------------------------
    // extends NimbusTestBase
    // -----------------------------------------------------------------------------------------

    /**
     * This is how coordinate your Java test suite code with the conf files to use.
     * @return absolute path to the value that should be set for $NIMBUS_HOME
     * @throws Exception if $NIMBUS_HOME cannot be determined
     */
    protected String getNimbusHome() throws Exception {
        return this.determineSuitesPath() + "/spotinstances/home";
    }

    @AfterSuite(alwaysRun=true)
    public void suiteTeardown() throws Exception {
        super.suiteTeardown();
    }

    protected void setUpVmms() throws RemoteException {
        logger.info("Before test method: overriden setUpVmms(), unique instances");
        
        boolean active = true;
        String nodePool = "default";
        String net = "*";
        boolean vacant = true;
        Gson gson = new Gson();
        List<VmmNode> nodes = new ArrayList<VmmNode>(4);
        nodes.add(new VmmNode("fakehost1", active, nodePool, 512, net, vacant));
        nodes.add(new VmmNode("fakehost2", active, nodePool, 512, net, vacant));
        nodes.add(new VmmNode("fakehost3", active, nodePool, 256, net, vacant));
        final String nodesJson = gson.toJson(nodes);
        RemoteNodeManagement rnm = this.locator.getNodeManagement();
        rnm.addNodes(nodesJson);
    }


    // -----------------------------------------------------------------------------------------
    // TESTS
    // -----------------------------------------------------------------------------------------

    @Test
    @DirtiesContext
    public void simpleBackfillTest() throws Exception {
        Manager rm = this.locator.getManager();
        Caller superuser = this.populator().getSuperuserCaller();

        logger.info(rm.getVMMReport());

        logger.debug("Submitting backfill requests..");

        AsyncCreateRequest backfill1 = this.populator().getBackfillRequest("backfill1", 3);
        RequestInfo backfill1Result = rm.addBackfillRequest(backfill1, superuser);
        AsyncCreateRequest backfill2 = this.populator().getBackfillRequest("backfill2", 5);
        RequestInfo backfill2Result = rm.addBackfillRequest(backfill2, superuser);

        logger.debug("Waiting 2 seconds for resources to be allocated.");
        Thread.sleep(2000);

        // Check backfill request state
        RequestInfo[] backfillRequestsByCaller = rm.getBackfillRequestsByCaller(superuser);
        assertEquals(2, backfillRequestsByCaller.length);

        logger.info(rm.getVMMReport());
    }

    @Test
    @DirtiesContext
    public void simpleBackfillPreemptionTest() throws Exception {
        Manager rm = this.locator.getManager();
        Caller superuser = this.populator().getSuperuserCaller();

        logger.debug("Submitting backfill requests..");

        AsyncCreateRequest backfill1 = this.populator().getBackfillRequest("backfill1", 3);
        RequestInfo backfill1Result = rm.addBackfillRequest(backfill1, superuser);
        AsyncCreateRequest backfill2 = this.populator().getBackfillRequest("backfill2", 5);
        RequestInfo backfill2Result = rm.addBackfillRequest(backfill2, superuser);

        logger.debug("Waiting 2 seconds for resources to be allocated.");
        Thread.sleep(2000);

        // Check backfill request state
        RequestInfo[] backfillRequestsByCaller = rm.getBackfillRequestsByCaller(superuser);
        assertEquals(2, backfillRequestsByCaller.length);

        logger.info(rm.getVMMReport());

        // Three regular VMs should preempt
        Caller caller = this.populator().getCaller();
        CreateRequest req = this.populator().getCreateRequest("regular", 1200, 256 , 3);
        rm.create(req, caller);

        logger.debug("Waiting 2 seconds for resources to be allocated.");
        Thread.sleep(2000);

        logger.info(rm.getVMMReport());
    }
}
