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

package org.globus.workspace.testing.suites.basic;

import org.globus.workspace.testing.NimbusTestBase;
import org.nimbustools.api.repr.CreateResult;
import org.nimbustools.api.repr.vm.VM;
import org.nimbustools.api.services.rm.Manager;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;

import java.io.File;

public class BasicSuite extends NimbusTestBase {

    // -----------------------------------------------------------------------------------------
    // extends NimbusTestBase
    // -----------------------------------------------------------------------------------------

    @BeforeSuite
    @Override
    public void suiteSetup() throws Exception {
        super.suiteSetup();
    }

    @AfterSuite
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
        return this.determineSuitesPath() + File.separator + "basic/home";
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

        final CreateResult result =
                rm.create(this.populator().getCreateRequest("suite:basic:leaseOne"),
                          this.populator().getCaller());

        final VM[] vms = result.getVMs();
        assertEquals(1, vms.length);
        for (VM vm : vms) {
            logger.info("Leased vm '" + vm.getID() + '\'');
        }
    }
}
