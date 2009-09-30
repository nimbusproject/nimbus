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

import edu.emory.mathcs.backport.java.util.concurrent.ExecutorService;
import org.globus.workspace.persistence.PersistenceAdapter;
import org.globus.workspace.service.binding.GlobalPolicies;
import org.globus.workspace.service.impls.async.ResourceMessage;
import org.globus.workspace.service.impls.site.PropagationAdapter;

/**
 * Will go away as we organize more and get some final things into IoC
 *
 * Some non-singleton classes are overly coupled right now and so this serves
 * as a way for those to easily locate some modules without going through the
 * non-singleton prototype-factory pattern yet.  So we're concentrating the
 * over-coupledness into a bundle.  Impls of the modules still all come into
 * the application over IoC to various places.
 *
 * Work needed in task, xentask, and state transition (task launch) regions
 * in particular.
 *
 * @see org.globus.workspace.service.impls.WorkspaceHomeImpl
 */
public interface TempLocator {

    public GlobalPolicies getGlobalPolicies();
    public PathConfigs getPathConfigs();
    public PersistenceAdapter getPersistenceAdapter();
    public PropagationAdapter getPropagationAdapter();
    public ResourceMessage getResourceMessage();
}
