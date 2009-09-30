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

public class PropEC2AccessManager implements EC2AccessManager {

    // right now this just grabs EC2 info from java env vars.
    // in the future it'll be something fancy?

    private EC2AccessID accessId;

    private final static String PROP_EC2_KEY = "EC2_KEY";
    private final static String PROP_EC2_SECRET = "EC2_SECRET";


    public PropEC2AccessManager() {
        String key = System.getProperty(PROP_EC2_KEY);
        if (key == null) {
            throw new IllegalStateException("need property "+PROP_EC2_KEY);
        }
        String secret = System.getProperty(PROP_EC2_SECRET);
        if (secret == null) {
            throw new IllegalStateException("need property "+PROP_EC2_SECRET);
        }

        accessId = new EC2AccessID(key, secret);
    }

    public EC2AccessID getAccessID(Caller caller) {
        // right now we just deal with one  EC2 credential
        return accessId;
    }

    public EC2AccessID getAccessIDByKey(String key)
            throws EC2AccessException {
        if (accessId.getKey().equals(key)) {
            return this.accessId;
        }
        throw new EC2AccessException("No EC2 access ID is known with " +
                "the provided key");
    }
}
