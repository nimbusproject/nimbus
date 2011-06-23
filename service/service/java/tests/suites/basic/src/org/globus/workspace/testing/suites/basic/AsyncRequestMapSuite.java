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

import org.apache.commons.dbcp.BasicDataSource;
import org.globus.workspace.async.AsyncRequest;
import org.globus.workspace.async.AsyncRequestMap;
import org.globus.workspace.async.AsyncRequestStatus;
import org.globus.workspace.persistence.DataConvert;
import org.globus.workspace.persistence.PersistenceAdapter;
import org.globus.workspace.service.binding.vm.VirtualMachine;
import org.globus.workspace.testing.NimbusTestBase;
import org.globus.workspace.testing.NimbusTestContextLoader;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.CreateResult;
import org.nimbustools.api.repr.ctx.Context;
import org.nimbustools.api.repr.vm.NIC;
import org.nimbustools.api.repr.vm.VM;
import org.nimbustools.api.services.rm.Manager;
import org.springframework.scheduling.annotation.Async;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.Test;
import org.springframework.core.io.FileSystemResource;
import sun.management.FileSystem;


import java.util.Calendar;
import java.util.Collection;
import java.util.Properties;

import static org.testng.AssertJUnit.*;

@ContextConfiguration(
        locations={"file:./service/service/java/tests/suites/basic/home/services/etc/nimbus/workspace-service/other/main.xml"},
        loader=NimbusTestContextLoader.class)
public class AsyncRequestMapSuite extends NimbusTestBase {

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
    public void persistOne() throws Exception {
        logger.debug("persistOne");
        final Manager rm = this.locator.getManager();
        final Caller caller = this.populator().getCaller();

        PersistenceAdapter persistence = (PersistenceAdapter) applicationContext.getBean("nimbus-rm.persistence.PersistenceAdapter");
        AsyncRequestMap asyncRequestMap = new AsyncRequestMap(persistence);


        // Validate that we have a working AsyncRequestMap. It should be empty
        Collection<AsyncRequest> allRequests = asyncRequestMap.getAll();
        logger.debug("You have " + allRequests.size() + " requests.");
        assert(allRequests.size() == 0);

        // Test putting and getting from persistence
        String testID = "fake-id";
        Double testMaxBid = 42.0;
        boolean testSpotinstances = false;
        String testGroupID = "fake-group-id";
        boolean testIsPersistent = true;
        Caller testCaller = this.populator().getSuperuserCaller();
        Context context = null;
        String testSshKeyName = "fake-ssh-key";
        Calendar testCreationTime = Calendar.getInstance();
        testCreationTime.setTimeInMillis(424242424242l);
        String testNIC = "FakeName;FakeAssociation;FAKEMAC;NetMode;IPmethod;192.168.1.42;192.168.1.1;192.168.1.2;subnetmask;dns;hostname;null;null;null;null";
        VirtualMachine testVM = new VirtualMachine();
        testVM.setID(42);
        testVM.setName("fakename");
        testVM.setNetwork(testNIC);
        testVM.setPropagateRequired(false);
        testVM.setUnPropagateRequired(true);
        VirtualMachine[] testBindings = new VirtualMachine[1];
        testBindings[0] = testVM;
        int testAllocatedVM = 42;

        DataConvert dataConvert = new DataConvert(this.locator.getReprFactory());
        NIC[] testNICs = dataConvert.getNICs(testVM);
        logger.debug("Nics: " + testNICs[0]);

        //public AsyncRequest(String id, boolean spotinstances, Double spotPrice, boolean persistent, Caller caller, String groupID, VirtualMachine[] bindings, Context context, NIC[] requestedNics, String sshKeyName, Calendar creationTime) {
        AsyncRequest testRequest = new AsyncRequest(testID, testSpotinstances, testMaxBid, testIsPersistent, testCaller, testGroupID, testBindings, context, testNICs, testSshKeyName, testCreationTime);
        testRequest.addAllocatedVM(testAllocatedVM);
        asyncRequestMap.addOrReplace(testRequest);

        String secondID = "this-is-the-other-one";
        double secondMaxBid = 4.5;
        VirtualMachine[] secondBindings = new VirtualMachine[1];
        VirtualMachine secondVM = new VirtualMachine();
        secondVM.setID(52);
        secondBindings[0] = secondVM;

        AsyncRequest secondRequest = new AsyncRequest(secondID, testSpotinstances, secondMaxBid, testIsPersistent, testCaller, testGroupID, secondBindings, context, testNICs, testSshKeyName, testCreationTime);

        allRequests = asyncRequestMap.getAll();
        assert(allRequests != null);
        logger.debug("You have " + allRequests.size() + " requests.");
        assert(allRequests.size() == 1);

        // Note, the persistence layer just dumps the context, so we don't test for it

        AsyncRequest gotRequest = asyncRequestMap.getByID(testID);
        assertEquals(testID, gotRequest.getId());
        assertEquals(testMaxBid, gotRequest.getMaxBid());
        assertEquals(testSpotinstances, gotRequest.isSpotRequest());
        assertEquals(testGroupID, gotRequest.getGroupID());
        assertEquals(testIsPersistent, gotRequest.isPersistent());
        assertEquals(testCaller, gotRequest.getCaller());
        assertEquals(testSshKeyName, gotRequest.getSshKeyName());
        assertEquals(testCreationTime, gotRequest.getCreationTime());
        assertEquals(testVM.getID(), gotRequest.getBindings()[0].getID());
        assertEquals(testNICs[0].getIpAddress(), gotRequest.getRequestedNics()[0].getIpAddress());
        assertEquals(testAllocatedVM, gotRequest.getAllocatedVMs()[0]);
        assertEquals(AsyncRequestStatus.OPEN, gotRequest.getStatus());


        //Now Mutate the Map
        asyncRequestMap.addOrReplace(secondRequest);

        gotRequest.setStatus(AsyncRequestStatus.ACTIVE);
        asyncRequestMap.addOrReplace(gotRequest);

        AsyncRequest gotSecondRequest = asyncRequestMap.getByID(secondID);
        AsyncRequest updatedRequest = asyncRequestMap.getByID(testID);
        assertEquals(AsyncRequestStatus.ACTIVE, updatedRequest.getStatus());
        assertEquals(testAllocatedVM, updatedRequest.getAllocatedVMs()[0]);

        asyncRequestMap.addOrReplace(updatedRequest);
        AsyncRequest updatedRequest1 = asyncRequestMap.getByID(testID);
        assertEquals(testAllocatedVM, updatedRequest1.getAllocatedVMs()[0]);
        assertEquals(0, gotSecondRequest.getAllocatedVMs().length);
    }
}