/*
 * Copyright 1999-2010 University of Chicago
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

package org.globus.workspace.scheduler.defaults;

import org.globus.workspace.scheduler.Reservation;
import org.globus.workspace.scheduler.Scheduler;
import org.nimbustools.api.services.rm.ResourceRequestDeniedException;
import org.nimbustools.api.services.rm.ManageException;

/**
 * Will change.
 *
 * In the future this interface is planned to support more features, e.g.
 * resource requirement ranges (min/max duration, memory, etc.) and specific
 * start/stop ranges (AR).
 */
public interface SlotManagement {

    /**
     * @param request a single workspace or homogenous group-workspace request
     * @return Reservation res
     * @throws ResourceRequestDeniedException exc
     */
    public Reservation reserveSpace(NodeRequest request)
                  throws ResourceRequestDeniedException;

    /**
     * @param requests an array of single workspace or homogenous
     *                 group-workspace requests
     * @param coschedid coscheduling (ensemble) ID
     * @return Reservation res
     * @throws ResourceRequestDeniedException exc
     */
    public Reservation reserveCoscheduledSpace(NodeRequest[] requests,
                                               String coschedid)
                  throws ResourceRequestDeniedException;

    /**
     * Only handling per VM release now.
     *
     * @param vmid vmid
     * @throws ManageException exc
     */
    public void releaseSpace(int vmid) throws ManageException;

    /**
     * @return true if implementation can support coscheduling
     */
    public boolean canCoSchedule();

    /**
     * @return true if implementation is running in best effort mode
     */
    public boolean isBestEffort();

    /**
     * Strict evacuation means that the scheduler should not allow any
     * time consuming action on the slot after the running duration expires
     * (actions such as unpropagate).
     *
     * @return true if implementation requires strict evacuation
     */
    public boolean isEvacuationStrict();

    // see DefaultSchedulerAdapter#initialize()
    public void setScheduler(Scheduler adapter);

    /**
     * An implementation may ignore the neededAssociations parameter of
     * the reserveSpace method.  This method enables the caller to find
     * out if it does.
     *
     * @return true if neededAssociations is supported
     */
    public boolean isNeededAssociationsSupported();
}
