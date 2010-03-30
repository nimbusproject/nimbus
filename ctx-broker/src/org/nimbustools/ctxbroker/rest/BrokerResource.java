/*
 * Copyright 1999-2009 University of Chicago
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
package org.nimbustools.ctxbroker.rest;

import com.google.gson.Gson;
import org.apache.axis.message.addressing.EndpointReferenceType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.wsrf.Resource;
import org.globus.wsrf.ResourceException;
import org.globus.wsrf.ResourceKey;
import org.nimbustools.ctxbroker.ContextBrokerException;
import org.nimbustools.ctxbroker.blackboard.CtxStatus;
import org.nimbustools.ctxbroker.service.ContextBrokerHomeImpl;
import org.nimbustools.ctxbroker.service.ContextBrokerResourceImpl;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

@Path("/ctx")
public class BrokerResource {

    private static final Log logger =
            LogFactory.getLog(BrokerResource.class.getName());

    private ContextBrokerHomeImpl home; //TODO this has to come from somewhere

    private final Gson gson = new Gson();

    @POST
    @Path("/")
    public Response createContext(@Context UriInfo uriInfo) {

        final String callerDn = null; //TODO get dn from security
        final boolean expectInjections = false;

        final String rawId;
        final BrokerContact contact;
        try {
            final EndpointReferenceType epr =
                    this.home.createNewResource(callerDn, expectInjections);

            final ResourceKey resourceKey = this.home.getResourceKey(epr);
            final String contextId = resourceKey.toString();

            rawId = (String) resourceKey.getValue();


            contact = new BrokerContact(this.home.getBrokerURL(),
                    contextId, this.home.getContextSecret(epr));
        } catch (ContextBrokerException e) {
            logger.error("Problem creating a context: "+e.getMessage(), e);
            return Response.serverError().build();
        }

        final UriBuilder uriBuilder =
                uriInfo.getAbsolutePathBuilder().
                        path(this.getClass(), "checkContext");

        final URI uri = uriBuilder.build(rawId);

        return Response.created(uri)
                .entity(gson.toJson(contact))
                .build();
    }

    @GET
    @Path("/{id}")
    public Response checkContext(@PathParam("id") String id) {

        final Resource resource;
        try {
            final ResourceKey resourceKey = this.home.getResourceKey(id);
            resource = this.home.find(resourceKey);
        } catch (ResourceException e) {
            logger.error("Problem retrieving ctx resource with id '"+id+"': "
                    +e.getMessage(), e);
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        ContextBrokerResourceImpl brokerResource = (ContextBrokerResourceImpl) resource;
        try {
            final CtxStatus status = brokerResource.getBlackboard().getStatus();

            return Response.ok(gson.toJson(status)).build();
        } catch (ContextBrokerException e) {
            logger.error("Problem checking a context status: "+e.getMessage(), e);
            return Response.serverError().build();
        }
    }
}
