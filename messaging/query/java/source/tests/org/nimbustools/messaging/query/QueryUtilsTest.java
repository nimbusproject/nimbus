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
package org.nimbustools.messaging.query;

import org.testng.annotations.Test;
import static org.testng.Assert.*;
import static org.nimbustools.messaging.query.QueryUtils.safeStringEquals;

public class QueryUtilsTest {
    @Test
    public void testSafeStringEquals() {

        final String str1 = "abc123";
        final String str2 = "abc124";

        final String str3 = "sandwiches!";

        assertFalse(safeStringEquals(null, str1));
        assertFalse(safeStringEquals(str1, null));
        assertTrue(safeStringEquals(null, null));

        assertTrue(safeStringEquals(str1, str1));
        assertFalse(safeStringEquals(str1, str2));
        assertFalse(safeStringEquals(str3, str1));
    }
}
