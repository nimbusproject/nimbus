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
import org.nimbustools.api.repr.CreateRequest;
import org.nimbustools.api.repr.CreateResult;
import org.nimbustools.api.repr.vm.State;
import org.nimbustools.api.repr.vm.VM;
import org.nimbustools.api.services.rm.Manager;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.Test;


/**
 * Tests that exercise the termination system, in particular the retry feature that makes it
 * possible for the service to continue killing the nodes til they're actually dead and gone.
 * Transient termination failures have been an issue (e.g. the VMM cannot be contacted at
 * that moment).
 *
 * Now there are two new internal states introduced: DestroySucceeded and DestroyFailed.
 * Only when DestroySucceeded has been reached will the workspace service remove leases etc.
 * If the VM is in the DestroyFailed state, the resource sweeper will try to kill it.
 *
 * The TestNG "singleThreaded" setting does not seem to be taking (this may be an IDEA
 * config issue), so instead each test below just "depends" on the previous one.  If they
 * run simultaneously, they will fail because a static mock task is used and also the global
 * VM count is examined.
 *
 * This "depends" situation has the unfortunate side effect of requiring you to change the
 * depends to "prereqs" if you just want to run a single method over and over vs. running
 * the whole suite.
 */
@ContextConfiguration(
        locations={"file:./service/service/java/tests/suites/failure/home/services/etc/nimbus/workspace-service/other/main.xml"},
        loader=NimbusTestContextLoader.class)
public class TerminateSuite extends NimbusTestBase {

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
    @Test(groups="prereqs")
    public void retrieveModuleLocator() throws Exception {
        logger.debug("retrieveModuleLocator");
        final Manager rm = this.locator.getManager();
        final VM[] vms = rm.getGlobalAll();

        // we know there are zero so far because it is in group 'prereqs'
        assertEquals(0, vms.length);
    }


    // -----------------------------------------------------------------------------------------
    // NORMAL TERMINATE BEHAVIOR
    // -----------------------------------------------------------------------------------------

    /**
     * Lease a VM and then destroy it.
     */
    @Test(groups="basic1", dependsOnGroups="prereqs")
    public void leaseOne() throws Exception {
        logger.debug("leaseOne");
        final Manager rm = this.locator.getManager();

        VM[] allvms = rm.getGlobalAll();
        assertEquals(0, allvms.length);

        final Caller caller = this.populator().getCaller();
        final CreateResult result =
                rm.create(this.populator().getCreateRequest("suite:failure:leaseOne"),
                          caller);

        final VM[] vms = result.getVMs();
        assertEquals(1, vms.length);
        assertNotNull(vms[0]);
        logger.info("Leased vm '" + vms[0].getID() + '\'');

        assertTrue(rm.exists(vms[0].getID(), Manager.INSTANCE));

        Thread.sleep(4000L);
        rm.trash(vms[0].getID(), Manager.INSTANCE, caller);

        Thread.sleep(1000L);
        
        allvms = rm.getGlobalAll();
        assertEquals(0, allvms.length);

    }

    /**
     * Lease a number of VMs and then destroy them all at once.
     */
    @Test(groups="basic5", dependsOnGroups="basic1")
    public void leaseMany() throws Exception {
        logger.debug("leaseMany");
        final Manager rm = this.locator.getManager();

        VM[] allvms = rm.getGlobalAll();
        assertEquals(0, allvms.length);

        final int durationSecs = 240;
        final int memory = 64;
        final int numNodes = 5;
        final CreateRequest creq =
                this.populator().getCreateRequest("suite:failure:leaseMany",
                                                  durationSecs, memory, numNodes);
        final Caller caller = this.populator().getCaller();
        final CreateResult result = rm.create(creq, caller);

        final VM[] vms = result.getVMs();
        assertEquals(5, vms.length);
        for (int i = 0; i < vms.length; i++) {
            assertNotNull(vms[i]);
            logger.info("Leased vm '" + vms[i].getID() + '\'');
            assertTrue(rm.exists(vms[i].getID(), Manager.INSTANCE));
        }

        Thread.sleep(2000L);
        
        for (int i = 0; i < vms.length; i++) {
            assertTrue(rm.exists(vms[i].getID(), Manager.INSTANCE));
        }
        allvms = rm.getGlobalAll();
        assertEquals(5, allvms.length);

        Thread.sleep(2000L);

        for (int i = 0; i < vms.length; i++) {
            rm.trash(vms[i].getID(), Manager.INSTANCE, caller);
        }

        Thread.sleep(2000L);

        allvms = rm.getGlobalAll();
        assertEquals(0, allvms.length);

        for (int i = 0; i < vms.length; i++) {
            assertFalse(rm.exists(vms[i].getID(), Manager.INSTANCE));
        }
    }

