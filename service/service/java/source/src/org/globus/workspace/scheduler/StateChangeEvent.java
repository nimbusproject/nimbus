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
import org.globus.workspace.StateChangeInterested;
import org.globus.workspace.WorkspaceConstants;
import org.nimbustools.api.services.rm.ManageException;

/**
 * Notify state change interested about state changes
 */
public class StateChangeEvent implements TimerListener {

    private static final Log logger = LogFactory.getLog(StateChangeEvent.class.getName());

    private Integer id = null;
    private int[] ids = null;
    private final int state;
    private final StateChangeInterested interested;

    public StateChangeEvent(int[] ids, int state, StateChangeInterested interested) {
        this.ids = ids;
        this.state = state;
        this.interested = interested;
    }

    public StateChangeEvent(int id, int state, StateChangeInterested interested) {
        this.id = id;
        this.state = state;
        this.interested = interested;
    }    
    
    public void timerExpired(Timer timer) {
        if (this.interested == null) {
            logger.fatal("adapter is null");
            return;
        }

        if (this.ids == null && this.id == null) {
            logger.fatal("ids is null");
            return;
        }

        if (this.state == WorkspaceConstants.STATE_INVALID) {
            logger.fatal("state is invalid");
            return;
        }

        if(this.id != null){
            try {
                this.interested.stateNotification(id, state);
            } catch (ManageException e) {
                logger.fatal(e);
            }
        } else if(this.ids != null){
            this.interested.stateNotification(ids, state);
        }
        
    }
}
