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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.axis.message.addressing.EndpointReferenceType;

import org.nimbustools.ctxbroker.generated.gt4_0.types.VoidType;
import org.nimbustools.ctxbroker.generated.gt4_0.types.RetrieveResponse_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.types.InjectData_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.types.ErrorExitingSend_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.types.OkExitingSend_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.types.InjectData_TypeData;
import org.nimbustools.ctxbroker.generated.gt4_0.types.IdentitiesResponse_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.types.IdentitiesSend_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.types.CreateContextResponse_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.types.CreateContext_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.description.IdentityProvides_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.description.Requires_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.description.BrokerContactType;
import org.nimbustools.ctxbroker.generated.gt4_0.description.AgentDescription_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.broker.NimbusContextualizationFault;
import org.nimbustools.ctxbroker.BrokerConstants;
import org.nimbustools.ctxbroker.ContextBrokerException;

import org.globus.wsrf.Constants;
import org.globus.wsrf.ResourceContext;
import org.globus.wsrf.security.SecurityManager;
import org.nimbustools.ctxbroker.rest.RestHttp;

import javax.naming.InitialContext;
import java.io.File;

public class ContextBrokerServiceImpl implements ContextBrokerService {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------
    
    private static final Log logger =
        LogFactory.getLog(ContextBrokerServiceImpl.class.getName());

    public static final String CONTEXTUALIZATION_HOME =
                        Constants.JNDI_SERVICES_BASE_NAME +
                                BrokerConstants.CTX_BROKER_PATH + "/home";

    public static final String REST_HTTP =
                        Constants.JNDI_SERVICES_BASE_NAME +
                                BrokerConstants.CTX_BROKER_PATH + "/rest";


    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private final ContextBrokerHome home;
    private final RestHttp restHttp;

    
    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    public ContextBrokerServiceImpl() throws Exception {
        this.home = discoverHome();
        this.restHttp = discoverRestHttp();
    }

