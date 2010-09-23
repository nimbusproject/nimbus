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

package org.globus.workspace.client_core;

import org.apache.axis.message.addressing.EndpointReferenceType;
import org.globus.workspace.common.print.Print;
import org.globus.workspace.client_core.utils.WSUtils;
import org.nimbustools.messaging.gt4_0.common.CommonUtil;
import org.nimbustools.ctxbroker.generated.gt4_0.broker.NimbusContextBrokerPortType;

import javax.xml.rpc.Stub;

public abstract class WSAction_Ctx extends WSAction {

    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    /**
     * New WorkspaceContextBrokerPortType instance will be created from given
     * parameters during the validation phase.
     *
     * @param epr       endpoint to create portType with, may not be null
     * @param stubConf  to configure portType with, may not be null
     * @param debug     for debug messages; if null, a new instance of
     *                  disabled print impl will be created
     */
    public WSAction_Ctx(EndpointReferenceType epr,
                        StubConfigurator stubConf,
                        Print debug) {
        super(epr, stubConf, debug);
    }

    /**
     * Re-use pre-created NimbusContextBrokerPortType instance.
     *
     * @param brokerPortType to use for action, may not be null
     * @param debug    for debug messages; if null, a new instance of disabled
     *                 print impl will be created
     */
    public WSAction_Ctx(NimbusContextBrokerPortType brokerPortType,
                        Print debug) {
        super(brokerPortType, debug);
    }

    // -------------------------------------------------------------------------
    // VALIDATE
    // -------------------------------------------------------------------------

    public void validateAll() throws ParameterProblem {

        if (this.portType != null) {

            // future programmer error, should not be possible
            if (!(this.portType instanceof NimbusContextBrokerPortType)) {
                throw new ParameterProblem(
                    "portType is not NimbusContextBrokerPortType");
            }

            return; // *** EARLY RETURN ***
        }

        try {
            final NimbusContextBrokerPortType port =
                    WSUtils.initContextBrokerPortType(this.epr);
            this.stubConf.setOptions((Stub)port);
            this.portType = port;
        } catch (Throwable t) {
            final String err = "Problem setting up: " +
                    CommonUtil.genericExceptionMessageWrapper(t);
            throw new ParameterProblem(err, t);
        }
    }

}
