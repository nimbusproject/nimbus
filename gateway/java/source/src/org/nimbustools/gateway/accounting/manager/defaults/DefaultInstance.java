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

package org.nimbustools.gateway.accounting.manager.defaults;

import org.nimbustools.gateway.accounting.manager.Instance;
import org.nimbustools.api.repr.vm.VM;
import org.nimbustools.api.repr.vm.Schedule;
import org.nimbustools.api.repr.vm.State;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.defaults.repr.DefaultCaller;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "ACCOUNT_INSTANCE")
public class DefaultInstance implements Instance {

    private static final Log logger =
            LogFactory.getLog(DefaultInstance.class.getName());
    private static final int HOUR_IN_MS = 60 * 60 * 1000;


    private String id;
    private VM vm;
    private String callerIdentity;
    private Caller caller;
    private int rate;

    private Date startTime;
    private Date stopTime;

    private int charge;

    public DefaultInstance(VM vm, Caller caller, int rate) {
        if (vm == null) {
            throw new IllegalArgumentException("vm may not be null");
        }
        this.vm = vm;

        //ensure that id never changes
        this.id = vm.getID();
        if (id == null) {
            throw new IllegalArgumentException("vm ID may not be null");
        }

        if (caller == null) {
            throw new IllegalArgumentException("caller may not be null");
        }
        this.caller = caller;
        this.callerIdentity = caller.getIdentity();

        if (rate < 0) {
            throw new IllegalArgumentException("rate may not be negative");
        }
        this.rate = rate;


        this.charge = 0;
    }

    DefaultInstance() {
    }

    @Id
    @Column(name = "ID")
    public String getID() {
        return id;
    }

    void setID(String id) {
        this.id = id;
    }

    @Transient
    public VM getVM() {
        return this.vm;
    }

    public void setVM(VM vm) {
        this.vm = vm;
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
        return this.caller;
    }

    @Transient
    public boolean isRunning() {
        final State state = this.vm.getState();
        return state != null && State.STATE_Running.equals(state.getState());
    }

    @Transient
    public boolean isTerminated() {
        final State state = this.vm.getState();
        if (state == null) {
            return false;
        }
        final String stateStr = state.getState();
        return stateStr != null && (stateStr.equals(State.STATE_TransportReady) ||
            stateStr.equals(State.STATE_Cancelled) ||
            stateStr.equals(State.STATE_Corrupted));
    }

    @Column(name = "RATE")
    public int getRate() {
        return this.rate;
    }

    void setRate(int rate) {
        this.rate = rate;
    }

    @Column(name = "CHARGE")
    public int getCharge() {
        return this.charge;
    }

    public void setCharge(int charge) {
        if (charge < 0) {
            throw new IllegalArgumentException("charge may not be negative");
        }

        this.charge = charge;
    }

    @Column(name = "START_TIME")
    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    @Column(name = "STOP_TIME")
    public Date getStopTime() {
        return stopTime;
    }

    public void setStopTime(Date stopTime) {
        this.stopTime = stopTime;
    }

    public boolean updateFromVM() {
        if (this.vm == null) {
            return false;
        }

        Schedule schedule = this.vm.getSchedule();
        if (schedule == null) {
            return false;
        }

        boolean updated = false;
        if (this.startTime == null && schedule.getStartTime() != null) {
            this.startTime = schedule.getStartTime().getTime();
            updated = true;
        }

        if (this.stopTime == null && schedule.getDestructionTime() != null) {
            this.stopTime = schedule.getDestructionTime().getTime();
            updated = true;
        }

        return updated;

    }

    /**
     * Takes the current time and calculates the new charge
     * @param chargeTime current time
     * @return credits charged since last tick
     */
    public int calculateCharge(Date chargeTime) {
        if (chargeTime == null) {
            throw new IllegalArgumentException("chargeTime may not be null");
        }

        // short circuit for free instances
        if (this.rate == 0) {
            return 0;
        }


        // no charges until instance is running
        if (this.startTime == null) {
            return 0;
        }


        // strategy is to calculate the new total charge and return the delta


        if (this.stopTime != null) {
            chargeTime = this.stopTime;
        }

        // java date structures are weird

        long startTimeMs = this.startTime.getTime();
        long nowMs = chargeTime.getTime();

        long durationMs = nowMs - startTimeMs;

        int hours = (int) durationMs / HOUR_IN_MS;

        if (durationMs >= hours*HOUR_IN_MS) {
            hours++;
        }

        int newCharge = hours * this.rate;

        return newCharge - this.charge;
    }

    public void addCharge(int charge) {
        // not threadsafe!
        this.charge += charge;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DefaultInstance that = (DefaultInstance) o;

        if (!id.equals(that.id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
