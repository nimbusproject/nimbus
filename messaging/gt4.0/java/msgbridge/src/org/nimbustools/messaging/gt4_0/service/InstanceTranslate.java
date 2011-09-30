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

package org.nimbustools.messaging.gt4_0.service;

import org.apache.axis.message.addressing.EndpointReferenceType;
import org.ggf.jsdl.CPUArchitecture_Type;
import org.ggf.jsdl.Exact_Type;
import org.ggf.jsdl.ProcessorArchitectureEnumeration;
import org.ggf.jsdl.RangeValue_Type;
import org.nimbustools.api._repr._ShutdownTasks;
import org.nimbustools.api.repr.CannotTranslateException;
import org.nimbustools.api.repr.ReprFactory;
import org.nimbustools.api.repr.ShutdownTasks;
import org.nimbustools.api.repr.vm.NIC;
import org.nimbustools.api.repr.vm.ResourceAllocation;
import org.nimbustools.api.repr.vm.Schedule;
import org.nimbustools.api.repr.vm.State;
import org.nimbustools.api.repr.vm.VM;
import org.nimbustools.messaging.gt4_0.BaseTranslate;
import org.nimbustools.messaging.gt4_0.EPRGenerator;
import org.nimbustools.messaging.gt4_0.common.CommonUtil;
import org.nimbustools.messaging.gt4_0.common.Constants_GT4_0;
import org.nimbustools.messaging.gt4_0.generated.metadata.logistics.IPConfig_Type;
import org.nimbustools.messaging.gt4_0.generated.metadata.logistics.IPConfig_TypeAcquisitionMethod;
import org.nimbustools.messaging.gt4_0.generated.metadata.logistics.Logistics;
import org.nimbustools.messaging.gt4_0.generated.metadata.logistics.Nic_Type;
import org.nimbustools.messaging.gt4_0.generated.metadata.logistics.VirtualNetwork_Type;
import org.nimbustools.messaging.gt4_0.generated.negotiable.CPU_Type;
import org.nimbustools.messaging.gt4_0.generated.negotiable.PostShutdown_Type;
import org.nimbustools.messaging.gt4_0.generated.negotiable.ResourceAllocation_Type;
import org.nimbustools.messaging.gt4_0.generated.types.CurrentState;
import org.nimbustools.messaging.gt4_0.generated.types.CurrentState_Enumeration;
import org.nimbustools.messaging.gt4_0.generated.types.Schedule_Type;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Calendar;
import java.util.HashMap;

public class InstanceTranslate extends BaseTranslate {

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected final EPRGenerator eprgen;
    

    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------
    
    public InstanceTranslate(ReprFactory reprFactory, URL containerURL) {
        super(reprFactory);
        this.eprgen = new EPRGenerator(containerURL,
                                       Constants_GT4_0.SERVICE_PATH,
                                       Constants_GT4_0.RESOURCE_KEY_QNAME);
    }

    
    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    protected static final HashMap statusMap = new HashMap(16);
    static {
        statusMap.put(State.STATE_Cancelled,
                      CurrentState_Enumeration.Cancelled);
        statusMap.put(State.STATE_Corrupted,
                      CurrentState_Enumeration.Corrupted);
        statusMap.put(State.STATE_Paused,
                      CurrentState_Enumeration.Paused);
        statusMap.put(State.STATE_Propagated,
                      CurrentState_Enumeration.Propagated);
        statusMap.put(State.STATE_Running,
                      CurrentState_Enumeration.Running);
        statusMap.put(State.STATE_TransportReady,
                      CurrentState_Enumeration.TransportReady);
        statusMap.put(State.STATE_Unpropagated,
                      CurrentState_Enumeration.Unpropagated);
        // workspace wsdl does not know this one yet:
        statusMap.put(State.STATE_TowardsTransportReady,
                      CurrentState_Enumeration.Propagated);
    }

    protected static final HashMap acqMethodMap = new HashMap(8);
    static {
        acqMethodMap.put(NIC.ACQUISITION_AllocateAndConfigure,
                         IPConfig_TypeAcquisitionMethod.AllocateAndConfigure);
        acqMethodMap.put(NIC.ACQUISITION_AcceptAndConfigure,
                         IPConfig_TypeAcquisitionMethod.AcceptAndConfigure);
        acqMethodMap.put(NIC.ACQUISITION_Advisory,
                         IPConfig_TypeAcquisitionMethod.Advisory);
    }
    
