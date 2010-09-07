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
import org.globus.workspace.client.Opts;
import org.globus.workspace.client.WorkspaceCLIMain;
import org.globus.workspace.common.print.Print;

import java.util.ArrayList;

/**
 * Runs the workspace client, files are created ahead of time
 * @see org.globus.workspace.cloud.client.util.ExecuteUtil
 */
public class RunTask implements Callable {


    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    public static final String LOG_FILE_NAME = "run";
    public static final String DEBUG_LOG_FILE_NAME = "run-debug";


    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private final String epr;
    private final String name;
    private final String factory;
    private final String metadata;
    private final String deployment;
    private final String sshfile;
    private final long pollMs;
    private final boolean noStateChecks;
    private final String idAuthz;
    private final String exitState;
    private final String ensembleEprPath;
    private final boolean newEnsemble;
    private final boolean forceGroupPrint;
    private final String mdUserDataPath;
    private final String ipIdDir;

    private final Print pr;



    public String getEnsembleEprPath() {
        return ensembleEprPath;
    }

    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    public RunTask(String eprPath,
                   String workspaceFactoryURL,
                   String metadataPath,
                   String deploymentPath,
                   String sshPubPath,
                   long pollMilliseconds,
                   boolean disableAllStateChecks,
                   String shortName,
                   String identityAuthorization,
                   String exitStateStr,
                   String ensembleEprPath,
                   boolean newEnsemble,
                   boolean forceGroupPrint,
                   String mdUserDataPath,
                   String ipIdDir,
                   Print print) {

        if (print == null) {
            throw new IllegalArgumentException("print may not be null");
        }
        this.pr = print;

        if (eprPath == null) {
            throw new IllegalArgumentException(
                                "eprPath may not be null");
        }
        this.epr = eprPath;

        this.name = shortName;

        this.idAuthz = identityAuthorization;

        if (workspaceFactoryURL == null) {
            throw new IllegalArgumentException(
                                "workspaceFactoryURL may not be null");
        }
        this.factory = workspaceFactoryURL;

        if (metadataPath == null) {
            throw new IllegalArgumentException(
                                "metadataPath may not be null");
        }
        this.metadata = metadataPath;

        if (deploymentPath == null) {
            throw new IllegalArgumentException(
                                "deploymentPath may not be null");
        }
        this.deployment = deploymentPath;

        this.pollMs = pollMilliseconds;
        this.noStateChecks = disableAllStateChecks;
        this.forceGroupPrint = forceGroupPrint;

        // may be null
        this.sshfile = sshPubPath;

        // may be null, means don't send the parameter
        this.exitState = exitStateStr;

        // may be null, means not an ensemble related request
        this.ensembleEprPath = ensembleEprPath;
        this.newEnsemble = newEnsemble;

        // may be null
        this.mdUserDataPath = mdUserDataPath;
        this.ipIdDir = ipIdDir;
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

        final ArrayList cmdList = new ArrayList(16);

        if (this.pr.isDebugEnabled()) {
            cmdList.add("--" + Opts.DEBUG_OPT_STRING);
        }
        cmdList.add("--" + Opts.DEPLOY_OPT_STRING);
        cmdList.add("--" + Opts.FILE_OPT_STRING);
        cmdList.add(this.epr);
        cmdList.add("--" + Opts.SERVICE_OPT_STRING);
        cmdList.add(this.factory);
        cmdList.add("--" + Opts.METADATA_OPT_STRING_LONG);
        cmdList.add(this.metadata);
        cmdList.add("--" + Opts.REQUEST_OPT_STRING_LONG);
        cmdList.add(this.deployment);

        if (this.exitState != null) {
            cmdList.add("--" + Opts.EXIT_STATE_OPT_STRING);
            cmdList.add(this.exitState);
        }

        cmdList.add("--" + Opts.AUTHORIZATION_OPT_STRING);
        if (this.idAuthz != null) {
            cmdList.add(this.idAuthz);
        } else {
            cmdList.add("host");
        }

        if (this.name != null) {
            cmdList.add("--" + Opts.DISPLAY_NAME_OPT_STRING);
            cmdList.add(this.name);
        }

        if (this.sshfile != null) {
            cmdList.add("--" + Opts.SSHFILE_OPT_STRING);
            cmdList.add(this.sshfile);
        }

        if (this.mdUserDataPath != null) {
            cmdList.add("--" + Opts.MD_USERDATA_OPT_STRING);
            cmdList.add(this.mdUserDataPath);
        }

        if (this.noStateChecks) {
            cmdList.add("--" + Opts.NONOTIFY_OPT_STRING);
        } else {
            if (this.pollMs > 0) {
                cmdList.add("--" + Opts.POLL_DELAY_OPT_STRING);
                cmdList.add(Long.toString(this.pollMs));
            }
            // if neither --poll-delay and --nosubscriptions are set,
            // notifications will be used
        }

        if (this.ensembleEprPath != null) {
            if (this.newEnsemble) {
                cmdList.add("--" + Opts.ENSEMBLE_NEW_OPT_STRING);
            } else {
                cmdList.add("--" + Opts.ENSEMBLE_JOIN_OPT_STRING);
            }
            cmdList.add(this.ensembleEprPath);
        }

        if (this.ipIdDir != null) {
            cmdList.add("--" + Opts.EPR_ID_DIR_OPT_STRING);
            cmdList.add(this.ipIdDir);
        }

        if (this.forceGroupPrint) {
            cmdList.add("--" + Opts.GROUP_PRINT_OPT_STRING);
        }

        final String[] cmd =
                (String[]) cmdList.toArray(new String[cmdList.size()]);

        return WorkspaceCLIMain.mainNoExit(cmd, this.pr);
    }
}
