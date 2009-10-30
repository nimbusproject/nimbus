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

package org.nimbustools.messaging.gt4_0_elastic.context;

import org.nimbustools.messaging.gt4_0.common.NimbusLocalMasterContext;

import javax.naming.InitialContext;

public class ElasticContext extends BaseContext {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    public static final String THIS_JNDI_LOOKUP =
            NimbusLocalMasterContext.MASTER_JNDI_BASE + "elasticContext";


    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    public ElasticContext() {
        super("nimbus-elastic.rm",
              "nimbus-elastic.general",
              "nimbus-elastic.security",
              "nimbus-elastic.image",
              "elastic context",
              "elasticConf");
    }

    
    // -------------------------------------------------------------------------
    // SET
    // -------------------------------------------------------------------------

    public synchronized void setElasticConf(String conf) {
        this.springConf = conf;
    }


    // -------------------------------------------------------------------------
    // ELASTIC CONTEXT DISCOVERY
    // -------------------------------------------------------------------------

    /**
     * @return ElasticContext, never null
     * @throws Exception could not locate
     */
    public static ElasticContext discoverElasticContext() throws Exception {

        InitialContext ctx = null;
        try {
            ctx = new InitialContext();

            final ElasticContext masterContext =
                    (ElasticContext) ctx.lookup(THIS_JNDI_LOOKUP);

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
