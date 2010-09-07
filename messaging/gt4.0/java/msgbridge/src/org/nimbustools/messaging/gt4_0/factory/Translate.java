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

import org.apache.axis.types.URI;
import org.apache.axis.message.addressing.EndpointReferenceType;
import org.nimbustools.api._repr._Caller;
import org.nimbustools.api.repr.CannotTranslateException;
import org.nimbustools.api._repr._CreateRequest;
import org.nimbustools.api._repr._CustomizationRequest;
import org.nimbustools.api._repr.ctx._Context;
import org.nimbustools.api.repr.CreateResult;
import org.nimbustools.api.repr.ReprFactory;
import org.nimbustools.api.repr.CustomizationRequest;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.ctx.Context;
import org.nimbustools.api.repr.vm.NIC;
import org.nimbustools.api.repr.vm.VM;
import org.nimbustools.api.services.ctx.ContextBrokerException;
import org.nimbustools.messaging.gt4_0.generated.metadata.VirtualWorkspace_Type;
import org.nimbustools.messaging.gt4_0.generated.metadata.definition.Definition;
import org.nimbustools.messaging.gt4_0.generated.metadata.logistics.Contextualization_Type;
import org.nimbustools.messaging.gt4_0.generated.metadata.logistics.IPConfig_TypeAcquisitionMethod;
import org.nimbustools.messaging.gt4_0.generated.metadata.logistics.Logistics;
import org.nimbustools.messaging.gt4_0.generated.metadata.logistics.VirtualNetwork_Type;
import org.nimbustools.messaging.gt4_0.generated.metadata.logistics.Nic_Type;
import org.nimbustools.messaging.gt4_0.generated.negotiable.WorkspaceDeployment_Type;
import org.nimbustools.messaging.gt4_0.generated.negotiable.PostShutdown_Type;
import org.nimbustools.messaging.gt4_0.generated.types.CustomizeTask_Type;
import org.nimbustools.messaging.gt4_0.generated.types.OptionalParameters_Type;
import org.nimbustools.messaging.gt4_0.generated.types.WorkspaceCreateRequest_Type;
import org.nimbustools.messaging.gt4_0.generated.types.WorkspaceCreateResponse_Type;
import org.nimbustools.messaging.gt4_0.generated.types.CreatedWorkspace_Type;
import org.nimbustools.messaging.gt4_0.ensemble.EnsembleUtil;
import org.nimbustools.messaging.gt4_0.common.Constants_GT4_0;
import org.nimbustools.messaging.gt4_0.IdentityUtil;
import org.nimbustools.messaging.gt4_0.BaseTranslate;
import org.nimbustools.messaging.gt4_0.EPRGenerator;
import org.nimbustools.messaging.gt4_0.service.InstanceTranslate;

import java.util.HashMap;
import java.net.URL;

