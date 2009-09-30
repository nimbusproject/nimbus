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

import org.globus.workspace.client_core.StubConfigurator;
import org.globus.workspace.client_core.utils.WSUtils;
import org.nimbustools.messaging.gt4_0.generated.WorkspacePortType;
import org.apache.axis.message.addressing.EndpointReferenceType;
import org.oasis.wsn.TopicExpressionType;

public class SubscribeTermination_Instance extends Subscribe_Common {

    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    public SubscribeTermination_Instance(
                                EndpointReferenceType workspaceEndpoint,
                                StubConfigurator stubConfigurator,
                                EndpointReferenceType consumerEndpoint) {
        super(workspaceEndpoint, stubConfigurator, consumerEndpoint);
    }

    public SubscribeTermination_Instance(WorkspacePortType instancePortType,
                                         EndpointReferenceType consumerEndpoint) {
        super(instancePortType, consumerEndpoint);
    }


    // -------------------------------------------------------------------------
    // overrides Subscribe_Common
    // -------------------------------------------------------------------------

    protected TopicExpressionType getTopicExpressionType() {
        return WSUtils.getTerminationTopic();
    }
}
