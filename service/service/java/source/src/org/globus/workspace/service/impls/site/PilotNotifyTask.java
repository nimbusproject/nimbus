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

import edu.emory.mathcs.backport.java.util.concurrent.Callable;

/**
 * Callable async task wrapper for PilotNotificationUtil.oneNotification()
 * 
 * @see PilotNotificationUtil#oneNotification(String, String, int, String, String, SlotPollCallback) 
 */
public class PilotNotifyTask implements Callable {

    
    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private final String name;
    private final String state;
    private final int code;
    private final String message;
    private final String log_msg;
    private final SlotPollCallback slotcall;


    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    public PilotNotifyTask(String name, String state, int code, String message,
                           String log_msg, SlotPollCallback slotcall) {
        this.name = name;
        this.state = state;
        this.code = code;
        this.message = message;
        this.log_msg = log_msg;
        this.slotcall = slotcall;
    }

    
    // -------------------------------------------------------------------------
    // implements Callable
    // -------------------------------------------------------------------------

    /**
     * @return Boolean result of PilotNotificationUtil.oneNotification
     * @throws Exception
     */
    public Object call() throws Exception {

        final boolean result =
                PilotNotificationUtil.oneNotification(this.name,
                                                      this.state,
                                                      this.code,
                                                      this.message,
                                                      this.log_msg,
                                                      this.slotcall);
        return Boolean.valueOf(result);
    }
}
