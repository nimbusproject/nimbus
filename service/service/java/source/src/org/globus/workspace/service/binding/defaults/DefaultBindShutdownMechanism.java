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

import org.globus.workspace.service.binding.BindShutdownMechanism;
import org.globus.workspace.service.binding.GlobalPolicies;
import org.globus.workspace.service.binding.vm.VirtualMachineDeployment;
import org.globus.workspace.WorkspaceConstants;
import org.nimbustools.api.services.rm.CreationException;
import org.nimbustools.api.repr.CreateRequest;

public class DefaultBindShutdownMechanism implements BindShutdownMechanism {

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected final GlobalPolicies globals;
    

    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    public DefaultBindShutdownMechanism(GlobalPolicies globalPolicies) {
        if (globalPolicies == null) {
            throw new IllegalArgumentException("globalPolicies may not be null");
        }
        this.globals = globalPolicies;
    }
    

    // -------------------------------------------------------------------------
    // implements BindShutdownMechanism
    // -------------------------------------------------------------------------

    public void consume(VirtualMachineDeployment dep, String given)
            throws CreationException {

        final String shutdownRequest;
        if (given == null) {
            shutdownRequest = CreateRequest.SHUTDOWN_TYPE_NORMAL;
        } else {
            shutdownRequest = given;
        }

        if (CreateRequest.SHUTDOWN_TYPE_NORMAL.equals(shutdownRequest)) {

            if (this.globals.isUnpropagateAfterRunningTimeEnabled()) {
                dep.setRequestedShutdown(WorkspaceConstants.DEFAULT_SHUTDOWN_NORMAL);
            } else {
                final String err = "requested shutdown state 'normal' is " +
                        "disabled, this site does not allow unpropagation " +
                        "from VMM nodes after the running time has expired. " +
                        "Use 'trash' instead and invoke unpropagation " +
                        "manually before running time has expired.";
                throw new CreationException(err);
            }

        } else if (CreateRequest.SHUTDOWN_TYPE_SERIALIZE.equals(shutdownRequest)) {

            // If this was implemented, !UnPropagateAfterRunningTimeEnabled
            // would also apply
            final String err = "requested shutdown state 'serialized' can " +
                    "not be accomplished, serialization is not supported";
            throw new CreationException(err);

        } else if (CreateRequest.SHUTDOWN_TYPE_TRASH.equals(shutdownRequest)) {

            dep.setRequestedShutdown(WorkspaceConstants.DEFAULT_SHUTDOWN_TRASH);

        } else {
            throw new CreationException("unknown shutdown type '" +
                    shutdownRequest + "'");
        }
        
    }
}
