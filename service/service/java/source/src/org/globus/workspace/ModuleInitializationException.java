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

public class ModuleInitializationException extends WorkspaceException {

    public ModuleInitializationException() {
        super();
    }

    public ModuleInitializationException(String message) {
        super(message);
    }

    public ModuleInitializationException(String message, Exception e) {
        super(message, e);
    }

    public ModuleInitializationException(String message, Throwable t) {
        super(message, t);
    }

    public ModuleInitializationException(Exception e) {
        super("", e);
    }
}
