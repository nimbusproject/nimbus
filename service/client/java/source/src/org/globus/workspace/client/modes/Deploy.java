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

package org.globus.workspace.client.modes;

import org.globus.workspace.client_core.StubConfigurator;
import org.globus.workspace.common.print.Print;
import org.globus.workspace.client_core.print.PrCodes;
import org.globus.workspace.client_core.ParameterProblem;
import org.globus.workspace.client_core.ExecutionProblem;
import org.globus.workspace.client_core.ExitNow;
import org.globus.workspace.client_core.repr.State;
import org.globus.workspace.client_core.utils.FileUtils;
import org.globus.workspace.client_core.utils.StateUtils;
import org.globus.workspace.client_core.utils.WSUtils;
import org.globus.workspace.client_core.utils.EPRUtils;
import org.globus.workspace.client.AllArguments;
import org.globus.workspace.client.Opts;
import org.globus.workspace.client.modes.aux.CommonLogs;
import org.nimbustools.messaging.gt4_0.generated.metadata.VirtualWorkspace_Type;
import org.nimbustools.messaging.gt4_0.generated.negotiable.WorkspaceDeployment_Type;
import org.nimbustools.messaging.gt4_0.generated.negotiable.InitialState_Type;
import org.nimbustools.messaging.gt4_0.generated.negotiable.ShutdownMechanism_Type;
import org.nimbustools.messaging.gt4_0.generated.negotiable.PostShutdown_Type;
import org.nimbustools.messaging.gt4_0.generated.types.OptionalParameters_Type;
import org.nimbustools.messaging.gt4_0.generated.types.CustomizeTask_Type;
import org.nimbustools.messaging.gt4_0.common.CommonUtil;
import org.nimbustools.messaging.gt4_0.common.InvalidDurationException;
import org.globus.wsrf.impl.security.authentication.Constants;
import org.globus.wsrf.impl.security.authorization.Authorization;
import org.globus.wsrf.impl.security.authorization.HostAuthorization;
import org.globus.wsrf.utils.AddressingUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.axis.message.addressing.EndpointReferenceType;
import org.apache.axis.types.URI;

import java.io.IOException;
import java.io.File;
import java.net.URL;
import java.net.MalformedURLException;

public class Deploy extends Subscribe {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(Deploy.class.getName());

    
    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    // Factory create parameters:
    VirtualWorkspace_Type vw;
    WorkspaceDeployment_Type dep;
    OptionalParameters_Type optionalParameters;

    // Factory target:
    EndpointReferenceType factoryEPR;

    // note that "subscribe" has same meaning here whether polling or not
    boolean subscribeAfterDeployment;

    // misc
    String groupBaseName;
    String instanceEprPath;
    String groupEprPath;
    StubConfigurator stubConfigurator;
    boolean printLikeGroup;
    
    // l'ensemble
    boolean createEnsemble;
    EndpointReferenceType joinEnsembleEPR;
    boolean lastInEnsemble;
    String newEnsembleEprPath;

    // Delegation related:
    String delegationFactoryUrl;
    int numberOfDelegationsNeeded;
    int delegationLifetime;
    Object delegationSecMechanism;
    Object delegationProtection;
    Authorization delegationAuthorization;
    boolean delegationXferCredToo;

    
    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    public Deploy(Print print,
                  AllArguments arguments,
                  StubConfigurator stubConfigurator) {
        super(print, arguments, stubConfigurator);
        this.stubConfigurator = stubConfigurator;
    }
    

    // -------------------------------------------------------------------------
    // GENERAL
    // -------------------------------------------------------------------------

    public String name() {
        return "Deploy";
    }

    // too much for this file, delegate out
    public void runImpl() throws ParameterProblem, ExecutionProblem, ExitNow {
        try {
            new DeployRun(this, this.pr).run();
        } finally {
            this.doneCleanupSubscriptions();
        }
    }

    
    // -------------------------------------------------------------------------
    // VALIDATE/INTAKE (GENERAL)
    // -------------------------------------------------------------------------

