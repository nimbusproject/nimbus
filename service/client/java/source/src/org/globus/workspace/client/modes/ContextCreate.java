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
import org.apache.axis.message.addressing.Address;
import org.apache.axis.types.URI;
import org.globus.workspace.common.print.Print;
import org.globus.workspace.client.AllArguments;
import org.globus.workspace.client.Opts;
import org.globus.workspace.client.modes.aux.CommonLogs;
import org.globus.workspace.client_core.StubConfigurator;
import org.globus.workspace.client_core.ParameterProblem;
import org.globus.workspace.client_core.ExecutionProblem;
import org.globus.workspace.client_core.ExitNow;
import org.globus.workspace.client_core.actions.Ctx_Create;
import org.globus.workspace.client_core.utils.FileUtils;
import org.globus.workspace.client_core.print.PrCodes;
import org.globus.workspace.client_common.CommonStrings;
import org.globus.wsrf.encoding.ObjectSerializer;
import org.nimbustools.ctxbroker.generated.gt4_0.broker.NimbusContextualizationFault;
import org.nimbustools.ctxbroker.generated.gt4_0.types.CreateContextResponse_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.description.BrokerContactType;

import javax.xml.namespace.QName;

public class ContextCreate extends Mode {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(ContextCreate.class.getName());


    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected Ctx_Create ctx_create;
    protected boolean dryrun;
    protected String contextEPRpath;
    protected String ctxContactXmlPath;


    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    public ContextCreate(Print print,
                         AllArguments arguments,
                         StubConfigurator stubConfigurator) {
        super(print, arguments, stubConfigurator);
    }

    // -------------------------------------------------------------------------
    // extends Mode
    // -------------------------------------------------------------------------

    public String name() {
        return "Create-context";
    }

    public void validateOptionsImpl() throws ParameterProblem {

        this.validateEndpoint();
        this.contextEPRpath = this.args.eprFile;
        this.ctxContactXmlPath = this.args.ctxContactXmlPath;
        this.dryrun = this.args.dryrun;
        CommonLogs.logBoolean(this.dryrun, "dryrun mode", this.pr, logger);
    }

    public void runImpl() throws ParameterProblem, ExecutionProblem, ExitNow {
        this._runImpl();
    }

    
    // -------------------------------------------------------------------------
    // VALIDATION
    // -------------------------------------------------------------------------

    protected void setupAction(EndpointReferenceType epr) {
        this.ctx_create = new Ctx_Create(epr, this.stubConf, this.pr);
    }

    private void validateEndpoint() throws ParameterProblem {

        final EndpointReferenceType epr;
        if (this.stubConf.getEPR() == null) {

            if (this.args.targetServiceUrl == null) {
                throw new ParameterProblem(name() + " requires a " +
                        "Context Broker URL, see \"" +
                        Opts.CTX_CREATE_OPT_STRING + " -h\"");
            }

            try {
                epr = new EndpointReferenceType(
                                new Address(this.args.targetServiceUrl));
            } catch (URI.MalformedURIException e) {
                throw new ParameterProblem("Given context broker URL " +
                        "appears to be invalid: " + e.getMessage(), e);
            }

            this.stubConf.setEPR(epr);
        } else {
            epr = this.stubConf.getEPR();
            this.args.targetServiceUrl = epr.getAddress().toString();
        }

        if (this.pr.enabled()) {
            // address print
            final String msg = "Context Broker:\n    " +
                                    this.args.targetServiceUrl;
            if (this.pr.useThis()) {
                this.pr.infoln(PrCodes.CREATE__CTXBROKER_ENDPOINT,
                               "\n" + msg);
            } else if (this.pr.useLogging()) {
                logger.info(msg);
            }
        }

        this.setupAction(epr);
    }

    
    // -------------------------------------------------------------------------
    // RUN
    // -------------------------------------------------------------------------

    private void _runImpl() throws ParameterProblem, ExecutionProblem, ExitNow {

        if (this.ctx_create == null) {
            throw new ExecutionProblem("run called w/o validate (?)");
        }

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

        CreateContextResponse_Type resp;
        try {
            resp = this.ctx_create.create();
        } catch (NimbusContextualizationFault e) {
            final String err =
                    CommonStrings.faultStringOrCommonCause(e, "context broker");
            throw new ExecutionProblem(err, e);
        }

        this.writeContextEprPossibly(resp.getContextEPR());
        this.writeBrokerContactPossibly(resp.getContact());
    }

    private void writeContextEprPossibly(EndpointReferenceType epr)

            throws ExecutionProblem {

        if (epr == null) {
            return; // *** EARLY RETURN ***
        }

        if (this.contextEPRpath == null) {
            return; // *** EARLY RETURN ***
        }

        final QName eprQName =
            new QName("", this.ctx_create.getSettings().
                                    getGeneratedContextEprElementName());

        try {
            FileUtils.writeEprToFile(epr,
                                     this.contextEPRpath,
                                     eprQName);

            if (this.pr.enabled()) {
                this.pr.infoln(PrCodes.CREATE__EPRFILE_WRITES, "");
                final String msg = "Wrote new context broker EPR to \"" +
                        this.contextEPRpath + "\"";
                if (this.pr.useThis()) {
                    this.pr.infoln(PrCodes.CREATE__EPRFILE_WRITES,
                                   msg);
                } else if (this.pr.useLogging()) {
                    logger.info(msg);
                }
            }

        } catch (Exception e) {
            final String err = "Problem writing EPR to file: ";
            throw new ExecutionProblem(err + e.getMessage(), e);
        }
    }

    private void writeBrokerContactPossibly(BrokerContactType contact)

            throws ExecutionProblem {

        if (contact == null) {
            return; // *** EARLY RETURN ***
        }

        if (this.ctxContactXmlPath == null) {
            return; // *** EARLY RETURN ***
        }

        final QName qName =
            new QName("", this.ctx_create.getSettings().
                                getGeneratedContextBrokerContactElementName());

        try {
            FileUtils.writeStringToFile(
                    ObjectSerializer.toString(contact, qName),
                    this.ctxContactXmlPath);

            if (this.pr.enabled()) {
                this.pr.infoln(PrCodes.CREATE__CTXBROKER_CONTACTINF, "");
                final String msg = "Wrote context broker contact " +
                        "information (for agents to use) to \"" +
                        this.ctxContactXmlPath + "\"";
                if (this.pr.useThis()) {
                    this.pr.infoln(PrCodes.CREATE__CTXBROKER_CONTACTINF,
                                   msg);
                } else if (this.pr.useLogging()) {
                    logger.info(msg);
                }
            }

        } catch (Exception e) {
            final String err = "Problem writing EPR to file: ";
            throw new ExecutionProblem(err + e.getMessage(), e);
        }
    }
}
