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
package org.nimbustools.auto_common.ezpz_ca;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMReader;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.KeyPair;
import java.io.*;

/**
 * Creates a Java Keystore from PEM encoded cert and private key
 */
public class KeystoreFromPEM {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static KeyStore createJavaKeystore(X509Certificate cert, PrivateKey key, String password)
            throws Exception {

        KeyStore store = KeyStore.getInstance("JKS", "SUN");
        store.load(null, password.toCharArray());
        store.setKeyEntry("", key, password.toCharArray(),
                new Certificate[] {cert});

        return store;
    }

    public static void createJavaKeystore(File certFile, File keyFile,
                                          File keystoreFile, String password)
            throws Exception {

        X509Certificate cert = (X509Certificate) readPemObject(certFile);
        KeyPair keypair = (KeyPair) readPemObject(keyFile);
        KeyStore store = createJavaKeystore(cert, keypair.getPrivate(), password);
        OutputStream outStream = new FileOutputStream(keystoreFile);
        try {
            store.store(outStream, password.toCharArray());
        } finally {
            outStream.close();
        }
    }

    private static Object readPemObject(File file) throws IOException {
        FileReader reader = new FileReader(file);
        try {
            PEMReader pemReader = new PEMReader(reader, null, BouncyCastleProvider.PROVIDER_NAME);
            return pemReader.readObject();
        } finally {
            reader.close();
        }
    }

    public static void main(String[] args) {

        if (args == null || args.length != 4) {
            System.err.println("Needs these arguments:\n" +
                    "1 - the certificate file\n" +
                    "2 = the private key file\n" +
                    "3 - the destination file\n" +
                    "4 - the keystore password\n"
            );
            System.exit(1);
        }

        try {
            File certFile = new File(args[0]);
            File keyFile = new File(args[1]);
            File keystoreFile = new File(args[2]);
            String password = args[3];

            if (keystoreFile.exists()) {
                throw new Exception("keystore file already exists!");
                //TODO maybe it would be better to add to existing keystore?
            }
            
            createJavaKeystore(certFile, keyFile, keystoreFile, password);

        } catch (Throwable t) {
            System.err.println("Problem: " + t.getMessage());
			t.printStackTrace();
            System.exit(1);
        }
    }
}