    // overrides Subscribe
    public void validateOptionsImpl() throws ParameterProblem {

        this.validateDeployEndpoint();
        this.validateMetadata();
        this.validateDeployName();
        this.validateDeploymentRequest();
        this.validateOptionalParameters();
        this.validateEnsembleCmdlineArgs();
        this.validateMiscCmdlineArgs();

        if (this.dep.getNodeNumber() > 1) {
            this.isGroupRequest = true;
        }

        this.validateSubscriptionImpl();
    }

    private void validateDeployName() throws ParameterProblem {

        String metadataName = null;
        if (this.vw.getName() != null) {
            metadataName = this.vw.getName().toString();
        }

        if (this.pr.enabled()) {
            final String dbg;
            if (metadataName == null) {
                dbg = "Metadata name is null";
            } else {
                dbg = "Metadata name: " + metadataName + "'";
            }
            if (this.pr.useThis()) {
                this.pr.dbg(dbg);
            } else if (this.pr.useLogging()) {
                logger.debug(dbg);
            }
        }

        this.shortName = this.args.shortName;
        if (this.pr.enabled()) {
            final String dbg;
            if (this.shortName == null) {
                dbg = "Given display name is null";
            } else {
                dbg = "Given display name: " + this.shortName + "'";
            }
            if (this.pr.useThis()) {
                this.pr.dbg(dbg);
            } else if (this.pr.useLogging()) {
                logger.debug(dbg);
            }
        }

        if (this.shortName != null) {
            this.nameToPrint = this.shortName;
        } else {
            this.nameToPrint = metadataName;
        }

    }

    
    // -------------------------------------------------------------------------
    // VALIDATE/INTAKE REQUIREMENTS
    // -------------------------------------------------------------------------

    private void validateDeployEndpoint() throws ParameterProblem {

        if (this.stubConf.getEPR() == null) {

            final String urlString;
            if (this.args.targetServiceUrl == null) {
                urlString = EPRUtils.defaultFactoryUrlString;
            } else {
                urlString = this.args.targetServiceUrl;
            }

            try {
                new URL(urlString);
            } catch (MalformedURLException e) {
                throw new ParameterProblem("Given factory service URL " +
                        "appears to be invalid: " + e.getMessage(), e);
            }

            if (this.pr.enabled()) {
                // address print
                final String msg = "Workspace Factory Service:\n    " +
                                        urlString;
                if (this.pr.useThis()) {
                    this.pr.infoln(PrCodes.CREATE__FACTORY_ENDPOINT,
                                   msg);
                } else if (this.pr.useLogging()) {
                    logger.info(msg);
                }
            }

            try {
                this.factoryEPR = AddressingUtils.createEndpointReference(
                                       urlString, EPRUtils.defaultFactoryKey());
            } catch (Exception e) {
                final String err = "Problem creating factory endpoint: ";
                throw new ParameterProblem(err + e.getMessage(), e);
            }

            this.stubConf.setEPR(this.factoryEPR);

        } else {

            this.factoryEPR = this.stubConf.getEPR();

            final String eprStr;
            try {
                eprStr = EPRUtils.eprToString(this.factoryEPR);
            } catch (Exception e) {
                throw new ParameterProblem(e.getMessage(), e);
            }

            if (this.pr.enabled()) {
                // xml print
                final String dbg =
                        "\nWorkspace Factory Service EPR:" +
                                "\n------------------------------\n" +
                                    eprStr + "------------------------------\n";
                
                if (this.pr.useThis()) {
                    this.pr.dbg(dbg);
                } else if (this.pr.useLogging()) {
                    logger.debug(dbg);
                }
            }

            if (this.pr.enabled()) {
                // address print
                final String msg = "\nWorkspace Factory Service:\n    " +
                                this.factoryEPR.getAddress().toString() + "\n";
                if (this.pr.useThis()) {
                    this.pr.infoln(PrCodes.CREATE__FACTORY_ENDPOINT,
                                   msg);
                } else if (this.pr.useLogging()) {
                    logger.info(msg);
                }
            }
        }
    }

    private void validateMetadata() throws ParameterProblem {
        if (this.args.metadataPath == null) {
            throw new ParameterProblem(name() + " requires metadata file.");
        }

        try {
            this.vw = FileUtils.getMetadataFromFile(this.pr,
                                                    this.args.metadataPath);
        } catch (Exception e) {
            final String err = "Problem with metadata file: ";
            throw new ParameterProblem(err + e.getMessage(), e);
        }
    }

