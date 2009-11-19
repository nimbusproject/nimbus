package org.nimbustools.gateway.ec2.manager;

import com.xerox.amazonws.ec2.LaunchConfiguration;
import com.xerox.amazonws.ec2.ReservationDescription;
import org.nimbustools.api.repr.CreateRequest;
import org.nimbustools.api.repr.CannotTranslateException;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.vm.VM;
import org.nimbustools.api.repr.vm.NIC;
import org.nimbustools.api.repr.vm.State;
import org.nimbustools.api._repr.vm._VM;

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

public interface Translator {

    LaunchConfiguration translateCreateRequest(CreateRequest request, Caller caller)
            throws CannotTranslateException;

    _VM translateVM(ReservationDescription.Instance instance,
                          Caller caller,
                          int launchIndex) throws CannotTranslateException;

    NIC[] createNics(ReservationDescription.Instance instance);

    State translateStateCode(int stateCode)
                              throws CannotTranslateException;

    VM[] translateReservationInstances(ReservationDescription desc, Caller caller)
            throws CannotTranslateException;

    void updateVM(_VM vm, ReservationDescription.Instance instance)
            throws CannotTranslateException;
}
