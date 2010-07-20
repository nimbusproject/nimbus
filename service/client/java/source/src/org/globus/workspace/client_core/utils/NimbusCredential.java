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

package org.globus.workspace.client_core.utils;

import org.globus.gsi.GlobusCredential;
import org.globus.gsi.GlobusCredentialException;
import org.globus.gsi.gssapi.GlobusGSSCredentialImpl;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;

public class NimbusCredential {

    public static final String NIMBUS_UNENCRYPTED_USER_CERT = "NIMBUS_UNENCRYPTED_USER_CERT";
    public static final String NIMBUS_UNENCRYPTED_USER_KEY = "NIMBUS_UNENCRYPTED_USER_KEY";


    public static void setUnencryptedCredential(String usercertPath, String userkeyPath)
            throws Exception {
        setNimbusCredentialPath(usercertPath);
        setNimbusUnencryptedKeyPath(userkeyPath);
    }

    public static void setNimbusCredentialPath(String usercertPath) throws Exception {
        if (usercertPath == null || usercertPath.trim().length() == 0) {
            throw new Exception("Invalid nimbus certificate path: " + usercertPath);
        }
        System.setProperty(NIMBUS_UNENCRYPTED_USER_CERT, usercertPath);
    }

    public static void setNimbusUnencryptedKeyPath(String userkeyPath) throws Exception {
        if (userkeyPath == null || userkeyPath.trim().length() == 0) {
            throw new Exception("Invalid nimbus key path: " + userkeyPath);
        }
        System.setProperty(NIMBUS_UNENCRYPTED_USER_KEY, userkeyPath);
    }
    
    /**
     * If Nimbus certificate is configured in properties, return GSSCredential object of it.
     * @return credential to use or null
     * @throws org.globus.gsi.GlobusCredentialException problem loading the desired certificate
     * @throws org.ietf.jgss.GSSException problem loading the desired certificate
     */
    public static GSSCredential getGSSCredential()
            throws GlobusCredentialException, GSSException {

        final GlobusCredential credential = getGlobusCredential();
        if (credential == null) {
            return null;
        }
        return new GlobusGSSCredentialImpl(credential, GSSCredential.INITIATE_ONLY);
    }

    /**
     * If Nimbus certificate is configured in properties, return GlobusCredential object of it.
     * @return credential to use or null
     * @throws org.globus.gsi.GlobusCredentialException problem loading the desired certificate
     */
    public static GlobusCredential getGlobusCredential()
            throws GlobusCredentialException {

        final String usercertPath = getNimbusCertificatePath();
        if (usercertPath == null) {
            return null;
        }
        final String userkeyPath = getNimbusUnencryptedKeyPath();
        if (userkeyPath == null) {
            return null;
        }
        return new GlobusCredential(usercertPath, userkeyPath);
    }

    public static String getNimbusCertificatePath() {
        final String usercertPath = System.getProperty(NIMBUS_UNENCRYPTED_USER_CERT);
        if (usercertPath == null || usercertPath.trim().length() == 0) {
            return null;
        }
        return usercertPath;
    }

    public static String getNimbusUnencryptedKeyPath() {
        final String userkeyPath = System.getProperty(NIMBUS_UNENCRYPTED_USER_KEY);
        if (userkeyPath == null || userkeyPath.trim().length() == 0) {
            return null;
        }
        return userkeyPath;
    }
}
