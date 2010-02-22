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
package org.nimbustools.messaging.rest;

import org.nimbustools.messaging.rest.repr.User;
import org.nimbustools.messaging.rest.repr.AccessKey;
import org.nimbustools.auto_common.ezpz_ca.HashUtil;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.core.codec.Base64;
import org.mortbay.util.QuotedStringTokenizer;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.util.*;
import java.io.*;
import java.security.NoSuchAlgorithmException;

public class GridmapUsersService implements UsersService, InitializingBean {

    private static final String DEFAULT_LOCAL_USERNAME = "not_a_real_user";

    private final Object lock = new Object();

    private String localUserName;

    private Resource gridmapResource;
    private Resource groupAuthzResource;
    private Resource queryUsersResource;

    private TokenizedFile gridmap;
    private TokenizedFile queryUsers;

    /**
     * Retrieve a list of all users
     *
     * @return List of users
     */
    public List<User> getUsers() {

        List<User> users = new ArrayList<User>();

        synchronized (this.lock) {
            refreshIfNeeded(gridmap);
            for (String[] line : this.gridmap.getLines()) {
                String dn = line[0];
                String lineId = getUserId(dn);

                User user = new User();
                user.setId(lineId);
                user.setDn(dn);
                users.add(user);
            }
        }
        return users;
    }

    /**
     * Retrieve a specific user by ID
     *
     * @param id ID of user to retrieve
     * @return The user
     * @throws UnknownUserException
     *          A user with specified ID does not exist
     */
    public User getUserById(String id) throws UnknownUserException {

        synchronized (this.lock) {
            refreshIfNeeded(gridmap);
            String[] line = gridmap.getLinesMap().get(id);
            if (line == null) {
                throw new UnknownUserException();
            }
            User user = new User();
            user.setId(id);
            user.setDn(line[0]);
            return user;
        }
    }

