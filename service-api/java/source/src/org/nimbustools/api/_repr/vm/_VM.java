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

package org.nimbustools.api._repr.vm;

import org.nimbustools.api.repr.vm.VM;
import org.nimbustools.api.repr.vm.NIC;
import org.nimbustools.api.repr.vm.ResourceAllocation;
import org.nimbustools.api.repr.vm.Schedule;
import org.nimbustools.api.repr.vm.State;
import org.nimbustools.api.repr.vm.VMFile;
import org.nimbustools.api.repr.Caller;

public interface _VM extends VM {

    public void setID(String id);
    public void setGroupID(String id);
    public void setCoschedID(String id);
    public void setNics(NIC[] nics);
    public void setVMFiles(VMFile[] vmFiles);
    public void setResourceAllocation(ResourceAllocation ra);
    public void setSchedule(Schedule schedule);
    public void setState(State state);
    public void setCreator(Caller creator);
    public void setLaunchIndex(int launchIndex);
    public void setMdUserData(String mdUserData);
    public void setSshKeyName(String keyName);
}
