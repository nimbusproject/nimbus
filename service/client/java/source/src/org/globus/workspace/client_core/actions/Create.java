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

package org.globus.workspace.client_core.actions;

import org.apache.axis.message.addressing.EndpointReferenceType;
import org.apache.axis.types.Duration;
import org.apache.axis.types.URI;
import org.ggf.jsdl.RangeValue_Type;
import org.nimbustools.messaging.gt4_0.common.CommonUtil;
import org.nimbustools.messaging.gt4_0.common.InvalidDurationException;
import org.globus.workspace.common.print.Print;
import org.globus.workspace.client_core.ExecutionProblem;
import org.globus.workspace.client_core.ParameterProblem;
import org.globus.workspace.client_core.StubConfigurator;
import org.globus.workspace.client_core.WSAction_Factory;
import org.globus.workspace.client_core.repr.GenericIntRange;
import org.globus.workspace.client_core.repr.Networking;
import org.globus.workspace.client_core.repr.ResourceAllocation;
import org.globus.workspace.client_core.repr.Schedule;
import org.globus.workspace.client_core.repr.ShutdownMech;
import org.globus.workspace.client_core.repr.State;
import org.globus.workspace.client_core.repr.Workspace;
import org.globus.workspace.client_core.utils.RMIUtils;
import org.nimbustools.messaging.gt4_0.generated.WorkspaceFactoryPortType;
import org.nimbustools.messaging.gt4_0.generated.ensemble.WorkspaceEnsembleFault;
import org.nimbustools.messaging.gt4_0.generated.metadata.VirtualWorkspace_Type;
import org.nimbustools.messaging.gt4_0.generated.metadata.logistics.VirtualNetwork_Type;
import org.nimbustools.messaging.gt4_0.generated.negotiable.DeploymentTime_Type;
import org.nimbustools.messaging.gt4_0.generated.negotiable.ResourceAllocation_Type;
import org.nimbustools.messaging.gt4_0.generated.negotiable.ShutdownMechanism_Type;
import org.nimbustools.messaging.gt4_0.generated.negotiable.WorkspaceDeployment_Type;
import org.nimbustools.messaging.gt4_0.generated.types.CreatedWorkspace_Type;
import org.nimbustools.messaging.gt4_0.generated.types.OptionalParameters_Type;
import org.nimbustools.messaging.gt4_0.generated.types.Schedule_Type;
import org.nimbustools.messaging.gt4_0.generated.types.WorkspaceCreateRequest_Type;
import org.nimbustools.messaging.gt4_0.generated.types.WorkspaceCreateResponse_Type;
import org.nimbustools.messaging.gt4_0.generated.types.WorkspaceCreationFault;
import org.nimbustools.messaging.gt4_0.generated.types.WorkspaceMetadataFault;
import org.nimbustools.messaging.gt4_0.generated.types.WorkspaceResourceRequestDeniedFault;
import org.nimbustools.messaging.gt4_0.generated.types.WorkspaceSchedulingFault;
import org.nimbustools.messaging.gt4_0.generated.types.WorkspaceContextualizationFault;

import java.rmi.RemoteException;

/**
 * See Action class notes for general usage information.
 *
 * See Workspace for notes on what will be populated for you after creation
 *
 * @see org.globus.workspace.client_core.Action
 *
 * @see WSAction_Factory
 *
 * @see Workspace
 *
 */
public class Create extends WSAction_Factory {

    
    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected VirtualWorkspace_Type vw;

    protected WorkspaceDeployment_Type req;

    protected OptionalParameters_Type opts;

    protected EndpointReferenceType joinEnsembleEPR;
    protected EndpointReferenceType joinContextEPR;

    protected boolean createEnsemble;
    protected boolean createContext;

    protected boolean lastInEnsemble;
    protected boolean lastInContext;

    // internal use

    protected boolean expectEnsembleEPR;
    protected boolean expectContextEPR;


    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    /**
     * @see WSAction_Factory
     */
    public Create(StubConfigurator stubConf,
                  Print debug) {
        super(stubConf, debug);
    }

