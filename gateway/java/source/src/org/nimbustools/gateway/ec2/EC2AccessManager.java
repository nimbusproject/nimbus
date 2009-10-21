package org.nimbustools.gateway.ec2;

import org.nimbustools.api.repr.Caller;
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

public interface EC2AccessManager {

    /**
     * Gets the appropriate access ID for a user
     * @param caller Calling user
     * @return valid EC2 access ID or null if none is valid
     * @throws EC2AccessException
     */
    public EC2AccessID getAccessID(Caller caller) throws EC2AccessException;

    public EC2AccessID getAccessIDByKey(String key) throws EC2AccessException;
}
