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

package org.nimbustools.messaging.gt4_0.factory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.nimbustools.messaging.gt4_0.generated.types.WorkspaceCreateResponse_Type;
import org.nimbustools.messaging.gt4_0.generated.types.WorkspaceCreateRequest_Type;
import org.nimbustools.messaging.gt4_0.generated.types.WorkspaceCreationFault;
import org.nimbustools.messaging.gt4_0.generated.types.WorkspaceMetadataFault;
import org.nimbustools.messaging.gt4_0.generated.types.WorkspaceSchedulingFault;
import org.nimbustools.messaging.gt4_0.generated.types.WorkspaceResourceRequestDeniedFault;
import org.nimbustools.messaging.gt4_0.generated.ensemble.WorkspaceEnsembleFault;
import org.nimbustools.messaging.gt4_0.FaultUtil;
import org.globus.wsrf.ResourceContext;
import org.globus.wsrf.security.SecurityManager;
import org.nimbustools.api.services.rm.MetadataException;
import org.nimbustools.api.services.rm.SchedulingException;
import org.nimbustools.api.services.rm.ResourceRequestDeniedException;
import org.nimbustools.api.services.rm.CoSchedulingException;
import org.nimbustools.api.services.rm.AuthorizationException;
import org.nimbustools.api.services.ctx.ContextBrokerException;
import org.nimbustools.api.repr.CannotTranslateException;
import org.safehaus.uuid.UUIDGenerator;

public class FactoryService {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(FactoryService.class.getName());


    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    // singleton right now, cache it
    private FactoryResource resource;
    private final UUIDGenerator uuidGen = UUIDGenerator.getInstance();

    
    // -------------------------------------------------------------------------
    // HELPER
    // -------------------------------------------------------------------------

    protected FactoryResource getFactoryResource() throws Exception {
        final ResourceContext context =
                    ResourceContext.getResourceContext();
        return (FactoryResource)context.getResource();
    }

    
    // -------------------------------------------------------------------------
    // methods corresponding to WorkspaceFactoryPortType
    // -------------------------------------------------------------------------
    
    public WorkspaceCreateResponse_Type create(
                        WorkspaceCreateRequest_Type createRequest)
            throws WorkspaceCreationFault,
                   WorkspaceMetadataFault,
                   WorkspaceSchedulingFault,
                   WorkspaceResourceRequestDeniedFault,
                   WorkspaceEnsembleFault {

        final String callerDN = SecurityManager.getManager().getCaller();

        if (callerDN == null) {
            logger.error("VWS-CREATE: no caller identity");
            throw FactoryUtil.makeCreationFault("no caller identity", null);
        }

        if (this.resource == null) {
            try {
                this.resource = this.getFactoryResource();
            } catch (Exception e) {
                logger.error("", e);
                throw FactoryUtil.makeCreationFault("", e);
            }
        }

        final WorkspaceCreateResponse_Type resp;
        try {
            resp = this.resource.create(createRequest, callerDN);
        } catch (Exception e) {

            final String err =
                    "Error creating workspace(s): " + e.getMessage();
            if (logger.isDebugEnabled()) {
                logger.error(err, e);
            } else {
                logger.error(err);
            }

            if (e instanceof MetadataException) {
                throw FactoryUtil.makeMetadataFault(err, e);
            } else if (e instanceof CannotTranslateException) {
                throw FactoryUtil.makeMetadataFault(err, e);
            } else if (e instanceof AuthorizationException) {
                throw FactoryUtil.makeDeniedFault(err, e);
            } else if (e instanceof SchedulingException) {
                throw FactoryUtil.makeSchedulingFault(err, e);
            } else if (e instanceof ResourceRequestDeniedException){
                throw FactoryUtil.makeDeniedFault(err, e);
            } else if (e instanceof ContextBrokerException){
                throw FactoryUtil.makeCreationFault(err, e);
            } else if (e instanceof CoSchedulingException) {
                throw FactoryUtil.makeCoschedulingFault(err, e);
            } else {
                throw FactoryUtil.makeCreationFault(err, e);
            }

        } catch (Throwable t) {
            throw FactoryUtil.makeCreationFault(
                    FaultUtil.unknown(t, "factory.create"), null);
        }

        return resp;
    }
}
