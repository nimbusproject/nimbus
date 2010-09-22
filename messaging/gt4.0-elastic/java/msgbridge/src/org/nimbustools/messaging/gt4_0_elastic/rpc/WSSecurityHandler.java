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

import org.globus.gsi.jaas.GlobusPrincipal;
import org.globus.wsrf.impl.security.authentication.Constants;
import org.globus.wsrf.impl.security.authentication.wssec.WSSecurityBasicHandler;
import org.nimbustools.messaging.gt4_0_elastic.Constants_GT4_0_Elastic;

import javax.security.auth.Subject;
import javax.xml.rpc.handler.MessageContext;
import javax.xml.rpc.handler.soap.SOAPMessageContext;
import java.util.Iterator;
import java.util.Set;

public class WSSecurityHandler extends WSSecurityBasicHandler {

    // -------------------------------------------------------------------------
    // overrides WSSecurityBasicHandler
    // -------------------------------------------------------------------------

    public boolean handleRequest(MessageContext context) {

        // using our own RequestEngine instead of WSSecurityRequestEngine
        // in order to override timestamp problems
        final boolean handlerResult =
                this.handleMessage((SOAPMessageContext) context,
                                   RequestEngine.getEngine());

        if (isElasticPortType(context)) {
            this.demoteAnonymous(context);
        }

        return handlerResult;
    }


    // -------------------------------------------------------------------------
    // ASCERTAIN OPERATION
    // -------------------------------------------------------------------------

    // other handlers case for services, e.g. AuthenticationServiceHandler
    public static boolean isElasticPortType(MessageContext context) {

        if (context == null) {
            return false;
        }

        final org.apache.axis.MessageContext ctx;
        if (context instanceof org.apache.axis.MessageContext) {
            ctx = (org.apache.axis.MessageContext) context;
        } else {
            return false;
        }
        
        final String target = ctx.getTargetService();

        if (target == null) {
            return false;
        }

        if (target.endsWith(Constants_GT4_0_Elastic.SERVICE_PATH)) {
            return true;
        }

        if (target.endsWith(Constants_GT4_0_Elastic.GATEWAY_SERVICE_PATH)) {
            return true;
        }

        return false;
    }


    // -------------------------------------------------------------------------
    // DEMOTE ANONYMOUS
    // -------------------------------------------------------------------------

    /**
     * Makes sure JAAS Subject is OK for usage -- we want to authorize against
     * non-anonymous credentials (such as those coming from secure message).
     * Most clients using the Elastic PortType are using anonymous https,
     * so their identity needs to come from the secmsg envelope.
     *
     * @param context msgctx, may not be null
     */
    public void demoteAnonymous(MessageContext context) {

        if (context == null) {
            throw new IllegalArgumentException("context may not be null");
        }

        final Subject subject =
                (Subject) context.getProperty(Constants.PEER_SUBJECT);

        if (subject == null) {
            return; // *** EARLY RETURN ***
        }

        final Set principals = subject.getPrincipals();
        if (principals == null) {
            return; // *** EARLY RETURN ***
        }

        if (principals.size() == 1) {
            // nothing we can do about this
            return; // *** EARLY RETURN ***
        }

        final Iterator iter = principals.iterator();
        while (iter.hasNext()) {
            final Object obj = iter.next();
            // instanceof rejects nulls
            if (obj instanceof GlobusPrincipal) {
                final GlobusPrincipal gp = (GlobusPrincipal)obj;
                final String name = gp.getName();
                // ugh
                if (name != null && name.equals("<anonymous>")) {
                    principals.remove(gp);
                    break;
                }
            }
        }
    }
}
