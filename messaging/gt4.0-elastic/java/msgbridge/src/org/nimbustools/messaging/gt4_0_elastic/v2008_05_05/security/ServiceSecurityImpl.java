/*
 * Copyright 1999-2008 University of Chicago
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.util.Base64;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.CreateKeyPairResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.CreateKeyPairType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.DeleteKeyPairResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.DeleteKeyPairType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.DescribeKeyPairsInfoType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.DescribeKeyPairsItemType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.DescribeKeyPairsResponseInfoType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.DescribeKeyPairsResponseItemType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.DescribeKeyPairsResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.DescribeKeyPairsType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.ImportKeyPairResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.ImportKeyPairType;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.ServiceSecurity;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.rm.ContainerInterface;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.service.UnimplementedOperations;
import org.nimbustools.messaging.gt4_0_elastic.DisabledException;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.CannotTranslateException;

import java.rmi.RemoteException;
import java.util.List;
import java.util.LinkedList;

/**
 * extends UnimplementedOperations to make sure the unimplemented operations of
 * the ServiceSecurity interface are covered by some implementation.
 */
public class ServiceSecurityImpl extends UnimplementedOperations
                                 implements ServiceSecurity {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(ServiceSecurityImpl.class.getName());
    private static final String FAKE_FINGERPRINT = "N0:KE:YF:IN:GE:RP:RI:NT";


    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected final SSHKeys sshKeys;
    protected final KeyGen keyGen;
    protected final ContainerInterface container;


    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    public ServiceSecurityImpl(SSHKeys sshKeysImpl,
                               KeyGen keyGenImpl,
                               ContainerInterface containerImpl) {
        if (sshKeysImpl == null) {
            throw new IllegalArgumentException("sshKeysImpl may not be null");
        }
        this.sshKeys = sshKeysImpl;

        if (keyGenImpl == null) {
            throw new IllegalArgumentException("keyGenImpl may not be null");
        }
        this.keyGen = keyGenImpl;

        if (containerImpl == null) {
            throw new IllegalArgumentException("containerImpl may not be null");
        }
        this.container = containerImpl;
    }

    
    // -------------------------------------------------------------------------
    // *PARTIALLY* implements ServiceSecurity
    // -------------------------------------------------------------------------

    public CreateKeyPairResponseType createKeyPair(CreateKeyPairType req)
            throws RemoteException {

        // no use proceeding if these calls fail:
        final Caller caller = this.container.getCaller();
        final String ownerID;
        try {
            ownerID = this.container.getOwnerID(caller);
        } catch (CannotTranslateException e) {
            throw new RemoteException(e.getMessage(), e);
        }

        if (req == null) {
            throw new RemoteException("createKeyPair request is missing");
        }

        final String input = req.getKeyName();
        if (input == null) {
            throw new RemoteException(
                    "createKeyPair request does not contain key name");
        }

        final String splitToken;
        try {

            splitToken = this.sshKeys.getSplitToken();
            
        } catch (DisabledException e) {


            /* If split token method is disabled that means the standard
               "create keypair serverside" method is enabled.  Input field
               is the requested key name. */
            try {
                // *** EARLY RETURN ***
                return this.keyGen.createNewKeyPair(caller, input.trim());
            } catch (DisabledException e1) {
                throw new RuntimeException(
                        "Both SSH key implementations are disabled?");
            } catch (KeyGenException e1) {
                throw new RemoteException(e1.getMessage(), e1);
            }
        }

        // this is like an 'else' clause, see createNewKeyPair() call
        return this.splitMethod(splitToken, input, ownerID);
    }

    public ImportKeyPairResponseType importKeyPair(ImportKeyPairType req)
            throws RemoteException {

        // no use proceeding if these calls fail:
        final Caller caller = this.container.getCaller();
        final String ownerID;
        try {
            ownerID = this.container.getOwnerID(caller);
        } catch (CannotTranslateException e) {
            throw new RemoteException(e.getMessage(), e);
        }

        if (req == null) {
            throw new RemoteException("key name is missing");
        }

        final String keyName = req.getKeyName();
        if (keyName == null) {
            throw new RemoteException(
                    "createKeyPair request does not contain key name");
        }

        final String publicKeyMaterial = req.getPublicKeyMaterial();
        if (publicKeyMaterial == null) {
            throw new RemoteException("key material is missing");
        }
        if (!Base64.isBase64(publicKeyMaterial)) {
            throw new RemoteException("key material does not appear to " +
                    "be base64 encoded?");
        }
        final byte[] bytes = Base64.decode(publicKeyMaterial.getBytes());
        final String keyMaterial = new String(bytes);

        this.sshKeys.newKey(ownerID, keyName, keyMaterial, FAKE_FINGERPRINT);

        final ImportKeyPairResponseType resp =
                new ImportKeyPairResponseType(FAKE_FINGERPRINT, keyName, null);

        logger.info("SSH key registered, name='" + keyName +
                "', owner ID='" + ownerID + "'");

        return resp;
    }

    public DescribeKeyPairsResponseType describeKeyPairs(
                                                    DescribeKeyPairsType req)
            throws RemoteException {

        // no use proceeding if these calls fail:
        final Caller caller = this.container.getCaller();
        final String ownerID;
        try {
            ownerID = this.container.getOwnerID(caller);
        } catch (CannotTranslateException e) {
            throw new RemoteException(e.getMessage(), e);
        }

        if (req == null) {
            throw new RemoteException("describeKeyPairs request is missing");
        }

        final DescribeKeyPairsInfoType pairsInfoType = req.getKeySet();
        final DescribeKeyPairsItemType[] keyPairsItemTypes =
                                                pairsInfoType.getItem();

        final String[] filterQuery;
        if (keyPairsItemTypes == null || keyPairsItemTypes.length == 0) {
            filterQuery = null;
        } else {
            filterQuery = new String[keyPairsItemTypes.length];
            for (int i = 0; i < keyPairsItemTypes.length; i++) {
                if (keyPairsItemTypes[i] == null) {
                    throw new RemoteException(
                            "describeKeyPairs request is invalid, contains empty element?");
                }
                filterQuery[i] = keyPairsItemTypes[i].getKeyName();
                if (filterQuery[i] == null
                        || filterQuery[i].trim().length() == 0) {
                    throw new RemoteException(
                            "describeKeyPairs request is invalid, contains empty element?");
                }
            }
        }

        if (filterQuery == null) {
            return this.describeAllPairs(ownerID);
        } else {
            return this.describeSomePairs(ownerID, filterQuery);
        }
    }

    public DeleteKeyPairResponseType deleteKeyPair(DeleteKeyPairType req)
            throws RemoteException {

        // no use proceeding if these calls fail:
        final Caller caller = this.container.getCaller();
        final String ownerID;
        try {
            ownerID = this.container.getOwnerID(caller);
        } catch (CannotTranslateException e) {
            throw new RemoteException(e.getMessage(), e);
        }

        if (req == null) {
            throw new RemoteException("deleteKeyPair request is missing");
        }

        final String keyToDelete = req.getKeyName();

        final boolean aKeyWasDeleted =
                this.sshKeys.removeKey(ownerID, keyToDelete);

        final DeleteKeyPairResponseType dkprt =
                new DeleteKeyPairResponseType();
        dkprt.set_return(aKeyWasDeleted);
        return dkprt;
    }


    // -------------------------------------------------------------------------
    // SPLITTING IMPL (PUBKEY ONLY METHOD)
    // -------------------------------------------------------------------------

    protected CreateKeyPairResponseType splitMethod(String splitToken,
                                                    String input,
                                                    String ownerID)
            throws RemoteException {

        final String err = "Cannot register keypair.  Semantics " +
                    "for keypair creation are different than normal, see " +
                    "documentation.  " +
                    "(Expecting <keyname>" + splitToken + "<keyvalue>)";

        final int idx = input.indexOf(splitToken);
        if (idx < 1) {
            // (can't be idx 0 either, there would be no keyname then)
            throw new RemoteException(err + " (no token '" + splitToken + "')");
        }

        if (input.length() < splitToken.length() + 2) {
            throw new RemoteException(err + " (token present but " +
                    "name or value is missing)");
        }

        final String givenKeyName = input.substring(0,idx);
        final int validx = idx + splitToken.length();
        final String givenKeyValue = input.substring(validx);

        final String keyName = givenKeyName.trim();
        final String keyValue = givenKeyValue.trim();

        if (keyName.length() == 0 || keyValue.length() == 0) {
            throw new RemoteException(err + " (token present but " +
                    "name or value is missing)");
        }

        final String fingerprint = FAKE_FINGERPRINT;
        this.sshKeys.newKey(ownerID, keyName, keyValue, fingerprint);

        final CreateKeyPairResponseType ckprt = new CreateKeyPairResponseType();
        ckprt.setKeyFingerprint(fingerprint);
        ckprt.setKeyName(keyName);
        ckprt.setKeyMaterial(keyValue);

        logger.info("SSH key registered, name='" + keyName +
                "', owner ID='" + ownerID + "'");

        return ckprt;
    }


    // -------------------------------------------------------------------------
    // DESCRIBE IMPL
    // -------------------------------------------------------------------------

    protected DescribeKeyPairsResponseType describeAllPairs(String ownerID) {

        if (ownerID == null) {
            throw new IllegalArgumentException("ownerID may not be null");
        }

        final SSHKey[] allKeys = this.sshKeys.getOwnerKeys(ownerID);
        final List retList = new LinkedList();
        for (int i = 0; i < allKeys.length; i++) {
            final SSHKey key = allKeys[i];
            if (key == null) {
                logger.error("null in allKeys[]");
                continue; // *** SKIP ***
            }
            final DescribeKeyPairsResponseItemType one = this.getAPair(key);
            if (one == null) {
                continue; // *** SKIP ***
            }
            retList.add(one);
        }

        return convertDKPRTList(retList);
    }

    protected DescribeKeyPairsResponseType describeSomePairs(String ownerID,
                                                             String[] filter) {

        if (filter == null || filter.length == 0) {
            throw new IllegalArgumentException("filters may not be null/empty");
        }

        final List retList = new LinkedList();
        for (int i = 0; i < filter.length; i++) {
            if (filter[i] == null || filter[i].trim().length() == 0) {
                throw new IllegalArgumentException(
                        "filters may not have null/empty elements");
            }
            final String keyname = filter[i].trim();
            final SSHKey key = this.sshKeys.findKey(ownerID, keyname);
            if (key == null) {
                continue; // *** SKIP ***
            }
            final DescribeKeyPairsResponseItemType one = this.getAPair(key);
            if (one == null) {
                continue; // *** SKIP ***
            }
            retList.add(one);
        }

        return convertDKPRTList(retList);
    }

    protected DescribeKeyPairsResponseItemType getAPair(SSHKey key) {
        if (key == null) {
            throw new IllegalArgumentException("key may not be null");
        }
        final DescribeKeyPairsResponseItemType dkprit =
                new DescribeKeyPairsResponseItemType();
        dkprit.setKeyName(key.getKeyName());
        dkprit.setKeyFingerprint(key.getFingerprint());
        return dkprit;
    }

    private static DescribeKeyPairsResponseType convertDKPRTList(List retList) {
        final DescribeKeyPairsResponseItemType[] rets =
                (DescribeKeyPairsResponseItemType[]) retList.toArray(
                        new DescribeKeyPairsResponseItemType[retList.size()]);

        final DescribeKeyPairsResponseInfoType dkprt =
                new DescribeKeyPairsResponseInfoType();
        dkprt.setItem(rets);

        final DescribeKeyPairsResponseType response =
                new DescribeKeyPairsResponseType();
        response.setKeySet(dkprt);

        return response;
    }
}
