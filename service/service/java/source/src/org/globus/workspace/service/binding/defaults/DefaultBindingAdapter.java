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

package org.globus.workspace.service.binding.defaults;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.workspace.WorkspaceConstants;
import org.globus.workspace.WorkspaceException;
import org.globus.workspace.service.binding.BindCredential;
import org.globus.workspace.service.binding.BindCustomizations;
import org.globus.workspace.service.binding.BindDisks;
import org.globus.workspace.service.binding.BindInitialState;
import org.globus.workspace.service.binding.BindKernel;
import org.globus.workspace.service.binding.BindNetwork;
import org.globus.workspace.service.binding.BindResourceRequest;
import org.globus.workspace.service.binding.BindSchedule;
import org.globus.workspace.service.binding.BindShutdownMechanism;
import org.globus.workspace.service.binding.BindVMM;
import org.globus.workspace.service.binding.BindResourcePool;
import org.globus.workspace.service.binding.BindingAdapter;
import org.globus.workspace.service.binding.vm.FileCopyNeed;
import org.globus.workspace.service.binding.vm.VirtualMachine;
import org.globus.workspace.service.binding.vm.VirtualMachineDeployment;
import org.nimbustools.api.repr.CreateRequest;
import org.nimbustools.api.repr.vm.ResourceAllocation;
import org.nimbustools.api.services.rm.CreationException;
import org.nimbustools.api.services.rm.ResourceRequestDeniedException;