    private void validateDeploymentRequest() throws ParameterProblem {

        if (this.args.depRequestFilePath != null) {
            try {
                this.dep = FileUtils.getRequestFromFile(
                                    this.pr, this.args.depRequestFilePath);
            } catch (Exception e) {
                final String err = "Problem with deployment request file: ";
                throw new ParameterProblem(err + e.getMessage(), e);
            }
        }

        int deployDuration = 0;
        if (this.args.deploy_DurationString != null) {
            deployDuration = this._getDurationArg();
        }

        int memoryRequest = 0;
        if (this.args.deploy_MemoryString != null) {
            memoryRequest = this._getMemoryArg();
        }

        short numNodes = 0;
        if (this.args.deploy_NumNodesString != null) {
            numNodes = this._getNumNodesArg();
        }

        InitialState_Type requestState = null;
        if (this.args.deploy_StateString != null) {
            requestState = this._getDeployStateArg();
        }

        ShutdownMechanism_Type shutdownMechanism = null;
        if (this.args.trashAtShutdown) {
            shutdownMechanism = ShutdownMechanism_Type.Trash;
        }

        URI newPropagationTargetURI = null;
        try {
            if (this.args.saveTarget != null) {
                newPropagationTargetURI = new URI(this.args.saveTarget);
            }
        } catch (Exception e) {
            throw new ParameterProblem("Problem with save-target '" +
                    this.args.saveTarget + "': " + e.getMessage(), e);
        }

        if (this.dep == null) {

            boolean end = false;
            
            String err = name() + " requires a deployment request.  This " +
                    "can be specified by file or by a number of arguments." +
                    "  Problem encountered is that a file request is not " +
                    "configured and expected commandline arguments are " +
                    "missing: ";

            if (deployDuration < 1 && memoryRequest < 1) {
                end = true;
                err += "duration and memory request.";
            } else if (memoryRequest < 1) {
                end = true;
                err += "memory request.";
            } else if (deployDuration < 1) {
                end = true;
                err += "duration.";
            }

            if (end) {
                throw new ParameterProblem(err);
            }

            // default is 1
            if (numNodes < 1) {
                numNodes = 1;
            }

            // default is Running
            if (requestState == null) {
                requestState = InitialState_Type.Running;
            }

            // shutdownMechanism has no default except 'not set' which
            // triggers 'normal'

            this.dep = WSUtils.constructDeploymentType(deployDuration,
                                                       memoryRequest,
                                                       numNodes,
                                                       requestState,
                                                       shutdownMechanism,
                                                       newPropagationTargetURI);

            if (this.pr.enabled()) {
                final String msg =
                        "Created deployment request soley from arguments.";
                if (this.pr.useThis()) {
                    this.pr.infoln(
                            PrCodes.DEPREQ__USING_ARGS, msg);
                } else if (this.pr.useLogging()) {
                    logger.info(msg);
                }
            }

        } else {

            if (deployDuration > 0) {
                if (this.pr.enabled()) {
                    final String msg =
                        "Duration argument provided: overriding duration " +
                        "found in deployment request file, it is now: " +
                        deployDuration + " minutes";

                    final String dbg;
                    try {
                        final int oldMins = CommonUtil.durationToMinutes(
                                this.dep.getDeploymentTime().getMinDuration());
                        dbg = "Old duration: " + oldMins + " minutes";
                    } catch (InvalidDurationException e) {
                        throw new ParameterProblem(e.getMessage(), e);
                    }

                    if (this.pr.useThis()) {
                        this.pr.infoln(
                            PrCodes.DEPREQ__FILE_OVERRIDE, msg);
                        this.pr.dbg(dbg);
                    } else if (this.pr.useLogging()) {
                        logger.info(msg);
                        logger.debug(dbg);
                    }
                }
                this.dep.setDeploymentTime(
                        WSUtils.constructDeploymentTime_Type(deployDuration));
            }

            if (memoryRequest > 0) {
                if (this.pr.enabled()) {
                    final String msg =
                        "Memory argument provided: overriding memory " +
                        "found in deployment request file, it is now: " +
                        memoryRequest + " MB";

                    if (this.pr.useThis()) {
                        this.pr.infoln(
                            PrCodes.DEPREQ__FILE_OVERRIDE, msg);
                    } else if (this.pr.useLogging()) {
                        logger.info(msg);
                    }
                }

                WSUtils.setMemory(memoryRequest,
                                  this.dep.getResourceAllocation());
            }

            if (numNodes > 0) {
                if (this.pr.enabled()) {
                    final String msg =
                        "Num-nodes argument provided: overriding number " +
                        "found in deployment request file, it is now: " +
                        numNodes + " nodes";

                    final String dbg = "Old num-nodes: " +
                                this.dep.getNodeNumber();

                    if (this.pr.useThis()) {
                        this.pr.infoln(
                            PrCodes.DEPREQ__FILE_OVERRIDE, msg);
                        this.pr.dbg(dbg);
                    } else if (this.pr.useLogging()) {
                        logger.info(msg);
                        logger.debug(dbg);
                    }
                }

                this.dep.setNodeNumber(numNodes);
            }

            if (requestState != null) {

                if (this.pr.enabled()) {
                    final String msg =
                        "Initial state argument provided: overriding " +
                        "initial state setting found in deployment request " +
                        "file, it is now: " + requestState.getValue();

                    final String dbg = "Old state: " +
                                this.dep.getInitialState().getValue();

                    if (this.pr.useThis()) {
                        this.pr.infoln(
                            PrCodes.DEPREQ__FILE_OVERRIDE, msg);
                        this.pr.dbg(dbg);
                    } else if (this.pr.useLogging()) {
                        logger.info(msg);
                        logger.debug(dbg);
                    }
                }

                this.dep.setInitialState(requestState);
            }

            if (shutdownMechanism != null) {

                if (this.pr.enabled()) {

                    boolean presentInFile = false;
                    String dbg = "No setting in file for shutdown mechanism";
                    if (this.dep.getShutdownMechanism() != null) {
                        presentInFile = true;
                        dbg = "Old shutdown mechanism: " +
                            this.dep.getShutdownMechanism().getValue();
                    }

                    final String msg;
                    if (presentInFile) {
                        msg = "Shutdown mechanism argument provided: " +
                               "overriding shutdown mechanism setting found " +
                               "in deployment request file, it is now: " +
                               requestState.getValue();
                    } else {
                        msg = "Setting shutdown mechanism to: " +
                               requestState.getValue();
                    }

                    if (this.pr.useThis()) {
                        this.pr.infoln(
                            PrCodes.DEPREQ__FILE_OVERRIDE, msg);
                        this.pr.dbg(dbg);
                    } else if (this.pr.useLogging()) {
                        logger.info(msg);
                        logger.debug(dbg);
                    }
                }

                this.dep.setShutdownMechanism(shutdownMechanism);
            }

            if (newPropagationTargetURI != null) {

                if (this.pr.enabled()) {

                    boolean presentInFile = false;
                    String dbg = "No setting in file for alternate " +
                            "propagation target";
                    
                    if (this.dep.getPostShutdown() != null) {

                        if (this.dep.getPostShutdown().
                                getRootPartitionUnpropagationTarget() != null) {

                            presentInFile = true;
                            dbg = "Old alternate propagation target: " +
                                    this.dep.getPostShutdown().
                                        getRootPartitionUnpropagationTarget().toString();
                        }
                    }

                    final String msg;
                    if (presentInFile) {
                        msg = "Alternate propagation target argument provided: " +
                               "overriding the setting found " +
                               "in deployment request file, it is now '" +
                               newPropagationTargetURI.toString() + "'";
                    } else {
                        msg = "Setting alternate propagation target to '" +
                               newPropagationTargetURI.toString() + "'";
                    }

                    if (this.pr.useThis()) {
                        this.pr.infoln(
                            PrCodes.DEPREQ__FILE_OVERRIDE, msg);
                        this.pr.dbg(dbg);
                    } else if (this.pr.useLogging()) {
                        logger.info(msg);
                        logger.debug(dbg);
                    }
                }

                PostShutdown_Type postTasks = this.dep.getPostShutdown();
                if (postTasks == null) {
                    postTasks = new PostShutdown_Type();
                }

                postTasks.setRootPartitionUnpropagationTarget(
                                                newPropagationTargetURI);

                this.dep.setPostShutdown(postTasks);
            }
        }
    }

