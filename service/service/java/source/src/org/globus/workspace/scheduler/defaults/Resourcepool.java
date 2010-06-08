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

import java.util.Hashtable;

public class Resourcepool {

    private Hashtable entries;
    private long fileTime = -1;

    public Hashtable getEntries() {
        return this.entries;
    }

    public void setEntries(Hashtable entries) {
        this.entries = entries;
    }

    public long getFileTime() {
        return this.fileTime;
    }

    public void setFileTime(long fileTime) {
        this.fileTime = fileTime;
    }

    public String toString() {
        return "Resource{" +
                "fileTime=" + this.fileTime +
                ", entries=\n" + this.entries +
                "\n}";
    }

}
