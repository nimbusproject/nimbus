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

import org.nimbustools.api._repr._CreateRequest;
import org.nimbustools.api.repr.CustomizationRequest;
import org.nimbustools.api.repr.ctx.Context;
import org.nimbustools.api.repr.vm.Kernel;
import org.nimbustools.api.repr.vm.NIC;
import org.nimbustools.api.repr.vm.RequiredVMM;
import org.nimbustools.api.repr.vm.ResourceAllocation;
import org.nimbustools.api.repr.vm.Schedule;
import org.nimbustools.api.repr.vm.VMFile;

import java.util.Arrays;

public class DefaultCreateRequest implements _CreateRequest {

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private String name;
    private VMFile[] vmFiles;
    private NIC[] nics;
    private Schedule schedule;
    private String coscheduleID;
    private boolean coscheduleDone;
    private boolean coscheduleMember;
    private ResourceAllocation ra;
    private RequiredVMM requiredVMM;
    private Kernel kernel;
    private CustomizationRequest[] custRequests;
    private String shutdownType;
    private String initialStateRequest;
    private Context context;
    private String mdUserData;
    private String sshKeyName;
    

    // -------------------------------------------------------------------------
    // implements org.nimbustools.api.repr.CreateRequest
    // -------------------------------------------------------------------------

    public String getName() {
        return this.name;
    }

    public VMFile[] getVMFiles() {
        return this.vmFiles;
    }

    public NIC[] getRequestedNics() {
        return this.nics;
    }

    public Schedule getRequestedSchedule() {
        return this.schedule;
    }

    public String getCoScheduleID() {
        return this.coscheduleID;
    }

    public boolean isCoScheduleDone() {
        return this.coscheduleDone;
    }

    public boolean isCoScheduleMember() {
        return this.coscheduleMember;
    }

    public ResourceAllocation getRequestedRA() {
        return this.ra;
    }

    public RequiredVMM getRequiredVMM() {
        return this.requiredVMM;
    }

    public Kernel getRequestedKernel() {
        return this.kernel;
    }

    public CustomizationRequest[] getCustomizationRequests() {
        return this.custRequests;
    }

    public String getShutdownType() {
        return this.shutdownType;
    }

    public String getInitialStateRequest() {
        return this.initialStateRequest;
    }

    public Context getContext() {
        return this.context;
    }

    public String getMdUserData() {
        return this.mdUserData;
    }

    public String getSshKeyName() {
        return sshKeyName;
    }

    // -------------------------------------------------------------------------
    // implements org.nimbustools.api._repr._CreateRequest
    // -------------------------------------------------------------------------

    public void setName(String name) {
        this.name = name;
    }

    public void setVMFiles(VMFile[] vmFiles) {
        this.vmFiles = vmFiles;
    }

    public void setRequestedNics(NIC[] nics) {
        this.nics = nics;
    }

    public void setRequestedSchedule(Schedule schedule) {
        this.schedule = schedule;
    }

    public void setCoScheduleID(String id) {
        this.coscheduleID = id;
    }

    public void setCoScheduleDone(boolean done) {
        this.coscheduleDone = done;
    }

    public void setCoScheduleMember(boolean member) {
        this.coscheduleMember = member;
    }

    public void setRequestedRA(ResourceAllocation requestedRA) {
        this.ra = requestedRA;
    }

    public void setRequiredVMM(RequiredVMM requiredVMM) {
        this.requiredVMM = requiredVMM;
    }

    public void setRequestedKernel(Kernel kernel) {
        this.kernel = kernel;
    }

    public void setCustomizationRequests(CustomizationRequest[] requests) {
        this.custRequests = requests;
    }

    public void setShutdownType(String type) {
        this.shutdownType = type;
    }

    public void setInitialStateRequest(String state) {
        this.initialStateRequest = state;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public void setMdUserData(String mdUserData) {
        this.mdUserData = mdUserData;
    }

    public void setSshKeyName(String sshKeyName) {
        this.sshKeyName = sshKeyName;
    }

    // -------------------------------------------------------------------------
    // DEBUG STRING
    // -------------------------------------------------------------------------

    public String toString() {
        final boolean userDataPresent = this.mdUserData != null;
        final String prefix = "\n\n{{{ ";
        return "\nDefaultCreateRequest" +
                prefix + "name='" + this.name + '\'' +
                prefix + "vmFiles=" +
                (this.vmFiles == null ? null : Arrays.asList(this.vmFiles)) +
                prefix + "nics=" +
                (this.nics == null ? null : Arrays.asList(this.nics)) +
                prefix + "schedule=" + this.schedule +
                prefix + "coscheduleID='" + this.coscheduleID + "'" +
                prefix + "coscheduleDone=" + this.coscheduleDone +
                prefix + "coscheduleMember=" + this.coscheduleMember +
                prefix + "ra=" + this.ra +
                prefix + "requiredVMM=" + this.requiredVMM +
                prefix + "kernel=" + this.kernel +
                prefix + "custRequests=" +
                (this.custRequests == null ? null : Arrays.asList(this.custRequests)) +
                prefix + "shutdownType='" + this.shutdownType + "'" +
                prefix + "userDataPresent? " + userDataPresent +
                prefix + "initialStateRequest='" + this.initialStateRequest + "'" +
                prefix + "sshKeyName='" + this.sshKeyName + "'" +
                prefix + "context=" + this.context + "\n";
    }
}
