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

package org.globus.workspace.service.binding.vm;

import org.globus.workspace.WorkspaceConstants;

public class VirtualMachineDeployment {

    public static final int NOTSET = -1;

    // seconds
    private int minDuration = NOTSET;
    private int requestedState = WorkspaceConstants.STATE_INVALID;
    private int requestedDefaultShutdown =
                       WorkspaceConstants.DEFAULT_SHUTDOWN_NORMAL;
    private VirtualMachineDeploymentNIC[] nics;
    private VirtualMachineDeploymentStorage[] storage;
    private String CPUArchitecture;
    // TODO: change these to double?  JSDL RangeValue_Type uses doubles
    private int individualCPUSpeed = NOTSET;
    private int CPUPercentage = NOTSET;
    private int individualPhysicalMemory = NOTSET;

    public static String shutdownMechName(int i) {
        String name;
        switch (i) {
            case WorkspaceConstants.DEFAULT_SHUTDOWN_NORMAL:
                 name = "Normal"; break;
            case WorkspaceConstants.DEFAULT_SHUTDOWN_SERIALIZE:
                 name = "Serialize"; break;
            case WorkspaceConstants.DEFAULT_SHUTDOWN_TRASH:
                 name = "Trash"; break;
            default:
                 name = "Invalid";
        }
        return name;
    }

    public int getMinDuration() {
        return this.minDuration;
    }

    public void setMinDuration(int minDuration) {
        this.minDuration = minDuration;
    }

    public int getRequestedState() {
        return this.requestedState;
    }

    public void setRequestedState(int requestedState) {
        this.requestedState = requestedState;
    }

    public int getRequestedShutdown() {
        return this.requestedDefaultShutdown;
    }

    public void setRequestedShutdown(int requestedDefaultShutdown) {
        if (requestedDefaultShutdown < WorkspaceConstants.DEFAULT_SHUTDOWN_NORMAL ||
            requestedDefaultShutdown > WorkspaceConstants.DEFAULT_SHUTDOWN_TRASH) {
            this.requestedDefaultShutdown =
                    WorkspaceConstants.DEFAULT_SHUTDOWN_INVALID;
        } else {
            this.requestedDefaultShutdown = requestedDefaultShutdown;
        }
    }

    public VirtualMachineDeploymentNIC[] getNics() {
        return this.nics;
    }

    public void setNics(VirtualMachineDeploymentNIC[] nics) {
        this.nics = nics;
    }

    public VirtualMachineDeploymentStorage[] getStorage() {
        return this.storage;
    }

    public void setStorage(VirtualMachineDeploymentStorage[] storage) {
        this.storage = storage;
    }

    public String getCPUArchitecture() {
        return this.CPUArchitecture;
    }

    public void setCPUArchitecture(String CPUArchitecture) {
        this.CPUArchitecture = CPUArchitecture;
    }

    public int getIndividualCPUSpeed() {
        return this.individualCPUSpeed;
    }

    public void setIndividualCPUSpeed(int individualCPUSpeed) {
        this.individualCPUSpeed = individualCPUSpeed;
    }

    public int getCPUPercentage() {
        return this.CPUPercentage;
    }

    public void setCPUPercentage(int CPUPercentage) {
        this.CPUPercentage = CPUPercentage;
    }

    public int getIndividualPhysicalMemory() {
        return this.individualPhysicalMemory;
    }

    public void setIndividualPhysicalMemory(int individualPhysicalMemory) {
        this.individualPhysicalMemory = individualPhysicalMemory;
    }

    public static VirtualMachineDeployment cloneOne(
                                    final VirtualMachineDeployment vmd) {

        if (vmd == null) {
            return null;
        }

        final VirtualMachineDeployment newvmd = new VirtualMachineDeployment();

        newvmd.CPUArchitecture = vmd.CPUArchitecture;
        newvmd.CPUPercentage = vmd.CPUPercentage;
        newvmd.individualCPUSpeed = vmd.individualCPUSpeed;
        newvmd.individualPhysicalMemory = vmd.individualPhysicalMemory;
        newvmd.minDuration = vmd.minDuration;
        newvmd.requestedDefaultShutdown = vmd.requestedDefaultShutdown;
        newvmd.requestedState = vmd.requestedState;


        if (vmd.nics != null) {
            newvmd.nics = new VirtualMachineDeploymentNIC[vmd.nics.length];
            for (int i = 0; i < vmd.nics.length; i++) {
                newvmd.nics[i] =
                        VirtualMachineDeploymentNIC.cloneOne(vmd.nics[i]);
            }
        }

        if (vmd.storage != null) {
            newvmd.storage =
                    new VirtualMachineDeploymentStorage[vmd.storage.length];
            for (int i = 0; i < vmd.storage.length; i++) {
                newvmd.storage[i] =
                      VirtualMachineDeploymentStorage.cloneOne(vmd.storage[i]);
            }
        }

        return newvmd;
    }
}