    /**
     * Lease a VM and then wait for it to expire
     */
    @Test(groups="basicwait", dependsOnGroups="basic5")
    public void leaseOneWait() throws Exception {
        logger.debug("leaseOneWait");
        final Manager rm = this.locator.getManager();

        VM[] allvms = rm.getGlobalAll();
        assertEquals(0, allvms.length);

        final int durationSecs = 5;
        final int memory = 64;
        final int numNodes = 1;
        final CreateRequest creq =
                this.populator().getCreateRequest("suite:failure:leaseOneWait",
                                                  durationSecs, memory, numNodes);
        final Caller caller = this.populator().getCaller();
        final CreateResult result = rm.create(creq, caller);

        final VM[] vms = result.getVMs();
        assertEquals(1, vms.length);
        assertNotNull(vms[0]);
        logger.info("Leased vm '" + vms[0].getID() + '\'');
        assertTrue(rm.exists(vms[0].getID(), Manager.INSTANCE));

        Thread.sleep(10000L);

        assertFalse(rm.exists(vms[0].getID(), Manager.INSTANCE));
        allvms = rm.getGlobalAll();
        assertEquals(0, allvms.length);
    }

    /**
     * Lease a VM and then unpropagate it.  Then allow to expire
     */
    @Test(groups="unpropWait", dependsOnGroups="basicwait")
    public void unpropOneWait() throws Exception {
        logger.debug("unpropOneWait");
        final Manager rm = this.locator.getManager();

        VM[] allvms = rm.getGlobalAll();
        assertEquals(0, allvms.length);

        final int durationSecs = 5;
        final int memory = 64;
        final int numNodes = 1;
        final CreateRequest creq =
                this.populator().getCreateRequest("suite:failure:unpropOneWait",
                                                  durationSecs, memory, numNodes);
        final Caller caller = this.populator().getCaller();
        final CreateResult result = rm.create(creq, caller);

        final VM[] vms = result.getVMs();
        assertEquals(1, vms.length);
        assertNotNull(vms[0]);
        logger.info("Leased vm '" + vms[0].getID() + '\'');
        assertTrue(rm.exists(vms[0].getID(), Manager.INSTANCE));

        Thread.sleep(1500L);

        allvms = rm.getGlobalAll();
        assertEquals(State.STATE_Running, allvms[0].getState().getState());

        // Unpropagate:
        rm.shutdownSave(vms[0].getID(), Manager.INSTANCE,
                        this.populator().getShutdownTasks() , caller);

        allvms = rm.getGlobalAll();
        assertEquals(1, allvms.length);
        
        // Wait for complete termination:
        Thread.sleep(15000L);

        assertFalse(rm.exists(vms[0].getID(), Manager.INSTANCE));
        allvms = rm.getGlobalAll();
        assertEquals(0, allvms.length);
    }


    // -----------------------------------------------------------------------------------------
    // FAILURE BEHAVIOR
    // -----------------------------------------------------------------------------------------