    protected static ContextBrokerHome discoverHome() throws Exception {

        InitialContext ctx = null;
        try {
            ctx = new InitialContext();

            final ContextBrokerHome home =
                    (ContextBrokerHome) ctx.lookup(CONTEXTUALIZATION_HOME);

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

    protected static RestHttp discoverRestHttp() throws Exception {

        InitialContext ctx = null;
        try {
            ctx = new InitialContext();

            final RestHttp rest =
                    (RestHttp) ctx.lookup(REST_HTTP);

            if (rest == null) {
                throw new Exception("null from JNDI for RestHttp (?)");
            }

            return rest;

        } finally {
            if (ctx != null) {
                ctx.close();
            }
        }
    }
    

    // -------------------------------------------------------------------------
    // implements ContextBrokerService
    // -------------------------------------------------------------------------

    public CreateContextResponse_Type create(CreateContext_Type create)
            throws NimbusContextualizationFault {

        final String caller = SecurityManager.getManager().getCaller();
        logger.info("WS-CTX-CREATE invoked by " + caller);

        if (caller == null || caller.trim().length() == 0) {
            throw ContextFault.makeCtxFault(
                    "WS-CTX-CREATE caller is empty (?)", null);
        }

        boolean expectInjections = false;
        if (create != null && create.isExpectInjections()) {
            expectInjections = true;
        }
        
        try {
            final EndpointReferenceType ref =
                    this.home.createNewResource(caller, expectInjections);
            final String id = this.home.getResourceKey(ref).toString();

            final CreateContextResponse_Type resp =
                    new CreateContextResponse_Type();
            resp.setContextEPR(ref);
            resp.setContact(
                    new BrokerContactType(this.home.getBrokerURL(), id,
                                          this.home.getContextSecret(ref)));

            return resp;
            
        } catch (ContextBrokerException e) {
            throw ContextFault.makeCtxFault(e.getMessage(), null);
        }
    }

    public VoidType injectdata(InjectData_Type sent)
            throws NimbusContextualizationFault {

        if (sent == null) {
            throw ContextFault.makeCtxFault("sent empty input", null);
        }

        final InjectData_TypeData[] datas = sent.getData();
        if (datas == null || datas.length == 0) {
            throw ContextFault.makeCtxFault("sent empty input", null);
        }

        final String caller = SecurityManager.getManager().getCaller();
        logger.info("WS-CTX-INJECT-DATA invoked by " + caller);

        if (caller == null || caller.trim().length() == 0) {
            logger.trace("WS-CTX-INJECT-DATA caller is empty", null);
        }

        final ContextBrokerResource resource;
        try {
            resource = getResource();
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.error(e.getMessage(), e);
            } else {
                logger.error(e.getMessage());
            }
            throw ContextFault.makeCtxFault(e.getMessage(), null);
        }

        // Dirty, but lets us have one resource with one policy (bootstrap
        // credential still gets access to retrieve).
        if (caller == null || !caller.equals(resource.getCreatorDN())) {
            logger.info("WS-CTX-INJECT-DATA unauthorized " +
                        "-- invoked by " + caller);
            throw ContextFault.makeCtxFault("unauthorized", null);
        }

        for (InjectData_TypeData data : datas) {
            try {
                resource.injectData(data.getName(), data.get_value());
            } catch (ContextBrokerException e) {
                if (logger.isDebugEnabled()) {
                    logger.error(e.getMessage(), e);
                } else {
                    logger.error(e.getMessage());
                }
                throw ContextFault.makeCtxFault(e.getMessage(), null);
            }
        }
        return new VoidType();
    }

    public VoidType errorExiting(ErrorExitingSend_Type sent)
            throws NimbusContextualizationFault {

        final String caller = SecurityManager.getManager().getCaller();

        IdentityProvides_Type[] identities;
        Integer id = null;
        StringBuffer buf;

        final ContextBrokerResource resource;
        try {
            resource = getResource();
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.error(e.getMessage(), e);
            } else {
                logger.error(e.getMessage());
            }
            throw ContextFault.makeCtxFault(e.getMessage(), null);
        }

        buf = new StringBuffer();
        buf.append("WS-CTX-ERROR-EXITING invoked by '")
           .append(caller)
           .append("' (");
        if (sent != null) {
            identities = sent.getIdentity();
            if (identities == null) {
                buf.append("identities is null");
            } else {
                id = this.getID(resource, identities, null);
                if (id != null) {
                    buf.append("identities map to #")
                       .append(id);
                } else {
                    buf.append("identities don't map to a known ID");
                }
            }
        } else {
            buf.append("sent envelope is null");
        }
        buf.append(")");
        logger.error(buf.toString());

        if (sent == null) {
            throw ContextFault.makeCtxFault("empty input", null);
        }

        if (id == null) {
            throw ContextFault.makeCtxFault(
                    "cannot resolve WS-CTX-ERROR-EXITING caller", null);
        }

        try {
            resource.errorExit(id, sent.getErrorcode(), sent.getMessage());
        } catch (ContextBrokerException e) {
            if (logger.isDebugEnabled()) {
                logger.error(e.getMessage(), e);
            } else {
                logger.error(e.getMessage());
            }
            throw ContextFault.makeCtxFault(e.getMessage(), null);
        }
        return new VoidType();
    }

    public VoidType okExiting(OkExitingSend_Type sent)
            throws NimbusContextualizationFault {

        final String caller = SecurityManager.getManager().getCaller();

        IdentityProvides_Type[] identities;
        Integer id = null;
        StringBuffer buf = null;

        final ContextBrokerResource resource;
        try {
            resource = getResource();
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.error(e.getMessage(), e);
            } else {
                logger.error(e.getMessage());
            }
            throw ContextFault.makeCtxFault(e.getMessage(), null);
        }

