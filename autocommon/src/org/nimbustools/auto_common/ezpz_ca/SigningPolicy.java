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

import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.globus.gsi.CertUtil;

import javax.security.auth.x500.X500Principal;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.File;
import java.io.OutputStreamWriter;
import java.security.cert.X509Certificate;
import java.security.Security;

public class SigningPolicy {

	private static final String PREFIX = 
			"# ca-signing-policy.conf, see ca-signing-policy.doc for more information\n" +
			"#\n" +
			"# This is the configuration file describing the policy for what CAs are\n" +
			"# allowed to sign whoses certificates.\n" +
			"#\n" +
			"# This file is parsed from start to finish with a given CA and subject\n" +
			"# name.\n" +
			"# subject names may include the following wildcard characters:\n" +
			"#    *    Matches any number of characters.\n" +
			"#    ?    Matches any single character.\n" +
			"#\n" +
			"# CA names must be specified (no wildcards). Names containing whitespaces\n" +
			"# must be included in single quotes, e.g. 'Certification Authority'.\n" +
			"# Names must not contain new line symbols.\n" +
			"# The value of condition attribute is represented as a set of regular\n" +
			"# expressions. Each regular expression must be included in double quotes.\n" +
			"#\n" +
			"# This policy file dictates the following policy:\n" +
			"#   -The Globus CA can sign Globus certificates\n" +
			"#\n" +
			"# Format:\n" +
			"#------------------------------------------------------------------------\n" +
			"#  token type  | def.authority |                value\n" +
			"#--------------|---------------|-----------------------------------------\n" +
			"# EACL entry #1|\n";

	private static final String SUFFIX = "\n\n# end of EACL\n\n";

	public static String getPolicyString(String caCertPath) throws Exception {

		final X509Certificate cert;
		final FileReader fr = new FileReader(caCertPath);
		try {
			Security.addProvider(new BouncyCastleProvider());
			final PEMReader reader =
					new PEMReader(fr, null, BouncyCastleProvider.PROVIDER_NAME);
			try {
				cert = (X509Certificate) reader.readObject();
			} finally {
				reader.close();
			}
		} finally {
			fr.close();
		}

		// access_id_CA
		final X500Principal subjectDN = cert.getSubjectX500Principal();
        final String DN = subjectDN.getName(X500Principal.RFC2253);
		final String access_id_CA = CertUtil.toGlobusID(DN, false);

		// cond_subjects
		final String signingtarget = EzPzCA.deriveSigningTargetString(cert);
		final String cond_subjectsRFC2253 = EzPzCA.getTargetDNfromSchema(signingtarget, "*");
		final String cond_subjects = CertUtil.toGlobusID(cond_subjectsRFC2253, true);


		final StringBuilder sb = new StringBuilder(PREFIX);
		sb.append("\n\n access_id_CA      X509         '");
	    sb.append(access_id_CA);
	    sb.append("'\n\n pos_rights        globus        CA:sign\n\n cond_subjects     globus       '\"");
		sb.append(cond_subjects);
		sb.append("\"'\n\n");
		sb.append(SUFFIX);
		return sb.toString();
	}



  	// -------------------------------------------------------------------------
    // MAIN
    // -------------------------------------------------------------------------

    public static void main(String[] args) {

        if (args == null || args.length != 2) {
            System.err.println("Needs these arguments:\n" +
                    "1 - the CA cert file you want signing policy for\n" +
					"2 - the target file (must not exist)");
            System.exit(1);
        }

        try {

			final File target = new File(args[1]);
			if (target.exists()) {
				throw new Exception("File already exists: " + target.getAbsolutePath());
			}

            final String policy = SigningPolicy.getPolicyString(args[0]);
			final OutputStreamWriter osw = new FileWriter(target);
			try {
				osw.write(policy);
			} finally {
				osw.close();
			}
			
        } catch (Exception e) {
            System.err.println("Problem: " + e.getMessage());
			e.printStackTrace();
            System.exit(1);
        }
    }
}