    private int _getDurationArg() throws ParameterProblem {
        
        final int deployDuration;
        try {
            deployDuration = Integer.parseInt(this.args.deploy_DurationString);
        } catch (NumberFormatException nfe) {
            throw new ParameterProblem(
                    "The given duration argument is not a valid number " +
                        "(given '" + this.args.deploy_DurationString + "')");
        }

        if (deployDuration < 1) {
            throw new ParameterProblem(
                    "The given duration argument is less than one " +
                        "(given '" + this.args.deploy_DurationString + "')");
        }

        return deployDuration;
    }

    private int _getMemoryArg() throws ParameterProblem {

        final int memory;
        try {
            memory = Integer.parseInt(this.args.deploy_MemoryString);
        } catch (NumberFormatException nfe) {
            throw new ParameterProblem(
                    "The given memory argument is not a valid number " +
                        "(given '" + this.args.deploy_MemoryString + "')");
        }

        if (memory < 1) {
            throw new ParameterProblem(
                    "The given memory argument is less than one " +
                        "(given '" + this.args.deploy_MemoryString + "')");
        }

        return memory;
    }

    private short _getNumNodesArg() throws ParameterProblem {

        final int numNodes;
        try {
            numNodes = Integer.parseInt(this.args.deploy_NumNodesString);
        } catch (NumberFormatException nfe) {
            throw new ParameterProblem(
                    "The given num-nodes argument is not a valid number " +
                        "(given '" + this.args.deploy_NumNodesString + "')");
        }

        if (numNodes < 1) {
            throw new ParameterProblem(
                    "The given num-nodes argument is less than one " +
                        "(given '" + this.args.deploy_NumNodesString + "')");
        }

        if (numNodes > Short.MAX_VALUE) {
            throw new ParameterProblem("Your num-nodes request exceeds the " +
                    "capacity of the num-nodes data-type.");
        }

        return (short)numNodes;
    }

