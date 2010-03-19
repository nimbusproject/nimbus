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
package org.nimbustools.auto_common.ezpz_ca;

import java.io.File;
import java.io.IOException;
import java.io.FileReader;
import java.security.cert.X509Certificate;
import java.security.Security;

import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.globus.gsi.CertUtil;

import javax.security.auth.x500.X500Principal;

public class CertDN {


    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static String dnFromPath(String path) throws IOException {

        final File certFile = new File(path);
        if (!certFile.canRead()) {
            final String msg = "File '" + path + "' can not be read.";
            throw new IOException(msg);
        }

		final FileReader fr = new FileReader(certFile);
		try {
			final PEMReader reader =
					new PEMReader(fr, null, BouncyCastleProvider.PROVIDER_NAME);
			try {
				final X509Certificate cert = (X509Certificate) reader.readObject();
                final X500Principal principal = cert.getSubjectX500Principal();
                final String DN = principal.getName(X500Principal.RFC2253);

                return CertUtil.toGlobusID(DN, false);

            } finally {
				reader.close();
			}
		} finally {
			fr.close();
		}
    }


    public static void main(String[] args) {

        if (args == null || args.length != 1) {
            System.err.println("Needs these arguments:\n" +
                    "1 - the certificate file");
            System.exit(1);
        }

        try {
            final String dn = dnFromPath(args[0]);
            System.out.println(dn);
        } catch (Throwable t) {
            System.err.println("Problem: " + t.getMessage());
			t.printStackTrace();
            System.exit(1);
        }
    }
}
