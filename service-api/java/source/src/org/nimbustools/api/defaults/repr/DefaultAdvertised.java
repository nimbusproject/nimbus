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

package org.nimbustools.api.defaults.repr;

import org.nimbustools.api._repr._Advertised;

import java.util.Arrays;

public class DefaultAdvertised implements _Advertised {

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private int defaultRunningTimeSeconds;
    private int maximumRunningTimeSeconds;
    private int maximumAfterRunningTimeSeconds;
    private String[] cpuArchitectureNames;
    private String[] vmmVersions;
    private String vmm;
    private String[] networkNames;
    private int maxGroupSize;
    private int chargeGranularity;


    // -------------------------------------------------------------------------
    // implements org.nimbustools.api.repr.Advertised
    // -------------------------------------------------------------------------

    public int getDefaultRunningTimeSeconds() {
        return this.defaultRunningTimeSeconds;
    }

    public int getMaximumRunningTimeSeconds() {
        return this.maximumRunningTimeSeconds;
    }

    public int getMaximumAfterRunningTimeSeconds() {
        return this.maximumAfterRunningTimeSeconds;
    }

    public String[] getCpuArchitectureNames() {
        return this.cpuArchitectureNames;
    }

    public String[] getVmmVersions() {
        return this.vmmVersions;
    }

    public String getVmm() {
        return this.vmm;
    }

    public String[] getNetworkNames() {
        return this.networkNames;
    }

    public int getMaxGroupSize() {
        return this.maxGroupSize;
    }

    public int getChargeGranularity() {
        return this.chargeGranularity;
    }

    
    // -------------------------------------------------------------------------
    // implements org.nimbustools.api._repr.__Advertised
    // -------------------------------------------------------------------------

    public void setDefaultRunningTimeSeconds(int defaultRunningTime) {
        this.defaultRunningTimeSeconds = defaultRunningTime;
    }

    public void setMaximumRunningTimeSeconds(int maximumRunningTime) {
        this.maximumRunningTimeSeconds = maximumRunningTime;
    }

    public void setMaximumAfterRunningTimeSeconds(int maximumAfterRunningTime) {
        this.maximumAfterRunningTimeSeconds = maximumAfterRunningTime;
    }

    public void setCpuArchitectureNames(String[] cpuArchitectureNames) {
        this.cpuArchitectureNames = cpuArchitectureNames;
    }

    public void setVmmVersions(String[] vmmVersions) {
        this.vmmVersions = vmmVersions;
    }

    public void setVmm(String vmm) {
        this.vmm = vmm;
    }

    public void setNetworkNames(String[] networkNames) {
        this.networkNames = networkNames;
    }

    public void setMaxGroupSize(int maxGroupSize) {
        this.maxGroupSize = maxGroupSize;
    }

    public void setChargeGranularity(int chargeGranularity) {
        this.chargeGranularity = chargeGranularity;
    }

    // -------------------------------------------------------------------------
    // DEBUG STRING
    // -------------------------------------------------------------------------


    public String toString() {
        return "DefaultAdvertised{" +
                "defaultRunningTimeSeconds=" + defaultRunningTimeSeconds +
                ", maximumRunningTimeSeconds=" + maximumRunningTimeSeconds +
                ", maximumAfterRunningTimeSeconds=" +
                maximumAfterRunningTimeSeconds +
                ", cpuArchitectureNames='" +
                (cpuArchitectureNames  == null ? null : Arrays.asList(cpuArchitectureNames)) +
                ", vmmVersions=" +
                (vmmVersions == null ? null : Arrays.asList(vmmVersions)) +
                ", vmm='" + vmm + '\'' +
                ", networkNames=" +
                (networkNames == null ? null : Arrays.asList(networkNames)) +
                ", maxGroupSize=" + maxGroupSize +
                ", chargeGranularity=" + chargeGranularity +
                '}';
    }
}
