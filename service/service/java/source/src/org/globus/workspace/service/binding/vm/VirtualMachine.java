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
import org.globus.workspace.Lager;
import org.globus.workspace.service.binding.WorkspaceInstantiation;
import org.nimbustools.api.services.rm.ManageException;
import org.apache.commons.logging.Log;

/**
 * This internal VirtualMachine representation will go away, moving to using
 * the org.nimbustools.api representation objects directly (or at least
 * our own implementations of the interfaces) to avoid copies.
 *
 * Access to this is not synchronized, assumes set once (keeping set
 * methods for our manual ORM).
 */
public class VirtualMachine extends WorkspaceInstantiation {

    private VirtualMachineDeployment deployment;

    private String network;

    private String kernel;

    private String kernelParameters;

    private String node;
    
    private boolean preemptable;

    private String associationsNeeded;

    private VirtualMachinePartition[] partitions;

    private FileCopyNeed[] fileCopyNeeds;

    private String credentialName;

    private String mdUserData;
   //requested vmm type
    private String vmm;
    //requested vmm version
    private String vmmVersion;

    /* ------------------------------------------------ */
    /*   get/set                                        */
    /* ------------------------------------------------ */

    public VirtualMachineDeployment getDeployment() {
        return this.deployment;
    }

    public void setDeployment(VirtualMachineDeployment deployment) {
        this.deployment = deployment;
    }

    public String getKernelParameters() {
        return this.kernelParameters;
    }

    public void setKernelParameters(String kernelParameters) {
        this.kernelParameters = kernelParameters;
    }

    public String getKernel() {
        return this.kernel;
    }

    public void setKernel(String kernel) {
        this.kernel = kernel;
    }

    public String getNetwork() {
        return this.network;
    }

    public void setNetwork(String network) {
        this.network = network;
    }

    public String getAssociationsNeeded() {
        return this.associationsNeeded;
    }

    public void setAssociationsNeeded(String associationsNeeded) {
        this.associationsNeeded = associationsNeeded;
    }

    public VirtualMachinePartition[] getPartitions() {
        return this.partitions;
    }

    public void setPartitions(VirtualMachinePartition[] partitions) {
        this.partitions = partitions;
    }

    public String getNode() {
        return this.node;
    }

    public void setNode(String node) {
        if (node == null) {
            this.node = null;
        } else if (node.equalsIgnoreCase("null")) {
            this.node = null;
        } else {
            this.node = node;
        }
    }

    public String getVmm() {
        return this.vmm;
    }

    public void setVmm(String vmm) {
        this.vmm = vmm;
    }

    public String getVmmVersion() {
        return this.vmmVersion;
    }

    public void setVmmVersion(String vmmVersion) {
        this.vmmVersion = vmmVersion;
    }

    public String getMdUserData() {
        return this.mdUserData;
    }

    public void setMdUserData(String mdUserData) {
        this.mdUserData = mdUserData;
    }

    public boolean isPreemptable() {
        return preemptable;
    }

    public void setPreemptable(boolean preemptable) {
        this.preemptable = preemptable;
    }

    public String getCredentialName() {
        return this.credentialName;
    }

    public void setCredentialName(String credentialName) {
        this.credentialName = credentialName;
    }

    public synchronized void addFileCopyNeed(FileCopyNeed need) {
        if (this.fileCopyNeeds == null) {
            this.fileCopyNeeds = new FileCopyNeed[1];
            this.fileCopyNeeds[0] = need;
        } else {
            final int curlen = this.fileCopyNeeds.length;
            final FileCopyNeed[] src = this.fileCopyNeeds;
            final FileCopyNeed[] dst = new FileCopyNeed[curlen+1];
            System.arraycopy(src, 0, dst, 0, curlen);
            dst[curlen] = need;
            this.fileCopyNeeds = dst;
        }
    }

    public synchronized FileCopyNeed[] getFileCopyNeeds() {
        if (this.fileCopyNeeds == null) {
            return new FileCopyNeed[0];
        } else {
            return this.fileCopyNeeds;
        }
    }

    public synchronized boolean isFileCopyAllDone() {
        if (this.fileCopyNeeds == null) {
            return true;
        }
        for (int i = 0; i < this.fileCopyNeeds.length; i++) {
            if (!this.fileCopyNeeds[i].onImage()) {
                return false;
            }
        }
        return true;
    }

    public synchronized void addUnpropTargetSuffixes()

            throws ManageException {

        if (this.id == null) {
            throw new IllegalStateException("cannot call if ID is unset");
        }

        final String suffix = "-" + this.id.toString();
        this._adjustRootUnpropTarget(null, suffix, null);
    }

    public synchronized void overrideRootUnpropTarget(String path, Log logger)

            throws ManageException {

        this._adjustRootUnpropTarget(path, null, logger);
    }

    private void _adjustRootUnpropTarget(String path, String suffix, Log logger)

