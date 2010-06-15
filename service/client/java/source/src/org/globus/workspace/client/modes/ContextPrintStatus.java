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

import org.apache.axis.message.addressing.EndpointReferenceType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.workspace.client.AllArguments;
import org.globus.workspace.client.Opts;
import org.globus.workspace.client.modes.aux.CommonLogs;
import org.globus.workspace.client_common.BaseClient;
import org.globus.workspace.client_core.ExecutionProblem;
import org.globus.workspace.client_core.ExitNow;
import org.globus.workspace.client_core.ParameterProblem;
import org.globus.workspace.client_core.StubConfigurator;
import org.globus.workspace.client_core.actions.Ctx_Identities;
import org.globus.workspace.client_core.print.PrCodes;
import org.globus.workspace.client_core.utils.EPRUtils;
import org.globus.workspace.common.print.Print;
import org.nimbustools.ctxbroker.generated.gt4_0.broker.NimbusContextualizationFault;
import org.nimbustools.ctxbroker.generated.gt4_0.description.IdentityProvides_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.types.Node_Type;
import org.nimbustools.messaging.gt4_0.common.CommonUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ContextPrintStatus extends Mode {
    
    // -------------------------------------------------------------------------
    // INNER CLASS
    // -------------------------------------------------------------------------

    // tuple...
    protected static class IpAndStatus {
        public final String ip;
        public final String status;

        protected IpAndStatus(String ip, String status) {
            if (ip == null) {
                throw new IllegalArgumentException("ip is missing");
            }
            if (status == null) {
                throw new IllegalArgumentException("status is missing");
            }
            this.ip = ip;
            this.status = status;
        }
    }

    
    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(ContextPrintStatus.class.getName());

    public static final String CTX_SUCCESS = "[CTX_SUCCESS]";
    public static final String CTX_ERROR = "[CTX_ERROR]";
    public static final String CTX_CHECKED_IN_PENDING = "[CTX_CHECKED_IN_PENDING]";
    public static final String CTX_NOT_CHECKED_IN = "[CTX_NOT_CHECKED_IN]";


    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private Ctx_Identities identitiesQuery;
    private boolean dryrun;

    
    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    public ContextPrintStatus(Print print,
                          AllArguments arguments,
                          StubConfigurator stubConfigurator) {
        super(print, arguments, stubConfigurator);
    }


    // -------------------------------------------------------------------------
    // extends Mode
    // -------------------------------------------------------------------------

    public String name() {
        return "Context-pending-print";
    }

    public void validateOptionsImpl() throws ParameterProblem {

        this.validateEndpoint();
        this.validateIpDir();
        this.dryrun = this.args.dryrun;
        CommonLogs.logBoolean(this.dryrun, "dryrun mode", this.pr, logger);
    }

    public void runImpl() throws ParameterProblem, ExecutionProblem, ExitNow {
        this._runImpl();
    }

    
    // -------------------------------------------------------------------------
    // VALIDATION
    // -------------------------------------------------------------------------

    private void validateEndpoint() throws ParameterProblem {

        final EndpointReferenceType epr = this.stubConf.getEPR();

        if (epr == null) {
            throw new ParameterProblem(name() + " requires EPR");
        }

        final String eprStr;
        try {
            eprStr = EPRUtils.eprToString(epr);
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

        this.identitiesQuery =
                new Ctx_Identities(epr, this.stubConf, this.pr);
    }

    private void validateIpDir() throws ParameterProblem {

        if (this.args.eprIdDir == null) {
            throw new ParameterProblem(name() + " requires --" + Opts.EPR_ID_DIR_OPT_STRING);
        }

        final File adir = new File(this.args.eprIdDir);
        if (!adir.exists()) {
            throw new ParameterProblem(
                    "Does not exist: " + this.args.eprIdDir);
        }
        if (!adir.isDirectory()) {
            throw new ParameterProblem(
                    "Must be a directory: " + this.args.eprIdDir);
        }
        if (!adir.canRead()) {
            throw new ParameterProblem(
                    "Can not read: " + this.args.eprIdDir);
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
                    this.pr.infoln(PrCodes.CTXPRINTSTATUS__DRYRUN, msg);
                } else if (this.pr.useLogging()) {
                    logger.info(msg);
                }
            }

            return; // *** EARLY RETURN ***
        }

        final List<String> knownIps = this.gatherIpAddressesFromFilesystem();
        
        if (knownIps.isEmpty()) {
            if (this.pr.enabled()) {
                final String msg =
                        "Could not find any IP addresses to analyze, is this directory " +
                                "empty? '" + this.args.eprIdDir + '\'';
                if (this.pr.useThis()) {
                    this.pr.errln(PrCodes.CTXPRINTSTATUS__ONE_ERROR, msg);
                } else if (this.pr.useLogging()) {
                    logger.error(msg);
                }
            }
            throw new ExitNow(BaseClient.APPLICATION_EXIT_CODE);
        }

        final Node_Type[] nodes = this.queryContextBroker();

        final List<IpAndStatus> results = this.analyze(knownIps, nodes);

        // if all are OK, it exits normally
        boolean notAllOK = false;

        String longestString = "";
        for (IpAndStatus result : results) {
            if (longestString.length() < result.ip.length()) {
                longestString = result.ip;
            }
        }
        final int longestStringLen = longestString.length();

        for (IpAndStatus result : results) {

            if (!result.status.equals(CTX_SUCCESS)) {
                notAllOK = true;
            }

            if (this.pr.enabled()) {

                final StringBuilder printIp = new StringBuilder(result.ip);
                while (printIp.length() < longestStringLen) {
                    printIp.append(' ');
                }
                printIp.append("    ").append(result.status);
                
                if (this.pr.useThis()) {
                    this.pr.infoln(PrCodes.CTXPRINTSTATUS__ONE_IP, printIp.toString());
                } else if (this.pr.useLogging()) {
                    logger.info(printIp.toString());
                }
            }
        }

        if (notAllOK) {
            throw new ExitNow(BaseClient.CTX_PENDING_RESULTS);
        }
    }

    private List<IpAndStatus> analyze(Collection<String> knownIps, Node_Type[] nodes) {

        final List<IpAndStatus> results = new ArrayList<IpAndStatus>(knownIps.size());

        for (String knownIp : knownIps) {

            final String dbg = "Analyzing known ip " + knownIp;
            if (this.pr.useThis()) {
                this.pr.dbg(dbg);
            } else if (this.pr.useLogging()) {
                logger.debug(dbg);
            }

            boolean foundIp = false;

            for (Node_Type node : nodes) {
                final IdentityProvides_Type[] identities = node.getIdentity();
                for (IdentityProvides_Type identity : identities) {
                    final String ip = identity.getIp();
                    if (ip != null && ip.equals(knownIp)) {

                        final String status = this.getNodeState(node);
                        results.add(new IpAndStatus(knownIp, status));
                        
                        final String dbg2 = "  - found context node with that ip: " + status;
                        if (this.pr.useThis()) {
                            this.pr.dbg(dbg2);
                        } else if (this.pr.useLogging()) {
                            logger.debug(dbg2);
                        }

                        foundIp = true;
                        break;
                    }
                }
            }

            if (!foundIp) {

                results.add(new IpAndStatus(knownIp, CTX_NOT_CHECKED_IN));

                final String dbg2 = "  - did not find context node with that ip";
                if (this.pr.useThis()) {
                    this.pr.dbg(dbg2);
                } else if (this.pr.useLogging()) {
                    logger.debug(dbg2);
                }
            }

        }

        return results;
    }

    private String getNodeState(Node_Type node) {
        if (node == null) {
            throw new IllegalArgumentException("node is missing");
        }
        if (!node.isExited()) {
            return CTX_CHECKED_IN_PENDING;
        } else if (node.isOk()) {
            return CTX_SUCCESS;
        } else {
            return CTX_ERROR;
        }
    }

    private Node_Type[] queryContextBroker() throws ExitNow,
                                                    ExecutionProblem,
                                                    ParameterProblem {
        final Node_Type[] nodes;
        try {
            this.identitiesQuery.setQueryAll(true);
            nodes = this.identitiesQuery.identities();
        } catch (NimbusContextualizationFault e) {
            final String err = CommonUtil.genericExceptionMessageWrapper(e);
            if (this.pr.enabled()) {
                final String errMsg = "Problem querying context broker: " + err;
                if (this.pr.useThis()) {
                    this.pr.errln(errMsg);
                } else if (this.pr.useLogging()) {
                    logger.error(errMsg);
                }
            }
            throw new ExitNow(1);
        }
        return nodes;
    }

    private List<String> gatherIpAddressesFromFilesystem() throws ExitNow {
        
        final List<String> knownIps = new ArrayList<String>(10);
        final File idDir = new File(this.args.eprIdDir);
        final File[] idFiles = idDir.listFiles();
        for (File idFile : idFiles) {
            final String[] parts = idFile.getName().split("-");
            if (parts == null || parts.length != 2) {
                if (this.pr.enabled()) {
                    final String msg =
                            "id directory " + this.args.eprIdDir +
                                    " contains unexpected file name: " + idFile.getName();
                    if (this.pr.useThis()) {
                        this.pr.errln(PrCodes.CTXPRINTSTATUS__ONE_ERROR, msg);
                    } else if (this.pr.useLogging()) {
                        logger.error(msg);
                    }
                }
                throw new ExitNow(BaseClient.APPLICATION_EXIT_CODE);
            }
            knownIps.add(parts[1]);
            final String dbg = "Found locally known node ip " + parts[1];
            if (this.pr.useThis()) {
                this.pr.dbg(dbg);
            } else if (this.pr.useLogging()) {
                logger.debug(dbg);
            }
        }

        return knownIps;
    }
}
