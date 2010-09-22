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

package org.globus.workspace.service.impls.site;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.SimpleTimeZone;

public class PilotNotificationUtil {

    private static final Log logger =
           LogFactory.getLog(PilotNotificationUtil.class.getName());

    public static final String STATE_PILOT_TEST = "pilot-test";
    public static final String STATE_PILOT_RESERVED = "pilot-reserved";
    public static final String STATE_PILOT_UNRESERVING = "pilot-unreserving";
    public static final String STATE_PILOT_EARLY = "pilot-earlyunreserving";
    public static final String STATE_PILOT_KILLED = "pilot-killed";

    static boolean oneNotification(String name,
                                   String state,
                                   int code,
                                   String message,
                                   String log_msg,
                                   SlotPollCallback slotcall)
                throws Exception {

        if (name == null) {
            throw new Exception("name may not be null: " + log_msg);
        }

        if (state == null) {
            throw new Exception("state may not be null: " + log_msg);
        }

        if (!isPilotState(state)) {
            throw new Exception("state is not a pilot state: " + log_msg);
        }

        final String[] nameParts = name.split("\\+\\+\\+");
        if (nameParts.length != 2) {
            String msg = "name is not a valid pilot name, length split by " +
                    "+++ != 2, received: " + log_msg;
            throw new Exception(msg);
        }

        // the way split is called, neither of these will be empty or null
        final String slotid = nameParts[0];
        final String hostname = nameParts[1];

        if (state.equals(STATE_PILOT_TEST)) {
            String msg2 = "Received testing notification from " +
                          "slotid = '" + slotid + "', host = '" + hostname +
                          "', code = " + code;
            if (message != null) {
                msg2 += ", message = '" + message + "'";
            } else {
                msg2 += " (no message sent)";
            }
            logger.info(msg2);
            return false;
        }

        if (state.equals(STATE_PILOT_RESERVED)) {
            if (code == 0) {
                Calendar cal = parseTimestamp(name, message);
                slotcall.reserved(slotid, hostname, cal);
            } else {
                slotcall.errorReserving(slotid, hostname, message);
            }
        }

        if (state.equals(STATE_PILOT_UNRESERVING)) {
            if (code == 0) {
                slotcall.unreserving(slotid, hostname);
            } else {
                slotcall.errorUnreserving(slotid, hostname, message);
            }
        }

        if (state.equals(STATE_PILOT_EARLY)) {
            if (code == 0) {
                Calendar cal = parseTimestamp(name, message);
                slotcall.earlyUnreserving(slotid, hostname, cal);
            } else {
                slotcall.errorEarlyUnreserving(slotid, hostname, message);
            }
        }

        if (state.equals(STATE_PILOT_KILLED)) {
            if (message == null) {
                logger.error("pilot '" + name + "' sent killed VM " +
                             "notification without a list of killed VMs?" +
                             " Not notifying.");
                return false;
            }

            if (message.indexOf(' ') >= 0) {
                logger.error("pilot '" + name + "' sent killed VM " +
                             "notification with non-conforming list of " +
                             "killed VMs?  Not notifying.  Message: " +
                             message);
                return false;
            }

            slotcall.kills(slotid, hostname, message.split(","));
        }

        return true;

    }

    static boolean isPilotState(String state) {
        return state != null && state.startsWith("pilot");
    }

    static Calendar parseTimestamp(String pilot,
                                   String timestamp) throws Exception {

        String errmsg = "non conforming timestamp sent by pilot '" +
                    pilot + "', timestamp = '" + timestamp + "', ";

        if (timestamp == null) {
            throw new Exception(errmsg + "timestamp may not be null");
        }

        final String[] tokens = timestamp.trim().split("-");

        if (tokens.length != 6) {
            throw new Exception(errmsg + "token length = " + tokens.length);
        }

        final int year;
        final int month;
        final int day;
        final int hour;
        final int minute;
        final int second;
        try {
            year = Integer.parseInt(tokens[0]);
            int mo = Integer.parseInt(tokens[1]);
            month = mo - 1; // Calendar needs month to start from zero
            day = Integer.parseInt(tokens[2]);
            hour = Integer.parseInt(tokens[3]);
            minute = Integer.parseInt(tokens[4]);
            second = Integer.parseInt(tokens[5]);
        } catch (NumberFormatException e) {
            throw new Exception(errmsg + "a token is not an integer");
        }

        // negative numbers are impossible because we split by -

        // calendar is OK with greater numbers than expected (it just
        // increases its internal timestamp by that much); we're not

        if (month > 11) {
            throw new Exception(errmsg + "month > 11");
        }

        if (day > 31) {
            throw new Exception(errmsg + "day > 31");
        }

        if (hour > 23) {
            throw new Exception(errmsg + "hour > 23");
        }

        if (minute > 59) {
            throw new Exception(errmsg + "minute > 59");
        }

        // 61 is intentional, leaps etc...
        if (second > 61) {
            throw new Exception(errmsg + "second > 61");
        }

        final TimeZone tz = new SimpleTimeZone(0, "GMT");
        final Calendar cal = Calendar.getInstance(tz);
        cal.clear();
        cal.set(year, month, day, hour, minute, second);
        return cal;
    }
}
