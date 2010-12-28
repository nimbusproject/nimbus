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

package org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.security.defaults;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.KeyPair;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.CannotTranslateException;
import org.nimbustools.messaging.gt4_0_elastic.DisabledException;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.CreateKeyPairResponseType;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.rm.ContainerInterface;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.security.KeyGen;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.security.KeyGenException;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.security.SSHKeys;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;

public class DefaultKeyGen implements KeyGen {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(DefaultKeyGen.class.getName());


    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected final SSHKeys sshKeys;
    protected final ContainerInterface container;
    
    protected boolean pubkeyOnly;
    protected String typeStr;
    protected int keyType;
    protected int keySize = -1;
    protected boolean keySizeConfigured;
    

    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    public DefaultKeyGen(SSHKeys sshKeysImpl,
                         ContainerInterface containerImpl) {

        if (sshKeysImpl == null) {
            throw new IllegalArgumentException("sshKeysImpl may not be null");
        }
        this.sshKeys = sshKeysImpl;

        if (containerImpl == null) {
            throw new IllegalArgumentException("containerImpl may not be null");
        }
        this.container = containerImpl;
    }


    // -------------------------------------------------------------------------
    // SET
    // -------------------------------------------------------------------------

    public void setPubkeyOnly(boolean pubkeyOnly) {
        this.pubkeyOnly = pubkeyOnly;
    }

    public void setKeyType(String keyType) {
        this.typeStr = keyType;
    }

    public void setKeySize(int keySize) {
        this.keySizeConfigured = true;
        this.keySize = keySize;
    }

    
    // -------------------------------------------------------------------------
    // IoC INIT METHOD
    // -------------------------------------------------------------------------

    void validate() throws Exception {

        if (this.pubkeyOnly) {
            return; // *** EARLY RETURN ***
        }

        if (this.typeStr == null
                || this.typeStr.trim().length() == 0) {
            throw new Exception("Invalid: Missing generated SSH key type " +
                    "(must be 'rsa' or 'dsa')");
        }

        if(this.typeStr.trim().equalsIgnoreCase("rsa")) {
            this.keyType = KeyPair.RSA;
        } else if (this.typeStr.trim().equalsIgnoreCase("dsa")) {
            this.keyType = KeyPair.DSA;
        } else {
            throw new Exception("Invalid: sshkeygen type must only be 'rsa' " +
                    "or 'dsa', current configuration '" + this.typeStr + "'");
        }

        if (!this.keySizeConfigured) {
            throw new Exception(
                    "Invalid: Missing generated SSH key size configuration");
        }

        if (this.keySize < 1024) {
            throw new Exception("Invalid: Generated SSH key size " +
                    "configuration is less than 1024?");
        }
    }
    
    
    // -------------------------------------------------------------------------
    // implements KeyGen
    // -------------------------------------------------------------------------

    public CreateKeyPairResponseType createNewKeyPair(Caller caller,
                                                      String keyName)
            throws DisabledException, KeyGenException {
        try {
            return this.newKey(this.container.getOwnerID(caller), keyName);
        } catch (IOException e) {
            throw new KeyGenException(e.getMessage(), e);
        } catch (CannotTranslateException e) {
            throw new KeyGenException(e.getMessage(), e);
        }
    }

    protected CreateKeyPairResponseType newKey(String ownerID, String keyName)
            throws KeyGenException, IOException, DisabledException {

        if (this.pubkeyOnly) {
            throw new DisabledException("SSH key generation is disabled");
        }

        final JSch jsch=new JSch();
        final KeyPair kpair;
        try {
            kpair = KeyPair.genKeyPair(jsch, this.keyType, this.keySize);
        } catch (JSchException e) {
            throw new KeyGenException(e.getMessage(), e);
        }

        final String generatedFingerprint = kpair.getFingerPrint();
        if (generatedFingerprint == null) {
            throw new KeyGenException("fingerprint is missing");
        }
        final String[] parts = generatedFingerprint.split(" ");
        if (parts.length != 2) {
            throw new KeyGenException("fingerprint not in expected " +
                    "format: '" + generatedFingerprint + "'");
        }

        final String fingerprint = parts[1];
        if (fingerprint == null || fingerprint.trim().length() == 0) {
            throw new KeyGenException("fingerprint not in expected " +
                    "format: '" + generatedFingerprint + "'");
        }
        
        final StringOutputStream pubsos = new StringOutputStream();
        final StringOutputStream privsos = new StringOutputStream();

        kpair.writePublicKey(pubsos, "clouduser-" + ownerID);
        kpair.writePrivateKey(privsos);

        final String pubKeyString = pubsos.toString();
        if (pubKeyString == null || pubKeyString.trim().length() == 0) {
            throw new KeyGenException("generated pubkey is missing");
        }

        final String privKeyString = privsos.toString();
        if (privKeyString == null || privKeyString.trim().length() == 0) {
            throw new KeyGenException("generated privkey is missing");
        }

        // register it before returning
        this.sshKeys.newKey(ownerID, keyName, pubKeyString, fingerprint);

        logger.info("New SSH key created, name='" + keyName +
                "', owner ID='" + ownerID + "'");

        final CreateKeyPairResponseType ckprt = new CreateKeyPairResponseType();
        ckprt.setKeyFingerprint(fingerprint);
        ckprt.setKeyMaterial(privKeyString);
        ckprt.setKeyName(keyName);
        return ckprt;
    }
}
