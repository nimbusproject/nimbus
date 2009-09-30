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
import org.globus.workspace.client_core.actions.Ctx_Create_Injectable;
import org.globus.workspace.common.print.Print;
import org.globus.workspace.client.AllArguments;
import org.apache.axis.message.addressing.EndpointReferenceType;

/**
 * Clone of ContextCreate but it allows the context to receive injections.
 *
 * If a context is set to be able to receive injections, then nothing will
 * happen until you call the noMoreInjections remote operation.
 */
public class ContextCreate_Injectable extends ContextCreate {

    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    public ContextCreate_Injectable(Print print,
                                    AllArguments arguments,
                                    StubConfigurator stubConfigurator) {
        super(print, arguments, stubConfigurator);
    }

    
    // -------------------------------------------------------------------------
    // extends ContextCreate
    // -------------------------------------------------------------------------

    protected void setupAction(EndpointReferenceType epr) {
        this.ctx_create = new Ctx_Create_Injectable(epr, this.stubConf, this.pr);
    }
}
