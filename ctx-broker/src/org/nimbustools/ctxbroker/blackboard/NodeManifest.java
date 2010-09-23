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
package org.nimbustools.ctxbroker.blackboard;

import org.nimbustools.ctxbroker.Identity;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public class NodeManifest {

    private List<Identity> identities;
    private List<DataPair> data;
    private List<RoleIdentityPair> requiredRoles;

    public NodeManifest(List<Identity> identities,
                        List<DataPair> data,
                        List<RoleIdentityPair> requiredRoles) {
        if (identities == null) {
            throw new IllegalArgumentException("identities may not be null");
        }
        if (data == null) {
            throw new IllegalArgumentException("data may not be null");
        }
        if (requiredRoles == null) {
            throw new IllegalArgumentException("requiredRoles may not be null");
        }

        this.identities = Collections.unmodifiableList(
                new ArrayList<Identity>(identities));

        this.data = Collections.unmodifiableList(
                new ArrayList<DataPair>(data));

        this.requiredRoles = Collections.unmodifiableList(
                new ArrayList<RoleIdentityPair>(requiredRoles));

    }

    public List<Identity> getIdentities() {
        return identities;
    }

    public List<DataPair> getData() {
        return data;
    }

    public List<RoleIdentityPair> getRequiredRoles() {
        return requiredRoles;
    }
}
