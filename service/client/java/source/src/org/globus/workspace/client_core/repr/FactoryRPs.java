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

package org.globus.workspace.client_core.repr;

import org.globus.workspace.client_core.utils.StringUtils;

public class FactoryRPs {

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private String cpuArchitectureName;
    private String VMM;
    private String[] vmmVersions = StringUtils.EMPTY_STRING_ARRAY;
    private String[] associations = StringUtils.EMPTY_STRING_ARRAY;

    // max ~25k days
    private int defaultRunningSeconds = -1;
    private int maximumRunningSeconds = -1;

    // -------------------------------------------------------------------------
    // GET/SET
    // -------------------------------------------------------------------------

    /**
     * @return null if not set
     */
    public String getCpuArchitectureName() {
        return this.cpuArchitectureName;
    }

    public void setCpuArchitectureName(String cpuArchitectureName) {
        this.cpuArchitectureName = cpuArchitectureName;
    }

    /**
     * @return null if not set
     */
    public String getVMM() {
        return this.VMM;
    }

    public void setVMM(String VMM) {
        this.VMM = VMM;
    }

    /**
     * @return never null, length 0 if not set
     */
    public String[] getVmmVersions() {
        return this.vmmVersions;
    }

    public void setVmmVersions(String[] vmmVersionArray) {
        if (vmmVersionArray == null || vmmVersionArray.length == 0) {
            this.vmmVersions = StringUtils.EMPTY_STRING_ARRAY;
        } else {
            this.vmmVersions = vmmVersionArray;
        }
    }

    /**
     * @return never null, length 0 if not set
     */
    public String[] getAssociations() {
        return this.associations;
    }

    public void setAssociations(String[] associationArray) {
        if (associationArray == null || associationArray.length == 0) {
            this.associations = StringUtils.EMPTY_STRING_ARRAY;
        } else {
            this.associations = associationArray;
        }
    }

    /**
     * @return <1 if not set
     */
    public int getDefaultRunningSeconds() {
        return this.defaultRunningSeconds;
    }

    public void setDefaultRunningSeconds(int defaultRunningSeconds) {
        this.defaultRunningSeconds = defaultRunningSeconds;
    }

    /**
     * @return <1 if not set
     */
    public int getMaximumRunningSeconds() {
        return this.maximumRunningSeconds;
    }

    public void setMaximumRunningSeconds(int maximumRunningSeconds) {
        this.maximumRunningSeconds = maximumRunningSeconds;
    }
}
