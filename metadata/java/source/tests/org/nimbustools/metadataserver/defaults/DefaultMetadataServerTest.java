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
package org.nimbustools.metadataserver.defaults;

import net.sf.ehcache.CacheManager;
import org.nimbustools.api._repr.vm._NIC;
import org.nimbustools.api.defaults.repr.vm.DefaultNIC;
import org.nimbustools.api.repr.vm.NIC;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Properties;

import static org.nimbustools.metadataserver.defaults.DefaultMetadataServer.CONTACT_SOCKET_PREFIX;
import static org.testng.Assert.*;

public class DefaultMetadataServerTest {
    private DefaultMetadataServer server;

    @BeforeMethod
    public void setup() throws Exception {
        if (this.server != null && this.server.isListening()) {
            this.server.stop();
            this.server = null;
        }
        final CacheManager cacheManager = 
            CacheManager.create(getClass().getResource("default-ehcache.xml"));
        this.server = new DefaultMetadataServer(cacheManager);
        this.server.setCustomizationPath("/some/fake/path");
        this.server.setEnabled(true);
    }

    @Test
    public void testDefaultOnly() throws Exception {

        Properties props = new Properties();
        props.setProperty(CONTACT_SOCKET_PREFIX, "127.0.0.1:5555");
        this.server.setProperties(props);
        this.server.initServerAndListen();

        assertContactURL(getNICs("public"), "127.0.0.1:5555");
    }

    @Test
    public void test0000Only() throws Exception {

        Properties props = new Properties();
        props.setProperty(CONTACT_SOCKET_PREFIX, "0.0.0.0:5555");
        this.server.setProperties(props);

        boolean failed = false;
        try {
        this.server.initialize();
        } catch (Exception e) {
            failed = true;
        }
        assertTrue(failed);
    }

    @Test
    public void testMultipleBindings() throws Exception {

        Properties props = new Properties();
        props.setProperty(CONTACT_SOCKET_PREFIX+".public", "127.0.0.1:5555");
        props.setProperty(CONTACT_SOCKET_PREFIX+".private", "127.0.0.1:5556");
        this.server.setProperties(props);
        this.server.initServerAndListen();

        assertContactURL(getNICs("public"), "127.0.0.1:5555");
        assertContactURL(getNICs("private"), "127.0.0.1:5556");

        assertContactURL(getNICs("public", "private"), "127.0.0.1:5555");
        assertContactURL(getNICs("private", "public"), "127.0.0.1:5556");

    }
    
    @Test
    public void testMultipleBindingsAndDefault() throws Exception {

        Properties props = new Properties();
        props.setProperty(CONTACT_SOCKET_PREFIX+".public", "127.0.0.1:5555");
        props.setProperty(CONTACT_SOCKET_PREFIX+".private", "127.0.0.1:5556");
        props.setProperty(CONTACT_SOCKET_PREFIX, "127.0.0.1:5557");
        this.server.setProperties(props);
        this.server.initServerAndListen();

        assertContactURL(getNICs("public"), "127.0.0.1:5555");
        assertContactURL(getNICs("private"), "127.0.0.1:5556");

        assertContactURL(getNICs("public", "private"), "127.0.0.1:5555");
        assertContactURL(getNICs("private", "public"), "127.0.0.1:5556");

        assertContactURL(getNICs("sandwiches"), "127.0.0.1:5557");
        assertContactURL(getNICs("sandwiches", "public"), "127.0.0.1:5555");
    }



    private void assertContactURL(NIC[] nics, String expected) {
        if (expected != null && !expected.startsWith("http://")) {
            expected = "http://" + expected;
        }

        assertEquals(this.server.getContactURL(nics), expected);
    }

    private NIC[] getNICs(String... networks) {
        NIC[] nics = new NIC[networks.length];

        for (int i = 0; i < networks.length; i++) {
            String network = networks[i];

            _NIC nic = new DefaultNIC();
            nic.setNetworkName(network);
            nics[i] = nic;
        }
        return nics;
    }
}
