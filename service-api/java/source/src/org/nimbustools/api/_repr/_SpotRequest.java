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

package org.nimbustools.api._repr;

import java.util.Calendar;

import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.SpotRequest;
import org.nimbustools.api.repr.si.SIRequestState;
import org.nimbustools.api.repr.vm.ResourceAllocation;
import org.nimbustools.api.repr.vm.VMFile;

public interface _SpotRequest extends SpotRequest {
    
    public void setRequestID(String requestId);
    public void setSpotPrice(Double spotPrice);
    public void setPersistent(boolean persistent);    
    public void setState(SIRequestState state);
    public void setCreationTime(Calendar date);
    public void setVMIds(String[] ids);
    
    public void setVMFiles(VMFile[] files);
    public void setSshKeyName(String sshKeyName);
    public void setCreator(Caller creator);
    public void setMdUserData(String mdUserData);    
    public void setResourceAllocation(ResourceAllocation ra);
    public void setGroupID(String groupId);    
}
