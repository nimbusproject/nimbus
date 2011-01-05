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

import org.globus.wsrf.ResourceProperties;
import org.globus.wsrf.Resource;
import org.globus.wsrf.ResourcePropertySet;
import org.globus.wsrf.ResourceException;
import org.globus.wsrf.ResourceProperty;
import org.globus.wsrf.impl.SimpleResourcePropertySet;
import org.globus.wsrf.impl.SimpleResourceProperty;
import org.nimbustools.messaging.gt4_0.common.CommonUtil;
import org.nimbustools.messaging.gt4_0.common.Constants_GT4_0;
import org.nimbustools.messaging.gt4_0.common.AddCustomizations;
import org.nimbustools.messaging.gt4_0.ctx.ContextBrokerHome;
import org.nimbustools.messaging.gt4_0.generated.metadata.definition.VMM_Type;
import org.nimbustools.messaging.gt4_0.generated.metadata.definition.VMM_TypeType;
import org.nimbustools.messaging.gt4_0.generated.metadata.logistics.Contextualization_Type;
import org.nimbustools.messaging.gt4_0.generated.types.Associations;
import org.nimbustools.messaging.gt4_0.generated.types.WorkspaceCreateRequest_Type;
import org.nimbustools.messaging.gt4_0.generated.types.WorkspaceCreateResponse_Type;
import org.nimbustools.api.repr.Advertised;
import org.nimbustools.api.repr.CreateResult;
import org.nimbustools.api._repr._CreateRequest;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.CannotTranslateException;
import org.nimbustools.api.repr.ReprFactory;
import org.nimbustools.api.repr.vm.VM;
import org.nimbustools.api.repr.ctx.Context;
import org.nimbustools.api.services.rm.Manager;
import org.nimbustools.api.services.rm.SchedulingException;
import org.nimbustools.api.services.rm.ResourceRequestDeniedException;
import org.nimbustools.api.services.rm.CreationException;
import org.nimbustools.api.services.rm.MetadataException;
import org.nimbustools.api.services.rm.CoSchedulingException;
import org.nimbustools.api.services.rm.AuthorizationException;
import org.nimbustools.api.services.ctx.ContextBrokerException;
import org.nimbustools.api.services.metadata.MetadataServer;
import org.apache.axis.types.Duration;
import org.apache.axis.message.addressing.EndpointReferenceType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class FactoryResource implements ResourceProperties, Resource {
    
    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    protected static final Log logger =
            LogFactory.getLog(FactoryResource.class.getName());

    
    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected final Manager manager;
    protected final ReprFactory repr;
    protected final MetadataServer mdServer;
    protected final Translate translate;
    protected final ContextBrokerHome brokerHome;
    protected ResourcePropertySet propSet;


    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    public FactoryResource(Manager manager,
                           Translate translate,
                           ContextBrokerHome ctxBrokerHome,
                           ReprFactory repr,
                           MetadataServer mdServer) {
        if (manager == null) {
            throw new IllegalArgumentException("manager may not be null");
        }
        if (translate == null) {
            throw new IllegalArgumentException("translate may not be null");
        }
        this.manager = manager;
        this.repr = repr;
        this.mdServer = mdServer;
        this.translate = translate;
        this.brokerHome = ctxBrokerHome;
    }

    
    // -------------------------------------------------------------------------
    // implements ResourceProperties
    // -------------------------------------------------------------------------

    public ResourcePropertySet getResourcePropertySet() {
        return this.propSet;
    }

    
    // -------------------------------------------------------------------------
    // CREATE: DISPATCH TO VWS + HANDLE CTXBROKER/ENSEMBLE
    // -------------------------------------------------------------------------

    public WorkspaceCreateResponse_Type create(WorkspaceCreateRequest_Type req,
                                               String callerDN)
            throws AuthorizationException,
                   SchedulingException,
                   ResourceRequestDeniedException,
                   CreationException,
                   MetadataException,
                   CoSchedulingException,
                   CannotTranslateException,
                   ContextBrokerException {
        
        // ------------------------------------------
        // translate WS inputs into plain vws objects
        // ------------------------------------------
        _CreateRequest cReq = this.translate.getCreateRequest(req);
        final Caller caller = this.translate.getCaller(callerDN);

        // --------------------------------------
        // handle context broker related requests
        // --------------------------------------
        final EndpointReferenceType ctxEPR;
        final Contextualization_Type ctx = this.translate.getWSctx(req);
        if (ctx == null) {
            ctxEPR = null;
            this.doNotConsumeCtx(ctx, req);
        } else {
            ctxEPR = this.consumeCtx(ctx, req, cReq, callerDN);
        }

        // optional
        AddCustomizations.addAll(cReq, this.repr, this.mdServer);

        // --------------
        // *** create ***
        // --------------
        final CreateResult result = this.manager.create(cReq, caller);

        // ---------------------------------------------------
        // WS response based on creation and ctxBroker results
        // ---------------------------------------------------
        final WorkspaceCreateResponse_Type response =
                this.translate.getCreateResponse_Type(result, ctxEPR);
        
        // --------------------------------------------------
        // after-creation ctxBroker interaction (inform/lock)
        // --------------------------------------------------
        if (ctxEPR != null) {
            final boolean lockCtx =  Boolean.TRUE.equals(req.getLockContext());
            try {
                this.postCreateContextNeeds(result, response, callerDN,
                                            ctx, ctxEPR, lockCtx);
            } catch (Throwable t) {
                // trash whatever was just created
                this.backOutAfterCreation(result, t);
            }
        }

        // ------------------------
        // return valid WS response
        // ------------------------
        return response;
    }

    protected String backOutAfterCreation(CreateResult result, Throwable t1) {

        if (logger.isDebugEnabled()) {
            logger.error(t1.getMessage(), t1);
        } else {
            logger.error(t1.getMessage());
        }

        final String problemString =
                "Contextualization problem: " + t1.getMessage();
        
        final StringBuffer buf = new StringBuffer(problemString);

        final VM[] vms = result.getVMs();
        if (vms == null) {
            buf.append("[[ WARNING: no VMs, nothing backed out ]]"); // ??
            return buf.toString(); // *** EARLY RETURN ***
        }

        for (int i = 0; i < vms.length; i++) {
            try {
                this.manager.trash(vms[i].getID(), Manager.INSTANCE, null);
            } catch (Throwable t2) {
                buf.append("  Problem during backout: [[")
                   .append(t2.getMessage())
                   .append("]].");

                if (logger.isDebugEnabled()) {
                    logger.error(t2.getMessage(), t2);
                } else {
                    logger.error(t2.getMessage());
                }
            }
        }

        return buf.toString();
    }

    protected void postCreateContextNeeds(CreateResult result,
                                          WorkspaceCreateResponse_Type wsResp,
                                          String callerDN,
                                          Contextualization_Type ctx,
                                          EndpointReferenceType ctxEPR,
                                          boolean lockCtx)
            throws ContextBrokerException, CannotTranslateException {

        if (result == null) {
            throw new IllegalArgumentException("result may not be null");
        }

        final VM[] vms = result.getVMs();
        if (vms == null || vms.length == 0) {
            throw new IllegalArgumentException("result has no VMs");
        }

        for (int i = 0; i < vms.length; i++) {
            final VM vm = vms[i];
            this.brokerHome.addWorkspace(ctxEPR,
                                         new Integer(vm.getID()),
                                         callerDN,
                                         this.translate.getWSnics(vm.getNics()),
                                         ctx);
        }

        if (lockCtx) {
            this.brokerHome.lockResource(ctxEPR);
        }

        // NOTES:
        
        // locking Exception would not happen unless client sent lock msg
        // in two (or more) different deployments simultaneously AND
        // those had an add-workspace race that collided here (or
        // the ctx lock WS operation was called at just the right
        // time).  Client should just lock once.

        // Exception could also be because ctx resource was externally
        // terminated in the last split second.

        // Possible so we at least log the error messages and backout
        // the workspace addition (via letting exception fly)
    }

    // the context broker is not virtualized yet, plans are for it to look
    // like the Manager; as well as make it easy to have it be a remote
    // endpoint or locally hosted (either one, transparently to the factory)
    protected EndpointReferenceType consumeCtx(Contextualization_Type ctx,
                                               WorkspaceCreateRequest_Type req,
                                               _CreateRequest cReq,
                                               String callerDN)
            
            throws ContextBrokerException {
        
        if (ctx == null) {
            throw new IllegalArgumentException("ctx may not be null");
        }
        if (this.brokerHome == null) {
            throw new ContextBrokerException("contextualization is " +
                    "required but there is no configured context broker");
        }

        // pre validate contextualization document, saves processing
        // time and can avoid need for back-outs
        // todo: when broker becomes full standalone and is also in a remote
        //       container, do not make this call
        this.brokerHome.basicValidate(ctx);

        EndpointReferenceType ctxEPR = req.getContextEPR();
        
        if (ctxEPR == null) {
            ctxEPR = this.brokerHome.createNewResource(callerDN);
        } else {
            // check up front (see above, same problem with remote brokers)
            this.brokerHome.authorizedToAddWorkspace(ctxEPR, callerDN);
        }

        final String bootstrapPath =
                this.brokerHome.getDefaultBootstrapPathOnWorkspace();

        final String bootstrapText =
                this.brokerHome.getIncompleteBootstrap(ctxEPR, callerDN);

        final Context context =
                this.translate.getCtx(bootstrapPath, bootstrapText);

        cReq.setContext(context);

        return ctxEPR;
    }

    protected void doNotConsumeCtx(Contextualization_Type ctx,
                                   WorkspaceCreateRequest_Type req)
            throws ContextBrokerException {

        if (req == null) {
            throw new IllegalArgumentException("req may not be null");
        }

        final EndpointReferenceType ctxEPR = req.getContextEPR();
        if (ctxEPR != null) {
            throw new ContextBrokerException("Context EPR is provided " +
                    "but no contextualization section is present in " +
                    "logistics?");
        }

        if (Boolean.TRUE.equals(req.getLockContext())) {
            throw new ContextBrokerException("Contextualization is not" +
                    " required but a contextualization lock is requested (" +
                    "lastDeploymentForContextualizationResource), this is " +
                    "illegal.");
        }
    }
    
    
    // -------------------------------------------------------------------------
    // LOAD INFORMATION
    // -------------------------------------------------------------------------

    /**
     * If a resource property set existed in the past, this replaces it.
     * 
     * @param advert information from manager
     * @throws ResourceException problem
     */
    public void setRPs(Advertised advert) throws ResourceException {

        if (advert == null) {
            throw new ResourceException("advert may not be missing (yet)");
        }

        this.propSet = new SimpleResourcePropertySet(
                                Constants_GT4_0.FACTORY_RP_SET);

        /* DefaultRunningTime: */
        
        final Integer ttl =
                new Integer(advert.getDefaultRunningTimeSeconds());
        final Duration ttlDur =
                CommonUtil.minutesToDuration(ttl.intValue());
                
        ResourceProperty prop =
                new SimpleResourceProperty(
                        Constants_GT4_0.RP_FACTORY_DefTTL);
        prop.add(ttlDur);
        this.propSet.add(prop);

        /* MaximumRunningTime: */

        final Integer maxttl =
                        new Integer(advert.getMaximumRunningTimeSeconds());
        final Duration maxTtlDur =
                        CommonUtil.minutesToDuration(maxttl.intValue());

        prop = new SimpleResourceProperty(
                        Constants_GT4_0.RP_FACTORY_MaxTTL);
        prop.add(maxTtlDur);
        this.propSet.add(prop);


        /* TODO: NOT an RP yet: MaximumAfterRunningTime */
        //final Integer offset =
        //        new Integer(advert.getMaximumAfterRunningTime());
        //final Duration offsetDur =
        //        CommonUtil.minutesToDuration(offset.intValue());

        
        /* CPUArchitectureName */

        final String[] validArches = advert.getCpuArchitectureNames();
        if (validArches != null && validArches.length > 0) {
            prop = new SimpleResourceProperty(Constants_GT4_0.RP_FACTORY_CPUArch);
            // Can only advertise one of them due to JSDL XSD restrictions.
            prop.add(validArches[0]);
            this.propSet.add(prop);
        }

        if (advert.getVmm() != null) {
            final VMM_Type vmm = new VMM_Type();
            vmm.setType(VMM_TypeType.Xen);
            final String[] versions = advert.getVmmVersions();
            if (versions != null) {
                vmm.setVersion(versions);
            }
            prop = new SimpleResourceProperty(Constants_GT4_0.RP_FACTORY_VMM);
            prop.add(vmm);
            this.propSet.add(prop);
        }

        final String[] assocs;
        try {
            assocs = advert.getNetworkNames();
        } catch (Exception e) {
            throw new ResourceException(e);
        }

        if (assocs != null && assocs.length > 0) {
            final Associations rpAssoc = new Associations(assocs);
            prop = new SimpleResourceProperty(Constants_GT4_0.RP_FACTORY_ASSOCIATIONS);
            prop.add(rpAssoc);
            this.propSet.add(prop);
        }
    }
}
