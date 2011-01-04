/*
 * Copyright 1999-2010 University of Chicago
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
package org.globus.workspace.async;

import org.globus.workspace.StateChangeInterested;
import org.globus.workspace.scheduler.defaults.PreemptableSpaceManager;
import org.nimbustools.api.services.rm.ResourceRequestDeniedException;

/**
 * Interface that represents
 * an Asynchronous Request module
 */
public interface AsyncRequestManager extends AsyncRequestHome, PreemptableSpaceManager, StateChangeInterested {

    /**
     * Adds an asynchronous request to this manager
     * @param request the request to be added
     * @throws ResourceRequestDeniedException If this type of request is disabled
     */
    public void addRequest(AsyncRequest request) throws ResourceRequestDeniedException;
    
}
