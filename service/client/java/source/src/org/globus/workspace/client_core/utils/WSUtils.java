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

package org.globus.workspace.client_core.utils;

import org.nimbustools.messaging.gt4_0.generated.WorkspaceFactoryPortType;
import org.nimbustools.messaging.gt4_0.generated.WorkspaceFactoryServiceAddressingLocator;
import org.nimbustools.messaging.gt4_0.generated.WorkspacePortType;
import org.nimbustools.messaging.gt4_0.generated.WorkspaceServiceAddressingLocator;
import org.nimbustools.messaging.gt4_0.generated.status.WorkspaceStatusPortType;
import org.nimbustools.messaging.gt4_0.generated.status.WorkspaceStatusServiceAddressingLocator;
import org.nimbustools.messaging.gt4_0.generated.ensemble.WorkspaceEnsemblePortType;
import org.nimbustools.messaging.gt4_0.generated.ensemble.WorkspaceEnsembleServiceAddressingLocator;
import org.nimbustools.messaging.gt4_0.generated.negotiable.WorkspaceDeployment_Type;
import org.nimbustools.messaging.gt4_0.generated.negotiable.InitialState_Type;
import org.nimbustools.messaging.gt4_0.generated.negotiable.ShutdownMechanism_Type;
import org.nimbustools.messaging.gt4_0.generated.negotiable.DeploymentTime_Type;
import org.nimbustools.messaging.gt4_0.generated.negotiable.ResourceAllocation_Type;
import org.nimbustools.messaging.gt4_0.generated.negotiable.PostShutdown_Type;
import org.nimbustools.messaging.gt4_0.generated.group.WorkspaceGroupPortType;
import org.nimbustools.messaging.gt4_0.generated.group.WorkspaceGroupServiceAddressingLocator;
import org.nimbustools.messaging.gt4_0.common.Constants_GT4_0;
import org.nimbustools.ctxbroker.generated.gt4_0.broker.NimbusContextBrokerPortType;
import org.nimbustools.ctxbroker.generated.gt4_0.broker.NimbusContextBrokerServiceAddressingLocator;
import org.globus.workspace.client_core.ParameterProblem;
import org.globus.axis.util.Util;
import org.globus.wsrf.WSRFConstants;
import org.globus.wsrf.WSNConstants;
import org.globus.wsrf.impl.security.authentication.Constants;
import org.globus.wsrf.impl.security.descriptor.ClientSecurityDescriptor;
import org.globus.wsrf.impl.security.authorization.Authorization;
import org.apache.axis.message.addressing.EndpointReferenceType;
import org.apache.axis.types.Duration;
import org.apache.axis.types.URI;
import org.ggf.jsdl.Exact_Type;
import org.ggf.jsdl.RangeValue_Type;
import org.oasis.wsn.TopicExpressionType;

import javax.xml.rpc.ServiceException;
import java.util.List;
import java.util.Vector;
import java.util.MissingResourceException;

public class WSUtils {

    public static final List TERMINATION_TOPIC_PATH;
    public static final List CURRENT_STATE_TOPIC_PATH;

    static {
        TERMINATION_TOPIC_PATH = new Vector(1);
        TERMINATION_TOPIC_PATH.add(WSRFConstants.TERMINATION_TOPIC);

        CURRENT_STATE_TOPIC_PATH = new Vector(1);
        // topic path of a ResourcePropertyTopic is just the RP name
        CURRENT_STATE_TOPIC_PATH.add(Constants_GT4_0.RP_CURRENT_STATE);
    }

    public static TopicExpressionType getStateTopic() {
        final TopicExpressionType topicExpression = new TopicExpressionType();
        topicExpression.setDialect(getTopicDialect());
        topicExpression.setValue(Constants_GT4_0.RP_CURRENT_STATE);
        return topicExpression;
    }

    public static TopicExpressionType getTerminationTopic() {
        final TopicExpressionType topicExpression = new TopicExpressionType();
        topicExpression.setDialect(getTopicDialect());
        topicExpression.setValue(WSRFConstants.TERMINATION_TOPIC);
        return topicExpression;
    }

    private static URI getTopicDialect() {

        try {
            return new URI(WSNConstants.SIMPLE_TOPIC_DIALECT);
        } catch (URI.MalformedURIException e) {
            final String err =
                    "Correct URI constant is missing: " + e.getMessage();
            final String clazz = "WSNConstants";
            final String key = "SIMPLE_TOPIC_DIALECT";
            // not really, but close enough match...
            throw new MissingResourceException(err, clazz, key);
        }
    }

    public static WorkspacePortType initServicePortType(EndpointReferenceType epr)

            throws ServiceException {

        if (epr == null) {
            // getWorkspacePortTypePort does not check this for null
            throw new IllegalArgumentException("epr may not be null");
        }

        final WorkspaceServiceAddressingLocator locator =
                new WorkspaceServiceAddressingLocator();

        return locator.getWorkspacePortTypePort(epr);
    }

    public static WorkspaceFactoryPortType initFactoryPortType(EndpointReferenceType epr)
            throws ServiceException {

        if (epr == null) {
            // getWorkspaceFactoryPortTypePort does not check this for null
            throw new IllegalArgumentException("epr may not be null");
        }

        final WorkspaceFactoryServiceAddressingLocator locator =
                new WorkspaceFactoryServiceAddressingLocator();

        return locator.getWorkspaceFactoryPortTypePort(epr);
    }

    public static WorkspaceStatusPortType initStatusPortType(EndpointReferenceType epr)
            throws ServiceException {

        if (epr == null) {
            // getWorkspaceStatusPortTypePort does not check this for null
            throw new IllegalArgumentException("epr may not be null");
        }

        final WorkspaceStatusServiceAddressingLocator locator =
                new WorkspaceStatusServiceAddressingLocator();

        return locator.getWorkspaceStatusPortTypePort(epr);
    }

