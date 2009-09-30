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

package org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.service;

import org.nimbustools.messaging.gt4_0_elastic.context.GatewayContext;

public class GatewayService extends DelegatingService {

    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    public GatewayService() throws Exception {
        super();
    }


    // -------------------------------------------------------------------------
    // FIND IMPLS
    // -------------------------------------------------------------------------

    // This class is instantiated with no-arg constructor outside of any IoC
    // system etc., so we hook into one here.

    protected synchronized void findManager() throws Exception {
        if (this.rm == null) {
            final GatewayContext ctx =
                    GatewayContext.discoverGatewayContext();
            this.rm = ctx.findRM();
        }
    }

    protected synchronized void findGeneral() throws Exception {
        if (this.general == null) {
            final GatewayContext ctx =
                    GatewayContext.discoverGatewayContext();
            this.general = ctx.findGeneral();
        }
    }

    protected synchronized void findSecurity() throws Exception {
        this.security = null;
    }

    protected synchronized void findImage() throws Exception {
        if (this.image == null) {
            final GatewayContext ctx =
                    GatewayContext.discoverGatewayContext();
            this.image = ctx.findImage();
        }
    }
}
