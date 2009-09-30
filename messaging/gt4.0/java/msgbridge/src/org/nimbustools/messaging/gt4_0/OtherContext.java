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

package org.nimbustools.messaging.gt4_0;

import org.globus.wsrf.Constants;
import org.nimbustools.messaging.gt4_0.ctx.ContextBrokerHome;

import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

public class OtherContext {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    // this will go away once virtualizing the context broker is done:
    public static final String CTX_BROKER_PATH = "WorkspaceContextBroker";
    public static final String CONTEXTUALIZATION_HOME =
                        Constants.JNDI_SERVICES_BASE_NAME +
                                CTX_BROKER_PATH +
                        Constants.HOME_NAME;


    // -------------------------------------------------------------------------
    // BROKER
    // -------------------------------------------------------------------------

    /**
     * @return context broker home, if configured; null, if unconfigured
     * @throws Exception problem with JNDI and/or initialization of module
     */
    public static ContextBrokerHome discoverContextBrokerHome() throws Exception {
        
        InitialContext ctx = null;
        try {
            ctx = new InitialContext();

            final ContextBrokerHome brokerHome =
                    (ContextBrokerHome) ctx.lookup(CONTEXTUALIZATION_HOME);

            if (brokerHome == null) {
                // should be NameNotFoundException if missing
                throw new Exception("null from JNDI for ContextBrokerHome (?)");
            }

            return brokerHome;
            
        } catch (NamingException e) {
            if (e instanceof NameNotFoundException) {

                // DISABLED, likely commented out in the default jndi config 
                return null; // *** EARLY RETURN ***
                
            } else {
                // this is usually because of org.globus JNDI Initializable
                throw e;
            }
        } finally {
            if (ctx != null) {
                ctx.close();
            }
        }
    }
}
