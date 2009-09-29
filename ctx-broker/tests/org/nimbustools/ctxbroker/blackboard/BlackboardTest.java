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



}