        // this is debug level because it would be overwhelming in anything
        // but test situations
        if (logger.isDebugEnabled()) {
            buf = new StringBuffer();
            buf.append("WS-CTX-OK-EXITING invoked by '")
               .append(caller)
               .append("' (");
            if (sent != null) {
                identities = sent.getIdentity();
                if (identities == null) {
                    buf.append("identities is null");
                } else {
                    id = this.getID(resource, identities, null);
                    if (id != null) {
                        buf.append("identities map to #")
                           .append(id);
                    } else {
                        buf.append("identities don't map to a known ID");
                    }
                }
            } else {
                buf.append("sent envelope is null");
            }
            buf.append(")");
            logger.debug(buf.toString());
        }

        if (sent == null) {
            throw ContextFault.makeCtxFault("empty input", null);
        }

        if (buf == null) {
            identities = sent.getIdentity();
            id = this.getID(resource, identities, null);
        }

        if (id == null) {
            throw ContextFault.makeCtxFault(
                    "cannot resolve WS-CTX-OK-EXITING caller", null);
        }

        try {
            resource.okExit(id);
        } catch (ContextBrokerException e) {
            if (logger.isDebugEnabled()) {
                logger.error(e.getMessage(), e);
            } else {
                logger.error(e.getMessage());
            }
            throw ContextFault.makeCtxFault(e.getMessage(), null);
        }
        return new VoidType();
    }

    public VoidType noMoreInjections(VoidType none)

            throws NimbusContextualizationFault {

        final String caller = SecurityManager.getManager().getCaller();
        logger.info("WS-NO-MORE-INJECTIONS invoked by " + caller);

        if (caller == null || caller.trim().length() == 0) {
            logger.trace("WS-NO-MORE-INJECTIONS caller is empty", null);
        }

        final ContextBrokerResource resource;
        try {
            resource = getResource();
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.error(e.getMessage(), e);
            } else {
                logger.error(e.getMessage());
            }
            throw ContextFault.makeCtxFault(e.getMessage(), null);
        }

        // Dirty, but lets us have one resource with one policy (bootstrap
        // credential still gets access to retrieve).
        //
        // Caller can be null so that an internal service thread can lock
        // if need be.
        if (caller != null && !caller.equals(resource.getCreatorDN())) {
            logger.info("WS-NO-MORE-INJECTIONS unauthorized " +
                        "-- invoked by " + caller);
            throw ContextFault.makeCtxFault("unauthorized", null);
        }

        try {
            resource.noMoreInjections();
        } catch (ContextBrokerException e) {
            throw ContextFault.makeCtxFault(e.getMessage(), e);
        }
        
        return new VoidType();
    }

    public RetrieveResponse_Type retrieve(AgentDescription_Type sent)

            throws NimbusContextualizationFault {

        final String caller = SecurityManager.getManager().getCaller();

        IdentityProvides_Type[] identities = null;
        Integer id = null;
        StringBuffer buf = null;

        final ContextBrokerResource resource;
        try {
            resource = getResource();
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.error(e.getMessage(), e);
            } else {
                logger.error(e.getMessage());
            }
            throw ContextFault.makeCtxFault(e.getMessage(), null);
        }

        // this is debug level because it would be overwhelming in anything
        // but test situations
        if (logger.isDebugEnabled()) {
            buf = new StringBuffer();
            buf.append("WS-CTX-RETRIEVE invoked by '")
               .append(caller)
               .append("' (");
            if (sent != null) {
                identities = sent.getIdentity();
                if (identities == null) {
                    buf.append("identities is null");
                } else {
                    id = this.getID(resource, identities, sent);
                    if (id != null) {
                        buf.append("identities map to #")
                           .append(id);
                    } else {
                        buf.append("identities don't map to a known ID");
                    }
                }
            } else {
                buf.append("sent envelope is null");
            }
            buf.append(")");
            logger.debug(buf.toString());
        }

        if (sent == null) {
            throw ContextFault.makeCtxFault("empty input", null);
        }

        if (buf == null) {
            identities = sent.getIdentity();
            id = this.getID(resource, identities, sent);
        }

        if (id == null) {
            throw ContextFault.makeCtxFault(
                    "broker cannot resolve this caller's identity", null);
        }

        final Requires_Type requires;
        try {
            requires = resource.retrieve(id);
        } catch (ContextBrokerException e) {
            if (logger.isDebugEnabled()) {
                logger.error(e.getMessage(), e);
            } else {
                logger.error(e.getMessage());
            }
            throw ContextFault.makeCtxFault(e.getMessage(), null);
        }

        final RetrieveResponse_Type response = new RetrieveResponse_Type();

        // For now we take requires being null to mean node is not complete.
        // If resource is not locked we are also currently not answering.
        // i.e., no partial answers.  Also for now.

        if (requires == null) {
            //requires = new Requires_Type();
            //requires.setIdentity(null);
            //requires.setRole(null);
            response.setComplete(false);
        } else {
            response.setComplete(true);
        }

        response.setNoMoreInjections(resource.isNoMoreInjections());
        response.setRequires(requires);

        return response;
    }

    public IdentitiesResponse_Type identities(IdentitiesSend_Type sent)
            throws NimbusContextualizationFault {

        final String caller = SecurityManager.getManager().getCaller();

        final ContextBrokerResource resource;
        try {
            resource = getResource();
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.error(e.getMessage(), e);
            } else {
                logger.error(e.getMessage());
            }
            throw ContextFault.makeCtxFault(e.getMessage(), null);
        }

        boolean all = false;
        String host = null;
        String ip = null;
        if (sent != null) {
            if (sent.getAll() != null) {
                all = sent.getAll();
            }
            host = sent.getHost();
            ip = sent.getIp();
        }

        StringBuffer buf;
        
        buf = new StringBuffer();
        buf.append("WS-CTX-IDENTITIES invoked by '")
           .append(caller)
           .append("' (");
        if (sent != null) {
            buf.append("all: ")
               .append(all)
               .append(", host: '")
               .append(host)
               .append("', ip: '")
               .append(ip)
               .append("'");
        } else {
            buf.append("sent envelope is null");
        }
        buf.append(")");
        logger.info(buf.toString());

        final IdentitiesResponse_Type response = new IdentitiesResponse_Type();
        try {
            if (all) {
                response.setNode(resource.identityQueryAll());
            } else if (host != null) {
                response.setNode(resource.identityQueryHost(host));
            } else if (ip != null) {
                response.setNode(resource.identityQueryIP(ip));
            } else {
                throw ContextFault.makeCtxFault("nothing queried?", null);
            }
        } catch (ContextBrokerException e) {
            if (logger.isDebugEnabled()) {
                logger.error(e.getMessage(), e);
            } else {
                logger.error(e.getMessage());
            }
            throw ContextFault.makeCtxFault(e.getMessage(), null);
        }
        
        return response;
    }
    

    // -------------------------------------------------------------------------
    // OTHER
    // -------------------------------------------------------------------------

    private Integer getID(ContextBrokerResource resource,
                          IdentityProvides_Type[] identities,
                          AgentDescription_Type sent)
            throws NimbusContextualizationFault {

        // every single query from an agent must contain identities
        if (identities == null || identities.length == 0) {
            logger.error("Caller did not send identities.");
            return null;
        }

        try {
            return this.home.getID(resource, identities, sent);
        } catch (ContextBrokerException e) {
            logger.error(e.getMessage(), e);
            throw ContextFault.makeCtxFault(e.getMessage(), null);
        }
    }

    private static ContextBrokerResource getResource()
            throws Exception {
        
        try {
            final ResourceContext context =
                    ResourceContext.getResourceContext();
            return (ContextBrokerResource)context.getResource();
        } catch (Exception e) {
            final String err = "could not find that context";
            logger.error(err, e);
            throw new Exception(err, e);
        }
    }
}
