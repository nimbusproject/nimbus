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

import org.bouncycastle.asn1.DEREncodable;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.security.auth.x500.X500Principal;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class CertFilenameHash {

	private MessageDigest md5 = null;

	public CertFilenameHash() throws NoSuchAlgorithmException {
		this.md5 = MessageDigest.getInstance("MD5");
		Security.addProvider(new BouncyCastleProvider());
	}

	/**
	* Returns equivalent of:
	* openssl x509 -in "cert-file" -hash -noout
	*
	* @param subjectDN dn object
	* @return hash for certificate names
	* @throws java.io.IOException issue
	*/
	public String nameHash(Principal subjectDN) throws IOException {
		return hash(encodePrincipal(subjectDN));
	}

	public static byte[] encodePrincipal(Principal subject) throws IOException {
		if (subject instanceof X500Principal) {
			return ((X500Principal) subject).getEncoded();
		} else if (subject instanceof X509Name) {
			final ByteArrayOutputStream bout = new ByteArrayOutputStream();
			final DEROutputStream der = new DEROutputStream(bout);
			final DEREncodable nm = (DEREncodable) subject;
			der.writeObject(nm.getDERObject());
			return bout.toByteArray();
		} else {
			throw new ClassCastException("unsupported input class: "
					+ subject.getClass().toString());
		}
	}

	private String hash(byte[] data) {

		this.md5.reset();
		this.md5.update(data);

		final byte[] md = this.md5.digest();

		final long ret = (fixByte(md[0]) | (fixByte(md[1]) << 8L) |
				fixByte(md[2]) << 16L | fixByte(md[3]) << 24L) & 0xffffffffL;

		return Long.toHexString(ret);
	}

	private static long fixByte(byte b) {
		return (b < 0) ? (long) (b + 256) : (long) b;
	}

	public String hashFromPath(String existingFile)
			throws IOException, CertificateException, NoSuchProviderException {

		final File certFile = new File(existingFile);
        if (!certFile.canRead()) {
            final String msg = "File '" + existingFile + "' can not be read.";
            throw new IOException(msg);
        }

		final FileReader fr = new FileReader(certFile);
		try {
			final PEMReader reader =
					new PEMReader(fr, null, BouncyCastleProvider.PROVIDER_NAME);
			try {
				final X509Certificate cert = (X509Certificate) reader.readObject();
				return this.nameHash(cert.getSubjectDN());
			} finally {
				reader.close();
			}
		} finally {
			fr.close();
		}
	}


  	// -------------------------------------------------------------------------
    // MAIN
    // -------------------------------------------------------------------------

    public static void main(String[] args) {

        if (args == null || args.length != 1) {
            System.err.println("Needs these arguments:\n" +
                    "1 - the cert file you want hashed name of");
            System.exit(1);
        }

        try {
            final String newhex = new CertFilenameHash().hashFromPath(args[0]);
			System.out.println(newhex);
        } catch (Exception e) {
            System.err.println("Problem: " + e.getMessage());
			e.printStackTrace();
            System.exit(1);
        }
    }
}
