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

package org.globus.workspace.service.binding.authorization;

import org.globus.workspace.NamespaceTranslator;
import org.globus.workspace.service.binding.vm.VirtualMachine;
import org.nimbustools.api.services.rm.AuthorizationException;
import org.nimbustools.api.services.rm.ResourceRequestDeniedException;

import javax.security.auth.Subject;

public class DisabledCallout implements CreationAuthorizationCallout
{

    public boolean isEnabled() {
        return false;
    }

    public Integer isPermitted(VirtualMachine[] bindings, String callerDN,
                               Subject subject, Long elapsedMins,
                               Long reservedMins, int numWorkspaces)

            throws AuthorizationException, ResourceRequestDeniedException {

        throw new AuthorizationException("Authorization callout is disabled, " +
                "it is illegal to call isPermitted()");
    }
}
