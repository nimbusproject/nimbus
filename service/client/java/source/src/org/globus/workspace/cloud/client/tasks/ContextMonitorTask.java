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

import org.globus.workspace.common.print.Print;
import org.globus.workspace.client.WorkspaceCLIMain;
import org.globus.workspace.client.Opts;
import org.globus.workspace.cloud.client.cluster.KnownHostsTask;

import java.util.ArrayList;

import edu.emory.mathcs.backport.java.util.concurrent.Callable;

public class ContextMonitorTask implements Callable {

    private final String eprPath;
    private final String name;
    private final String idAuthz;
    private final String reportDir;
    private final long pollMs;
    private final String knownHostsFile;
    private final String knownHostsTasks;

    private final Print pr;

    public ContextMonitorTask(String ctxEprPath,
                              String identityAuthorization,
                              String shortName,
                              String reportDirectory,
                              String sshKnownHostsFile,
                              KnownHostsTask[] knownHostTasks,
                              long pollMilliseconds,
                              Print print) {
        if (print == null) {
            throw new IllegalArgumentException("print may not be null");
        }
        this.pr = print;
        this.eprPath = ctxEprPath;
        this.name = shortName;
        this.idAuthz = identityAuthorization;
        this.reportDir = reportDirectory;
        this.pollMs = pollMilliseconds;

        if (knownHostTasks == null || knownHostTasks.length == 0) {
            
            this.knownHostsFile = null;
            this.knownHostsTasks = null;

        } else {

            if (sshKnownHostsFile == null) {
                throw new IllegalArgumentException(
                        "known_hosts tasks but no known_hosts file");
            }
            this.knownHostsFile = sshKnownHostsFile;
            
            final StringBuffer buf = new StringBuffer(64);
            for (int i = 0; i < knownHostTasks.length; i++) {
                if (i > 0) {
                    buf.append(",");
                }
                final KnownHostsTask task = knownHostTasks[i];
                buf.append(task.ipaddr)
                   .append("::")
                   .append(task.interfaceName);

                if (task.printName != null) {
                    buf.append("::")
                       .append(task.printName);
                }
            }
            
            this.knownHostsTasks = buf.toString();
        }
    }

    public Object call() throws Exception {
        return new Integer(this.execute());
    }

    int execute() {

        // Could change this now to use the client API more directly, but
        // there is not a significant advantage to do so right now.

        final ArrayList cmdList = new ArrayList(4);

        if (this.pr.isDebugEnabled()) {
            cmdList.add("--" + Opts.DEBUG_OPT_STRING);
        }
        cmdList.add("--" + Opts.EPRFILE2_OPT_STRING);
        cmdList.add(this.eprPath);

        cmdList.add("--" + Opts.CTX_MONITOR_OPT_STRING);

        cmdList.add("--" + Opts.POLL_DELAY_OPT_STRING);
        cmdList.add(Long.toString(this.pollMs));

        if (this.reportDir != null) {
            cmdList.add("--" + Opts.REPORTDIR_OPT_STRING);
            cmdList.add(this.reportDir);
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

        if (this.knownHostsTasks != null) {
            cmdList.add("--" + Opts.SSHHOSTS_OPT_STRING);
            cmdList.add(this.knownHostsFile);
            cmdList.add("--" + Opts.ADJUST_SSHHOSTS_OPT_STRING);
            cmdList.add(this.knownHostsTasks);
        }

        final String[] cmd =
                (String[]) cmdList.toArray(new String[cmdList.size()]);

        // Could change this now to use the client API more directly.
        return WorkspaceCLIMain.mainNoExit(cmd, this.pr);
    }
}
