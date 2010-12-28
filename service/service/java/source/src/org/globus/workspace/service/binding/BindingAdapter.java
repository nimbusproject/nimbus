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

import org.globus.workspace.WorkspaceException;
import org.globus.workspace.service.binding.vm.FileCopyNeed;
import org.globus.workspace.service.binding.vm.VirtualMachine;
import org.nimbustools.api.repr.CreateRequest;
import org.nimbustools.api.services.rm.ResourceRequestDeniedException;
import org.nimbustools.api.services.rm.CreationException;

/**
 * In this process, the request is run through a filter of the capabilities
 * and policies of the site and the data is also validated.  "Intake" is
 * another potential name for this.
 *
 * Authorization decisions are made, naming issues are resolved
 * (networking settings for example), resource requirements
 * constraints are narrowed (or completely decided on) and anything
 * not supported by the site (or by policy) is either resolved
 * (perhaps it is OK for the particular thing to be unsupported)
 * or if impossible to resolve, exceptions (WS faults) are returned.
 *
 * Once a binding is established, it is sent to a scheduler.  If
 * the scheduler supports handling constraints, those constraints
 * must be sent to the scheduler via the binding object, but we
 * don't support sending constraints in this implementation.
 *
 * If a workspace creation authorization callout is enabled, that
 * is called after the binding process but *before* sending to the
 * scheduler to ensure that site policies are enforced before any
 * resources are used or reserved.
 *
 * It is good to enforce these first, because both binding and the
 * scheduling algorithm could act as filters in a 2 step chain of
 * constraint resolution.  Say we encounter a memory requirement
 * specified in the metadata as min-256, the authorization callout
 * could express site a policy such as "because of such and such
 * attribute, this client can request memory be no higher than 1024"
 * and send the range 256->1024 to the scheduler.  Or a deployment
 * could not configure policy in the authorization callout and let
 * the scheduler handle everything.
 */

public interface BindingAdapter {

    public VirtualMachine[] processRequest(CreateRequest req)
            throws ResourceRequestDeniedException, CreationException;

    public void backOutAllocations(VirtualMachine vm)
                throws WorkspaceException;

    public void backOutAllocations(VirtualMachine[] vms)
                throws WorkspaceException;

    public FileCopyNeed newFileCopyNeed(String srcContent,
                                                  String dstPath)
                throws WorkspaceException;
}
