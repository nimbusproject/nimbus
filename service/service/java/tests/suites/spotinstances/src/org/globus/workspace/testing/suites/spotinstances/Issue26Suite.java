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
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.CreateRequest;
import org.nimbustools.api.repr.CreateResult;
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
public class Issue26Suite extends NimbusTestBase {

    // -----------------------------------------------------------------------------------------
    // STATIC VARIABLES
    // -----------------------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(Issue26Suite.class.getName());


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
        List<VmmNode> nodes = new ArrayList<VmmNode>(4);
        nodes.add(new VmmNode("fakehost1", true, "default", 3072, "*", true));
        nodes.add(new VmmNode("fakehost2", true, "default", 3072, "*", true));
        nodes.add(new VmmNode("fakehost3", true, "default", 3072, "*", true));
        nodes.add(new VmmNode("fakehost4", true, "default", 3072, "*", true));
        final String nodesJson = gson.toJson(nodes);
        RemoteNodeManagement rnm = this.locator.getNodeManagement();
        rnm.addNodes(nodesJson);
    }

        // -----------------------------------------------------------------------------------------
    // TESTS
    // -----------------------------------------------------------------------------------------

    @Test
    @DirtiesContext
    public void tooMuchMemory() throws Exception {
        final Manager rm = this.locator.getManager();
        final Caller caller = this.populator().getCaller();
        final CreateRequest request =
                this.populator().getCreateRequest("suite:issue26:tooMuchMemory", 240, 3584, 1);
        boolean notEnoughMemory = false;
        try {
            final CreateResult result = rm.create(request, caller);
        } catch (NotEnoughMemoryException e) {
            notEnoughMemory = true;
        }
        assertTrue(notEnoughMemory);
    }
}
