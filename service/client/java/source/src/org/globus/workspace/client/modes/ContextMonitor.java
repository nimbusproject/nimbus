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
import org.globus.workspace.client_core.StubConfigurator;
import org.globus.workspace.client_core.ParameterProblem;
import org.globus.workspace.client_core.ExecutionProblem;
import org.globus.workspace.client_core.ExitNow;
import org.globus.workspace.client_core.print.PrCodes;
import org.globus.workspace.client_core.utils.EPRUtils;
import org.globus.workspace.client_core.utils.FileUtils;
import org.globus.workspace.client_core.actions.Ctx_RPQuery;
import org.globus.workspace.client_core.actions.Ctx_Identities;
import org.globus.workspace.common.print.Print;
import org.globus.workspace.common.print.PrintOpts;
import org.nimbustools.messaging.gt4_0.common.CommonUtil;
import org.nimbustools.ctxbroker.generated.gt4_0.types.ContextualizationContext;
import org.nimbustools.ctxbroker.generated.gt4_0.types.Node_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.types.MatchedRole_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.broker.NimbusContextualizationFault;
import org.nimbustools.ctxbroker.generated.gt4_0.description.IdentityProvides_Type;
import org.globus.workspace.common.client.CommonPrint;
import org.globus.workspace.client.AllArguments;
import org.globus.workspace.client.modes.aux.CommonLogs;
import org.globus.workspace.client_common.CommonStrings;
import org.globus.wsrf.encoding.SerializationException;
import org.oasis.wsrf.faults.BaseFaultType;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.StringTokenizer;
import java.util.ArrayList;

public class ContextMonitor extends Mode {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(ContextMonitor.class.getName());

    private static final DateFormat localFormat =
            DateFormat.getDateTimeInstance();
    public static String newline = System.getProperty("line.separator");

    public static final long DEFAULT_POLL_MS = 5000;
    public static final int DEFAULT_POOL_SIZE = 4;
    

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private String nameToPrint;
    private Ctx_RPQuery rpQuery;
    private Ctx_Identities identitiesQuery;
    private EndpointReferenceType epr;
    private boolean dryrun;
    private long pollDelayMs;
    private String sshKnownHostsPath;
    private String sshKnownHostsDirPath;
    private boolean adjustSshKnownHosts;
    private AdjustTask[] adjustTasks;
    
    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    public ContextMonitor(Print print,
                          AllArguments arguments,
                          StubConfigurator stubConfigurator) {
        super(print, arguments, stubConfigurator);
    }


    // -------------------------------------------------------------------------
    // extends Mode
    // -------------------------------------------------------------------------

    public String name() {
        return "Monitor-context";
    }

