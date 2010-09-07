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

package org.nimbustools.api.repr.vm;

public interface State {

    public static final String STATE_Unpropagated = "Unpropagated";
    public static final String STATE_Propagated = "Propagated";
    public static final String STATE_Running = "Running";
    public static final String STATE_Paused = "Paused";
    public static final String STATE_TransportReady = "TransportReady";
    public static final String STATE_Corrupted = "Corrupted";
    public static final String STATE_Cancelled = "Cancelled";

    // workspace wsdl does not know this one yet
    public static final String STATE_TowardsTransportReady = "TowardsTransportReady";

    public String getState();
    public Throwable getProblem();
}