public class Translate extends BaseTranslate {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    protected static final HashMap acqMethodMap = new HashMap(8);
    static {
        acqMethodMap.put(IPConfig_TypeAcquisitionMethod.AllocateAndConfigure,
                         NIC.ACQUISITION_AllocateAndConfigure);
        acqMethodMap.put(IPConfig_TypeAcquisitionMethod.AcceptAndConfigure,
                         NIC.ACQUISITION_AcceptAndConfigure);
        acqMethodMap.put(IPConfig_TypeAcquisitionMethod.Advisory,
                         NIC.ACQUISITION_Advisory);
    }

    
    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected final TranslateDefinition trdef;
    protected final TranslateRA trra;
    protected final TranslateNet trnet;
    protected final InstanceTranslate trinst;
    protected final EPRGenerator instanceEPRs;
    protected final EPRGenerator groupEPRs;
    protected final EPRGenerator ensembleEPRs;


    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    public Translate(ReprFactory reprFactory,
                     TranslateDefinition trDefinition,
                     TranslateRA trResourceAllocation,
                     TranslateNet trNetwork,
                     InstanceTranslate trInstance,
                     URL containerURL) {

        super(reprFactory);

        if (trDefinition == null) {
            throw new IllegalArgumentException("trDefinition may not be null");
        }
        this.trdef = trDefinition;

        if (trResourceAllocation == null) {
            throw new IllegalArgumentException("trResourceAllocation may not be null");
        }
        this.trra = trResourceAllocation;

        if (trNetwork == null) {
            throw new IllegalArgumentException("trNetwork may not be null");
        }
        this.trnet = trNetwork;

        if (trInstance == null) {
            throw new IllegalArgumentException("trInstance may not be null");
        }
        this.trinst = trInstance;

        if (containerURL == null) {
            throw new IllegalArgumentException("containerURL may not be null");
        }

        this.instanceEPRs =
                new EPRGenerator(containerURL,
                                 Constants_GT4_0.SERVICE_PATH,
                                 Constants_GT4_0.RESOURCE_KEY_QNAME);

        this.groupEPRs =
                new EPRGenerator(containerURL,
                                 Constants_GT4_0.GROUP_SERVICE_PATH,
                                 Constants_GT4_0.GROUP_RESOURCE_KEY_QNAME);

        this.ensembleEPRs =
                new EPRGenerator(containerURL,
                                 Constants_GT4_0.ENSEMBLE_SERVICE_PATH,
                                 Constants_GT4_0.ENSEMBLE_RESOURCE_KEY_QNAME);
    }

    
    // -------------------------------------------------------------------------
    // TRANSLATE TO: CreateRequest
    // -------------------------------------------------------------------------

    public _CreateRequest getCreateRequest(WorkspaceCreateRequest_Type wsreq)
            throws CannotTranslateException {
        
        if (wsreq == null) {
            throw new CannotTranslateException("wsreq may not be null");
        }

        final _CreateRequest req = this.repr._newCreateRequest();
        final VirtualWorkspace_Type vw = wsreq.getMetadata();
        
        final URI name = vw.getName();
        if (name != null) {
            req.setName(name.toString());
        }

        // -------------------------------------------------------------
        // Weed out the needed items:
        // -------------------------------------------------------------

        final Definition def = vw.getDefinition();
        if (def == null) {
            throw new CannotTranslateException(
                    "create request definition may not be missing");
        }

        final VirtualNetwork_Type net;
        
        final Logistics log = vw.getLogistics();
        if (log == null) {
            net = null;
        } else {
            net = log.getNetworking();
        }

        final CustomizeTask_Type[] customizes;

        final OptionalParameters_Type optional = wsreq.getOptionalParameters();
        if (optional != null) {
            customizes = optional.getFilewrite();
            req.setMdUserData(optional.getMdServerUserdata());
        } else {
            customizes = null;
        }

        // Boolean object can be null for conversion like this (null==false)
        final boolean ensDone =
                Boolean.TRUE.equals(wsreq.getEnsembleDone());
        final boolean partOfEnsemble =
                Boolean.TRUE.equals(wsreq.getPartOfEnsemble());
        final EndpointReferenceType ensembleEPR = wsreq.getEnsembleEPR();

        // it'd be interesting to not require this and use all defaults but
        // it is currently required:
        final WorkspaceDeployment_Type dep = wsreq.getResourceRequest();
        if (dep == null) {
            throw new CannotTranslateException(
                    "create request deployment may not be missing");
        }

        final PostShutdown_Type post = dep.getPostShutdown();

        // -------------------------------------------------------------
        // Translate:
        // -------------------------------------------------------------

        this.trdef.translateDefinitionRelated(req, def, post);
        this.trra.translateResourceRelated(req, dep);
        if (net != null) {
            this.trnet.translateNetworkingRelated(req, net);
        }

        if (customizes != null && customizes.length > 0) {
            final CustomizationRequest[] crs =
                    new CustomizationRequest[customizes.length];
            for (int i = 0; i < customizes.length; i++) {
                final _CustomizationRequest cr =
                        this.repr._newCustomizationRequest();
                cr.setContent(customizes[i].getContent());
                cr.setPathOnVM(customizes[i].getPathOnVM());
                crs[i] = cr;
            }
            req.setCustomizationRequests(crs);
        }

        req.setCoScheduleMember(partOfEnsemble);
        req.setCoScheduleDone(ensDone);
        if (ensembleEPR != null) {
            try {
                req.setCoScheduleID(EnsembleUtil.getResourceID(ensembleEPR));
            } catch (Exception e) {
                throw new CannotTranslateException(e.getMessage(), e);
            }
        }
        
        return req;
    }


