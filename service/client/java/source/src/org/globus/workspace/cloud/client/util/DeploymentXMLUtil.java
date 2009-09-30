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

package org.globus.workspace.cloud.client.util;

import org.nimbustools.messaging.gt4_0.common.Constants_GT4_0;
import org.globus.workspace.client_core.utils.WSUtils;
import org.nimbustools.messaging.gt4_0.generated.negotiable.WorkspaceDeployment_Type;
import org.nimbustools.messaging.gt4_0.generated.negotiable.ShutdownMechanism_Type;
import org.nimbustools.messaging.gt4_0.generated.negotiable.InitialState_Type;
import org.apache.axis.types.URI;

import javax.xml.namespace.QName;

public class DeploymentXMLUtil {

    public static final QName deploymentQName =
                new QName(Constants_GT4_0.NS_NEGOTIABLE,
                          "WorkspaceDeployment");

    public static WorkspaceDeployment_Type constructDeployment(
            int durationMinutes,
            int memoryMegabytes,
            String newPropagationTargetURL) {

        return constructDeployment(durationMinutes,
                                   memoryMegabytes,
                                   newPropagationTargetURL,
                                   1);
    }

    public static WorkspaceDeployment_Type constructDeployment(
            int durationMinutes,
            int memoryMegabytes,
            String newPropagationTargetURL,
            int numInstances) {

        ShutdownMechanism_Type shutdownType = ShutdownMechanism_Type.Trash;
        URI newPropagationTargetURI = null;
        
        if (newPropagationTargetURL != null) {
            try {
                newPropagationTargetURI = new URI(newPropagationTargetURL);
            } catch (URI.MalformedURIException e) {
                throw new IllegalArgumentException(e.getMessage());
            }
            shutdownType = null;
        }

        return WSUtils.constructDeploymentType(durationMinutes,
                                               memoryMegabytes,
                                               numInstances,
                                               InitialState_Type.Running,
                                               shutdownType,
                                               newPropagationTargetURI);
    }
}
