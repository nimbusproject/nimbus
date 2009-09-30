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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.workspace.common.print.Print;
import org.globus.workspace.client_core.repr.Workspace;

public class GenericListener {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(GenericListener.class.getName());
    

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected final Print pr;


    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    /**
     * No printing possible
     */
    public GenericListener() {
        this(null);
    }

    /**
     * @param print may be null (will be disabled)
     */
    public GenericListener(Print print) {
        if (print == null) {
            this.pr = new Print();
        } else {
            this.pr = print;
        }
    }


    // -------------------------------------------------------------------------
    // printing
    // -------------------------------------------------------------------------

    protected String workspaceID(Workspace workspace) {
        if (workspace == null) {
            return "[[workspace reference is missing]]";
        }
        return workspace.getDisplayName();
    }

    protected void errlogNoMaster(int prcode, Workspace workspace) {

        if (!this.pr.enabled()) {
            return; // *** EARLY RETURN ***
        }

        final String err =
                "Listener did not receive reference to subscription master: " +
                        this.workspaceID(workspace);

        if (this.pr.useThis()) {
            this.pr.errln(prcode, err);
        } else if (this.pr.useLogging()) {
            logger.error(err);
        }
    }

    protected void errlogNoWorkspace(int prcode) {

        if (!this.pr.enabled()) {
            return; // *** EARLY RETURN ***
        }

        final String err = "Listener did not receive reference to workspace.";

        if (this.pr.useThis()) {
            this.pr.errln(prcode, err);
        } else if (this.pr.useLogging()) {
            logger.error(err);
        }
    }
}