    protected static final HashMap archMap = new HashMap(16);
    static {
        archMap.put(ResourceAllocation.ARCH_arm,
                    ProcessorArchitectureEnumeration.arm);
        archMap.put(ResourceAllocation.ARCH_ia64,
                    ProcessorArchitectureEnumeration.ia64);
        archMap.put(ResourceAllocation.ARCH_mips,
                    ProcessorArchitectureEnumeration.mips);
        archMap.put(ResourceAllocation.ARCH_other,
                    ProcessorArchitectureEnumeration.other);
        archMap.put(ResourceAllocation.ARCH_parisc,
                    ProcessorArchitectureEnumeration.parisc);
        archMap.put(ResourceAllocation.ARCH_powerpc,
                    ProcessorArchitectureEnumeration.powerpc);
        archMap.put(ResourceAllocation.ARCH_sparc,
                    ProcessorArchitectureEnumeration.sparc);
        archMap.put(ResourceAllocation.ARCH_x86,
                    ProcessorArchitectureEnumeration.x86);
        archMap.put(ResourceAllocation.ARCH_x86_32,
                    ProcessorArchitectureEnumeration.x86_32);
        archMap.put(ResourceAllocation.ARCH_x86_64,
                    ProcessorArchitectureEnumeration.x86_64);
    }


    // -------------------------------------------------------------------------
    // TRANSLATE TO: EndpointReferenceType
    // -------------------------------------------------------------------------

    /**
     * @param id instance ID
     * @return instance EPR
     * @throws CannotTranslateException construction problem
     */
    public EndpointReferenceType getEPR(String id)
            throws CannotTranslateException {
        return this.eprgen.getEPR(id);
    }

    
    // -------------------------------------------------------------------------
    // TRANSLATE TO: CurrentState
    // -------------------------------------------------------------------------

    public CurrentState getCurrentState(VM vm) throws CannotTranslateException {
        
        if (vm == null) {
            throw new CannotTranslateException("vm may not be null");
        }

        final State state = vm.getState();
        if (state == null) {
            throw new CannotTranslateException("state may not be null");
        }


        final CurrentState_Enumeration stateEnum =
                (CurrentState_Enumeration) statusMap.get(state.getState());

        if (stateEnum == null) {
            throw new CannotTranslateException(
                    "do not recognize VM state '" + state.getState() + "'");
        }

        final CurrentState ret = new CurrentState();
        ret.setState(stateEnum);

        final Throwable t = state.getProblem();
        if (t != null) {
            ret.setWorkspaceFault(
                    InstanceUtil.makeWorkspaceFault(t.getMessage(), t));
        }

        return ret;
    }

    
    // -------------------------------------------------------------------------
    // TRANSLATE TO: Logistics (with no contextualization portion)
    // -------------------------------------------------------------------------

    public Logistics getLogistics(VM vm) throws CannotTranslateException {

        if (vm == null) {
            throw new CannotTranslateException("vm may not be null");
        }

        final NIC[] nics = vm.getNics();
        if (nics == null || nics.length == 0) {
            return new Logistics(); // *** EARLY RETURN ***
        }

        final VirtualNetwork_Type t_network = new VirtualNetwork_Type();
        t_network.setNic(this.getWSnics(nics));

        final Logistics log = new Logistics();
        log.setNetworking(t_network);
        return log;
    }


    // -------------------------------------------------------------------------
    // TRANSLATE TO: Nic_Type[]
    // -------------------------------------------------------------------------

    public Nic_Type[] getWSnics(NIC[] nics) throws CannotTranslateException {

        if (nics == null) {
            return null;
        }

        final Nic_Type[] wsNics = new Nic_Type[nics.length];

        for (int i = 0; i < nics.length; i++) {
            final NIC nic = nics[i];

            final Nic_Type wsnic = new Nic_Type();
            wsnic.setName(nic.getName());
            wsnic.setMAC(nic.getMAC());
            wsnic.setAssociation(nic.getNetworkName());

            final IPConfig_Type ip = new IPConfig_Type();
            ip.setBroadcast(nic.getBroadcast());
            ip.setGateway(nic.getGateway());
            ip.setHostname(nic.getHostname());
            ip.setIpAddress(nic.getIpAddress());
            ip.setNetmask(nic.getNetmask());
            ip.setNetwork(nic.getNetwork());

            final IPConfig_TypeAcquisitionMethod method =
                    (IPConfig_TypeAcquisitionMethod)
                            acqMethodMap.get(nic.getAcquisitionMethod());
            if (method == null) {
                throw new CannotTranslateException(
                        "do not recognize acquisition method '" +
                                nic.getAcquisitionMethod() + "'");
            }
            ip.setAcquisitionMethod(method);

            wsnic.setIpConfig(ip);
            wsNics[i] = wsnic;
        }

        return wsNics;
    }

    
    // -------------------------------------------------------------------------
    // TRANSLATE TO: Schedule_Type
    // -------------------------------------------------------------------------

