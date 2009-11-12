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

package org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.rm;

import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.CreateRequest;
import org.nimbustools.api.repr.CannotTranslateException;
import org.nimbustools.api.repr.CreateResult;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.RunInstancesType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.ReservationInfoType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.RunInstancesResponseType;

import java.rmi.RemoteException;

public interface Run {

    /**
     * Translate launch request into something the Manager understands.
     * 
     * @param req given request
     * @param caller caller object
     * @return valid create request for manager
     * @throws RemoteException unexpected error
     * @throws CannotTranslateException invalid request or configuration
     */
    public CreateRequest translateRunInstances(RunInstancesType req,
                                               Caller caller)
            throws RemoteException, CannotTranslateException;

    /**
     * Translate Manager's CreateResult into something the elastic clients
     * understand.
     * 
     * @param result valid result from Manager
     * @param caller caller object
     * @param sshKeyName nickname of key used in request, may be null
     * @return valid elastic response
     * @throws Exception problem (will require backout)
     */
    public RunInstancesResponseType translateCreateResult(CreateResult result,
                                                     Caller caller,
                                                     String sshKeyName)
            throws Exception;
}
