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

public class StateOrTerminated {

    private final State theState;
    private final boolean terminated;

    /**
     * @param state if null, means this is terminated instead of state
     */
    public StateOrTerminated(State state) {
        this.theState = state;
        this.terminated = state == null;
    }

    /**
     * @return state, if null that means this is terminated, not state
     */
    public State getState() {
        return this.theState;
    }

    /**
     * @return true if terminated, if false that means getState will not be null
     */
    public boolean isTerminated() {
        return this.terminated;
    }
}
