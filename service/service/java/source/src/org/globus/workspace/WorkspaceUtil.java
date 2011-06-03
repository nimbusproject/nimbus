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

package org.globus.workspace;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;

import edu.emory.mathcs.backport.java.util.concurrent.ExecutorService;
import edu.emory.mathcs.backport.java.util.concurrent.Executors;
import edu.emory.mathcs.backport.java.util.concurrent.Callable;
import edu.emory.mathcs.backport.java.util.concurrent.FutureTask;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;


public class WorkspaceUtil {

    private static final Log logger =
        LogFactory.getLog(WorkspaceUtil.class.getName());

    // TODO: shutdown if container is asking for exit
    private static ExecutorService executor = Executors.newCachedThreadPool();


    public static boolean isInvalidState(int newstate) {
        if (newstate == WorkspaceConstants.STATE_DESTROY_SUCCEEDED ||
                newstate == WorkspaceConstants.STATE_DESTROY_FAILED) {
            return false;
        }
        return newstate < WorkspaceConstants.STATE_FIRST_LEGAL
            || newstate > WorkspaceConstants.STATE_LAST_LEGAL;
    }

    // Ceilings to nearest minute.
    public static long secondsToMinutes(long seconds) {
        if (seconds == 0) {
            return 0;
        } else {
            long minutes = seconds/60;
            if (seconds % 60 > 0) {
                minutes += 1;
            }
            return minutes;
        }
    }

    /**
     * @param  command
     *         Array of command + arguments, to fork a process
     * @param eventLog log events to info?
     * @param traceLog alternatively, log events to trace?
     * @return String
     *         The output of the process on to stdout
     * @throws WorkspaceException exc
     * @throws ReturnException
     *         Exception containing return code other than zero and also the
     *         stdin/stdout/stderr if needed.
     */
    public static String runCommand(String[] command,
                                    boolean eventLog,
                                    boolean traceLog)
                                throws WorkspaceException, ReturnException {
            return runCommand(command, true, null, eventLog, traceLog);
    }

    /**
     * @param  command
     *         Array of command + arguments, to fork a process
     * @param eventLog log events to info?
     * @param traceLog alternatively, log events to trace?
     * @param trackingID optional for event logging, an id > 0?
     * @return String
     *         The output of the process on to stdout
     * @throws WorkspaceException exc
     * @throws ReturnException
     *         Exception containing return code other than zero and also the
     *         stdin/stdout/stderr if needed.
     */
    public static String runCommand(String[] command,
                                    boolean eventLog,
                                    boolean traceLog,
                                    int trackingID)
                                throws WorkspaceException, ReturnException {
        return runCommand(command, true, null, eventLog, traceLog, trackingID);
    }

    /**
     * @param command command array
     * @param event passing in false disables the event log
     * @param stdin stdin for process
     * @param eventLog log events to info?
     * @param traceLog alternatively, log events to trace?
     * @return stdout
     * @throws WorkspaceException exc
     * @throws ReturnException if exit code != 0, will contain return code
     *         as well as stdout and stderr if they exist.
     */
    public static String runCommand(String[] command,
                                    boolean event,
                                    String stdin,
                                    boolean eventLog,
                                    boolean traceLog)
                                throws WorkspaceException, ReturnException {
        return runCommand(command, event, stdin, eventLog, traceLog, -1);
    }

