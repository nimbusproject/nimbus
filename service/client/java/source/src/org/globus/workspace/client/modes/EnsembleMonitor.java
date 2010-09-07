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

package org.globus.workspace.client.modes;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.axis.message.addressing.EndpointReferenceType;
import org.globus.workspace.client_core.actions.Ensemble_Report;
import org.globus.workspace.client_core.StubConfigurator;
import org.globus.workspace.client_core.ParameterProblem;
import org.globus.workspace.client_core.ExecutionProblem;
import org.globus.workspace.client_core.ExitNow;
import org.globus.workspace.client_core.repr.Workspace;
import org.globus.workspace.client_core.repr.State;
import org.globus.workspace.client_core.repr.Networking;
import org.globus.workspace.client_core.repr.Nic;
import org.globus.workspace.client_core.repr.Schedule;
import org.globus.workspace.client_core.utils.EPRUtils;
import org.globus.workspace.client_core.utils.StringUtils;
import org.globus.workspace.client_core.utils.FileUtils;
import org.globus.workspace.client_core.utils.ScheduleUtils;
import org.globus.workspace.client_core.print.PrCodes;
import org.globus.workspace.common.print.Print;
import org.nimbustools.messaging.gt4_0.common.CommonUtil;
import org.globus.workspace.common.client.CommonPrint;
import org.globus.workspace.client.AllArguments;
import org.globus.workspace.client.modes.aux.CommonLogs;
import org.globus.workspace.client_common.CommonStrings;
import org.nimbustools.messaging.gt4_0.generated.types.WorkspaceUnknownFault;
import org.nimbustools.messaging.gt4_0.generated.types.CurrentState_Enumeration;
import org.nimbustools.messaging.gt4_0.generated.ensemble.WorkspaceEnsembleFault;
import org.globus.wsrf.encoding.SerializationException;
import org.oasis.wsrf.faults.BaseFaultType;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.DateFormat;
import java.util.Calendar;

public class EnsembleMonitor extends Mode  {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(EnsembleMonitor.class.getName());

    private static final DateFormat localFormat =
            DateFormat.getDateTimeInstance();
    public static String newline = System.getProperty("line.separator");

    public static final int DEFAULT_POOL_SIZE = 4;
    

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private String nameToPrint;
    private Ensemble_Report ensembleReport;
    private EndpointReferenceType epr;
    private boolean dryrun;
    private long pollDelayMs;
    private CurrentState_Enumeration targetState;
    private State compareState;

    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    public EnsembleMonitor(Print print,
                           AllArguments arguments,
                           StubConfigurator stubConfigurator) {
        super(print, arguments, stubConfigurator);
    }


    // -------------------------------------------------------------------------
    // extends Mode
    // -------------------------------------------------------------------------

    public String name() {
        return "Monitor-ensemble";
    }

    public void validateOptionsImpl() throws ParameterProblem {

        this.handlePollDelay();
        this.handleExitState();
        this.validateEndpoint();
        this.setName();
        this.validateReportdir();
        this.dryrun = this.args.dryrun;
        CommonLogs.logBoolean(this.dryrun, "dryrun mode", this.pr, logger);
    }

    public void runImpl() throws ParameterProblem, ExecutionProblem, ExitNow {
        this._runImpl();
    }

    // -------------------------------------------------------------------------
    // VALIDATION
    // -------------------------------------------------------------------------

    private void handlePollDelay() throws ParameterProblem {

        if (this.args.pollDelayString == null) {
            throw new ParameterProblem("poll delay is required");
        }

        try {
            this.pollDelayMs = Long.parseLong(this.args.pollDelayString);
        } catch (NumberFormatException e) {
            throw new ParameterProblem("Given poll delay is not valid: '" +
                    this.args.pollDelayString + "': " + e.getMessage(), e);
        }

        if (this.pollDelayMs < 1) {
            throw new ParameterProblem("Given poll delay is less than 1ms: " +
                    this.pollDelayMs + "ms");
        }
    }

    private void handleExitState() throws ParameterProblem {

        if (this.args.exitStateString == null) {
            this.targetState = CurrentState_Enumeration.Running;
            this.compareState = new State("Running");
            return;  // *** EARLY RETURN ***
        }


        if (!State.testValidState(this.args.exitStateString)) {
            throw new ParameterProblem("Provided exit string is not a " +
                    "valid state: '" + this.args.exitStateString + "'");
        }

        this.compareState = new State(this.args.exitStateString);

        if (this.pr.enabled()) {
            final String dbg = "Exit state: " + this.compareState.toString();
            if (this.pr.useThis()) {
                this.pr.dbg(dbg);
            } else if (this.pr.useLogging()) {
                logger.debug(dbg);
            }
        }

        if (!this.compareState.isOKState()) {
            throw new ParameterProblem("Provided exit string is not a " +
                    "valid target state: '" + this.args.exitStateString + "'");
        }

        this.targetState = CurrentState_Enumeration.
                                fromString(this.compareState.toString());
    }