    /**
     * @see WSAction_Factory
     */
    public Create(EndpointReferenceType epr,
                  StubConfigurator stubConf,
                  Print debug) {
        super(epr, stubConf, debug);
    }

    /**
     * @see WSAction_Factory
     */
    public Create(WorkspaceFactoryPortType factoryPortType,
                  Print debug) {
        super(factoryPortType, debug);
    }

    // -------------------------------------------------------------------------
    // GET/SET OPTIONS
    // -------------------------------------------------------------------------

    /**
     * @return vw to create
     */
    public VirtualWorkspace_Type getVw() {
        return this.vw;
    }

    /**
     * Set vw to create.
     *
     * @param workspace vw to create
     */
    public void setVw(VirtualWorkspace_Type workspace) {
        this.vw = workspace;
    }

    /**
     * @return deployment request in use
     */
    public WorkspaceDeployment_Type getReq() {
        return this.req;
    }

    /**
     * Set deployment request
     *
     * @param deploymentRequest deployment request
     */
    public void setReq(WorkspaceDeployment_Type deploymentRequest) {
        this.req = deploymentRequest;
    }

    /**
     * @return Optional Parameters in use or null
     */
    public OptionalParameters_Type getOpts() {
        return this.opts;
    }

    /**
     * Not required.
     *
     * May be null to override previous setting (default is null).
     *
     * @param optional optional parameters
     */
    public void setOptionalParameters(OptionalParameters_Type optional) {
        this.opts = optional;
    }

    /**
     * @return configured ensemble EPR or null
     */
    public EndpointReferenceType getJoinEnsembleEPR() {
        return this.joinEnsembleEPR;
    }

    /**
     * @return configured context EPR or null
     */
    public EndpointReferenceType getJoinContextEPR() {
        return this.joinContextEPR;
    }

    /**
     * @param epr ensemble EPR to join, may be null to override previous setting
     */
    public void setJoinEnsembleEPR(EndpointReferenceType epr) {
        this.joinEnsembleEPR = epr;
    }

    /**
     * @param epr context EPR to join, may be null to override previous setting
     */
    public void setJoinContextEPR(EndpointReferenceType epr) {
        this.joinContextEPR = epr;
    }

    /**
     * @return true if setCreateEnsemble(true)
     * @see #setCreateEnsemble(boolean)
     */
    public boolean isCreateEnsemble() {
        return this.createEnsemble;
    }

    /**
     * @return true if setCreateContext(true)
     * @see #setCreateContext(boolean)
     */
    public boolean isCreateContext() {
        return this.createContext;
    }

    /**
     * @param create true if the workspace creation request should trigger
     *               an ensemble creation
     */
    public void setCreateEnsemble(boolean create) {
        this.createEnsemble = create;
    }

    /**
     * @param create true if the workspace creation request should trigger
     *               a context creation
     */
    public void setCreateContext(boolean create) {
        this.createContext = create;
    }

    /**
     * @return true if setLastInEnsemble(true)
     * @see #setLastInEnsemble(boolean) 
     */
    public boolean isLastInEnsemble() {
        return this.lastInEnsemble;
    }

    /**
     * @return true if setLastInContext(true)
     * @see #setLastInContext(boolean) 
     */
    public boolean isLastInContext() {
        return this.lastInContext;
    }

    /**
     * Last-in-ensemble is a creation flag that triggers an "ensemble done"
     * operation after this workspace is created (it's all only one WS operation)
     *
     * Ensemble EPR must also be set if this is true.
     *
     * @param last true if the workspace creation request should trigger
     *             an "ensemble done" operation afterwards
     * @see #setJoinEnsembleEPR(EndpointReferenceType) 
     */
    public void setLastInEnsemble(boolean last) {
        this.lastInEnsemble = last;
    }

