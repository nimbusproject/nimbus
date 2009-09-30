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

package org.nimbustools.api.defaults.repr;

import org.nimbustools.api._repr._Advertised;
import org.nimbustools.api._repr._Caller;
import org.nimbustools.api._repr._CreateRequest;
import org.nimbustools.api._repr._CreateResult;
import org.nimbustools.api._repr._CustomizationRequest;
import org.nimbustools.api._repr._ShutdownTasks;
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
import org.nimbustools.api.repr.ReprFactory;

/**
 * The implementation of this is authored on the fly by Spring IoC's cglib
 * feature.
 */
public abstract class DefaultReprFactory implements ReprFactory {

    public String report() {
        return "Default representation object factory " +
                "(class implementation is auto-generated)";
    }

    
    // -------------------------------------------------------------------------
    // WRITABLE
    // -------------------------------------------------------------------------

    // repr package
    public abstract _Advertised _newAdvertised();
    public abstract _Caller _newCaller();
    public abstract _CreateRequest _newCreateRequest();
    public abstract _CreateResult _newCreateResult();
    public abstract _CustomizationRequest _newCustomizationRequest();
    public abstract _ShutdownTasks _newShutdownTasks();
    public abstract _Usage _newUsage();

    // vm package
    public abstract _Kernel _newKernel();
    public abstract _NIC _newNIC();
    public abstract _RequiredVMM _newRequiredVMM();
    public abstract _ResourceAllocation _newResourceAllocation();
    public abstract _Schedule _newSchedule();
    public abstract _State _newState();
    public abstract _VM _newVM();
    public abstract _VMFile _newVMFile();

    // ctx package
    public abstract _Context _newContext();
    
}
