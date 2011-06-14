/*
 * Copyright 1999-2011 University of Chicago
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
import org.globus.workspace.xen.xenssh.MockShutdownTrash;
import org.nimbustools.api.repr.AsyncCreateRequest;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.CreateRequest;
import org.nimbustools.api.repr.CreateResult;
import org.nimbustools.api.repr.RequestInfo;
import org.nimbustools.api.repr.vm.VM;
import org.nimbustools.api.services.admin.RemoteNodeManagement;
import org.nimbustools.api.services.rm.Manager;
import org.nimbustools.api.services.rm.NotEnoughMemoryException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

@Listeners({ org.globus.workspace.testing.suites.spotinstances.TestListener.class })
@ContextConfiguration(locations={"file:./service/service/java/tests/suites/spotinstances/" +
                                 "home/services/etc/nimbus/workspace-service/other/main.xml"},
                      loader= NimbusTestContextLoader.class)
public class BackfillTerminationSuite extends NimbusTestBase {

    // -----------------------------------------------------------------------------------------
    // STATIC VARIABLES
    // -----------------------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(BackfillTerminationSuite.class.getName());


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
        logger.info("Before test method: overriden setUpVmms()");
        Gson gson = new Gson();
        List<VmmNode> nodes = new ArrayList<VmmNode>(1);
        nodes.add(new VmmNode("fakehost1", true, "default", 512, "*", true));
        final String nodesJson = gson.toJson(nodes);
        RemoteNodeManagement rnm = this.locator.getNodeManagement();
        rnm.addNodes(nodesJson);
    }

    // -----------------------------------------------------------------------------------------
    // TESTS
    // -----------------------------------------------------------------------------------------

    @Test
    @DirtiesContext
    public void backfillWontDie() throws Exception {
        Manager rm = this.locator.getManager();
        Caller superuser = this.populator().getSuperuserCaller();

        logger.info(rm.getVMMReport());

        logger.debug("Submitting backfill request");

        AsyncCreateRequest backfill1 = this.populator().getBackfillRequest("backfill1", 1);
        RequestInfo backfill1Result = rm.addBackfillRequest(backfill1, superuser);

        logger.debug("Waiting 2 seconds for resources to be allocated.");
        Thread.sleep(2000);

        // Check backfill request state
        RequestInfo[] backfillRequestsByCaller = rm.getBackfillRequestsByCaller(superuser);
        assertEquals(1, backfillRequestsByCaller.length);

        logger.info(rm.getVMMReport());

        // Set the shutdown task to not work
        MockShutdownTrash.resetFailCount();
        MockShutdownTrash.setFail(true);
        logger.warn("Set to fail.");

        // One regular VM that needs all the 512 RAM will preempt
        Caller caller = this.populator().getCaller();
        CreateRequest req = this.populator().getCreateRequest("regular", 1200, 512 , 1);

        final long startMs = System.currentTimeMillis();

        boolean notEnoughMemory = false;
        try {
            rm.create(req, caller);
        } catch (NotEnoughMemoryException e) {
            notEnoughMemory = true;
        }

        final long endMs = System.currentTimeMillis();
        final long totalSeconds = (endMs - startMs) / 1000;
        logger.info("Total seconds: " + totalSeconds);

        // That should have waited up to ~20 seconds before giving up on the incoming request
        assertTrue(totalSeconds > 18);

        // backfill wouldn't die, and the service correctly denies the incoming request
        assertTrue(notEnoughMemory);
    }


        @Test
    @DirtiesContext
    public void backfillEventuallyDies() throws Exception {
        Manager rm = this.locator.getManager();
        Caller superuser = this.populator().getSuperuserCaller();

        logger.info(rm.getVMMReport());

        logger.debug("Submitting backfill request");

        AsyncCreateRequest backfill1 = this.populator().getBackfillRequest("backfill1", 1);
        RequestInfo backfill1Result = rm.addBackfillRequest(backfill1, superuser);

        logger.debug("Waiting 2 seconds for resources to be allocated.");
        Thread.sleep(2000);

        // Check backfill request state
        RequestInfo[] backfillRequestsByCaller = rm.getBackfillRequestsByCaller(superuser);
        assertEquals(1, backfillRequestsByCaller.length);

        logger.info(rm.getVMMReport());

        // Set the shutdown task to not work
        MockShutdownTrash.resetFailCount();
        MockShutdownTrash.setFail(true);
        logger.warn("Set to fail.");

        // One regular VM that needs all the 512 RAM will preempt
        Caller caller = this.populator().getCaller();
        CreateRequest req = this.populator().getCreateRequest("regular", 1200, 512 , 1);

        // In 10 seconds, trigger the shutdown task to start succeeding
        this.suiteExecutor.submit(new DestroyEnableFutureTask(10));

        final long startMs = System.currentTimeMillis();
        final CreateResult result = rm.create(req, caller);

        final long endMs = System.currentTimeMillis();
        final long totalSeconds = (endMs - startMs) / 1000;
        logger.info("Total seconds: " + totalSeconds);

        final VM[] vms = result.getVMs();
        assertEquals(1, vms.length);
        assertNotNull(vms[0]);
        logger.info("Leased vm '" + vms[0].getID() + '\'');

        assertTrue(rm.exists(vms[0].getID(), Manager.INSTANCE));

        // That should have only waited around ~10 seconds (+/- 2 seconds for sweeper)
        assertTrue(totalSeconds > 9);
        assertTrue(totalSeconds < 14);
    }

}
