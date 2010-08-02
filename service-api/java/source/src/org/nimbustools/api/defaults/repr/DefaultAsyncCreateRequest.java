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

import java.util.Arrays;

import org.nimbustools.api._repr._AsyncCreateRequest;

public class DefaultAsyncCreateRequest extends DefaultCreateRequest implements _AsyncCreateRequest {

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected String instanceType;    
    // -------------------------------------------------------------------------
    // implements org.nimbustools.api.repr.AsyncCreateRequest
    // -------------------------------------------------------------------------
    
    public String getInstanceType() {
        return this.instanceType;
    }
    
    // -------------------------------------------------------------------------
    // implements org.nimbustools.api._repr._AsyncCreateRequest
    // -------------------------------------------------------------------------

    public void setInstanceType(String instanceType) {
       this.instanceType = instanceType;
    }         
    
    // -------------------------------------------------------------------------
    // DEBUG STRING
    // -------------------------------------------------------------------------

    public String toString() {
        final boolean userDataPresent = this.mdUserData != null;
        final String prefix = "\n\n{{{ ";
        return "\nDefaultCreateSpotRequest" +
                prefix + "name='" + this.name + '\'' +
                prefix + "vmFiles=" +
                (this.vmFiles == null ? null : Arrays.asList(this.vmFiles)) +
                prefix + "nics=" +
                (this.nics == null ? null : Arrays.asList(this.nics)) +
                prefix + "schedule=" + this.schedule +
                prefix + "coscheduleID='" + this.coscheduleID + "'" +
                prefix + "coscheduleDone=" + this.coscheduleDone +
                prefix + "coscheduleMember=" + this.coscheduleMember +
                prefix + "ra=" + this.ra +
                prefix + "requiredVMM=" + this.requiredVMM +
                prefix + "kernel=" + this.kernel +
                prefix + "custRequests=" +
                (this.custRequests == null ? null : Arrays.asList(this.custRequests)) +
                prefix + "shutdownType='" + this.shutdownType + "'" +
                prefix + "userDataPresent? " + userDataPresent +
                prefix + "initialStateRequest='" + this.initialStateRequest + "'" +
                prefix + "sshKeyName='" + this.sshKeyName + "'" +
                prefix + "context=" + this.context + "'" +
                prefix + "instanceType=" + this.instanceType + "\n";
        
    }

}
