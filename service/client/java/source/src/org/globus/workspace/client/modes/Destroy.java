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

import org.globus.workspace.client_core.print.PrCodes;
import org.globus.workspace.client_core.ParameterProblem;
import org.globus.workspace.client_core.ExecutionProblem;
import org.globus.workspace.client_core.ExitNow;
import org.globus.workspace.client_core.StubConfigurator;
import org.globus.workspace.client_core.utils.EPRUtils;
import org.globus.workspace.client_core.utils.StringUtils;
import org.globus.workspace.client_core.actions.Destroy_Group;
import org.globus.workspace.client_core.actions.Destroy_Instance;
import org.globus.workspace.client_core.actions.Destroy_Ensemble;
import org.globus.workspace.client.AllArguments;
import org.globus.workspace.client.modes.aux.CommonLogs;
import org.globus.workspace.client_common.CommonStrings;
import org.nimbustools.messaging.gt4_0.common.CommonUtil;
import org.globus.workspace.common.print.Print;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.axis.message.addressing.EndpointReferenceType;
import org.oasis.wsrf.faults.BaseFaultType;

public class Destroy extends Mode {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(Destroy.class.getName());

    
    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private String nameToPrint;
    private Destroy_Instance instanceDestroy;
    private Destroy_Group groupDestroy;
    private Destroy_Ensemble ensembleDestroy;
    private EndpointReferenceType epr;
    private boolean dryrun;

    
    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    public Destroy(Print print,
                   AllArguments arguments,
                   StubConfigurator stubConfigurator) {
        super(print, arguments, stubConfigurator);
    }

    
    // -------------------------------------------------------------------------
    // extends Mode
    // -------------------------------------------------------------------------

    public String name() {
        return "Destroy";
    }

    public void validateOptionsImpl() throws ParameterProblem {

        this.validateEndpoint();
        this.setName();
        this.dryrun = this.args.dryrun;
        CommonLogs.logBoolean(this.dryrun, "dryrun mode", this.pr, logger);
    }

    public void runImpl() throws ParameterProblem, ExecutionProblem, ExitNow {
        
        if (this.dryrun) {

            if (this.pr.enabled()) {
                final String msg = "Dryrun, done.";
                if (this.pr.useThis()) {
                    this.pr.infoln(PrCodes.DESTROY__DRYRUN, msg);
                } else if (this.pr.useLogging()) {
                    logger.info(msg);
                }
            }

            return; // *** EARLY RETURN ***
        }

        if (this.pr.enabled() && this.pr.useThis()) {
            final String msg =
                    "Destroying " + this.nameToPrint  + "...";
            this.pr.infoln(PrCodes.ALL_SINGLESHOT_MODES__EXTRALINES, "");
            this.pr.info(PrCodes.ALL_SINGLESHOT_MODES__PRINT_WAITING_DOTS, msg);
            this.pr.flush();
        }

        String type = null;
        try {
            if (this.instanceDestroy != null) {
                this.instanceDestroy.destroy();
            } else if (this.groupDestroy != null) {
                type = "group";
                this.groupDestroy.destroy();
            } else if (this.ensembleDestroy != null) {
                type = "ensemble";
                this.ensembleDestroy.destroy();
            } else {
                throw new IllegalStateException(
                        "there is no port type to destroy with");
            }
        } catch (BaseFaultType e) {
            final String err = CommonStrings.faultStringOrCommonCause(e, type);
            throw new ExecutionProblem(err, e);
        }

        if (this.pr.enabled()) {
            if (this.pr.useThis()) {
                final String msg = " destroyed.";
                this.pr.infoln(PrCodes.ALL_SINGLESHOT_MODES__PRINT_WAITING_DOTS, msg);
            } else if (this.pr.useLogging()) {
                logger.info("Destroyed " + this.nameToPrint);
            }
        }
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

        final String kind;
        if (EPRUtils.isInstanceEPR(this.epr)) {
            this.instanceDestroy =
                    new Destroy_Instance(this.epr, this.stubConf, this.pr);
            kind = "an instance";
        } else if (EPRUtils.isGroupEPR(this.epr)) {
            this.groupDestroy =
                    new Destroy_Group(this.epr,  this.stubConf, this.pr);
            kind = "a group";
        } else if (EPRUtils.isEnsembleEPR(this.epr)) {
            this.ensembleDestroy =
                    new Destroy_Ensemble(this.epr, this.stubConf, this.pr);
            kind = "an ensemble";
        } else {
            throw new ParameterProblem(name() + " requires an instance, " +
                    "group, or ensemble EPR.");
        }

        if (this.pr.enabled()) {
            final String dbg = "Given EPR is " + kind + " EPR";
            if (this.pr.useThis()) {
                this.pr.dbg(dbg);
            } else if (this.pr.useLogging()) {
                logger.debug(dbg);
            }
        }
    }

    private void setName() {

        if (this.args.shortName != null) {
            this.nameToPrint = this.args.shortName;
        } else {
            if (this.instanceDestroy != null) {
                this.nameToPrint = "workspace " +
                        EPRUtils.getIdFromEPR(this.epr);
            } else if (this.groupDestroy != null) {
                this.nameToPrint = "group \"" +
                        EPRUtils.getGroupIdFromEPR(this.epr) + "\"";
            } else if (this.ensembleDestroy != null) {
                this.nameToPrint = "ensemble \"" +
                        EPRUtils.getEnsembleIdFromEPR(this.epr) + "\"";
            }

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
}
