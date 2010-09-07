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

package org.nimbustools.api.defaults.repr;

import org.nimbustools.api._repr._ShutdownTasks;

import java.net.URI;

public class DefaultShutdownTasks implements _ShutdownTasks {

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private URI baseFileUnpropagationTarget;
    private boolean appendID;


    // -------------------------------------------------------------------------
    // implements org.nimbustools.api.repr.ShutdownTasks
    // -------------------------------------------------------------------------

    public URI getBaseFileUnpropagationTarget() {
        return this.baseFileUnpropagationTarget;
    }

    public boolean isAppendID() {
        return this.appendID;
    }


    // -------------------------------------------------------------------------
    // implements org.nimbustools.api._repr._ShutdownTasks
    // -------------------------------------------------------------------------

    public void setBaseFileUnpropagationTarget(URI target) {
        this.baseFileUnpropagationTarget = target;
    }

    public void setAppendID(boolean appendID) {
        this.appendID = appendID;
    }
}
