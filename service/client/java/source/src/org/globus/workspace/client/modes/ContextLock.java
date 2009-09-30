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
import org.globus.workspace.client_core.actions.Ctx_Lock;
import org.globus.workspace.common.print.Print;
import org.nimbustools.messaging.gt4_0.common.CommonUtil;
import org.globus.workspace.client.AllArguments;
import org.globus.workspace.client.modes.aux.CommonLogs;
import org.globus.workspace.client_common.CommonStrings;
import org.oasis.wsrf.faults.BaseFaultType;

public class ContextLock extends Mode {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(ContextLock.class.getName());


    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private String nameToPrint;
    private Ctx_Lock contextLock;
    private EndpointReferenceType epr;
    private boolean dryrun;


    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    public ContextLock(Print print,
                       AllArguments arguments,
                       StubConfigurator stubConfigurator) {
        super(print, arguments, stubConfigurator);
    }


    // -------------------------------------------------------------------------
    // extends Mode
    // -------------------------------------------------------------------------

    public String name() {
        return "Lock-context";
    }

    public void validateOptionsImpl() throws ParameterProblem {

        this.validateEndpoint();
        this.dryrun = this.args.dryrun;
        CommonLogs.logBoolean(this.dryrun, "dryrun mode", this.pr, logger);
    }

    public void runImpl() throws ParameterProblem, ExecutionProblem, ExitNow {
        
        if (this.dryrun) {

            if (this.pr.enabled()) {
                final String msg = "Dryrun, done.";
                if (this.pr.useThis()) {
                    this.pr.infoln(PrCodes.CTXLOCK__DRYRUN, msg);
                } else if (this.pr.useLogging()) {
                    logger.info(msg);
                }
            }

            return; // *** EARLY RETURN ***
        }

        if (this.pr.enabled() && this.pr.useThis()) {
            final String msg =
                    "Locking context '" + this.nameToPrint  + "'...";
            this.pr.infoln(PrCodes.ALL_SINGLESHOT_MODES__EXTRALINES, "");
            this.pr.info(PrCodes.ALL_SINGLESHOT_MODES__PRINT_WAITING_DOTS, msg);
            this.pr.flush();
        }

        try {
            if (this.contextLock != null) {
                this.contextLock.lock();
            } else {
                throw new IllegalStateException(
                        "there is no port type to call done/launch with");
            }
        } catch (BaseFaultType e) {
            final String err =
                    CommonStrings.faultStringOrCommonCause(e, "context");
            throw new ExecutionProblem(err, e);
        }

        if (this.pr.enabled()) {
            if (this.pr.useThis()) {
                final String msg = " done.";
                this.pr.infoln(PrCodes.ALL_SINGLESHOT_MODES__PRINT_WAITING_DOTS, msg);
            } else if (this.pr.useLogging()) {
                logger.info("Locked context '" + this.nameToPrint + "'");
            }
        }
    }
    
    
    // -------------------------------------------------------------------------
    // VALIDATION ETC
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

        this.nameToPrint = EPRUtils.getContextIdFromEPR(this.epr);

        this.contextLock = new Ctx_Lock(this.epr, this.stubConf, this.pr);
    }
}