    private InitialState_Type _getDeployStateArg() throws ParameterProblem {

        // will handle case difference:
        if (!StateUtils.isValidRequestState(this.args.deploy_StateString)) {
            throw new ParameterProblem(
                    "The given request-state argument is not a valid " +
                            "initial state to request (given '" +
                            this.args.deploy_StateString + "')");
        }

        // InitialState_Type.fromValue() will not handle case difference,
        // which is why do the following annoying thing:

        final State st = new State(this.args.deploy_StateString);

        if (st.isPaused()) {
            return InitialState_Type.Paused;
        }

        if (st.isPropagated()) {
            return InitialState_Type.Propagated;
        }

        if (st.isRunning()) {
            return InitialState_Type.Running;
        }

        if (st.isUnpropagated()) {
            return InitialState_Type.Unpropagated;
        }

        if (st.isUnstaged()) {
            return InitialState_Type.Unstaged;
        }

        throw new IllegalStateException(
                "isValidRequestState() is apparently wrong.");
    }


    // -------------------------------------------------------------------------
    // VALIDATE/INTAKE OPTIONAL PARAMETERS
    // -------------------------------------------------------------------------

    private void validateOptionalParameters() throws ParameterProblem {

        if (this.args.optionalParametersPath != null) {
            try {
                this.optionalParameters =
                        FileUtils.getOptionalFromFile(
                                this.pr, this.args.optionalParametersPath);
            } catch (Exception e) {
                final String err = "Problem with optional parameters file: ";
                throw new ParameterProblem(err + e.getMessage(), e);
            }
        } else {
            this.optionalParameters = new OptionalParameters_Type();
        }

        if (this.args.sshKeyPath != null) {
            this._handleSshKeyPath();
        }

        if (this.args.mdUserDataPath != null) {
            this._handleMdUserDataPath();
        }

        this._handleStaging();
    }

