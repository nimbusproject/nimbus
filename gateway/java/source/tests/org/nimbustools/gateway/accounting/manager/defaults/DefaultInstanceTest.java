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

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;
import org.testng.annotations.Test;
import org.nimbustools.api.repr.vm.VM;
import org.nimbustools.api.repr.vm.Schedule;
import org.nimbustools.api.repr.vm.State;
import org.nimbustools.api.repr.Caller;

import java.util.Calendar;

public class DefaultInstanceTest {
    @Test
    public void testCalculateCharge() {

        final int rate = 1;
        int totalCharge = 0;

        Schedule sched = mock(Schedule.class);
        State state = mock(State.class);
        when(state.getState()).thenReturn(State.STATE_Unpropagated);
        VM vm = mock(VM.class);
        when(vm.getID()).thenReturn("fakeID");
        when(vm.getSchedule()).thenReturn(sched);
        when(vm.getState()).thenReturn(state);
        Caller caller = mock(Caller.class);

        DefaultInstance inst = new DefaultInstance(vm, caller, 1);

        // instance hasn't started yet, zero charge
        int charge = inst.calculateCharge(Calendar.getInstance().getTime());
        assertEquals(charge, 0);

        final Calendar startTime = Calendar.getInstance();
        startTime.set(2009, 6, 16, 10, 0, 0);

        inst.setStartTime(startTime.getTime());


        //ok it has started, so even 0 runtime should still trigger a charge
        charge = inst.calculateCharge(startTime.getTime());
        assertEquals(charge,rate);
        totalCharge += charge;
        inst.setCharge(totalCharge);

        //now nothing short of startTime +1h should trigger a charge
        Calendar now = (Calendar)startTime.clone();
        charge = inst.calculateCharge(now.getTime());
        assertEquals(charge, 0);

        now.add(Calendar.MINUTE, 10);
        charge = inst.calculateCharge(now.getTime());
        assertEquals(charge, 0);

        // +1hr triggers a charge
        now = (Calendar)startTime.clone();
        now.add(Calendar.HOUR, 1);
        charge = inst.calculateCharge(now.getTime());
        assertEquals(charge, rate);
        totalCharge += charge;
        inst.setCharge(totalCharge);

        // okay now we don't check for a couple hours (and change)
        now.add(Calendar.HOUR, 2);
        now.add(Calendar.MINUTE, 12);
        charge = inst.calculateCharge(now.getTime());
        assertEquals(charge,rate*2);
        totalCharge += charge;
        inst.setCharge(totalCharge);

        // once the termination time is set, the remaining charge is
        // based from that point
        Calendar stopTime = (Calendar) now.clone();
        stopTime.add(Calendar.HOUR,  1);
        stopTime.add(Calendar.MINUTE, 5);

        inst.setStopTime(stopTime.getTime());

        now.add(Calendar.HOUR, 5);
        charge = inst.calculateCharge(now.getTime());
        assertEquals(charge, rate);
        totalCharge += charge;
        inst.setCharge(totalCharge);

        // no more charges after termination
        now.add(Calendar.HOUR, 1);
        charge = inst.calculateCharge(now.getTime());
        assertEquals(charge, 0);

        assertEquals(inst.getCharge(), totalCharge);
    }

    @Test
    public void checkState() {

        Caller caller = mock(Caller.class);

        State state = mock(State.class);
        VM vm = mock(VM.class);
        when(vm.getID()).thenReturn("fake id");

        DefaultInstance inst = new DefaultInstance(vm, caller, 1);

        // first, not running or terminated when state is null
        when(vm.getState()).thenReturn(null);
        assertFalse(inst.isRunning());
        assertFalse(inst.isTerminated());

        when(vm.getState()).thenReturn(state);

        when(state.getState()).thenReturn(State.STATE_Running);
        assertTrue(inst.isRunning());
        assertFalse(inst.isTerminated());

        when(state.getState()).thenReturn(State.STATE_Cancelled);
        assertFalse(inst.isRunning());
        assertTrue(inst.isTerminated());

        when(state.getState()).thenReturn(State.STATE_Corrupted);
        assertFalse(inst.isRunning());
        assertTrue(inst.isTerminated());

        when(state.getState()).thenReturn(State.STATE_TransportReady);
        assertFalse(inst.isRunning());
        assertTrue(inst.isTerminated());

        when(state.getState()).thenReturn(State.STATE_Paused);
        assertFalse(inst.isRunning());
        assertFalse(inst.isTerminated());

        when(state.getState()).thenReturn(State.STATE_Propagated);
        assertFalse(inst.isRunning());
        assertFalse(inst.isTerminated());

        when(state.getState()).thenReturn(State.STATE_TowardsTransportReady);
        assertFalse(inst.isRunning());
        assertFalse(inst.isTerminated());

        when(state.getState()).thenReturn(State.STATE_Unpropagated);
        assertFalse(inst.isRunning());
        assertFalse(inst.isTerminated());

    }
}
