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

import org.globus.gsi.GlobusCredential;
import org.globus.gsi.CertUtil;

import javax.security.auth.x500.X500Principal;
import java.io.File;
import java.security.PrivateKey;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Calendar;

public class GenerateNewCert {

    // five year certs
    public static final int VALIDITY_MONTHS = 60;

    
    // -------------------------------------------------------------------------
    // GENERATE
    // -------------------------------------------------------------------------

    public String generateCert(String targetDir,
                               String CN,
                               String pubpemName,
                               String privpemName,
                               String caPubPemPath,
                               String caPrivPemPath) throws Exception {

        if (targetDir == null) {
            throw new IllegalArgumentException("targetDir may not be null");
        }
        if (CN == null) {
            throw new IllegalArgumentException("CN may not be null");
        }
        if (pubpemName == null) {
            throw new IllegalArgumentException("pubpemName may not be null");
        }
        if (privpemName == null) {
            throw new IllegalArgumentException("privpemName may not be null");
        }
        if (caPubPemPath == null) {
            throw new IllegalArgumentException("caPubPemPath may not be null");
        }
        if (caPrivPemPath == null) {
            throw new IllegalArgumentException("caPrivPemPath may not be null");
        }

        final String pubpath = targetDir + File.separator + pubpemName;
        final String privpath = targetDir + File.separator + privpemName;

        final File pubFileCheck = new File(pubpath);
        if (pubFileCheck.exists()) {
            throw new Exception("File already exists: " + pubpath);
        }
        final File privFileCheck = new File(privpath);
        if (privFileCheck.exists()) {
            throw new Exception("File already exists: " + privpath);
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

        final KeyPair keyPair = ca.createNewKeyPair();

        final Calendar expires = Calendar.getInstance();
        expires.add(Calendar.MONTH, VALIDITY_MONTHS);
        final X509Certificate newcert =
                ca.signNewCertificate(CN, keyPair.getPublic(), expires);

        new CertWriter().writeCert(newcert, keyPair, pubpath, privpath);

        final X500Principal subjectDN = newcert.getSubjectX500Principal();
        final String DN = subjectDN.getName(X500Principal.RFC2253);

        // globus style DN
        return CertUtil.toGlobusID(DN, false);
    }

    
    // -------------------------------------------------------------------------
    // MAIN
    // -------------------------------------------------------------------------

    public static void main(String[] args) {

        if (args == null || args.length != 6) {
            System.err.println("Needs these arguments:\n" +
                    "1 - the target directory path\n" +
                    "2 - the new CN to create\n" +
                    "3 - the target pub pem name\n" +
                    "4 - the target priv pem name\n" +
                    "5 - the pub pem of EzPz CA\n" +
                    "6 - the priv pem of EzPz CA");
            System.exit(1);
        }

        try {
            final String globusDN = new GenerateNewCert().
                                        generateCert(args[0], args[1], args[2],
                                                     args[3], args[4], args[5]);

            System.out.println(globusDN);
            
        } catch (Exception e) {
            System.err.println("Problem creating certificate: " + e.getMessage());
            System.exit(1);
        }
    }
}
