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

/**
 * Entity that manages workspaces
 * that use pre-emptable space
 * 
 * A pre-emptable-space-enabled {@link SlotManagement} 
 * will normally contain one associated 
 * {@link PreemptableSpaceManager} in order to
 * release space in emergency situations (ie. there
 * is no sufficient memory for a non-preemptable
 * reservation)
 * 
 */
public interface PreemptableSpaceManager {

    /**
     * Releases the needed amount of space from
     * pre-emptables reservations.
     * 
     * This method is called when the {@link SlotManagement} 
     * doesn't find sufficient space for a non-preemptable 
     * reservation, so it tries to fulfill that request with 
     * space currently allocated to a non-preemptable workspace.
     * 
     * This method should block until the process of
     * releasing the needed memory is completed, what
     * means that who is calling might assume that
     * the needed space is already released by the end
     * of this method's execution.
     * 
     * @param memoryToRelease the minimum amount
     * of memory that should be released from
     * pre-emptable reservations. In case this value
     * is higher than the amount of space currently
     * managed by this {@link PreemptableSpaceManager},
     * all the pre-emptable space currently allocated 
     * must be released.
     * 
     */
    public void releaseSpace(Integer memoryToRelease);

    /**
     * Notification that a change to the resource pool DB
     * has been made. 
     */
    public void recalculateAvailableInstances();
    
    /**
     * Initalizes this {@link PreemptableSpaceManager}.
     * 
     * This method is called after the associated
     * {@link SlotManagement} was initialized, indicating
     * that the resource pool DB was already populated and
     * can be queried
     */
    public void init();
    
}
