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

package org.globus.workspace.service.binding.vm;

import java.io.Serializable;

public class VirtualMachineDeploymentStorage implements Serializable {
    private String logicalName;
    private int individualDiskSpace = -1;

    public String getLogicalName() {
        return this.logicalName;
    }

    public void setLogicalName(String logicalName) {
        this.logicalName = logicalName;
    }

    public int getIndividualDiskSpace() {
        return this.individualDiskSpace;
    }

    public void setIndividualDiskSpace(int individualDiskSpace) {
        this.individualDiskSpace = individualDiskSpace;
    }

    public static VirtualMachineDeploymentStorage cloneOne(
                                final VirtualMachineDeploymentStorage st) {

        if (st == null) {
            return st;
        }

        final VirtualMachineDeploymentStorage newst =
                              new VirtualMachineDeploymentStorage();

        newst.logicalName = st.logicalName;
        newst.individualDiskSpace = st.individualDiskSpace;

        return newst;
    }
}
