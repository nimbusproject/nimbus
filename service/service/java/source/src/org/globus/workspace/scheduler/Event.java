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

import commonj.timers.Timer;
import commonj.timers.TimerListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.workspace.WorkspaceConstants;
import org.nimbustools.api.services.rm.ManageException;

/**
 * Used to prevent scheduler from hijacking a thread.
 * Currently only used at workspace creation time.
 */
public class Event implements TimerListener {

    private static final Log logger = LogFactory.getLog(Event.class.getName());

    private final int[] ids;
    private final int state;
    private final Scheduler adapter;

    public Event(int[] ids, int state, Scheduler adapter) {
        this.ids = ids;
        this.state = state;
        this.adapter = adapter;
    }

    public void timerExpired(Timer timer) {
        if (this.adapter == null) {
            logger.fatal("adapter is null");
            return;
        }

        if (this.ids == null) {
            logger.fatal("ids is null");
            return;
        }

        if (this.state == WorkspaceConstants.STATE_INVALID) {
            logger.fatal("state is invalid");
            return;
        }

        for (int i = 0; i < this.ids.length; i++) {
            try {
                this.adapter.stateNotification(this.ids[i], this.state);
            } catch (ManageException e) {
                logger.fatal(e);
            }
        }
    }
}
