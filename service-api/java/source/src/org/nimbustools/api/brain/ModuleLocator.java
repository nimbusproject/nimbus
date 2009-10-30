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

package org.nimbustools.api.brain;

import org.nimbustools.api.repr.ReprFactory;
import org.nimbustools.api.services.metadata.MetadataServer;
import org.nimbustools.api.services.rm.Manager;
import org.nimbustools.api.services.security.KeyManager;

/**
 * For use from above to find key modules, where "above" means the remote
 * messaging system (or some test/console framework).  Those cannot be served
 * by our dependency injection (they can be IoC of course, but it would be a
 * different application context unless you took time to integrate directly).
 *
 * @see BreathOfLife
 */
public interface ModuleLocator {
    public Manager getManager();
    public ReprFactory getReprFactory();
    public MetadataServer getMetadataServer();
    public KeyManager getKeyManager();
    //public ContextBroker getContextBroker();
}
