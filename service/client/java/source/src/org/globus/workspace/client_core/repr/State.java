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

package org.globus.workspace.client_core.repr;

import org.nimbustools.messaging.gt4_0.generated.negotiable.InitialState_Type;
import org.nimbustools.messaging.gt4_0.generated.types.CurrentState;
import org.nimbustools.messaging.gt4_0.generated.types.CurrentState_Enumeration;
import org.nimbustools.messaging.gt4_0.generated.types.WorkspaceFault;

/**
 * If instances couldn't carry Exception, we could use private constructor
 * and public final instance idiom...
 */
public class State {

    public static final String STATE_UNSET = "UNSET";

    public static final String STATE_Unstaged = "Unstaged";
    public static final String STATE_Unpropagated = "Unpropagated";
    public static final String STATE_Propagated = "Propagated";
    public static final String STATE_Running = "Running";
    public static final String STATE_Paused = "Paused";
    public static final String STATE_TransportReady = "TransportReady";
    public static final String STATE_StagedOut = "StagedOut";
    public static final String STATE_Corrupted = "Corrupted";
    public static final String STATE_Cancelled = "Cancelled";

    public static final State DEFAULT_INITIAL_STATE =
                                        new State(STATE_Running, true);

    public static final String[] VALID_STATES = { STATE_UNSET,
                                                  STATE_Unstaged,
                                                  STATE_Unpropagated,
                                                  STATE_Propagated,
                                                  STATE_Running,
                                                  STATE_Paused,
                                                  STATE_TransportReady,
                                                  STATE_StagedOut,
                                                  STATE_Corrupted,
                                                  STATE_Cancelled };

    public static final String[] OK_STATES = { STATE_Unstaged,
                                               STATE_Unpropagated,
                                               STATE_Propagated,
                                               STATE_Running,
                                               STATE_Paused,
                                               STATE_TransportReady,
                                               STATE_StagedOut };

    public static final String[] PROBLEM_STATES = { STATE_Corrupted,
                                                    STATE_Cancelled };

    private String state = STATE_UNSET;
    private Exception problem;
    private final boolean nochanges;

    public State() {
        this.nochanges = false;
    }

    public State(String stateString) {
        this.setState(stateString);
        this.nochanges = false;
    }

    public State(String stateString, boolean immutable) {
        this.setState(stateString);
        this.nochanges = immutable;
    }

    /**
     * USE FOR LOGGING ETC ONLY
     * @return value of state, possibly also value of problem exception
     */
    public synchronized String toString() {
        if (this.problem != null) {
            return this.state +
                    ", problem exception: " + this.problem.getMessage();
        }
        return this.state;
    }

    public synchronized String getState() {
        return this.state;
    }

    public synchronized void setState(String newState) {

        if (this.nochanges) {
            throw new IllegalStateException("cannot change immutable state");
        }

        if (newState == null) {
            throw new IllegalArgumentException("newState is null");
        }

        if (!testValidState(newState)) {
            throw new IllegalArgumentException(
                                "newState is not a valid state string");
        }

        this.state = newState;
    }

    public synchronized Exception getProblem() {
        return this.problem;
    }

    public synchronized void setProblem(Exception exception) {
        if (this.nochanges) {
            throw new IllegalStateException("cannot change immutable state");
        }
        this.problem = exception;
    }

    // -----------------------------------------------------------------

    /**
     * @return true if cancelled or corrupted
     */
    public synchronized boolean isProblemState() {
        return testProblemState(this.state);
    }

    public synchronized boolean isOKState() {
        return testOKState(this.state);
    }

    public synchronized boolean isOKAndEqualOrAfter(State afterThis) {
        
        if (afterThis == null) {
            return false; // *** EARLY RETURN ***
        }

        // does not work if either are not OK
        if (!this.isOKState()) {
            return false; // *** EARLY RETURN ***
        }

        final String afterThisString = afterThis.getState();
        // does not work if either are not OK
        if (!testOKState(afterThisString)) {
            return false; // *** EARLY RETURN ***
        }

        if (this.equals(afterThis)) {
            return true; // *** EARLY RETURN ***
        }

        int afterThisIndex = -1;
        for (int i = 0; i < OK_STATES.length; i++) {
            if (OK_STATES[i].equals(afterThisString)) {
                afterThisIndex = i;
                break;
            }
        }

        int thisIndex = -1;
        for (int i = 0; i < OK_STATES.length; i++) {
            if (OK_STATES[i].equals(this.state)) {
                thisIndex = i;
                break;
            }
        }

        // collapse these
        // also, Propagated is ambiguous but we leave that problem be for now
        final int runningIndex = 3;
        final int pausedIndex = 4;
        if (thisIndex == pausedIndex) {
            thisIndex = runningIndex;
        }
        if (afterThisIndex == pausedIndex) {
            afterThisIndex = runningIndex;
        }
        

        if (thisIndex < 0 || afterThisIndex < 0) {
            return false; // *** EARLY RETURN ***
        }

        return thisIndex >= afterThisIndex;
    }
    

