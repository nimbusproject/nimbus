package org.nimbustools.messaging.rest;/*
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

import org.nimbustools.messaging.rest.UsersService;
import org.nimbustools.messaging.rest.UnknownKeyException;
import org.nimbustools.messaging.rest.repr.User;
import org.nimbustools.messaging.rest.repr.AccessKey;
import org.springframework.security.core.codec.Base64;
import org.joda.time.DateTime;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.security.NoSuchAlgorithmException;

public class MockUsersService implements UsersService {

    HashMap<String,User> users = new HashMap<String, User>();
    HashMap<String,AccessKey> keys = new HashMap<String, AccessKey>();

    public List<User> getUsers() {
        return new ArrayList<User>(users.values());
    }

    public User getUserById(String id) throws UnknownUserException {
        final User user = users.get(id);

        if (user == null) {
            throw new UnknownUserException();
        }
        return user;
    }

    public User addUser(User user) {
        users.put(user.getId(), user);
        return user;
    }

    public AccessKey getAccessKey(User user) throws UnknownKeyException {
        AccessKey key = keys.get(user.getId());
        if (key == null) {
            throw new UnknownKeyException();
        }
        return key;
    }

    public AccessKey createAccessKey(User user) {
        final KeyGenerator keyGen;
        try {
            keyGen = KeyGenerator.getInstance("HmacSHA1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        keyGen.init(256);
        final SecretKey key = keyGen.generateKey();

        final String secret = new String(Base64.encode(key.getEncoded()));

        AccessKey accessKey = new AccessKey();
        accessKey.setKey(user.getId());
        accessKey.setSecret(secret);
        accessKey.setEnabled(true);
        accessKey.setCreationTime(new DateTime());

        keys.put(user.getId(), accessKey);

        return accessKey;
    }

}
