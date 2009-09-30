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

import commonj.timers.Timer;
import commonj.timers.TimerListener;
import commonj.timers.TimerManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.workspace.persistence.PersistenceAdapter;

/**
 * Delayed database persistence of last cursor.
 */
public class CursorPersistence implements TimerListener {

    private static final Log logger =
        LogFactory.getLog(CursorPersistence.class.getName());

    private final PersistenceAdapter persistence;
    private final long timerDelay;

    private Timer timer;
    private TimerManager timerMgr;
    private long currentPosition;

    /* ***** */
    /* SETUP */
    /* ***** */

    CursorPersistence(TimerManager timerManager,
                      PersistenceAdapter database,
                      long delayms) {

        if (database == null) {
            throw new IllegalArgumentException("database may not be null");
        }
        this.persistence = database;

        if (delayms < 1) {
            throw new IllegalArgumentException("delay must be greater than 0");
        }
        this.timerDelay = delayms;
        
        if (timerManager == null) {
            throw new IllegalArgumentException("timerManager may not be null");
        }
        this.timerMgr = timerManager;
    }

    /* ***** */
    /* USAGE */
    /* ***** */

    void cursorPosition(long position) {
        this.updateOrPersist(false, position);
    }

    /* **** */
    /* IMPL */
    /* **** */

    public void timerExpired(Timer aTimer) {
        this.updateOrPersist(true, -1);
    }

    private synchronized void updateOrPersist(boolean persist,
                                              long position) {

        if (persist) {

            // ignore position parameter if persist == true

            try {
                this.persistence.updateCursorPosition(this.currentPosition);
                this.timer = null;

            } catch (Throwable t) {
                if (logger.isDebugEnabled()) {
                    logger.error(t.getMessage(), t);
                } else {
                    logger.error(t.getMessage());
                }
            }

        } else {

            this.currentPosition = position;
            this.schedule();
        }
    }

    private synchronized void schedule() {
        if (this.timer == null) {
            this.timer = this.timerMgr.schedule(this, this.timerDelay);
        }
    }
}
