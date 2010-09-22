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

package org.globus.workspace.cloud.client.security;

import org.globus.common.CoGProperties;
import org.globus.gsi.TrustedCertificates;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.StringTokenizer;

public class CertUtil {

    /**
     * @param paths can be dirs - or a mix of dirs and files
     * @param err PrintStream for error messages
     * @param debug PrintStream for debug messages
     * @return never null, len 0 or more, all exceptions squelched.
     */
    public static Cert[] loadCerts(String[] paths,
                                   PrintStream err,
                                   PrintStream debug) {

        final ArrayList allcerts = new ArrayList(paths.length * 8);
        for (int i = 0; i < paths.length; i++) {
            try {
                allcerts.addAll(load(paths[i], debug));
            } catch (Exception e) {
                final String msg =
                        "Problem with '" + paths[i] + "': " + e.getMessage();
                if (err != null) {
                    err.println(msg);
                } else if (debug != null) {
                    debug.println(msg);
                }
            }
        }
        return (Cert[]) allcerts.toArray(new Cert[allcerts.size()]);
    }

    private static ArrayList load(String path, PrintStream debug)
            throws IOException, GeneralSecurityException {
        
        final ArrayList certs = new ArrayList(8);
        File f = new File(path);

        if (!f.canRead()) {
            throw new IOException("can't read " + f.getAbsolutePath());
        }

        if (!f.isDirectory()) {

            try {

                final X509Certificate cert =
                        org.globus.gsi.CertUtil.loadCertificate(f.getAbsolutePath());

                certs.add(new Cert(f.getAbsolutePath(), cert));

            } catch (GeneralSecurityException e) {
                throw e;
            }

        } else {

            final String[] cas = f.list(TrustedCertificates.getCertFilter());

            if (cas != null) {

                for (int i = 0; i < cas.length; i++) {

                    final String caFilename = f.getPath() +
                                                   File.separatorChar + cas[i];

                    final File caFile = new File(caFilename);

                    if (caFile.canRead()) {

                        try {

                            final X509Certificate cert =
                                    org.globus.gsi.CertUtil.loadCertificate(
                                            caFile.getAbsolutePath());

                            certs.add(new Cert(caFile.getAbsolutePath(),cert));

                        } catch (GeneralSecurityException e) {

                            if (debug != null) {
                                debug.println("Problem with '" +
                                        caFile.getAbsolutePath() + "': " +
                                        e.getMessage());
                            }
                        }

                    }
                }
            }
        }

        return certs;
    }

    public static String[] trustedCertificateDirectories(PrintStream debug)
            throws Exception {

        final String dirsStr =
                    CoGProperties.getDefault().getCaCertLocations();
        if (dirsStr == null) {
            throw new Exception("Couldn't find trusted CA location(s)");
        }

        final StringTokenizer tok = new StringTokenizer(dirsStr, ",");
        int len = tok.countTokens();
        if (debug != null) {
            debug.println("Found " + tok.countTokens() +
                                                " CA cert locations");
        }

        final ArrayList dirs = new ArrayList(len);
        while (tok.hasMoreTokens()) {
            final String dir = tok.nextToken();
            if (debug != null) {
                String msg = "Found a CA directory: '" + dir + "'";
                final File file = new File(dir);
                if (file.exists()) {
                    msg += " (exists)";
                } else {
                    msg += " (does not exist)";
                }
                if (file.isDirectory()) {
                    msg += " (is dir)";
                } else {
                    msg += " (is not a dir)";
                }
                debug.println(msg);
            }
            dirs.add(dir);
        }

        return (String[]) dirs.toArray(new String[dirs.size()]);
    }
}
