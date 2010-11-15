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

import org.nimbustools.api._repr._SpotRequestInfo;

public class DefaultSpotRequestInfo extends DefaultRequestInfo implements _SpotRequestInfo {

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private boolean persistent;
    private Double spotPrice;
    
    // -------------------------------------------------------------------------
    // implements org.nimbustools.api.repr.RequestSIResult
    // -------------------------------------------------------------------------       
    
    public boolean isPersistent() {
        return persistent;
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
    
    public void setSpotPrice(Double spotPrice) {
        this.spotPrice = spotPrice;
    }

    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (persistent ? 1231 : 1237);
        result = prime * result
                + ((spotPrice == null) ? 0 : spotPrice.hashCode());
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        DefaultSpotRequestInfo other = (DefaultSpotRequestInfo) obj;
        if (persistent != other.persistent)
            return false;
        if (spotPrice == null) {
            if (other.spotPrice != null)
                return false;
        } else if (!spotPrice.equals(other.spotPrice))
            return false;
        return true;
    }

    public String toString() {
        return "DefaultSpotRequestInfo [persistent=" + persistent
                + ", spotPrice=" + spotPrice + ", creationTime=" + creationTime
                + ", creator=" + creator + ", groupid=" + groupid
                + ", instanceCount=" + instanceCount + ", mdUserData="
                + mdUserData + ", requestId=" + requestId
                + ", resourceAllocation=" + resourceAllocation
                + ", sshKeyName=" + sshKeyName + ", state=" + state
                + ", vmFiles=" + Arrays.toString(vmFiles) + ", vmIds="
                + Arrays.toString(vmIds) + "]";
    }
    
    
}

