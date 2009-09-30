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

package org.globus.workspace.network;

import java.util.List;

public class Association {

    private String dns;
    private List entries;
    private long fileTime = -1;

    public Association(String dns) {
        if (dns != null) {
            this.dns = dns.trim();
        }
    }

    public String getDns() {
        return this.dns;
    }

    public List getEntries() {
        return this.entries;
    }

    public void setEntries(List entries) {
        this.entries = entries;
    }

    public long getFileTime() {
        return this.fileTime;
    }

    public void setFileTime(long fileTime) {
        this.fileTime = fileTime;
    }

    public String toString() {
        return "Network{" +
                "dns='" + this.dns + '\'' +
                ", fileTime=" + this.fileTime +
                ", entries=\n" + this.entries +
                "\n}";
    }
}
