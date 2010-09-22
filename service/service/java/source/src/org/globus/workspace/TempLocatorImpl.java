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

package org.globus.workspace;

import org.globus.workspace.service.binding.GlobalPolicies;
import org.globus.workspace.service.impls.site.PropagationAdapter;
import org.globus.workspace.service.impls.async.ResourceMessage;
import org.globus.workspace.persistence.PersistenceAdapter;

/**
 * some things would be too time consuming to get into IoC right now,
 * holding off for future organization
 *
 * @see TempLocator
 */
public class TempLocatorImpl implements TempLocator {

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected final PathConfigs pathConfigs;
    protected final PropagationAdapter propagation;
    protected final GlobalPolicies globals;
    protected final PersistenceAdapter persistence;
    protected final ResourceMessage resourceMessage;


    public TempLocatorImpl(PathConfigs pathConfigsImpl,
                           PropagationAdapter propagationImpl,
                           GlobalPolicies globalsImpl,
                           PersistenceAdapter persistenceImpl,
                           ResourceMessage resourceMessageImpl) {

        if (pathConfigsImpl == null) {
            throw new IllegalArgumentException("pathConfigsImpl may not be null");
        }
        this.pathConfigs = pathConfigsImpl;

        if (propagationImpl == null) {
            throw new IllegalArgumentException("propagationImpl may not be null");
        }
        this.propagation = propagationImpl;

        if (globalsImpl == null) {
            throw new IllegalArgumentException("globalsImpl may not be null");
        }
        this.globals = globalsImpl;

        if (persistenceImpl == null) {
            throw new IllegalArgumentException("persistenceImpl may not be null");
        }
        this.persistence = persistenceImpl;

        if (resourceMessageImpl == null) {
            throw new IllegalArgumentException("resourceMessageImpl may not be null");
        }
        this.resourceMessage = resourceMessageImpl;
    }

    public GlobalPolicies getGlobalPolicies() {
        return this.globals;
    }

    public PathConfigs getPathConfigs() {
        return this.pathConfigs;
    }

    public PersistenceAdapter getPersistenceAdapter() {
        return this.persistence;
    }

    public PropagationAdapter getPropagationAdapter() {
        return this.propagation;
    }

    public ResourceMessage getResourceMessage() {
        return this.resourceMessage;
    }
}
