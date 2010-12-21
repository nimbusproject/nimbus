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
import org.globus.workspace.testing.NimbusTestContextLoader;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.CreateRequest;
import org.nimbustools.api.repr.CreateResult;
import org.nimbustools.api.services.rm.Manager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.Test;

import java.util.UUID;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotSame;


/**
 * Runs idempotency tests in parallel to verify, uh, idempotency
 */
@ContextConfiguration(
        locations={"file:./service/service/java/tests/suites/basic/home/services/etc/nimbus/workspace-service/other/main.xml"},
        loader=NimbusTestContextLoader.class)
@DirtiesContext
public class ParallelIdempotentCreationSuite extends NimbusTestBase{

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


    final String parallelIdempotencyTestToken = UUID.randomUUID().toString();
    @Test(threadPoolSize = 10, invocationCount = 10, timeOut = 10000)
    public void parallelIdempotencyTest() throws Exception {

        final Manager rm = this.locator.getManager();

        final Caller caller = this.populator().getCaller();
        final CreateRequest request1 =
                this.populator().getIdempotentCreateRequest("suite:basic:idempotency",
                        parallelIdempotencyTestToken);
        final CreateResult result1 = rm.create(request1, caller);

        logger.info("Leased vm '" + result1.getVMs()[0].getID() + '\'');
        assertEquals(parallelIdempotencyTestToken, result1.getVMs()[0].getClientToken());
        assertEquals("1", result1.getVMs()[0].getID());
    }
}
