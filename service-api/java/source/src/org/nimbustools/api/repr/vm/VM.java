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

package org.nimbustools.api.repr.vm;

import org.nimbustools.api.repr.Caller;

public interface VM {

    public String getID();
    public String getGroupID();
    public String getCoschedID();
    public NIC[] getNics();
    public VMFile[] getVMFiles();
    public ResourceAllocation getResourceAllocation();
    public Schedule getSchedule();
    public State getState();
    public Caller getCreator();
    public int getLaunchIndex();
    public String getMdUserData();
    public String getSshKeyName();
    
    public String getLifeCycle();
    public String getSpotInstanceRequestID();

    public String getDetails();

    String getClientToken();
}
