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
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNotSame;
import static org.testng.AssertJUnit.assertTrue;

import org.globus.workspace.testing.NimbusTestBase;
import org.globus.workspace.testing.NimbusTestContextLoader;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.CreateRequest;
import org.nimbustools.api.repr.CreateResult;
import org.nimbustools.api.repr.vm.State;
import org.nimbustools.api.repr.vm.VM;
import org.nimbustools.api.services.rm.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.Test;

import java.util.UUID;

@ContextConfiguration(
        locations={"file:./service/service/java/tests/suites/basic/home/services/etc/nimbus/workspace-service/other/main.xml"},
        loader=NimbusTestContextLoader.class)
public class IdempotentCreationSuite extends NimbusTestBase{

    // -----------------------------------------------------------------------------------------
    // extends NimbusTestBase
    // -----------------------------------------------------------------------------------------

    @AfterSuite(alwaysRun=true)
    @Override
    public void suiteTeardown() throws Exception {
        super.suiteTeardown();
    }

    @Override
    protected String getNimbusHome() throws Exception {
        return this.determineSuitesPath() + "/basic/home";
    }


    @Test
    @DirtiesContext
    public void testBasicIdempotency() throws Exception {
        final Manager rm = this.locator.getManager();

        String token = UUID.randomUUID().toString();

        final Caller caller = this.populator().getCaller();


        // 1. first start a basic single VM with a clientToken

        final CreateRequest request1 = this.populator().getIdempotentCreateRequest("suite:basic:idempotency", token);
        final CreateResult result1 = rm.create(request1, caller);

        logger.info("Leased vm '" + result1.getVMs()[0].getID() + '\'');
        assertEquals(token, result1.getVMs()[0].getClientToken());


        // 2. now re-launch with the same clientToken, should get back the same instance
        //    (and no new instance should have been started)

        final CreateRequest request2 = this.populator().getIdempotentCreateRequest("suite:basic:idempotency", token);
        final CreateResult result2 = rm.create(request2, caller);

        assertEquals(result1.getVMs()[0].getID(), result2.getVMs()[0].getID());
        assertEquals(token, result2.getVMs()[0].getClientToken());
        logger.info("Leased same vm '" + result2.getVMs()[0].getID() + '\'');

        assertEquals(1, rm.getAllByCaller(caller).length);


        // 3. Now launch a VM with the same parameters but different clientToken --
        //    should get a new VM

        String anotherToken = UUID.randomUUID().toString();
        final CreateRequest request3 = this.populator().getIdempotentCreateRequest("suite:basic:idempotency", anotherToken);
        final CreateResult result3 = rm.create(request3, caller);
        logger.info("Leased vm '" + result3.getVMs()[0].getID() + '\'');
        assertNotSame(result1.getVMs()[0].getID(), result3.getVMs()[0].getID());
        assertEquals(2, rm.getAllByCaller(caller).length);


        // 4. Kill the first VM and then attempt to re-launch with its same clientToken.
        //    We should get back a terminated state response with the original ID

        rm.trash(result1.getVMs()[0].getID(), Manager.INSTANCE, caller);
        assertEquals(1, rm.getAllByCaller(caller).length);

        final CreateRequest request4 = this.populator().getIdempotentCreateRequest("suite:basic:idempotency", token);
        final CreateResult result4 = rm.create(request4, caller);
        logger.info("Leased same vm (terminated) '" + result4.getVMs()[0].getID() + '\'');
        assertEquals(result1.getVMs()[0].getID(), result2.getVMs()[0].getID());
        assertEquals(State.STATE_Cancelled, result4.getVMs()[0].getState().getState());
        assertEquals(token, result4.getVMs()[0].getClientToken());


        // 5. Now attempt to launch requests with the same clientTokens but
        //    different other parameters. Should get IdempotentCreationMismatchException
        //    errors


        // different node count
        assertIdempotentMismatch(rm,
                caller,
                this.populator().
                getCreateRequest("suite:basic:idempotency", 240, 64, 5, anotherToken)
        );

        // different name
        assertIdempotentMismatch(rm,
                caller,
                this.populator().
                getCreateRequest("suite:basic:idempotency:different", 240, 64, 1, anotherToken)
        );

        // different duration
        assertIdempotentMismatch(rm,
                caller,
                this.populator().
                getCreateRequest("suite:basic:idempotency", 20, 64, 1, anotherToken)
        );

        // different memory
        assertIdempotentMismatch(rm,
                caller,
                this.populator().
                getCreateRequest("suite:basic:idempotency", 240, 128, 1, anotherToken)
        );


        // 6. Do the same thing for the terminated idempotent request.
        //    We can't test as many parameters here (memory, duration) because
        //    not all of that info is persisted after termination.

        // different node count
        assertIdempotentMismatch(rm,
                caller,
                this.populator().
                getCreateRequest("suite:basic:idempotency", 240, 64, 5, token)
        );

        // different name
        assertIdempotentMismatch(rm,
                caller,
                this.populator().
                getCreateRequest("suite:basic:idempotency:different", 240, 64, 1, token)
        );


        Thread.sleep(200L);
    }

    private void assertIdempotentMismatch(Manager rm,
                                                 Caller caller,
                                                 CreateRequest request) throws Exception {
        boolean gotException = false;
        try {
            rm.create(request, caller);
        } catch (IdempotentCreationMismatchException e) {
            logger.info("Got expected mismatch error: "+ e.getMessage());
            gotException = true;
        }
        assertTrue("Expected "+IdempotentCreationMismatchException.class.getName() +
                " exception", gotException);
    }


}