    public void validateOptionsImpl() throws ParameterProblem {

        this.handlePollDelay();
        this.validateEndpoint();
        this.setName();
        this.validateReportdir();
        this.validateAdjustSSHhostsList();
        this.validateSSHhostsFile();
        this.validateSSHhostsDir();
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

        this.rpQuery =
                new Ctx_RPQuery(this.epr, this.stubConf, this.pr);
        this.identitiesQuery =
                new Ctx_Identities(this.epr, this.stubConf, this.pr);
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

    private void validateAdjustSSHhostsList() throws ParameterProblem {

        if (this.args.adjustSshHostsList == null) {
            return; // *** EARLY RETURN ***
        }

        this.adjustSshKnownHosts = true;

        final StringTokenizer st =
                new StringTokenizer(this.args.adjustSshHostsList, ",");

        final ArrayList adjustTaskList = new ArrayList(st.countTokens());
        while (st.hasMoreTokens()) {
            final String val = st.nextToken();
            final AdjustTask task = taskFromString(val.trim());
            if (task != null) {
                adjustTaskList.add(task);
            }
        }
        this.adjustTasks =
                (AdjustTask[]) adjustTaskList.toArray(
                        new AdjustTask[adjustTaskList.size()]);
        
        this.pr.debugln("Configured " + this.adjustTasks.length +
                            " known_hosts adjust tasks.");

        for (final AdjustTask task : this.adjustTasks) {
            if (task.iface == null) {
                this.pr.debugln("IP '" + task.ipAddress + "', " +
                        "all interfaces");
            } else {
                this.pr.debugln("IP '" + task.ipAddress + "', " +
                        "one interface: '" + task.iface + "'");
            }
        }
    }

    private AdjustTask taskFromString(String val) throws ParameterProblem {
        this.pr.debugln("Examining adjust-task string '" + val + "'");
        final StringTokenizer st = new StringTokenizer(val, "::");
        if (st.countTokens() == 1) {
            return new AdjustTask(st.nextToken(), null, null);
        } else if (st.countTokens() == 2) {
            return new AdjustTask(st.nextToken(), st.nextToken(), null);
        } else if (st.countTokens() == 3) {
            return new AdjustTask(st.nextToken(), st.nextToken(), st.nextToken());
        } else {
            throw new ParameterProblem(
                    "known_hosts adjustment string is invalid: '" + val + "'");
        }
    }

    private static class AdjustTask {
        final String ipAddress;
        final String iface;
        final String printName;

        private AdjustTask(String ipAddress, String interfaceName, String toPrintName) {
            this.ipAddress = ipAddress;
            this.iface = interfaceName;
            this.printName = toPrintName;
        }
    }

    private void validateSSHhostsFile() throws ParameterProblem {

        // file not needed if adjust flag is not present
        if (!this.adjustSshKnownHosts) {
            return; // *** EARLY RETURN ***
        }

        if (this.args.sshHostsPath == null) {
            return; // *** EARLY RETURN ***
        }

        final File f = new File(this.args.sshHostsPath);
        if (f.exists()) {
            if (!f.canWrite()) {
                throw new ParameterProblem("Given known_hosts path ('" +
                        this.args.sshHostsPath + "') is not writable.");
            }
            this.sshKnownHostsPath = f.getAbsolutePath();
            return; // *** EARLY RETURN ***
        }

        // if it doesn't exist, create it
        try {
            if (!f.createNewFile()) {
                throw new ParameterProblem(
                        "Could not create new known_hosts file @ '" +
                                f.getAbsolutePath() + "'");
            }
        } catch (IOException e) {
            final String err = CommonUtil.genericExceptionMessageWrapper(e);
            throw new ParameterProblem(
                        "Could not create new known_hosts file @ '" +
                                f.getAbsolutePath() + "': " + err, e);
        }

        this.sshKnownHostsPath = f.getAbsolutePath();

        if (this.pr.enabled()) {
            final String msg = "Created known_hosts file @ '" +
                                            this.sshKnownHostsPath + "'";
            if (this.pr.useThis()) {
                this.pr.infoln(PrCodes.CTXMONITOR__KNOWNHOSTS_FILE_CREATE, msg);
            } else if (this.pr.useLogging()) {
                logger.info(msg);
            }
        }
    }

    private void validateSSHhostsDir() throws ParameterProblem {

        // dir files not needed if adjust flag is not present
        if (!this.adjustSshKnownHosts) {
            return; // *** EARLY RETURN ***
        }

        if (this.args.sshHostsDirPath == null) {
            return; // *** EARLY RETURN ***
        }

        final File f = new File(this.args.sshHostsDirPath);
        if (f.exists()) {
            if (!f.canWrite()) {
                throw new ParameterProblem("Given known_hosts directory ('" +
                        this.args.sshHostsDirPath + "') is not writable.");
            }
            this.sshKnownHostsDirPath = f.getAbsolutePath();
        } else {
            throw new ParameterProblem("Given known_hosts directory ('" +
                        this.args.sshHostsDirPath + "') does not exist.");
        }
    }

    private void setName() {

        if (this.args.shortName != null) {
            this.nameToPrint = this.args.shortName;
        } else {
            this.nameToPrint = "\"" +
                        EPRUtils.getContextIdFromEPR(this.epr) + "\"";
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
                    this.pr.infoln(PrCodes.CTXMONITOR__DRYRUN, msg);
                } else if (this.pr.useLogging()) {
                    logger.info(msg);
                }
            }

            return; // *** EARLY RETURN ***
        }

