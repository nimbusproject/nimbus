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
package org.globus.workspace.creation;

import edu.emory.mathcs.backport.java.util.concurrent.locks.Lock;
import org.globus.workspace.service.InstanceResource;
import org.nimbustools.api.services.rm.ManageException;

import java.util.List;


/**
 * Provides methods for maintaining idempotent launch state
 *
 * Use of this class MUST be synchronized because of check-then-act semantics of
 * getReservation() and addReservation(). getLock() provides locks for each unique
 * caller/clientToken pair but you could also synchronize on the implementing class
 * directly.
 */
public interface IdempotentCreationManager {

    /**
     * Acquires a lock for the specific request pair
     * @param creatorID the external caller ID
     * @param clientToken the client-provided idempotency request token
     * @return a Lock to synchronize on for the specified request
     */
    Lock getLock(String creatorID, String clientToken);

    /**
     * Returns a lock to allow reference-counting implementations to free it
     * @param creatorID the external caller ID
     * @param clientToken the client-provided idempotency request token
     */
    void returnLock(String creatorID, String clientToken);


    /**
     * Gets an existing idempotent reservation, if any
     *
     * This method MUST be called under synchronization relative to addReservation().
     *
     *
     * @param creatorID the external caller ID
     * @param clientToken the client-provided idempotency request token
     * @return an existing reservation, or null if none exists
     */
    IdempotentReservation getReservation(String creatorID, String clientToken);

    /**
     * Adds a new idempotent reservation
     * @param creatorID the external caller ID
     * @param clientToken the client-provided idempotency request token
     * @param resources the acquired resources
     */
    void addReservation(String creatorID, String clientToken, List<InstanceResource> resources);

    /**
     * Remove an existing idempotency reservation
     * @param creatorID the external caller ID
     * @param clientToken the client-provided idempotency request token
     * @throws ManageException failed to remove reservation
     */
    void removeReservation(String creatorID, String clientToken)
            throws ManageException;

}
