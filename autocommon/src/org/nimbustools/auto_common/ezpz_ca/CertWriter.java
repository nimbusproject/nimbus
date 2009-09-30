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

import org.apache.axis.encoding.Base64;
import org.globus.gsi.OpenSSLKey;
import org.globus.gsi.bc.BouncyCastleOpenSSLKey;

import java.security.cert.X509Certificate;
import java.security.KeyPair;
import java.io.StringWriter;
import java.io.FileWriter;

public class CertWriter {

    // for PEM strings
    public static final int LINE_LENGTH = 64;
    public static final String lineSep = "\n";
    public static final String certHeader = "-----BEGIN CERTIFICATE-----";
    public static final String certFooter = "-----END CERTIFICATE-----";
    public static final String keyHeader = "-----BEGIN RSA PRIVATE KEY-----";
    public static final String keyFooter = "-----END RSA PRIVATE KEY-----";
    

    public void writeCert(X509Certificate cert,
                          KeyPair keyPair,
                          String pubpath,
                          String privpath) throws Exception {
        
        if (cert == null) {
            throw new IllegalArgumentException("cert may not be null");
        }
        if (keyPair == null) {
            throw new IllegalArgumentException("keyPair may not be null");
        }
        if (pubpath == null) {
            throw new IllegalArgumentException("pubpath may not be null");
        }
        if (privpath == null) {
            throw new IllegalArgumentException("privpath may not be null");
        }

        final String pubKeyPEM =
                certToPEMString(Base64.encode(cert.getEncoded()));

        final OpenSSLKey k = new BouncyCastleOpenSSLKey(keyPair.getPrivate());
        final StringWriter writer = new StringWriter();
        k.writeTo(writer);
        final String privKeyPEM = writer.toString();

        final FileWriter pubFile = new FileWriter(pubpath);
        final FileWriter privFile = new FileWriter(privpath);

        pubFile.write(pubKeyPEM);
        pubFile.close();
        privFile.write(privKeyPEM);
        privFile.close();
    }


    /**
     * Creates PEM encoded cert string with line length, header and footer.
     *
     * @param base64Data already encoded into string
     * @return string
     */
    public static String certToPEMString(String base64Data) {
        return toStringImpl(base64Data, false);
    }

    private static String toStringImpl(String base64Data, boolean isKey) {

        int length = LINE_LENGTH;
        int offset = 0;

        final StringBuffer buf = new StringBuffer(2048);

        if (isKey) {
            buf.append(keyHeader);
        } else {
            buf.append(certHeader);
        }
        buf.append(lineSep);

        final int size = base64Data.length();
        while (offset < size) {
            if (LINE_LENGTH > (size - offset)) {
                length = size - offset;
            }
            buf.append(base64Data.substring(offset, offset+length));
            buf.append(lineSep);
            offset = offset + LINE_LENGTH;
        }

        if (isKey) {
            buf.append(keyFooter);
        } else {
            buf.append(certFooter);
        }
        buf.append(lineSep);

        return buf.toString();
    }
}
