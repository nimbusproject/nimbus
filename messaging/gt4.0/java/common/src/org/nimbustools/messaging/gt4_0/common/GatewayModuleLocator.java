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

import org.nimbustools.api.brain.ModuleLocator;
import org.nimbustools.api.services.rm.Manager;
import org.nimbustools.api.services.metadata.MetadataServer;
import org.nimbustools.api.services.security.KeyManager;
import org.nimbustools.api.repr.ReprFactory;

public class GatewayModuleLocator implements ModuleLocator {

    public Manager getManager() {
        return this.getRealML().getManager();
    }

    public ReprFactory getReprFactory() {
        return this.getRealML().getReprFactory();
    }

    public MetadataServer getMetadataServer() {
        return this.getRealML().getMetadataServer();
    }

    public KeyManager getKeyManager() {
        return this.getRealML().getKeyManager();
    }

    protected ModuleLocator getRealML() {
        final GatewayMasterContext context;
        try {
            context = GatewayMasterContext.discoverApplicationContext();
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        return context.getModuleLocator();
    }
}
