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

package org.globus.workspace.client_core.subscribe_tools;

import org.globus.workspace.client_core.repr.Workspace;

/**
 * This provides a uniform, implementation hiding internal interface for
 * subscribing to workspace changes, currently either by true asynchronous
 * notifications or by a polling based implementation.
 *
 * All implementations (asynchronous notifications, polling) provide one
 * asynchronous view to the API caller, using listener registration + callback.
 *
 * i.e., polling implementation(s) translate events into callbacks for you.
 * 
 * Each workspace topic is supported.  Topic changes are propagated to listener
 * objects you register here.  Since the Workspace object is only a loose
 * container that allows many kinds of usage, the decision was made to not
 * connect listeners to the Workspace object (putting listener invocations
 * under setXYZ etc.).
 *
 * Instead, the listener is a PRE-change hook.  Defaults that just effect
 * the change (if applicable) and do nothing else are provided, and you can
 * extend/override anything.  Seeing usage examples from pre-existing code
 * is probably the fastest way to learn how to do things.
 *
 * ---
 *
 * Where possible, resources are saved by sharing (for example, the current
 * asynchronous notification implementation has just one listening server for
 * all asynchronous connections that SubscriptionMaster is tracking). Keep that
 * in mind if you'd like to use multiple SubscriptionMaster objects with
 * slightly different settings for different workspaces or groups of workspaces,
 * etc. (which you can certainly do).
 *
 * @see StateChangeListener
 * @see TerminationListener
 */
public interface SubscriptionMaster {

    /**
     * Newly track a workspace's state changes or add another listener.
     *
     * Note that if you are adding a listener, the OLD workspace object
     * instance will be used in the callback delivery as the parameter,
     * not this object (if it is for some reason a different object but
     * holds the same instance EPR as a previously tracked workspace).
     *
     * This is because Workspace object identity is based on the workspace 
     * EPR's address and resource key, not on any Workspace object specific
     * equals method (which is intentionally left unimplemented).
     *
     * (This is a necessary implementation detail because there are situations
     * when only the EPR is available to lookup a workspace instance)
     *
     * (could add a replace-workspace method, but don't see any urgent need
     *  for this right now...)
     *
     * @param workspace workspace to track, may not be null
     * @param listener listener to call
     * @return true if this is a newly tracked workspace (otherwise it will
     *         add this listener)
     */
    public boolean trackStateChanges(Workspace workspace,
                                     StateChangeListener listener);

    /**
     * Newly track a workspace's termination event or add another listener.
     *
     * For Workspace object equality notes:
     * @see #trackStateChanges(Workspace, StateChangeListener) 
     *
     * @param workspace may not be null
     * @param listener listener to call
     * @return true if this is a newly tracked workspace (otherwise it will
     *         add this listener)
     */
    public boolean trackTerminationChanges(Workspace workspace,
                                           TerminationListener listener);

    /**
     * Stop tracking this Workspace and all associated listeners.
     *
     * For Workspace object equality notes:
     * @see #trackStateChanges(Workspace, StateChangeListener)
     * 
     * @param workspace may not be null
     * @return true if removed, false if it wasn't there
     */
    public boolean untrackWorkspace(Workspace workspace);

}
