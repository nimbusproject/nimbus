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

package org.globus.workspace.cmdutils;

import org.globus.workspace.WorkspaceException;

import java.util.ArrayList;

public class SSHUtil {

    /* The SSH and SCP executable to run on this node */
    private static String executable;
    private static String scpExe;

    private static String account;
    private static String sshIdFile;

    public static void setSshexe(String path) {
        if (executable != null) {
            return;
        }
        executable = path;
    }

    public static void setScpexe(String path) {
        if (scpExe != null) {
            return;
        }
        scpExe = path;
    }

    public static void setSshaccount(String acct) {
        if (account != null) {
            return;
        }
        if (acct != null && acct.trim().length() == 0) {
            return;
        }
        account = acct;
    }


    public static void setSshIdentityFile(String idfile) {
        if (sshIdFile != null) {
            return;
        }
        if (idfile != null && idfile.trim().length() == 0) {
            return;
        }
        sshIdFile = idfile;
    }

    public static String getScpexe() {
        return scpExe;
    }

    public static ArrayList constructSshCommand(String node)
            throws WorkspaceException {

        if (node == null) {
            throw new IllegalArgumentException("node for SSH is null");
        }

        final ArrayList cmd = new ArrayList();

        if (executable == null) {
            final String err =
                    "ssh executable path is not configured, but needed";
            throw new WorkspaceException(err);
        }

        cmd.add(executable);
        cmd.add("-n");
        cmd.add("-T");
        cmd.add("-o");
        cmd.add("BatchMode=yes");

        if (sshIdFile != null) {
            cmd.add("-i");
            cmd.add(sshIdFile);
        }

        if (account != null) {
            cmd.add(account + "@" + node);
        } else {
            cmd.add(node);
        }

        return cmd;
    }

    public static ArrayList constructScpCommandPrefix()
            throws WorkspaceException {

        final ArrayList cmd = new ArrayList();

        if (scpExe == null) {
            final String err =
                    "scp executable path is not configured, but needed";
            throw new WorkspaceException(err);
        }

        cmd.add(scpExe);

        if (sshIdFile != null) {
            cmd.add("-i");
            cmd.add(sshIdFile);
        }

        return cmd;
    }

    public static ArrayList constructScpCommandSuffix(String node,
                                                      String targetDir) {

        if (node == null) {
            throw new IllegalArgumentException("node for SCP is null");
        }

        final ArrayList cmd = new ArrayList();

        String arg;
        if (account != null && account.length() != 0) {
            arg = account + "@" + node;
        }
        else {
            arg = node;
        }

        arg += ":" + targetDir + "/";

        cmd.add(arg);

        return cmd;
    }

}
