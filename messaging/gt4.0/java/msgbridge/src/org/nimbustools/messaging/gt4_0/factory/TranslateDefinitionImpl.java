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

package org.nimbustools.messaging.gt4_0.factory;

import org.apache.axis.types.URI;
import org.ggf.jsdl.CPUArchitecture_Type;
import org.ggf.jsdl.ProcessorArchitectureEnumeration;
import org.nimbustools.api._repr._CreateRequest;
import org.nimbustools.api._repr.vm._Kernel;
import org.nimbustools.api._repr.vm._RequiredVMM;
import org.nimbustools.api._repr.vm._ResourceAllocation;
import org.nimbustools.api._repr.vm._VMFile;
import org.nimbustools.api.repr.CannotTranslateException;
import org.nimbustools.api.repr.ReprFactory;
import org.nimbustools.api.repr.vm.ResourceAllocation;
import org.nimbustools.api.repr.vm.VMFile;
import org.nimbustools.messaging.gt4_0.generated.metadata.definition.BlankDisk_Type;
import org.nimbustools.messaging.gt4_0.generated.metadata.definition.BoundDisk_Type;
import org.nimbustools.messaging.gt4_0.generated.metadata.definition.Definition;
import org.nimbustools.messaging.gt4_0.generated.metadata.definition.DiskCollection_Type;
import org.nimbustools.messaging.gt4_0.generated.metadata.definition.DiskPermissions_Type;
import org.nimbustools.messaging.gt4_0.generated.metadata.definition.Kernel_Type;
import org.nimbustools.messaging.gt4_0.generated.metadata.definition.Requirements_Type;
import org.nimbustools.messaging.gt4_0.generated.metadata.definition.VMM_Type;
import org.nimbustools.messaging.gt4_0.generated.metadata.definition.VMM_TypeType;
import org.nimbustools.messaging.gt4_0.generated.negotiable.PostShutdown_Type;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;

public class TranslateDefinitionImpl implements TranslateDefinition {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    protected static final HashMap archMap = new HashMap(16);
    static {
        archMap.put(ProcessorArchitectureEnumeration.arm,
                    _ResourceAllocation.ARCH_arm);
        archMap.put(ProcessorArchitectureEnumeration.ia64,
                    _ResourceAllocation.ARCH_ia64);
        archMap.put(ProcessorArchitectureEnumeration.mips,
                    _ResourceAllocation.ARCH_mips);
        archMap.put(ProcessorArchitectureEnumeration.other,
                    _ResourceAllocation.ARCH_other);
        archMap.put(ProcessorArchitectureEnumeration.parisc,
                    _ResourceAllocation.ARCH_parisc);
        archMap.put(ProcessorArchitectureEnumeration.powerpc,
                    _ResourceAllocation.ARCH_powerpc);
        archMap.put(ProcessorArchitectureEnumeration.sparc,
                    _ResourceAllocation.ARCH_sparc);
        archMap.put(ProcessorArchitectureEnumeration.x86,
                    _ResourceAllocation.ARCH_x86);
        archMap.put(ProcessorArchitectureEnumeration.x86_32,
                    _ResourceAllocation.ARCH_x86_32);
        archMap.put(ProcessorArchitectureEnumeration.x86_64,
                    _ResourceAllocation.ARCH_x86_64);
    }

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected ReprFactory repr;


    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    public TranslateDefinitionImpl(ReprFactory reprFactory) {
        if (reprFactory == null) {
            throw new IllegalArgumentException("reprFactory may not be null");
        }
        this.repr = reprFactory;
    }


    // -------------------------------------------------------------------------
    // TRANSLATE DEFINITION
    // -------------------------------------------------------------------------

