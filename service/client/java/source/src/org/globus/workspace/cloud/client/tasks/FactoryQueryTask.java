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

package org.globus.workspace.cloud.client.tasks;

import edu.emory.mathcs.backport.java.util.concurrent.Callable;

import java.util.ArrayList;

import org.globus.workspace.client.WorkspaceCLIMain;
import org.globus.workspace.client.Opts;
import org.globus.workspace.common.print.Print;

public class FactoryQueryTask implements Callable {

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private final String url;
    private final String idAuthz;

    private final Print pr;


    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    public FactoryQueryTask(String factoryURL,
                            String identityAuthorization,
                            Print print) {
        if (print == null) {
            throw new IllegalArgumentException("print may not be null");
        }
        this.pr = print;
        this.url = factoryURL;
        this.idAuthz = identityAuthorization;
    }


    // -------------------------------------------------------------------------
    // implements Callable
    // -------------------------------------------------------------------------

    public Object call() throws Exception {
        return new Integer(this.execute());
    }


    // -------------------------------------------------------------------------
    // EXECUTE
    // -------------------------------------------------------------------------

    int execute() {

        // Could change this now to use the client API more directly, but
        // there is not a significant advantage to do so right now.

        final ArrayList cmdList = new ArrayList(4);

        if (this.pr.isDebugEnabled()) {
            cmdList.add("--" + Opts.DEBUG_OPT_STRING);
        }
        cmdList.add("--" + Opts.SERVICE_OPT_STRING);
        cmdList.add(this.url);
        cmdList.add("--" + Opts.FACTORYRP_OPT_STRING);

        cmdList.add("--" + Opts.AUTHORIZATION_OPT_STRING);
        if (this.idAuthz != null) {
            cmdList.add(this.idAuthz);
        } else {
            cmdList.add("host");
        }

        final String[] cmd =
                (String[]) cmdList.toArray(new String[cmdList.size()]);

        return WorkspaceCLIMain.mainNoExit(cmd, this.pr);
    }

}