    /**
     * Lease one VM, kill it but trigger shutdown task to fail.  System should keep trying.
     * After a wait, trigger shutdown task to succeed.  Ensure it finally gets killed.
     */
    @Test(groups="basicfail1", dependsOnGroups="unpropWait")
    public void basicFail() throws Exception {
        logger.debug("basicFail");
        final Manager rm = this.locator.getManager();

        VM[] allvms = rm.getGlobalAll();
        assertEquals(0, allvms.length);
        
        MockShutdownTrash.resetFailCount();

        final Caller caller = this.populator().getCaller();
        final CreateResult result =
                rm.create(this.populator().getCreateRequest("suite:failure:basicFail"),
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
        Thread.sleep(4000L);

        // Check that it is at least trying to terminate the node:
        int lastFailCount = MockShutdownTrash.getFailCount();
        assertFalse(0 == lastFailCount);
        
        assertTrue(rm.exists(vms[0].getID(), Manager.INSTANCE));
        allvms = rm.getGlobalAll();
        assertEquals(1, allvms.length);

        Thread.sleep(4000L);
        assertTrue(rm.exists(vms[0].getID(), Manager.INSTANCE));
        allvms = rm.getGlobalAll();
        assertEquals(1, allvms.length);

        // Start succeeding to kill the instance:
        MockShutdownTrash.setFail(false);
        logger.warn("Set to succeed.");

        Thread.sleep(8000L);
        
        // Should now be gone:
        assertFalse(rm.exists(vms[0].getID(), Manager.INSTANCE));
        allvms = rm.getGlobalAll();
        assertEquals(0, allvms.length);
    }

    /**
     * More of the same idea as in basicFail() but working with more instances and situations
     */
    @Test(groups="complicatedfail", dependsOnGroups="basicfail1")
    public void complicatedFail() throws Exception {
        logger.debug("complicatedFail");
        final Manager rm = this.locator.getManager();

        VM[] allvms = rm.getGlobalAll();
        assertEquals(0, allvms.length);
        
        MockShutdownTrash.resetFailCount();

        final int durationSecs = 240;
        final int memory = 64;
        final int numNodes = 5;
        final CreateRequest creq =
                this.populator().getCreateRequest("suite:failure:complicatedFail",
                                                  durationSecs, memory, numNodes);
        final Caller caller = this.populator().getCaller();
        final CreateResult result = rm.create(creq, caller);

        final VM[] vms = result.getVMs();
        assertEquals(5, vms.length);
        for (int i = 0; i < vms.length; i++) {
            assertNotNull(vms[i]);
            logger.info("Leased vm '" + vms[i].getID() + '\'');
            assertTrue(rm.exists(vms[i].getID(), Manager.INSTANCE));
        }

        Thread.sleep(2000L);
        assertEquals(0, MockShutdownTrash.getFailCount());

        // Fail at killing the instances:
        MockShutdownTrash.setFail(true);
        logger.warn("Set to fail.");

        rm.trash(vms[0].getID(), Manager.INSTANCE, caller);
        rm.trash(vms[1].getID(), Manager.INSTANCE, caller);
        Thread.sleep(4000L);

        int lastFailCount = MockShutdownTrash.getFailCount();
        assertFalse(0 == lastFailCount);
        assertTrue(rm.exists(vms[0].getID(), Manager.INSTANCE));
        assertTrue(rm.exists(vms[1].getID(), Manager.INSTANCE));
        allvms = rm.getGlobalAll();
        assertEquals(5, allvms.length);

        Thread.sleep(4000L);
        assertTrue(rm.exists(vms[0].getID(), Manager.INSTANCE));
        assertTrue(rm.exists(vms[1].getID(), Manager.INSTANCE));
        allvms = rm.getGlobalAll();
        assertEquals(5, allvms.length);

        // Start succeeding to kill the instances:
        MockShutdownTrash.setFail(false);
        logger.warn("Set to succeed.");

        Thread.sleep(8000L);

        // Should now be gone:
        assertFalse(rm.exists(vms[0].getID(), Manager.INSTANCE));
        assertFalse(rm.exists(vms[1].getID(), Manager.INSTANCE));
        allvms = rm.getGlobalAll();
        assertEquals(3, allvms.length);

        // Start counting from scratch
        MockShutdownTrash.resetFailCount();

        // Add two in a new group, with very short duration
        final int durationSecs2 = 5;
        final int memory2 = 64;
        final int numNodes2 = 2;
        final CreateRequest creq2 =
                this.populator().getCreateRequest("suite:failure:complicatedFail2",
                                                  durationSecs2, memory2, numNodes2);
        final CreateResult result2 = rm.create(creq2, caller);

        VM[] vms2 = result2.getVMs();
        assertEquals(2, vms2.length);
        for (int i = 0; i < vms2.length; i++) {
            assertNotNull(vms2[i]);
            logger.info("Leased vm '" + vms2[i].getID() + '\'');
            assertTrue(rm.exists(vms2[i].getID(), Manager.INSTANCE));
        }

        allvms = rm.getGlobalAll();
        assertEquals(5, allvms.length);
        
        // Wait for them to terminate successfully
        Thread.sleep(8000L);

        assertEquals(0, MockShutdownTrash.getFailCount());

        // Should now be gone:
        assertFalse(rm.exists(vms2[0].getID(), Manager.INSTANCE));
        assertFalse(rm.exists(vms2[1].getID(), Manager.INSTANCE));
        allvms = rm.getGlobalAll();
        assertEquals(3, allvms.length);

        // Old ones should remain:
        assertTrue(rm.exists(vms[2].getID(), Manager.INSTANCE));
        assertTrue(rm.exists(vms[3].getID(), Manager.INSTANCE));
        assertTrue(rm.exists(vms[4].getID(), Manager.INSTANCE));

        // Fail at killing the instances:
        MockShutdownTrash.setFail(true);
        logger.warn("Set to fail.");

        // Try to kill all but one
        rm.trash(vms[2].getID(), Manager.INSTANCE, caller);
        rm.trash(vms[3].getID(), Manager.INSTANCE, caller);
        Thread.sleep(4000L);

        // Shouldn't work
        allvms = rm.getGlobalAll();
        assertEquals(3, allvms.length);
        assertTrue(rm.exists(vms[2].getID(), Manager.INSTANCE));
        assertTrue(rm.exists(vms[3].getID(), Manager.INSTANCE));
        assertTrue(rm.exists(vms[4].getID(), Manager.INSTANCE));

        // Start succeeding to kill the instances:
        MockShutdownTrash.setFail(false);
        logger.warn("Set to succeed.");

        Thread.sleep(8000L);

        // Should now be gone:
        assertFalse(rm.exists(vms[2].getID(), Manager.INSTANCE));
        assertFalse(rm.exists(vms[3].getID(), Manager.INSTANCE));
        allvms = rm.getGlobalAll();
        assertEquals(1, allvms.length);

        // And this one should now die right away:
        rm.trash(vms[4].getID(), Manager.INSTANCE, caller);
        assertFalse(rm.exists(vms[4].getID(), Manager.INSTANCE));
        allvms = rm.getGlobalAll();
        assertEquals(0, allvms.length);
    }


    /**
     * Lease a VM and then shut it down (but not trash or unpropagate). Then play the
     * termination failed game
     */
    @Test(groups="unpropFail", dependsOnGroups="complicatedfail")
    public void unpropOneFail() throws Exception {
        logger.debug("unpropOneFail");
        final Manager rm = this.locator.getManager();

        VM[] allvms = rm.getGlobalAll();
        assertEquals(0, allvms.length);

        final int durationSecs = 5;
        final int memory = 64;
        final int numNodes = 1;
        final CreateRequest creq =
                this.populator().getCreateRequest("suite:failure:unpropOneFail",
                                                  durationSecs, memory, numNodes);
        final Caller caller = this.populator().getCaller();
        final CreateResult result = rm.create(creq, caller);

        final VM[] vms = result.getVMs();
        assertEquals(1, vms.length);
        assertNotNull(vms[0]);
        logger.info("Leased vm '" + vms[0].getID() + '\'');
        assertTrue(rm.exists(vms[0].getID(), Manager.INSTANCE));

        Thread.sleep(1500L);

        MockShutdownTrash.resetFailCount();

        allvms = rm.getGlobalAll();
        assertEquals(State.STATE_Running, allvms[0].getState().getState());

        // Shutdown (but don't unpropagate or trash it):
        rm.shutdown(vms[0].getID(), Manager.INSTANCE, null, caller);

        allvms = rm.getGlobalAll();
        assertEquals(1, allvms.length);

        // Fail at killing the instances:
        MockShutdownTrash.setFail(true);
        logger.warn("Set to fail.");

        // Wait for complete termination ... to fail.
        Thread.sleep(15000L);

        assertFalse(0 == MockShutdownTrash.getFailCount());

        assertTrue(rm.exists(vms[0].getID(), Manager.INSTANCE));
        allvms = rm.getGlobalAll();
        assertEquals(1, allvms.length);

        // Start succeeding to kill the instance:
        MockShutdownTrash.setFail(false);
        logger.warn("Set to succeed.");

        Thread.sleep(8000L);

        // Should now be gone:
        assertFalse(rm.exists(vms[0].getID(), Manager.INSTANCE));
        allvms = rm.getGlobalAll();
        assertEquals(0, allvms.length);
        
    }
}