    public void translateDefinitionRelated(_CreateRequest req,
                                           Definition def,
                                           PostShutdown_Type post)
            throws CannotTranslateException {

        if (req == null) {
            throw new IllegalArgumentException("req may not be null");
        }
        if (def == null) {
            throw new IllegalArgumentException("def may not be null");
        }

        final Requirements_Type requires = def.getRequirements();
        if (requires == null) {
            throw new CannotTranslateException(
                    "Requirements_Type may not be missing");
        }

        this.translateRequirement_Type(req, requires);

        final DiskCollection_Type disks = def.getDiskCollection();
        if (disks == null) {
            throw new CannotTranslateException(
                    "DiskCollection_Type may not be missing");
        }

        this.translateDiskCollection_Type(req, disks);

        if (post != null) {
            final URI unpropTarget = post.getRootPartitionUnpropagationTarget();
            if (unpropTarget != null) {
                final VMFile[] vmFiles = req.getVMFiles();
                if (vmFiles == null) {
                    throw new CannotTranslateException(
                            "expecting files to be translated already");
                }
                for (int i = 0; i < vmFiles.length; i++) {
                    if (!(vmFiles[i] instanceof _VMFile)) {
                        throw new CannotTranslateException(
                            "expecting writable VMFile");
                    }

                    final _VMFile file = (_VMFile)vmFiles[i];

                    if (file.isRootFile()) {
                        file.setUnpropURI(this.convertURI(unpropTarget));
                        break;
                    }
                }
            }
        }
    }

    protected void translateRequirement_Type(_CreateRequest req,
                                             Requirements_Type requires)
            throws CannotTranslateException {

        if (req == null) {
            throw new IllegalArgumentException("req may not be null");
        }
        if (requires == null) {
            throw new IllegalArgumentException("requires may not be null");
        }

        final CPUArchitecture_Type arch = requires.getCPUArchitecture();
        if (arch == null) {
            throw new CannotTranslateException(
                    "CPUArchitecture_Type may not be missing");
        }

        final ProcessorArchitectureEnumeration archEnum =
                                        arch.getCPUArchitectureName();
        if (archEnum == null) {
            throw new CannotTranslateException(
                    "ProcessorArchitectureEnumeration may not be missing");
        }

        final String archStr = (String) archMap.get(archEnum);
        if (archStr == null) {
            throw new CannotTranslateException(
                    "ProcessorArchitectureEnumeration contains " +
                            "unrecognized value '" + archEnum.toString() + "'");
        }

        ResourceAllocation ra = req.getRequestedRA();
        if (ra == null) {
            ra = this.repr._newResourceAllocation();
            req.setRequestedRA(ra);
        }
        if (!(ra instanceof _ResourceAllocation)) {
            throw new CannotTranslateException(
                    "expecting writable ResourceAllocation");
        }
        ((_ResourceAllocation)ra).setArchitecture(archStr);

        final VMM_Type vmm = requires.getVMM();
        if (vmm != null) {
            final VMM_TypeType type = vmm.getType();
            if (type != null) {
                final _RequiredVMM reqVMM = this.repr._newRequiredVMM();
                reqVMM.setType(type.getValue());
                reqVMM.setVersions(vmm.getVersion());
                req.setRequiredVMM(reqVMM);
            }
        }

        final Kernel_Type kernel = requires.getKernel();
        if (kernel != null) {
            final _Kernel reqKernel = this.repr._newKernel();
            final URI image = kernel.getImage();
            reqKernel.setKernel(this.convertURI(image));
            reqKernel.setParameters(kernel.getParameters());
            req.setRequestedKernel(reqKernel);
        }
    }

