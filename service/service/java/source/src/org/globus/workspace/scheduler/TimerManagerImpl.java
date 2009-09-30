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

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import commonj.timers.StopTimerListener;
import commonj.timers.Timer;
import commonj.timers.TimerListener;
import commonj.timers.TimerManager;

/**
 * Copied from Java core.
 */
public class TimerManagerImpl implements TimerManager
{
    private int poolSize = 1;
    private List taskList = new LinkedList();
    private List timerPool = new ArrayList();
    private int currentTimer = 0;

    public TimerManagerImpl()
    {
        for(int i = 0; i < this.poolSize; i++)
        {
            timerPool.add(new java.util.Timer(true));
        }
    }

    public synchronized void resume()
    {
        Iterator taskIterator = this.taskList.iterator();
        while(taskIterator.hasNext())
        {
            ((TimerListenerWrapper) taskIterator.next()).resume();
        }
    }

    public Timer schedule(TimerListener listener, Date firstTime, long period)
    {
        java.util.Timer timer =
            (java.util.Timer) timerPool.get(this.currentTimer);
        this.currentTimer = (this.currentTimer + 1)%this.poolSize;
        Timer newTimer = new TimerImpl(listener, period);
        TimerListenerWrapper task = new TimerListenerWrapper(newTimer, this);
        this.taskList.add(task);
        timer.schedule(task, firstTime, period);
        return newTimer;
    }

    public Timer schedule(TimerListener listener, Date time)
    {
        java.util.Timer timer =
            (java.util.Timer) timerPool.get(this.currentTimer);
        this.currentTimer = (this.currentTimer + 1)%this.poolSize;
        Timer newTimer = new TimerImpl(listener, 0);
        TimerListenerWrapper task = new TimerListenerWrapper(newTimer, this);
        this.taskList.add(task);
        timer.schedule(task, time);
        return newTimer;
    }

    public Timer schedule(TimerListener listener, long delay, long period)
    {
        java.util.Timer timer =
            (java.util.Timer) timerPool.get(this.currentTimer);
        this.currentTimer = (this.currentTimer + 1)%this.poolSize;
        Timer newTimer = new TimerImpl(listener, period);
        TimerListenerWrapper task = new TimerListenerWrapper(newTimer, this);
        this.taskList.add(task);
        timer.schedule(task, delay, period);
        return newTimer;
    }

    public Timer schedule(TimerListener listener, long delay)
    {
        java.util.Timer timer =
            (java.util.Timer) timerPool.get(this.currentTimer);
        this.currentTimer = (this.currentTimer + 1)%this.poolSize;
        Timer newTimer = new TimerImpl(listener, 0);
        TimerListenerWrapper task = new TimerListenerWrapper(newTimer, this);
        this.taskList.add(task);
        timer.schedule(task, delay);
        return newTimer;
    }

    public Timer scheduleAtFixedRate(TimerListener listener, Date firstTime,
        long period)
    {
        java.util.Timer timer =
            (java.util.Timer) timerPool.get(this.currentTimer);
        this.currentTimer = (this.currentTimer + 1)%this.poolSize;
        Timer newTimer = new TimerImpl(listener, period);
        TimerListenerWrapper task = new TimerListenerWrapper(newTimer, this);
        this.taskList.add(task);
        timer.scheduleAtFixedRate(task, firstTime, period);
        return newTimer;
    }

    public Timer scheduleAtFixedRate(TimerListener listener, long delay,
        long period)
    {
        java.util.Timer timer =
            (java.util.Timer) timerPool.get(this.currentTimer);
        this.currentTimer = (this.currentTimer + 1)%this.poolSize;
        Timer newTimer = new TimerImpl(listener, period);
        TimerListenerWrapper task = new TimerListenerWrapper(newTimer, this);
        this.taskList.add(task);
        timer.scheduleAtFixedRate(task, delay, period);
        return newTimer;
    }

    public synchronized void stop()
    {
        Iterator taskIterator = this.taskList.iterator();
        TimerListenerWrapper task;
        TimerListener listener;

        while(taskIterator.hasNext())
        {
            task = (TimerListenerWrapper) taskIterator.next();
            task.stop();
            listener = task.getListener();
            if(listener instanceof StopTimerListener)
            {
                ((StopTimerListener) listener).timerStop(task.getTimer());
            }
        }
        this.taskList.clear();
    }

    public synchronized void suspend()
    {
        Iterator taskIterator = this.taskList.iterator();
        while(taskIterator.hasNext())
        {
            ((TimerListenerWrapper) taskIterator.next()).suspend();
        }
    }

    protected synchronized void removeTask(Object task)
    {
        this.taskList.remove(task);
    }


    public int getPoolSize()
    {
        return this.poolSize;
    }

    public void setPoolSize(int poolSize)
    {
        // don't allow shrinking of the pool
        for(int i = this.poolSize; i < poolSize; i++)
        {
            timerPool.add(new java.util.Timer(true));
        }
        this.poolSize = poolSize;
    }
}
