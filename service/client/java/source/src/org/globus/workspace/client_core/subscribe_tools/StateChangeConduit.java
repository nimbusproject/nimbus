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

import org.apache.axis.message.addressing.EndpointReferenceType;
import org.globus.workspace.client_core.repr.State;

/**
 * Undocumented.
 *
 * Usually would only need to look at/use this if you're altering the
 * subscription implementation (or making a new one).
 */
public interface StateChangeConduit {

    public void stateChange(EndpointReferenceType epr, State newState);

}