    /**
     * Last-in-context is a creation flag that triggers a "context lock"
     * operation after this workspace is created (it's all only one WS operation)
     *
     * @param last true if the workspace creation request should trigger
     *             a "context lock" operation afterwards
     * @see #setJoinContextEPR(EndpointReferenceType)
     */
    public void setLastInContext(boolean last) {
        this.lastInContext = last;
    }
    

    // -------------------------------------------------------------------------
    // VALIDATE
    // -------------------------------------------------------------------------

    /**
     * @throws ParameterProblem issue that will stop creation attempt
     */
    public void validateAll() throws ParameterProblem {
        super.validateAll();
        this.validateMetadata();
        this.validateDeploymentRequest();
        this.validateOptionalParameters();
        this.validateEnsembleRelated();
        this.validateContextRelated();
    }

    /**
     * @throws ParameterProblem issue that will stop creation attempt
     */
    public void validateMetadata() throws ParameterProblem {

        if (this.vw == null) {
            throw new ParameterProblem("no metadata is configured");
        }

        if (this.vw.getDefinition() == null) {
            throw new ParameterProblem("no definition in given metadata");
        }

        if (this.vw.getLogistics() == null) {
            throw new ParameterProblem("no logistics in given metadata");
        }

        if (this.vw.getName() == null) {
            // TODO: debug print this
            final URI name;
            try {
                name = new URI("http://no-name");
            } catch (URI.MalformedURIException e) {
                throw new ParameterProblem(
                        "default name problem: " + e.getMessage(), e);
            }
            this.vw.setName(name);
        }

        // do nothing else as of now, could get into pre-validating what
        // factory would semantically reject
    }

    /**
     * NodeNumber may not be uninitialized.
     *
     * Create will only accept requests with NodeNumber == 1
     * CreateGroup will only accept requests with NodeNumber > 1
     *
     * @see Create_Group#validateDeploymentRequestNodeNumber()
     *
     * @throws ParameterProblem issue that will stop creation attempt
     */
    public void validateDeploymentRequest() throws ParameterProblem {
        this.validateDeploymentRequestNodeNumber();
        this.validateDeploymentRequestCommon();
    }
    
    protected void validateDeploymentRequestCommon() throws ParameterProblem {

        if (this.req == null) {
            throw new ParameterProblem("no deployment request is configured");
        }

        this.validateDeploymentRequestNodeNumber();

        final ResourceAllocation_Type ra = this.req.getResourceAllocation();
        if (ra == null) {
            throw new ParameterProblem("ResourceAllocation is not present");
        }

        final RangeValue_Type rvt =
                this.req.getResourceAllocation().getIndividualPhysicalMemory();
        if (rvt == null) {
            throw new ParameterProblem(
                    "memory specification is not present");
        }
        final GenericIntRange exact = new GenericIntRange(rvt);
        if (exact.getMin() != exact.getMax()) {
            throw new ParameterProblem(
                    "memory range requests aren't supported right now");
        }

        if (this.req.getInitialState() != null) {
            final State testing =
                    State.fromInitialState_Type(this.req.getInitialState());
            if (testing == null) {
                throw new ParameterProblem(
                    "initial state in deployment request is unrecognized?");
            }
        }

        final DeploymentTime_Type time = this.req.getDeploymentTime();
        if (time == null) {
            throw new ParameterProblem("no DeploymentTime_Type in request?");
        }

        final Duration minDuration = time.getMinDuration();
        if (minDuration == null) {
            throw new ParameterProblem("no minDuration in request?");
        }

        try {
            if (CommonUtil.durationToSeconds(minDuration) < 1) {
                throw new ParameterProblem(
                        "minDuration in request is less than 1 second?");
            }
        } catch (InvalidDurationException e) {
            throw new ParameterProblem(
                        "minDuration is invalid: " + e.getMessage(), e);
        }
    }

    /**
     * Will only accept requests with NodeNumber == 1
     *
     * @throws ParameterProblem issue that will stop creation attempt
     */
    protected void validateDeploymentRequestNodeNumber()
            throws ParameterProblem {

        final int nodeNum = (int) this.req.getNodeNumber();
        if (nodeNum != 1) {
            throw new ParameterProblem(
                    "this class does not support group requests, " +
                            "deployment request is asking for " + nodeNum);
        }
    }