    protected void translateDiskCollection_Type(_CreateRequest req,
                                                DiskCollection_Type disks)
            throws CannotTranslateException {

        if (req == null) {
            throw new IllegalArgumentException("req may not be null");
        }
        if (disks == null) {
            throw new IllegalArgumentException("disks may not be null");
        }

        this.serializationUnsupported(disks);

        final BoundDisk_Type rootdisk = disks.getRootVBD();
        if (rootdisk == null) {
            throw new CannotTranslateException(
                    "root disk/partition description may not be missing");
        }

        final ArrayList files = new ArrayList(8);
        files.add(this.translateRootFile(rootdisk));

        final BoundDisk_Type[] parts = disks.getPartition();
        if (parts != null && parts.length > 0) {
            for (int i = 0; i < parts.length; i++) {
                if (parts[i] != null) {
                    files.add(this.translateOneFile(parts[i]));
                }
            }
        }

        final BlankDisk_Type[] blanks = disks.getBlankspacePartition();
        if (blanks != null && blanks.length > 0) {
            for (int i = 0; i < blanks.length; i++) {
                if (blanks[i] != null) {
                    files.add(this.translateOneBlank(blanks[i]));
                }
            }
        }

        final _VMFile[] vmFiles =
                (_VMFile[]) files.toArray(new _VMFile[files.size()]);

        req.setVMFiles(vmFiles);
    }

    protected void serializationUnsupported(DiskCollection_Type disks)
            throws CannotTranslateException {

        if (disks == null) {
            throw new IllegalArgumentException("disks may not be null");
        }
        if (disks.getRAM() != null) {
            throw new CannotTranslateException(
                    "VWS API does not support serialized images as input yet");
        }
        if (disks.getSwap() != null) {
            throw new CannotTranslateException(
                    "VWS API does not support serialized images as input yet");
        }
    }

    protected _VMFile translateRootFile(BoundDisk_Type bd)
            throws CannotTranslateException {
        return this._translateFile(bd, true);
    }

    protected _VMFile translateOneFile(BoundDisk_Type bd)
            throws CannotTranslateException {
        return this._translateFile(bd, false);
    }

    protected _VMFile _translateFile(BoundDisk_Type bd, boolean rootfile)
            throws CannotTranslateException {

        if (bd == null) {
            throw new IllegalArgumentException("bd may not be null");
        }

        final _VMFile file = this.repr._newVMFile();

        file.setRootFile(rootfile);
        file.setBlankSpaceName(null);
        file.setBlankSpaceSize(-1);

        final URI uri = bd.getLocation();
        if (uri == null) {
            throw new CannotTranslateException("a disk is missing location");
        }

        file.setURI(this.convertURI(uri));

        final String mountAs = bd.getMountAs();
        if (mountAs == null) {
            throw new CannotTranslateException("a disk is missing mountAs");
        }
        file.setMountAs(mountAs);

        final DiskPermissions_Type perms = bd.getPermissions();

        if (perms == null) {
            file.setDiskPerms(VMFile.DISKPERMS_ReadWrite);
        } else if (perms.equals(DiskPermissions_Type.ReadOnly)) {
            file.setDiskPerms(VMFile.DISKPERMS_ReadOnly);
        } else if (perms.equals(DiskPermissions_Type.ReadWrite)) {
            file.setDiskPerms(VMFile.DISKPERMS_ReadWrite);
        } else {
            throw new CannotTranslateException(
                    "unknown disk permission: '" + perms + "'");
        }

        return file;
    }

    protected _VMFile translateOneBlank(BlankDisk_Type bd)
            throws CannotTranslateException {

        if (bd == null) {
            throw new IllegalArgumentException("bd may not be null");
        }

        final _VMFile file = this.repr._newVMFile();
        file.setRootFile(false);
        file.setURI(null);

        final String name = bd.getPartitionName();
        if (name == null) {
            throw new CannotTranslateException(
                    "a blank disk description is missing PartitionName");
        }
        file.setBlankSpaceName(name);

        final String mountAs = bd.getMountAs();
        if (mountAs == null) {
            throw new CannotTranslateException(
                    "a blank disk description is missing mountAs");
        }
        file.setMountAs(mountAs);

        // setBlankSpaceSize is needed, but will get set in resource allocation
        // consumption methods

        return file;
    }

    // sigh
    public java.net.URI convertURI(URI axisURI)
            throws CannotTranslateException {
        try {
            return axisURI == null ? null : new java.net.URI(axisURI.toString());
        } catch (URISyntaxException e) {
            throw new CannotTranslateException(e.getMessage(), e);
        }
    }
     
    
}