public class DefaultBindingAdapter implements BindingAdapter,
                                              WorkspaceConstants {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(DefaultBindingAdapter.class.getName());
    

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected final BindSchedule bindSchedule;
    protected final BindInitialState bindInitialState;
    protected final BindShutdownMechanism bindShutdownMechanism;
    protected final BindCustomizations bindCustomizations;
    protected final BindCredential bindCredential;
    protected final BindKernel bindKernel;
    protected final BindResourceRequest bindResourceRequest;
    protected final BindDisks bindDisks;
    protected final BindVMM bindVMM;
    protected final BindNetwork bindNetwork;
    protected final BindResourcePool bindResourcePool;

    
    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    public DefaultBindingAdapter(BindSchedule bindScheduleImpl,
                                 BindInitialState bindInitialStateImpl,
                                 BindShutdownMechanism bindShutdownImpl,
                                 BindCustomizations bindCustomizationsImpl,
                                 BindCredential bindCredentialImpl,
                                 BindKernel bindKernelImpl,
                                 BindDisks bindDisksImpl,
                                 BindResourceRequest bindResourceRequestImpl,
                                 BindVMM bindVMMImpl,
                                 BindNetwork bindNetworkImpl,
                                 BindResourcePool bindResourcePoolImpl) {

        if (bindScheduleImpl == null) {
            throw new IllegalArgumentException("bindScheduleImpl may not be null");
        }
        this.bindSchedule = bindScheduleImpl;

        if (bindInitialStateImpl == null) {
            throw new IllegalArgumentException("bindInitialStateImpl may not be null");
        }
        this.bindInitialState = bindInitialStateImpl;

        if (bindShutdownImpl == null) {
            throw new IllegalArgumentException("bindShutdownImpl may not be null");
        }
        this.bindShutdownMechanism = bindShutdownImpl;

        if (bindCustomizationsImpl == null) {
            throw new IllegalArgumentException("bindCustomizationsImpl may not be null");
        }
        this.bindCustomizations = bindCustomizationsImpl;

        if (bindCredentialImpl == null) {
            throw new IllegalArgumentException("bindCredentialImpl may not be null");
        }
        this.bindCredential = bindCredentialImpl;

        if (bindKernelImpl == null) {
            throw new IllegalArgumentException("bindKernelImpl may not be null");
        }
        this.bindKernel = bindKernelImpl;

        if (bindDisksImpl == null) {
            throw new IllegalArgumentException("bindDisksImpl may not be null");
        }
        this.bindDisks = bindDisksImpl;

        if (bindResourceRequestImpl == null) {
            throw new IllegalArgumentException("bindResourceRequestImpl may not be null");
        }
        this.bindResourceRequest = bindResourceRequestImpl;

        if (bindVMMImpl == null) {
            throw new IllegalArgumentException("bindVMMImpl may not be null");
        }
        this.bindVMM = bindVMMImpl;

        if (bindNetworkImpl == null) {
            throw new IllegalArgumentException("bindNetworkImpl may not be null");
        }
        this.bindNetwork = bindNetworkImpl;

        if (bindResourcePoolImpl == null) {
            throw new IllegalArgumentException("bindResourcePoolImpl may not be null");
        }
        this.bindResourcePool = bindResourcePoolImpl;
    }


    // -------------------------------------------------------------------------
    // PROCESS
    // -------------------------------------------------------------------------

    public VirtualMachine[] processRequest(CreateRequest req)
            throws ResourceRequestDeniedException,
                   CreationException {

        if (req == null) {
            throw new IllegalArgumentException("req may not be null");
        }

        final ResourceAllocation requestedRA = req.getRequestedRA();
        if (requestedRA == null) {
            throw new IllegalArgumentException("requestedRA may not be null");
        }

        final int numNodes = req.getRequestedRA().getNodeNumber();
        final VirtualMachine vm = new VirtualMachine();
        
        final String name = req.getName();
        vm.setName(name);
        
        vm.setPreemptable(req.getRequestedRA().isSpotInstance());

        if (numNodes > 1) {
            logger.debug("binding " + numNodes + " virtual machines: " + name);
        } else {
            logger.debug("binding virtual machine: " + name);
        }

        final VirtualMachineDeployment dep = new VirtualMachineDeployment();
        vm.setDeployment(dep);

        this.bindNetwork.neededAllocations(vm, req.getRequestedNics());
        this.bindResourcePool.consume(vm, req.getRequestedResourcePool());
        this.bindSchedule.consume(dep, req.getRequestedSchedule());
        this.bindInitialState.consume(vm, req.getInitialStateRequest());
        this.bindShutdownMechanism.consume(dep, req.getShutdownType());
        this.bindKernel.consume(vm, req.getRequestedKernel());
        this.bindDisks.consume(vm, req.getVMFiles());
        this.bindResourceRequest.consume(dep, req.getRequestedRA());
        this.bindVMM.consume(vm, req.getRequiredVMM());
        this.bindCustomizations.consume(vm, req.getCustomizationRequests());
        this.bindCredential.consume(vm, req.getCredential());

        // all in group get the same data
        if (req.getMdUserData() != null) {
            vm.setMdUserData(req.getMdUserData());
        }

        final VirtualMachine[] vms = new VirtualMachine[numNodes];
        for (int i = 0; i < vms.length; i++) {
            try {
                vms[i] = VirtualMachine.cloneOne(vm);
            } catch (Exception e) {
                throw new CreationException(e.getMessage(), e);
            }
        }
        
        return vms;
    }


    // -------------------------------------------------------------------------
    // BACKOUT ALLOCATIONS
    // -------------------------------------------------------------------------
    
    public void backOutAllocations(VirtualMachine vm)
            throws WorkspaceException {
        //this.bindNetwork.backOutIPAllocations(vm);
    }

    public void backOutAllocations(VirtualMachine[] vms)
            throws WorkspaceException {
        //this.bindNetwork.backOutIPAllocations(vms);
    }


    // -------------------------------------------------------------------------
    // OTHER
    // -------------------------------------------------------------------------
    
    public FileCopyNeed newFileCopyNeed(String srcContent,
                                                  String dstPath)
                throws WorkspaceException {

        return this.bindCustomizations
                        .newFileCopyNeedImpl(srcContent, dstPath);
    }

}
