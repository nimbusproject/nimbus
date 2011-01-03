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
package org.globus.workspace.creation.defaults;

import org.globus.workspace.service.InstanceResource;
import org.globus.workspace.service.binding.vm.FileCopyNeed;
import org.globus.workspace.service.binding.vm.VirtualMachine;
import org.nimbustools.api.repr.ShutdownTasks;
import org.nimbustools.api.services.rm.DestructionCallback;
import org.nimbustools.api.services.rm.StateChangeCallback;

import java.util.Calendar;

/**
 * This class is used for returns from repeated idempotent create requests with
 * instances that have already been terminated.
 */
public class IdempotentInstanceResource implements InstanceResource {
    private static final String UNSUPPORTED_MSG = "this InstanceResource object " +
            "is only for CreationManager returns, not mutative operations";

    protected int id;
    protected String name;
    protected String ensembleId;
    protected String groupId;
    protected int groupSize;
    protected int launchIndex;
    protected String creatorId;
    protected VirtualMachine vm;
    protected int state;
    protected String clientToken;

    public IdempotentInstanceResource(int id,
                                      String name,
                                      String ensembleId,
                                      String groupId,
                                      int groupSize,
                                      int launchIndex,
                                      String creatorId,
                                      VirtualMachine vm,
                                      int state,
                                      String clientToken) {
        this.id = id;
        this.name = name;
        this.ensembleId = ensembleId;
        this.groupId = groupId;
        this.groupSize = groupSize;
        this.launchIndex = launchIndex;
        this.creatorId = creatorId;
        this.vm = vm;
        this.state = state;
        this.clientToken = clientToken;
    }

    public int getID() {
        return id;
    }

    public void setID(int id) {
        throw new UnsupportedOperationException(UNSUPPORTED_MSG);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        throw new UnsupportedOperationException(UNSUPPORTED_MSG);
    }

    public String getEnsembleId() {
        return ensembleId;
    }

    public void setEnsembleId(String name) {
        throw new UnsupportedOperationException(UNSUPPORTED_MSG);
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String name) {
        throw new UnsupportedOperationException(UNSUPPORTED_MSG);
    }

    public int getGroupSize() {
        return groupSize;
    }

    public void setGroupSize(int groupsize) {
        throw new UnsupportedOperationException(UNSUPPORTED_MSG);
    }

    public boolean isLastInGroup() {
        return launchIndex == groupSize-1;
    }

    public void setLastInGroup(boolean lastInGroup) {
        throw new UnsupportedOperationException(UNSUPPORTED_MSG);
    }

    public boolean isPartOfGroupRequest() {
        return groupId != null;
    }

    public void setPartOfGroupRequest(boolean isInGroupRequest) {
        throw new UnsupportedOperationException(UNSUPPORTED_MSG);
    }

    public int getLaunchIndex() {
        return launchIndex;
    }

    public void setLaunchIndex(int launchIndex) {
        throw new UnsupportedOperationException(UNSUPPORTED_MSG);
    }

    public VirtualMachine getVM() {
        return vm;
    }

    public String getCreatorID() {
        return creatorId;
    }

    public void setCreatorID(String ID) {
        throw new UnsupportedOperationException(UNSUPPORTED_MSG);
    }

    public String getClientToken() {
        return clientToken;
    }

    public void setClientToken(String clientToken) {
        throw new UnsupportedOperationException(UNSUPPORTED_MSG);
    }

    public Calendar getStartTime() {
        return null;
    }

    public Calendar getTerminationTime() {
        return null;
    }

    public void setTerminationTime(Calendar termTime) {
        throw new UnsupportedOperationException(UNSUPPORTED_MSG);
    }

    public void setStartTime(Calendar startTime) {
        throw new UnsupportedOperationException(UNSUPPORTED_MSG);
    }

    public void activate() {
        throw new UnsupportedOperationException(UNSUPPORTED_MSG);
    }

    public void activateOverride(int targetState) {
        throw new UnsupportedOperationException(UNSUPPORTED_MSG);
    }