    private void _handleStaging() throws ParameterProblem {

        this.numberOfDelegationsNeeded = 0;
        if (this.optionalParameters.getStageIn() != null
                &&
                this.optionalParameters.getStageIn().getStagingCredential() == null) {
            
            if (this.args.delegationFactoryUrl != null) {
                this.numberOfDelegationsNeeded += 1;
            }
        }

        if (this.optionalParameters.getStageOut() != null
                && this.optionalParameters.getStageOut().getStagingCredential() == null) {
            if (this.args.delegationFactoryUrl != null) {
                this.numberOfDelegationsNeeded += 1;
            }
        }

        if (this.numberOfDelegationsNeeded == 0) {

            if (this.pr.enabled()) {
                final String msg = "No staging credential needed.";
                if (this.pr.useThis()) {
                    this.pr.dbg(msg);
                } else if (this.pr.useLogging()) {
                    logger.debug(msg);
                }
            }

            return;  // *** EARLY RETURN ***
        }

        if (this.pr.enabled()) {

            final String msg = "Requested delegation and " +
                    this.numberOfDelegationsNeeded + " staging request";
            if (this.numberOfDelegationsNeeded == 1) {
                this.pr.infoln(msg + " needs a staging credential.");
            } else {
                this.pr.infoln(msg + "s need a staging credential.");
            }

            if (this.pr.useThis()) {
                this.pr.infoln(PrCodes.OPTIONALPARAM__THEREST, msg);
            } else if (this.pr.useLogging()) {
                logger.info(msg);
            }
        }

        if (this.args.delegationFactoryUrl == null) {
            throw new ParameterProblem("Delegation is required but a " +
                    "delegation factory URL was not supplied");
        } else {
            this.delegationFactoryUrl = this.args.delegationFactoryUrl;
        }

        if (this.args.delegationLifetimeString != null) {
            try {
                this.delegationLifetime =
                        Integer.parseInt(this.args.delegationLifetimeString);
            } catch (NumberFormatException nfe) {
                throw new ParameterProblem(
                        "The given delegation lifetime argument is not a " +
                        "valid number (given '" + 
                        this.args.delegationLifetimeString + "')");
            }
            if (this.pr.enabled()) {
                final String msg =
                        "Delegation lifetime: " + this.delegationLifetime;
                if (this.pr.useThis()) {
                    this.pr.dbg(msg);
                } else if (this.pr.useLogging()) {
                    logger.debug(msg);
                }
            }
        }

        if (this.args.delegationXferCredToo) {
            this.delegationXferCredToo = true;
            if (this.pr.enabled()) {
                final String msg = "Going to use same delegated " +
                        "credential as the transfer credential";
                if (this.pr.useThis()) {
                    this.pr.dbg(msg);
                } else if (this.pr.useLogging()) {
                    logger.debug(msg);
                }
            }
        }

        this.delegationSecMechanism = this.stubConf.getMechanism();
        if (this.delegationSecMechanism == null) {
            this.delegationSecMechanism = Constants.GSI_TRANSPORT;
            if (this.pr.enabled()) {
                final String msg = "Going to use transport security for " +
                    "delegation factory call.";
                if (this.pr.useThis()) {
                    this.pr.dbg(msg);
                } else if (this.pr.useLogging()) {
                    logger.debug(msg);
                }
            }
        }

        this.delegationProtection = this.stubConf.getProtection();
        if (this.delegationProtection == null) {
            this.delegationProtection = Constants.SIGNATURE;
            if (this.pr.enabled()) {
                final String msg = "Going to use signature for " +
                    "delegation factory call.";
                if (this.pr.useThis()) {
                    this.pr.dbg(msg);
                } else if (this.pr.useLogging()) {
                    logger.debug(msg);
                }
            }
        }

        this.delegationAuthorization = this.stubConf.getAuthorization();
        if (this.delegationAuthorization == null) {
            this.delegationAuthorization = HostAuthorization.getInstance();
            if (this.pr.enabled()) {
                final String msg = "Going to use host authz for " +
                    "delegation factory call.";
                if (this.pr.useThis()) {
                    this.pr.dbg(msg);
                } else if (this.pr.useLogging()) {
                    logger.debug(msg);
                }
            }
        }
    }

