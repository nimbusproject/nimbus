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

import org.globus.workspace.service.binding.BindResourceRequest;
import org.globus.workspace.service.binding.vm.VirtualMachineDeployment;
import org.nimbustools.api.repr.vm.ResourceAllocation;
import org.nimbustools.api.services.rm.CreationException;
import org.nimbustools.api.services.rm.ResourceRequestDeniedException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DefaultBindResourceRequest implements BindResourceRequest {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(DefaultBindResourceRequest.class.getName());

    
    // -------------------------------------------------------------------------
    // implements BindResourceRequest
    // -------------------------------------------------------------------------

    public void consume(VirtualMachineDeployment dep,
                        ResourceAllocation requestedRA)
            throws CreationException, ResourceRequestDeniedException {

        if (dep == null) {
            throw new IllegalArgumentException("dep may not be null");
        }
        if (requestedRA == null) {
            throw new IllegalArgumentException("requestedRA may not be null");
        }

        final String arch = requestedRA.getArchitecture();
        if (arch != null) {
            dep.setCPUArchitecture(arch);
        } else {
            logger.warn("no CPUArchitecture/CPUArchitectureName " +
                        "in requirements?");
        }

        dep.setIndividualPhysicalMemory(requestedRA.getMemory());
    }
}
