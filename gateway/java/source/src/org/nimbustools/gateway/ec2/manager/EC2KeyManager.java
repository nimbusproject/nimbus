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

import org.nimbustools.api.services.security.*;
import org.nimbustools.api.services.rm.OperationDisabledException;
import org.nimbustools.api.services.rm.DoesNotExistException;
import org.nimbustools.api.services.rm.AuthorizationException;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.gateway.ec2.EC2AccessManager;
import org.nimbustools.gateway.ec2.EC2AccessID;
import org.nimbustools.gateway.ec2.EC2AccessException;

import java.util.List;
import java.util.Collections;
import java.util.ArrayList;

import com.xerox.amazonws.ec2.Jec2;
import com.xerox.amazonws.ec2.EC2Exception;
import com.xerox.amazonws.ec2.KeyPairInfo;

public class EC2KeyManager
        implements KeyManager {

    private final EC2GatewayManager gatewayManager;
    private final EC2AccessManager accessManager;

    public EC2KeyManager(EC2GatewayManager gatewayManager,
                              EC2AccessManager accessManager) {
        if (gatewayManager == null) {
            throw new IllegalArgumentException("gatewayManager may not be null");
        }
        this.gatewayManager = gatewayManager;

        if (accessManager == null) {
            throw new IllegalArgumentException("accessManager may not be null");
        }
        this.accessManager = accessManager;

    }
    

    public void addKey(KeyPair key, Caller caller) throws OperationDisabledException {
        throw new OperationDisabledException();
    }

    public KeyPair generateKey(String name, Caller caller)
            throws KeyExistsException, AuthorizationException {

        Jec2 client = getClient(caller);
        try {

            final String hash = SecurityUtil.getCallerHash(caller);
            final String ec2KeyName = SecurityUtil.prefixKeyName(name, hash);

            final KeyPairInfo info = client.createKeyPair(ec2KeyName);

            return new KeyPair(name,
                    info.getKeyFingerprint(),
                    null,
                    info.getKeyMaterial());

        } catch (EC2Exception e) {
            throw new KeyExistsException(e);
        }
    }

    public void removeKey(String name, Caller caller)
            throws DoesNotExistException, AuthorizationException {
        Jec2 client = getClient(caller);
        try {

            final String hash = SecurityUtil.getCallerHash(caller);
            final String ec2KeyName = SecurityUtil.prefixKeyName(name, hash);

            client.deleteKeyPair(ec2KeyName);
        } catch (EC2Exception e) {
            throw new DoesNotExistException(e);
        }
    }

    public List<KeyPair> listKeys(Caller caller) throws AuthorizationException {

        final Jec2 client = getClient(caller);
        final List<KeyPairInfo> keypairs;
        try {
            keypairs = client.describeKeyPairs(Collections.<String>emptyList());
        } catch (EC2Exception e) {
            throw new AuthorizationException(e);
        }
        final String hash = SecurityUtil.getCallerHash(caller);

        ArrayList<KeyPair> list = null;
        for (KeyPairInfo key : keypairs) {

            String keyName = key.getKeyName();

            if (keyName != null && SecurityUtil.checkKeyName(keyName, hash)) {
                if (list == null) {
                    list = new ArrayList<KeyPair>();
                }
                final String trimmed = SecurityUtil.trimKeyName(keyName, hash);
                list.add(new KeyPair(trimmed, key.getKeyFingerprint(), null, null));
            }
        }
        if (list == null) {
            return Collections.emptyList();
        }
        return list;
    }

    private Jec2 getClient(Caller caller) throws AuthorizationException {
        final EC2AccessID accessID;
        try {
            accessID = accessManager.getAccessID(caller);
        } catch (EC2AccessException e) {
            throw new AuthorizationException("Caller has no EC2 credentials");
        }
        return gatewayManager.getClient(accessID);
    }
}
