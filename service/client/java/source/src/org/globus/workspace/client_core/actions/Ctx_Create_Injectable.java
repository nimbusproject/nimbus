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
import org.globus.workspace.client_core.StubConfigurator;
import org.globus.workspace.common.print.Print;
import org.nimbustools.ctxbroker.generated.gt4_0.broker.NimbusContextBrokerPortType;
import org.nimbustools.ctxbroker.generated.gt4_0.types.CreateContext_Type;

public class Ctx_Create_Injectable extends Ctx_Create {

    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    public Ctx_Create_Injectable(EndpointReferenceType epr,
                                 StubConfigurator stubConf,
                                 Print debug) {
        super(epr, stubConf, debug);
    }

    public Ctx_Create_Injectable(NimbusContextBrokerPortType ctxBrokerPortType,
                                 Print debug) {
        super(ctxBrokerPortType, debug);
    }

    // -------------------------------------------------------------------------
    // extends
    // -------------------------------------------------------------------------

    protected CreateContext_Type getCreateArguments() {
        final CreateContext_Type t_create = new CreateContext_Type();
        t_create.setExpectInjections(true);
        return t_create;
    }
}
