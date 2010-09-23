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

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.io.File;

public class GenerateNewCA {

    // five year CA cert
    public static final int VALIDITY_MONTHS = 60;

    // -------------------------------------------------------------------------
    // REPLACE
    // -------------------------------------------------------------------------

    public void generateCA(String basename,
                           String caDirPath) throws Exception {

        if (basename == null) {
            throw new IllegalArgumentException("basename may not be null");
        }
        if (caDirPath == null) {
            throw new IllegalArgumentException("caDirPath may not be null");
        }

        final CAFactory caFactory = new CAFactory();

        final KeyPair keyPair = caFactory.createNewKeyPair();
        final X509Certificate caCert =
                caFactory.create(basename, VALIDITY_MONTHS, keyPair);
        if (caCert == null) {
            throw new Exception(
                    "No certificate authority certificate was created?");
        }

        caCert.checkValidity();

        final String pubpath = caDirPath + File.separator + basename + ".pem";
        final String privpath =
                caDirPath + File.separator + "private-key-" + basename + ".pem";
        new CertWriter().writeCert(caCert, keyPair, pubpath, privpath);
    }


    // -------------------------------------------------------------------------
    // MAIN
    // -------------------------------------------------------------------------

    public static void main(String[] args) {

        if (args == null || args.length != 2) {
            System.err.println("Needs these arguments:\n" +
                    "1 - the ca directory path (created already)\n" +
                    "2 - the base name of ca");
            System.exit(1);
        }

        try {
            new GenerateNewCA().generateCA(args[1], args[0]);
        } catch (Exception e) {
            System.err.println("Problem creating CA: " + e.getMessage());
            System.exit(1);
        }
    }
}
