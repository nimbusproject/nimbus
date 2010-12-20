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
import org.globus.workspace.creation.IdempotentInstance;
import org.globus.workspace.creation.IdempotentReservation;
import org.globus.workspace.service.InstanceResource;
import org.nimbustools.api.services.rm.ManageException;

import java.util.ArrayList;
import java.util.List;
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

    public void returnLock(String creatorID, String clientToken) {
        // No implementation, we choose to leak references
    }

    public IdempotentReservation getReservation(String creatorID, String clientToken) {
        if (creatorID == null) {
            throw new IllegalArgumentException("creatorID may not be null");
        }
        if (clientToken == null) {
            throw new IllegalArgumentException("clientToken may not be null");
        }

        final String key = creatorID + "|" + clientToken;
        return this.reservationMap.get(key);
    }

    public void addReservation(String creatorID, String clientToken, List<InstanceResource> resources) {

        if (creatorID == null) {
            throw new IllegalArgumentException("creatorID may not be null");
        }
        if (clientToken == null) {
            throw new IllegalArgumentException("clientToken may not be null");
        }
        if (resources == null || resources.isEmpty()) {
            throw new IllegalArgumentException("resources may not be null or empty");
        }

        final String groupId = resources.get(0).getGroupId();

        ArrayList<IdempotentInstance> instances = new ArrayList<IdempotentInstance>(resources.size());
        for (InstanceResource resource : resources) {
            final IdempotentInstanceImpl instance = new IdempotentInstanceImpl(
                    resource.getID(),
                    resource.getName(),
                    resource.getLaunchIndex());

            instances.add(instance);
        }

        final IdempotentReservationImpl reservation =
                new IdempotentReservationImpl(creatorID, clientToken, groupId, instances);

        final String key = creatorID + "|" + clientToken;
        this.reservationMap.put(key,
                reservation);
    }

    public void removeReservation(String creatorID, String clientToken)
            throws ManageException {
        final String key = creatorID + "|" + clientToken;

        this.reservationMap.remove(key);
    }
}
