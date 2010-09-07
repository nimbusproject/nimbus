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

package org.globus.workspace.service.binding.defaults;

import org.globus.workspace.service.binding.BindSchedule;
import org.globus.workspace.service.binding.GlobalPolicies;
import org.globus.workspace.service.binding.vm.VirtualMachineDeployment;
import org.nimbustools.api.repr.vm.Schedule;

public class DefaultBindSchedule implements BindSchedule {

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected final GlobalPolicies globals;

    
    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    public DefaultBindSchedule(GlobalPolicies globalPolicies) {
        if (globalPolicies == null) {
            throw new IllegalArgumentException("globalPolicies may not be null");
        }
        this.globals = globalPolicies;
    }

    
    // -------------------------------------------------------------------------
    // implements BindSchedule
    // -------------------------------------------------------------------------

    public void consume(VirtualMachineDeployment dep,
                        Schedule requestedSchedule) {
        
        if (dep == null) {
            throw new IllegalArgumentException("dep may not be null");
        }

        if (requestedSchedule == null) {
            dep.setMinDuration(this.globals.getDefaultRunningTimeSeconds());
        } else {
            dep.setMinDuration(requestedSchedule.getDurationSeconds());
        }
    }
}
