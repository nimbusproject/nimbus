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

package org.nimbustools.api.repr;

import org.nimbustools.api.NimbusModule;
import org.nimbustools.api._repr._Advertised;
import org.nimbustools.api._repr._AsyncCreateRequest;
import org.nimbustools.api._repr._Caller;
import org.nimbustools.api._repr._CreateRequest;
import org.nimbustools.api._repr._CreateResult;
import org.nimbustools.api._repr._CustomizationRequest;
import org.nimbustools.api._repr._RequestInfo;
import org.nimbustools.api._repr._ShutdownTasks;
import org.nimbustools.api._repr._SpotCreateRequest;
import org.nimbustools.api._repr._SpotPriceEntry;
import org.nimbustools.api._repr._SpotRequestInfo;
import org.nimbustools.api._repr._Usage;
import org.nimbustools.api._repr.ctx._Context;
import org.nimbustools.api._repr.vm._Kernel;
import org.nimbustools.api._repr.vm._NIC;
import org.nimbustools.api._repr.vm._RequiredVMM;
import org.nimbustools.api._repr.vm._ResourceAllocation;
import org.nimbustools.api._repr.vm._Schedule;
import org.nimbustools.api._repr.vm._State;
import org.nimbustools.api._repr.vm._VM;
import org.nimbustools.api._repr.vm._VMFile;

public interface ReprFactory extends NimbusModule {

    // -------------------------------------------------------------------------
    // WRITABLE
    // -------------------------------------------------------------------------

    // repr package
    public _Advertised _newAdvertised();
    public _Caller _newCaller();
    public _CreateRequest _newCreateRequest();
    public _CreateResult _newCreateResult();
    public _CustomizationRequest _newCustomizationRequest();
    public _ShutdownTasks _newShutdownTasks();
    public _Usage _newUsage();
    public _RequestInfo _newRequestInfo();
    public _SpotRequestInfo _newSpotRequestInfo();
    public _SpotPriceEntry _newSpotPriceEntry();
    public _AsyncCreateRequest _newBackfillRequest();
    public _SpotCreateRequest _newSpotCreateRequest();

    // vm package
    public _Kernel _newKernel();
    public _NIC _newNIC();
    public _RequiredVMM _newRequiredVMM();
    public _ResourceAllocation _newResourceAllocation();
    public _Schedule _newSchedule();
    public _State _newState();
    public _VM _newVM();
    public _VMFile _newVMFile();

    // ctx package
    public _Context _newContext();
    
    
}
