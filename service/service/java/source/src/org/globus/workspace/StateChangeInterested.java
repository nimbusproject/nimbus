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

package org.globus.workspace;

import org.nimbustools.api.services.rm.ManageException;

public interface StateChangeInterested {

    /**
     * This notification allows the scheduler to be autonomous
     * from the service layer's actions if it wants to be (instead
     * of allowing the resource states to progress, it could time
     * every state transition by continually re-adjusting the
     * resource's target state when it is time to transition it).
     *
     * The first state notification (always preceded by a call to
     * schedule) signals that the scheduler may act.  This allows
     * the service layer to finalize creation before the scheduler
     * acts on a a resouce.
     *
     * @param vmid id
     * @param state STATE_* in WorkspaceConstants
     * @throws ManageException problem
     */
    public void stateNotification(int vmid, int state)

            throws ManageException;
    
    /**
     * Batch state notification
     * NOTE: This version doesn't throw exception when
     * an error occurs during the notification. If error conditions need to be
     * treated, use {@code stateNotification(int vmid, int state)}
     * instead. However, implementations of this interface
     * are recommended to log errors.
     * @param vmids
     * @param state
     */
    public void stateNotification(int[] vmids, int state);    
    
}
