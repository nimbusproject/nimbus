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

import java.io.Serializable;

public class SSHKey implements Serializable {

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected final String ownerID;
    protected final String keyName;
    protected final String pubKeyValue;
    protected final String fingerprint;


    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    public SSHKey(String ownerID, String keyName,
                  String pubKeyValue, String fingerprint) {
        
        if (ownerID == null) {
            throw new IllegalArgumentException("ownerID may not be null");
        }
        if (keyName == null) {
            throw new IllegalArgumentException("keyName may not be null");
        }
        if (pubKeyValue == null) {
            throw new IllegalArgumentException("pubKeyValue may not be null");
        }
        if (fingerprint == null) {
            throw new IllegalArgumentException("fingerprint may not be null");
        }

        this.ownerID = ownerID;
        this.keyName = keyName;
        this.pubKeyValue = pubKeyValue;
        this.fingerprint = fingerprint;
    }


    // -------------------------------------------------------------------------
    // GET
    // -------------------------------------------------------------------------

    /**
     * @return ownerID, never null
     */
    public String getOwnerID() {
        return this.ownerID;
    }

    /**
     * @return key name, never null
     */
    public String getKeyName() {
        return this.keyName;
    }

    /**
     * @return public key value, never null
     */
    public String getPubKeyValue() {
        return this.pubKeyValue;
    }

    /**
     * @return sha1-digest(privkey) fingerprint value, never null
     */
    public String getFingerprint() {
        return this.fingerprint;
    }
}
