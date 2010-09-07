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

package org.nimbustools.api.services.rm;

import org.nimbustools.api.repr.CreateRequest;

/**
 * <p>May be used to perform checks for nulls, negative numbers, etc.</p>
 *
 * <p>RM implementation does not need to use this if it doesn't want to but
 * it relieves developer of tending to those often tedious "that would never
 * happen, right?" sanity checks.</p>
 *
 * @see org.nimbustools.api.defaults.services.rm.DefaultBasicLegality
 */
public interface BasicLegality {

    /**
     * @param req request to manager from any messaging layer
     * @throws CreationException general problem (arrays with null values, etc)
     * @throws MetadataException missing metadata or fundamental problem with it 
     * @throws ResourceRequestDeniedException missing RA or fundamental problem with it
     */
    public void checkCreateRequest(CreateRequest req)
           throws CreationException,
                  MetadataException,
                  SchedulingException,
                  ResourceRequestDeniedException;
}