    private void _handleSshKeyPath() throws ParameterProblem {

        final CustomizeTask_Type[] previous =
                    this.optionalParameters.getFilewrite();

        if (previous != null && previous.length > 0) {
            final String msg = "Replacing previous customization tasks " +
                    "with ONLY an SSH policy file write";
            if (this.pr.useThis()) {
                this.pr.infoln(
                    PrCodes.OPTIONALPARAM__CUSTOMIZATION_OVERRIDE,
                    msg);
            } else if (this.pr.useLogging()) {
                logger.info(msg);
            }
        }

        final String sshPolicy;
        try {
            sshPolicy =
                    FileUtils.getSshPolicyFromFile(this.pr,
                                                   this.args.sshKeyPath);
        } catch (IOException e) {
            final String err = "Problem with SSH policy file: ";
            throw new ParameterProblem(err + e.getMessage(), e);
        }

        if (sshPolicy == null || sshPolicy.trim().length() == 0) {
            throw new ParameterProblem("SSH policy file is empty?");
        }

        final CustomizeTask_Type ctt =
                new CustomizeTask_Type(sshPolicy,
                                       "/root/.ssh/authorized_keys");
        final CustomizeTask_Type[] ctts = {ctt};
        this.optionalParameters.setFilewrite(ctts);
    }

    private void _handleMdUserDataPath() throws ParameterProblem {


        final String userData;
        try {
            userData =
                    FileUtils.getUserDataFromFile(this.pr,
                                                  this.args.mdUserDataPath);
        } catch (IOException e) {
            final String err = "Problem with user data file: ";
            throw new ParameterProblem(err + e.getMessage(), e);
        }

        if (userData == null || userData.trim().length() == 0) {
            throw new ParameterProblem(
                    "User data file is empty? (specified file '" + 
                            this.args.mdUserDataPath + "')");
        }
        
        this.optionalParameters.setMdServerUserdata(userData);
    }


    // -------------------------------------------------------------------------
    // VALIDATE/INTAKE ENSEMBLE ARGUMENTS
    // -------------------------------------------------------------------------

    private void validateEnsembleCmdlineArgs() throws ParameterProblem {

        if (this.args.joinEnsembleEprFile != null &&
                this.args.newEnsembleEprFile != null) {

            throw new ParameterProblem("Both --" +
                    Opts.ENSEMBLE_JOIN_OPT_STRING + " and --" +
                    Opts.ENSEMBLE_NEW_OPT_STRING + " were given.  " +
                    "Only provide one.  --" +
                    Opts.ENSEMBLE_JOIN_OPT_STRING + " if you want " +
                    "this deployment to be part of an already " +
                    "created ensemble, --" +
                    Opts.ENSEMBLE_NEW_OPT_STRING + " if a new " +
                    "ensemble should be created along with this " +
                    "deployment.");
        }

        this.lastInEnsemble = this.args.lastInEnsemble;

        if (this.args.newEnsembleEprFile != null && this.lastInEnsemble) {

            throw new ParameterProblem("You may not specify --" +
                    Opts.ENSEMBLE_NEW_OPT_STRING + " (which " +
                    "requests a new ensemble be created) and --" +
                    Opts.ENSEMBLE_LAST_OPT_STRING + ".");
        }

        this.joinEnsembleEPR = null;
        if (this.args.joinEnsembleEprFile != null) {

            try {
                this.joinEnsembleEPR =
                        FileUtils.getEPRfromFile(this.args.joinEnsembleEprFile);
            } catch (Exception e) {
                throw new ParameterProblem("Problem deserializing " +
                        "EPR from '" + this.args.joinEnsembleEprFile +
                        "' file: " + e.getMessage(), e);
            }

            if(!EPRUtils.isEnsembleEPR(this.joinEnsembleEPR)) {
                throw new ParameterProblem("File '" +
                        this.args.joinEnsembleEprFile + "' does not " +
                        "contain a valid ensemble EPR file");
            }
        }

        if (this.args.newEnsembleEprFile != null) {
            this.createEnsemble = true;
            this.newEnsembleEprPath = this.args.newEnsembleEprFile;
        }
    }
    

    // -------------------------------------------------------------------------
    // VALIDATE/INTAKE MISC. ARGUMENTS
    // -------------------------------------------------------------------------

