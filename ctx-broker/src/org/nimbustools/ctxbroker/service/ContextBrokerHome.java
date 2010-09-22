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

package org.nimbustools.ctxbroker.service;

import org.globus.wsrf.ResourceKey;
import org.apache.axis.message.addressing.EndpointReferenceType;
import org.nimbustools.ctxbroker.ContextBrokerException;
import org.nimbustools.ctxbroker.Identity;
import org.nimbustools.ctxbroker.generated.gt4_0.description.IdentityProvides_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.description.AgentDescription_Type;

public interface ContextBrokerHome {

    // "spec" is at least 20 ='s
    public static final String FIELD_SEPARATOR =
            "\n\n=======================================================\n\n";

    public EndpointReferenceType createNewResource(String callerDN,
                                                   boolean allowInjections)
            throws ContextBrokerException;

    public String getBrokerURL()
            throws ContextBrokerException;

    public String getContextSecret(EndpointReferenceType ref)
            throws ContextBrokerException;

    public ResourceKey getResourceKey(EndpointReferenceType epr)
            throws ContextBrokerException;

    /**
     * Treats provides and requires documents in the empty, provided
     * interpretation.
     *
     * @param resource the resource to add workspace to
     * @param workspaceID workspace ID
     * @param identities known ID information up to this point
     * @param ctxDocAndID the provided contextualization document + IDs
     * @throws ContextBrokerException illegals/unimplemented etc
     */
    public void addWorkspace(ContextBrokerResource resource,
                             Integer workspaceID,
                             Identity[] identities,
                             AgentDescription_Type ctxDocAndID)

            throws ContextBrokerException;

    public Integer getID(ContextBrokerResource resource,
                         IdentityProvides_Type[] identities,
                         AgentDescription_Type sent) throws ContextBrokerException;
}
