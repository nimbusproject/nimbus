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

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import org.globus.workspace.testing.NimbusTestBase;
import org.globus.workspace.testing.NimbusTestContextLoader;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.CreateResult;
import org.nimbustools.api.repr.vm.VM;
import org.nimbustools.api.services.rm.Manager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.Test;


/**
 * Suite to exemplify the use of spring test annotation @DirtiesContext, 
 * that indicate that a test method dirties the context for the current test. 
 * 
 * This annotation can be used if the context needs to be reloaded,
 * because a test has modified it (for example, by making the DB dirty 
 * or replacing a bean definition).
 * 
 * In this example, the test dirtyTest() leases a VM but doesn't
 * destroy it at the end of the test. The following test (checkEmpty())
 * checks if there are no VM leases. If the @DirtiesContext annotation
 * is not used in the dirtyTest(), the checkEmpty() test fails.
 */
@ContextConfiguration(
        locations={"file:./service/service/java/tests/suites/basic/home/services/etc/nimbus/workspace-service/other/main.xml"},
        loader=NimbusTestContextLoader.class)
public class DirtiesContextExampleSuite extends NimbusTestBase {

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
        return this.determineSuitesPath() + "/basic/home";
    }


    // -----------------------------------------------------------------------------------------
    // TESTS
    // -----------------------------------------------------------------------------------------

    /**
     * Lease a VM and doesn't destroy it
     * @throws Exception problem
     */
    @DirtiesContext
    @Test(groups="dirtyTest")
    public void dirtyTest() throws Exception {
        logger.debug("dirtyTest");
        final Manager rm = this.locator.getManager();

        final Caller caller = this.populator().getCaller();
        final CreateResult result =
                rm.create(this.populator().getCreateRequest("suite:basic:dirtyTest"),
                          caller);

        final VM[] vms = result.getVMs();
        assertEquals(1, vms.length);
        assertNotNull(vms[0]);
        logger.info("Leased vm '" + vms[0].getID() + '\'');

        assertTrue(rm.exists(vms[0].getID(), Manager.INSTANCE));
    }
    

    /**
     * Check if no VM's are left from previous test
     * @throws Exception problem
     */
    @Test(dependsOnGroups="dirtyTest")
    public void checkEmpty() throws Exception {
        logger.debug("checkExistence");
        final Manager rm = this.locator.getManager();
        
        final VM[] vms = rm.getGlobalAll();

        assertEquals(0, vms.length);
    }
    
}
