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
package org.globus.workspace.remoting.admin;

import java.util.Collection;

public class VmmNode {

    @SuppressWarnings({"UnusedDeclaration"}) // used by GSON deserialization
    VmmNode() {}

    public VmmNode(String hostname, boolean active, String poolName,
                          int memory, String networkAssociations,
                          boolean vacant) {
        this.hostname = hostname;
        this.active = active;
        this.poolName = poolName;
        this.memory = memory;
        this.networkAssociations = networkAssociations;
        this.vacant = vacant;
    }

    private String hostname;
    private boolean active;
    private String poolName;
    private int memory;
    private int memRemain;
    private String networkAssociations;
    private boolean vacant;


    public String getHostname() {
        return hostname;
    }

    public boolean isActive() {
        return active;
    }

    public String getPoolName() {
        return poolName;
    }

    public int getMemory() {
        return memory;
    }

    public int getMemRemain() {
        return memRemain;
    }

    public String getNetworkAssociations() {
        return networkAssociations;
    }

    public boolean isVacant() {
        return vacant;
    }

    public void setMemRemain(int memRemain) {
        this.memRemain = memRemain;
    }

    @Override
    public String toString() {
        return "VmmNode{" +
                "hostname='" + hostname + '\'' +
                ", active=" + active +
                ", poolName='" + poolName + '\'' +
                ", memory=" + memory +
                ", memory remaining=" + memRemain +
                ", networkAssociations='" + networkAssociations + '\'' +
                ", vacant=" + vacant +
                '}';
    }
}
