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

import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERInputStream;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.jce.X509V3CertificateGenerator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;

public class CAFactory {

    private final KeyPairGenerator kpGen;
    private final X509V3CertificateGenerator certGen;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public CAFactory() throws Exception {
        this.kpGen = KeyPairGenerator.getInstance("RSA", "BC");
        this.kpGen.initialize(1024, new SecureRandom());
        this.certGen = new X509V3CertificateGenerator();
    }

    public X509Certificate create(String baseName,
                                  int months, 
                                  KeyPair keyPair) throws Exception {

        final X509Principal newprincipal =
                new X509Principal("O=Auto,OU=" + baseName + ",CN=CA");
        
        this.certGen.reset();

        /*
          "The entity that created the certificate is responsible for  assigning
          it a serial number to distinguish it from other certificates it issues.
          This information is used in numerous ways, for example when a
          certificate is revoked its serial number is placed in a Certificate
          Revocation List (CRL)"
        */
        this.certGen.setSerialNumber(BigInteger.ZERO);

        final Calendar expires = Calendar.getInstance();
		expires.add(Calendar.MONTH, months);
        this.certGen.setNotBefore(new Date(System.currentTimeMillis() - 10000));
        this.certGen.setNotAfter(expires.getTime());

        this.certGen.setSubjectDN(newprincipal);
        this.certGen.setIssuerDN(newprincipal);
        this.certGen.setSignatureAlgorithm("SHA1withRSA");

        final PublicKey pubkey = keyPair.getPublic();
        this.certGen.setPublicKey(pubkey);

        // begin X509/BC security nastiness, not sure these are the very best
        // choices but it is working...

        final ByteArrayInputStream in =
                new ByteArrayInputStream(pubkey.getEncoded());
        final SubjectPublicKeyInfo spki =
                new SubjectPublicKeyInfo(
                        (ASN1Sequence)new DERInputStream(in).readObject());
        final SubjectKeyIdentifier ski = new SubjectKeyIdentifier(spki);

        final ByteArrayInputStream in2 =
                new ByteArrayInputStream(newprincipal.getEncoded());
        final GeneralNames generalNames = new GeneralNames(
                (ASN1Sequence)new DERInputStream(in2).readObject());
        final AuthorityKeyIdentifier aki =
                new AuthorityKeyIdentifier(spki, generalNames, BigInteger.ZERO);


        this.certGen.addExtension(X509Extensions.BasicConstraints,
                                 true,
                                 new BasicConstraints(true));

        /*
        this.certGen.addExtension(X509Extensions.KeyUsage,
                                  true,
                                  new KeyUsage(KeyUsage.digitalSignature |
                                               KeyUsage.keyEncipherment));
        */

        this.certGen.addExtension(X509Extensions.SubjectKeyIdentifier,
                                  false,
                                  ski);
        
        this.certGen.addExtension(X509Extensions.AuthorityKeyIdentifier,
                                  false,
                                  aki);

        this.certGen.addExtension(X509Extensions.KeyUsage,
                                 false,
                                 new KeyUsage(KeyUsage.cRLSign |
                                              KeyUsage.keyCertSign));

        return this.certGen.generateX509Certificate(keyPair.getPrivate());
    }

    public KeyPair createNewKeyPair() {
        return this.kpGen.generateKeyPair();
    }

    /*
     * mini test
     */
    public static void main(String[] args) throws Exception {
        final CAFactory caFactory = new CAFactory();
        final X509Certificate caCert =
                caFactory.create("321lfn432oifno", 24,
                                 caFactory.createNewKeyPair());
        caCert.checkValidity();
        System.out.println(caCert.getSubjectDN().toString());
    }
}
