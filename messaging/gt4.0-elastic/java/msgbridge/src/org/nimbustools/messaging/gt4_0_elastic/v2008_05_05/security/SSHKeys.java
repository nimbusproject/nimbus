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

import org.nimbustools.messaging.gt4_0_elastic.DisabledException;

public interface SSHKeys {

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
    public String getSplitToken() throws DisabledException;

    /**
     * Find key.  Looked up by ownerID + keyName.  null if missing.
     *
     * @param ownerID unique owner ID
     * @param keyName shortcut name.  unique per owner only
     * @return key object or null if one cannot be found
     */
    public SSHKey findKey(String ownerID, String keyName);

    /**
     * Retrieve all keys from this owner.
     *
     * @param ownerID unique owner ID
     * @return all owner's key objects or zero-length array if zero are found
     */
    public SSHKey[] getOwnerKeys(String ownerID);


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
    public boolean newKey(String ownerID, String keyName,
                          String pubKeyContent, String fingerprint);


    /**
     * Remove a key from this ownerID's set of keys.
     *
     * @param ownerID unique owner ID
     * @param keyName unique name for key (unique for the owner)
     * @return true if this deleted a key
     */
    public boolean removeKey(String ownerID, String keyName);
}
