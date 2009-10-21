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
package org.nimbustools.gateway.ec2.creds.defaults;

import org.testng.annotations.Test;
import static org.testng.Assert.*;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.nimbustools.api.defaults.repr.DefaultCaller;
import org.nimbustools.gateway.ec2.creds.EC2AccessException;
import org.nimbustools.gateway.ec2.creds.EC2AccessID;
import org.nimbustools.gateway.ec2.creds.EC2UserPair;

import java.util.Map;
import java.io.ByteArrayInputStream;

public class DefaultEC2AccessManagerTest {
    private final String key1 = "accessID1";
    private final String secret1 = "secret1";
    private final String key2 = "accessID2";
    private final String secret2 = "secret2";

    private final String creds = "# this is a comment\n"+
            " # so is this\n"+
            key1 +" "+secret1+"\n"+
            "  \n"+
            "#another comment\n"+
            "\n"+
            " "+key2+"\t"+secret2+"  \n";

    @Test
    public void testInitialize() throws Exception {

        DefaultEC2AccessManager manager = new DefaultEC2AccessManager();
        manager.setSessionFactory(mock(SessionFactory.class));


        manager.setCredentialResource(stringAsResource(creds));
        manager.initialize();

        final Map<String,EC2AccessID> accessIds = manager.getAccessIds();
        assertEquals(accessIds.size(), 2);

        final EC2AccessID id1 = accessIds.get(key1);
        assertEquals(id1.getKey(), key1);
        assertEquals(id1.getSecret(), secret1);

        final EC2AccessID id2 = accessIds.get(key2);
        assertEquals(id2.getKey(), key2);
        assertEquals(id2.getSecret(), secret2);


        // okay now stick some bad data on the end of the "file"
        String badcreds = creds + "this isn't even properly formatted\n";

        manager = new DefaultEC2AccessManager();
        manager.setSessionFactory(mock(SessionFactory.class));
        manager.setCredentialResource(stringAsResource(badcreds));

        boolean failed = false;
        try {
        manager.initialize();
        } catch (Exception e) {
            failed = true;
        }
        assertTrue(failed);
    }

    private static Resource stringAsResource(String str) {
        ByteArrayInputStream s = new ByteArrayInputStream(str.getBytes());
        return new InputStreamResource(s);
    }

    @Test
    public void testGetAccessID() throws Exception {

        final String dn = "Steve Jobs";
        final String unknownDn = "Fake Steve Jobs";

        final DefaultCaller caller = new DefaultCaller();
        caller.setIdentity(dn);

        final DefaultCaller unknownCaller = new DefaultCaller();
        unknownCaller.setIdentity(unknownDn);

        EC2UserPair userPair = new EC2UserPair();
        userPair.setDn(dn);
        userPair.setAccessId(key1);

        final Session session = mock(Session.class);
        when(session.get(EC2UserPair.class, dn)).
                thenReturn(userPair);
        when(session.get(EC2UserPair.class, unknownDn)).
                thenReturn(null);

        final SessionFactory sessionFactory = mock(SessionFactory.class);
        when(sessionFactory.getCurrentSession()).thenReturn(session);

        DefaultEC2AccessManager manager = new DefaultEC2AccessManager();
        manager.setCredentialResource(stringAsResource(creds));
        manager.setSessionFactory(sessionFactory);
        manager.initialize();

        final EC2AccessID accessID = manager.getAccessID(caller);
        assertEquals(accessID.getKey(), key1);
        assertEquals(accessID.getSecret(), secret1);

        boolean failed = false;
        try {
            manager.getAccessID(unknownCaller);
        } catch (EC2AccessException e) {
            failed = true;
        }
        assertTrue(failed);
    }
}
