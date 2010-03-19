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
package org.nimbustools.messaging.rest;

import org.nimbustools.messaging.rest.repr.User;
import org.nimbustools.messaging.rest.repr.AccessKey;

import java.util.List;

public interface UsersService {

    /**
     * Retrieve a list of all users
     * @return List of users
     */
    List<User> getUsers();

    /**
     * Retrieve a specific user by ID
     * @param id ID of user to retrieve
     * @return The user
     * @throws UnknownUserException A user with specified ID does not exist
     */
    User getUserById(String id) throws UnknownUserException;

    /**
     * Add a new user
     * @param user The user
     * @return
     * @throws DuplicateUserException User already exists
     */
    User addUser(User user) throws DuplicateUserException;

    /**
     * Retrieve the access key for a user
     * @param user The user
     * @return The access key
     * @throws UnknownKeyException User does not have an access key
     */
    AccessKey getAccessKey(User user) throws UnknownKeyException;

    /**
     * Creates an access key for user. If a key exists, it will
     * be discarded and replaced with a new one.
     * @param user The user
     * @return New access key
     */
    AccessKey createAccessKey(User user);
}