        ContextualizationContext contextRP;
        try {

            while (true) {
                try {
                    contextRP = this.rpQuery.query();
                    if (this.analyzeResult(contextRP)) {
                        break;
                    }
                    Thread.sleep(this.pollDelayMs);
                } catch (InterruptedException e) {
                    // ignore
                }
            }

            final String msg = "Querying ended via analyzeResult";
            if (this.pr.useThis()) {
                this.pr.debugln(msg);
            } else {
                logger.debug(msg);
            }

        } catch (BaseFaultType e) {
            final String err =
                    CommonStrings.faultStringOrCommonCause(e, "context");
            throw new ExecutionProblem(err, e);
        }

        if (contextRP.isErrorPresent()) {

            if (this.pr.enabled()) {
                final String msg =
                        "Problem with " + this.nameToPrint + " context";
                if (this.pr.useThis()) {
                    this.pr.infoln(PrCodes.CTXMONITOR__ONE_ERROR, msg);
                } else if (this.pr.useLogging()) {
                    logger.info(msg);
                }
            }

        } else if (contextRP.isAllOK()) {

            if (this.pr.enabled()) {
                final String msg = this.nameToPrint + ": contextualized";
                if (this.pr.useThis()) {
                    this.pr.infoln(PrCodes.CTXMONITOR__ALL_OK, "  - " + msg);
                } else if (this.pr.useLogging()) {
                    logger.info(msg);
                }
            }

        } else {
            throw new ExecutionProblem("Incorrect analysis of ctx query?");
        }

        // both oneErrorExists and allOK trigger report(s) if path is configured
        if (this.args.reportDir != null) {
            try {
                this.writeSummary(contextRP);
            } catch (Exception e) {
                final String err = CommonUtil.genericExceptionMessageWrapper(e);
                throw new ExecutionProblem(
                        "Problem writing ctx summary: " + err, e);
            }
        }

        // in all cases get the full node report
        Node_Type[] nodes = null;
        try {
            this.identitiesQuery.setQueryAll(true);
            nodes = this.identitiesQuery.identities();
        } catch (NimbusContextualizationFault e) {
            final String err = CommonUtil.genericExceptionMessageWrapper(e);
            if (this.pr.enabled()) {
                final String errMsg = "Problem querying ctx nodes: " + err;
                if (this.pr.useThis()) {
                    this.pr.errln(errMsg);
                } else if (this.pr.useLogging()) {
                    logger.error(errMsg);
                }
            }
        }

        // both oneErrorExists and allOK trigger report(s) if path is configured
        if (nodes != null && this.args.reportDir != null) {
            try {
                this.writeReports(nodes);
            } catch (Exception e) {
                final String err = CommonUtil.genericExceptionMessageWrapper(e);
                throw new ExecutionProblem(
                        "Problem writing ctx summary: " + err, e);
            }
        }

        if (contextRP.isErrorPresent()) {
            throw new ExitNow(1);
        }
        
