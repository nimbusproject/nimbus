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

package org.nimbustools.auto_common.ezpz_ca;

import java.security.NoSuchAlgorithmException;
import java.security.MessageDigest;

public class HashUtil {

    private final MessageDigest md5;

    public HashUtil() throws NoSuchAlgorithmException {
        this.md5 = MessageDigest.getInstance("MD5");
    }

    public String hashDN(String dn) throws NoSuchAlgorithmException {

        if (dn == null) {
            return null;
        }
        return this.hash(dn.getBytes());
    }

    // bit twiddling solution from the jglobus Attic gets it right for CAs
    // http://www.cogkit.org/viewcvs/viewcvs.cgi/src/jglobus/src/org/globus/security/Attic/HashUtil.java?rev=HEAD&content-type=text/vnd.viewcvs-markup
    private String hash(byte [] data) {

        this.md5.reset();
        this.md5.update(data);

        final byte[] md = md5.digest();

        final long ret = (fixByte(md[0]) | fixByte(md[1]) << 8L |
             fixByte(md[2])<<16L | fixByte(md[3])<<24L )&0xffffffffL;

        return Long.toHexString(ret);
    }

    private static long fixByte(byte b) {
        return (b<0) ? (long)(b+256) : (long)b;
    }
}
