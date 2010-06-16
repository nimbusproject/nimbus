/*
 * Copyright 1999-2007 University of Chicago
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

package org.globus.workspace.client_common;

import org.globus.workspace.client_core.StubConfigurator;

public abstract class BaseClient
           extends TempBaseClient    // use a modified version of the BC
           implements StubConfigurator {

    public static final int SUCCESS_EXIT_CODE = 0;
    public static final int COMMAND_LINE_EXIT_CODE = 1;
    public static final int APPLICATION_EXIT_CODE = 2;
    public static final int UNKNOWN_EXIT_CODE = 3;
    public static final int CTX_PENDING_RESULTS = 6;

    public static String retCodeDebugStr(int retCode) {

        final String description;
        switch(retCode) {
            case SUCCESS_EXIT_CODE: description = "success"; break;
            case COMMAND_LINE_EXIT_CODE: description = "input problem"; break;
            case APPLICATION_EXIT_CODE: description = "problem running"; break;
            case UNKNOWN_EXIT_CODE: description = "unhandled problem"; break;
            default: description = "unknown return code";
        }

        return "Return code " + retCode + ": " + description;
    }

}
