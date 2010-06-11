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

/**
 * Most recent testing: Torque 2.2.1
 *
 * Should move to non-static
 */
public class TorqueUtil {

    /* The Torque submission executable (qsub) to run on container node */
    private final String submit;

    /* The Torque deletion executable (qdel) to run on container node */
    private final String delete;

    public TorqueUtil(String submitExe, String deleteExe) {
        if (submitExe == null) {
            throw new IllegalArgumentException("submitExe may not be null");
        }
        this.submit = submitExe;
        
        if (deleteExe == null) {
            throw new IllegalArgumentException("deleteExe may not be null");
        }
        this.delete = deleteExe;
    }


    /**
     * Construct qsub command from given arguments
     *
     * Someone (anyone) is responsible for initializing first.  Caller should
     * check if initialized before use.
     *
     * @param destination if not null, destination (like a queue name) is sent,
     *        For Torque 2.1.8 this should be one of three forms: "queue",
     *        "@server", or "queue@server"
     * @param memoryMB memory to request
     * @param nodenum nodes to request
     * @param ppn processors per node
     * @param walltimeSeconds requested walltime
     * @param extraProperties extra properties, appended after ppn
     *        e.g. if string is "xen" this will happen: -l nodes=1:ppn=2:xen
     * @param stdoutPath path to job's stdout (if rcp/scp is configured in
     *        Torque). Non-absolute path results in deployment specific
     *        results.  If null and therefore not added to command, Torque
     *        2.1.8 will pick a default name for this file. Read Torque docs
     *        for "-o path_name", can include e.g. hostname prefix.
     * @param reRunnable job can survive restarts?
     * @param mail if mail is true, MAIL_AT_EXIT and MAIL_AT_ABORT will be set,
     *             otherwise NO_MAIL will be set
     * @param account string to apply to job
     * @return ArrayList of cmdline tokens
     * @throws org.globus.workspace.WorkspaceException problem with parameters or initialization
     */
    public ArrayList constructQsub(String destination,
                                   int memoryMB,
                                   int nodenum,
                                   int ppn,
                                   long walltimeSeconds,
                                   String extraProperties,
                                   String stdoutPath,
                                   boolean reRunnable,
                                   boolean mail,
                                   String account)
            throws WorkspaceException {

        if (memoryMB < 1) {
            final String err = "invalid memory " +
                    "request: " + Integer.toString(memoryMB);
            throw new WorkspaceException(err);
        }

        if (ppn < 1) {
            final String err = "invalid processors per node " +
                    "request: " + Integer.toString(ppn);
            throw new WorkspaceException(err);
        }

        if (walltimeSeconds < 1) {
            final String err = "invalid walltime request: " +
                    Long.toString(walltimeSeconds);
            throw new WorkspaceException(err);
        }

        final ArrayList cmd = new ArrayList(20);

        cmd.add(this.submit);

        // join stderr and stdout into stdout
        cmd.add("-j");
        cmd.add("oe"); // order matters, eo makes join go into stderr

        // default for Torque is true, but explicitly setting either in case
        // that default ever changes
        cmd.add("-r");
        if (reRunnable) {
            cmd.add("y");
        } else {
            cmd.add("n");
        }

        cmd.add("-m");
        if (mail) {
            cmd.add("ea"); // MAIL_AT_EXIT | MAIL_AT_ABORT
        } else {
            cmd.add("n"); // NO_MAIL
        }

        if (destination != null) {
            cmd.add("-q");
            cmd.add(destination);
        }

        cmd.add("-l");
        String props = "nodes=" + nodenum + ":ppn=" + ppn;
        if (extraProperties != null && extraProperties.trim().length() != 0) {
            props = props + ":" + extraProperties;
        }
        cmd.add(props);
        cmd.add("-l");
        cmd.add("walltime=" + walltimeFromSeconds(walltimeSeconds));

        cmd.add("-l");
        cmd.add("mem=" + Integer.toString(memoryMB) + "mb");

        if (stdoutPath != null) {
            cmd.add("-o");
            cmd.add(stdoutPath);
        }

        if (account != null) {
            cmd.add("-A");
            cmd.add(account);
        }

        return cmd;
    }

    /**
     * Construct qsub command from given arguments
     *
     * Someone (anyone) is responsible for initializing first.  Caller should
     * check if initialized before use.
     *
     * @param jobid Torque job ID
     * @return ArrayList of cmdline tokens
     * @throws org.globus.workspace.WorkspaceException exc
     */
    public String[] constructQdel(String jobid)
            throws WorkspaceException {

        if (jobid == null) {
            throw new WorkspaceException("no Torque ID was provided");
        }

        return new String[]{this.delete, jobid};
    }

    /**
     * Converts seconds into a human readable walltime string with as many
     * H's as necessary. Mainly doing this for human readable logging.
     * @param walltimeSecs seconds
     * @return time string
     */
    public static String walltimeFromSeconds(long walltimeSecs) {
        if (walltimeSecs < 0 || walltimeSecs == 0) {
            return "00:00:00";
        }

        final long hours = walltimeSecs / 3600;
        final long hoursDiff = walltimeSecs - hours * 3600;
        final long minutes = hoursDiff / 60;
        final long seconds = walltimeSecs - hours * 3600 - minutes * 60;

        final StringBuffer buf = new StringBuffer(16);
        appenNum(buf, hours, false);
        appenNum(buf, minutes, false);
        appenNum(buf, seconds, true);

        return buf.toString();
    }

    private static void appenNum(StringBuffer buf, long num, boolean end) {
        if (num == 0 && !end) {
            buf.append("00");
        } else if (num < 10) {
            buf.append("0")
               .append(num);
        } else {
            buf.append(num);
        }
        if (!end) {
            buf.append(":");
        }
    }
}
