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

import java.util.Calendar;

import org.nimbustools.api._repr._RequestSIResult;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.si.SIRequestState;
import org.nimbustools.api.repr.vm.ResourceAllocation;
import org.nimbustools.api.repr.vm.VMFile;

public class DefaultSIResult implements _RequestSIResult {

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private String requestId;
    private String groupid;
    private Calendar creationTime;
    private boolean persistent;
    private VMFile[] vmFiles;
    private ResourceAllocation resourceAllocation;
    private SIRequestState state;
    private Caller creator;
    private String mdUserData;
    private String sshKeyName;
    private Double spotPrice;
    
    // -------------------------------------------------------------------------
    // implements org.nimbustools.api.repr.RequestSIResult
    // -------------------------------------------------------------------------       
    
    public String getRequestID() {
        return requestId;
    }

    public String getGroupID() {
        return groupid;
    }
    
    public Calendar getCreationTime() {
        return creationTime;
    }
    
    public boolean isPersistent() {
        return persistent;
    }
    
    public VMFile[] getVMFiles() {
        return vmFiles;
    }
    
    public ResourceAllocation getResourceAllocation() {
        return resourceAllocation;
    }
    
    public SIRequestState getState() {
        return state;
    }
    
    public void setRequestID(String requestId) {
        this.requestId = requestId;
    }
    
    public Caller getCreator() {
        return creator;
    }
    
    public void setGroupID(String groupid) {
        this.groupid = groupid;
    }
    
    public void setCreationTime(Calendar creationTime) {
        this.creationTime = creationTime;
    }    
    
    public String getMdUserData() {
        return mdUserData;
    }
    
    public String getSshKeyName() {
        return sshKeyName;
    }
    
    
    public Double getSpotPrice() {
        return spotPrice;
    }    

    // -------------------------------------------------------------------------
    // implements org.nimbustools.api._repr._RequestSIResult
    // -------------------------------------------------------------------------

    public void setPersistent(boolean persistent) {
        this.persistent = persistent;
    }
    
    public void setVMFiles(VMFile[] vmFiles) {
        this.vmFiles = vmFiles;
    }    
    
    public void setResourceAllocation(ResourceAllocation resourceAllocation) {
        this.resourceAllocation = resourceAllocation;
    }    
    
    public void setState(SIRequestState state) {
        this.state = state;
    }    
    
    public void setCreator(Caller creator) {
        this.creator = creator;
    }      
    
    public void setMdUserData(String mdUserData) {
        this.mdUserData = mdUserData;
    }    
    
    public void setSshKeyName(String sshKeyName) {
        this.sshKeyName = sshKeyName;
    }    
    
    public void setSpotPrice(Double spotPrice) {
        this.spotPrice = spotPrice;
    }    
    
}

