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

package org.globus.workspace.testing.utils;

import java.net.URI;
import java.net.URISyntaxException;

import org.nimbustools.api._repr._Caller;
import org.nimbustools.api._repr._CreateRequest;
import org.nimbustools.api._repr._RequestSI;
import org.nimbustools.api._repr.vm._NIC;
import org.nimbustools.api._repr.vm._RequiredVMM;
import org.nimbustools.api._repr.vm._ResourceAllocation;
import org.nimbustools.api._repr.vm._Schedule;
import org.nimbustools.api._repr.vm._VMFile;
import org.nimbustools.api.defaults.repr.DefaultRequestSI;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.CreateRequest;
import org.nimbustools.api.repr.ReprFactory;
import org.nimbustools.api.repr.RequestSI;
import org.nimbustools.api.repr.si.SIConstants;
import org.nimbustools.api.repr.vm.NIC;
import org.nimbustools.api.repr.vm.ResourceAllocation;
import org.nimbustools.api.repr.vm.VMFile;

/**
 * Non-static to allow suites to easily override defaults for their own situation.
 * @see org.globus.workspace.testing.NimbusTestBase#populator()
 */
public class ReprPopulator {

    protected ReprFactory repr;

    public ReprPopulator(ReprFactory repr) {
        if (repr == null) {
            throw new IllegalArgumentException("repr is missing");
        }
        this.repr = repr;
    }

    /**
     * @param name used for logging
     * @return simple, populated CreateRequest to send into RM API
     * @throws Exception problem
     */
    public CreateRequest getCreateRequest(String name) throws Exception {
        return getCreateRequest(name, 240, 64, 1);
    }
    
    public CreateRequest getCreateRequest(String name, int durationSecs, int mem, int numNodes) throws Exception {
        final _CreateRequest req = this.repr._newCreateRequest();

        populate(req, durationSecs, name, mem, numNodes, false);

        return req;
    }    

    private void populate(final _CreateRequest req, int durationSeconds, String name, int mem, int numNodes, boolean preemptable) throws URISyntaxException {
        req.setName(name);
        
        final _NIC nic = this.repr._newNIC();
        nic.setNetworkName("public");
        nic.setAcquisitionMethod(NIC.ACQUISITION_AllocateAndConfigure);
        req.setRequestedNics(new _NIC[]{nic});

        final _ResourceAllocation ra = this.repr._newResourceAllocation();
        req.setRequestedRA(ra);
        final _Schedule schedule = this.repr._newSchedule();
        schedule.setDurationSeconds(durationSeconds);
        req.setRequestedSchedule(schedule);
        ra.setNodeNumber(numNodes);
        ra.setMemory(mem);
        req.setShutdownType(CreateRequest.SHUTDOWN_TYPE_TRASH);
        req.setInitialStateRequest(CreateRequest.INITIAL_STATE_RUNNING);

        ra.setArchitecture(ResourceAllocation.ARCH_x86);
        ra.setPreemptable(preemptable);
        final _RequiredVMM reqVMM = this.repr._newRequiredVMM();
        reqVMM.setType("Xen");
        reqVMM.setVersions(new String[]{"3"});
        req.setRequiredVMM(reqVMM);

        final _VMFile file = this.repr._newVMFile();
        file.setRootFile(true);
        file.setBlankSpaceName(null);
        file.setBlankSpaceSize(-1);
        file.setURI(new URI("file:///tmp/nothing"));
        file.setMountAs("sda1");
        file.setDiskPerms(VMFile.DISKPERMS_ReadWrite);
        req.setVMFiles(new _VMFile[]{file});
    }

    public Caller getCaller() {
        return getCaller("TEST_RUNNER");
    }
    
    public Caller getCaller(String id) {
        final _Caller caller = this.repr._newCaller();
        caller.setIdentity(id);
        return caller;
    }    

    public Caller getSuperuserCaller() {
        // workspace-service is currently broken with superuser
        return this.repr._newCaller();
    }
    
    public RequestSI getBasicRequestSI(String name, int numNodes, Double spotPrice, boolean persistent) throws Exception {
        final _RequestSI reqSI = new DefaultRequestSI();
        
        populate(reqSI, 500, name, SIConstants.SI_TYPE_BASIC_MEM, numNodes, true);
        
        reqSI.setInstanceType(SIConstants.SI_TYPE_BASIC);
        reqSI.setSpotPrice(spotPrice);
        reqSI.setPersistent(persistent);
        
        return reqSI;
    }    
}
