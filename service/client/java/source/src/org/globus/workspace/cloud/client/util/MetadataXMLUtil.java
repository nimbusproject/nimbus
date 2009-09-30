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

package org.globus.workspace.cloud.client.util;

import org.nimbustools.messaging.gt4_0.generated.metadata.VirtualWorkspace_Type;
import org.nimbustools.messaging.gt4_0.generated.metadata.logistics.Logistics;
import org.nimbustools.messaging.gt4_0.generated.metadata.logistics.VirtualNetwork_Type;
import org.nimbustools.messaging.gt4_0.generated.metadata.logistics.Nic_Type;
import org.nimbustools.messaging.gt4_0.generated.metadata.logistics.IPConfig_Type;
import org.nimbustools.messaging.gt4_0.generated.metadata.logistics.IPConfig_TypeAcquisitionMethod;
import org.nimbustools.messaging.gt4_0.generated.metadata.definition.Definition;
import org.nimbustools.messaging.gt4_0.generated.metadata.definition.DiskCollection_Type;
import org.nimbustools.messaging.gt4_0.generated.metadata.definition.BoundDisk_Type;
import org.nimbustools.messaging.gt4_0.generated.metadata.definition.DiskPermissions_Type;
import org.nimbustools.messaging.gt4_0.generated.metadata.definition.Requirements_Type;
import org.nimbustools.messaging.gt4_0.generated.metadata.definition.VMM_Type;
import org.nimbustools.messaging.gt4_0.generated.metadata.definition.VMM_TypeType;
import org.nimbustools.messaging.gt4_0.generated.metadata.definition.Kernel_Type;
import org.nimbustools.messaging.gt4_0.common.Constants_GT4_0;
import org.apache.axis.types.URI;
import org.ggf.jsdl.CPUArchitecture_Type;
import org.ggf.jsdl.ProcessorArchitectureEnumeration;

import javax.xml.namespace.QName;

/**
 * Construct axis metadata representation.
 *
 * Only handles one partition and one NIC (AllocateAndConfigure only).
 */
public class MetadataXMLUtil {

    public static final QName metadataQName =
                new QName(Constants_GT4_0.NS_METADATA,
                          "VirtualWorkspace");

    public static VirtualWorkspace_Type constructMetadata(URI imageURI,
                                                          String mountAs,
                                                          URI runName,
                                                          String[] associations,
                                                          String[] nicNames,
                                                          String cpuType,
                                                          String vmmVersion,
                                                          String vmmType,
														  URI kernel) {
        
        final VirtualWorkspace_Type vw = new VirtualWorkspace_Type();

        vw.setName(runName);

        vw.setDefinition(constructDefinition(imageURI,
                                             mountAs,
                                             cpuType,
                                             vmmVersion,
                                             vmmType,
											 kernel));

        vw.setLogistics(constructLogistics(associations,
                                           nicNames));

        return vw;
    }

    public static Definition constructDefinition(URI imageURI,
                                                 String mountAs,
                                                 String cpuType,
                                                 String vmmVer,
                                                 String vmmType,
												 URI kernel) {
        final Definition def = new Definition();
        def.setRequirements(constructRequirements(cpuType, vmmVer, vmmType, kernel));
        def.setDiskCollection(contructDiskCollection(imageURI, mountAs));
        return def;
    }

    private static DiskCollection_Type contructDiskCollection(URI imageURI,
                                                              String mountAs) {
        final DiskCollection_Type dctype = new DiskCollection_Type();
        final BoundDisk_Type bd = new BoundDisk_Type();
        bd.setLocation(imageURI);
        bd.setMountAs(mountAs);
        bd.setPermissions(DiskPermissions_Type.ReadWrite);
        dctype.setRootVBD(bd);
        return dctype;
    }
    
    private static Requirements_Type constructRequirements(String cpuType,
                                                           String vmmVersion,
                                                           String vmmType,
														   URI kernel) {
        final Requirements_Type rtype = new Requirements_Type();
        final ProcessorArchitectureEnumeration pae =
                ProcessorArchitectureEnumeration.fromString(cpuType);
        final CPUArchitecture_Type cpu = new CPUArchitecture_Type(pae, null);
        rtype.setCPUArchitecture(cpu);
        final String[] versions = {vmmVersion};
        rtype.setVMM(new VMM_Type(VMM_TypeType.fromString(vmmType), versions));
		if (kernel != null) {
			final Kernel_Type kt = new Kernel_Type(kernel, null, null);
			rtype.setKernel(kt);
		}
        return rtype;
    }

    private static Logistics constructLogistics(String[] associations,
                                                String[] nicNames) {

        if (nicNames == null || nicNames.length == 0) {
            return null;
        }

        if (associations == null || nicNames.length != associations.length) {
            throw new IllegalArgumentException(
                    "associations length and nicNames length must match");
        }

        final Nic_Type[] nics = new Nic_Type[nicNames.length];

        for (int i = 0; i < nicNames.length; i++) {
            final String nicName = nicNames[i];
            final Nic_Type nic = new Nic_Type();
            nic.setAssociation(associations[i]);
            final IPConfig_Type ipconfig = new IPConfig_Type();
            ipconfig.setAcquisitionMethod(
                    IPConfig_TypeAcquisitionMethod.AllocateAndConfigure);
            nic.setIpConfig(ipconfig);
            nic.setName(nicName);
            nics[i] = nic;
        }

        final VirtualNetwork_Type vn = new VirtualNetwork_Type(nics);
        return new Logistics(null, vn);
    }
}
