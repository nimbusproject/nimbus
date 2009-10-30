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

package org.nimbustools.api.defaults.brain;

import org.nimbustools.api.brain.ModuleLocator;
import org.nimbustools.api.repr.ReprFactory;
import org.nimbustools.api.services.rm.Manager;
import org.nimbustools.api.services.metadata.MetadataServer;

/**
 * The implementation of this is authored on the fly by Spring IoC's cglib
 * feature.
 */
public abstract class DefaultModuleLocator implements ModuleLocator {
    public abstract Manager getManager();
    public abstract ReprFactory getReprFactory();
    public abstract MetadataServer getMetadataServer();
    public abstract SecurityManager getSecurityManager();
    //public abstract ContextBroker getContextBroker();
}
