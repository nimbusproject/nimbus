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

import org.nimbustools.messaging.gt4_0_elastic.DisabledException;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.general.ElasticPersistence;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.security.SSHKey;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.security.SSHKeys;

import java.util.List;

public class DefaultSSHKeys implements SSHKeys {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    protected static final SSHKey[] EMPTY_SSH_KEYS = new SSHKey[0];


    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    final private ElasticPersistence persistence;

    protected boolean pubkeyOnly;
    protected String splitToken;
    
    
    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    public DefaultSSHKeys(ElasticPersistence persistence) {
        if (persistence == null) {
            throw new IllegalArgumentException("persistence may not be null");
        }
        this.persistence = persistence;

    }


    // -------------------------------------------------------------------------
    // SET
    // -------------------------------------------------------------------------

    public void setPubkeyOnly(boolean pubkeyOnly) {
        this.pubkeyOnly = pubkeyOnly;
    }
    
    public void setSplitToken(String splitToken) {
        this.splitToken = splitToken;
    }


    // -------------------------------------------------------------------------
    // IoC INIT METHOD
    // -------------------------------------------------------------------------

    void validate() throws Exception {
        if (this.pubkeyOnly) {
            if (this.splitToken == null
                    || this.splitToken.trim().length() == 0) {
                throw new Exception("Invalid: Missing ssh key 'split token'");
            }
        }
    }
    

    // -------------------------------------------------------------------------
    // implements SSHKeys
    // -------------------------------------------------------------------------

    /**
     * In order to not have to generate a private key server side, the key
     * name request is expected to have a token in it that splits the key
     * name from the pubkey content.
     *
     * Behavior can be disabled in which case keygen is used.
     *
     * @return token, never null
     * @throws DisabledException related behavior disabled
     */
    public String getSplitToken() throws DisabledException {
        if (!this.pubkeyOnly) {
            throw new DisabledException("pubkey-only behavior disabled");
        }
        return this.splitToken;
    }

    /**
     * Find key.  Looked up by ownerID + keyName.  null if missing.
     *
     * @param ownerID unique owner ID
     * @param keyName shortcut name.  unique per owner only
     *
     * @return key object or null if one cannot be found
     */
    public synchronized SSHKey findKey(String ownerID, String keyName) {

        if (ownerID == null) {
            throw new IllegalArgumentException("ownerID may not be null");
        }
        if (keyName == null) {
            throw new IllegalArgumentException("keyName may not be null");
        }

        try {
            return this.persistence.getSSHKey(ownerID, keyName);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Retrieve all keys from this owner.
     *
     * @param ownerID unique owner ID
     *
     * @return all owner's key objects or zero-length array if zero are found
     */
    public synchronized SSHKey[] getOwnerKeys(String ownerID) {

        if (ownerID == null) {
            throw new IllegalArgumentException("ownerID may not be null");
        }

        try {
            final List<SSHKey> keys = persistence.getSSHKeys(ownerID);
            return keys.toArray(new SSHKey[keys.size()]);

        } catch (Exception e) {
            return EMPTY_SSH_KEYS;
        }
    }

    /**
     * Add a key to this ownerID's set of keys.
     *
     * If the owner has already registered something with the same 'keyName' it
     * will be overwritten.
     *
     * @param ownerID       unique owner ID, !null
     * @param keyName       unique name for key (unique for the owner), !null
     * @param pubKeyContent may not be null, !null
     *
     * @param fingerprint   may not be null, exception will be thrown
     * @return true if this was new, false if it replaced something
     */
    public synchronized boolean newKey(String ownerID,
                                       String keyName,
                                       String pubKeyContent,
                                       String fingerprint) {

        if (ownerID == null) {
            throw new IllegalArgumentException("ownerID may not be null");
        }
        if (keyName == null) {
            throw new IllegalArgumentException("keyName may not be null");
        }
        if (pubKeyContent == null) {
            throw new IllegalArgumentException("pubKeyContent may not be null");
        }
        if (fingerprint == null) {
            throw new IllegalArgumentException("fingerprint may not be null");
        }

        final SSHKey key = new SSHKey(ownerID, keyName,
                                      pubKeyContent, fingerprint);

        try {
            final boolean exists = persistence.getSSHKey(ownerID, keyName) != null;

            if (exists) {
                persistence.updateSSHKey(key);
            } else {
                persistence.putSSHKey(key);
            }
            return !exists;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Remove a key from this ownerID's set of keys.
     *
     * @param ownerID unique owner ID
     * @param keyName unique name for key (unique for the owner)
     * @return true if this deleted a key
     */
    public boolean removeKey(String ownerID, String keyName) {

        if (ownerID == null) {
            throw new IllegalArgumentException("ownerID may not be null");
        }
        if (keyName == null) {
            throw new IllegalArgumentException("keyName may not be null");
        }

        try {
            return persistence.deleteSSHKey(ownerID, keyName);
        } catch (Exception e) {
            return false;
        }
    }
}
