/*
 * Copyright 1999-2010 University of Chicago
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

package org.globus.workspace.scheduler.defaults;

public class ResourcepoolEntry {

    private String resourcePool;
    private String hostname;
    private int memMax = -1; // in MBytes
    private int memCurrent = -1; // in MBytes
    private int memPreemptable = -1; // in MBytes    
    private String supportedAssociations;

    public ResourcepoolEntry(String resourcePool, String hostname, int memMax,
                             int memCurrent, int memPreemptable, String sa) {
        this.resourcePool = resourcePool;
        this.hostname = hostname;
        this.memMax = memMax;
        this.memCurrent = memCurrent;
        this.memPreemptable = memPreemptable;
        this.supportedAssociations = sa;
    }

    public String getHostname() {
        return this.hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public int getMemMax() {
        return this.memMax;
    }

    public void setMemMax(int memMax) {
        this.memMax = memMax;
    }

    public void setMemPreemptable(int memPreemptable) {
        this.memPreemptable = memPreemptable;
    }

    public int getMemPreemptable() {
        return memPreemptable;
    }    
    
    public int getMemCurrent() {
        return this.memCurrent;
    }

    public void setMemCurrent(int memCurrent) {
        this.memCurrent = memCurrent;
    }

    public void addMemCurrent(int add) {
        this.memCurrent += add;
    }
    
    public void addMemPreemptable(int add) {
        this.memPreemptable += add;
    }    

    public int percentEmpty() {
        if (this.memCurrent == 0) {
            return 0;
        }
        if (this.memCurrent == this.memMax) {
            return 100;
        }
        if (this.memCurrent > this.memMax) {
            throw new IllegalStateException("current memory is higher than max memory");
        }

        final double div = ((double)this.memCurrent / (double)this.memMax);
        final double percentage = div * 100.0;
        if (percentage < 1.0 && percentage > 0.0) {
            return 1;
        }
        return (int) percentage;
    }

    public String getSupportedAssociations() {
        return this.supportedAssociations;
    }

    public void setSupportedAssociations(String supportedAssociations) {
        this.supportedAssociations = supportedAssociations;
    }

    public String toString() {
        return "ResourcepoolEntry{" +
                "hostname='" + this.hostname + '\'' +
                ", memMax=" + this.memMax +
                ", memPreemptable=" + this.getMemPreemptable() +
                ", memCurrent=" + this.memCurrent +
                ", supportedNetworks='" + this.supportedAssociations +
                ", percentEmpty= " + this.percentEmpty() + '\'' +
                '}';
    }
    
    public void setResourcePool(String resourcePool) {
        this.resourcePool = resourcePool;
    }

    public String getResourcePool() {
        return resourcePool;
    }

}
