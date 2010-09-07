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

package org.nimbustools.api.defaults.repr.vm;

import org.nimbustools.api._repr.vm._NIC;

public class DefaultNIC implements _NIC {

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private String name;
    private String acquisitionMethod;
    private String networkName;
    private String MAC;
    private String hostname;
    private String ipAddress;
    private String netmask;
    private String gateway;
    private String broadcast;
    private String network;
    
    
    // -------------------------------------------------------------------------
    // implements org.nimbustools.api.repr.vm.NIC
    // -------------------------------------------------------------------------

    public String getName() {
        return this.name;
    }

    public String getAcquisitionMethod() {
        return this.acquisitionMethod;
    }

    public String getNetworkName() {
        return this.networkName;
    }

    public String getMAC() {
        return this.MAC;
    }

    public String getHostname() {
        return this.hostname;
    }

    public String getIpAddress() {
        return this.ipAddress;
    }

    public String getNetmask() {
        return this.netmask;
    }

    public String getGateway() {
        return this.gateway;
    }

    public String getBroadcast() {
        return this.broadcast;
    }

    public String getNetwork() {
        return this.network;
    }
    

    // -------------------------------------------------------------------------
    // implements org.nimbustools.api._repr.vm._NIC
    // -------------------------------------------------------------------------

    public void setName(String name) {
        this.name = name;
    }

    public void setAcquisitionMethod(String method) {
        this.acquisitionMethod = method;
    }

    public void setNetworkName(String networkName) {
        this.networkName = networkName;
    }

    public void setMAC(String MAC) {
        this.MAC = MAC;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public void setBroadcast(String broadcast) {
        this.broadcast = broadcast;
    }

    public void setNetwork(String network) {
        this.network = network;
    }


    // -------------------------------------------------------------------------
    // DEBUG STRING
    // -------------------------------------------------------------------------

    public String toString() {
        return "DefaultNIC{" +
                "name='" + name + '\'' +
                ", acquisitionMethod='" + acquisitionMethod + '\'' +
                ", networkName='" + networkName + '\'' +
                ", MAC='" + MAC + '\'' +
                ", hostname='" + hostname + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", netmask='" + netmask + '\'' +
                ", gateway='" + gateway + '\'' +
                ", broadcast='" + broadcast + '\'' +
                ", network='" + network + '\'' +
                '}';
    }
}
