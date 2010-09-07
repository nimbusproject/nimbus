/*
 * Copyright 1999-2009 University of Chicago
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
package org.nimbustools.ctxbroker.blackboard;

import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;
import static org.testng.Assert.*;
import static org.testng.Assert.assertEquals;
import org.nimbustools.ctxbroker.ContextBrokerException;
import org.nimbustools.ctxbroker.Identity;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.UUID;

public class BlackboardTest {

    final static String ID = "test-Blackboard";

    int nextWorkspaceID = 0;

    private synchronized Integer getWorkspaceID() {
        return nextWorkspaceID++;
    }
    private Identity getIdentity(int workspaceID) {
        return new Identity("publicnic",
                "192.168.0."+workspaceID,
                "id-"+workspaceID,
                "asdfghjk");
    }

    private static void assertDataPairs(List<DataPair> actual, List<DataPair> expected) {
        assertEquals(actual.size(), expected.size());
        for (DataPair expectedPair : expected) {
            boolean found = false;
            for (DataPair actualPair : actual) {
                if (expectedPair.getName().equals(actualPair.getName()) &&
                        expectedPair.getValue().equals(expectedPair.getValue())) {
                    found = true;
                    break;
                }
            }
            assertTrue(found, "Data pair: "+expectedPair.toString() +
                    " was not found in actual data pair list");
        }
    }

    private static void assertIdentitiesEqual(Identity actual, Identity expected) {
        assertNotNull(actual);
        assertNotNull(expected);

        assertEquals(actual.getIface(), expected.getIface());
        assertEquals(actual.getIp(), expected.getIp());
        assertEquals(actual.getPubkey(), expected.getPubkey());
        assertEquals(actual.getHostname(), expected.getHostname());
    }


    @Test
    public void testNodeCountChange() throws ContextBrokerException {
        Blackboard bb = new Blackboard(ID);

        Integer workspaceId = getWorkspaceID();
        Identity[] ids = new Identity[] {getIdentity(workspaceId)};

        bb.addWorkspace(workspaceId, ids, true, null, null, null, 3);

        boolean failed = false;
        try {
            bb.addWorkspace(workspaceId, ids, true, null, null, null, 4);
        } catch (ContextBrokerException e) {
            failed = true;
        }

        assertTrue(failed, "Mismatched node counts did not cause failure");
    }

    @Test
    public void testTooManyNodes() {
        Blackboard bb = new Blackboard(ID);

        final int nodeCount = 3;

        boolean failed = false;
        // we try to add one more node than we advertise
        for (int i=0; i<=nodeCount; i++) {

            Integer workspaceId = getWorkspaceID();
            Identity[] ids = new Identity[] {getIdentity(workspaceId)};
            try {
                bb.addWorkspace(workspaceId, ids, true, null,
                        null, null, nodeCount);
            } catch (ContextBrokerException e) {
                assertEquals(i, nodeCount, "addWorkspace() failed on an " +
                        "earlier node than expected");
                failed = true;
            }
        }
        assertTrue(failed, "Adding one too many nodes did not cause failure");
    }

    @Test
    public void testDuplicateNodeId() throws ContextBrokerException {
        Blackboard bb = new Blackboard(ID);
        Integer workspaceId = getWorkspaceID();
        Identity[] ids = new Identity[] {getIdentity(workspaceId)};

        bb.addWorkspace(workspaceId, ids, true, null, null, null, 3);

        boolean failed = false;
        try {
            bb.addWorkspace(workspaceId, ids, true, null, null, null, 3);
        } catch (ContextBrokerException e) {
            failed = true;
        }
        assertTrue(failed, "Adding node with duplicate ID did not cause failure");

    }



    @DataProvider(name="allofthebools")
    private Object[][] generateBooleans() {
        // seems a little silly
        return new Object[][] {
                new Object[] {true},
                new Object[] {false}
        };
    }

    @Test(dataProvider = "allofthebools")
    public void testSingleNode(boolean reportOk) throws ContextBrokerException {
        Blackboard bb = new Blackboard(ID);
        Integer workspaceId = getWorkspaceID();
        Identity[] ids = new Identity[] {getIdentity(workspaceId)};

        DataPair[] data = new DataPair[] {
                new DataPair("foo", "bar"),
                new DataPair("billy", "anyteen")
        };

        CtxStatus ctxStatus = bb.getStatus();
        assertFalse(ctxStatus.isAllOk());
        assertFalse(ctxStatus.isErrorOccurred());
        assertFalse(ctxStatus.isComplete());

        bb.addWorkspace(workspaceId, ids, true, null, data, null, 1);

        final NodeManifest nodeManifest = bb.retrieve(workspaceId);
        assertDataPairs(nodeManifest.getData(), Arrays.asList(data));
        assertEquals(nodeManifest.getRequiredRoles().size(), 0);
        assertEquals(nodeManifest.getIdentities().size(), 1);


        // node has retrieved its data but not reported in yet
        ctxStatus = bb.getStatus();
        assertFalse(ctxStatus.isAllOk());
        assertFalse(ctxStatus.isErrorOccurred());
        assertTrue(ctxStatus.isComplete());


        if (reportOk) {
            bb.okExit(workspaceId);

            ctxStatus = bb.getStatus();
            assertTrue(ctxStatus.isAllOk());
            assertFalse(ctxStatus.isErrorOccurred());
            assertTrue(ctxStatus.isComplete());

            final List<NodeStatus> list = bb.identities(true, null, null);
            assertEquals(list.size(), 1);
            final NodeStatus nodeStatus = list.get(0);

            assertEquals(nodeStatus.getIdentities().size(), 1);
            assertIdentitiesEqual(nodeStatus.getIdentities().get(0), ids[0]);

            assertEquals(nodeStatus.getErrorCode(),0);
            assertNull(nodeStatus.getErrorMessage());
            assertFalse(nodeStatus.isErrorOccurred());
            assertTrue(nodeStatus.isOkOccurred());
        } else {

            final short exitCode = 1;
            final String errorMsg = "this isn't really an error";
            bb.errorExit(workspaceId, exitCode, errorMsg);

            ctxStatus = bb.getStatus();
            assertFalse(ctxStatus.isAllOk());
            assertTrue(ctxStatus.isErrorOccurred());
            assertTrue(ctxStatus.isComplete());

            final List<NodeStatus> list = bb.identities(true, null, null);
            assertEquals(list.size(), 1);
            final NodeStatus nodeStatus = list.get(0);

            assertEquals(nodeStatus.getIdentities().size(), 1);
            assertIdentitiesEqual(nodeStatus.getIdentities().get(0), ids[0]);

            assertEquals(nodeStatus.getErrorCode(), exitCode);
            assertEquals(nodeStatus.getErrorMessage(), errorMsg);
            assertTrue(nodeStatus.isErrorOccurred());
            assertFalse(nodeStatus.isOkOccurred());
        }


    }

    @Test(dataProvider = "allofthebools")
    public void testMasterWorker(boolean requireAllIdentities)
            throws ContextBrokerException {

        // one-way dependency: a master role that depends on nothing but is required by multiple workers

        // requireAllIdentities affects whether nodes want information for every node, or just
        // those that they depend on. This affects two things:
        //      * Whether nodes can be retrieve()d once they have dependencies satisfied but not all
        //        nodes have checked in
        //      * How many Identities nodes get back from retrieve()


        Blackboard bb = new Blackboard(ID);

        final String masterRole ="mastermaster";
        final int workerCount = 2;
        final int nodeCount = workerCount+1;

        final Integer masterId = getWorkspaceID();
        final Identity[] masterIdentities = new Identity[] { getIdentity(masterId)};
        final ProvidedRoleDescription[] masterProvidedRoles = new ProvidedRoleDescription[] {
                new ProvidedRoleDescription(masterRole, null)
        };
        final RequiredRole[] masterRequiredRoles = null;

        bb.addWorkspace(masterId, masterIdentities, requireAllIdentities,
                masterRequiredRoles, null, masterProvidedRoles, nodeCount);

        List<Integer> workerIds = new ArrayList<Integer>(workerCount);
        for (int i=0; i< workerCount; i++) {

            // context should not be complete and retrieve should fail until workers check in
            assertFalse(bb.isComplete());

            if (requireAllIdentities) {
                assertTrue(bb.retrieve(masterId) == null);
                for (Integer id : workerIds) {
                    assertTrue(bb.retrieve(id) == null);
                }
            } else {
                assertTrue(bb.retrieve(masterId) != null);
                for (Integer id : workerIds) {
                    assertTrue(bb.retrieve(id) != null);
                }
            }

            final Integer workerId = getWorkspaceID();
            workerIds.add(workerId);
            final Identity[] workerIdentities = new Identity[] { getIdentity(workerId)};
            final ProvidedRoleDescription[] workerProvidedRoles = null;
            final RequiredRole[] workerRequiredRoles = new RequiredRole[] {
                    new RequiredRole(masterRole, true, true)
            };

            bb.addWorkspace(workerId, workerIdentities, requireAllIdentities,
                    workerRequiredRoles, null, workerProvidedRoles, nodeCount);
        }

        // okay now everyone is checked in.
        assertTrue(bb.isComplete());
        for (Integer workerId : workerIds) {
            final NodeManifest man = bb.retrieve(workerId);

            assertTrue(man.getData().isEmpty());
            assertEquals(man.getRequiredRoles().size(), 1);
            RoleIdentityPair rolePair = man.getRequiredRoles().get(0);
            assertIdentitiesEqual(rolePair.getIdentity(), masterIdentities[0]);
            assertEquals(rolePair.getRole(), masterRole);

            if (requireAllIdentities) {
                assertEquals(man.getIdentities().size(), nodeCount);
            } else {
                // just the master
                assertEquals(man.getIdentities().size(), 1);
                assertIdentitiesEqual(man.getIdentities().get(0), masterIdentities[0]);
            }
        }

        final NodeManifest masterManifest = bb.retrieve(masterId);
        assertTrue(masterManifest.getData().isEmpty());
        assertTrue(masterManifest.getRequiredRoles().isEmpty());

        // if we don't require all identities, master will get 0 identities
        assertEquals(masterManifest.getIdentities().size(),
                requireAllIdentities ? nodeCount : 0);
    }

    @Test
    public void testFindIdentities() throws ContextBrokerException {

        final Blackboard bb = new Blackboard(ID);
        final int nodeCount = 3;

        Identity[] ids = new Identity[nodeCount];
        for (int i=0; i< nodeCount; i++) {
            Integer workspaceId = getWorkspaceID();
            ids[i] = getIdentity(workspaceId);

            bb.addWorkspace(workspaceId, new Identity[] {ids[i]},
                    true, null, null, null, nodeCount);
        }

        // find all identities
        final List<NodeStatus> allNodes = bb.identities(true, null, null);
        assertEquals(allNodes.size(), nodeCount);

        // find by IP
        for (Identity id : ids) {
            final List<NodeStatus> list = bb.identities(false, null, id.getIp());
            assertEquals(list.size(), 1);
            final List<Identity> nodeIdentities = list.get(0).getIdentities();
            assertEquals(list.size(), 1);
            assertIdentitiesEqual(nodeIdentities.get(0), id);
        }

        // and a nonexistent IP
        assertEquals(bb.identities(false, null, "10.10.10.10").size(), 0);


        // find by hostname
        for (Identity id : ids) {
            final List<NodeStatus> list = bb.identities(false, id.getHostname(), null);
            assertEquals(list.size(), 1);
            final List<Identity> nodeIdentities = list.get(0).getIdentities();
            assertEquals(list.size(), 1);
            assertIdentitiesEqual(nodeIdentities.get(0), id);
        }

        // and a nonexistent hostname
        assertEquals(bb.identities(false, "google.com", null).size(), 0);

    }

    @Test
    public void testInjectData() throws ContextBrokerException {
        final Blackboard bb = new Blackboard(ID);

        final String dataName = "OMGDATA";
        final String[] dataValues = new String[] {"VALUE1", "VALUE2"};
        

        // add a node that requires a data but provides no value

        DataPair dataNoValue = new DataPair(dataName);
        DataPair dataWithValue = new DataPair(dataName, dataValues[0]);


        Integer workspaceNoValueId = getWorkspaceID();
        bb.addWorkspace(workspaceNoValueId, new Identity[] {getIdentity(workspaceNoValueId)},
                true, null, new DataPair[] {dataNoValue}, null, 2);

        Integer workspaceWithValueId = getWorkspaceID();
        bb.addWorkspace(workspaceWithValueId, new Identity[] {getIdentity(workspaceWithValueId)},
                true, null, new DataPair[] {dataWithValue}, null, 2);

        bb.injectData(dataName, dataValues[1]);

        NodeManifest node1 = bb.retrieve(workspaceNoValueId);
        assertNotNull(node1);
        final List<DataPair> node1Data = node1.getData();
        assertEquals(node1Data.size(), dataValues.length);
        for (DataPair dp : node1Data) {
            assertEquals(dp.getName(), dataName);
            final String value = dp.getValue();
            assertNotNull(value);

            int matchCount = 0;
            for (String dataValue : dataValues) {
                if (value.equals(dataValue)) {
                    matchCount++;
                }
            }
            assertEquals(matchCount, 1);
        }

        NodeManifest node2 = bb.retrieve(workspaceWithValueId);
        assertNotNull(node2);
        final List<DataPair> node2Data = node2.getData();
        assertEquals(node2Data.size(), dataValues.length);
        for (DataPair dp : node2Data) {
            assertEquals(dp.getName(), dataName);
            final String value = dp.getValue();
            assertNotNull(value);

            int matchCount = 0;
            for (String dataValue : dataValues) {
                if (value.equals(dataValue)) {
                    matchCount++;
                }
            }
            assertEquals(matchCount, 1);
        }

    }

    @Test
    public void testStaticBlackboardFactory() {
        final String id = UUID.randomUUID().toString();

        final Blackboard blackboard = Blackboard.createOrGetBlackboard(id);

        assertSame(Blackboard.createOrGetBlackboard(id), blackboard);

        final String anotherId = UUID.randomUUID().toString();
        assertNotSame(Blackboard.createOrGetBlackboard(anotherId), blackboard);
    }

}
