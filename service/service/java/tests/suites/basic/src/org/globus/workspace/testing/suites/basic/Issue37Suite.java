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
import org.nimbustools.api.repr.vm.NIC;
import org.nimbustools.api.repr.vm.VM;
import org.nimbustools.api.services.admin.RemoteNodeManagement;
import org.nimbustools.api.services.rm.Manager;
import org.nimbustools.api.services.rm.ResourceRequestDeniedException;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.Test;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

@ContextConfiguration(
        locations={"file:./service/service/java/tests/suites/basic/home/services/etc/nimbus/workspace-service/other/main.xml"},
        loader=NimbusTestContextLoader.class)
public class Issue37Suite extends NimbusTestBase {

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
        List<VmmNode> nodes = new ArrayList<VmmNode>(4);
        nodes.add(new VmmNode("fakehost1", active, nodePool, 64, "public", vacant));
        nodes.add(new VmmNode("fakehost2", active, nodePool, 64, "private", vacant));
        final String nodesJson = gson.toJson(nodes);
        RemoteNodeManagement rnm = this.locator.getNodeManagement();
        rnm.addNodes(nodesJson);
    }

    /**
     * Lease a VM with an explicit private network, another with public, and then destroy them.
     *
     * @throws Exception problem
     */
    @Test
    public void leaseOnePublicOnePrivate() throws Exception {
        logger.debug("leaseOnePublicOnePrivate");
        final Manager rm = this.locator.getManager();

        final Caller caller = this.populator().getCaller();

        /* One */
        final CreateRequest req =
                this.populator().getCreateRequestCustomNetwork(
                        "suite:issue37:leaseOnePublicOnePrivate",
                        "public");
        final CreateResult result = rm.create(req, caller);

        final VM[] vms = result.getVMs();
        assertEquals(1, vms.length);
        assertNotNull(vms[0]);
        logger.info("Leased vm '" + vms[0].getID() + '\'');

        assertTrue(rm.exists(vms[0].getID(), Manager.INSTANCE));

        final NIC[] nics = vms[0].getNics();
        assertEquals(1, nics.length);
        assertEquals("public", nics[0].getNetworkName());

        /* Two */
        final CreateRequest req2 =
                this.populator().getCreateRequestCustomNetwork(
                        "suite:issue37:leaseOnePublicOnePrivate",
                        "private");
        final CreateResult result2 = rm.create(req2, caller);

        final VM[] vms2 = result2.getVMs();
        assertEquals(1, vms2.length);
        assertNotNull(vms2[0]);
        logger.info("Leased vm '" + vms2[0].getID() + '\'');

        assertTrue(rm.exists(vms2[0].getID(), Manager.INSTANCE));

        final NIC[] nics2 = vms2[0].getNics();
        assertEquals(1, nics2.length);
        assertEquals("private", nics2[0].getNetworkName());

        Thread.sleep(1000L);
        rm.trash(vms[0].getID(), Manager.INSTANCE, caller);
        rm.trash(vms2[0].getID(), Manager.INSTANCE, caller);
    }

    /**
     * It should not be possible to lease two VMs that want private networks, there is only
     * one VMM with private ability (and RAM is being filled).
     *
     * @throws Exception problem
     */
    @Test
    public void leaseTwoPrivate() throws Exception {
        logger.debug("leaseTwoPrivate");
        final Manager rm = this.locator.getManager();

        final Caller caller = this.populator().getCaller();

        /* One */
        final CreateRequest req =
                this.populator().getCreateRequestCustomNetwork(
                        "suite:issue37:leaseTwoPrivate",
                        "private");
        final CreateResult result = rm.create(req, caller);

        final VM[] vms = result.getVMs();
        assertEquals(1, vms.length);
        assertNotNull(vms[0]);
        logger.info("Leased vm '" + vms[0].getID() + '\'');

        assertTrue(rm.exists(vms[0].getID(), Manager.INSTANCE));

        final NIC[] nics = vms[0].getNics();
        assertEquals(1, nics.length);
        assertEquals("private", nics[0].getNetworkName());

        /* It should not be possible to lease two VMs that want private networks,
           there is only one VMM with private ability (and RAM is being filled). */
        final CreateRequest req2 =
                this.populator().getCreateRequestCustomNetwork(
                        "suite:issue37:leaseOnePublicOnePrivate",
                        "private");

        boolean rrdException = false; 
        try {
            final CreateResult result2 = rm.create(req2, caller);
        } catch (ResourceRequestDeniedException e) {
            rrdException = true;
        }
        assertTrue(rrdException);

        Thread.sleep(1000L);
        rm.trash(vms[0].getID(), Manager.INSTANCE, caller);
    }
}
