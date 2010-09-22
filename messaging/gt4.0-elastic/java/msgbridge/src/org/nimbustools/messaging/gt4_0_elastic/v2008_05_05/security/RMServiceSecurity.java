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
package org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.security;

import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.ServiceSecurity;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.rm.ContainerInterface;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.service.UnimplementedOperations;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.*;
import org.nimbustools.api.brain.ModuleLocator;
import org.nimbustools.api.services.security.KeyManager;
import org.nimbustools.api.services.security.KeyExistsException;
import org.nimbustools.api.services.security.KeyPair;
import org.nimbustools.api.services.rm.AuthorizationException;
import org.nimbustools.api.services.rm.OperationDisabledException;
import org.nimbustools.api.services.rm.DoesNotExistException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.rmi.RemoteException;
import java.util.List;

/**
 * ServiceSecurity implementation that dispatches directly to RM API security manager
 */
public class RMServiceSecurity extends UnimplementedOperations
        implements ServiceSecurity {

    private static final Log logger =
            LogFactory.getLog(ServiceSecurityImpl.class.getName());

    private final ContainerInterface containerInterface;
    private final KeyManager keyManager;

    public RMServiceSecurity(ContainerInterface containerInterface, ModuleLocator moduleLocator) {
        if (containerInterface == null) {
            throw new IllegalArgumentException("containerInterface may not be null");
        }
        this.containerInterface = containerInterface;

        if (moduleLocator == null) {
            throw new IllegalArgumentException("moduleLocator may not be null");
        }

        this.keyManager = moduleLocator.getKeyManager();

        if (keyManager == null) {
            throw new IllegalArgumentException("moduleLocator must have a keyManager");
        }
    }

    public CreateKeyPairResponseType createKeyPair(CreateKeyPairType createKeyPairRequestMsg)
            throws RemoteException {

        final String keyName = createKeyPairRequestMsg.getKeyName();

        final KeyPair keyPair;
        try {
            keyPair = keyManager.generateKey(keyName, containerInterface.getCaller());

        } catch (AuthorizationException e) {
            logger.error("Error generating keypair",e);
            throw new RemoteException("You do not have authorization to generate a keypair");
        } catch (OperationDisabledException e) {
            logger.error("Error generating keypair",e);
            throw new RemoteException("Key generation is not supported");
        } catch (KeyExistsException e) {
            logger.error("Error generating keypair",e);
            throw new RemoteException("A keypair named '"+keyName+"' already exists");
        }

        return new CreateKeyPairResponseType(
                keyPair.getFingerprint(),
                keyPair.getPrivateKey(),
                keyName, ""); // TODO do something real with requestId
    }

    public DescribeKeyPairsResponseType describeKeyPairs(DescribeKeyPairsType describeKeyPairsRequestMsg)
            throws RemoteException {

        //TODO specific key describe. needs API modification

        final List<KeyPair> list;
        try {
            list = keyManager.listKeys(containerInterface.getCaller());
        } catch (AuthorizationException e) {
            logger.error("Error describing keypairs",e);
            throw new RemoteException("You do not have authorization to list keypairs");
        }

        DescribeKeyPairsResponseItemType[] items =
                new DescribeKeyPairsResponseItemType[list.size()];
        for (int i=0; i<list.size(); i++) {
            KeyPair keyPair = list.get(i);
            items[i] = new DescribeKeyPairsResponseItemType(
                    keyPair.getFingerprint(), keyPair.getKeyName());
        }

        DescribeKeyPairsResponseInfoType respInfo = new DescribeKeyPairsResponseInfoType(items);
        return new DescribeKeyPairsResponseType(respInfo, "");
        // TODO do something real with requestId
    }

    public DeleteKeyPairResponseType deleteKeyPair(DeleteKeyPairType deleteKeyPairRequestMsg)
            throws RemoteException {

        boolean success = true;
        try {
            keyManager.removeKey(
                deleteKeyPairRequestMsg.getKeyName(),
                    containerInterface.getCaller());
        } catch (AuthorizationException e) {
            logger.error("Error removing keypair",e);
            throw new RemoteException("You do not have authorization to remove keypairs");
        } catch (DoesNotExistException e) {
            logger.error("Error removing keypair",e);
            success = false;
        }
        return new DeleteKeyPairResponseType(success, "");
        // TODO do something real with requestId
    }
}