    private void refreshIfNeeded(TokenizedFile file) {
        try {
            file.refreshIfNeeded();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Add a new user
     *
     * @param user The user
     * @return The new user, with ID
     * @throws DuplicateUserException
     *          User already exists
     */
    public User addUser(User user) throws DuplicateUserException {

        String dn = user.getDn().trim();
        final String userId = getUserId(dn);

        synchronized (this.lock) {
            if (gridmap.getLinesMap().containsKey(userId)) {
                throw new DuplicateUserException("DN '"+dn+
                        "' already exists in gridmap");
            }

            final String gridmapLine = "\""+ dn +"\" " + this.localUserName;
            appendLineToResource(gridmapResource, gridmapLine);
            if (groupAuthzResource != null) {
                appendLineToResource(groupAuthzResource, gridmapLine);
            }
        }

        user = new User();
        user.setDn(dn);
        user.setId(userId);
        return user;
    }

    private static void appendLineToResource(Resource resource, String line) {
        try {
            final File file = resource.getFile();
            FileOutputStream f = new FileOutputStream(file, true);
            OutputStreamWriter writer = new OutputStreamWriter(f);
            writer.write(line+"\n");
            writer.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieve the access key for a user
     *
     * @param user The user
     * @return The access key
     * @throws UnknownKeyException
     *          User does not have an access key
     */
    public AccessKey getAccessKey(User user)
            throws UnknownKeyException {

        if (user == null) {
            throw new IllegalArgumentException("user may not be null");
        }

        final String userId = user.getId();

        if (userId == null) {
            throw new IllegalArgumentException("user's ID may not be null");
        }

        synchronized (this.lock) {
            final String[] line = this.queryUsers.getLinesMap().get(userId);
            if (line == null) {
                throw new UnknownKeyException();
            }
            if (line.length < 3) {
                throw new UnknownKeyException();
            }
            AccessKey accessKey = new AccessKey();
            accessKey.setKey(line[1]);
            accessKey.setSecret(line[2]); //uhhhhh
            accessKey.setEnabled(true);
            return accessKey;
        }
    }

    /**
     * Creates an access key for user. If a key exists, it will
     * be discarded and replaced with a new one.
     *
     * @param user The user
     * @return New access key
     */
    public AccessKey createAccessKey(User user) {

        if (user == null) {
            throw new IllegalArgumentException("user may not be null");
        }

        final String dn = user.getDn();

        if (dn == null) {
            throw new IllegalArgumentException("user's DN may not be null");
        }

        final String accessId;
        try {
            accessId = getHashUtil().hashDN(dn);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        final String secret = generateSecretKey();

        final String line = "\""+dn+"\" "+accessId+" "+secret;

        appendLineToResource(this.queryUsersResource, line);

        AccessKey accessKey = new AccessKey();
        accessKey.setEnabled(true);
        accessKey.setKey(accessId);
        accessKey.setSecret(secret);
        return accessKey;
    }

    private String generateSecretKey() {
        final KeyGenerator keyGen;
        try {
            keyGen = KeyGenerator.getInstance("HmacSHA1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        keyGen.init(256);
        final SecretKey key = keyGen.generateKey();
        return new String(Base64.encode(key.getEncoded()));
    }

    public void initialize() throws Exception {
        this.gridmap = new TokenizedFile(this.gridmapResource.getFile());
        this.gridmap.refreshIfNeeded();
        this.queryUsers = new TokenizedFile(this.queryUsersResource.getFile());
        this.queryUsers.refreshIfNeeded();

        if (this.groupAuthzResource != null) {
            if (!this.groupAuthzResource.exists()) {
                throw new Exception(
                        "groupAuthz file resource specified doesn't exist: "+
                        groupAuthzResource.getFilename());
            }
        }
    }

    public Resource getGridmapResource() {
        return gridmapResource;
    }

    public void setGridmapResource(Resource gridmapResource) {
        this.gridmapResource = gridmapResource;
    }

    public Resource getGroupAuthzResource() {
        return groupAuthzResource;
    }

    public void setGroupAuthzResource(Resource groupAuthzResource) {
        this.groupAuthzResource = groupAuthzResource;
    }

    public Resource getQueryUsersResource() {
        return queryUsersResource;
    }

    public void setQueryUsersResource(Resource queryUsersResource) {
        this.queryUsersResource = queryUsersResource;
    }

    public String getLocalUserName() {
        return localUserName;
    }

    public void setLocalUserName(String localUserName) {
        this.localUserName = localUserName;
    }

    public void afterPropertiesSet() throws Exception {
        if (this.gridmapResource == null) {
            throw new IllegalArgumentException("this.gridmapResource may not be null");
        }
        if (this.queryUsersResource == null) {
            throw new IllegalArgumentException("this.queryUsersResource may not be null");
        }

        if (this.localUserName == null || this.localUserName.trim().length() == 0) {
            this.localUserName = DEFAULT_LOCAL_USERNAME;
        }

        this.initialize();
    }

    static String getUserId(String dn) {
        return getUserId(dn, getHashUtil());
    }

    static String getUserId(String dn, HashUtil hashUtil) {
        try {
            return "0-"+hashUtil.hashDN(dn);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    static HashUtil getHashUtil() {
        final HashUtil hashUtil;
        try {
            hashUtil = new HashUtil();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        return hashUtil;
    }

    /**
     * Works with a special file format: DN followed by zero or more
     * additional fields, delimited by whitespace.
     * Gridmap or query users files, for example. The DN is hashed
     * into a user ID for quick lookups.
     */
    private static class TokenizedFile {

        private static final String DELIMS = " \t\n\r\f";

        final File file;
        private long lastModified;

        final List<String[]> list;
        final Map<String,String[]> map;

        public TokenizedFile(File file) {
            if (file == null) {
                throw new IllegalArgumentException("file may not be null");
            }
            this.file = file;

            this.list = new ArrayList<String[]>();
            this.map = new HashMap<String, String[]>();
        }

        public boolean refreshIfNeeded() throws IOException {
            if (this.file.lastModified() > this.lastModified) {
                load(this.file);
                return true;
            }
            return false;
        }

        public List<String[]> getLines() {
            return this.list;
        }

        public Map<String, String[]> getLinesMap() {
            return this.map;
        }

        private void load(File file) throws IOException {

            if (file == null) {
                throw new IllegalArgumentException("file may not be null");
            }

            InputStream in = null;
            try {
                in = new FileInputStream(file);
                this.lastModified = file.lastModified();
                load(in);
            } finally {
                if (in != null) {
                    try { in.close(); }
                    catch(Exception ignored) {
                    }
                }
            }
        }

        private void load(InputStream stream) throws IOException {
            final BufferedReader reader =
                    new BufferedReader(new InputStreamReader(stream));

            this.list.clear();
            this.map.clear();

            final HashUtil hashUtil = GridmapUsersService.getHashUtil();

            String line;
            final List<String> tokens = new ArrayList<String>();
            while ((line = reader.readLine()) != null) {
                tokens.clear();
                line = line.trim();

                if (line.length() == 0 || line.charAt(0) == '#') {
                    continue;
                }

                final QuotedStringTokenizer tokenizer =
                        new QuotedStringTokenizer(line, DELIMS);

                try {
                    while (tokenizer.hasMoreTokens()) {
                        tokens.add(tokenizer.nextToken());
                    }

                } catch (NoSuchElementException e) {
                    continue;
                }

                String[] tokensArray = new String[tokens.size()];
                tokensArray = tokens.toArray(tokensArray);

                String userId = GridmapUsersService.getUserId(
                        tokensArray[0], hashUtil);

                this.list.add(tokensArray);
                this.map.put(userId, tokensArray);
            }
        }
    }
}
