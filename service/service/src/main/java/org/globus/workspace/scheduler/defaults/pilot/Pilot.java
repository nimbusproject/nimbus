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

public interface Pilot {

    /**
     * All arguments are required.  No validation occurs here.
     *
     * @param memory MB to reserve
     * @param duration duration of slot, in seconds
     * @param graceperiod duration of LRM grace period, in seconds
     * @param slotid slot identifier
     * @param contact notification information for SSH notifications
     * @return ArrayList of cmdline tokens
     * @throws ManageException exception if input is invalid
     */
    public ArrayList constructReserveSlot(int memory,
                                          long duration,
                                          int graceperiod,
                                          String slotid,
                                          String contact)
            
            throws ManageException;

    /**
     * Common, optional arguments.  Only one of the ouput selectors should be
     * true.
     *
     * @param quiet suppress output
     * @param verbose verbose output
     * @param trace trace output
     * @param configpath absolute path of pilot configuration file override
     * @return ArrayList of cmdline tokens
     * @throws ManageException exception if input is invalid
     */
    public ArrayList constructCommon(boolean quiet,
                                     boolean verbose,
                                     boolean trace,
                                     String configpath)

            throws ManageException;

}
