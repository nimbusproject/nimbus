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
import org.globus.workspace.common.print.Print;
import org.globus.workspace.client.Opts;
import org.globus.workspace.client.WorkspaceCLIMain;

import java.util.ArrayList;

public class CreateContextTask implements Callable {

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private final String brokerURL;
    private final String createdContextEprFilePath;
    private final String brokerContactFilePath;
    private final boolean expectInjections;
    private final String idAuthz;

    private final Print pr;

    
    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    public CreateContextTask(String brokerURL,
                             String createdContextEprFilePath,
                             String brokerContactFilePath,
                             boolean expectInjections,
                             String identityAuthorization,
                             Print print) {
        
        if (print == null) {
            throw new IllegalArgumentException("print may not be null");
        }
        this.pr = print;

        this.brokerURL = brokerURL;
        this.createdContextEprFilePath = createdContextEprFilePath;
        this.brokerContactFilePath = brokerContactFilePath;
        this.expectInjections = expectInjections;
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

        final ArrayList cmdList = new ArrayList(10);

        if (this.pr.isDebugEnabled()) {
            cmdList.add("--" + Opts.DEBUG_OPT_STRING);
        }

        if (this.expectInjections) {
            cmdList.add("--" + Opts.CTX_CREATE_INJECTABLE_OPT_STRING);
        } else {
            cmdList.add("--" + Opts.CTX_CREATE_OPT_STRING);
        }
        
        cmdList.add("--" + Opts.SERVICE2_OPT_STRING);
        cmdList.add(this.brokerURL);

        cmdList.add("--" + Opts.FILE_OPT_STRING);
        cmdList.add(this.createdContextEprFilePath);

        cmdList.add("--" + Opts.CTX_CONTACT_PATH_OPT_STRING);
        cmdList.add(this.brokerContactFilePath);

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
