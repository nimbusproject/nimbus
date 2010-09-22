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

package org.nimbustools.messaging.gt4_0.status;

import org.globus.wsrf.impl.SingletonResourceHome;
import org.globus.wsrf.jndi.Initializable;
import org.globus.wsrf.Resource;
import org.globus.wsrf.ResourceException;

import org.nimbustools.messaging.gt4_0.common.NimbusLocalMasterContext;
import org.nimbustools.messaging.gt4_0.common.NimbusMasterContext;
import org.nimbustools.messaging.gt4_0.service.InstanceTranslate;
import org.nimbustools.api.services.rm.Manager;
import org.nimbustools.api.repr.ReprFactory;

import java.net.URL;
import java.io.IOException;

public class StatusHome extends SingletonResourceHome
                        implements Initializable {

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private NimbusMasterContext master;

    
    // -------------------------------------------------------------------------
    // implements Initializable
    // -------------------------------------------------------------------------

    public void initialize() throws Exception {

        this.master = NimbusLocalMasterContext.discoverApplicationContext();

        // instantiate resource and cause SingletonResourceHome to cache
        // the reference, this.findSingleton() is called
        find(null);
    }


    // -------------------------------------------------------------------------
    // extends SingletonResourceHome
    // -------------------------------------------------------------------------

    protected Resource findSingleton() throws ResourceException {

        if (this.master == null) {
            throw new ResourceException(
                    "StatusHome not initialized properly, no master context");
        }

        // some room for internal IoC in the future
        
        final Manager mgr = this.master.getModuleLocator().getManager();

        final ReprFactory repr =
                this.master.getModuleLocator().getReprFactory();

        final URL baseURL;
        try {
            baseURL = this.master.getBaseURL();
        } catch (IOException e) {
            final String err =
                    "Problem finding base container URL: " + e.getMessage();
            throw new ResourceException(err, e);
        }

        final InstanceTranslate trInstance =
                new InstanceTranslate(repr, baseURL);

        final StatusTranslate translate = new StatusTranslate(repr, trInstance);

        return new StatusResource(mgr, translate);
    }

}
