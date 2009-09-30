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

import commonj.timers.TimerListener;
import commonj.timers.CancelTimerListener;
import commonj.timers.Timer;

/**
 * Copied from Java core.
 */
public class TimerImpl implements Timer {

    private TimerListenerWrapper timerTask;
    private TimerListener listener;
    private long period;

    public TimerImpl(TimerListener listener, long period)
    {
        this.period = period;
        this.listener = listener;
    }

    /* (non-Javadoc)
     * @see commonj.timer.Timer#cancel()
     */
    public boolean cancel()
    {
        if(this.listener instanceof CancelTimerListener)
        {
            ((CancelTimerListener) this.listener).timerCancel(this);
        }
        return timerTask.cancel();
    }

    /* (non-Javadoc)
     * @see commonj.timer.Timer#getPeriod()
     */
    public long getPeriod()
    {
        return this.period;
    }

    /* (non-Javadoc)
     * @see commonj.timer.Timer#getTimerListener()
     */
    public TimerListener getTimerListener()
    {
        return this.listener;
    }

    /* (non-Javadoc)
     * @see commonj.timer.Timer#scheduledExecutionTime()
     */
    public long scheduledExecutionTime()
    {
        return this.timerTask.scheduledExecutionTime() + this.period;
    }

    /**
     * @return Returns the timerTask.
     */
    protected TimerListenerWrapper getTimerTask()
    {
        return this.timerTask;
    }

    /**
     * @param timerTask The timerTask to set.
     */
    protected void setTimerTask(TimerListenerWrapper timerTask)
    {
        this.timerTask = timerTask;
    }

}
