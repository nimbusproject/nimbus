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

package org.globus.workspace.testing.suites.failure;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import org.globus.workspace.testing.NimbusTestBase;
import org.globus.workspace.testing.NimbusTestContextLoader;
import org.globus.workspace.xen.xenssh.MockShutdownTrash;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.CreateResult;
import org.nimbustools.api.repr.vm.VM;
import org.nimbustools.api.services.rm.Manager;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.Test;


@ContextConfiguration(
        locations={"file:./service/service/java/tests/suites/failure/home/services/etc/nimbus/workspace-service/other/main.xml"},
        loader=NimbusTestContextLoader.class)
public class BackoffSuite extends NimbusTestBase {

    // -----------------------------------------------------------------------------------------
    // extends NimbusTestBase
    // -----------------------------------------------------------------------------------------

    @AfterSuite(alwaysRun=true)
    @Override
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
        return this.determineSuitesPath() + "/failure/home";
    }


    // -----------------------------------------------------------------------------------------
    // PREREQ TESTS (if any of these fail, nothing else will work at all)
    // -----------------------------------------------------------------------------------------

    /**
     * Check if ModuleLocator can be retrieved and used at all.
     */
    @Test(groups="pr3r3qs")
    public void retrieveModuleLocator() throws Exception {
        logger.debug("retrieveModuleLocator");
        final Manager rm = this.locator.getManager();
        final VM[] vms = rm.getGlobalAll();

        // we know there are zero so far because it is in group 'prereqs'
        assertEquals(0, vms.length);
    }


    // -----------------------------------------------------------------------------------------
    // FAILURE BEHAVIOR
    // -----------------------------------------------------------------------------------------

    @Test(groups="nobackoff", dependsOnGroups="pr3r3qs")
    public void noBackoff() throws Exception {
        logger.debug("nobackoff");
        final Manager rm = this.locator.getManager();

        VM[] allvms = rm.getGlobalAll();
        assertEquals(0, allvms.length);
        
        MockShutdownTrash.resetFailCount();

        final Caller caller = this.populator().getCaller();
        final CreateResult result =
                rm.create(this.populator().getCreateRequest("suite:backoff:nobackoff"),
                          caller);

        final VM[] vms = result.getVMs();
        assertEquals(1, vms.length);
        assertNotNull(vms[0]);
        logger.info("Leased vm '" + vms[0].getID() + '\'');

        assertTrue(rm.exists(vms[0].getID(), Manager.INSTANCE));

        Thread.sleep(2000L);
        assertEquals(0, MockShutdownTrash.getFailCount());

        // Fail at killing the instance:
        MockShutdownTrash.setFail(true);
        logger.warn("Set to fail.");

        rm.trash(vms[0].getID(), Manager.INSTANCE, caller);
        Thread.sleep(2000L);

        // Check that it is at least trying to terminate the node:
        int lastFailCount = MockShutdownTrash.getFailCount();
        assertFalse(0 == lastFailCount);
        
        assertTrue(rm.exists(vms[0].getID(), Manager.INSTANCE));
        allvms = rm.getGlobalAll();
        assertEquals(1, allvms.length);

        long lastFailMs = MockShutdownTrash.getMsAtLastAttempt();
        assertFalse(0 == lastFailMs);

        Thread.sleep(4000L);

        long nextFailMs = MockShutdownTrash.getMsAtLastAttempt();
        assertFalse(nextFailMs == lastFailMs);

        assertTrue(rm.exists(vms[0].getID(), Manager.INSTANCE));
        allvms = rm.getGlobalAll();
        assertEquals(1, allvms.length);

        // Start succeeding to kill the instance:
        MockShutdownTrash.setFail(false);
        logger.warn("Set to succeed.");

        Thread.sleep(4000L);
        
        // Should now be gone:
        assertFalse(rm.exists(vms[0].getID(), Manager.INSTANCE));
        allvms = rm.getGlobalAll();
        assertEquals(0, allvms.length);

        long finalFailMs = MockShutdownTrash.getMsAtLastAttempt();
        assertFalse(finalFailMs == nextFailMs);

        // It should have stopped
        Thread.sleep(4000L);
        assertEquals(finalFailMs, MockShutdownTrash.getMsAtLastAttempt());
    }


    @Test(groups="yesbackoff", dependsOnGroups="nobackoff")
    public void testBackoff() throws Exception {
        logger.debug("yesbackoff");
        final Manager rm = this.locator.getManager();

        VM[] allvms = rm.getGlobalAll();
        assertEquals(0, allvms.length);

        MockShutdownTrash.resetFailCount();

        final Caller caller = this.populator().getCaller();
        final CreateResult result =
                rm.create(this.populator().getCreateRequest("suite:backoff:yesbackoff"),
                          caller);

        final VM[] vms = result.getVMs();
        assertEquals(1, vms.length);
        assertNotNull(vms[0]);
        logger.info("Leased vm '" + vms[0].getID() + '\'');

        assertTrue(rm.exists(vms[0].getID(), Manager.INSTANCE));

        Thread.sleep(2000L);
        assertEquals(0, MockShutdownTrash.getFailCount());

        // Fail at killing the instance:
        MockShutdownTrash.setFail(true);
        logger.warn("Set to fail.");

        rm.trash(vms[0].getID(), Manager.INSTANCE, caller);
        Thread.sleep(2000L);

        // Check that it is at least trying to terminate the node:
        int lastFailCount = MockShutdownTrash.getFailCount();
        assertFalse(0 == lastFailCount);

        assertTrue(rm.exists(vms[0].getID(), Manager.INSTANCE));
        allvms = rm.getGlobalAll();
        assertEquals(1, allvms.length);

        long prevFailMs = MockShutdownTrash.getMsAtLastAttempt();
        assertFalse(0 == prevFailMs);

        Thread.sleep(4000L);

        long nextFailMs = MockShutdownTrash.getMsAtLastAttempt();
        assertFalse(nextFailMs == prevFailMs);

        logger.debug("fail difference: " + (nextFailMs - prevFailMs));

        prevFailMs = nextFailMs;
        nextFailMs = 0;

        assertTrue(rm.exists(vms[0].getID(), Manager.INSTANCE));
        allvms = rm.getGlobalAll();
        assertEquals(1, allvms.length);

        long markerTime1 = prevFailMs;

        Thread.sleep(2100L);
        nextFailMs = MockShutdownTrash.getMsAtLastAttempt();
        logger.debug("fail difference: " + (nextFailMs - prevFailMs));
        prevFailMs = nextFailMs;
        nextFailMs = 0;

        Thread.sleep(2300L);
        nextFailMs = MockShutdownTrash.getMsAtLastAttempt();
        logger.debug("fail difference: " + (nextFailMs - prevFailMs));
        prevFailMs = nextFailMs;
        nextFailMs = 0;

        Thread.sleep(1900L);
        nextFailMs = MockShutdownTrash.getMsAtLastAttempt();
        logger.debug("fail difference: " + (nextFailMs - prevFailMs));
        prevFailMs = nextFailMs;
        nextFailMs = 0;

        Thread.sleep(2100L);

        assertEquals(5, MockShutdownTrash.getFailCount());
        
        Thread.sleep(5000L);
        nextFailMs = MockShutdownTrash.getMsAtLastAttempt();
        logger.debug("fail difference: " + (nextFailMs - prevFailMs));
        prevFailMs = nextFailMs;
        nextFailMs = 0;

        Thread.sleep(4100L);

        assertEquals(6, MockShutdownTrash.getFailCount());
        
        nextFailMs = MockShutdownTrash.getMsAtLastAttempt();
        logger.debug("fail difference: " + (nextFailMs - prevFailMs));
        prevFailMs = nextFailMs;
        nextFailMs = 0;

        Thread.sleep(4300L);
        nextFailMs = MockShutdownTrash.getMsAtLastAttempt();
        logger.debug("fail difference: " + (nextFailMs - prevFailMs));
        prevFailMs = nextFailMs;
        nextFailMs = 0;

        Thread.sleep(4900L);
        nextFailMs = MockShutdownTrash.getMsAtLastAttempt();
        logger.debug("fail difference: " + (nextFailMs - prevFailMs));
        prevFailMs = nextFailMs;
        nextFailMs = 0;

        Thread.sleep(3100L);
        nextFailMs = MockShutdownTrash.getMsAtLastAttempt();
        logger.debug("fail difference: " + (nextFailMs - prevFailMs));
        prevFailMs = nextFailMs;
        nextFailMs = 0;

        Thread.sleep(4300L);
        nextFailMs = MockShutdownTrash.getMsAtLastAttempt();
        logger.debug("fail difference: " + (nextFailMs - prevFailMs));
        prevFailMs = nextFailMs;
        nextFailMs = 0;

        assertEquals(7, MockShutdownTrash.getFailCount());

        Thread.sleep(5100L);
        nextFailMs = MockShutdownTrash.getMsAtLastAttempt();
        logger.debug("fail difference: " + (nextFailMs - prevFailMs));
        prevFailMs = nextFailMs;
        nextFailMs = 0;

        Thread.sleep(6300L);
        nextFailMs = MockShutdownTrash.getMsAtLastAttempt();
        logger.debug("fail difference: " + (nextFailMs - prevFailMs));

        assertEquals(8, MockShutdownTrash.getFailCount());
    }

}
