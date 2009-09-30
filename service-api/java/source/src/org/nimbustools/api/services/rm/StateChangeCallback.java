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

package org.nimbustools.api.services.rm;

import org.nimbustools.api.repr.vm.State;

/**
 * <p>Register with manager in order to get notifications that an instance has
 * changed state.</p>
 *
 * <p>The message layer might have its own asynchronous notification protocol
 * and this allows those mechanisms to be triggered.</p>
 */
public interface StateChangeCallback {

    public void newState(State state);
}
