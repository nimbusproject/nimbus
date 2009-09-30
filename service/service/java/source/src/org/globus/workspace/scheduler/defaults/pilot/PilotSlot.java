/*
 * Copyright 1999-2008 University of Chicago
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

package org.globus.workspace.scheduler.defaults.pilot;

public class PilotSlot {
    public final String uuid;
    public final int vmid;
    public final boolean pending;
    public final boolean terminal;
    public final String lrmhandle;
    public final int duration;
    public final String nodename;
    public final boolean partOfGroup;
    public final boolean pendingRemove;

    public PilotSlot(String uuid, int vmid, boolean pending, boolean terminal,
                     String lrmhandle, int duration, String nodename,
                     boolean partOfGroup, boolean pendingRemove) {
        this.uuid = uuid;
        this.vmid = vmid;
        this.pending = pending;
        this.terminal = terminal;
        this.lrmhandle = lrmhandle;
        this.duration = duration;
        this.nodename = nodename;
        this.partOfGroup = partOfGroup;
        this.pendingRemove = pendingRemove;
    }

    public String toString() {
        return "PilotSlot{" +
                "uuid='" + this.uuid + '\'' +
                ", vmid=" + this.vmid +
                ", pending=" + this.pending +
                ", terminal=" + this.terminal +
                ", lrmhandle='" + this.lrmhandle + '\'' +
                ", nodename='" + this.nodename + '\'' +
                ", partOfGroup=" + this.partOfGroup +
                ", pendingRemove=" + this.pendingRemove +
                '}';
    }
}
