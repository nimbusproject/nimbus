/*
 * Copyright 1999-2006 University of Chicago
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

package org.nimbustools.messaging.gt4_0.common;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.axis.types.Duration;
import org.oasis.wsrf.faults.BaseFaultType;
import org.globus.wsrf.utils.FaultHelper;

import java.util.List;
import java.util.LinkedList;

public class CommonUtil {

    private static final Log logger =
            LogFactory.getLog(CommonUtil.class.getName());

    public static final String PRETTY_CAUSES_NUM_KEY =
                                    "nimbus.errors.parent.number";

    public static final String PRETTY_CAUSES_STACKTRACES =
                                    "nimbus.errors.stacktraces";

    public static Duration minutesToDuration(int minutes) {
        /*
        * false - non-negative duration
        * 0 years
        * 0 months
        * 0 days
        * 0 hours
        * # minutes
        * 0 seconds
        */
        return new Duration(false, 0, 0, 0, 0, minutes, 0);
    }

    public static Duration secondsToDuration(int seconds) {
        /*
        * false - non-negative duration
        * 0 years
        * 0 months
        * 0 days
        * 0 hours
        * 0 minutes
        * # seconds
        */
        return new Duration(false, 0, 0, 0, 0, 0, seconds);
    }

    /**
     * Converts duration into seconds.  The current Duration class
     * implementation does not support proper <, >, or even equals
     * operations.  Choosing to convert all Durations off to seconds
     * for comparison rather than extend and fix the Duration class
     * implementation.
     *
     * Max duration is max(int) seconds. This is around ~25k days.
     *
     * Assumes years and month are ZERO and negative is FALSE.  Years
     * and months cannot be compared reliably because they can only be
     * partially ordered.
     *
     * TODO: possibly incorporate this restriction into the service schema?
     *
     * TODO: what to do if these ints are made to overflow?
     *
     * Uses the
     * {@link Double#doubleToLongBits(double) doubleToLongBits}
     * method.
     *
     * @param dur Positive duration instance w/o years or months.
     * @return Number of seconds in the Duration instance.
     * @throws InvalidDurationException problem
     */
    public static int durationToSeconds(Duration dur)
            throws InvalidDurationException {

        final int days;
        final int hours;
        final int mins;
        final double secs;
        final Long longsecs;
        final int intsecs;

        if (dur.isNegative()) {
            throw new InvalidDurationException("Duration can not be negative.");
        }

        if (dur.getYears() != 0) {
            throw new InvalidDurationException("Years in duration must be " +
                    "zero.");
        }

        if (dur.getMonths() != 0) {
            throw new InvalidDurationException("Months in duration must be " +
                    "zero.");
        }
        // Convert each to next lowest, until we get to seconds.
        days = dur.getDays();
        hours = dur.getHours() + days * 24;
        mins = dur.getMinutes() + hours * 60;
        secs = dur.getSeconds() + mins * 60;

        // Convert to int

        longsecs = new Long(new Double(secs).longValue());
        intsecs = longsecs.intValue();
        if (intsecs == Integer.MAX_VALUE) {
            throw new InvalidDurationException("Duration can not be " +
                    "longer than ~25k days.");
        }
        return intsecs;
    }


    /**
     * Ceilings to nearest minute.
     * @param dur duration
     * @return nearest minute (cieling)
     * @throws InvalidDurationException problem
     */
    public static int durationToMinutes(Duration dur)
            throws InvalidDurationException {

        final int seconds = durationToSeconds(dur);
        return secondsToMinutes(seconds);
    }

    // Ceilings to nearest minute.
    public static int secondsToMinutes(int seconds) {
        if (seconds == 0) {
            return 0;
        } else {
            int minutes = seconds/60;
            if (seconds % 60 > 0) {
                minutes += 1;
            }
            return minutes;
        }
    }


    /**
     * Buck stops here...  Using this mostly to include a message in a
     * close-to-user exception, where the Throwable gets set to that
     * exception's cause (for debug mode stacktrace).
     *
     * For example:
     *
     * <code>throw new Exception(CommonStrings.genericExceptionMessageWrapper(t), t);</code>
     *
     * @param any some Throwable you have not dealt with
     * @return some kind of message, even a non-informative one.
     * @see #recurseForSomeString(Throwable)
     */
    public static String genericExceptionMessageWrapper(Throwable any) {

        // Bad if a user ever sees this contingency, always needs fix for the
        // next release.
        final String fallback = "[[ Sorry, could not find any problem " +
                "description. See debugging output for stacktrace and please " +
                "inform development list, including debugging output if " +
                "you can.";

        final String fallback_suffix = " ]]";

        if (any == null) {
            return fallback + fallback_suffix;
        }

        String message = any.getMessage();
        if (message == null) {
            message = recurseForSomeString(any);
        }
        if (message == null) {
            message = fallback + "  Problem name: \"" +
                            any.getClass().getName() + "\"" + fallback_suffix;
        }
        return message;
    }

    /**
     * todo: method description
     *
     * Makes use of <code>BaseFaultType</code> awareness, via
     * <code>#faultString(BaseFaultType)</code>
     *
     * @param throwable may be null
     * @return null or first encountered error description string
     * @see #faultString(org.oasis.wsrf.faults.BaseFaultType)
     */
    public static String recurseForSomeString(Throwable throwable) {

        Throwable t = throwable;

        while (true) {

            if (t == null) {
                return null;
            }

            String msg = t.getMessage();

            if (msg != null) {
                return msg; // *** RETURN ***
            }

            if (t instanceof BaseFaultType) {
                msg = faultString((BaseFaultType)t);

                if (msg == null) {
                    t = t.getCause();
                } else {
                    return msg; // *** RETURN ***
                }

            } else {
                t = t.getCause();
            }

        }
    }

    /**
     * finds the root cause and prints its error description or if there is no
     * error description, just its class name
     *
     * Makes use of <code>BaseFaultType</code> awareness, via
     * <code>#faultString(BaseFaultType)</code>
     *
     * @param throwable may be null
     * @param suffixClassChain if true, class names of all errors involved are
     *        tacked on to the string like: [[ classname --> classname --> ...]]
     * @param numIncludedParentMsgs if suffixClassChain is true, this is the
     *        number of parents up from cause to print the exception messages
     *        for in the chain msg (not just the parent types)
     * @param lookForSysProp if true, the "nimbus.errors.parent.number" will
     *        be consulted and could possibly override the choice for
     *        numIncludedParentMsgs
     * @return LAST encountered error description/classname, only null if
     *         input is null (there is neither a type nor message to report)
     */
    public static String recurseForRootString(final Throwable throwable,
                                              final boolean suffixClassChain,
                                              final int numIncludedParentMsgs,
                                              final boolean lookForSysProp) {
        

        int realNumIncludedParentMsgs = numIncludedParentMsgs;
        if (lookForSysProp) {
            final String numParentsString =
                        System.getProperty(PRETTY_CAUSES_NUM_KEY);

            try {
                if (numParentsString != null
                        && numParentsString.trim().length() != 0) {
                   realNumIncludedParentMsgs =
                           Integer.parseInt(numParentsString);
                }
            } catch (Throwable t2) {
                logger.error("Could not parse a number from the '" +
                        PRETTY_CAUSES_NUM_KEY + "' system property.");
            }
        }

        final String alsoStackTracesStr =
                        System.getProperty(PRETTY_CAUSES_STACKTRACES);

        boolean alsoStackTraces = false;
        if (alsoStackTracesStr != null &&
                alsoStackTracesStr.trim().equalsIgnoreCase("true")) {
            alsoStackTraces = true;
        }

        final List parentMsgs = new LinkedList();
        final List parentTypes = new LinkedList();
        final List parentStacktraces = new LinkedList();
        final StringBuffer buf = new StringBuffer();

        int numDeep = 0;

        Throwable t = throwable;
        Throwable lastt = null;

        while (true) {

            if (t == null) {
                return _doneRecursingForRootString(lastt,
                                                   numDeep,
                                                   buf,
                                                   suffixClassChain,
                                                   parentMsgs,
                                                   parentTypes,
                                                   realNumIncludedParentMsgs,
                                                   alsoStackTraces,
                                                   parentStacktraces);
            }

            numDeep += 1;

            String thisMsg = t.getMessage();
            if (thisMsg == null && t instanceof BaseFaultType) {
                thisMsg = faultString((BaseFaultType)t);
            }

            if (thisMsg == null) {

                if (t instanceof RuntimeException) {

                    final StringBuffer lilStack = new StringBuffer();
                    lilStack.append("No message, but RuntimeException so " +
                            "including part of the stack trace: [[ ");
                    lilStack.append("\n").append(t.getClass().getName());
                    final StackTraceElement[] runTimeTraces = t.getStackTrace();
                    for (int i = 0; i < runTimeTraces.length; i++) {
                        final StackTraceElement runTimeTrace = runTimeTraces[i];
                        lilStack.append("\n\t at ").append(runTimeTrace);

                        if (i == 10) {
                            final int remaining = runTimeTraces.length - i;
                            if (remaining > 0) {
                                lilStack.append("\n ...")
                                        .append(remaining)
                                        .append(" more.");
                            }
                            break;
                        }
                    }

                    lilStack.append("\n ]]");
                    thisMsg = lilStack.toString();

                } else {
                    thisMsg = "no message";
                }
            }

            final String thisType = t.getClass().getName();

            final Throwable thisCause = t.getCause();

            if (thisCause == null) {
                lastt = t;
                t = null;
                buf.append(thisMsg).append(" (").append(thisType).append(")");
            } else {
                // keep going deeper
                lastt = t;
                t = thisCause;
                parentMsgs.add(thisMsg);
                parentTypes.add(thisType);
                parentStacktraces.add(t.getStackTrace());
            }
        }
    }

    private static String _doneRecursingForRootString(
                                        final Throwable lastt,
                                        final int numDeep,
                                        final StringBuffer buf,
                                        final boolean suffixClassChain,
                                        final List parentMsgs,
                                        final List parentTypes,
                                        final int includedParentMsgs,
                                        final boolean alsoStackTraces,
                                        final List parentStacktraces) {

        if (numDeep == 0) {
            return null; // *** EARLY RETURN ***
        }
        
        if (numDeep == 1) {
            return buf.toString(); // *** EARLY RETURN ***
        }

        final String main = buf.toString();

        if (!suffixClassChain) {
            return main; // *** EARLY RETURN ***
        }

        final int numMsgs = parentMsgs.size();
        if (numMsgs != parentTypes.size()) {
            return main + "\n[[**** PROBLEM WITH ATTAINING CAUSE CHAIN: " +
                    "parentMsgs and parentTypes sizes do not match ****]]";
        }

        final StringBuffer retbuf = new StringBuffer(main);
        retbuf.append("\n[[**** Cause chain report ****]]");

        for (int i = 0; i < numMsgs; i++) {
            retbuf.append("\n  ");
            for (int j = 0; j < i; j++) {
                retbuf.append(" ");
            }
            final String type = (String) parentTypes.get(i);
            retbuf.append("caused by (#").append(i+1).append("): ").append(type);
        }
        retbuf.append("\n");
        
        int startIncluding = numMsgs - includedParentMsgs;
        if (startIncluding < 0) {
            startIncluding = 0;
        }

        for (int i = 0; i < numMsgs; i++) {
            if (i >= startIncluding) {
                retbuf.append("\n");
                final String msg = (String) parentMsgs.get(i);
                retbuf.append("Message for #")
                      .append(i+1)
                      .append(":\n")
                      .append(msg);
                if (includedParentMsgs > 1) {
                    retbuf.append("\n========================================");
                }

                if (alsoStackTraces) {

                    final String thisTraceName = "Stacktrace for #" + (i+1);
                    
                    retbuf.append("\n\n")
                          .append(thisTraceName)
                          .append(":\n")
                          .append(msg);

                    final StackTraceElement[] traces = 
                            (StackTraceElement[]) parentStacktraces.get(i);

                    if (traces == null || traces.length == 0) {
                        retbuf.append("     no stacktrace available (?)");
                    } else {
                        for (int j = 0; j < traces.length; j++) {
                            final StackTraceElement trace = traces[j];
                            retbuf.append("\tat ").append(trace).append("\n");
                        }
                    }

                    retbuf.append("\n(END ").append(thisTraceName).append(")");

                    if (includedParentMsgs > 1) {
                        retbuf.append("\n========================================");
                    }
                    retbuf.append("\n");
                }
            }
        }

        retbuf.append("\n\nOriginal message:\n").append(main);

        if (alsoStackTraces) {
            retbuf.append("\nStacktrace for original problem: ");
            if (lastt == null) {
                retbuf.append("     no throwable available (?)");
            } else {

                final StackTraceElement[] traces = lastt.getStackTrace();
                if (traces == null || traces.length == 0) {
                    retbuf.append("     no stacktrace available (?)");
                } else {
                    for (int j = 0; j < traces.length; j++) {
                        final StackTraceElement trace = traces[j];
                        retbuf.append("\tat ").append(trace).append("\n");
                    }
                }
            }
            retbuf.append("\n(END stacktrace for original problem)");
        }
        
        retbuf.append("\n[[**** end of cause chain report ****]]\n\n");
        return retbuf.toString();
    }


    /**
     * todo: method description
     *
     * @param e may be null
     * @return null if input is null, first encountered error description, or class name
     */
    public static String faultString(BaseFaultType e) {

        if (e == null) {
            return null;
        }

        final FaultHelper helper = new FaultHelper(e);
        final String[] descriptions = helper.getDescription();

        if (descriptions == null || descriptions.length == 0) {

            // Decided that anything recursing into BaseFaultType causes for
            // strings via #getCause(), such as #recurseForSomeString(), is
            // not going to find anything.

            return "Fault without any problem description: " +
                                            e.getClass().getName();
        }

        final StringBuffer buf = new StringBuffer(2048);

        buf.append(descriptions[0]);

        if (descriptions.length > 1) {
            for (int i = 1; i < descriptions.length; i++) {
                buf.append(" || ")
                   .append(descriptions[i]);
            }
        }
        return buf.toString();
    }
}
