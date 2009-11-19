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
package org.nimbustools.api.services.security;

public class KeyPair {
    
    protected String keyName;
    protected String publicKey;
    protected String privateKey;
    protected String fingerprint;

    public KeyPair(String name, String fingerprint,
                   String publicKey, String privateKey) {

        if (name == null) {
            throw new IllegalArgumentException("name may not be null");
        }
        if (fingerprint == null) {
            throw new IllegalArgumentException("fingerprint may not be null");
        }

        // publicKey and privateKey can be null

        this.keyName = name;
        this.fingerprint = fingerprint;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
    }

    public String getKeyName() {
        return keyName;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public String getFingerprint() {
        return fingerprint;
    }

}
