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

package org.globus.workspace.creation;

import org.globus.workspace.async.AsyncRequest;
import org.globus.workspace.service.InstanceResource;

import org.nimbustools.api.repr.Advertised;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.CreateRequest;
import org.nimbustools.api.repr.AsyncCreateRequest;
import org.nimbustools.api.services.rm.AuthorizationException;
import org.nimbustools.api.services.rm.CoSchedulingException;
import org.nimbustools.api.services.rm.CreationException;
import org.nimbustools.api.services.rm.MetadataException;
import org.nimbustools.api.services.rm.ResourceRequestDeniedException;
import org.nimbustools.api.services.rm.SchedulingException;

public interface CreationManager {

    public InstanceResource[] create(CreateRequest req, Caller caller)
           throws CoSchedulingException,
                  CreationException,
                  MetadataException,
                  ResourceRequestDeniedException,
                  SchedulingException,
                  AuthorizationException;
    
    /**
     * An asynchronous create request is not satisfied at the same time
     * it is submitted, but when the Asynchronous Request Manager
     * decides to fulfill that request based on policies.
     * 
     * Currently, asynchronous requests can be Spot Instance
     * requests or backfill requests.
     * 
     * @param req the asynchronous create request
     * @param caller the owner of the request
     * @return the added asynchronous request
     * @throws CreationException
     * @throws MetadataException
     * @throws ResourceRequestDeniedException
     * @throws SchedulingException
     */
    public AsyncRequest addAsyncRequest(AsyncCreateRequest req, Caller caller)
                  throws CreationException,
                  MetadataException,
                  ResourceRequestDeniedException,
                  SchedulingException;

    public Advertised getAdvertised();
}
