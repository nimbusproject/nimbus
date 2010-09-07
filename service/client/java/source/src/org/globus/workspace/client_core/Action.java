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

package org.globus.workspace.client_core;

import edu.emory.mathcs.backport.java.util.concurrent.Callable;

/**
 * <p>Base class for all actions in the actions subpackage.</p>
 *
 * <p>All may be run directly or run in a thread via the Callable interface
 * (casting the return object is necessary in that case).</p>
 *
 * <p>In the future, actions will alternatively take objects from the "repr"
 * package as arguments: in some future, any generated class (tied to axis
 * and webservices currently) should be entirely encapsulated (allowing
 * implementation switching/versioning etc.).</p>
 *
 * <p>If used by more than one thread at a time for whatever reason, you are
 * responsible for controlling access to this un-synchronized action
 * implementation in your own ways.</p>
 *
 * <p>The basic expectation is that callers should ensure no Action object
 * fields are altered while calling validation methods or running the
 * action.  But all usage patterns are left open intentionally.</p>
 *
 * @see Callable
 */
public abstract class Action implements Callable {

    protected Settings settings = new Settings();

    /**
     * Each action may have its action called via Callable in a thread.
     *
     * If used this way, the return type needs to be cast appropriately.
     *
     * @return Object return object of action, if any
     * @throws Exception
     *
     * @see edu.emory.mathcs.backport.java.util.concurrent.Callable
     * @see edu.emory.mathcs.backport.java.util.concurrent.FutureTask
     */
    public abstract Object call() throws Exception;

    /**
     * @throws ParameterProblem issue that is known a priori to cause the
     *                          action to fail if it were invoked
     */
    public abstract void validateAll() throws ParameterProblem;

    /**
     * @return Settings instance in use, never null
     * @see Settings
     */
    public Settings getSettings() {
        return this.settings;
    }

    /**
     * @param prefs Settings instance to use instead of default, may not be null
     * @see Settings
     */
    public void setSettings(Settings prefs) {
        if (prefs == null) {
            throw new IllegalArgumentException(
                            "preferences argument may not be null");
        }
        this.settings = prefs;
    }
}
