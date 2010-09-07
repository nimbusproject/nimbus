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

import commonj.timers.TimerManager;
import org.globus.workspace.Lager;

public class PilotPoll extends NotificationPoll {

    private final SlotPollCallback slotcall;

    public PilotPoll(TimerManager timerManager,
                     Lager lagerImpl,
                     long delay,
                     String verifiedPath,
                     long filepos,
                     SlotPollCallback call) throws Exception {
        
        super(timerManager, lagerImpl, delay, verifiedPath, filepos, call, "pilot");
        this.slotcall = call;
    }

    // sample reserved line from pilot with hostname report
    // $UUID+++hostname::pilot-reserved:0

    // sample succeess line from pilot:
    // $UUID+++hostname::pilot-unreserved::0

    // sample early unreserving line from pilot
    // $UUID+++hostname::pilot-earlyunreserving::0::timestamp
    // timestamp: YYYY-MM-DD-HH-MM-SS  (always UTC/GMT)
    // (UTC/GMT differences are insignificant for our purposes)

    // sample reserved error line from pilot:
    // $UUID+++hostname::pilot-reserved::1::Some::error::message

    // sample error line from pilot:
    // $UUID+++hostname::pilot-unreserved::1::Some::error::message

    // sample killed line from pilot:
    // $UUID+++hostname::pilot-killed::1::workspace-2,workspace-13,workspace-3

    // state values from workspace-pilot: with 0 or !0 error code:
    //   pilot-test (ignored, this is for pilot's evaluate mode)
    //   pilot-reserved
    //   pilot-unreserving
    //   pilot-earlyunreserving

    // state values from workspace-pilot: error code ignored
    //   pilot-killed

    protected boolean oneNotification(String name,
                                      String state,
                                      int code,
                                      String message) {

        String log_msg = "pilot file-based notification read, name = '" +
                         name + "', state = '" + state + "', " +
                         "code = " + code;

        if (message != null) {
            log_msg += ", message = " + message;
        } else {
            log_msg += ", no message";
        }

        if (lager.pollLog) {
            logger.trace(log_msg);
        }
        
        if (this.slotcall == null) {
            logger.error("received pilot notification but " +
                  "no SlotPollCallback is configured: " + log_msg);
            return false;
        }

        try {
            return PilotNotificationUtil.
                                    oneNotification(name, state, code, message,
                                            log_msg, this.slotcall);
        } catch (Exception e) {
            logger.error(e.getMessage());
            return false;
        }
    }
}
