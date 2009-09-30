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

package org.nimbustools.api.defaults.repr;

import org.nimbustools.api._repr._CreateResult;
import org.nimbustools.api.repr.vm.VM;

import java.util.Arrays;

public class DefaultCreateResult implements _CreateResult {

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------
    
    private VM[] vms;
    private String groupID;
    private String coscheduleID;

    
    // -------------------------------------------------------------------------
    // implements org.nimbustools.api.repr.CreateResult
    // -------------------------------------------------------------------------

    public VM[] getVMs() {
        return this.vms;
    }

    public String getGroupID() {
        return this.groupID;
    }

    public String getCoscheduledID() {
        return this.coscheduleID;
    }


    // -------------------------------------------------------------------------
    // implements org.nimbustools.api._repr._CreateResult
    // -------------------------------------------------------------------------

    public void setVMs(VM[] vms) {
        this.vms = vms;
    }

    public void setGroupID(String id) {
        this.groupID = id;
    }

    public void setCoscheduledID(String id) {
        this.coscheduleID = id;
    }


    // -------------------------------------------------------------------------
    // DEBUG STRING
    // -------------------------------------------------------------------------

    public String toString() {
        return "DefaultCreateResult{" +
                "vms=" + (vms == null ? null : Arrays.asList(vms)) +
                ", groupID='" + groupID + '\'' +
                ", coscheduleID='" + coscheduleID + '\'' +
                '}';
    }
}
