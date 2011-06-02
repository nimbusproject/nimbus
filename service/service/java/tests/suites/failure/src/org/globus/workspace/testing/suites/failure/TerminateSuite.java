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
     * @throws Exception problem
     */
    @Test(groups="prereqs")
    public void retrieveModuleLocator() throws Exception {
        logger.debug("retrieveModuleLocator");
        final Manager rm = this.locator.getManager();
        final VM[] vms = rm.getGlobalAll();

        // we know there are zero so far because it is in group 'prereqs'
        assertEquals(0, vms.length);
    }

    /**
     * Lease a VM and then destroy it.
     * @throws Exception problem
     */
    @Test(dependsOnGroups="prereqs")
    public void leaseOne() throws Exception {
        logger.debug("leaseOne");
        final Manager rm = this.locator.getManager();

        final Caller caller = this.populator().getCaller();
        final CreateResult result =
                rm.create(this.populator().getCreateRequest("suite:basic:leaseOne"),
                          caller);

        final VM[] vms = result.getVMs();
        assertEquals(1, vms.length);
        assertNotNull(vms[0]);
        logger.info("Leased vm '" + vms[0].getID() + '\'');

        assertTrue(rm.exists(vms[0].getID(), Manager.INSTANCE));

        Thread.sleep(4000L);
        rm.trash(vms[0].getID(), Manager.INSTANCE, caller);

        Thread.sleep(1000L);
        
        final VM[] allvms = rm.getGlobalAll();
        assertEquals(0, allvms.length);

    }
}
