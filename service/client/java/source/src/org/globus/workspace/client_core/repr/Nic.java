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

package org.globus.workspace.client_core.repr;

import org.nimbustools.messaging.gt4_0.generated.metadata.logistics.Nic_Type;
import org.nimbustools.messaging.gt4_0.generated.metadata.logistics.IPConfig_Type;

/**
 * Serves as a way to begin encapsulating protocol/implementations but
 * also importantly serves as a copy... to use in group request per-instance
 * tracking for example.
 */
public class Nic {

    // Nic_Type
    private String name;
    private String mac;
    private String association;

    // IPConfig_Type
    private String hostname;
    private String ipAddress;
    private String netmask;
    private String gateway;
    private String broadcast;
    private String network;
    private NicAcquisitionMethod acquisitionMethod;

    public Nic() {
    }

    public Nic(Nic_Type xmlNic) {

        if (xmlNic == null) {
            throw new IllegalArgumentException("xmlNic is null");
        }

        final IPConfig_Type ipconf = xmlNic.getIpConfig();
        if (ipconf == null) {
            throw new IllegalArgumentException("IPConfig_Type is null?");
        }

        this.name = xmlNic.getName();
        this.mac = xmlNic.getMAC();
        this.association = xmlNic.getAssociation();

        this.hostname = ipconf.getHostname();
        this.ipAddress = ipconf.getIpAddress();
        this.netmask = ipconf.getNetmask();

        this.gateway = ipconf.getGateway();
        this.broadcast = ipconf.getBroadcast();
        this.network = ipconf.getNetwork();

        final String acqVal = ipconf.getAcquisitionMethod().getValue();
        if (!NicAcquisitionMethod.testValidMethod(acqVal)) {
            throw new IllegalArgumentException(
                    "acquisitionMethod is invalid? value: '" + acqVal + "'");
        }
        this.acquisitionMethod = new NicAcquisitionMethod(acqVal);
    }

    /* ******* */
    /* GET/SET */
    /* ******* */

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMac() {
        return this.mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getAssociation() {
        return this.association;
    }

    public void setAssociation(String association) {
        this.association = association;
    }

    public NicAcquisitionMethod getAcquisitionMethod() {
        return this.acquisitionMethod;
    }

    public void setAcquisitionMethod(NicAcquisitionMethod acquisitionMethod) {
        this.acquisitionMethod = acquisitionMethod;
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

    public String getNetmask() {
        return this.netmask;
    }

    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }

    public String getGateway() {
        return this.gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public String getBroadcast() {
        return this.broadcast;
    }

    public void setBroadcast(String broadcast) {
        this.broadcast = broadcast;
    }

    public String getNetwork() {
        return this.network;
    }

    public void setNetwork(String network) {
        this.network = network;
    }
}
