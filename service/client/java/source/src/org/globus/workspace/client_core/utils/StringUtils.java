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

import org.nimbustools.messaging.gt4_0.generated.types.OptionalParameters_Type;
import org.nimbustools.messaging.gt4_0.generated.types.TransferRequest_Type;
import org.globus.workspace.client_core.repr.Workspace;
import org.globus.workspace.client_core.repr.State;
import org.globus.workspace.client_core.repr.Schedule;
import org.nimbustools.messaging.gt4_0.common.CommonUtil;
import org.globus.wsrf.encoding.ObjectSerializer;
import org.globus.wsrf.encoding.SerializationException;
import org.apache.axis.message.addressing.EndpointReferenceType;

import javax.xml.namespace.QName;
import java.io.StringWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;

public class StringUtils {

    public static final String[] EMPTY_STRING_ARRAY = new String[0];
    public static final String SYSTEM_LINE_SEPARATOR =
                                        System.getProperty("line.separator");

    public static String axisBeanToString(Object o, QName qName)
            throws SerializationException, IOException {

        if (o == null || qName == null) {
            return null;
        }

        StringWriter writer = null;
        final String ret;
        try {
            writer = new StringWriter();
            writer.write(ObjectSerializer.toString(o, qName));
            ret = writer.toString();
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
        return ret;
    }

    public static String eprToString(EndpointReferenceType epr)
            throws SerializationException, IOException {

        final QName qName = new QName("", "EPR");
        return axisBeanToString(epr, qName);
    }

    public static String commonAtServiceAddressSuffix(EndpointReferenceType epr) {
        if (epr == null || epr.getAddress() == null) {
            return null;
        }
        return " @ \"" + epr.getAddress().toString() + "\"";
    }

    public static String debugDumpOptional(OptionalParameters_Type opts) {

        if (opts == null) {
            throw new IllegalArgumentException("opts may not be null");
        }

        final StringBuffer buf = new StringBuffer(2048); 

        buf.append("Optional parameters:\n");

        TransferRequest_Type xfer = opts.getStageIn();
        if (xfer != null) {
            buf.append("  - StageIn:\n");
            dumpXfer(buf, xfer);
        }
        xfer = opts.getStageOut();
        if (xfer != null) {
            buf.append("  - StageOut:\n");
            dumpXfer(buf, xfer);
        }

        return buf.toString();
    }

    private static void dumpXfer(StringBuffer buf,
                                 TransferRequest_Type xfer) {
        if (xfer.getSourceURL() != null) {
            buf.append("      Source URL: ");
            buf.append(xfer.getSourceURL().toString());
        }
        if (xfer.getDestURL() != null) {
            buf.append("      Dest URL: ");
            buf.append(xfer.getDestURL().toString());
        }
        if (xfer.getServiceEndpoint() != null) {
            buf.append("      Service URL: ");
            buf.append(xfer.getServiceEndpoint().toString());
        }
        if (xfer.getStagingCredential() != null) {
            buf.append("      Staging credential: ");
            buf.append(xfer.getStagingCredential().toString());
        }
        if (xfer.getTransferCredential() != null) {
            buf.append("      Transfer credential: ");
            buf.append(xfer.getTransferCredential().toString());
        }
        if (xfer.getChecksum() != null) {
            buf.append("      Checksum: ");
            buf.append(xfer.getChecksum());
        }
        if (xfer.getChecksumtype() != null) {
            buf.append("      Checksum type: ");
            buf.append(xfer.getChecksumtype());
        }
    }

    public static String getTextFileViaInputStream(InputStream is)
            throws IOException {

        InputStreamReader isr = null;
        BufferedReader br = null;
        final StringWriter writer = new StringWriter();
        String ret = null;

        try {
            isr = new InputStreamReader(is);
            br = new BufferedReader(isr);

            String line;
            do {
                line = br.readLine();
                if (line != null) {
                    writer.write(line);
                    writer.write(SYSTEM_LINE_SEPARATOR);
                }
            } while (line != null);

            ret = writer.toString();

        } finally {
            writer.close();
            if (br != null) {
                br.close();
            }
            if (isr != null) {
                isr.close();
            }

        }

        return ret;
    }

    public static String shortStringReprMultiLine(Workspace workspace) {
        if (workspace == null) {
            return null; // *** EARLY RETURN ***
        }

        final EndpointReferenceType epr = workspace.getEpr();
        final String id;
        if (epr == null) {
            id = "[no id]";
        } else {
            id = "#" + EPRUtils.getIdFromEPR(epr);
        }

        final String netStr = NetUtils.oneLineNetString(workspace);

        final String problemPrefix = "** PROBLEM: ";

        final State state = workspace.getCurrentState();
        String stateStr;
        String problemStr = null;
        if (state == null) {
            stateStr = "[no state]";
        } else {
            stateStr = "State: " + state.getState();
            final Exception problem = state.getProblem();
            if (problem != null) {
                final String fullProblemString =  CommonUtil.
                        genericExceptionMessageWrapper(problem);

                final int len = fullProblemString.length();
                final int newlineIdx = fullProblemString.indexOf('\n');

                if (newlineIdx < 0 && len > 60) {
                    final String first60 = fullProblemString.substring(0,60);
                    problemStr = problemPrefix + first60 + " [TRUNCATED]";
                } else if (newlineIdx >= 0 && newlineIdx < 60) {
                    int start = newlineIdx;
                    if (start == 0) {
                        start = 1; // if first char is newline, get rid of it
                    }
                    final String upToNewline =
                            fullProblemString.substring(start,newlineIdx);
                    problemStr = problemPrefix + upToNewline + " [TRUNCATED]";
                } else if (len > 60) {
                    final String first60 = fullProblemString.substring(0,60);
                    problemStr = problemPrefix + first60 + " [TRUNCATED]";
                } else {
                    problemStr = problemPrefix + fullProblemString;
                }
            }
        }

        final Schedule schedule = workspace.getCurrentSchedule();
        String duration = null;
        String shutdownTime = null;
        String startTime = null;
        if (problemStr == null) {
            duration = ScheduleUtils.getDurationMessage(schedule);
            shutdownTime = ScheduleUtils.getShutdownMessage(schedule);
            startTime = ScheduleUtils.getStartTimeMessage(schedule);
        }
        final String termTime = ScheduleUtils.getTerminationMessage(schedule);

        final StringBuffer buf = new StringBuffer(1024);
        buf.append("[*] - Workspace ")
           .append(id)
           .append(". ")
           .append(netStr);
        buf.append("\n      ");
        buf.append(stateStr);
        if (problemStr != null) {
            buf.append("\n      ");
            buf.append(problemStr);
        }
        if (duration != null) {
            buf.append("\n      ").append(duration);
        }
        if (startTime != null) {
            buf.append("\n      ").append(startTime);
        }
        if (shutdownTime != null) {
            buf.append("\n      ").append(shutdownTime);
        }
        if (termTime != null) {
            buf.append("\n      ").append(termTime);
        }
        return buf.toString();
    }
}
