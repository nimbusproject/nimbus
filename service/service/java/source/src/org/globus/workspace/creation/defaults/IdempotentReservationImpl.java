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

import org.globus.workspace.creation.IdempotentInstance;
import org.globus.workspace.creation.IdempotentReservation;

import java.util.Collections;
import java.util.List;

public class IdempotentReservationImpl implements IdempotentReservation {

    private String creatorId;
    private String clientToken;
    private String groupId;
    private List<IdempotentInstance> instances;

    public IdempotentReservationImpl(String creatorId,
                                     String clientToken,
                                     String groupId,
                                     List<IdempotentInstance> instances) {
        if (creatorId == null) {
            throw new IllegalArgumentException("creatorId may not be null");
        }
        if (clientToken == null) {
            throw new IllegalArgumentException("clientToken may not be null");
        }
        if (instances == null) {
            throw new IllegalArgumentException("instances may not be null");
        }

        this.creatorId = creatorId;
        this.clientToken = clientToken;
        this.groupId = groupId;

        this.instances = Collections.unmodifiableList(instances);
        if (this.instances == null) {
            throw new IllegalArgumentException("instances may not be empty");
        }
    }

    public String getCreatorId() {
        return creatorId;
    }

    public String getClientToken() {
        return clientToken;
    }

    public String getGroupId() {
        return groupId;
    }

    public List<IdempotentInstance> getInstances() {
        return instances;
    }
}
