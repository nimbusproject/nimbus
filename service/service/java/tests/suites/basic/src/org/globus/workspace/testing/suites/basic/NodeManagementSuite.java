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
package org.globus.workspace.testing.suites.basic;

import com.google.gson.Gson;
import org.globus.workspace.remoting.admin.VmmNode;
import org.globus.workspace.testing.NimbusTestBase;
import org.globus.workspace.testing.NimbusTestContextLoader;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.CreateRequest;
import org.nimbustools.api.repr.CreateResult;
import org.nimbustools.api.repr.vm.VM;
import org.nimbustools.api.services.admin.RemoteNodeManagement;
import org.nimbustools.api.services.rm.Manager;
import org.nimbustools.api.services.rm.ResourceRequestDeniedException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.Test;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

@ContextConfiguration(
        locations={"file:./service/service/java/tests/suites/basic/home/services/etc/nimbus/workspace-service/other/main.xml"},
        loader=NimbusTestContextLoader.class)
public class NodeManagementSuite extends NimbusTestBase {

    RemoteNodeManagement remoteNodeManagement;

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

    protected void setUpVmms() throws RemoteException {
        logger.info("Before test method: overriden setUpVmms(), unique VMM list");

        boolean active = true;
        String nodePool = "default";
        boolean vacant = true;
        Gson gson = new Gson();
        List<VmmNode> nodes = new ArrayList<VmmNode>();
        nodes.add(new VmmNode("fakehost1", active, nodePool, 64, "*", vacant));
        final String nodesJson = gson.toJson(nodes);
        RemoteNodeManagement rnm = this.locator.getNodeManagement();
        rnm.addNodes(nodesJson);
        this.remoteNodeManagement = rnm;
    }

    // -----------------------------------------------------------------------------------------
    // TESTS
    // -----------------------------------------------------------------------------------------

    @Test
    @DirtiesContext
    public void memoryFreed() throws Exception {
        final Manager rm = this.locator.getManager();
        final Caller caller = this.populator().getCaller();
        final CreateRequest request =
                this.populator().getCreateRequest("memoryFreed", 240, 63, 1);
        final CreateResult result = rm.create(request, caller);
        final VM[] vms = result.getVMs();

        VmmNode node = getNodes()[0];
        assertFalse(node.isVacant());

        rm.trash(vms[0].getID(), Manager.INSTANCE, caller);
        node = getNodes()[0];

        assertTrue(node.isVacant());
    }

    @Test
    @DirtiesContext
    public void badNetworkMemoryFreed() throws Exception {
        final Manager rm = this.locator.getManager();
        final Caller caller = this.populator().getCaller();
        final CreateRequest request =
                this.populator().getCreateRequest("badNetworkMemoryFreed", 240, 63, 1, null, "notarealnetwork");
        boolean gotError = false;
        try {
            rm.create(request, caller);

        } catch (ResourceRequestDeniedException e) {
            gotError = true;
            logger.error("Got error", e);
        }
        assertTrue(gotError);

        VmmNode node = getNodes()[0];
        assertTrue(node.isVacant());

    }

    private VmmNode[] getNodes() throws RemoteException {
        final String nodesJson = this.remoteNodeManagement.listNodes();
        final Gson gson = new Gson();
        return gson.fromJson(nodesJson, VmmNode[].class);
    }


}