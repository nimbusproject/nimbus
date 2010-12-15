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
import org.nimbustools.api.services.rm.Manager;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.RequestSpotInstancesResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.RequestSpotInstancesType;

import java.rmi.RemoteException;

public interface RequestSI {

    public RequestSpotInstancesResponseType requestSpotInstances(
                                    RequestSpotInstancesType req, Caller caller, Manager manager)
            
                throws RemoteException;

}
