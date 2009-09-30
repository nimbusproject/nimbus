package org.nimbustools.gateway.ec2.monitoring.defaults;

import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.vm.State;
import org.nimbustools.api.repr.vm.Schedule;
import org.nimbustools.api.repr.vm.VM;
import org.nimbustools.api.services.rm.DestructionCallback;
import org.nimbustools.api.services.rm.StateChangeCallback;
import org.nimbustools.api._repr.vm._VM;
import org.nimbustools.api.defaults.repr.vm.DefaultState;
import org.nimbustools.api.defaults.repr.vm.DefaultSchedule;
import org.nimbustools.api.defaults.repr.DefaultCaller;
import org.nimbustools.gateway.ec2.monitoring.EC2Instance;

import java.util.ArrayList;
import java.util.Date;
import java.util.Calendar;

import javax.persistence.*;
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

@Entity
@Table(name="EC2_INSTANCE")
public class DefaultEC2Instance implements EC2Instance {

    private String id;
    private Caller caller;
    private String callerIdentity;
    private String accessKey;
    private _VM vm;

    // used for synchronizing access to listener lists
    private final Object listenerLock = new Object();

    private ArrayList<StateChangeCallback> stateChangeListeners;
    private ArrayList<DestructionCallback> destructionListeners;

    public DefaultEC2Instance(String id, _VM vm, String accessKey) {
        if (id == null) {
            throw new IllegalArgumentException("id may not be null");
        }
        this.id = id;


        if (vm == null) {
            throw new IllegalArgumentException("vm may not be null");
        }
        this.vm = vm;


        this.caller = vm.getCreator();
        if (this.caller == null) {
            throw new IllegalArgumentException("vm creator may not be null");
        }
        this.callerIdentity = caller.getIdentity();

        if (accessKey == null) {
            throw new IllegalArgumentException("accessKey may not be null");
        }
        this.accessKey = accessKey;

        stateChangeListeners = null;
        destructionListeners = null;

    }

    DefaultEC2Instance() {

    }

    @Id
    @Column(name = "ID")
    public String getId() {
        return id;
    }

    void setId(String id) {
        this.id = id;
    }

    @Column(name= "DN")
    public String getCallerIdentity() {
        return this.callerIdentity;
    }

    void setCallerIdentity(String callerIdentity) {
        this.callerIdentity = callerIdentity;
        DefaultCaller caller = new DefaultCaller();
        caller.setIdentity(callerIdentity);
        this.caller = caller;

    }

    @Transient
    public Caller getCaller() {
        return caller;
    }


    @Column(name = "ACCESS_ID")
    public String getAccessKey() {
        return accessKey;
    }

    void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    @Transient
    public _VM getVM() {
        return this.vm;
    }

    public void setVM(_VM vm) {
        if (this.vm != null) {
            throw new IllegalStateException("instance already has a VM (?)");
        }
        this.vm = vm;
    }

    @Transient
    public boolean isLaunched() {
        final Schedule sched = vm.getSchedule();
        if (sched == null) {
            return false;
        }
        return (sched.getStartTime() != null);
    }

    @Transient
    public boolean isTerminated() {
        final Schedule sched = vm.getSchedule();
        if (sched == null) {
            return false;
        }
        return (sched.getDestructionTime() != null);
    }

    @Transient
    public Date getLaunchTime() {
        Schedule sched = vm.getSchedule();
        if (sched == null) {
            return null;
        }

        Calendar cal = sched.getStartTime();
        if (cal == null) {
            return null;
        }

        return new Date(cal.getTimeInMillis());
    }

    @Transient
    public Date getTerminationTime() {

        Schedule sched = vm.getSchedule();
        if (sched == null) {
            return null;
        }

        Calendar cal = sched.getDestructionTime();
        if (cal == null) {
            return null;
        }

        return new Date(cal.getTimeInMillis());
    }

    public void markLaunched(Calendar launchTime) {

        if (launchTime == null) {
            throw new IllegalArgumentException("launchTime may not be null");
        }

        final Schedule sched = vm.getSchedule();
        final Calendar destructionTime =
                sched != null ? sched.getDestructionTime() : null;

        DefaultSchedule newSched = new DefaultSchedule();
        newSched.setStartTime((Calendar)launchTime.clone());
        newSched.setDestructionTime(destructionTime);
        vm.setSchedule(newSched);

        DefaultState state = new DefaultState();
        state.setState(State.STATE_Running);
        vm.setState(state);

        synchronized (listenerLock) {
            if (stateChangeListeners != null) {
                for (StateChangeCallback cb : stateChangeListeners) {
                    if (cb != null) {
                        cb.newState(state);
                    }
                }
            }
        }
    }

    public void markTerminated(Calendar termTime) {

        if (termTime == null) {
            throw new IllegalArgumentException("termTime may not be null");
        }

        Schedule sched = vm.getSchedule();
        final Calendar startTime =
                sched != null ? sched.getStartTime() : null;

        DefaultSchedule newSched = new DefaultSchedule();
        newSched.setStartTime(startTime);
        newSched.setDestructionTime((Calendar)termTime.clone());
        vm.setSchedule(newSched);

        DefaultState state = new DefaultState();
        state.setState(State.STATE_Cancelled);
        vm.setState(state);

        synchronized (listenerLock) {
            if (destructionListeners != null) {
                for (DestructionCallback cb : destructionListeners) {
                    if (cb != null) {
                        cb.destroyed();
                    }
                }
            }
        }

    }

    public void registerStateChangeListener(StateChangeCallback listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener may not be null");
        }

        synchronized (listenerLock) {

            if (stateChangeListeners == null) {
                stateChangeListeners = new ArrayList<StateChangeCallback>();
            }

            stateChangeListeners.add(listener);
        }
    }

    public void registerDestructionListener(DestructionCallback listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener may not be null");
        }
        synchronized (listenerLock) {

            if (destructionListeners == null) {
                destructionListeners = new ArrayList<DestructionCallback>();
            }

            destructionListeners.add(listener);
        }
    }
}
