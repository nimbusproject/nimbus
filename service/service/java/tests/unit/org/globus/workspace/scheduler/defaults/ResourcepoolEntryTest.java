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

package org.globus.workspace.scheduler.defaults;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import org.testng.annotations.Test;

public class ResourcepoolEntryTest {

    @Test
    public void testPercentEmpty() {
        ResourcepoolEntry re = new ResourcepoolEntry("aResourcePool", "ahostname", 4096, 2048, 0, "*", true);
        assertEquals(re.percentEmpty(), 50);
        re = new ResourcepoolEntry("aResourcePool", "ahostname", 4096, 1024, 0, "*", true);
        assertEquals(re.percentEmpty(), 25);
        re = new ResourcepoolEntry("aResourcePool", "ahostname", 4096, 0, 0, "*", true);
        assertEquals(re.percentEmpty(), 0);
        re = new ResourcepoolEntry("aResourcePool", "ahostname", 4096, 1, 0, "*", true);
        if (re.percentEmpty() == 0) {
            fail();
        }
        re = new ResourcepoolEntry("aResourcePool", "ahostname", 4096, 4096, 0,  "*", true);
        assertEquals(re.percentEmpty(), 100);
    }

    @Test(expectedExceptions=IllegalStateException.class)
    public void testPercentEmptyIllegal() {
        ResourcepoolEntry re = new ResourcepoolEntry("aResourcePool", "ahostname", 4096, 4097, 0, "*", true);
        re.percentEmpty();
    }
}
