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

import org.nimbustools.api.services.rm.ResourceRequestDeniedException;
import org.nimbustools.api.services.rm.ManageException;

public interface AssociationAdapter {

    /**
     * @return all configured names, never null (can be length zero)
     * @throws ManageException problem retrieving names
     */
    public String[] getAssociationNames() throws ManageException;


    /**
     * @param name requested association name
     *             if null, considered to be default if configured
     *
     * @param vmid for logging
     * 
     * @return Object[] length 2
     *             AssociationEntry [0]
     *             String dns setting [1]
     *
     * @throws ResourceRequestDeniedException not available
     */
    public Object[] getNextEntry(String name, int vmid)

            throws ResourceRequestDeniedException;

    /**
     * @param name association, can not be null
     * @param ipAddress IP, can not be null
     * @param trackingID for logs
     * @throws ManageException problem
     */
    public void retireEntry(String name, String ipAddress, int trackingID)

            throws ManageException;

    /**
     * @return new, unique MAC address; NULL if adapter is not configured to
     *         assign MAC addresses
     * @throws ResourceRequestDeniedException thrown if adapter is
     *         configured to assign MAC addrs but no unique MACs are available
     */
    public String newMAC()
            
            throws ResourceRequestDeniedException;
    

}
