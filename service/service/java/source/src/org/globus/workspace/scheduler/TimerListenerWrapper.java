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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.TimerTask;

import commonj.timers.Timer;
import commonj.timers.TimerListener;

/**
 * Copied from Java core.
 */
public class TimerListenerWrapper extends TimerTask {

    private static Log logger =
        LogFactory.getLog(TimerListenerWrapper.class.getName());

    private Timer timer;
    private TimerListener listener;
    private TimerManagerImpl timerManager;
    private boolean suspended = false;
    private boolean expired = false;

    /**
     * @param timer
     * @param timerManager
     */
    public TimerListenerWrapper(Timer timer, TimerManagerImpl timerManager)
    {
        this.timer = timer;
        if(timer instanceof TimerImpl)
        {
            ((TimerImpl) timer).setTimerTask(this);
        }
        this.listener = timer.getTimerListener();
        this.timerManager = timerManager;
    }

    public synchronized void suspend()
    {
        this.suspended = true;
    }

    public synchronized void resume()
    {
        this.suspended = false;
        if(this.expired == true)
        {
            executeTask();
            this.expired = false;
        }
    }

    private void executeTask() {
        try {
            this.listener.timerExpired(timer);
        } catch (Throwable e) {
            logger.debug("Timer exception - ignoring", e);
        }
        if (this.timer.getPeriod() == 0)
        {
            this.timerManager.removeTask(this);
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public synchronized void run()
    {
        if(!this.suspended)
        {
            executeTask();
        }
        else
        {
            this.expired = true;
        }
    }

    protected boolean stop()
    {
        return super.cancel();
    }

    public boolean cancel()
    {
        this.timerManager.removeTask(this);
        return super.cancel();
    }

    /**
     * @return Returns the listener.
     */
    protected TimerListener getListener()
    {
        return this.listener;
    }

    /**
     * @return Returns the timer.
     */
    protected Timer getTimer()
    {
        return this.timer;
    }

}
