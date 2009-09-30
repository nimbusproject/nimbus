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
package org.nimbustools.gateway.ec2.manager;

import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;
import static org.testng.Assert.*;

import static org.nimbustools.gateway.ec2.manager.SecurityUtil.*;
import org.nimbustools.api.defaults.repr.DefaultCaller;

public class SecurityUtilTest {
    // so conceited
    private final String dn = "/O=Grid/OU=GlobusTest/OU=simple-workspace-ca/OU=uchicago.edu/CN=David LaBissoniere";
    private final String hash = "21833c12";

    @Test
    public void testGetCallerHash() {
        DefaultCaller caller = new DefaultCaller();
        caller.setIdentity(dn);

        assertEquals(getCallerHash(caller), hash);
    }

    @DataProvider(name = "checkKeyNameArgs")
    private Object[][] dataCheckKeyName() {
        return new Object[][] {
                new Object[] {hash+"-"+"fooo", true},
                new Object[] {hash+"-", false},
                new Object[] {hash, false},
                new Object[] {"fooo", false}
        };
    }

    @Test(dataProvider = "checkKeyNameArgs")
    public void testCheckKeyName(String keyName, boolean match) {
        assertEquals(checkKeyName(keyName, hash), match);
    }

    @Test
    public void testPrefixKeyName() {
        assertEquals(prefixKeyName("foo",hash), hash+"-foo");
    }

    @Test
    public void testTrimKeyName() {
        assertEquals(trimKeyName(hash+"-foo",hash), "foo");
    }
}
