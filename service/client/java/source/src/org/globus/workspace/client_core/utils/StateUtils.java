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

package org.globus.workspace.client_core.utils;

import org.globus.workspace.client_core.repr.State;
import org.nimbustools.messaging.gt4_0.generated.types.CurrentState;
import org.nimbustools.messaging.gt4_0.common.Constants_GT4_0;
import org.apache.axis.message.MessageElement;

public class StateUtils {

    public static boolean isValidRequestState(String state) {
        if (!State.testValidState(state)) {
            return false;
        }
        if (State.STATE_Unstaged.equalsIgnoreCase(state)) {
            return true;
        }
        if (State.STATE_Unpropagated.equalsIgnoreCase(state)) {
            return true;
        }
        if (State.STATE_Propagated.equalsIgnoreCase(state)) {
            return true;
        }
        if (State.STATE_Running.equalsIgnoreCase(state)) {
            return true;
        }
        if (State.STATE_Paused.equalsIgnoreCase(state)) {
            return true;
        }
        return false;
    }

    /**
     * @param workspaceID for exception string if there is a problem
     * @param serviceAddress for exception string if there is a problem
     * @param me retrieved from WS
     * @return state, never null
     * @throws Exception problem
     */
    public static State fromWireHelper(String workspaceID,
                                       String serviceAddress,
                                       MessageElement[] me) throws Exception {

        final String prefix = "Problem deserializing CurrentState " +
                        "from workspace ID '" + workspaceID +
                        "', service address '" + serviceAddress +
                        "'.  Error: ";

        if (me == null || me.length == 0) {
            throw new Exception(prefix + "no MessageElement[]");
        }

        final State state;
        try {
            final CurrentState currentState =
                    (CurrentState) me[0].getValueAsType(
                                Constants_GT4_0.RP_CURRENT_STATE,
                                CurrentState.class);

            state = State.fromCurrentState_Type(currentState);
        } catch (Throwable e) {
            throw new Exception(prefix + e.getMessage(), e);
        }

        if (state == null) {
            throw new Exception(prefix + "could not parse out State as type");
        }

        return state;
    }
}
