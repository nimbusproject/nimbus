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
package org.nimbustools.gateway.ec2.manager;

import org.nimbustools.api.repr.Caller;

import java.security.NoSuchAlgorithmException;

public class SecurityUtil {

    private static String SEP = "-";

    public static String getCallerHash(Caller caller) {
        if (caller == null) {
            throw new IllegalArgumentException("caller may not be null");
        }
        try {
            return HashUtil.hashDN(caller.getIdentity());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Checks that keyname is prefixed by specified user hash
     * @param keyName Prefixed key name to check
     * @param userHash User hash to look for
     * @return Whether key name is prefixed by user hash
     */
    public static boolean checkKeyName(String keyName, String userHash) {

        if (keyName == null) {
            throw new IllegalArgumentException("keyName may not be null");
        }
        keyName = keyName.trim();
        if (keyName.length() == 0) {
            throw new IllegalArgumentException("keyName may not be empty");
        }

        if (userHash == null) {
            throw new IllegalArgumentException("userHash may not be null");
        }
        userHash = userHash.trim();
        if (userHash.length() == 0) {
            throw new IllegalArgumentException("userHash may not be empty");
        }

        final String prefix = userHash + SEP;
        return (keyName.startsWith(prefix) &&
                keyName.length() > prefix.length());
    }

    /**
     * Prepends user hash to key name
     * @param keyName key name (not already prefixed)
     * @param userHash user hash
     * @return Modified key name
     */
    public static String prefixKeyName(String keyName, String userHash) {
        if (keyName == null) {
            throw new IllegalArgumentException("keyName may not be null");
        }
        keyName = keyName.trim();
        if (keyName.length() == 0) {
            throw new IllegalArgumentException("keyName may not be empty");
        }

        if (userHash == null) {
            throw new IllegalArgumentException("userHash may not be null");
        }
        userHash = userHash.trim();
        if (userHash.length() == 0) {
            throw new IllegalArgumentException("userHash may not be empty");
        }

        if (userHash.contains(SEP)) {
            throw new IllegalArgumentException(
                    "userHash may not contain seperator char("+SEP+")");
        }

        return userHash+SEP+keyName;
    }

    /**
     * Removes the hash prefix from key name
     * @param keyName prefixed key name
     * @param userHash user hash
     * @return Trimmed key name
     */
    public static String trimKeyName(String keyName, String userHash) {
        if (checkKeyName(keyName, userHash)) {
            return keyName.substring(userHash.length()+1);
        }
        throw new IllegalArgumentException("keyName must be prefixed by userHash");
    }

}