        if (nodes != null && this.adjustSshKnownHosts) {
            try {
                adjustKnownHosts(nodes,
                                 this.sshKnownHostsPath,
                                 this.sshKnownHostsDirPath,
                                 this.adjustTasks,
                                 this.pr);
            } catch (Exception e) {
                final String err = CommonUtil.genericExceptionMessageWrapper(e);
                throw new ExecutionProblem(
                        "Problem adjusting known_hosts file @ '" +
                                this.sshKnownHostsPath + "': " + err, e);
            }
        }
    }

    // return: should querying end?
    private boolean analyzeResult(ContextualizationContext contextRP) {
        if (contextRP == null) {
            return false;
        }

        boolean ret = false;
        if (contextRP.isAllOK()) {
            ret = true;
        } else if (contextRP.isErrorPresent()) {
            ret = true;
        }

        if (this.pr.enabled()) {
            if (this.pr.useThis()) {
                this.pr.debugln(getResultString(contextRP, false));
            } else if (this.pr.useLogging()) {
                logger.debug(getResultString(contextRP, false));
            }
        }
        
        return ret;
    }

    private static String getResultString(ContextualizationContext contextRP,
                                          boolean roles) {
        final StringBuffer buf = new StringBuffer("\nCtx query response @ ");
        final String now = localFormat.format(Calendar.getInstance().getTime());
        buf.append(now).append("\n");

        if (contextRP.isNoMoreInjections()) {
            buf.append("  - [X] injects done     ");
        } else {
            buf.append("  - [ ] injects pending  ");
        }
        if (contextRP.isComplete()) {
            buf.append("[X] complete      ");
        } else {
            buf.append("[ ] not complete  ");
        }
        if (contextRP.isAllOK()) {
            buf.append("[X] all OK     ");
        } else {
            buf.append("[ ] no all OK  ");
        }
        if (contextRP.isErrorPresent()) {
            buf.append("[X] error present     ");
        } else {
            buf.append("[ ] no error present  ");
        }

        if (roles) {
            buf.append("\n").append(
                    getRolesString(contextRP.getMatchedRole(), "  - ", "\n"));
        }
        
        return buf.toString();
    }

    public static String getRolesString(MatchedRole_Type[] roles,
                                        String p,
                                        String s) {

        if (roles == null || roles.length == 0) {
            return p + "No roles" + s;
        }

        // first look through to find longest role name and largest total
        int longestNameChars = 0;
        int largestTotal = 0;
        for (MatchedRole_Type role : roles) {
            if (role.getName().length() > longestNameChars) {
                longestNameChars = role.getName().length();
            }
            if (role.getNumProvidersInContext() > largestTotal) {
                largestTotal = role.getNumProvidersInContext();
            }
        }

        // don't want number format via Java because it will add zeroes..
        short numDigits = 0;
        if (largestTotal >= 10000) {  // someday
            numDigits = 5;
        } else if (largestTotal >= 1000) {
            numDigits = 4;
        } else if (largestTotal >= 100) {
            numDigits = 3;
        } else if (largestTotal >= 10) {
            numDigits = 2;
        } else {
            numDigits = 1;
        }

        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < longestNameChars; i++) {
            buf.append(" ");
        }
        final String maxNameStr = buf.toString();

        buf = new StringBuffer();
        for (int i = 0; i < numDigits; i++) {
            buf.append(" ");
        }
        final String maxTotalStr = buf.toString();

        buf = new StringBuffer();

        for (MatchedRole_Type role : roles) {

            final String printName =
                    justifyRight(role.getName(), maxNameStr);
            final String printTotal =
                    justifyRight(Integer.toString(
                            role.getNumProvidersInContext()), maxTotalStr);
            final String printFilled =
                    justifyRight(Integer.toString(
                            role.getNumFilledProviders()), maxTotalStr);
            buf.append(p)
                    .append(printName)
                    .append(": ")
                    .append(printTotal)
                    .append(" total & ")
                    .append(printFilled)
                    .append(" filled.")
                    .append(s);
        }
        return buf.toString();
    }

    private static String justifyRight(String str, String max) {
        if (str == null || max == null) {
            return max;
        }
        final int len = str.length();
        if (len < max.length()) {
            return max.substring(len) + str;
        } else {
            return str;
        }
    }

    // -------------------------------------------------------------------------
    // REPORT WRITING
    // -------------------------------------------------------------------------

    protected void writeSummary(ContextualizationContext contextRP)
            throws SerializationException, IOException {

        final String fileName;
        if (contextRP.isAllOK()) {
            fileName = "CTX-OK.txt";
        } else {
            fileName = "CTX-ERR.txt";
        }

        final File f = new File(this.args.reportDir, fileName);
        FileUtils.writeStringToFile(getResultString(contextRP, true),
                                    f.getAbsolutePath());

        if (this.pr.enabled()) {
            final String msg =
                    "wrote ctx summary to '" + f.getAbsolutePath() + "'";
            if (this.pr.useThis()) {
                this.pr.infoln(PrCodes.CTXMONITOR__REPORT_DIR, "  - " + msg);
            } else if (this.pr.useLogging()) {
                logger.info(msg);
            }
        }
    }

    protected void writeReports(Node_Type[] nodes) {

        final short numDigits;
        if (nodes.length > 1000) {
            numDigits = 4;
        } else if (nodes.length > 100) {
            numDigits = 3;
        } else if (nodes.length > 10) {
            numDigits = 2;
        } else {
            numDigits = 1;
        }

        final NumberFormat format = NumberFormat.getInstance();
        format.setMinimumIntegerDigits(numDigits);
        this._writeReports(nodes, this.args.reportDir, format);

        if (this.pr.enabled()) {
            final String msg = "wrote reports to '" + this.args.reportDir + "'";
            if (this.pr.useThis()) {
                this.pr.infoln(PrCodes.CTXMONITOR__REPORT_DIR, "  - " + msg);
            } else if (this.pr.useLogging()) {
                logger.info(msg);
            }
        }
    }


    protected void _writeReports(Node_Type[] nodes,
                                 String reportDir,
                                 NumberFormat format) {

        for (int i = 0; i < nodes.length; i++) {
            
            final Node_Type node = nodes[i];
            final String prefix = this.getStateString(node) + "-vm-";
            final String path = prefix + format.format(i+1) + ".txt";

            try {
                this.writeOneReport(nodes[i], reportDir, path);
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
        
    protected void writeOneReport(Node_Type node, String dir, String file)
            throws SerializationException, IOException {

        final File f = new File(dir, file);
        FileUtils.writeStringToFile(this.getOneReportText(node),
                                    f.getAbsolutePath());

        if (this.pr.enabled()) {
            final String msg = "    Wrote '" + file + "'";
            if (this.pr.useThis()) {
                this.pr.infoln(PrCodes.CTXMONITOR__SINGLE_REPORT_NAMES, msg);
            } else if (this.pr.useLogging()) {
                logger.info(msg);
            }
        }
    }

    protected String getStateString(Node_Type node) {
        if (node == null) {
            return "UNKNOWN";
        }
        if (!node.isExited()) {
            return "DID-NOT-EXIT";
        }
        if (node.isOk()) {
            return "OK";
        }
        final Short errCode = node.getErrorCode();
        return "ERROR-" + errCode.toString();
    }

    protected String getOneReportText(Node_Type node) {

        final StringBuffer buf = new StringBuffer(16384);

        final String now = localFormat.format(Calendar.getInstance().getTime());

        buf.append("## File autogenerated at ")
           .append(now);
        add2Newlines(buf);

        buf.append(CommonPrint.textDebugSection("STATUS"));
        add2Newlines(buf);
        buf.append(this.getOneStatus(node));
        add2Newlines(buf);

        buf.append(CommonPrint.textDebugSection("IDENTITIES"));
        add2Newlines(buf);
        buf.append(this.getIdentities(node.getIdentity()));
        add2Newlines(buf);

        if (node.getErrorMessage() != null) {
            buf.append(CommonPrint.textDebugSection("ERROR TEXT"));
            add2Newlines(buf);
            add2Newlines(buf);
            buf.append(node.getErrorMessage());
            add2Newlines(buf);
        }

        return buf.toString();
    }

    protected String getOneStatus(Node_Type node) {

        final StringBuffer buf = new StringBuffer(2048);
        buf.append("Node exited: ")
           .append(node.isExited());
        addNewline(buf);
        buf.append("Node OK: ")
           .append(node.isOk());
        addNewline(buf);

        final Short err = node.getErrorCode();
        if (err != null) {
            buf.append("Node ERROR: ")
               .append(err.toString());
            addNewline(buf);
        }
        return buf.toString();
    }
    
    protected String getIdentities(IdentityProvides_Type[] identity) {
        if (identity == null || identity.length == 0) {
            return "No identities";
        }

        final StringBuffer buf = new StringBuffer(2048);

        for (IdentityProvides_Type id : identity) {

            addNewline(buf);

            final String iface = id.get_interface();
            buf.append("INTERFACE NAME: ");
            if (iface != null) {
                buf.append(iface);
            } else {
                buf.append(" (not supplied)");
            }
            addNewline(buf);

            final String ip = id.getIp();
            buf.append("IP ADDRESS: ");
            if (ip != null) {
                buf.append(ip);
            } else {
                buf.append(" (not supplied)");
            }
            addNewline(buf);

            final String hostname = id.getHostname();
            buf.append("HOSTNAME: ");
            if (hostname != null) {
                buf.append(hostname);
            } else {
                buf.append(" (not supplied)");
            }
            addNewline(buf);

            final String sshkey = id.getPubkey();
            buf.append("SSH PUBLIC KEY:");
            if (sshkey != null) {
                buf.append("\n").append(sshkey);
            } else {
                buf.append(" (not supplied)");
            }
            add2Newlines(buf);
        }

        return buf.toString();
    }

    private static void addNewline(StringBuffer buf) {
        buf.append(newline);
    }

    private static void add2Newlines(StringBuffer buf) {
        buf.append(newline).append(newline);
    }

    // -------------------------------------------------------------------------
    // KNOWN HOSTS ADJUSTMENT
    // -------------------------------------------------------------------------

    private static void adjustKnownHosts(Node_Type[] nodes,
                                         String knownHostsPath,
                                         String sshKnownHostsDirPath,
                                         AdjustTask[] adjustTasks,
                                         Print pr)

            throws SerializationException, IOException {

        if (adjustTasks == null || adjustTasks.length == 0) {
            throw new IllegalArgumentException("no adjust tasks");
        }
        if (nodes == null || nodes.length == 0) {
            throw new IllegalArgumentException("no nodes");
        }
        if (pr == null) {
            throw new IllegalArgumentException("pr may not be null");
        }

        if (knownHostsPath != null) {

        pr.debugln("\nknown_hosts adjust path: '" + knownHostsPath + "'");

        for (final AdjustTask task : adjustTasks) {
            for (final Node_Type node : nodes) {

                final IdentityProvides_Type[] ids = node.getIdentity();
                if (ids != null) {
                    for (final IdentityProvides_Type id : ids) {
                        if (task.ipAddress.equals(id.getIp())) {
                            knownhosts_rem(pr, task, node, knownHostsPath);
                            knownhosts_add(pr, task, node, knownHostsPath);
                            break;
                        }
                    }
                }
            }
        }
    }

        if (sshKnownHostsDirPath != null) {

            pr.debugln("\nknown_hosts directory: '" + sshKnownHostsDirPath + "'");

            for (final AdjustTask task : adjustTasks) {
                for (final Node_Type node : nodes) {

                    final IdentityProvides_Type[] ids = node.getIdentity();
                    if (ids != null) {
                        for (final IdentityProvides_Type id : ids) {
                            if (task.ipAddress.equals(id.getIp())) {
                                knownhostsdir_add(pr, task, node,
                                                  sshKnownHostsDirPath);
                                break;
                            }
                        }
                    }
                }
            }

        }
    }

    private static void knownhostsdir_add(Print print,
                                          AdjustTask task,
                                          Node_Type node,
                                          String sshKnownHostsDirPath)
            throws SerializationException, IOException {

        final IdentityProvides_Type[] ids = node.getIdentity();
        for (final IdentityProvides_Type id : ids) {

            if (id.getPubkey() == null) {
                print.errln("No SSH key for " + id.getIp());
                continue;
            }

            // null task.iface means 'get all'
            if (task.iface == null
                    || task.iface.equals(id.get_interface())) {

                // adding via repo.add needs the real key apparently, sending
                // keyStr.getBytes() to HostKey constructor messes everything up

                final String newEntry = id.getHostname() + "," +
                        id.getIp() + " " + id.getPubkey();

                final String sendString = newEntry + "\n";

                final File newfile = new File(sshKnownHostsDirPath,
                                              id.getHostname());

                final String newfilePath = newfile.getAbsolutePath();

                FileUtils.writeStringToFile(sendString,
                                            newfilePath,
                                            false);

                String printString =
                        "\nWrote SSH key out to: " + newfilePath;
                if (task.printName != null) {
                    printString += "  [[ " + task.printName + " ]]";
                }
                print.infoln(printString);
            }
        }
        
    }

    private static void knownhosts_rem(Print print,
                                       AdjustTask task,
                                       Node_Type node,
                                       String knownHostsPath)
            throws SerializationException, IOException {

        final String[] lines =
                FileUtils.readFileAsString(knownHostsPath).split("\n");

        boolean somethingChanged = false;
        
        final IdentityProvides_Type[] ids = node.getIdentity();
        for (final IdentityProvides_Type id : ids) {

            if (id.getPubkey() == null) {
                print.errln("No SSH key for " + id.getIp());
                continue;
            }

            // null task.iface means 'get all'
            if (task.iface == null
                    || task.iface.equals(id.get_interface())) {

                int thisOneChanged = 0;
                for (int j = 0; j < lines.length; j++) {
                    final String line = lines[j];
                    if (line == null || line.trim().length() == 0) {
                        continue;
                    }
                    if (line.trim().startsWith("#")) {
                        continue;
                    }
                    if (line.indexOf(id.getHostname()) >= 0 ||
                            line.indexOf(id.getIp()) >= 0) {
                        lines[j] = "# " + line;
                        somethingChanged = true;
                        thisOneChanged += 1;
                    }
                }

                if (thisOneChanged > 0) {
                    String printString =
                            "\nCommented " + thisOneChanged +
                                    " entries in known_hosts for " +
                                    id.getHostname() + " / " + id.getIp();
                    if (task.printName != null) {
                        printString += "  [[ " + task.printName + " ]]";
                    }
                    print.debugln(printString);
                }
            }
        }

        if (somethingChanged) {
            // no join() ...
            final StringBuffer buf = new StringBuffer(200 * lines.length);
            for (String line : lines) {
                buf.append(line).append("\n");
            }
            FileUtils.writeStringToFile(buf.toString(), knownHostsPath);
        }
    }

    private static void knownhosts_add(Print print,
                                       AdjustTask task,
                                       Node_Type node,
                                       String knownHostsPath)
            throws SerializationException, IOException {

        final IdentityProvides_Type[] ids = node.getIdentity();
        for (final IdentityProvides_Type id : ids) {

            if (id.getPubkey() == null) {
                print.errln("No SSH key for " + id.getIp());
                continue;
            }

            // null task.iface means 'get all'
            if (task.iface == null
                    || task.iface.equals(id.get_interface())) {

                // adding via repo.add needs the real key apparently, sending
                // keyStr.getBytes() to HostKey constructor messes everything up

                final String newEntry = id.getHostname() + "," +
                        id.getIp() + " " + id.getPubkey();

                final String sendString =
                        "\n# cloud-client added this:\n" + newEntry + "\n";
                FileUtils.writeStringToFile(sendString,
                                            knownHostsPath,
                                            true);

                String printString =
                        "\nSSH trusts new key for " + id.getHostname();
                if (task.printName != null) {
                    printString += "  [[ " + task.printName + " ]]";
                }
                print.infoln(printString);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        IdentityProvides_Type id = new IdentityProvides_Type();
        id.set_interface(null);
        id.setHostname("xyz");
        id.setIp("1.2.3.4");
        id.setPubkey("ssh-rsa 132reoi3nfoi3nfoin3f#$@$@#$@#$@#$@#$");

        IdentityProvides_Type[] ids = {id};
        Node_Type node = new Node_Type();
        node.setIdentity(ids);
        Node_Type[] nodes = {node};

        AdjustTask task = new AdjustTask("1.2.3.4", null, "something");
        AdjustTask[] tasks = {task};
        Print pr = new Print(new PrintOpts(null), System.out, System.err, System.err);
        adjustKnownHosts(nodes, "/home/tim/known", null, tasks, pr);
    }
}
