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

import java.security.cert.X509Certificate;

import org.globus.gsi.CertUtil;

public class CertDN {

    public static String dnFromPath(String path) {
        X509Certificate cert = null;

        try {
            cert = CertUtil.loadCertificate(path);
        } catch(Exception e) {
            System.err.println("Unable to load the certificate : " + e.getMessage());
            System.exit(1);
        }

        return CertUtil.toGlobusID(cert.getSubjectDN());
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
