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

package org.nimbustools.messaging.gt4_0_elastic.rpc;

import org.globus.wsrf.impl.security.authentication.securemsg.X509SignHandler;

import javax.xml.rpc.handler.MessageContext;

/**
 * None of the clients out there can handle secmsg responses.
 */
public class SignHandler extends X509SignHandler {

    // -------------------------------------------------------------------------
    // overrides X509SignHandler#handleResponse(MessageContext)
    // -------------------------------------------------------------------------

    public boolean handleResponse(MessageContext context) {
        if (WSSecurityHandler.isElasticPortType(context)) {
            return true;
        } else {
            return super.handleResponse(context);
        }
    }
}
