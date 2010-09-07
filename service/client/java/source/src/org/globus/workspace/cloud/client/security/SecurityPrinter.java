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

import org.globus.gsi.GlobusCredential;
import org.globus.gsi.bc.BouncyCastleUtil;
import org.globus.workspace.common.SecurityUtil;

import java.io.PrintStream;

public class SecurityPrinter {

    private final GlobusCredential inuse;
    private String id;
    private String[] certDirs;

    public SecurityPrinter(GlobusCredential credential) throws Exception {
        if (credential == null) {
            throw new IllegalArgumentException("credential may not be null");
        }
        this.inuse = credential;
        this.identity();
        this.certDirs = null;
    }
    
    public SecurityPrinter(GlobusCredential credential,
                           String[] trustedCertDirs) throws Exception {
        this(credential);
        this.certDirs = trustedCertDirs;
    }

    private void identity() throws Exception {
        this.id = this.inuse.getIdentity();
        if (this.id == null) {
            throw new Exception("invalid default credential (no identity?)");
        }
    }

    public void print(String configuredCAHash,
                      PrintStream out,
                      PrintStream debug) throws Exception {

        if (out == null && debug == null) {
            // nothing to do
            return;
        }

        // one of these must be !null
        final PrintStream pr;
        if (out != null) {
            pr = out;
        } else {
            pr = debug;
        }

        pr.println("Credential in use:");
        pr.println("  - Identity: '" + this.id + "'");
        pr.println("  - Subject: '" + this.inuse.getSubject() + "'");
        pr.println("  - Issuer: '" + this.inuse.getIssuer() + "'");

        if (debug != null) {
            debug.println("\nGetting trusted CA directories.\n");
        }

        if (this.certDirs == null) {
            this.certDirs =
                    CertUtil.trustedCertificateDirectories(debug);
        }

        if (this.certDirs == null) {
            pr.println("Unexpected, null certDirs response.");
            throw new Exception("Unexpected, null certDirs response.");
        }

        if (debug != null) {
            debug.println("certDirs length: " + this.certDirs.length);
        }

        if (this.certDirs.length == 0) {

            pr.println("\nWarning: no trusted certificate directories are " +
                    "configured.");

        } else if (this.certDirs.length == 1) {

            pr.println("\nTrusted certificate path: " + this.certDirs[0]);

        } else if (this.certDirs.length > 1) {
            
            pr.println("\nTrusted certificate paths:");
            for (int i = 0; i < this.certDirs.length; i++) {
                pr.println("  - " + this.certDirs[i]);
            }
        }

        final Cert[] certs =
                CertUtil.loadCerts(this.certDirs, out, debug);

        if (certs == null) {
            pr.println("Unexpected, null certs response.");
            throw new Exception("Unexpected, null certs response.");
        }

        if (debug != null) {
            debug.println("\ncerts length: " + certs.length);
        }

        if (certs.length == 0) {
            pr.println("Warning: no certificates were found in any of the " +
                    "trusted CA directories");
        }

        if (debug != null) {
            for (int i = 0; i < certs.length; i++) {

                debug.println("\nCA CERT: '" +
                                    certs[i].getAbsolutePath() + "'");

                //debug.println("  -      Subject: " +
                //        certs[i].getCert().getSubjectDN().getName());

                // Just print Globus style for now
                debug.println("    DN: " +
                        BouncyCastleUtil.getIdentity(certs[i].getCert()));
                debug.println("  Hash: " + SecurityUtil.caNameHash(
                                           certs[i].getCert().getSubjectDN()));
            }
        }

        pr.println();

        if (configuredCAHash != null) {
            
            int numfound = 0;

            for (int i = 0; i < certs.length; i++) {
                final String hash = SecurityUtil.caNameHash(
                                           certs[i].getCert().getSubjectDN());

                if (configuredCAHash.equals(hash)) {
                    pr.println("Found configured cloud CA: '" +
                                            certs[i].getAbsolutePath() + "'");
                    if (debug != null) {
                        debug.println("   (DN: '" + BouncyCastleUtil.
                                getIdentity(certs[i].getCert()) + "')");
                    }
                    numfound += 1;
                }
            }

            if (numfound == 0) {
                pr.println("\nWarning: cloud CA was configured (hash '" +
                       configuredCAHash + "') but was not matched to any CA " +
                       "certificates in the trusted certificate directories.");
            } else {
                pr.println("\nNo warnings.");
            }

            if (numfound > 1 && debug != null) {
                debug.println("Found multiple cloud CA files (" +
                        numfound + " of them).");
            }

        } else if (debug != null) {
            debug.println("\nNo configured remote CA hash");
        }
    }
}