    private void validateEndpoint() throws ParameterProblem {

        this.epr = this.stubConf.getEPR();

        if (this.epr == null) {
            throw new ParameterProblem(name() + " requires EPR");
        }

        final String eprStr;
        try {
            eprStr = EPRUtils.eprToString(this.epr);
        } catch (Exception e) {
            final String err = CommonUtil.genericExceptionMessageWrapper(e);
            throw new ParameterProblem(err, e);
        }

        if (this.pr.enabled()) {
            // xml print
            final String dbg =
                    "\nGiven EPR:\n----------\n" + eprStr + "----------\n";

            if (this.pr.useThis()) {
                this.pr.dbg(dbg);
            } else if (this.pr.useLogging()) {
                logger.debug(dbg);
            }
        }

        this.ensembleReport =
                new Ensemble_Report(this.epr, this.stubConf, this.pr);
    }

    private void validateReportdir() throws ParameterProblem {

        if (this.args.reportDir == null) {
            return; // *** EARLY RETURN ***
        }

        final File f = new File(this.args.reportDir);
        if (!f.exists()) {
            throw new ParameterProblem("Given path for reports directory ('" +
                    this.args.reportDir + "') does not exist.");
        }

        if (!f.isDirectory()) {
            throw new ParameterProblem("Given path for reports directory ('" +
                    this.args.reportDir + "') is not a directory.");
        }

        if (!f.canWrite()) {
            throw new ParameterProblem("Given path for reports directory ('" +
                    this.args.reportDir + "') is not writable.");
        }
    }

    private void setName() {

        if (this.args.shortName != null) {
            this.nameToPrint = this.args.shortName;
        } else {
            this.nameToPrint = "ensemble \"" +
                        EPRUtils.getEnsembleIdFromEPR(this.epr) + "\"";

            final String service =
                    StringUtils.commonAtServiceAddressSuffix(this.epr);

            if (service != null) {
                this.nameToPrint += service;
            }
        }

        if (this.pr.enabled()) {
            final String dbg = "Name to print: '" + this.nameToPrint + "'";
            if (this.pr.useThis()) {
                this.pr.dbg(dbg);
            } else if (this.pr.useLogging()) {
                logger.debug(dbg);
            }
        }
    }

    // -------------------------------------------------------------------------
    // RUN
    // -------------------------------------------------------------------------

