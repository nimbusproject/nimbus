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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.globus.workspace.service.binding.GlobalPolicies;

public class DefaultGlobalPolicies implements GlobalPolicies {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(DefaultGlobalPolicies.class.getName());
    

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private int defaultRunningTimeSeconds;
    private int maximumRunningTimeSeconds;
    private int terminationOffsetSeconds;
    private String cpuArchitectureName;
    private String[] vmmVersions;
    private String vmm;

    // if 0 or negative, unlimited is OK
    private int maximumGroupSize;

    private boolean propagateEnabled;
    private boolean unpropagateEnabled;
    private boolean unpropagateAfterRunningTimeEnabled;
    private boolean allowStaticIPs;
    private boolean fake;
    private long fakelag;


    // -------------------------------------------------------------------------
    // GET/SET
    // -------------------------------------------------------------------------

    public int getDefaultRunningTimeSeconds() {
        return this.defaultRunningTimeSeconds;
    }

    public void setDefaultRunningTimeSeconds(int defaultRunningTimeSeconds) {
        this.defaultRunningTimeSeconds = defaultRunningTimeSeconds;
    }

    public int getMaximumRunningTimeSeconds() {
        return this.maximumRunningTimeSeconds;
    }

    public void setMaximumRunningTimeSeconds(int maximumRunningTimeSeconds) {
        this.maximumRunningTimeSeconds = maximumRunningTimeSeconds;
    }

    public int getTerminationOffsetSeconds() {
        return this.terminationOffsetSeconds;
    }

    public void setTerminationOffsetSeconds(int terminationOffsetSeconds) {
        this.terminationOffsetSeconds = terminationOffsetSeconds;
    }

    public String getCpuArchitectureName() {
        return this.cpuArchitectureName;
    }

    public void setCpuArchitectureName(String cpuArchitectureName) {
        this.cpuArchitectureName = cpuArchitectureName;
    }

    public String[] getVmmVersions() {
        return this.vmmVersions;
    }

    public void setVmmVersions(String[] vmmVersions) {
        this.vmmVersions = vmmVersions;
    }

    public String getVmm() {
        return this.vmm;
    }

    public void setVmm(String vmm) {
        this.vmm = vmm;
    }

    public int getMaximumGroupSize() {
        return this.maximumGroupSize;
    }

    public void setMaximumGroupSize(int maximumGroupSize) {
        this.maximumGroupSize = maximumGroupSize;
    }

    public boolean isPropagateEnabled() {
        return this.propagateEnabled;
    }

    public void setPropagateEnabled(boolean propagateEnabled) {
        this.propagateEnabled = propagateEnabled;
    }

    public boolean isUnpropagateEnabled() {
        return this.unpropagateEnabled;
    }

    public void setUnpropagateEnabled(boolean unpropagateEnabled) {
        this.unpropagateEnabled = unpropagateEnabled;
    }

    public boolean isUnpropagateAfterRunningTimeEnabled() {
        return this.unpropagateAfterRunningTimeEnabled;
    }

    public void setUnpropagateAfterRunningTimeEnabled(
            boolean unpropagateAfterRunningTimeEnabled) {
        this.unpropagateAfterRunningTimeEnabled =
                unpropagateAfterRunningTimeEnabled;
    }

    public boolean isFake() {
        return this.fake;
    }

    public void setFake(boolean fake) {
        this.fake = fake;
    }

    public boolean isAllowStaticIPs() {
        return this.allowStaticIPs;
    }

    public void setAllowStaticIPs(boolean allowStaticIPs) {
        this.allowStaticIPs = allowStaticIPs;
    }

    public long getFakelag() {
        return this.fakelag;
    }

    public void setFakelag(long fakelag) {
        this.fakelag = fakelag;
    }

    
    // -------------------------------------------------------------------------
    // IoC INIT METHOD
    // -------------------------------------------------------------------------

    public synchronized void validate() throws Exception {

        if (this.fake) {

            // The greatest trick that Xen played was convincing the world
            // it didn't exist
            logger.info("\n\n   ***************************" +
                      "\n     VIRTUALIZATION x2 MODE" +
                      "\n   ***************************" +
                      "\n\nFake tasks will take: " + this.fakelag + " ms.\n");
        }
    }
}