    /**
     * @throws ParameterProblem issue that will stop creation attempt
     */
    public void validateOptionalParameters() throws ParameterProblem {
        // do nothing as of now, could get into pre-validating what
        // factory would semantically reject 
    }

    /**
     * @throws ParameterProblem issue that will stop creation attempt
     */
    public void validateEnsembleRelated() throws ParameterProblem {

        if (this.joinEnsembleEPR != null && this.createEnsemble) {

            throw new ParameterProblem("\"create ensemble\" flag may not be " +
                    "set at the same time as a \"join ensemble\" EPR, that " +
                    "request would not make any sense");

        } else if (this.createEnsemble && this.lastInEnsemble) {

            throw new ParameterProblem("\"create ensemble\" flag may not be " +
                    "set at the same time as the \"last in ensemble\" flag, " +
                    "that request would not make any sense");
        }
    }

    /**
     * @throws ParameterProblem issue that will stop creation attempt
     */
    public void validateContextRelated() throws ParameterProblem {

        if (this.joinContextEPR != null && this.createContext) {

            throw new ParameterProblem("\"create context\" flag may not be " +
                    "set at the same time as a \"join context\" EPR, that " +
                    "request would not make any sense");

        } 
    }



    // -------------------------------------------------------------------------
    // EXECUTE
    // -------------------------------------------------------------------------

    protected Object action() throws Exception {
        return this.create();
    }

    /**
     * Create one workspace.
     *
     * @return Workspace
     * @throws ParameterProblem validation problem
     * @throws ExecutionProblem general problem running (connection errors etc)
     * @throws WorkspaceResourceRequestDeniedFault request can not be fulfilled
     *         because of either lack of current resources or the credential
     *         is not authorized to get requested resources (this includes
     *         less familiar limited resources such as public IP addresses)
     * @throws WorkspaceSchedulingFault issue scheduling the request
     * @throws WorkspaceMetadataFault invalid metadata
     * @throws WorkspaceEnsembleFault problem with ensemble service interaction
     * @throws WorkspaceContextualizationFault problem from context broker
     * @throws WorkspaceCreationFault uncategorized factory request issue
     */
    public Workspace create() throws ParameterProblem,
                                     ExecutionProblem,
                                     WorkspaceResourceRequestDeniedFault,
                                     WorkspaceSchedulingFault,
                                     WorkspaceMetadataFault,
                                     WorkspaceEnsembleFault,
                                     WorkspaceContextualizationFault,
                                     WorkspaceCreationFault {
        
        this.validateAll();
        final WorkspaceCreateResponse_Type response = this.createImpl();
        return this.handleInstanceCreation(response);
    }

    protected Workspace handleInstanceCreation(
                            WorkspaceCreateResponse_Type response)

            throws ExecutionProblem {


        final Workspace workspace = new Workspace();

        // these things are not based on any information returned
        try {
            this.populateInitialRepr(workspace);
        } catch (ParameterProblem e) {
            throw new ExecutionProblem(
                        "unexpected problem: " + e.getMessage(), e);
        }

        // length/not-null already checked
        final CreatedWorkspace_Type[] allrefs = response.getCreatedWorkspace();

        final Schedule_Type xmlSchedule = allrefs[0].getSchedule();
        if (xmlSchedule == null) {
            throw new ExecutionProblem(
                    "(?) no schedule in factory response");
        }

        final VirtualNetwork_Type xmlNetwork = allrefs[0].getNetworking();
        if (xmlNetwork != null) {
            workspace.setCurrentNetworking(new Networking(xmlNetwork));
        }

        workspace.setEpr(allrefs[0].getEpr());

        try {
            workspace.setInitialSchedule(new Schedule(xmlSchedule));
            // this is intentionally a separate object:
            workspace.setCurrentSchedule(new Schedule(xmlSchedule));
        } catch (InvalidDurationException e) {
            throw new ExecutionProblem(
                    "(?) invalid data in factory response: " +
                            e.getMessage(), e);
        }

        final EndpointReferenceType ensembleEPR = response.getEnsembleEPR();
        if (ensembleEPR != null) {
            workspace.setEnsembleMemberEPR(ensembleEPR);
        }

        final EndpointReferenceType contextEPR = response.getContextEPR();
        if (contextEPR != null) {
            workspace.setContextMemberEPR(contextEPR);
        }

        return workspace;
    }

