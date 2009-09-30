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

package org.nimbustools.messaging.gt4_0.factory;


import org.globus.wsrf.Resource;
import org.globus.wsrf.ResourceException;
import org.globus.wsrf.impl.SingletonResourceHome;
import org.globus.wsrf.jndi.Initializable;
import org.nimbustools.api.repr.ReprFactory;
import org.nimbustools.api.services.rm.Manager;
import org.nimbustools.api.services.metadata.MetadataServer;
import org.nimbustools.messaging.gt4_0.OtherContext;
import org.nimbustools.messaging.gt4_0.common.NimbusMasterContext;
import org.nimbustools.messaging.gt4_0.ctx.ContextBrokerHome;
import org.nimbustools.messaging.gt4_0.service.InstanceTranslate;

import java.io.IOException;
import java.net.URL;

public class FactoryHome extends SingletonResourceHome implements Initializable {

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private NimbusMasterContext master;


    // -------------------------------------------------------------------------
    // implements Initializable
    // -------------------------------------------------------------------------

    public void initialize() throws Exception {

        this.master = NimbusMasterContext.discoverApplicationContext();

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
                    "FactoryHome not initialized properly, no master context");
        }

        // some room for internal IoC in the future

        final Manager mgr = this.master.getModuleLocator().getManager();

        final ReprFactory repr =
                this.master.getModuleLocator().getReprFactory();

        final MetadataServer mdServer =
                this.master.getModuleLocator().getMetadataServer();

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

        final Translate translate =
                            new Translate(repr,
                                          new TranslateDefinitionImpl(repr),
                                          new TranslateRAImpl(repr),
                                          new TranslateNetImpl(repr),
                                          trInstance,
                                          baseURL);

        final ContextBrokerHome brokerHome;
        try {
            brokerHome = OtherContext.discoverContextBrokerHome();
        } catch (Exception e) {
            throw new ResourceException(e.getMessage(), e);
        }

        final FactoryResource resource =
                new FactoryResource(mgr, translate, brokerHome, repr, mdServer);

        resource.setRPs(mgr.getAdvertised());

        return resource;
    }
}
