/*
 * Copyright 1999-2010 University of Chicago
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
import org.globus.gsi.GlobusCredential;

import java.io.File;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.security.PrivateKey;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;

public class GenerateCRL {

    // -------------------------------------------------------------------------
    // GENERATE
    // -------------------------------------------------------------------------

    public void generateCRL(String targetPath,
                            String caPubPemPath,
                            String caPrivPemPath) throws Exception {

        if (targetPath == null) {
            throw new IllegalArgumentException("targetPath may not be null");
        }
        if (caPubPemPath == null) {
            throw new IllegalArgumentException("caPubPemPath may not be null");
        }
        if (caPrivPemPath == null) {
            throw new IllegalArgumentException("caPrivPemPath may not be null");
        }

        final File crlFileCheck = new File(targetPath);
        if (crlFileCheck.exists()) {
            throw new Exception("File already exists: " + targetPath);
        }

        File certFile = new File(caPubPemPath);
        if (!certFile.canRead()) {
            final String msg = "Configured CA certificate path ('" +
                               caPubPemPath + "') can not be read.";
            throw new Exception(msg);
        }

        certFile = new File(caPrivPemPath);
        if (!certFile.canRead()) {
            final String msg = "Configured CA key path ('" +
                               caPrivPemPath + "') can not be read.";
            throw new Exception(msg);
        }

        final GlobusCredential caGlobusCred =
                new GlobusCredential(caPubPemPath, caPrivPemPath);

        final X509Certificate caCert = caGlobusCred.getIdentityCertificate();
        final PrivateKey caPrivateKey = caGlobusCred.getPrivateKey();

        final EzPzCA ca = new EzPzCA(caCert, caPrivateKey,
                                     caGlobusCred.getIdentity());

        final X509CRL crl = ca.generateCRL();
        
        final String crlPEM = CertWriter.crlToPEMString(Base64.encode(crl.getEncoded()));
        final OutputStreamWriter crlFile = new FileWriter(targetPath);
        crlFile.write(crlPEM);
        crlFile.close();
    }


    // -------------------------------------------------------------------------
    // MAIN
    // -------------------------------------------------------------------------

    public static void main(String[] args) throws Exception {

        if (args == null || args.length != 3) {
            System.err.println("Needs these arguments:\n" +
                    "1 - the target path to write CRL\n" +
                    "2 - the pub pem of EzPz CA\n" +
                    "3 - the priv pem of EzPz CA");
            System.exit(1);
        }

        new GenerateCRL().generateCRL(args[0], args[1], args[2]);

        try {


        } catch (Exception e) {
            System.err.println("Problem creating CRL: " + e.getMessage());
            System.exit(1);
        }
    }
}
