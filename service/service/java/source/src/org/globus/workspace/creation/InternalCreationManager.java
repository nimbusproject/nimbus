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

package org.globus.workspace.creation;

import org.globus.workspace.service.InstanceResource;
import org.globus.workspace.service.binding.vm.VirtualMachine;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.ctx.Context;
import org.nimbustools.api.repr.vm.NIC;
import org.nimbustools.api.services.rm.AuthorizationException;
import org.nimbustools.api.services.rm.CoSchedulingException;
import org.nimbustools.api.services.rm.CreationException;
import org.nimbustools.api.services.rm.MetadataException;
import org.nimbustools.api.services.rm.ResourceRequestDeniedException;
import org.nimbustools.api.services.rm.SchedulingException;

/**
 * This interface provides internal modules
 * (such as the AsynchronousRequestManager) the
 * ability to instantiate Virtual Machines
 *
 */
public interface InternalCreationManager {

    /**
     * Instantiates virtual machines with
     * the given specification 
     *
     * @param bindings the virtual machines' specs
     * @param nics network interfaces
     * @param caller owner of the VMs
     * @param context the VM's context
     * @param groupID
     * @param coschedID the cosched id for this group of VMs
     * @param clientToken the idempotency token provided by client
     * @param spotInstances if the VMs are spot instances
     * @param chargeRatio ratio to compute the real minutes charge, typically <= 1.0 and > 0
     * @return an array of created InstanceResources
     * @throws CoSchedulingException
     * @throws CreationException
     * @throws MetadataException
     * @throws ResourceRequestDeniedException
     * @throws SchedulingException
     * @throws AuthorizationException
     * @return the created VM resources
     */
    public InstanceResource[] createVMs(VirtualMachine[] bindings,
                                        NIC[] nics,
                                        Caller caller,
                                        Context context,
                                        String groupID,
                                        String coschedID,
                                        String clientToken,
                                        boolean spotInstances,
                                        double chargeRatio)

            throws CoSchedulingException,
                   CreationException,
                   MetadataException,
                   ResourceRequestDeniedException,
                   SchedulingException,
                   AuthorizationException;
}
