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

import org.nimbustools.api.services.rm.DoesNotExistException;
import org.nimbustools.api.services.rm.ManageException;
import org.nimbustools.api.services.rm.CreationException;
import edu.emory.mathcs.backport.java.util.concurrent.ExecutorService;

public interface WorkspaceHome {

    // -------------------------------------------------------------------------
    // INSTANCE LOCATOR
    // -------------------------------------------------------------------------

    public InstanceResource newInstance(int id) throws CreationException;

    /**
     * Find resource with VM id.
     * 
     * @param id vm id, must be positive
     * @return resource never null
     * @throws ManageException problem retrieving resource
     * @throws DoesNotExistException can not find resource with this id
     */
    public InstanceResource find(int id)

            throws ManageException, DoesNotExistException;

    public InstanceResource find(String id)

            throws ManageException, DoesNotExistException;

    public InstanceResource[] findByCaller(String callerID)

            throws ManageException;

    public InstanceResource[] findByIP(String ip)

            throws ManageException;

    public InstanceResource[] findAll()

            throws ManageException;

    public boolean isActiveWorkspaceID(int id)
            
            throws ManageException;

    public boolean isActiveWorkspaceID(String id)

            throws ManageException;

    public Sweepable[] currentSweeps();


    // -------------------------------------------------------------------------
    // DESTRUCTION
    // -------------------------------------------------------------------------

    public void destroy(int id)

            throws ManageException, DoesNotExistException;

    public void destroy(String id)

            throws ManageException, DoesNotExistException;

    /**
     * Destroy a set of workspaces.  Return a list of errors separated by \n,
     * including if the workspace was not found, etc.  This does not cut out
     * early if there is any kind of problem.
     *
     * @param workspaces list of workspace IDs
     * @param sourceStr string for log msgs
     * @return string report on what happened
     */
    public String destroyMultiple(int[] workspaces, String sourceStr);


    /**
     * Destroy a set of workspaces.  Return a list of errors separated by \n,
     * including if the workspace was not found, etc.  This does not cut out
     * early if there is any kind of problem.
     *
     * Allow parameter to set to block until work is complete (up to twenty seconds).
     *
     * @param workspaces list of workspace IDs
     * @param sourceStr string for log msgs
     * @param block set true if you want to block until complete (up to twenty seconds)
     * @return string report on what happened
     */
    public String destroyMultiple(int[] workspaces, String sourceStr, boolean block);


    // -------------------------------------------------------------------------
    // OTHER
    // -------------------------------------------------------------------------

    /**
     * @see org.nimbustools.api.services.rm.Manager#recover_initialize
     * @throws Exception problem
     */
    public void recover_initialize() throws Exception;

    public void shutdownImmediately();

    // todo: will go away when task system is more organized
    public ExecutorService getSharedExecutor();

    public int convertID(String id) throws ManageException;
    public String convertID(int id) throws ManageException;

    /* Human readable VMM insight */
    public String getVMMReport();

    public String[] getResourcePools();
}