    public static WorkspaceGroupPortType initGroupServicePortType(EndpointReferenceType epr)
            throws ServiceException {

        if (epr == null) {
            // getWorkspaceGroupPortTypePort does not check this for null
            throw new IllegalArgumentException("epr may not be null");
        }

        final WorkspaceGroupServiceAddressingLocator locator =
                new WorkspaceGroupServiceAddressingLocator();

        return locator.getWorkspaceGroupPortTypePort(epr);
    }

    public static WorkspaceEnsemblePortType initEnsembleServicePortType(EndpointReferenceType epr)
            throws ServiceException {

        if (epr == null) {
            // getWorkspaceEnsemblePortTypePort does not check this for null
            throw new IllegalArgumentException("epr may not be null");
        }

        final WorkspaceEnsembleServiceAddressingLocator locator =
                new WorkspaceEnsembleServiceAddressingLocator();

        return locator.getWorkspaceEnsemblePortTypePort(epr);
    }

    public static NimbusContextBrokerPortType initContextBrokerPortType(EndpointReferenceType epr)
            throws ServiceException {

        if (epr == null) {
            // getWorkspaceContextBrokerPortTypePort does not check this for null
            throw new IllegalArgumentException("epr may not be null");
        }

        final NimbusContextBrokerServiceAddressingLocator locator =
                new NimbusContextBrokerServiceAddressingLocator();

        return locator.getNimbusContextBrokerPortTypePort(epr);
    }

    public static WorkspaceDeployment_Type constructDeploymentType(
                                int durationMinutes,
                                int memoryMegabytes,
                                int numNodes,
                                InitialState_Type requestState,
                                ShutdownMechanism_Type shutdownMechanism,
                                URI newPropagationTargetURI) {
        final int cores = -1;
        return constructDeploymentType(durationMinutes,
                                       memoryMegabytes,
                                       numNodes,
                                       requestState,
                                       shutdownMechanism,
                                       newPropagationTargetURI,
                                       cores);
    }

    public static WorkspaceDeployment_Type constructDeploymentType(
                                int durationMinutes,
                                int memoryMegabytes,
                                int numNodes,
                                InitialState_Type requestState,
                                ShutdownMechanism_Type shutdownMechanism,
                                URI newPropagationTargetURI,
                                int cores) {

        final WorkspaceDeployment_Type dep = new WorkspaceDeployment_Type();

        dep.setDeploymentTime(constructDeploymentTime_Type(durationMinutes));

        final ResourceAllocation_Type resAlloc = new ResourceAllocation_Type();
        setMemory(memoryMegabytes, resAlloc);
        setCores(cores, resAlloc);
        dep.setResourceAllocation(resAlloc);

        dep.setNodeNumber((short)numNodes);

        if (requestState != null) {
            dep.setInitialState(requestState);
        }

        if (shutdownMechanism != null) {
            dep.setShutdownMechanism(shutdownMechanism);
        }

        if (newPropagationTargetURI != null) {
            final PostShutdown_Type postTask = new PostShutdown_Type();
            postTask.setRootPartitionUnpropagationTarget(
                                            newPropagationTargetURI);
            dep.setPostShutdown(postTask);
        }

        return dep;
    }

    public static DeploymentTime_Type constructDeploymentTime_Type(int durationMinutes) {

        final Duration dur = new Duration();
        dur.setMinutes(durationMinutes);
        return new DeploymentTime_Type(dur);
    }

    public static void setMemory(int memoryMegabytes,
                                 ResourceAllocation_Type resAlloc) {

        if (resAlloc == null) {
            throw new IllegalArgumentException("resAlloc may not be null");
        }

        final Exact_Type[] exactMemAlloc = {new Exact_Type(memoryMegabytes)};
        final RangeValue_Type memRange = new RangeValue_Type();
        memRange.setExact(exactMemAlloc);
        resAlloc.setIndividualPhysicalMemory(memRange);
    }

    public static void setCores(int cores,
                                ResourceAllocation_Type resAlloc) {

        if (resAlloc == null) {
            throw new IllegalArgumentException("resAlloc may not be null");
        }

        // below 1 signals to use default which is picked by the target cloud (the old behavior)
        if (cores < 1) {
            return;
        }

        final Exact_Type[] exactCoresAlloc = {new Exact_Type(cores)};
        final RangeValue_Type coresRange = new RangeValue_Type();
        coresRange.setExact(exactCoresAlloc);
        resAlloc.setIndividualCPUCount(coresRange);
    }

    public static ClientSecurityDescriptor getClientSecDesc(
                                                       Object mech,
                                                       Object protection,
                                                       Authorization authz)
            throws ParameterProblem {

        if (mech == null) {
            throw new ParameterProblem(
                    "security mechanism is not specified");
        }

        if (protection == null) {
            throw new ParameterProblem(
                    "protection mechanism is not specified");
        }

        if (authz == null) {
            throw new ParameterProblem(
                    "service-authorization mechanism is not specified");
        }

        final ClientSecurityDescriptor desc = new ClientSecurityDescriptor();

        if (mech.equals(Constants.GSI_SEC_MSG)) {
            desc.setGSISecureMsg((Integer)protection);
        } else if (mech.equals(Constants.GSI_SEC_CONV)) {
            desc.setGSISecureConv((Integer)protection);
        } else if (mech.equals(Constants.GSI_TRANSPORT)) {
            desc.setGSITransport((Integer)protection);
            Util.registerTransport();
        }

        desc.setAuthz(authz);

        return desc;
    }
}