    private void validateMiscCmdlineArgs() throws ParameterProblem {

        this._handleEprFiles();
        this._handleExitState();
        this._handleVeryTerseNotifyState();
        this._handlePollDelay();
        this._handlePollMaxThreads();
        this._handleListenerOverride();

        this.dryrun = this.args.dryrun;
        CommonLogs.logBoolean(this.dryrun, "dryrun mode", this.pr, logger);

        this.autodestroy = this.args.autodestroy;
        CommonLogs.logBoolean(this.autodestroy, "autodestroy mode",
                              this.pr, logger);

        this.printLikeGroup = this.args.printLikeGroup;
        CommonLogs.logBoolean(this.printLikeGroup, "print like group",
                              this.pr, logger);

        this.subscribeAfterDeployment = this.args.subscriptions;
        CommonLogs.logBoolean(this.subscribeAfterDeployment,
                              "subscriptions after deploy",
                              this.pr, logger);

        this._logSubscribeStatus();
    }

    private void _handleEprFiles() throws ParameterProblem {

        if (this.dep.getNodeNumber() > 1) {
            
            this.groupEprPath = this.args.groupEprFile;
            this.groupBaseName = this.args.eprFile;

            if (this.pr.enabled()) {
                final String dbg = "Group request, using 'file' arg as " +
                        "group base name: '" + this.groupBaseName + "', " +
                        "path to group EPR file that will be created: '" +
                        this.groupEprPath + "'";
                if (this.pr.useThis()) {
                    this.pr.dbg(dbg);
                } else if (this.pr.useLogging()) {
                    logger.debug(dbg);
                }
            }

        } else {
            
            this.instanceEprPath = this.args.eprFile;

            if (this.pr.enabled()) {
                final String dbg = "Path to the EPR file that will be " +
                        "created: '" + this.instanceEprPath + "'";
                if (this.pr.useThis()) {
                    this.pr.dbg(dbg);
                } else if (this.pr.useLogging()) {
                    logger.debug(dbg);
                }
            }
        }

        if (this.args.eprIdDir != null) {
            final File hacky = new File(this.args.eprIdDir);
            if (!hacky.exists()) {
                throw new ParameterProblem(
                        "Does not exist: " + this.args.eprIdDir);
            }
            if (!hacky.isDirectory()) {
                throw new ParameterProblem(
                        "Must be a directory: " + this.args.eprIdDir);
            }
            if (!hacky.canWrite()) {
                throw new ParameterProblem(
                        "Can not write to: " + this.args.eprIdDir);
            }
        }
    }

    // overrides Subscribe
    protected void _logSubscribeStatus() {

        if (!this.pr.enabled()) {
            return; // *** EARLY RETURN ***
        }

        String listenTail = "";
        if (this.notificationListenerOverride_IPorHost != null
                && this.notificationListenerOverride_Port != null) {

            final String addr =
                    this.notificationListenerOverride_IPorHost + ":" +
                            this.notificationListenerOverride_Port.intValue();

            listenTail = " (listener override host+port: '" + addr + "')";

        } else if (this.notificationListenerOverride_IPorHost != null) {

            listenTail = " (listener override host: '" +
                    this.notificationListenerOverride_IPorHost + "')";
            
        }

        final String dbg;
        if (!this.subscribeAfterDeployment) {
            dbg = "subscription mode: DISABLED";
        } else if (this.pollDontListen) {
            dbg = "subscription mode: POLL (" + this.pollMS + "ms delay)";
        } else {
            dbg = "subscription mode: LISTENER" + listenTail;
        }
        if (this.pr.useThis()) {
            this.pr.dbg(dbg);
        } else if (this.pr.useLogging()) {
            logger.debug(dbg);
        }
    }


    // -------------------------------------------------------------------------
    // VALIDATE SUBSCRIPTION IMPLEMENTATION
    // -------------------------------------------------------------------------

    private void validateSubscriptionImpl() throws ParameterProblem {

        if (!this.subscribeAfterDeployment) {
            return;
        }

        if (this.pollDontListen) {
            this._handleSubscribeWithPoll();
        } else {
            this._handleSubscribeWithListen();
        }
    }
}
