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

package org.globus.workspace.client_core.subscribe_tools.internal;

import org.globus.workspace.client_core.repr.Workspace;
import org.globus.workspace.client_core.subscribe_tools.StateChangeListener;
import org.globus.workspace.client_core.subscribe_tools.TerminationListener;

import java.util.Vector;
import java.util.Arrays;
import java.util.Enumeration;

class WorkspaceAndListeners {

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private final Workspace workspace;
    private final Vector stateListeners;
    private final Vector termListeners;

    
    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    /**
     * @param workspace may not be null
     */
    WorkspaceAndListeners(Workspace workspace) {
        if (workspace == null) {
            throw new IllegalArgumentException("workspace may not be null");
        }
        this.workspace = workspace;
        this.stateListeners = new Vector(8);
        this.termListeners = new Vector(8);
    }

    /**
     * @param workspace may not be null
     * @param stateChangeListeners may be null or empty
     * @param terminationListeners may be null or empty
     */
    WorkspaceAndListeners(Workspace workspace,
                          StateChangeListener[] stateChangeListeners,
                          TerminationListener[] terminationListeners) {

        if (workspace == null) {
            throw new IllegalArgumentException("workspace may not be null");
        }
        this.workspace = workspace;
        if (stateChangeListeners == null
                || stateChangeListeners.length == 0) {
            this.stateListeners = new Vector(8);
        } else {
            this.stateListeners =
                    new Vector(stateChangeListeners.length);
            this.stateListeners.addAll(
                    Arrays.asList(stateChangeListeners));
        }

        if (terminationListeners == null
                || terminationListeners.length == 0) {
            this.termListeners = new Vector(8);
        } else {
            this.termListeners =
                    new Vector(terminationListeners.length);
            this.termListeners.addAll(
                    Arrays.asList(terminationListeners));
        }
    }


    // -------------------------------------------------------------------------
    // QUERY
    // -------------------------------------------------------------------------

    public Workspace getWorkspace() {
        return this.workspace;
    }

    Enumeration getStateChangeListeners() {
        return this.stateListeners.elements();
    }

    int numStateChangeListeners() {
        return this.stateListeners.size();
    }

    Enumeration getTerminationListeners() {
        return this.termListeners.elements();
    }

    int numTerminationListeners() {
        return this.termListeners.size();
    }

    
    // -------------------------------------------------------------------------
    // ADJUST
    // -------------------------------------------------------------------------

    void addStateListener(StateChangeListener listener) {
        this.stateListeners.add(listener);
    }

    void addTerminationListener(TerminationListener listener) {
        this.termListeners.add(listener);
    }
}
