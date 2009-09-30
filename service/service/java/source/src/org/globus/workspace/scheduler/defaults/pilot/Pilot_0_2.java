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

package org.globus.workspace.scheduler.defaults.pilot;

import org.nimbustools.api.services.rm.ManageException;

import java.util.ArrayList;

/**
 * Result of methods can be combined in any order, workspace-pilot 0.2
 * does not have any positional parameters.
 */
public class Pilot_0_2 implements Pilot {

    public ArrayList constructReserveSlot(int memory,
                                          long duration,
                                          int graceperiod,
                                          String slotid,
                                          String contact)
                            throws ManageException {

        if (slotid == null) {
            throw new ManageException("slotid must be non-null");
        }
        if (slotid.length() == 0) {
            throw new ManageException("slotid must be non-empty");
        }
        if (contact == null) {
            throw new ManageException("contact must be non-null");
        }
        if (contact.length() == 0) {
            throw new ManageException("contact must be non-empty");
        }

        final ArrayList cmd = new ArrayList(12);

        cmd.add("--reserveslot");
        cmd.add("-m");
        cmd.add(Integer.toString(memory));
        cmd.add("-d");
        cmd.add(Long.toString(duration));
        cmd.add("-g");
        cmd.add(Integer.toString(graceperiod));
        cmd.add("-i");
        cmd.add(slotid);
        cmd.add("-c");
        cmd.add(contact);
        
        return cmd;
    }

    public ArrayList constructCommon(boolean quiet,
                                     boolean verbose,
                                     boolean trace,
                                     String configpath)
            throws ManageException {

        if (quiet && (verbose || trace)) {
            throw new ManageException("only one output verbosity " +
                    "selector should be passed in == true");
        }

        if (verbose && trace) {
            throw new ManageException("only one output verbosity " +
                    "selector should be passed in == true");
        }

        final ArrayList cmd = new ArrayList(4);

        if (quiet) {
            cmd.add("-q");
        } else if (verbose) {
            cmd.add("-v");
        } else if (trace) {
            cmd.add("-t");
        }

        if (configpath != null && configpath.length() != 0) {
            cmd.add("-p");
            cmd.add(configpath);
        }

        return cmd;
    }

}
