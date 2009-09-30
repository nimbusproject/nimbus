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

package org.nimbustools.api.defaults.repr.vm;

import org.nimbustools.api._repr.vm._RequiredVMM;

import java.util.Arrays;

public class DefaultRequiredVMM implements _RequiredVMM {

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private String type;
    private String[] versions;


    // -------------------------------------------------------------------------
    // implements org.nimbustools.api.repr.vm.RequiredVMM
    // -------------------------------------------------------------------------

    public String getType() {
        return this.type;
    }

    public String[] getVersions() {
        return this.versions;
    }
    
    
    // -------------------------------------------------------------------------
    // implements org.nimbustools.api._repr.vm._RequiredVMM
    // -------------------------------------------------------------------------

    public void setType(String type) {
        this.type = type;
    }

    public void setVersions(String[] versions) {
        this.versions = versions;
    }


    // -------------------------------------------------------------------------
    // DEBUG STRING
    // -------------------------------------------------------------------------

    public String toString() {
        return "DefaultRequiredVMM{" +
                "type='" + type + '\'' +
                ", versions=" +
                (versions == null ? null : Arrays.asList(versions)) +
                '}';
    }
}