    public void start() {
        throw new UnsupportedOperationException(UNSUPPORTED_MSG);
    }

    public void shutdown(ShutdownTasks tasks) {
        throw new UnsupportedOperationException(UNSUPPORTED_MSG);
    }

    public void shutdownSave(ShutdownTasks tasks) {
        throw new UnsupportedOperationException(UNSUPPORTED_MSG);
    }

    public void pause(ShutdownTasks tasks) {
        throw new UnsupportedOperationException(UNSUPPORTED_MSG);
    }

    public void serialize(ShutdownTasks tasks) {
        throw new UnsupportedOperationException(UNSUPPORTED_MSG);
    }

    public void reboot(ShutdownTasks tasks) {
        throw new UnsupportedOperationException(UNSUPPORTED_MSG);
    }

    public void newStartTime(Calendar startTime) {
        throw new UnsupportedOperationException(UNSUPPORTED_MSG);
    }

    public void newStopTime(Calendar stopTime) {
        throw new UnsupportedOperationException(UNSUPPORTED_MSG);
    }

    public void newNetwork(String network) {
        throw new UnsupportedOperationException(UNSUPPORTED_MSG);
    }

    public void newFileCopyNeed(FileCopyNeed need) {
        throw new UnsupportedOperationException(UNSUPPORTED_MSG);
    }

    public void remove() {
        throw new UnsupportedOperationException(UNSUPPORTED_MSG);
    }

    public void setState(int state, Throwable throwable) {
        throw new UnsupportedOperationException(UNSUPPORTED_MSG);
    }

    public int getState() {
        return state;
    }

    public Throwable getStateThrowable() {
        return null;
    }

    public void setTargetState(int state) {
        throw new UnsupportedOperationException(UNSUPPORTED_MSG);
    }

    public int getTargetState() {
        throw new UnsupportedOperationException(UNSUPPORTED_MSG);
    }

    public boolean isOpsEnabled() {
        return false;
    }

    public void setOpsEnabled(boolean opsEnabled) {
        throw new UnsupportedOperationException(UNSUPPORTED_MSG);
    }

    public boolean isPropagateRequired() {
        return false;
    }

    public boolean isUnPropagateRequired() {
        return false;
    }

    public boolean isPropagateStartOK() {
        return false;
    }

    public boolean isVMMaccessOK() {
        return false;
    }

    public void setVMMaccessOK(boolean vmmAccessOK) {
        throw new UnsupportedOperationException(UNSUPPORTED_MSG);
    }

    public void newHostname(String hostname) {
        throw new UnsupportedOperationException(UNSUPPORTED_MSG);
    }

    public void registerStateChangeListener(StateChangeCallback listener) {
        throw new UnsupportedOperationException(UNSUPPORTED_MSG);
    }

    public void registerDestructionListener(DestructionCallback listener) {
        throw new UnsupportedOperationException(UNSUPPORTED_MSG);
    }

    public void populate(int id, VirtualMachine binding, Calendar startTime, Calendar termTime, String node, double chargeRatio) {
        throw new UnsupportedOperationException(UNSUPPORTED_MSG);
    }

    public void load(String key) {
        throw new UnsupportedOperationException(UNSUPPORTED_MSG);
    }

    public void setInitialOpsEnabled(boolean opsEnabled) {
        throw new UnsupportedOperationException(UNSUPPORTED_MSG);
    }

    public void setInitialState(int state, Throwable throwable) {
        throw new UnsupportedOperationException(UNSUPPORTED_MSG);
    }

    public void setInitialTargetState(int state) {
        throw new UnsupportedOperationException(UNSUPPORTED_MSG);
    }

    public void setInitialVMMaccessOK(boolean trashOK) {
        throw new UnsupportedOperationException(UNSUPPORTED_MSG);
    }

    public void setChargeRatio(double chargeRatio) {
        throw new UnsupportedOperationException(UNSUPPORTED_MSG);
    }

    public double getChargeRatio() {
        throw new UnsupportedOperationException(UNSUPPORTED_MSG);
    }
}
