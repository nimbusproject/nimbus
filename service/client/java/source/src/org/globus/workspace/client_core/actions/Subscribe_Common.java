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

import org.globus.workspace.client_core.Action;
import org.globus.workspace.client_core.ParameterProblem;
import org.globus.workspace.client_core.ExecutionProblem;
import org.globus.workspace.client_core.StubConfigurator;
import org.globus.workspace.client_core.utils.WSUtils;
import org.globus.workspace.client_core.utils.RMIUtils;
import org.nimbustools.messaging.gt4_0.generated.WorkspacePortType;
import org.oasis.wsn.Subscribe;
import org.oasis.wsn.ResourceUnknownFaultType;
import org.oasis.wsn.TopicPathDialectUnknownFaultType;
import org.oasis.wsn.SubscribeCreationFailedFaultType;
import org.oasis.wsn.TopicNotSupportedFaultType;
import org.oasis.wsn.InvalidTopicExpressionFaultType;
import org.oasis.wsn.TopicExpressionType;
import org.apache.axis.message.addressing.EndpointReferenceType;

import javax.xml.rpc.Stub;
import java.rmi.RemoteException;

public abstract class Subscribe_Common extends Action {


    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected final EndpointReferenceType endpoint;

    protected final StubConfigurator stubConf;

    protected final EndpointReferenceType consumerEPR;

    protected Subscribe subscribeMessage;

    protected WorkspacePortType servicePort;


    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    // TODO: handle groups

    public Subscribe_Common(EndpointReferenceType workspaceEndpoint,
                            StubConfigurator stubConfigurator,
                            EndpointReferenceType consumerEndpoint) {

        if (workspaceEndpoint == null) {
            throw new IllegalArgumentException(
                    "workspaceEndpoint epr may not be null");
        }
        this.endpoint = workspaceEndpoint;

        if (stubConfigurator == null) {
            throw new IllegalArgumentException(
                    "stubConfigurator may not be null");
        }
        this.stubConf = stubConfigurator;

        if (consumerEndpoint == null) {
            throw new IllegalArgumentException(
                    "consumerEndpoint may not be null");
        }
        this.consumerEPR = consumerEndpoint;
    }

    public Subscribe_Common(WorkspacePortType instancePortType,
                            EndpointReferenceType consumerEndpoint) {

        if (instancePortType == null) {
            throw new IllegalArgumentException(
                    "instancePortType epr may not be null");
        }
        this.servicePort = instancePortType;

        if (consumerEndpoint == null) {
            throw new IllegalArgumentException(
                    "consumerEndpoint may not be null");
        }
        this.consumerEPR = consumerEndpoint;

        this.endpoint = null;
        this.stubConf = null;
    }

    // -------------------------------------------------------------------------
    // VALIDATE
    // -------------------------------------------------------------------------

    public void validateAll() throws ParameterProblem {

        if (this.subscribeMessage == null) {
            this.subscribeMessage = this.getSubscribeMessage();
        }
        
        if (this.servicePort != null) {
            return; // *** EARLY RETURN ***
        }

        try {
            final WorkspacePortType port =
                    WSUtils.initServicePortType(this.endpoint);
            this.stubConf.setOptions((Stub)port);
            this.servicePort = port;
        } catch (Throwable t) {
            final String err = "Problem setting up: " + t.getMessage();
            throw new ParameterProblem(err, t);
        }
    }

    protected Subscribe getSubscribeMessage() {
        final Subscribe stateSubscribeMsg = new Subscribe();
        stateSubscribeMsg.setConsumerReference(this.consumerEPR);
        stateSubscribeMsg.setTopicExpression(this.getTopicExpressionType());
        return stateSubscribeMsg;
    }

    protected abstract TopicExpressionType getTopicExpressionType();


    // -------------------------------------------------------------------------
    // EXECUTE
    // -------------------------------------------------------------------------

    /**
     * CALLS subscribe()
     *
     * @return null
     * @throws Exception
     */
    public Object call() throws Exception {
        this.subscribe();
        return null;
    }

    /**
     * Subscribe
     *
     * @throws ParameterProblem validation problem
     * @throws ExecutionProblem general problem running
     * @throws ResourceUnknownFaultType race or bad epr
     * @throws SubscribeCreationFailedFaultType problem
     * @throws TopicPathDialectUnknownFaultType severe
     * @throws TopicNotSupportedFaultType severe
     * @throws InvalidTopicExpressionFaultType severe
     */
    public void subscribe() throws ParameterProblem,
                                   ExecutionProblem,
                                   ResourceUnknownFaultType,
                                   SubscribeCreationFailedFaultType,
                                   TopicPathDialectUnknownFaultType,
                                   TopicNotSupportedFaultType,
                                   InvalidTopicExpressionFaultType {

        this.validateAll();

        try {
            this.servicePort.subscribe(this.subscribeMessage);
        } catch (TopicNotSupportedFaultType e) {
            throw e;
        } catch (InvalidTopicExpressionFaultType e) {
            throw e;
        } catch (SubscribeCreationFailedFaultType e) {
            throw e;
        } catch (ResourceUnknownFaultType e) {
            throw e;
        } catch (TopicPathDialectUnknownFaultType e) {
            throw e;
        } catch (RemoteException e) {
            throw RMIUtils.generalRemoteException(e);
        }
    }
}
