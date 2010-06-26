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

import org.globus.workspace.service.InstanceResource;
import org.nimbustools.api.repr.Advertised;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.CreateRequest;
import org.nimbustools.api.repr.RequestSI;
import org.nimbustools.api.repr.SpotRequest;
import org.nimbustools.api.services.rm.CoSchedulingException;
import org.nimbustools.api.services.rm.CreationException;
import org.nimbustools.api.services.rm.MetadataException;
import org.nimbustools.api.services.rm.ResourceRequestDeniedException;
import org.nimbustools.api.services.rm.SchedulingException;

public interface Creation {

    public InstanceResource[] create(CreateRequest req, Caller caller)
           throws CoSchedulingException,
                  CreationException,
                  MetadataException,
                  ResourceRequestDeniedException,
                  SchedulingException;
    
    public SpotRequest requestSpotInstances(RequestSI req, Caller caller)
                  throws CoSchedulingException,
                  CreationException,
                  MetadataException,
                  ResourceRequestDeniedException,
                  SchedulingException;

    public Advertised getAdvertised();
}
