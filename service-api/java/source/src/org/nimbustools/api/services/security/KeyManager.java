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
package org.nimbustools.api.services.security;

import org.nimbustools.api.services.rm.OperationDisabledException;
import org.nimbustools.api.services.rm.DoesNotExistException;
import org.nimbustools.api.services.rm.AuthorizationException;
import org.nimbustools.api.repr.Caller;

import java.util.List;


public interface KeyManager {

    /***
     * Adds a new key to the manager
     * @param key key info
     * @param caller Requesting user
     * @throws KeyExistsException A key by the specified name already exists
     * @throws org.nimbustools.api.services.rm.OperationDisabledException backend does not support adding keys
     * @throws org.nimbustools.api.services.rm.AuthorizationException User is not authorized to manage keys
     */
    public void addKey(KeyPair key, Caller caller)
            throws AuthorizationException, OperationDisabledException, KeyExistsException;

    /***
     * Generates a new key on the server side
     * @param name name of the key to generate
     * @param caller Requesting user
     * @return full key pair, including private key
     * @throws org.nimbustools.api.services.rm.OperationDisabledException backend does not support server-side key gen
     * @throws KeyExistsException A key by the specified name already exists
     * @throws org.nimbustools.api.services.rm.AuthorizationException User is not authorized to manage keys
     */
    public KeyPair generateKey(String name, Caller caller)
            throws AuthorizationException, OperationDisabledException, KeyExistsException;

    /***
     * Removes an existing key from the manager
     * @param name name of key to remove
     * @param caller Requesting user
     * @throws org.nimbustools.api.services.rm.DoesNotExistException key does not exist
     * @throws org.nimbustools.api.services.rm.AuthorizationException User is not authorized to manage keys
     */
    public void removeKey(String name, Caller caller)
            throws AuthorizationException, DoesNotExistException;

    /***
     * List available keys
     * @return list of key pairs. name and fingerprint required, other fields are optional
     * @param caller Requesting user
     * @throws org.nimbustools.api.services.rm.AuthorizationException User is not authorized to manage keys
     */
    public List<KeyPair> listKeys(Caller caller) throws AuthorizationException;

}