    private void _runImpl() throws ParameterProblem, ExecutionProblem, ExitNow {

        if (this.dryrun) {

            if (this.pr.enabled()) {
                final String msg = "Dryrun, done.";
                if (this.pr.useThis()) {
                    this.pr.infoln(PrCodes.ENSEMBLEREPORT__DRYRUN, msg);
                } else if (this.pr.useLogging()) {
                    logger.info(msg);
                }
            }

            return; // *** EARLY RETURN ***
        }

        Result result = null;
        try {

            while (true) {
                try {
                    result = this.runQuery();
                    if (result.allAtState || result.oneErrorExists) {
                        break;
                    }
                    Thread.sleep(this.pollDelayMs);
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        
        } catch (BaseFaultType e) {
            final String err =
                    CommonStrings.faultStringOrCommonCause(e, "ensemble");
            throw new ExecutionProblem(err, e);
        }

        if (result.oneErrorExists) {

            if (this.pr.enabled()) {
                final String msg =
                        "Encountered a problem launching " + this.nameToPrint;
                if (this.pr.useThis()) {
                    this.pr.infoln(PrCodes.ENSMONITOR__ONE_ERROR, msg);
                } else if (this.pr.useLogging()) {
                    logger.info(msg);
                }
            }

        } else if (result.allAtState) {

            if (this.pr.enabled()) {
                final String msg = this.nameToPrint + ": all members are " +
                        this.targetState.toString();
                if (this.pr.useThis()) {
                    this.pr.infoln(PrCodes.ENSMONITOR__ALL_RUNNING,
                                   "  - " + msg);
                } else if (this.pr.useLogging()) {
                    logger.info(msg);
                }
            }

        } else {
            throw new ExecutionProblem("Incorrect response from report query?");
        }

        if (result.workspaces == null) {
            throw new ExecutionProblem("Incorrect response from report query?");
        }

        // both oneErrorExists and allRunning trigger report(s) if path is configured
        if (this.args.reportDir != null) {
            this.writeReports(result);
        }

        if (!result.allAtState) {
            throw new ExitNow(1);
        }
    }

    private Result runQuery() throws ExecutionProblem,
                                     WorkspaceUnknownFault,
                                     WorkspaceEnsembleFault,
                                     ParameterProblem {

        if (this.ensembleReport == null) {
            throw new IllegalStateException(
                    "there is no port type to call ensemble-report");
        }

        this.ensembleReport.setResponseOnlyIfError(true);
        this.ensembleReport.setReturnOnlyIfAll(this.targetState);
        final Workspace[] workspaces = this.ensembleReport.report();
        if (workspaces.length == 0) {
            return new Result(false, false, null);
        } else {
            return this.analyzeResult(workspaces);
        }
    }

    private Result analyzeResult(Workspace[] workspaces) {

        boolean allAtState = true;
        boolean oneErrorExists = false;

        for (int i = 0; i < workspaces.length; i++) {
            
            final Workspace workspace = workspaces[i];
            final State state = workspace.getCurrentState();

            if (state == null) {

                allAtState = false;

            } else {

                if (state.isCancelled() || state.isCorrupted()) {
                    oneErrorExists = true;
                    allAtState = false;
                    break; // definitive
                }

                if (!this.compareState.equals(state)) {
                    allAtState = false;
                }
            }
        }
        
        return new Result(oneErrorExists, allAtState, workspaces);
    }

    private class Result {
        final boolean oneErrorExists;
        final boolean allAtState;
        final Workspace[] workspaces;
        private Result(boolean oneErrorExists,
                       boolean allAtState,
                       Workspace[] workspaces) {
            this.oneErrorExists = oneErrorExists;
            this.allAtState = allAtState;
            this.workspaces = workspaces;
        }
    }

    // -------------------------------------------------------------------------
    // REPORT WRITING
    // -------------------------------------------------------------------------

    protected void writeReports(Result result) {

        if (result == null) {
            throw new IllegalArgumentException("result may not be null");
        }
        if (result.workspaces == null) {
            throw new IllegalArgumentException("workspaces may not be null");
        }

        final Workspace[] workspaces = result.workspaces;

        final short numDigits;
        if (workspaces.length > 1000) {
            numDigits = 4;
        } else if (workspaces.length > 100) {
            numDigits = 3;
        } else if (workspaces.length > 10) {
            numDigits = 2;
        } else {
            numDigits = 1;
        }

        final NumberFormat format = NumberFormat.getInstance();
        format.setMinimumIntegerDigits(numDigits);
        this._writeReports(workspaces, this.args.reportDir, format);

        if (this.pr.enabled()) {
            final String msg = "wrote reports to '" + this.args.reportDir + "'";
            if (this.pr.useThis()) {
                this.pr.infoln(PrCodes.ENSMONITOR__REPORT_DIR, "  - " + msg);
            } else if (this.pr.useLogging()) {
                logger.info(msg);
            }
        }
    }

    private void _writeReports(Workspace[] workspaces,
                               String reportDir,
                               NumberFormat format) {

        for (int i = 0; i < workspaces.length; i++) {

            final String suffix = this.getSuffix(workspaces[i]);
            final String prefix = this.getStateString(workspaces[i]) + "-vm-";
            final String path = prefix + format.format(i+1) + suffix;
            
            try {
                this.writeOneReport(workspaces[i], reportDir, path);
            } catch (Exception e) {
                // print error and continue
                if (this.pr.enabled()) {
                    final String msg =
                            CommonUtil.genericExceptionMessageWrapper(e);
                    if (this.pr.useThis()) {
                        this.pr.err(msg);
                    } else if (this.pr.useLogging()) {
                        if (logger.isDebugEnabled()) {
                            logger.error(msg, e);
                        } else {
                            logger.error(msg);
                        }
                    }
                }
            }
        }

    }

    protected String getStateString(Workspace w) {
        final State state = w.getCurrentState();
        if (state == null) {
            return "UNKNOWN";
        }
        return state.getState().toUpperCase();
    }

    protected String getSuffix(Workspace w) {
        final EndpointReferenceType EPR = w.getEpr();
        final String tail;
        if (EPR != null) {
            tail = "_eprkey-" + EPRUtils.getIdFromEPR(EPR) + ".txt";
        } else {
            tail = ".txt";
        }
        return tail;
    }

    protected void writeOneReport(Workspace w, String dir, String file)
            throws SerializationException, IOException {

        final File f = new File(dir, file);
        FileUtils.writeStringToFile(this.getOneReportText(w),
                                    f.getAbsolutePath());
        
        if (this.pr.enabled()) {
            final String msg = "    Wrote '" + file + "'";
            if (this.pr.useThis()) {
                this.pr.infoln(PrCodes.ENSMONITOR__SINGLE_REPORT_NAMES, msg);
            } else if (this.pr.useLogging()) {
                logger.info(msg);
            }
        }
    }


    protected String getOneReportText(Workspace w) {

        final StringBuffer buf = new StringBuffer(16384);

        final String now = localFormat.format(Calendar.getInstance().getTime());

        buf.append("## File autogenerated at ")
           .append(now);
        add2Newlines(buf);

        buf.append(CommonPrint.textDebugSection("STATUS"));
        add2Newlines(buf);
        buf.append(this.getOneStatus(w.getCurrentState()));
        add2Newlines(buf);

        buf.append(CommonPrint.textDebugSection("NETWORK"));
        add2Newlines(buf);
        buf.append(this.getOneNetwork(w.getCurrentNetworking()));
        add2Newlines(buf);

        buf.append(CommonPrint.textDebugSection("SCHEDULE"));
        add2Newlines(buf);
        buf.append(this.getOneSchedule(w.getCurrentSchedule()));
        add2Newlines(buf);

        buf.append(CommonPrint.textDebugSection("EPR/IDENTITY"));
        add2Newlines(buf);
        buf.append(this.getOneEPR(w.getEpr()));
        add2Newlines(buf);

        
        return buf.toString();
    }

    private static void addNewline(StringBuffer buf) {
        buf.append(newline);
    }

    private static void add2Newlines(StringBuffer buf) {
        buf.append(newline).append(newline);
    }

    // -------------------------------------------------------------------------

    protected String getOneStatus(State state) {
        if (state == null) {
            return "State: no information";
        }
        final StringBuffer buf = new StringBuffer(2048);
        buf.append("State: ")
           .append(state.getState());
        addNewline(buf);
        final Exception e = state.getProblem();
        if (e != null) {
            buf.append("Problem: ")
               .append(CommonUtil.genericExceptionMessageWrapper(e));
        }
        return buf.toString();
    }

    protected String getOneSchedule(Schedule sched) {
        if (sched == null) {
            return "Schedule: no information";
        }
        final StringBuffer buf = new StringBuffer(2048);

        buf.append(ScheduleUtils.getDurationMessage(sched));
        addNewline(buf);
        buf.append(ScheduleUtils.getStartTimeMessage(sched));
        addNewline(buf);
        buf.append(ScheduleUtils.getShutdownMessage(sched));
        addNewline(buf);
        buf.append(ScheduleUtils.getTerminationMessage(sched));
        addNewline(buf);
        return buf.toString();
    }

    protected String getOneNetwork(Networking networking) {
        if (networking == null) {
            return "Networking: no information";
        }
        final StringBuffer buf = new StringBuffer(2048);
        final Nic[] nics = networking.nics();
        for (int i = 0; i < nics.length; i++) {
            buf.append(this.getOneNIC(nics[i]));
            addNewline(buf);
        }
        return buf.toString();
    }

    protected String getOneNIC(Nic nic) {
        if (nic == null) {
            return "NIC: no information";
        }
        final String name = "NIC: " + nic.getName();
        final String host = "  - Hostname: " + nic.getHostname();
        final String ip = "  - IP: " + nic.getIpAddress();
        final String gateway = "  - Gateway: " + nic.getGateway();
        final String assoc = "  - Association: " + nic.getAssociation();

        final StringBuffer buf = new StringBuffer(name);
        buf.append(newline).append(host)
           .append(newline).append(ip)
           .append(newline).append(gateway)
           .append(newline).append(assoc);
        return buf.toString();
    }

    protected String getOneEPR(EndpointReferenceType wepr) {
        if (wepr == null) {
            return "EPR: none";
        }
        final StringBuffer buf = new StringBuffer(2048);
        buf.append("Workspace ID: ")
            .append(EPRUtils.getIdFromEPR(wepr));
        addNewline(buf);
        try {
            buf.append(EPRUtils.eprToString(wepr));
        } catch (Exception e) {
            buf.append("Problem deserializing EPR: ")
               .append(CommonUtil.genericExceptionMessageWrapper(e));
        }
        return buf.toString();
    }
}
