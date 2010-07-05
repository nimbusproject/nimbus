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

package org.globus.workspace.network;

public class AssociationEntry {

    private String hostname;
    private String ipAddress;
    private String mac;
    private String broadcast;
    private String subnetMask;
    private String gateway;
    private boolean inUse;
    private boolean explicitMac;

    public AssociationEntry(String ipAddress,
                            String macAddress,
                            String hostname,
                            String gateway,
                            String broadcast,
                            String subnetMask) {
        
        if (ipAddress != null) {
            this.ipAddress = ipAddress.trim();
        }
        if (macAddress != null) {
            this.mac = macAddress.trim();
        }
        if (hostname != null) {
            this.hostname = hostname.trim();
        }
        if (gateway != null) {
            this.gateway = gateway.trim();
        }
        if (broadcast != null) {
            this.broadcast = broadcast.trim();
        }
        if (subnetMask != null) {
            this.subnetMask = subnetMask.trim();
        }
    }

    public boolean isInUse() {
        return this.inUse;
    }

    public void setInUse(boolean inUse) {
        this.inUse = inUse;
    }

    public String getHostname() {
        return this.hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getIpAddress() {
        return this.ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getMac() {
        return this.mac;
    }

    public void setMac(String macAddress) {
        this.mac = macAddress;
    }

    public String getBroadcast() {
        return this.broadcast;
    }

    public void setBroadcast(String broadcast) {
        this.broadcast = broadcast;
    }

    public String getSubnetMask() {
        return this.subnetMask;
    }

    public void setSubnetMask(String subnetMask) {
        this.subnetMask = subnetMask;
    }

    public String getGateway() {
        return this.gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public boolean isExplicitMac() {
        return explicitMac;
    }

    public void setExplicitMac(boolean explicitMac) {
        this.explicitMac = explicitMac;
    }

    public String toString() {
        return "\nEntry{" +
                "hostname='" + this.hostname + '\'' +
                ", ipAddress='" + this.ipAddress + '\'' +
                ", mac='" + this.mac + '\'' +
                (this.explicitMac ? "(explicit)" : "") +
                ", gateway='" + this.gateway + '\'' +
                ", broadcast='" + this.broadcast + '\'' +
                ", subnetMask='" + this.subnetMask + '\'' +
                ", inUse=" + this.inUse +
                "}";
    }
}
