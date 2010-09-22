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

import org.nimbustools.api._repr._Caller;
import org.nimbustools.api._repr._CreateRequest;
import org.nimbustools.api._repr.vm._NIC;
import org.nimbustools.api._repr.vm._RequiredVMM;
import org.nimbustools.api._repr.vm._ResourceAllocation;
import org.nimbustools.api._repr.vm._Schedule;
import org.nimbustools.api._repr.vm._VMFile;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.CreateRequest;
import org.nimbustools.api.repr.ReprFactory;
import org.nimbustools.api.repr.vm.NIC;
import org.nimbustools.api.repr.vm.ResourceAllocation;
import org.nimbustools.api.repr.vm.VMFile;

import java.net.URI;

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
        final _CreateRequest req = this.repr._newCreateRequest();
        req.setName(name);

        final _NIC nic = this.repr._newNIC();
        nic.setNetworkName("public");
        nic.setAcquisitionMethod(NIC.ACQUISITION_AllocateAndConfigure);
        req.setRequestedNics(new _NIC[]{nic});

        final _ResourceAllocation ra = this.repr._newResourceAllocation();
        req.setRequestedRA(ra);
        final _Schedule schedule = this.repr._newSchedule();
        schedule.setDurationSeconds(240);
        req.setRequestedSchedule(schedule);
        ra.setNodeNumber(1);
        ra.setMemory(64);
        req.setShutdownType(CreateRequest.SHUTDOWN_TYPE_TRASH);
        req.setInitialStateRequest(CreateRequest.INITIAL_STATE_RUNNING);

        ra.setArchitecture(ResourceAllocation.ARCH_x86);
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

        return req;
    }

    public Caller getCaller() {
        final _Caller caller = this.repr._newCaller();
        caller.setIdentity("TEST_RUNNER");
        return caller;
    }

    public Caller getSuperuserCaller() {
        // workspace-service is currently broken with superuser
        return this.repr._newCaller();
    }
}
