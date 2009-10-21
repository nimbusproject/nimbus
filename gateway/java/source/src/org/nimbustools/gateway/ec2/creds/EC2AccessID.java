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

package org.nimbustools.gateway.ec2.creds;

public class EC2AccessID {
    private String key;
    private String secret;

    public String getKey() {
        return key;
    }

    public String getSecret() {
        return secret;
    }

    public EC2AccessID(String key, String secret) {
        if (key == null) {
            throw new IllegalArgumentException("key may not be null");
        }
        if (secret == null) {
            throw new IllegalArgumentException("secret may not be null");
        }

        this.key = key;
        this.secret = secret;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EC2AccessID that = (EC2AccessID) o;

        if (!key.equals(that.key)) return false;
        if (!secret.equals(that.secret)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = key.hashCode();
        result = 31 * result + secret.hashCode();
        return result;
    }
}
