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
package org.globus.workspace.creation.defaults;

import edu.emory.mathcs.backport.java.util.concurrent.locks.Lock;
import org.globus.workspace.DefaultLockManager;
import org.globus.workspace.LockManager;
import org.globus.workspace.creation.IdempotentCreationManager;
import org.globus.workspace.creation.IdempotentReservation;
import org.globus.workspace.service.InstanceResource;
import org.nimbustools.api.services.rm.DoesNotExistException;
import org.nimbustools.api.services.rm.ManageException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class IdempotentCreationManagerImpl implements IdempotentCreationManager {

    private LockManager lockManager;

    private ConcurrentMap<String, IdempotentReservation> reservationMap;

    public IdempotentCreationManagerImpl() {
        this.lockManager = new DefaultLockManager();
        this.reservationMap = new ConcurrentHashMap<String, IdempotentReservation>();

    }

    public Lock getLock(String creatorID, String clientToken) {
        if (creatorID == null) {
            throw new IllegalArgumentException("creatorID may not be null");
        }
        if (clientToken == null) {
            throw new IllegalArgumentException("clientToken may not be null");
        }

        final String key = creatorID + "|" + clientToken;
        return this.lockManager.getLock(key);
    }

    public IdempotentReservation getOrCreateReservation(String creatorID, String clientToken) {
        if (creatorID == null) {
            throw new IllegalArgumentException("creatorID may not be null");
        }
        if (clientToken == null) {
            throw new IllegalArgumentException("clientToken may not be null");
        }

        final String key = creatorID + "|" + clientToken;
        IdempotentReservation reservation = this.reservationMap.get(key);
        if (reservation == null) {
            this.reservationMap.put(key,
                    new IdempotentReservationImpl(creatorID, clientToken, null, null, false));

            return new IdempotentReservationImpl(creatorID, clientToken, null, null, true);
        } else {
            return reservation;
        }
    }

    public void completeReservation(String creatorID, String clientToken, InstanceResource[] resources)
            throws DoesNotExistException {

        if (creatorID == null) {
            throw new IllegalArgumentException("creatorID may not be null");
        }
        if (clientToken == null) {
            throw new IllegalArgumentException("clientToken may not be null");
        }
        if (resources == null || resources.length == 0) {
            throw new IllegalArgumentException("resources may not be null or empty");
        }

        final String key = creatorID + "|" + clientToken;
        IdempotentReservation reservation = this.reservationMap.get(key);
        if (reservation == null) {
            throw new DoesNotExistException("Idempotency reservation not found");
        }

        String groupId = null;
        String vmId = null;
        if (resources.length > 1) {
            groupId = resources[0].getGroupId();
        } else {
            vmId = String.valueOf(resources[0].getID());
        }

        this.reservationMap.put(key,
                new IdempotentReservationImpl(creatorID, clientToken, groupId, vmId, false));
    }

    public void removeReservation(String creatorID, String clientToken)
            throws ManageException {
        final String key = creatorID + "|" + clientToken;

        this.reservationMap.remove(key);
    }
}
