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

package org.globus.workspace.service.binding;

public interface GlobalPolicies {

    public int getDefaultRunningTimeSeconds();

    public int getMaximumRunningTimeSeconds();

    public int getTerminationOffsetSeconds();

    public String getCpuArchitectureName();

    public String[] getVmmVersions();

    public String getVmm();

    public int getMaximumGroupSize();

    public boolean isFake();

    public boolean isAllowStaticIPs();

    public long getFakelag();

    public boolean isPropagateEnabled();
    public void setPropagateEnabled(boolean propagateEnabled);

    public boolean isUnpropagateEnabled();
    public void setUnpropagateEnabled(boolean unpropagateEnabled);
    
    public boolean isUnpropagateAfterRunningTimeEnabled();
    public void setUnpropagateAfterRunningTimeEnabled(
                                boolean unpropagateAfterRunningTimeEnabled);
    
}
