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

import org.nimbustools.api.repr.Caller;
import org.nimbustools.gateway.ec2.creds.EC2AccessException;
import org.nimbustools.gateway.ec2.creds.EC2AccessID;
import org.nimbustools.gateway.ec2.creds.*;
import org.nimbustools.gateway.ec2.creds.EC2UserPair;
import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;
import org.springframework.core.io.Resource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.text.ParseException;

public class DefaultEC2AccessManager implements EC2AccessManager {

    private static final Log logger =
            LogFactory.getLog(DefaultEC2AccessManager.class.getName());


    private SessionFactory sessionFactory;
    private Resource credentialResource;


    private HashMap<String, EC2AccessID> accessIds;

    Map<String, EC2AccessID> getAccessIds() {
        return accessIds;
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public Resource getCredentialResource() {
        return credentialResource;
    }

    public void setCredentialResource(Resource credentialResource) {
        this.credentialResource = credentialResource;
    }

    public void initialize() throws Exception {
        if (this.sessionFactory == null) {
            throw new IllegalStateException("sessionFactory may not be null");
        }
        if (this.credentialResource == null) {
            throw new IllegalStateException("credentialResource may not be null");
        }

        this.accessIds = new HashMap<String, EC2AccessID>();

        final InputStream stream = credentialResource.getInputStream();
        final BufferedReader reader =
                new BufferedReader(new InputStreamReader(stream));

        try {
            String line;
            while ((line = reader.readLine()) != null) {
                final String[] pieces = line.trim().split("\\s+");
                if (pieces.length == 1 && pieces[0].length() == 0) {
                    continue;
                }

                if (pieces[0].startsWith("#")) {
                    continue;
                }

                if (pieces.length != 2) {
                    throw new ParseException("Invalid EC2 credential file format.", 0);
                }

                final EC2AccessID accessId =
                        new EC2AccessID(pieces[0], pieces[1]);
                if (this.accessIds.put(accessId.getKey(), accessId) != null) {
                    logger.warn("Parsed duplicate EC2 access ID: "+
                            accessId.getKey());
                } else {
                    logger.debug("Parsed EC2 access ID: " + accessId.getKey());
                }
            }
        } finally {
            stream.close();
        }
    }

    public EC2AccessID getAccessID(Caller caller) throws EC2AccessException {
        if (caller == null) {
            throw new IllegalArgumentException("caller may not be null");
        }
        final String identity = caller.getIdentity();
        if (identity == null) {
            throw new IllegalArgumentException("caller is invalid (has no identity");
        }

        final Session session = sessionFactory.getCurrentSession();
        final EC2UserPair userPair = (EC2UserPair) session.get(EC2UserPair.class,
                identity);

        if (userPair == null) {
            throw new EC2AccessException("User '"+ identity +
                    "' does not have an EC2 mapping");
        }

        return getAccessIDByKey(userPair.getAccessId());
    }

    public EC2AccessID getAccessIDByKey(String key) throws EC2AccessException {
        if (accessIds == null) {
            throw new EC2AccessException("Manager doesn't have any EC2 credentials");
        }
        final EC2AccessID accessId = accessIds.get(key);
        if (accessId == null) {
            throw new EC2AccessException(
                    "Manager doesn't have EC2 credential with ID: "+key);
        }
        return accessId;
    }

}
