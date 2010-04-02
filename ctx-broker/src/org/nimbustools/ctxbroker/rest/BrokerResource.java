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
import com.google.gson.GsonBuilder;
import org.apache.axis.message.addressing.EndpointReferenceType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.wsrf.Resource;
import org.globus.wsrf.ResourceException;
import org.globus.wsrf.ResourceKey;
import org.nimbustools.ctxbroker.ContextBrokerException;
import org.nimbustools.ctxbroker.blackboard.Blackboard;
import org.nimbustools.ctxbroker.blackboard.CtxStatus;
import org.nimbustools.ctxbroker.blackboard.NodeStatus;
import org.nimbustools.ctxbroker.service.ContextBrokerHomeImpl;
import org.nimbustools.ctxbroker.service.ContextBrokerResourceImpl;
import org.nimbustools.ctxbroker.service.ContextBrokerServiceImpl;
import org.nimbustools.messaging.query.security.QueryUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.naming.InitialContext;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;

@Path("/ctx")
public class BrokerResource {

    private static final Log logger =
            LogFactory.getLog(BrokerResource.class.getName());

    private ContextBrokerHomeImpl home;

    private final Gson gson;


    public BrokerResource() {
        try {
            this.home = discoverHome();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        GsonBuilder gsonBuilder = new GsonBuilder();
        gson = gsonBuilder.serializeNulls().create();
    }

    protected static ContextBrokerHomeImpl discoverHome() throws Exception {
        InitialContext ctx = null;
        try {
            ctx = new InitialContext();
            final ContextBrokerHomeImpl home =
                    (ContextBrokerHomeImpl) ctx.lookup(
                            ContextBrokerServiceImpl.CONTEXTUALIZATION_HOME);
            if (home == null) {
                throw new Exception("null from JNDI for ContextBrokerHome (?)");
            }
            return home;
        } finally {
            if (ctx != null) {
                ctx.close();
            }
        }
    }

    @POST
    @Path("/")
    public Response createContext(@Context UriInfo uriInfo) {

        final String callerDn = getCallerDn();
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

        final String callerDn = getCallerDn();

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

        final String creatorDn = brokerResource.getCreatorDN();
        if (creatorDn == null || !creatorDn.equals(callerDn)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        try {
            final Blackboard blackboard = brokerResource.getBlackboard();
            final CtxStatus status = blackboard.getStatus();
            final List<NodeStatus> identities = blackboard.identities(true, null, null);

            final ContextStatus responseStatus = new ContextStatus();
            responseStatus.setAllOk(status.isAllOk());
            responseStatus.setComplete(status.isComplete());
            responseStatus.setErrorOccurred(status.isErrorOccurred());
            responseStatus.setExpectedNodeCount(status.getTotalNodeCount());
            responseStatus.setNodeCount(status.getPresentNodeCount());

            responseStatus.setNodes(identities);

            return Response.ok(gson.toJson(responseStatus)).build();
        } catch (ContextBrokerException e) {
            logger.error("Problem checking a context status: "+e.getMessage(), e);
            return Response.serverError().build();
        }
    }


    private String getCallerDn() {
        final SecurityContext context = SecurityContextHolder.getContext();

        final Authentication auth = context.getAuthentication();
        final QueryUser principal = (QueryUser) auth.getPrincipal();

        return principal.getDn();
    }
}