    public Schedule_Type getSchedule_Type(VM vm)
            throws CannotTranslateException {

        if (vm == null) {
            throw new CannotTranslateException("vm may not be null");
        }

        final Schedule_Type t_sched = new Schedule_Type();

        final Schedule sched = vm.getSchedule();
        if (sched == null) {
            return t_sched; // *** EARLY RETURN ***
        }

        final int seconds = sched.getDurationSeconds();
        if (seconds > -1) {
            t_sched.setDuration(CommonUtil.secondsToDuration(seconds));
        }

        final Calendar startTime = sched.getStartTime();
        if (startTime != null) {
            t_sched.setActualInstantiationTime(startTime);
        }

        final Calendar termTime = sched.getDestructionTime();
        if (termTime != null) {
            t_sched.setActualTerminationTime(termTime);
        }

        return t_sched;
    }

    public String getDetails(VM vm)
    {
        return vm.getDetails();
    }
    

    // -------------------------------------------------------------------------
    // TRANSLATE TO: ResourceAllocation_Type
    // -------------------------------------------------------------------------

    // missing: support for Storage_Type, Bandwidth_Type
    public ResourceAllocation_Type getResourceAllocation_Type(VM vm)
            throws CannotTranslateException {

        if (vm == null) {
            throw new CannotTranslateException("vm may not be null");
        }

        final ResourceAllocation_Type alloc = new ResourceAllocation_Type();

        final ResourceAllocation ra = vm.getResourceAllocation();
        if (ra == null) {
            return alloc; // *** EARLY RETURN ***
        }

        this.setCPU(ra, alloc);

        final int raMem = ra.getMemory();
        if (raMem > -1) {
            final Exact_Type ex = new Exact_Type(raMem);
            final Exact_Type[] exacts = {ex};
            alloc.setIndividualPhysicalMemory(
                    new RangeValue_Type(exacts,null,null,null));
        }

        final int raCpus = ra.getIndCpuCount();
        if (raCpus > -1) {
            final Exact_Type ex = new Exact_Type(raCpus);
            final Exact_Type[] exacts = {ex};
            alloc.setIndividualCPUCount(
                    new RangeValue_Type(exacts,null,null,null));
        }

        final int raCpuPercent = ra.getCpuPercentage();
        if (raCpuPercent > -1) {
            final Exact_Type ex = new Exact_Type(raCpuPercent);
            final Exact_Type[] exacts = {ex};
            alloc.setCPUPercentage(
                    new RangeValue_Type(exacts,null,null,null));
        }

        return alloc;
    }

    protected void setCPU(ResourceAllocation ra, ResourceAllocation_Type alloc)
            throws CannotTranslateException {

        if (ra == null) {
            throw new IllegalArgumentException("ra may not be null");
        }
        if (alloc == null) {
            throw new IllegalArgumentException("alloc may not be null");
        }
        
        final String raArch = ra.getArchitecture();

        ProcessorArchitectureEnumeration arch = null;
        if (raArch != null) {
            arch = (ProcessorArchitectureEnumeration) archMap.get(raArch);
            if (arch == null) {
                throw new CannotTranslateException("do not recognize given " +
                        "CPU architecture '" + raArch + "'");
            }
        }

        final int raSpeed = ra.getIndCpuSpeed();
        
        RangeValue_Type indCPUspeed = null;
        if (raSpeed > -1) {
            final Exact_Type ex = new Exact_Type(raSpeed);
            final Exact_Type[] exacts = {ex};
            indCPUspeed = new RangeValue_Type(exacts,null,null,null);
        }

        if (arch != null || indCPUspeed != null) {

            // each are minOccurs=0
            alloc.setCPU(new CPU_Type(new CPUArchitecture_Type(arch, null),
                                      indCPUspeed));
        }
    }

    // -------------------------------------------------------------------------
    // TRANSLATE TO: ShutdownTasks
    // -------------------------------------------------------------------------

    public ShutdownTasks getShutdownTasks(PostShutdown_Type post,
                                          boolean appendID)
            throws URISyntaxException {
        
        if (post == null) {
            return null;
        }
        final _ShutdownTasks tasks = this.repr._newShutdownTasks();
        
        final URI target =
                this.convertURI(post.getRootPartitionUnpropagationTarget());
        tasks.setBaseFileUnpropagationTarget(target);
        tasks.setAppendID(appendID);

        return tasks;
    }

    // sigh
    public URI convertURI(org.apache.axis.types.URI axisURI)
            throws URISyntaxException {
        return axisURI == null ? null : new URI(axisURI.toString());
    }
}