    // -------------------------------------------------------------------------
    // TRANSLATE TO: Context
    // -------------------------------------------------------------------------

    public Context getCtx(String bootstrapPath, String bootstrapText)
            throws ContextBrokerException {
        
        if (bootstrapPath == null || bootstrapPath.trim().length() == 0) {
            throw new ContextBrokerException(
                    "context broker did not provide bootstrapPath");
        }

        if (bootstrapText == null || bootstrapText.trim().length() == 0) {
            throw new ContextBrokerException(
                    "context broker did not provide bootstrapPath");
        }
        
        final _Context ctx = this.repr._newContext();
        ctx.setBootstrapPath(bootstrapPath);
        ctx.setBootstrapText(bootstrapText);
        return ctx;
    }

    
    // -------------------------------------------------------------------------
    // TRANSLATE TO: Contextualization_Type
    // -------------------------------------------------------------------------
    
    public Contextualization_Type getWSctx(WorkspaceCreateRequest_Type req) {
        if (req == null) {
            return null; // *** EARLY RETURN ***
        }

        final VirtualWorkspace_Type vw = req.getMetadata();
        if (vw == null) {
            return null; // *** EARLY RETURN ***
        }

        final Logistics log = vw.getLogistics();
        if (log == null) {
            return null; // *** EARLY RETURN ***
        }

        return log.getContextualization();
    }

    // -------------------------------------------------------------------------
    // TRANSLATE TO: Nic_Type[]
    // -------------------------------------------------------------------------

    public Nic_Type[] getWSnics(NIC[] nics) throws CannotTranslateException {
        return this.trinst.getWSnics(nics);
    }

    
    // -------------------------------------------------------------------------
    // TRANSLATE TO: WorkspaceCreateResponse_Type
    // -------------------------------------------------------------------------

    public WorkspaceCreateResponse_Type
                    getCreateResponse_Type(CreateResult result,
                                           EndpointReferenceType ctxEPR)
            throws CannotTranslateException {

        if (result == null) {
            throw new IllegalArgumentException("result may not be null");
        }

        final WorkspaceCreateResponse_Type response =
                            new WorkspaceCreateResponse_Type();

        response.setCreatedWorkspace(
                this.getCreatedWorkspaces(result.getVMs()));

        final String groupID = result.getGroupID();
        if (groupID != null) {
            response.setGroupEPR(this.groupEPRs.getEPR(groupID));
        }

        final String coschedID = result.getCoscheduledID();
        if (coschedID != null) {
            response.setEnsembleEPR(this.ensembleEPRs.getEPR(coschedID));
        }

        if (ctxEPR != null) {
            response.setContextEPR(ctxEPR);
        }

        return response;
    }

    // -------------------------------------------------------------------------
    // TRANSLATE TO: CreatedWorkspace_Type[]
    // -------------------------------------------------------------------------

    public CreatedWorkspace_Type[] getCreatedWorkspaces(VM[] vms)
            throws CannotTranslateException {
        
        if (vms == null || vms.length == 0) {
            throw new CannotTranslateException("no VMs in RM response");
        }

        final CreatedWorkspace_Type[] created =
                new CreatedWorkspace_Type[vms.length];

        for (int i = 0; i < vms.length; i++) {
            created[i] = this.getCreatedWorkspace(vms[i]);
        }

        return created;
    }

    public CreatedWorkspace_Type getCreatedWorkspace(VM vm)
            throws CannotTranslateException {

        if (vm == null) {
            return null;
        }

        final CreatedWorkspace_Type created = new CreatedWorkspace_Type();

        created.setEpr(this.instanceEPRs.getEPR(vm.getID()));

        created.setSchedule(this.trinst.getSchedule_Type(vm));

        final Logistics log = this.trinst.getLogistics(vm);
        if (log != null) {
            created.setNetworking(log.getNetworking());
        }

        return created;
    }
}
