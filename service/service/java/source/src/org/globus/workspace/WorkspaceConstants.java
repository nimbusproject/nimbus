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

package org.globus.workspace;

public interface WorkspaceConstants {
    
    // -------------------------------------------------------------------------
    // SHORCUTS
    // -------------------------------------------------------------------------


    public static final int DEFAULT_SHUTDOWN_INVALID = -1;
    public static final int DEFAULT_SHUTDOWN_NORMAL = 0;
    public static final int DEFAULT_SHUTDOWN_SERIALIZE = 1;
    public static final int DEFAULT_SHUTDOWN_TRASH = 2;


    // -------------------------------------------------------------------------
    // STATES
    // -------------------------------------------------------------------------

    /* Initial value */
    public static final int STATE_INVALID = -2;


    /**
     * For Scheduler interface.  This state is only possible during
     * the time between scheduling and resource instantiation
     * finalization.
     * @see org.globus.workspace.scheduler.Scheduler#stateNotification(int, int)
     */
    public static final int STATE_SCHEDULED_ONLY = -1;


    // The guarantees about the values of these state constants are
    //
    // a) there are no gaps in the state values (unless you like Cantor?)
    // b) The valid states for a resource to have are between
    //    STATE_UNSTAGED and STATE_DESTROYING+STATE_CORRUPTED during any
    //    point it is evaluated by the StateTransition system and its entry
    //    points in WorkspaceResourceImpl
    // c) subtracting STATE_CORRUPTED from a state with value greater than
    //    STATE_CORRUPTED tells you the state the resource would have been
    //    moved to if something did not go wrong (example below)
    // d) there are two special cases to these rules: STATE_DESTROY_FAILED, STATE_DESTROY_SUCCEEDED

    public static final int STATE_FIRST_LEGAL = 0;
    public static final int STATE_UNSTAGED = 0;
    public static final int STATE_STAGING_IN = 1;
    public static final int STATE_UNPROPAGATED = 2;
    public static final int STATE_PROPAGATING = 3;
    public static final int STATE_PROPAGATING_TO_START = 4;
    public static final int STATE_PROPAGATING_TO_PAUSE = 5;

    public static final int STATE_PROPAGATED = 6;
    public static final int STATE_STARTING = 7;
    public static final int STATE_STARTED = 8;
    public static final int STATE_SERIALIZING = 9;
    public static final int STATE_SERIALIZED = 10;
    public static final int STATE_PAUSING = 11;
    public static final int STATE_PAUSED = 12;
    public static final int STATE_REBOOT = 13;
    public static final int STATE_SHUTTING_DOWN = 14;

    public static final int STATE_READYING_FOR_TRANSPORT = 15;
    public static final int STATE_READY_FOR_TRANSPORT = 16;
    public static final int STATE_STAGING_OUT = 17;
    public static final int STATE_STAGED_OUT = 18;

    public static final int STATE_CANCELLING_STAGING_IN = 19;
    public static final int STATE_CANCELLING_UNPROPAGATED = 20;
    public static final int STATE_CANCELLING_PROPAGATING = 21;
    public static final int STATE_CANCELLING_PROPAGATING_TO_START = 22;
    public static final int STATE_CANCELLING_PROPAGATING_TO_PAUSE = 23;
    public static final int STATE_CANCELLING_AT_VMM = 24;
    public static final int STATE_CANCELLING_READYING_FOR_TRANSPORT = 25;
    public static final int STATE_CANCELLING_READY_FOR_TRANSPORT = 26;
    public static final int STATE_CANCELLING_STAGING_OUT = 27;

    public static final int STATE_DESTROYING = 28;
    public static final int STATE_DESTROY_FAILED = -128; // See TerminateSuite notes
    public static final int STATE_DESTROY_SUCCEEDED = -256; // See TerminateSuite notes
    public static final int STATE_CORRUPTED_GENERIC = 29;

    // STATE_CORRUPTED means Corrupted-Unstaged, meaning resource was about
    // to be moved to STATE_UNSTAGED if there was no error
    //
    // STATE_CORRUPTED + STATE_PROPAGATED mean Corrupted-Propagated
    // and so on, up to STATE_LAST_LEGAL
    public static final int STATE_CORRUPTED = 30;

    public static final int STATE_LAST_LEGAL
                                = STATE_CORRUPTED + STATE_DESTROYING;
    
}
