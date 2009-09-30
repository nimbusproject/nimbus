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

package org.nimbustools.messaging.gt4_0.ctx;

import org.globus.wsrf.ResourceKey;
import org.nimbustools.messaging.gt4_0.generated.metadata.logistics.Contextualization_Type;
import org.nimbustools.messaging.gt4_0.generated.metadata.logistics.Nic_Type;
import org.nimbustools.api.services.ctx.ContextBrokerException;
import org.apache.axis.message.addressing.EndpointReferenceType;

public interface ContextBrokerHome {

    // todo: fix syntax exposure issue, see getIncompleteBootstrap()
    // "spec" is at least 20 ='s
    public static final String FIELD_SEPARATOR =
            "\n\n=======================================================\n\n";

    public EndpointReferenceType createNewResource(String callerDN)
            throws ContextBrokerException;

    
    public ResourceKey getResourceKey(EndpointReferenceType epr)
            throws ContextBrokerException;

    public String getDefaultBootstrapPathOnWorkspace()
            throws ContextBrokerException;

    /**
     * Service needs special knowledge of this string's internals.  Temporary.
     *
     * TODO: fix the root of the problem
     *       (multi-NIC VMs need to correlate against context data without
     *        involving the cloud service)
     *
     * @param ctxEPR context id
     * @param callerDN owner
     * @return incomplete bootstrap data
     * @throws ContextBrokerException problem
     */
    public String getIncompleteBootstrap(EndpointReferenceType ctxEPR,
                                         String callerDN)
            throws ContextBrokerException;

    /**
     * Treats provides and requires documents in the empty, provided
     * interpretation.
     *
     * Returns complete bootstrap data for creation task to send via VMM or
     * workspace-control etc.
     *
     * The bootstrap data is free to differentiate here based on the workspace
     * that is being added: therefore it is not safe to cache the returned value
     *
     * @param ctxEPR the resource to add workspace to
     * @param workspaceID workspace ID
     * @param callerDN the entity deploying this workspace
     * @param nics known information up to this point
     * @param ctxDoc the provided contextualization document
     * @throws ContextBrokerException illegals/unimplemented etc
     * @return complete bootstrap data for creation task
     */
    public String addWorkspace(EndpointReferenceType ctxEPR,
                               Integer workspaceID,
                               String callerDN,
                               Nic_Type[] nics,
                               Contextualization_Type ctxDoc)

            throws ContextBrokerException;


    /**
     * Pre validate contextualization document, saves processing time and
     * saves need for back-outs of groups
     * @param ctx ctx
     * @throws ContextBrokerException problem
     */
    public void basicValidate(Contextualization_Type ctx)
            throws ContextBrokerException;


    /**
     * Allow checks ahead of time before going through expensive creation
     * process.
     * @param ctxEPR the particular context in question
     * @param callerDN the DN attempting to add
     * @throws ContextBrokerException problem
     */
    public void authorizedToAddWorkspace(EndpointReferenceType ctxEPR,
                                         String callerDN)
            throws ContextBrokerException;


    /**
     * Lock a resource via non-contextBrokerService call.  Can only call
     * this *after* any new workspaces are added to the context.
     *
     * @param ctxEPR target context
     * @throws ContextBrokerException if already locked
     *         or if resource is not found
     */
    public void lockResource(EndpointReferenceType ctxEPR)
            throws ContextBrokerException;

}
