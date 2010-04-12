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

import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.io.*;
import java.util.Arrays;

/**
 * Creates a Java Keystore from PEM encoded cert and private key
 */
public class KeystoreFromPEM {

    public final static String ENTRY_ALIAS = "";

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static KeyStore createJavaKeystore(X509Certificate cert, PrivateKey key, String password)
            throws Exception {

        KeyStore store = KeyStore.getInstance("JKS", "SUN");
        store.load(null, password.toCharArray());
        store.setKeyEntry(ENTRY_ALIAS, key, password.toCharArray(),
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

    public static boolean checkJavaKeystore(File certFile, File keyFile,
                                          File keystoreFile, String password) throws Exception {
        X509Certificate cert = (X509Certificate) readPemObject(certFile);
        KeyPair keypair = (KeyPair) readPemObject(keyFile);
        PrivateKey privateKey = keypair.getPrivate();
        KeyStore store = KeyStore.getInstance("JKS", "SUN");
        final char[] passwordChars = password.toCharArray();

        InputStream inStream = new FileInputStream(keystoreFile);
        try {
            store.load(inStream, passwordChars);
        } finally {
            inStream.close();
        }
        final Certificate curCert = store.getCertificate(ENTRY_ALIAS);
        if (curCert == null ||
                !Arrays.equals(curCert.getEncoded(), cert.getEncoded())) {
            return false;
        }
        final Key curKey = store.getKey(ENTRY_ALIAS, passwordChars);
        return curKey != null &&
                Arrays.equals(curKey.getEncoded(), privateKey.getEncoded());
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
                if (checkJavaKeystore(certFile, keyFile,
                                keystoreFile, password)) {
                    System.exit(0);
                } else {
                    System.err.println("The keystore exists but does not " +
                            "contain the correct key and certificate");
                    System.exit(2);
                }
            }
            
            createJavaKeystore(certFile, keyFile, keystoreFile, password);

        } catch (Throwable t) {
            System.err.println("Problem: " + t.getMessage());
			t.printStackTrace();
            System.exit(1);
        }
    }
}
