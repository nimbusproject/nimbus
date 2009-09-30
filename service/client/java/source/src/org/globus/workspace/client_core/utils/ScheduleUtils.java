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

package org.globus.workspace.client_core.utils;

import org.globus.workspace.client_core.repr.Workspace;
import org.globus.workspace.client_core.repr.Schedule;
import org.globus.workspace.common.print.Print;
import org.globus.workspace.client_core.print.PrCodes;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Calendar;

public class ScheduleUtils {

    private static final Log logger =
            LogFactory.getLog(ScheduleUtils.class.getName());

    private static final String NO_TIME = "will be set after lease is secured";

    public static void instanceCreateResultSchedulePrint(Print pr,
                                                         Workspace workspace) {

        if (pr == null) {
            throw new IllegalArgumentException("print may not be null");
        }

        if (!pr.enabled()) {
            return; // *** EARLY RETURN ***
        }
        
        if (workspace == null) {
            throw new IllegalArgumentException("workspace may not be null");
        }

        final Schedule current = workspace.getCurrentSchedule();
        if (current == null) {

            final String err = "No schedule to print?";
            if (pr.useThis()) {
                pr.errln(err);
            } else if (pr.useLogging()) {
                logger.error(err);
            }

            return; // *** EARLY RETURN ***
        }

        _instSchedStarted(pr, current);
        _instSchedDuration(pr, current);
        _instSchedShutdown(pr, current);
        _instSchedTerm(pr, current);
    }

    private static void _instSchedDuration(Print pr, Schedule c) {
        final String durationMsg = getDurationMessage(c, true);
        if (pr.useThis()) {
            pr.infoln(PrCodes.CREATE__INSTANCE_CREATING_INIITIAL_DURATION,
                      durationMsg);
        } else if (pr.useLogging()) {
            logger.info(durationMsg);
        }
    }

    private static void _instSchedStarted(Print pr, Schedule c) {
        final String startedMsg = getStartTimeMessage(c, true);
        if (pr.useThis()) {
            pr.infoln(PrCodes.CREATE__INSTANCE_CREATING_INIITIAL_START_TIME,
                      startedMsg);
        } else if (pr.useLogging()) {
            logger.info(startedMsg);
        }
    }

    private static void _instSchedShutdown(Print pr, Schedule c) {
        final String stopMsg = getShutdownMessage(c, true);
        if (pr.useThis()) {
            pr.infoln(PrCodes.CREATE__INSTANCE_CREATING_INIITIAL_SHUTDOWN_TIME,
                      stopMsg);
        } else if (pr.useLogging()) {
            logger.info(stopMsg);
        }
    }

    private static void _instSchedTerm(Print pr, Schedule c) {
        final String termMsg = getTerminationMessage(c, true);
        if (pr.useThis()) {
            pr.infoln(PrCodes.CREATE__INSTANCE_CREATING_INIITIAL_TERMINATION_TIME,
                      termMsg);
        } else if (pr.useLogging()) {
            logger.info(termMsg);
        }
    }

    public static String getDurationMessage(Schedule schedule) {
        return getDurationMessage(schedule, false);
    }

    public static String getDurationMessage(Schedule schedule,
                                            boolean aligned) {

        if (schedule == null) {
            return null; // *** EARLY RETURN ***
        }
        
        final int minutes = schedule.getDurationMinutes();

        final String name = "Duration: ";
        final String prefix;
        if (aligned) {
            prefix = "         " + name;
        } else {
            prefix = name;
        }
        
        final String durationMsg;
        if (schedule.isMinutesExact()) {
            durationMsg = prefix  + minutes + " minutes.";
        } else {
            durationMsg = prefix + "~" + minutes + " minutes (" +
                    schedule.getDurationSeconds() + " seconds).";
        }

        return durationMsg;
    }

    public static String getStartTimeMessage(Schedule schedule) {
        return getStartTimeMessage(schedule, false);
    }

    public static String getStartTimeMessage(Schedule schedule,
                                             boolean aligned) {

        if (schedule == null) {
            return null; // *** EARLY RETURN ***
        }

        final Calendar started = schedule.getActualInstantiationTime();

        final String name = "Start time: ";
        final String prefix;
        if (aligned) {
            prefix = "       " + name;
        } else {
            prefix = name;
        }

        final String startedMsg;
        if (started != null) {
            startedMsg = prefix + started.getTime().toString();
        } else {
            startedMsg = prefix + NO_TIME;
        }

        return startedMsg;
    }

    public static String getShutdownMessage(Schedule schedule) {
        return getShutdownMessage(schedule, false);
    }

    public static String getShutdownMessage(Schedule schedule,
                                            boolean aligned) {

        if (schedule == null) {
            return null; // *** EARLY RETURN ***
        }

        final Calendar started = schedule.getActualInstantiationTime();

        final String name = "Shutdown time: ";
        final String prefix;
        if (aligned) {
            prefix = "    " + name;
        } else {
            prefix = name;
        }

        final String stopMsg;
        if (started != null) {
            final Calendar stopTime = (Calendar) started.clone();
            stopTime.add(Calendar.SECOND, schedule.getDurationSeconds());
            stopMsg = prefix + stopTime.getTime().toString();
        } else {
            stopMsg = prefix + NO_TIME;
        }

        return stopMsg;
    }

    public static String getTerminationMessage(Schedule schedule) {
        return getTerminationMessage(schedule, false);
    }

    public static String getTerminationMessage(Schedule schedule,
                                               boolean aligned) {

        if (schedule == null) {
            return null; // *** EARLY RETURN ***
        }

        final String name = "Termination time: ";
        final String prefix;
        if (aligned) {
            prefix = " " + name;
        } else {
            prefix = name;
        }

        final Calendar term = schedule.getActualTerminationTime();
        final String termMsg;
        if (term != null) {
            termMsg = prefix + term.getTime().toString();
        } else {
            termMsg = prefix + NO_TIME;
        }

        return termMsg;
    }

    public static boolean isWholeMinute(int seconds) {
        if (seconds < 0) {
            return false;
        }

        if (seconds == 0) {
            return true;
        }

        return seconds % 60 == 0;
    }
}
