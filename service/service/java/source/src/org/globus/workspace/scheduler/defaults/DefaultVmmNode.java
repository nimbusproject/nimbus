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
package org.globus.workspace.scheduler.defaults;

import org.globus.workspace.scheduler.VmmNode;

import java.util.Collection;

public class DefaultVmmNode implements VmmNode {

    @SuppressWarnings({"UnusedDeclaration"}) // used by GSON deserialization
    DefaultVmmNode() {}

    public DefaultVmmNode(String hostname, String poolName,
                          int memory, Collection<String> networkAssociations,
                          boolean vacant) {
        this.hostname = hostname;
        this.poolName = poolName;
        this.memory = memory;
        this.networkAssociations = networkAssociations;
        this.vacant = vacant;
    }

    private String hostname;
    private String poolName;
    private int memory;
    private Collection<String> networkAssociations;
    private boolean vacant;


    public String getHostname() {
        return hostname;
    }

    public String getPoolName() {
        return poolName;
    }

    public int getMemory() {
        return memory;
    }

    public Collection<String> getNetworkAssociations() {
        return networkAssociations;
    }

    public boolean isVacant() {
        return vacant;
    }
}
