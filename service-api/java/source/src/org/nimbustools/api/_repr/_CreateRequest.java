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

import org.nimbustools.api.repr.ctx.Context;
import org.nimbustools.api.repr.vm.Kernel;
import org.nimbustools.api.repr.vm.NIC;
import org.nimbustools.api.repr.vm.RequiredVMM;
import org.nimbustools.api.repr.vm.ResourceAllocation;
import org.nimbustools.api.repr.vm.Schedule;
import org.nimbustools.api.repr.vm.VMFile;
import org.nimbustools.api.repr.CreateRequest;
import org.nimbustools.api.repr.CustomizationRequest;

public interface _CreateRequest extends CreateRequest {

    public void setName(String name);
    public void setVMFiles(VMFile[] vmFiles);
    public void setRequestedNics(NIC[] requested);
    public void setRequestedSchedule(Schedule requested);
    public void setCoScheduleID(String id);
    public void setCoScheduleDone(boolean done);
    public void setCoScheduleMember(boolean member);
    public void setRequestedRA(ResourceAllocation requested);
    public void setRequiredVMM(RequiredVMM requiredVMM);
    public void setRequestedKernel(Kernel kernel);
    public void setCustomizationRequests(CustomizationRequest[] requests);
    public void setShutdownType(String type);
    public void setInitialStateRequest(String state);
    public void setContext(Context context);
    public void setMdUserData(String mdUserData);
    public void setSshKeyName(String keyName);
}
