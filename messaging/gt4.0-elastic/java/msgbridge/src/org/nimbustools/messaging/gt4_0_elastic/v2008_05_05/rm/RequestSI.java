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

package org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.rm;

import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.CannotTranslateException;
import org.nimbustools.api.repr.SpotCreateRequest;
import org.nimbustools.api.repr.SpotRequestInfo;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_06_15.RequestSpotInstancesResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_06_15.RequestSpotInstancesType;

import java.rmi.RemoteException;

public interface RequestSI {

    /**
     * Translate request spot instances 
     * into something the Manager understands.
     * 
     * @param req given SI request
     * @param caller caller object
     * @return valid create request for manager
     * @throws RemoteException unexpected error
     * @throws CannotTranslateException invalid request or configuration
     */
    public SpotCreateRequest translateReqSpotInstances(RequestSpotInstancesType req,
                                                       Caller caller)
            throws RemoteException, CannotTranslateException;

    /**
     * Translate Manager's SpotRequestInfo into something the elastic clients
     * understand.
     * 
     * @param result valid result from Manager
     * @param caller caller object
     * @return valid elastic response
     * @throws Exception problem (will require backout)
     */
    public RequestSpotInstancesResponseType translateSpotInfo(SpotRequestInfo result,
                                                          Caller caller)
            throws Exception;
}