            throws ManageException {
        
        if (this.partitions == null || this.partitions.length == 0) {
            throw new ManageException("partitions do not exist");
        }

        boolean found = false;
        for (int i = 0; i < this.partitions.length; i++) {
            if (this.partitions[i].isRootdisk()) {

                final String old = this.partitions[i].getAlternateUnpropTarget();

                if (logger != null && old != null) {
                    logger.info(Lager.ev(this.id) + "Overriding previously " +
                            "set alternate unpropagate target: '" + old + "'");
                } else if (logger != null) {
                    logger.info(Lager.ev(this.id) + "Setting " +
                            "alternate unpropagate target: '" + path + "'");
                }

                if (path != null) {

                    if (suffix != null) {
                        final String newpath = path + suffix;
                        this.partitions[i].setAlternateUnpropTarget(newpath);
                    } else {
                        this.partitions[i].setAlternateUnpropTarget(path);
                    }

                } else if (old != null && suffix != null) {
                    final String newpath = old + suffix;
                    this.partitions[i].setAlternateUnpropTarget(newpath);
                }
                
                found = true;
                break;
            }
        }

        if (!found) {
            throw new ManageException(
                    "could not find root disk to override its target");
        }
    }

    public String toString() {

        StringBuffer parts = null;
        if (this.partitions != null) {
            parts = new StringBuffer();
            for (int i = 0; i < partitions.length; i++) {
                parts.append(this.partitions[i]);
                parts.append("\n");
            }
        }

        String defaultShutdown = null;
        if (this.deployment != null) {
            defaultShutdown = VirtualMachineDeployment.shutdownMechName(
                                deployment.getRequestedShutdown());
        }

        int custLen = 0;
        if (this.fileCopyNeeds != null) {
            custLen = this.fileCopyNeeds.length;
        }

        boolean userDataPresent = this.mdUserData != null;

        return "VirtualMachine{" +
                "deployment=" + this.deployment +
                ", network='" + this.network + '\'' +
                ", kernel='" + this.kernel + '\'' +
                ", kernelParameters='" + this.kernelParameters + '\'' +
                ", node=" + this.node +
                ", propagateRequired='" + this.propagateRequired + '\'' +
                ", unPropagateRequired='" + this.unPropagateRequired + '\'' +
                ", defaultShutdown='" + defaultShutdown + '\'' +
                ", vmm='" + this.vmm + '\'' +
                ", vmmVersion='" + this.vmmVersion + '\'' +
                ", mdUserData is present? " + userDataPresent +
                ", partitions='" + parts + '\'' +
                ", networksNeeded='" + this.associationsNeeded + '\'' +
                ", customizationsNeeds length='" + custLen + '\'' +
                ", preemptable: " + this.preemptable+ '\'' +
                '}';
    }

    public boolean isPropagateStartOK() {
        return this.fileCopyNeeds == null ||
               this.fileCopyNeeds.length <= 0;
    }

    // part of the instantiation interface, nothing about VM deployment
    // class is known there
    public int getRequestedShutdownMechanism() {
        if (this.deployment != null) {
            return this.deployment.getRequestedShutdown();
        } else {
            return WorkspaceConstants.DEFAULT_SHUTDOWN_INVALID;
        }
    }

    // don't use clone()
    // be sure to differentiate afterwards if that's what is desired...
    public static VirtualMachine cloneOne(final VirtualMachine vm)
            throws Exception {
        
        if (vm == null) {
            return null;
        }

        final VirtualMachine newvm = new VirtualMachine();

        newvm.id = vm.id;
        newvm.associationsNeeded = vm.associationsNeeded;
        newvm.kernel = vm.kernel;
        newvm.kernelParameters = vm.kernelParameters;
        newvm.name = vm.name;
        newvm.network = vm.network;
        newvm.node = vm.node;
        newvm.propagateRequired = vm.propagateRequired;
        newvm.propagateStartOK = vm.propagateStartOK;
        newvm.unPropagateRequired = vm.unPropagateRequired;
        newvm.vmm = vm.vmm;
        newvm.vmmVersion = vm.vmmVersion;
        newvm.preemptable = vm.preemptable;

        if (vm.partitions != null) {
            newvm.partitions =
                    new VirtualMachinePartition[vm.partitions.length];
            for (int i = 0; i < vm.partitions.length; i++) {
                newvm.partitions[i] =
                        VirtualMachinePartition.cloneOne(vm.partitions[i]);
            }
        }

        if (vm.deployment != null) {
            newvm.deployment =
                    VirtualMachineDeployment.cloneOne(vm.deployment);
        }

        newvm.fileCopyNeeds =
                FileCopyNeed.cloneArray(vm.fileCopyNeeds);

        newvm.mdUserData = vm.mdUserData;
        newvm.credentialName = vm.credentialName;

        return newvm;
    }
}
