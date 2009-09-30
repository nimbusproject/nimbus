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

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import org.nimbustools.messaging.gt4_0_elastic.DisabledException;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.security.SSHKey;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.security.SSHKeys;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class DefaultSSHKeys implements SSHKeys {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    protected static final SSHKey[] EMPTY_SSH_KEYS = new SSHKey[0];


    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    // key: 'ownerID',  value: List of SSHKey objects
    private final Cache sshKeyCache;

    protected boolean pubkeyOnly;
    protected String splitToken;
    
    
    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    public DefaultSSHKeys(KeyCacheProvider cacheLocator) {

        if (cacheLocator == null) {
            throw new IllegalArgumentException("cacheLocator may not be null");
        }

        this.sshKeyCache = cacheLocator.getKeyCache();
        if (this.sshKeyCache == null) {
            throw new IllegalArgumentException(
                    "cacheLocator failed to provide key cache");
        }
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

        final Element el = this.sshKeyCache.get(ownerID);

        if (el == null) {
            return null; // *** EARLY RETURN ***
        }

        final List allOwnerKeys = (List) el.getObjectValue();
        if (allOwnerKeys.isEmpty()) {
            return null; // *** EARLY RETURN ***
        }

        final Iterator iter = allOwnerKeys.iterator();
        while (iter.hasNext()) {
            final SSHKey key = (SSHKey)iter.next();
            if (key.getKeyName().equals(keyName)) {
                return key;
            }
        }

        return null;
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

        final Element el = this.sshKeyCache.get(ownerID);

        if (el == null) {
            return EMPTY_SSH_KEYS; // *** EARLY RETURN ***
        }

        final List allOwnerKeys = (List) el.getObjectValue();
        if (allOwnerKeys.isEmpty()) {
            return EMPTY_SSH_KEYS; // *** EARLY RETURN ***
        }

        return (SSHKey[]) allOwnerKeys.toArray(new SSHKey[allOwnerKeys.size()]);
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

        final Element el = this.sshKeyCache.get(ownerID);
        final List allOwnerKeys;
        if (el == null) {
            allOwnerKeys = new LinkedList();
            final Element newel = new Element(ownerID, allOwnerKeys);
            this.sshKeyCache.put(newel);
        } else {
            allOwnerKeys = (List) el.getObjectValue();
        }

        boolean isNewKey = true;

        final Iterator iter = allOwnerKeys.iterator();
        while (iter.hasNext()) {
            final SSHKey key = (SSHKey)iter.next();
            if (key.getKeyName().equals(keyName)) {
                iter.remove();
                isNewKey = false;
                break;
            }
        }

        final SSHKey key = new SSHKey(ownerID, keyName,
                                      pubKeyContent, fingerprint);
        allOwnerKeys.add(key);
        this.sshKeyCache.flush();
        
        return isNewKey;
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

        final Element el = this.sshKeyCache.get(ownerID);

        if (el == null) {
            return false; // *** EARLY RETURN ***
        }

        final List allOwnerKeys = (List) el.getObjectValue();
        if (allOwnerKeys.isEmpty()) {
            return false; // *** EARLY RETURN ***
        }

        boolean deletedKey = false;

        final Iterator iter = allOwnerKeys.iterator();
        while (iter.hasNext()) {
            final SSHKey key = (SSHKey)iter.next();
            if (key.getKeyName().equals(keyName)) {
                iter.remove();
                deletedKey = true;
                break;
            }
        }

        return deletedKey;
    }
}
