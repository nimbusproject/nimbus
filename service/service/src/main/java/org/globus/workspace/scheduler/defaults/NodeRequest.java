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

package org.globus.workspace.scheduler.defaults;

public class NodeRequest {

    private int memory; // MBs
    private int duration; // seconds

    private int[] ids = null;
    private String[] neededAssociations = null;
    private String groupid = null;

    public NodeRequest(int memory,
                       int duration) {
        if (duration < 1) {
            throw new IllegalArgumentException("duration < 1 ?");
        }

        if (memory < 1) {
            throw new IllegalArgumentException("memory < 1 ?");
        }
        this.memory = memory;
        this.duration = duration;
    }

    public NodeRequest(int[] ids,
                       int memory,
                       int duration,
                       String[] neededAssociations,
                       String groupid) {
        this(memory, duration);

        this.ids = ids;
        this.neededAssociations = neededAssociations;
        this.groupid = groupid;
    }

    public int[] getIds() {
        return this.ids;
    }

    public void setIds(int[] ids) {
        this.ids = ids;
    }

    // not so nice, will be removed when group-op loops disappear entirely
    public void addId(int id) {
        if (this.ids == null) {
            this.ids = new int[] {id};
        } else {
            final int[] newids = new int[this.ids.length + 1];
            System.arraycopy(this.ids, 0, newids, 0, this.ids.length);
            newids[this.ids.length] = id;
            this.ids = newids;
        }
    }

    public int getNumNodes() {
        if (this.ids == null) {
            return 0;
        }
        return this.ids.length;
    }

    public int getMemory() {
        return this.memory;
    }

    public void setMemory(int memory) {
        this.memory = memory;
    }

    public int getDuration() {
        return this.duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }
    
    public String[] getNeededAssociations() {
        return this.neededAssociations;
    }

    public void setNeededAssociations(String[] neededAssociations) {
        this.neededAssociations = neededAssociations;
    }

    public String getGroupid() {
        return this.groupid;
    }

    public void setGroupid(String groupid) {
        this.groupid = groupid;
    }
}
