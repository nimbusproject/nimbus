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

package org.globus.workspace.cloud.client.cluster;

public class KnownHostsTask {

    public final int eprID;
    public final String ipaddr;
    public final String interfaceName;
    public final String printName;
    public final boolean perHostDir;
    public final String perHostDirPath;

    public KnownHostsTask(int eprID, String ip,
                          String ifaceName, String printName,
                          boolean perHostDir, String perHostDirPath) {
        this.ipaddr = ip;
        this.eprID = eprID;

        if (ifaceName == null) {
            throw new IllegalArgumentException("ifaceName may not be null");
        }
        this.interfaceName = ifaceName;
        this.printName = printName;
        this.perHostDir = perHostDir;
        this.perHostDirPath = perHostDirPath;
    }
}
