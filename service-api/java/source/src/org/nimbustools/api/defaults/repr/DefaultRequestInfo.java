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
import java.util.Calendar;

import org.nimbustools.api._repr._RequestInfo;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.si.RequestState;
import org.nimbustools.api.repr.vm.ResourceAllocation;
import org.nimbustools.api.repr.vm.VMFile;

public class DefaultRequestInfo implements _RequestInfo {

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected String requestId;
    protected String groupid;
    protected Calendar creationTime;
    protected VMFile[] vmFiles;
    protected ResourceAllocation resourceAllocation;
    protected RequestState state;
    protected Caller creator;
    protected String mdUserData;
    protected String sshKeyName;
    protected String[] vmIds;
    protected Integer instanceCount;
    
    // -------------------------------------------------------------------------
    // implements org.nimbustools.api.repr.RequestSIResult
    // -------------------------------------------------------------------------       
    
    public Integer getInstanceCount() {
        return instanceCount;
    }

    public String getRequestID() {
        return requestId;
    }

    public String getGroupID() {
        return groupid;
    }
    
    public Calendar getCreationTime() {
        return creationTime;
    }
    
    public VMFile[] getVMFiles() {
        return vmFiles;
    }
    
    public ResourceAllocation getResourceAllocation() {
        return resourceAllocation;
    }
    
    public RequestState getState() {
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
    
    public String[] getVMIds() {
        return this.vmIds;
    }       

    // -------------------------------------------------------------------------
    // implements org.nimbustools.api._repr._RequestSIResult
    // -------------------------------------------------------------------------

    public void setInstanceCount(Integer instanceCount) {
        this.instanceCount = instanceCount;
    }    
    
    public void setVMFiles(VMFile[] vmFiles) {
        this.vmFiles = vmFiles;
    }    
    
    public void setResourceAllocation(ResourceAllocation resourceAllocation) {
        this.resourceAllocation = resourceAllocation;
    }    
    
    public void setState(RequestState state) {
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
    
    public void setVMIds(String[] ids) {
        this.vmIds = ids;
    }    

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((creationTime == null) ? 0 : creationTime.hashCode());
        result = prime * result + ((creator == null) ? 0 : creator.hashCode());
        result = prime * result + ((groupid == null) ? 0 : groupid.hashCode());
        result = prime * result
                + ((mdUserData == null) ? 0 : mdUserData.hashCode());
        result = prime * result
                + ((requestId == null) ? 0 : requestId.hashCode());
        result = prime
                * result
                + ((resourceAllocation == null) ? 0 : resourceAllocation
                        .hashCode());
        result = prime * result
                + ((sshKeyName == null) ? 0 : sshKeyName.hashCode());
        result = prime * result + ((state == null) ? 0 : state.hashCode());
        result = prime * result + Arrays.hashCode(vmFiles);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DefaultRequestInfo other = (DefaultRequestInfo) obj;
        if (creationTime == null) {
            if (other.creationTime != null)
                return false;
        } else if (!creationTime.equals(other.creationTime))
            return false;
        if (creator == null) {
            if (other.creator != null)
                return false;
        } else if (!creator.equals(other.creator))
            return false;
        if (groupid == null) {
            if (other.groupid != null)
                return false;
        } else if (!groupid.equals(other.groupid))
            return false;
        if (mdUserData == null) {
            if (other.mdUserData != null)
                return false;
        } else if (!mdUserData.equals(other.mdUserData))
            return false;
        if (requestId == null) {
            if (other.requestId != null)
                return false;
        } else if (!requestId.equals(other.requestId))
            return false;
        if (resourceAllocation == null) {
            if (other.resourceAllocation != null)
                return false;
        } else if (!resourceAllocation.equals(other.resourceAllocation))
            return false;
        if (sshKeyName == null) {
            if (other.sshKeyName != null)
                return false;
        } else if (!sshKeyName.equals(other.sshKeyName))
            return false;
        if (state == null) {
            if (other.state != null)
                return false;
        } else if (!state.equals(other.state))
            return false;
        if (!Arrays.equals(vmFiles, other.vmFiles))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "DefaultSpotRequest [creationTime=" + creationTime
                + ", creator=" + creator + ", groupid=" + groupid
                + ", mdUserData=" + mdUserData
                + ", requestId=" + requestId + ", resourceAllocation="
                + resourceAllocation
                + ", sshKeyName=" + sshKeyName + ", state=" + state
                + ", vmFiles=" + Arrays.toString(vmFiles) + "]";
    } 
    
}

