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

package org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.general.defaults;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.general.ResourceAllocations;
import org.nimbustools.api.repr.vm.ResourceAllocation;
import org.nimbustools.api.repr.vm.RequiredVMM;
import org.nimbustools.api.repr.CannotTranslateException;
import org.nimbustools.api.repr.ReprFactory;
import org.nimbustools.api._repr.vm._ResourceAllocation;
import org.nimbustools.api._repr.vm._RequiredVMM;
import org.nimbustools.api.brain.ModuleLocator;

/**
 * Goldilocks RAs
 */
public class DefaultResourceAllocations implements ResourceAllocations {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(DefaultResourceAllocations.class.getName());
    

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected final ReprFactory repr;
    
    protected int smallMemory;
    protected int largeMemory;
    protected int xlargeMemory;

    protected int smallCPUs;
    protected int largeCPUs;
    protected int xlargeCPUs;

    protected String smallName;
    protected String largeName;
    protected String xlargeName;

    protected String unknownString;

    protected String cpuArch;
    protected String vmmType;
    protected String vmmVersion;

    protected RequiredVMM requestThisVMM;
    
    protected String siType;


    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    public DefaultResourceAllocations(ModuleLocator locator) throws Exception {
        this.repr = locator.getReprFactory();
    }

    // -------------------------------------------------------------------------
    // IoC INIT METHOD
    // -------------------------------------------------------------------------

    void validate() throws Exception {

        if (this.smallName == null || this.smallName.trim().length() == 0) {
            throw new Exception("Invalid: Missing small RA name");
        }
        if (this.largeName == null || this.largeName.trim().length() == 0) {
            throw new Exception("Invalid: Missing large RA name");
        }
        if (this.xlargeName == null || this.xlargeName.trim().length() == 0) {
            throw new Exception("Invalid: Missing x-large RA name");
        }
        if (this.unknownString == null || this.unknownString.trim().length() == 0) {
            throw new Exception("Invalid: Missing 'unknown' RA string");
        }

        if (this.smallMemory < 1) {
            throw new Exception("Invalid: Small RA memory is zero " +
                    "or negative: " + this.smallMemory);
        }
        if (this.largeMemory < 1) {
            throw new Exception("Invalid: Large RA memory is zero " +
                    "or negative: " + this.largeMemory);
        }
        if (this.xlargeMemory < 1) {
            throw new Exception("Invalid: Extra-large RA memory is zero " +
                    "or negative: " + this.xlargeMemory);
        }

        if (this.smallCPUs < 1) {
            throw new Exception("Invalid: Small RA CPUs is zero " +
                    "or negative: " + this.smallMemory);
        }
        if (this.largeCPUs < 1) {
            throw new Exception("Invalid: Large RA CPUs is zero " +
                    "or negative: " + this.largeMemory);
        }
        if (this.xlargeCPUs < 1) {
            throw new Exception("Invalid: Extra-large RA CPUs is zero " +
                    "or negative: " + this.xlargeMemory);
        }

        if (this.vmmType == null || this.vmmType.trim().length() == 0) {
            logger.warn("No VMM type configured to send in requests?");
        }

        if (this.vmmVersion == null || this.vmmVersion.trim().length() == 0) {
            logger.warn("No VMM version configured to send in requests?");
        }

        if (this.cpuArch == null || this.cpuArch.trim().length() == 0) {
            logger.warn("No CPU arch configured to send in requests?");
        }

        final _RequiredVMM vmm = this.repr._newRequiredVMM();
        vmm.setType(this.vmmType);
        final String[] versions = {this.vmmVersion};
        vmm.setVersions(versions);
        this.requestThisVMM = vmm;
    }


    // -------------------------------------------------------------------------
    // SET
    // -------------------------------------------------------------------------

    public void setSmallMemory(int smallMemory) {
        this.smallMemory = smallMemory;
    }

    public void setLargeMemory(int largeMemory) {
        this.largeMemory = largeMemory;
    }

    public void setXlargeMemory(int xlargeMemory) {
        this.xlargeMemory = xlargeMemory;
    }

    public void setSmallCPUs(int smallCPUs) {
        this.smallCPUs = smallCPUs;
    }

    public void setLargeCPUs(int largeCPUs) {
        this.largeCPUs = largeCPUs;
    }

    public void setXlargeCPUs(int xlargeCPUs) {
        this.xlargeCPUs = xlargeCPUs;
    }

