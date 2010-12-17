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

import org.globus.workspace.creation.IdempotentReservation;

public class IdempotentReservationImpl implements IdempotentReservation {

    private String creatorId;
    private String clientToken;
    private String groupId;
    private String vmId;
    private boolean isNew;

    public IdempotentReservationImpl(String creatorId, String clientToken, String groupId, String vmId, boolean aNew) {
        this.creatorId = creatorId;
        this.clientToken = clientToken;
        this.groupId = groupId;
        this.vmId = vmId;
        isNew = aNew;
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

    public String getVMId() {
        return vmId;
    }

    public boolean isNew() {
        return isNew;
    }

    public boolean isComplete() {
        return groupId != null || vmId != null;
    }
}
