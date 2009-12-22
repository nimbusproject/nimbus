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

package org.globus.workspace.service;

import org.nimbustools.api.services.rm.ManageException;
import org.nimbustools.api.services.rm.DoesNotExistException;

public interface WorkspaceGroupHome {

    /**
     * @param creatorID owner
     * @return new group
     * @throws ManageException impl problem
     */
    public GroupResource newGroup(String creatorID)

            throws ManageException;

    /**
     * @param groupid uuid resource key
     * @return resource, never null
     * @throws ManageException impl problem
     * @throws DoesNotExistException unknown ID
     */
    public GroupResource find(String groupid)

            throws ManageException, DoesNotExistException;

    /**
     * @param groupid uuid resource key
     * @return resources, never null but might be length zero
     * @throws ManageException impl problem
     * @throws DoesNotExistException unknown ID
     */
    public InstanceResource[] findMembers(String groupid)
            
            throws ManageException, DoesNotExistException;


    /**
     * @param groupid uuid resource key
     * @return ids, never null but might be length zero
     * @throws ManageException impl problem
     * @throws DoesNotExistException unknown ID
     */
    public int[] findMemberIDs(String groupid)

            throws ManageException, DoesNotExistException;


    /**
     * Destroy all (still remaining) resources in the group
     * @param id groupid
     * @throws DoesNotExistException already gone
     * @throws ManageException error
     */
    public void destroy(String id)

            throws ManageException, DoesNotExistException;
}
