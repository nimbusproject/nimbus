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
package org.nimbustools.auto_config;


import org.globus.workspace.groupauthz.HashUtil;
import org.globus.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.KeyGenerator;
import java.util.Properties;
import java.io.*;
import java.security.NoSuchAlgorithmException;

public class QueryUserAdd {
    private static final String QUERY_USERMAP_PATH = "query.usermap.path";

    private File userMapFile;

    public QueryUserAdd(String queryConfPath) throws Exception {
        Properties props = new Properties();
        props.load(new FileInputStream(queryConfPath));

        final String userMapPath = (String)props.get(QUERY_USERMAP_PATH);

        File f = new File(userMapPath);
        if (!f.exists() || !f.canRead()) {
            throw new Exception("Query user file ("+userMapPath+
                    ") does not exist or is not readable");
        }

        this.userMapFile = f;

    }

    public UserPair add(String dn) throws NoSuchAlgorithmException, IOException {
        final String hash = HashUtil.hashDN(dn);

        final KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA1");
        keyGen.init(256);
        final SecretKey key = keyGen.generateKey();

        final String secret = new String(Base64.encode(key.getEncoded()));

        FileOutputStream f = new FileOutputStream(userMapFile, true);
        OutputStreamWriter writer = new OutputStreamWriter(f);
        writer.write("\""+dn+"\" "+hash+" "+secret+"\n");
        writer.close();


        return new UserPair(hash, secret);
    }


    public static void mainImpl(String[] args) throws Exception {
        if (args == null || args.length != 2) {
            throw new Exception(
                    "You need to supply three and only three arguments:"
                  + "\n  1 - path to existing query.conf file"
                  + "\n  2 - DN");
        }

        final QueryUserAdd add = new QueryUserAdd(args[0]);
        final UserPair pair = add.add(args[1]);

        System.out.println(
                "Generated query credentials for user:\n"+
                "\tAccess ID: "+pair.getAccessID()+"\n"+
                "\tSecret key: "+pair.getSecret()+"\n"+
                "*Securely* distribute these tokens to the user.\n");
    }

    public static void main(String[] args) {
        try {
            mainImpl(args);
        } catch (Throwable t) {
            System.err.println("Problem: " + t.getMessage());
            System.exit(1);
        }
    }

    static class UserPair {
        private final String accessID;
        private final String secret;

        public UserPair(String accessID, String secret) {
            this.accessID = accessID;
            this.secret = secret;
        }

        public String getAccessID() {
            return accessID;
        }

        public String getSecret() {
            return secret;
        }
    }
}