    public void setSmallName(String smallName) {
        this.smallName = smallName;
    }

    public void setLargeName(String largeName) {
        this.largeName = largeName;
    }

    public void setXlargeName(String xlargeName) {
        this.xlargeName = xlargeName;
    }

    public void setUnknownString(String unknownString) {
        this.unknownString = unknownString;
    }

    public void setCpuArch(String cpuArch) {
        this.cpuArch = cpuArch;
    }

    public void setVmmType(String vmmType) {
        this.vmmType = vmmType;
    }

    public void setVmmVersion(String vmmVersion) {
        this.vmmVersion = vmmVersion;
    }
    
    public void setSiType(String siType) throws Exception {
        if(siType.equalsIgnoreCase("small")){
            this.siType = this.getSmallName();
        } else if(siType.equalsIgnoreCase("large")){
            this.siType = this.getLargeName();
        } else if(siType.equalsIgnoreCase("xlarge")){
            this.siType = this.getXlargeName();
        } else {
            throw new Exception("Invalid SI type in spotinstances configuration file. " +
            		            "Valid values are: small, large or xlarge");
        }
    }    

    // -------------------------------------------------------------------------
    // implements ResourceAllocations
    // -------------------------------------------------------------------------

    public String getSpotInstanceType(){
        return this.siType;
    }
    
    public String getSmallName() {
        return this.smallName;
    }

    public String getLargeName() {
        return this.largeName;
    }

    public String getXlargeName() {
        return this.xlargeName;
    }

    public String getCpuArch() {
        return this.cpuArch;
    }

    public String getVmmType() {
        return this.vmmType;
    }

    public String getVmmVersion() {
        return this.vmmVersion;
    }

    protected String getMatchingName(int memory) {
        if (this.smallMemory == memory) {
            return this.getSmallName();
        } else if (this.largeMemory == memory) {
            return this.getLargeName();
        } else if (this.xlargeMemory == memory) {
            return this.getXlargeName();
        } else {
            return this.unknownString;
        }
    }

    public String getMatchingName(ResourceAllocation ra)
            throws CannotTranslateException {

        if (ra == null) {
            throw new CannotTranslateException("RA is missing");
        }

        // only based on memory at the moment
        return this.getMatchingName(ra.getMemory());
    }

    public ResourceAllocation getMatchingRA(String name,
                                            int minNumNodes,
                                            int maxNumNodes,
                                            boolean spot)
            throws CannotTranslateException {

        final String cmpName;
        if (name == null) {
            // null means "use default"
            cmpName = this.getSmallName();
        } else {
            cmpName = name;
        }

        final _ResourceAllocation ra = this.repr._newResourceAllocation();
        ra.setNodeNumber(minNumNodes); // only respecting min at the moment

        if(spot && !cmpName.equals(siType)){
            throw new CannotTranslateException(
                    "Unsupported spot instance type: '" + name + "'." +
                    		" Currently supported SI type: " + siType);            
        }
        
        Integer memory = getInstanceMemory(cmpName);
        
        ra.setMemory(memory);

        Integer cpus = getInstanceCPUs(cmpName);

        ra.setIndCpuCount(cpus);

        ra.setSpotInstance(spot);
        
        ra.setArchitecture(this.cpuArch);

        return ra;
    }

    protected Integer getInstanceMemory(final String cmpName)
            throws CannotTranslateException {
        if (cmpName.equals(this.getSmallName())) {
            return this.smallMemory;
        } else if (cmpName.equals(this.getLargeName())) {
            return this.largeMemory;
        } else if (cmpName.equals(this.getXlargeName())) {
            return this.xlargeMemory;
        } else {
            throw new CannotTranslateException(
                    "Unknown instance type '" + cmpName + "'");
        }
    }

    protected Integer getInstanceCPUs(final String cmpName)
            throws CannotTranslateException {
        if (cmpName.equals(this.getSmallName())) {
            return this.smallCPUs;
        } else if (cmpName.equals(this.getLargeName())) {
            return this.largeCPUs;
        } else if (cmpName.equals(this.getXlargeName())) {
            return this.xlargeCPUs;
        } else {
            throw new CannotTranslateException(
                    "Unknown instance type '" + cmpName + "'");
        }
    }

    public RequiredVMM getRequiredVMM() {
        return this.requestThisVMM;
    }
}
