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

package org.globus.workspace.client.modes.aux;

import org.globus.workspace.client.modes.Mode;
import org.globus.workspace.client.AllArguments;
import org.globus.workspace.common.print.Print;
import org.globus.workspace.client_core.StubConfigurator;
import org.globus.workspace.client_core.ParameterProblem;
import org.globus.workspace.client_core.WSAction_Instance;
import org.globus.workspace.client_core.WSAction_Group;
import org.globus.workspace.client_core.utils.EPRUtils;
import org.globus.workspace.client_core.utils.StringUtils;
import org.nimbustools.messaging.gt4_0.common.CommonUtil;
import org.apache.axis.message.addressing.EndpointReferenceType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class SingleShotMode extends Mode {

    
    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    protected static final Log logger =
            LogFactory.getLog(SingleShotMode.class.getName());


    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected String nameToPrint;
    protected EndpointReferenceType epr;
    protected boolean dryrun;

    protected WSAction_Instance instanceAction;
    protected WSAction_Group groupAction;


    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    protected SingleShotMode(Print print,
                             AllArguments arguments,
                             StubConfigurator stubConfigurator) {
        super(print, arguments, stubConfigurator);
    }

    
    // -------------------------------------------------------------------------
    // VALIDATION
    // -------------------------------------------------------------------------

    protected abstract void setInstanceAction() throws ParameterProblem;
    
    protected abstract void setGroupAction() throws ParameterProblem;
    
    protected void validateOptionsImpl() throws ParameterProblem {

        this.validateEndpoint();
        this.setName();
        this.dryrun = this.args.dryrun;
        CommonLogs.logBoolean(this.dryrun, "dryrun mode", this.pr, logger);
    }

    protected void validateEndpoint() throws ParameterProblem {

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
            this.setInstanceAction();
            kind = "an instance";
        } else if (EPRUtils.isGroupEPR(this.epr)) {
            this.setGroupAction();
            kind = "a group";
        } else {
            throw new ParameterProblem(name() + " requires a valid EPR.");
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
            if (this.instanceAction != null) {
                this.nameToPrint = "workspace " +
                        EPRUtils.getIdFromEPR(this.epr);
            } else if (this.groupAction != null) {
                this.nameToPrint = "group \"" +
                        EPRUtils.getGroupIdFromEPR(this.epr) + "\"";
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
