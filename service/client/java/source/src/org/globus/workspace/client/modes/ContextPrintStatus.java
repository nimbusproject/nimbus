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
import org.globus.workspace.client_common.CommonStrings;
import org.globus.workspace.client_core.ExecutionProblem;
import org.globus.workspace.client_core.ExitNow;
import org.globus.workspace.client_core.ParameterProblem;
import org.globus.workspace.client_core.StubConfigurator;
import org.globus.workspace.client_core.actions.Ctx_Identities;
import org.globus.workspace.client_core.actions.Ctx_RPQuery;
import org.globus.workspace.client_core.print.PrCodes;
import org.globus.workspace.client_core.utils.EPRUtils;
import org.globus.workspace.common.print.Print;
import org.nimbustools.ctxbroker.generated.gt4_0.broker.NimbusContextualizationFault;
import org.nimbustools.ctxbroker.generated.gt4_0.description.IdentityProvides_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.types.ContextualizationContext;
import org.nimbustools.ctxbroker.generated.gt4_0.types.Node_Type;
import org.nimbustools.messaging.gt4_0.common.CommonUtil;
import org.oasis.wsrf.faults.BaseFaultType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ContextPrintStatus extends Mode {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(ContextPrintStatus.class.getName());


    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private String nameToPrint;
    private Ctx_RPQuery rpQuery;
    private Ctx_Identities identitiesQuery;
    private EndpointReferenceType epr;
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
        this.setName();
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

        final ContextualizationContext contextRP;
        try {
            contextRP = this.rpQuery.query();
        } catch (BaseFaultType e) {
            final String err =
                    CommonStrings.faultStringOrCommonCause(e, "context");
            throw new ExecutionProblem(err, e);
        }

        if (contextRP.isErrorPresent()) {

            if (this.pr.enabled()) {
                final String msg =
                        "Error reported to context broker for " + this.nameToPrint;
                if (this.pr.useThis()) {
                    this.pr.errln(PrCodes.CTXPRINTSTATUS__ONE_ERROR, msg);
                } else if (this.pr.useLogging()) {
                    logger.error(msg);
                }
            }
            throw new ExitNow(BaseClient.APPLICATION_EXIT_CODE);

        } else if (contextRP.isAllOK()) {

            if (this.pr.enabled()) {
                final String msg = "Context broker reports that all nodes for '" +
                        this.nameToPrint + "' have contextualized.";
                if (this.pr.useThis()) {
                    this.pr.infoln(PrCodes.CTXPRINTSTATUS__ALL_OK, msg);
                } else if (this.pr.useLogging()) {
                    logger.info(msg);
                }
            }
            throw new ExitNow(BaseClient.SUCCESS_EXIT_CODE);
        }
        
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

        final Node_Type[] nodes;
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
            throw new ExitNow(1);
        }


        final List<String> printIps = new ArrayList<String>(knownIps.size());
        
        // this is finally what the end user is asking for, print each IP address that we
        // know about locally but does not exist in context or is in context without an OK
        
        for (String knownIp : knownIps) {

            final String dbg = "Analyzing known ip " + knownIp;
            if (this.pr.useThis()) {
                this.pr.dbg(dbg);
            } else if (this.pr.useLogging()) {
                logger.debug(dbg);
            }

            boolean printThis = true;
            for (Node_Type node : nodes) {
                final IdentityProvides_Type[] identities = node.getIdentity();
                for (IdentityProvides_Type identity : identities) {
                    final String ip = identity.getIp();
                    if (ip != null && ip.equals(knownIp)) {
                        String tail = " [not OK]";
                        if (node.isOk()) {
                            tail = " [OK]";
                            printThis = false;
                        }

                        final String dbg2 = "  - found context node with that ip " + tail;
                        if (this.pr.useThis()) {
                            this.pr.dbg(dbg2);
                        } else if (this.pr.useLogging()) {
                            logger.debug(dbg2);
                        }
                    }
                }
            }

            if (printThis) {
                printIps.add(knownIp);
            }
        }

        for (String printIp : printIps) {
            if (this.pr.enabled()) {
                if (this.pr.useThis()) {
                    this.pr.infoln(PrCodes.CTXPRINTSTATUS__ONE_IP, printIp);
                } else if (this.pr.useLogging()) {
                    logger.info(printIp);
                }
            }
        }

        throw new ExitNow(BaseClient.CTX_PENDING_RESULTS);
    }

    private void setName() {

        if (this.args.shortName != null) {
            this.nameToPrint = this.args.shortName;
        } else {
            this.nameToPrint = '\"' +
                        EPRUtils.getContextIdFromEPR(this.epr) + '\"';
        }

        if (this.pr.enabled()) {
            final String dbg = "Name to print: '" + this.nameToPrint + '\'';
            if (this.pr.useThis()) {
                this.pr.dbg(dbg);
            } else if (this.pr.useLogging()) {
                logger.debug(dbg);
            }
        }
    }
}