    protected WorkspaceCreateResponse_Type createImpl()

                              throws WorkspaceCreationFault,
                                     WorkspaceResourceRequestDeniedFault,
                                     WorkspaceSchedulingFault,
                                     WorkspaceMetadataFault,
                                     WorkspaceEnsembleFault,
                                     WorkspaceContextualizationFault,
                                     ExecutionProblem {

        final WorkspaceCreateRequest_Type createRequest =
                                            new WorkspaceCreateRequest_Type();

        this.populateCreateRequest(createRequest);

        /* CREATE */

        final WorkspaceCreateResponse_Type response;
        try {
            response = ((WorkspaceFactoryPortType)this.portType).
                                                        create(createRequest);
        } catch (WorkspaceCreationFault e) {
            throw e;
        } catch (WorkspaceResourceRequestDeniedFault e) {
            throw e;
        } catch (WorkspaceSchedulingFault e) {
            throw e;
        } catch (WorkspaceMetadataFault e) {
            throw e;
        } catch (WorkspaceEnsembleFault e) {
            throw e;
        } catch (WorkspaceContextualizationFault e) {
            throw e;
        } catch (RemoteException e) {
            throw RMIUtils.generalRemoteException(e);
        }

        /* CHECK GENERAL ASSUMPTIONS */

        // the "(?)" symbol is a shorthand for "very unexpected"...

        if (response == null) {
            throw new ExecutionProblem("(?) null response");
        }

        final CreatedWorkspace_Type[] allrefs = response.getCreatedWorkspace();
        if (allrefs == null) {
            throw new ExecutionProblem(
                    "(?) null references in factory response");
        }

        /* CHECK NODE-NUMBER RELATED ASSUMPTIONS */

        final int expectedNodeNum =
                createRequest.getResourceRequest().getNodeNumber();

        if (expectedNodeNum == 1) {
            if (response.getGroupEPR() != null) {
                throw new ExecutionProblem("(?) group EPR assigned but " +
                        "this was not a group request");
            }
        } else {
            if (response.getGroupEPR() == null) {
                throw new ExecutionProblem("(?) group EPR was not assigned " +
                        "but this was a group request");
            }
        }

        if (allrefs.length != expectedNodeNum) {
            throw new ExecutionProblem("(?) expecting " + expectedNodeNum +
                  "CreatedWorkspace reference(s), received " + allrefs.length);
        }


        /* CHECK ENSEMBLE RELATED ASSUMPTIONS */

        final EndpointReferenceType ensembleEPR = response.getEnsembleEPR();

        if (this.expectEnsembleEPR && ensembleEPR == null) {
            throw new ExecutionProblem("(?) requested ensemble " +
                    "participation but no ensemble EPR was assigned");
        }

        if (!this.expectEnsembleEPR && ensembleEPR != null) {
            throw new ExecutionProblem("(?) ensemble EPR assigned but " +
                    "no request was made for ensemble participation");
        }

        /* CHECK CONTEXT RELATED ASSUMPTIONS */

        final EndpointReferenceType contextEPR = response.getContextEPR();

        if (this.expectContextEPR && contextEPR == null) {
            throw new ExecutionProblem("(?) requested context " +
                    "participation but no context EPR was assigned");
        }

        if (!this.expectContextEPR && contextEPR != null) {
            throw new ExecutionProblem("(?) context EPR assigned but " +
                    "no request was made for context participation");
        }

        return response;
    }

