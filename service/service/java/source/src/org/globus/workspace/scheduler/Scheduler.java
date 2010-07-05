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

package org.globus.workspace.scheduler;

import org.globus.workspace.StateChangeInterested;
import org.nimbustools.api.services.rm.ResourceRequestDeniedException;
import org.nimbustools.api.services.rm.SchedulingException;
import org.nimbustools.api.services.rm.ManageException;

import java.util.Calendar;

/**
 * TODO: move termination time/unpropagation scheduling into this module 
 */
public interface Scheduler extends StateChangeInterested{

    /* See WorkspaceResource interface for some of the things we
       expect from scheduler -> service interactions */

    /**
     * For multiple VMs, can only handle homogenous requests in this
     * atomic method.
     *
     * @see #proceedCoschedule for handling separate requests together 
     *
     * @param memory MB needed
     * @param duration seconds needed
     * @param neededAssociations networks needed
     * @param numNodes number needed
     * @param groupid group ID, can be null
     * @param coschedid co-scheduling ID, can be null
     * @param creatorDN creator's identifying DN, can be null
     * @return reservation never null
     * @throws ResourceRequestDeniedException will not grant
     * @throws SchedulingException internal problem
     */
    public Reservation schedule(int memory,
                                int duration,
                                String[] neededAssociations,
                                int numNodes,
                                String groupid,
                                String coschedid,
                                boolean preemptable,
                                String creatorDN)
                
            throws SchedulingException,
                   ResourceRequestDeniedException;

    /**
     * If one to many requests were successfully sent to the schedule
     * method using the coschedid parameter, this method signals to
     * proceed to schedule that group of requests.
     *
     * @see #schedule 
     *
     * @param coschedid co-scheduling ID
     * @throws SchedulingException internal problem
     * @throws ResourceRequestDeniedException can not grant
     */
    public void proceedCoschedule(String coschedid)

                throws SchedulingException,
                       ResourceRequestDeniedException;
    
    /**
     * Notification that the service is in recovery mode.
     * This signature may need to change.
     *
     * @param recovered number of VMs recovered
     */
    public void recover(int recovered);


    /**
     * If scheduler implements the slot setup, this is called by the slot
     * manager module when space has been reserved for a workspace.  If the
     * slot manager is best effort the scheduler will not invoke anything
     * until it hears this notification.
     *
     * Stop is necessary for scheduler, start and hostname are passed in
     * for convenience, better model is to adjust the resource in the slot
     * manager with the new information, but scheduler needs to retrieve
     * the resource anyhow.
     * TODO: refactor in future when there are more schedulers and slot mgrs
     *
     * @param vmid id
     * @param start time slot started
     * @param stop time to shut down
     * @param hostname node VM is running on
     * @throws ManageException exc
     */
    public void slotReserved(int vmid,
                             Calendar start,
                             Calendar stop,
                             String hostname) throws ManageException; 
    

    /**
     * Used just in backout situations, when request did not reach STATE_FIRST_LEGAL
     * NOTE: This is to be used instead of scheduler.stateNotification(id, WorkspaceConstants.STATE_DESTROYING),
     * when the request did not reach the first legal state
     * @param vmid id
     * @throws ManageException
     */
    public void removeScheduling(int vmid)

            throws ManageException;        
}
