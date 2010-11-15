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

package org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.general;

import org.nimbustools.api.repr.vm.ResourceAllocation;
import org.nimbustools.api.repr.vm.RequiredVMM;
import org.nimbustools.api.repr.CannotTranslateException;

public interface ResourceAllocations {

    public String getSmallName();
    public String getLargeName();
    public String getXlargeName();

    public String getCpuArch();
    public String getVmmType();
    public String getVmmVersion();

    /**
     * @param ra ra used
     * @return name to use
     * @throws CannotTranslateException problem
     */
    public String getMatchingName(ResourceAllocation ra)
            throws CannotTranslateException;

    /**
     * @param name elastic instance type name, if null it means request default
     * @param minNumNodes min #
     * @param maxNumNodes max #
     * @return ra to request
     * @throws CannotTranslateException problem
     */
    public ResourceAllocation getMatchingRA(String name,
                                            int minNumNodes,
                                            int maxNumNodes,
                                            boolean spot)
            throws CannotTranslateException;

    /**
     * @return vmm to request
     */
    public RequiredVMM getRequiredVMM();
    
    public String getSpotInstanceType();
}
