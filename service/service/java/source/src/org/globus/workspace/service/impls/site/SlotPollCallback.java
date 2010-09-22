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

package org.globus.workspace.service.impls.site;

import java.util.Calendar;

public interface SlotPollCallback extends NotificationPollCallback {

    /**
     * The pilot reports the slot has been successfully reserved and what
     * host it's ended up running on.  A timestamp is provided in case the
     * service was down at the time the notification was delivered.
     *
     * @param slotid uuid
     * @param hostname slot node
     * @param timestamp time of reservation
     */
    public void reserved(String slotid,
                         String hostname,
                         Calendar timestamp);

    /**
     * The pilot reports it started running but the slot was not successfully
     * reserved beacuse of some problem.
     *
     * @param slotid uuid
     * @param hostname slot node
     * @param error error message
     */
    public void errorReserving(String slotid,
                               String hostname,
                               String error);

    /**
     * The pilot reports that it has been interrupted and has determined
     * the signal was unexpected.  This can happen in three situations:
     *
     * 1. The LRM or administrator has decided to preempt the pilot for
     * whatever reason.
     *
     * 2. The node has been rebooted or shutdown.
     *
     * 3. The LRM job was cancelled by the slot manager.
     *
     * In each situation the pilot attempts to wait a specific (configurable)
     * ratio of the provided grace period.  In cases #1 and #2 this gives
     * the slot manager time to handle the problem (currently this involves
     * running shutdown-trash on all VMs in the slot).  In case #3 the slot
     * manager can just ignore this notification since it is already done
     * with the slot (which is why it cancelled the LRM job).
     *
     * @param slotid uuid
     * @param hostname slot node
     * @param timestamp the time that pilot sent this (second resolution only)
     */
    public void earlyUnreserving(String slotid,
                                 String hostname,
                                 Calendar timestamp);

    /**
     * The pilot reports that there was a problem early unreserving,
     * there is no action to take. An error message will usually accompany
     * this (for logging to service logs).
     *
     * @param slotid uuid
     * @param hostname slot node
     * @param error error message
     */
    public void errorEarlyUnreserving(String slotid,
                                      String hostname,
                                      String error);

    /**
     * The pilot reports that it has begun unreserving the slot, there is
     * nothing to be done now, this is the end (whether it passes or fails).
     * If there was something the manager was expected to do, earlyUnreserving
     * would have been called.
     *
     * @param slotid uuid
     * @param hostname slot node
     */
    public void unreserving(String slotid,
                            String hostname);

    /**
     * The pilot reports it has killed VMs.
     *
     * @param slotid uuid
     * @param hostname slot node
     * @param killed array of killed VM name
     */
    public void kills(String slotid,
                      String hostname,
                      String[] killed);

    /**
     * The pilot reports that it can not successfully unreserve the slot,
     * this is no action to take. An error message will usually accompany
     * this (for logging to service logs).
     *
     * @param slotid uuid
     * @param hostname slot node
     * @param error error message
     */
    public void errorUnreserving(String slotid,
                                 String hostname,
                                 String error);

}
