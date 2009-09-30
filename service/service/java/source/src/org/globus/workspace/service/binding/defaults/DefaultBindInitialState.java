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

package org.globus.workspace.service.binding.defaults;

import org.globus.workspace.service.binding.BindInitialState;
import org.globus.workspace.service.binding.vm.VirtualMachine;
import org.globus.workspace.WorkspaceConstants;
import org.nimbustools.api.services.rm.CreationException;
import org.nimbustools.api.repr.vm.State;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Hashtable;

public class DefaultBindInitialState implements BindInitialState,
                                                WorkspaceConstants {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(DefaultBindInitialState.class.getName());

    protected static final Hashtable initialTable = new Hashtable(8);

    static {
        initialTable.put(State.STATE_Unpropagated,
                new Integer(STATE_UNPROPAGATED));
        initialTable.put(State.STATE_Propagated,
                new Integer(STATE_PROPAGATED));
        initialTable.put(State.STATE_Running,
                new Integer(STATE_STARTED));
        initialTable.put(State.STATE_Paused,
                new Integer(STATE_PAUSED));
    }

    
    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected int[] legalRequestStates =
            {STATE_UNPROPAGATED, STATE_PROPAGATED, STATE_STARTED, STATE_PAUSED};

    
    // -------------------------------------------------------------------------
    // implements BindInitialState
    // -------------------------------------------------------------------------

    public void consume(VirtualMachine vm, String requestedInitialState)
            throws CreationException {

        if (requestedInitialState != null) {
            logger.debug("client requested deployment state: "
                                        + requestedInitialState);

            final int targetState =
                    this.convertInitialState(requestedInitialState);

            boolean ok = false;
            for (int i = 0; i < this.legalRequestStates.length; i++) {
                if (targetState == this.legalRequestStates[i]) {
                    ok = true;
                }
            }

            if (!ok) {
                final String err = "requested initial target state '" +
                        requestedInitialState + "' is illegal";
                throw new CreationException(err);
            }

            vm.getDeployment().setRequestedState(targetState);

        } else {
            logger.debug("client did not request deployment state, default" +
                    " is 'Running'");
            vm.getDeployment().setRequestedState(STATE_STARTED);
        }
    }


    // -------------------------------------------------------------------------
    // UTILITY
    // -------------------------------------------------------------------------

    protected int convertInitialState(String initial) {
        final Object o = initialTable.get(initial);
        if (o == null) {
            return STATE_CORRUPTED_GENERIC;
        } else {
            return ((Number) o).intValue();
        }
    }
}
