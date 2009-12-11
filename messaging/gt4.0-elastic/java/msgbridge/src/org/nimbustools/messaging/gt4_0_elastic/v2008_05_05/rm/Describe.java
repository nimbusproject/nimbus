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

package org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.rm;

import org.nimbustools.api.repr.CannotTranslateException;
import org.nimbustools.api.repr.vm.VM;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.DescribeInstancesResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.DescribeInstancesType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.PlacementResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.InstanceStateType;

import java.util.Calendar;

public interface Describe {
    
    public DescribeInstancesResponseType translate(VM[] vms,
                                                   String[] instanceIDs,
                                                   String ownerID)
            throws CannotTranslateException;

    public String[] findQueryIDs(DescribeInstancesType
                                        describeInstancesRequestMsg)             
            throws CannotTranslateException;

    public InstanceStateType getState(VM vm) throws CannotTranslateException;

    public String getReason(VM vm) throws CannotTranslateException;
    
    public String getImageID(VM vm) throws CannotTranslateException;

    public String getInstanceType(VM vm) throws CannotTranslateException;

    public PlacementResponseType getPlacement();

    public Calendar getLaunchTime(VM vm) throws CannotTranslateException;
}
