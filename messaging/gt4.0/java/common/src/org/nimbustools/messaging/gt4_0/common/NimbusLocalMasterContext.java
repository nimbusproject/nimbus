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

package org.nimbustools.messaging.gt4_0.common;

import org.globus.wsrf.Constants;

import javax.naming.InitialContext;

public class NimbusLocalMasterContext extends NimbusMasterContext {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    public static final String MASTER_JNDI_BASE =
            Constants.JNDI_SERVICES_BASE_NAME + "NimbusMasterContext/";

    public static final String THIS_JNDI_LOOKUP =
            MASTER_JNDI_BASE + "masterContext";

    private static final String nimbusJndiAdvice =
            "** The system is bootstrapped from a configuration called " +
            "'masterConf' near the top of a file usually located at " +
            "'$GLOBUS_LOCATION/etc/nimbus/jndi-config.xml'";

    private static final String nimbusConfAdvice =
            "** The 'masterConf' parameter is usually set to a file like " +
            "'$GLOBUS_LOCATION/etc/nimbus/workspace-service/other/main.xml'." +
            "  A configuration is present for this but it is not usable.";


    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    public NimbusLocalMasterContext() {
        super(nimbusJndiAdvice, nimbusConfAdvice);
    }


    // -------------------------------------------------------------------------
    // APPLICATION CONTEXT DISCOVERY
    // -------------------------------------------------------------------------

    /**
     * @return Application context, never null
     * @throws Exception could not locate
     */
    public static NimbusLocalMasterContext discoverApplicationContext() throws Exception {

        InitialContext ctx = null;
        try {
            ctx = new InitialContext();

            final NimbusLocalMasterContext masterContext =
                    (NimbusLocalMasterContext) ctx.lookup(THIS_JNDI_LOOKUP);

            if (masterContext == null) {
                // should be NameNotFoundException if missing
                throw new Exception("null from JNDI for MasterContext (?)");
            }

            return masterContext;

        } finally {
            if (ctx != null) {
                ctx.close();
            }
        }
    }
}
