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

package org.nimbustools.messaging.gt4_0.common;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.gsi.GlobusCredential;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.asn1.DEROutputStream;

import javax.security.auth.x500.X500Principal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.io.PrintStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;

// TODO: move to client
public class SecurityUtil {


    private static final Log logger =
            LogFactory.getLog(SecurityUtil.class.getName());

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

    public static void checkMD5() throws NoSuchAlgorithmException {
        if (md5 == null) {
            throw new NoSuchAlgorithmException(
                    "MD5 digester was not initialized");
        }
    }

    public static String hashGlobusCredential(GlobusCredential proxy,
                                              PrintStream debug)
            throws NoSuchAlgorithmException, IOException {

        checkMD5();

        if (proxy == null) {
            throw new IllegalArgumentException("proxy may not be null");
        }

        final String id = proxy.getIdentity();

        if (debug != null) {
            debug.println("proxy identity for hashing: '" + id + "'");
        }

        if (id == null) {
            throw new IllegalArgumentException("given proxy is invalid");
        }

        final String hsh = hash(id.getBytes());
        if (debug != null) {
            debug.println("hash '" + hsh + "' from '" + id + "'");
        }
        return hsh;
    }

    // Returns equivalent of: openssl x509 -in "cert-file" -hash -noout
    public static String caNameHash(Principal subjectDN)
            throws IOException, NoSuchAlgorithmException {

        checkMD5();
        return hash(encodePrincipal(subjectDN));
    }

    private static byte[] encodePrincipal(Principal subject) throws IOException {

        if (subject == null) {
            throw new IllegalArgumentException("subject may not be null");
        }

        if (subject instanceof X500Principal) {

            return ((X500Principal)subject).getEncoded();

        } else if (subject instanceof X509Name) {

            ByteArrayOutputStream bout = null;
            DEROutputStream der = null;
            try {
                bout = new ByteArrayOutputStream();
                der = new DEROutputStream(bout);
                final X509Name nm = (X509Name)subject;
                der.writeObject(nm.getDERObject());
                return bout.toByteArray();
            } finally {
                if (der != null) {
                    der.close();
                }
                if (bout != null) {
                    bout.close();
                }
            }

        } else {
            throw new ClassCastException("unsupported input class: "
                                + subject.getClass().toString());
        }
    }

    public static String hashDN(String dn) throws NoSuchAlgorithmException {

        if (dn == null) {
            return null;
        }

        checkMD5();

        return hash(dn.getBytes());
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