    /**
     * Validated already.  API caller can still cause problems if using
     * multiple thread writers and no locks etc.
     * 
     * @param createReq createReq
     */
    protected void populateCreateRequest(WorkspaceCreateRequest_Type createReq) {

        createReq.setMetadata(this.vw);

        createReq.setResourceRequest(this.req);

        if (this.opts != null) {
            createReq.setOptionalParameters(this.opts);
        }

        this.expectEnsembleEPR = false;

        if (this.createEnsemble) {
            createReq.setPartOfEnsemble(Boolean.TRUE);
            createReq.setEnsembleEPR(null);
            this.expectEnsembleEPR = true;
        } else if (this.joinEnsembleEPR != null) {
            createReq.setPartOfEnsemble(Boolean.TRUE);
            createReq.setEnsembleEPR(this.joinEnsembleEPR);
            this.expectEnsembleEPR = true;
        }

        if (this.lastInEnsemble) {
            createReq.setEnsembleDone(Boolean.TRUE);
        }

        this.expectContextEPR = false;

        if (this.createContext) {
            createReq.setContextEPR(null);
            this.expectContextEPR = true;
        } else if (this.joinContextEPR != null) {
            createReq.setContextEPR(this.joinContextEPR);
            this.expectContextEPR = true;
        }

        if (this.lastInContext) {
            createReq.setLockContext(Boolean.TRUE);
        }
        
    }

    /**
     * <p>Populate one workspace representation with as much data as possible
     * without using create's response object (using this class' instance
     * data).</p>
     *
     * <p>This can be called multiple times, all pointers going into the
     * workspace argument are to new, unique objects.  For example, this can
     * be used by CreateGroup to populate many workspace with unique state
     * objects (that can then diverge).</p>
     *
     * <p>Contents going into new Workspace representation are validated
     * already.  Although you can still cause problems if using multiple
     * thread writers and no locks etc., as noted in Action class notes.</p>
     * 
     * @param workspace workspace
     * @throws ParameterProblem validation problem, implies race condition
     *         with multiple threads unsafely using this object
     *
     */
    protected void populateInitialRepr(Workspace workspace)
            throws ParameterProblem {

        workspace.setUriName(this.vw.getName().toString());

        final VirtualNetwork_Type net = this.vw.getLogistics().getNetworking();
        if (net != null) {
            workspace.setRequestedNetworking(new Networking(net));
        }

        final ResourceAllocation_Type ra = this.req.getResourceAllocation();
        if (ra == null) {
            throw new ParameterProblem("(?) no ResourceAllocation_Type");
        }

        workspace.setRequestedResourceAllocation(new ResourceAllocation(ra));
        // this is intentionally a separate object:
        workspace.setCurrentResourceAllocation(new ResourceAllocation(ra));

        boolean trash = false;

        if (this.req.getShutdownMechanism() != null
            && this.req.getShutdownMechanism().equals(
                ShutdownMechanism_Type.Trash)) {

            trash = true;
        }
        final ShutdownMech mech = new ShutdownMech(trash);
        workspace.setRequestedShutdownMech(mech);

        if (this.req.getInitialState() == null) {
            workspace.setRequestedInitialState(State.DEFAULT_INITIAL_STATE);
        } else {
            final State initialState =
                    State.fromInitialState_Type(this.req.getInitialState());

            workspace.setRequestedInitialState(initialState);
        }

        // this might be incorrect information already, but it is the initial
        // state of all workspaces
        workspace.setCurrentState(new State(State.STATE_Unstaged));

        try {
            final DeploymentTime_Type time = this.req.getDeploymentTime();
            final int seconds =
                    CommonUtil.durationToSeconds(time.getMinDuration());
            final Schedule schedule = new Schedule();
            schedule.setDurationSeconds(seconds);
            workspace.setRequestedSchedule(schedule);
        } catch (NullPointerException e) {
            throw new ParameterProblem(e.getMessage(), e);
        } catch (InvalidDurationException e) {
            throw new ParameterProblem(e.getMessage(), e);
        }
    }
}