    /**
     * @param command command array
     * @param event passing in false disables the event log
     * @param stdin stdin for process
     * @param eventLog log events to info?
     * @param traceLog alternatively, log events to trace?
     * @param trackingID optional for event logging, an id > 0?
     * @return stdout
     * @throws WorkspaceException exc
     * @throws ReturnException if exit code != 0, will contain return code
     *         as well as stdout and stderr if they exist.
     */
    public static String runCommand(String[] command,
                                    boolean event,
                                    String stdin,
                                    boolean eventLog,
                                    boolean traceLog,
                                    int trackingID)
                                throws WorkspaceException, ReturnException {

        if (command == null) {
            logger.error("Command cannot be null");
            throw new WorkspaceException("Command cannot be null");
        }

        if (eventLog && event) {
            logger.info(Lager.ev(trackingID) + printCmd(command));
        } else if (traceLog && event) {
            logger.trace(printCmd(command));
        }

        final Runtime runtime = Runtime.getRuntime();
        String stdout = null;
        String stderr = null;
        InputStream processStdoutStream = null;
        InputStream processStderrStream = null;
        
        try {             
            final Process process = runtime.exec(command);

            // Unfortunately there can be buffer overflow problems if there are
            // not threads consuming stdout/stderr, seen that with workspace-
            // control create commands on certain platforms.

            processStderrStream = process.getErrorStream();
            final FutureTask stderrConsumer = new FutureTask(
                            new StreamConsumer(processStderrStream));

            processStdoutStream = process.getInputStream();
            final FutureTask stdoutConsumer = new FutureTask(
                            new StreamConsumer(processStdoutStream));

            executor.submit(stdoutConsumer);
            executor.submit(stderrConsumer);

            if (stdin != null) {
                if (traceLog) {
                    logger.trace("stdin provided");
                }
                BufferedWriter in = null;
                OutputStreamWriter osw = null;
                OutputStream os = null;
                try {
                    os = process.getOutputStream();
                    osw = new OutputStreamWriter(os);
                    in = new BufferedWriter(osw);
                    in.write(stdin);
                    in.newLine();
                    in.flush();
                } finally {
                    if (in != null) {
                        in.close();
                    }
                    if (osw != null) {
                        osw.close();
                    }
                    if (os != null) {
                        os.close();
                    }
                }
                if (traceLog) {
                    logger.trace("stdin sent");
                }
            } else {
                OutputStream os = null;
                try {
                    os = process.getOutputStream();
                } finally {
                    if (os != null) {
                        os.close();
                    }
                }
            }

            final int returnCode;
            try {
                returnCode = process.waitFor();
            } catch (InterruptedException exp) {
                logger.error("Interupped exp thrown ", exp);
                throw new WorkspaceException("Interrupted: ", exp);
            }

            if (eventLog && event) {
                logger.info(Lager.ev(trackingID) + "Return code is " + returnCode);
            } else if (traceLog && event) {
                logger.trace("Return code is " + returnCode);
            }

            try {
                stdout = (String) stdoutConsumer.get(60L, TimeUnit.SECONDS);
            } catch (Exception e) {
                logger.error(e.getMessage());
            }

            try {
                stderr = (String) stderrConsumer.get(60L, TimeUnit.SECONDS);
            } catch (Exception e) {
                logger.error(e.getMessage());
            }

            if (returnCode != 0) {
                if (stderr != null && stdout != null) {

                    logger.error(Lager.ev(trackingID) + "system command FAILURE" +
                                    "\nSTDOUT:\n" + stdout +
                                    "\n\nSTDERR:\n" + stderr);

                    throw new ReturnException(returnCode, stderr, stdout);

                } else if (stderr != null) {

                    logger.error(Lager.ev(trackingID) + "system command FAILURE" +
                                    "\nSTDERR:\n" + stderr);
                    throw new ReturnException(returnCode, stderr);

                } else {

                    logger.error(Lager.ev(trackingID) +
                                "system command FAILURE, no stdout or stderr");
                    throw new ReturnException(returnCode);
                }

            } else {
                if (stdout != null) {
                    if (eventLog && event) {
                        logger.info(Lager.ev(trackingID) + "\n" +
                                    "STDOUT:\n" + stdout);
                    } else if (traceLog && event) {
                        logger.trace("\nSTDOUT:\n" + stdout);
                    }
                }
            }
        } catch (IOException ioe) {
            logger.error(ioe);
            throw new WorkspaceException("", ioe);
        } finally {
            try {

                if (processStdoutStream != null) {
                    processStdoutStream.close();
                }
                if (processStderrStream != null) {
                    processStderrStream.close();
                }
            } catch (IOException exp) {
                logger.error("Could not close stream", exp);
            }
        }

        // may be null
        return stdout;
    }

    public static String printCmd(String[] exe) {
        if (exe == null) {
            return "null";
        }
        final StringBuffer buf = new StringBuffer(exe.length * 16);
        for (int i=0; i < exe.length; i++) {
            buf.append(exe[i]);
            buf.append(" ");
        }
        return buf.toString();
    }

    public static String scrubDEBUG(String msg) {

        if (msg == null) {
            return null;
        }

        final StringBuffer buf = new StringBuffer(msg.length());

        final String[] lines = msg.split("\n");

        final boolean addNewLine = (lines.length > 1);

        for (int i = 0; i < lines.length; i++) {
            if (!lines[i].matches(".*DEBUG.*")) {
                buf.append(lines[i]);
                if (addNewLine) {
                    buf.append("\n");
                }
            }
        }

        return buf.toString();
    }

    private static class StreamConsumer implements Callable {

        final InputStream is;

        StreamConsumer(InputStream stream) {
            if (stream == null) {
                throw new IllegalArgumentException("stream may not be null");
            }
            this.is = stream;
        }

        public Object call() throws Exception {

            InputStreamReader isr = null;
            BufferedReader br = null;
            StringBuffer output = null;

            try {
                isr = new InputStreamReader(this.is);
                br = new BufferedReader(isr);

                String line = br.readLine();
                if (line != null) {
                    output = new StringBuffer(line);
                    while (line != null) {
                        line = br.readLine();
                        if (line != null) {
                            output.append("\n").
                                   append(line);
                        }
                    }
                }
                
            } catch (IOException e) {
                logger.error(e.getMessage());
            } finally {
                try {
                    if (br != null) {
                        br.close();
                    }
                    if (isr != null) {
                        isr.close();
                    }
                    // this.is closed at end of runCommand
                } catch (IOException e) {
                    logger.error(e.getMessage());
                }
            }
            if (output == null || output.length() == 0) {
                return null;
            } else {
                return output.toString();
            }
        }
    }
}
