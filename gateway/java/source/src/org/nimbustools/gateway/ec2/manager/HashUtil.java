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

package org.nimbustools.gateway.ec2.manager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Implementation of the cloud configuration hashing convention.
 *
 * TODO: move away from statics
 */
public class HashUtil {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(HashUtil.class.getName());

    private static MessageDigest md5;

    static {
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            md5 = null;
            final String err = "Could not intialize MD5 digester: ";
            logger.fatal(err + e.getMessage(), e);
        }
    }


    // -------------------------------------------------------------------------
    // HASH IMPL
    // -------------------------------------------------------------------------

    public static boolean isInitialized() {
        return md5 != null;
    }

    /**
     * @param dn caller identity
     * @return hash, or null if input is null
     * @throws java.security.NoSuchAlgorithmException problem with hash implementation
     */
    public static synchronized String hashDN(String dn) throws NoSuchAlgorithmException {

        if (dn == null) {
            return null;
        }

        checkMD5();

        return hash(dn.getBytes());
    }

    private static void checkMD5() throws NoSuchAlgorithmException {
        if (md5 == null) {
            throw new NoSuchAlgorithmException(
                    "MD5 digester was not initialized");
        }
    }

    // bit twiddling solution from the jglobus Attic gets it right for CAs
    // http://www.cogkit.org/viewcvs/viewcvs.cgi/src/jglobus/src/org/globus/security/Attic/HashUtil.java?rev=HEAD&content-type=text/vnd.viewcvs-markup
    private static String hash(byte [] data) {

        md5.reset();
        md5.update(data);

        final byte[] md = md5.digest();

        final long ret = (fixByte(md[0]) | fixByte(md[1]) << 8L |
             fixByte(md[2])<<16L | fixByte(md[3])<<24L )&0xffffffffL;

        return Long.toHexString(ret);
    }

    private static long fixByte(byte b) {
        return (b<0) ? (long)(b+256) : (long)b;
    }
}