    public synchronized boolean isUNSET() {
        return STATE_UNSET.equalsIgnoreCase(this.state);
    }

    public synchronized boolean isUnstaged() {
        return STATE_Unstaged.equalsIgnoreCase(this.state);
    }

    public synchronized boolean isUnpropagated() {
        return STATE_Unpropagated.equalsIgnoreCase(this.state);
    }

    public synchronized boolean isPropagated() {
        return STATE_Propagated.equalsIgnoreCase(this.state);
    }

    public synchronized boolean isRunning() {
        return STATE_Running.equalsIgnoreCase(this.state);
    }

    public synchronized boolean isPaused() {
        return STATE_Paused.equalsIgnoreCase(this.state);
    }

    public synchronized boolean isTransportReady() {
        return STATE_TransportReady.equalsIgnoreCase(this.state);
    }

    public synchronized boolean isStagedOut() {
        return STATE_StagedOut.equalsIgnoreCase(this.state);
    }

    public synchronized boolean isCorrupted() {
        return STATE_Corrupted.equalsIgnoreCase(this.state);
    }

    public synchronized boolean isCancelled() {
        return STATE_Cancelled.equalsIgnoreCase(this.state);
    }

    /**
     * @param o an object
     * @return true if state matches. any accompanying error message is not
     *              consulted for equality check
     */
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final State state1 = (State) o;

        if (this.state != null
                ? !this.state.equalsIgnoreCase(state1.state)
                : state1.state != null) {
            return false;
        }

        return true;
    }

    public synchronized int hashCode() {
        int result = this.state != null ? this.state.hashCode() : 0;
        result = 31 * result;
        return result;
    }

    // -----------------------------------------------------------------

    public static boolean testValidState(String input) {

        if (input == null) {
            return false;
        }

        // Not using VALID_STATES, so we can use this method from constructor
        // (where VALID_STATES is not initialized yet in the first run).
        final String[] valid = { STATE_UNSET,
                                 STATE_Unstaged,
                                 STATE_Unpropagated,
                                 STATE_Propagated,
                                 STATE_Running,
                                 STATE_Paused,
                                 STATE_TransportReady,
                                 STATE_StagedOut,
                                 STATE_Corrupted,
                                 STATE_Cancelled };

        for (int i = 0; i < valid.length; i++) {
            if (valid[i].equalsIgnoreCase(input)) {
                return true;
            }
        }
        return false;
    }

    public static boolean testProblemState(String input) {
        if (input == null) {
            return false;
        }
        for (int i = 0; i < PROBLEM_STATES.length; i++) {
            if (PROBLEM_STATES[i].equalsIgnoreCase(input)) {
                return true;
            }
        }
        return false;
    }

    public static boolean testOKState(String input) {
        if (input == null) {
            return false;
        }
        for (int i = 0; i < OK_STATES.length; i++) {
            if (OK_STATES[i].equalsIgnoreCase(input)) {
                return true;
            }
        }
        return false;
    }

    // -----------------------------------------------------------------
    
    // in the future protocol implementation will be encapsulated better
    public static State fromInitialState_Type(InitialState_Type initial) {

        if (initial == null) {
            return null;
        }

        final String val = initial.getValue();

        if (testValidState(val)) {
            return new State(val);
        }

        return null;
    }

    // in the future protocol implementation will be encapsulated better
    public static State fromCurrentState_Type(CurrentState curr) {

        if (curr == null) {
            return null;
        }

        final CurrentState_Enumeration val = curr.getState();
        if (val == null) {
            return null;
        }

        State ret = null;
        if (testValidState(val.getValue())) {
            ret = new State(val.getValue());
            final WorkspaceFault fault = curr.getWorkspaceFault();
            if (fault != null) {
                ret.setProblem(fault);
            }
        }
        
        return ret;
    }
}

