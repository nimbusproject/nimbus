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

package org.nimbustools.api.repr;

import org.nimbustools.api.repr.ctx.Context;
import org.nimbustools.api.repr.vm.Kernel;
import org.nimbustools.api.repr.vm.NIC;
import org.nimbustools.api.repr.vm.RequiredVMM;
import org.nimbustools.api.repr.vm.ResourceAllocation;
import org.nimbustools.api.repr.vm.Schedule;
import org.nimbustools.api.repr.vm.VMFile;

public interface CreateRequest {

    public static final String SHUTDOWN_TYPE_NORMAL = "Normal";
    public static final String SHUTDOWN_TYPE_SERIALIZE = "Serialize";
    public static final String SHUTDOWN_TYPE_TRASH = "Trash";

    public static final String INITIAL_STATE_UNPROPAGATED = "Unpropagated";
    public static final String INITIAL_STATE_PROPAGATED = "Propagated";
    public static final String INITIAL_STATE_RUNNING = "Running";
    public static final String INITIAL_STATE_PAUSED = "Paused";

    public String getName();
    public VMFile[] getVMFiles();
    public NIC[] getRequestedNics();
    public Schedule getRequestedSchedule();
    public String getCoScheduleID();
    public boolean isCoScheduleDone();
    public boolean isCoScheduleMember();
    public ResourceAllocation getRequestedRA();
    public RequiredVMM getRequiredVMM();
    public Kernel getRequestedKernel();
    public CustomizationRequest[] getCustomizationRequests();
    public String getShutdownType();
    public String getInitialStateRequest();
    public Context getContext();
    public String getMdUserData();
    public String getSshKeyName();
}
