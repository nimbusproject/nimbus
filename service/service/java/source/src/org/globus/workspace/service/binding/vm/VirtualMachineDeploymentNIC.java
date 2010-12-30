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

import java.io.Serializable;

public class VirtualMachineDeploymentNIC implements Serializable {
    private String name;
    // todo: double? in JSDL, it's a double
    private int incomingNetworkBandwidth = -1;
    private int outgoingNetworkBandwidth = -1;

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getIncomingNetworkBandwidth() {
        return this.incomingNetworkBandwidth;
    }

    public void setIncomingNetworkBandwidth(int incoming) {
        this.incomingNetworkBandwidth = incoming;
    }

    public int getOutgoingNetworkBandwidth() {
        return this.outgoingNetworkBandwidth;
    }

    public void setOutgoingNetworkBandwidth(int outgoing) {
        this.outgoingNetworkBandwidth = outgoing;
    }

    public static VirtualMachineDeploymentNIC cloneOne(
                                  final VirtualMachineDeploymentNIC nic) {

        if (nic == null) {
            return null;
        }

        final VirtualMachineDeploymentNIC newnic =
                            new VirtualMachineDeploymentNIC();
        
        newnic.incomingNetworkBandwidth = nic.incomingNetworkBandwidth;
        newnic.name = nic.name;
        newnic.outgoingNetworkBandwidth = nic.outgoingNetworkBandwidth;

        return newnic;
    }
}
