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

package org.nimbustools.gateway.ec2;

import org.nimbustools.api.repr.Caller;

/**
 * Has a single EC2 access ID that it uses for all Callers
 */
public class SimpleEC2AccessManager implements EC2AccessManager {

    // to be set by IoC
    private String key = null;
    private String secret = null;

    private EC2AccessID accessId = null;


    void validate() throws Exception {
        if (key == null) {
            throw new Exception("Invalid: EC2 access key is not specified");
        }
        if (secret == null) {
            throw new Exception("Invalid: EC2 access secret is not specified");
        }
    }

    public EC2AccessID getAccessID(Caller caller) throws EC2AccessException {
        if (this.accessId == null) {
            this.accessId = new EC2AccessID(this.key, this.secret);
        }
        return this.accessId;
    }

    public EC2AccessID getAccessIDByKey(String key) throws EC2AccessException {
        if (key == null) {
            throw new IllegalArgumentException("key may not be null");
        }

        if (key.equals(this.key)) {
            if (this.accessId == null) {
                this.accessId = new EC2AccessID(this.key, this.secret);
            }
            return this.accessId;
        }
        throw new EC2AccessException("EC2 access ID key not known");

    }


    public void setKey(String key) {
        this.key = key;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }
}
