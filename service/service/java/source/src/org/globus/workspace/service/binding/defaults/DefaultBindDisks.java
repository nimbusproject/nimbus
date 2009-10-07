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

import org.globus.workspace.WorkspaceConstants;
import org.globus.workspace.service.binding.BindDisks;
import org.globus.workspace.service.binding.GlobalPolicies;
import org.globus.workspace.service.binding.vm.VirtualMachine;
import org.globus.workspace.service.binding.vm.VirtualMachinePartition;
import org.nimbustools.api.repr.vm.VMFile;
import org.nimbustools.api.services.rm.CreationException;
import org.nimbustools.api.services.rm.ResourceRequestDeniedException;

import java.util.ArrayList;
import java.net.URI;

public class DefaultBindDisks implements BindDisks {
    
    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected final GlobalPolicies globals;
	protected String sda1Replacement;

    
    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    public DefaultBindDisks(GlobalPolicies globalPolicies) {
        if (globalPolicies == null) {
            throw new IllegalArgumentException("globalPolicies may not be null");
        }
        this.globals = globalPolicies;
    }
    

    // -------------------------------------------------------------------------
	// GET/SET
	// -------------------------------------------------------------------------

	public void setSda1Replacement(String sda1Replacement) {
		this.sda1Replacement = sda1Replacement;
	}
	

	// -------------------------------------------------------------------------
    // implements BindDisks
    // -------------------------------------------------------------------------

    public void consume(VirtualMachine vm,
                        VMFile[] vmFiles)
            throws CreationException, ResourceRequestDeniedException {

        if (vmFiles == null || vmFiles.length == 0) {
            throw new CreationException("VM file description(s) missing");
        }

        final String vmName = vm.getName();

        boolean seenRootDisk = false;
        
        final boolean propagationEnabled = this.globals.isPropagateEnabled();
        
        final ArrayList partitions = new ArrayList(8);

        for (int i = 0; i < vmFiles.length; i++) {
            
            final VMFile file = vmFiles[i];
            if (file == null) {
                throw new CreationException(
                        "vmFile[] contents may not be null (index " + i + ")");
            }

            if (file.isRootFile()) {

                if (seenRootDisk) {
                    throw new CreationException("more than one root disk?");
                } else {
                    seenRootDisk = true;
                }

                // TODO:
                // only propagating root disk until multiple partitions are
                // propagated and staged as well, other (presumably readonly)
                // partitions are assumed to be in local disk cache for now
                partitions.add(this.rootPartition(vm,
                                                  file,
                                                  propagationEnabled));

            } else if (file.getBlankSpaceName() != null) {

                partitions.add(
                        this.blankspacePartition(file.getBlankSpaceSize(),
                                                 file,
                                                 vmName));
            } else {
                
                partitions.add(this.regularPartition(file,
                                                     vmName));
            }
        }

        final VirtualMachinePartition[] partitionArray =
                (VirtualMachinePartition[]) partitions.toArray(
                               new VirtualMachinePartition[partitions.size()]);

        vm.setPartitions(partitionArray);
    }

    
    // -------------------------------------------------------------------------
    // IMPL
    // -------------------------------------------------------------------------

    protected VirtualMachinePartition rootPartition(VirtualMachine vm,
                                                    VMFile file,
                                                    boolean propagationEnabled)
            throws CreationException {
        
        if (file == null) {
            throw new IllegalArgumentException("file may not be null");
        }

        final URI uri = file.getURI();
        if (uri == null) {
            throw new IllegalArgumentException("file.uri may not be null");
        }

        String mountAs = file.getMountAs();
        if (mountAs == null) {
            throw new IllegalArgumentException("file.mountAs may not be null");
        }
        
		// hack for newer Xen situations and backwards compatibility
		if (this.sda1Replacement != null && mountAs.trim().equalsIgnoreCase("sda1")) {
			mountAs = this.sda1Replacement;
		}

        final String rootDiskScheme = uri.getScheme();
        final boolean local = "file".equals(rootDiskScheme);

        if (!propagationEnabled && !local) {
            final String err = "cannot propagate: supplied image '" +
                    uri.toASCIIString() + "' is not specified with " +
                    "file:// and propagation is disabled";
            throw new CreationException(err);
        }

        final VirtualMachinePartition partition = new VirtualMachinePartition();

        if (propagationEnabled && !local) {
            vm.setPropagateRequired(true);
            partition.setPropRequired(true);
            if (vm.getDeployment().getRequestedShutdown() ==
                               WorkspaceConstants.DEFAULT_SHUTDOWN_TRASH) {
                vm.setUnPropagateRequired(false);
                partition.setUnPropRequired(false);
            }
            vm.setUnPropagateRequired(true);
            partition.setUnPropRequired(true);
        }

        partition.setImage(uri.toASCIIString());
        partition.setImagemount(mountAs);
        partition.setRootdisk(true);
        partition.setBlankspace(0);

        final String perms = file.getDiskPerms();
        if (VMFile.DISKPERMS_ReadOnly.equals(perms)) {
            partition.setReadwrite(false);
        } else if (VMFile.DISKPERMS_ReadWrite.equals(perms)) {
            partition.setReadwrite(true);
        }

        // only applicable to rootPartition currently
        // waiting on generalization of disk and propagation (both in
        // messaging layers and file movement tools)
        final URI unpropURI = file.getUnpropURI();
        if (unpropURI != null) {
            partition.setAlternateUnpropTarget(unpropURI.toASCIIString());
        }

        return partition;
    }

    protected VirtualMachinePartition regularPartition(VMFile file,
                                                       String vmName)
            throws CreationException {
        
        final VirtualMachinePartition partition = new VirtualMachinePartition();

        // see root partition comments
        partition.setPropRequired(false);
        partition.setUnPropRequired(false);

        final URI uri = file.getURI();

        if (!uri.getScheme().equals("file")) {
            final String err = "request for '" + vmName + "' contains " +
                    "partition that is not root disk or a blank space " +
                    "creation request but it is not a cache reference " +
                    "(currently no propagate support for these)";
            throw new CreationException(err);
        }

        partition.setImage(uri.toASCIIString());
        partition.setImagemount(file.getMountAs());
        partition.setRootdisk(false);
        partition.setBlankspace(0);

        final String perms = file.getDiskPerms();
        if (VMFile.DISKPERMS_ReadOnly.equals(perms)) {
            partition.setReadwrite(false);
        } else if (VMFile.DISKPERMS_ReadWrite.equals(perms)) {
            partition.setReadwrite(true);
        }

        return partition;
    }

    protected VirtualMachinePartition blankspacePartition(int megabytes,
                                                          VMFile file,
                                                          String vmName)
            throws CreationException {

        if (megabytes < 1) {
            final String err = "blank space request under 1 megabyte in " +
                    "request for '" + vmName + "'";
            throw new CreationException(err);
        }

        final VirtualMachinePartition partition = new VirtualMachinePartition();
        partition.setPropRequired(false);
        partition.setUnPropRequired(false);
        partition.setImage("file://fake");
        partition.setImagemount(file.getMountAs());
        partition.setRootdisk(false);
        partition.setReadwrite(true);
        partition.setBlankspace(megabytes);

        return partition;
    }
}
