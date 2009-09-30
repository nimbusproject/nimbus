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
import org.globus.workspace.client.modes.aux.CommonLogs;
import org.globus.workspace.client_common.CommonStrings;
import org.globus.workspace.client_core.ExecutionProblem;
import org.globus.workspace.client_core.ExitNow;
import org.globus.workspace.client_core.ParameterProblem;
import org.globus.workspace.client_core.StubConfigurator;
import org.globus.workspace.client_core.actions.Ctx_Retrieve;
import org.globus.workspace.client_core.print.PrCodes;
import org.globus.workspace.client_core.utils.EPRUtils;
import org.globus.workspace.client_core.utils.FileUtils;
import org.globus.workspace.common.print.Print;
import org.nimbustools.ctxbroker.generated.gt4_0.description.Cloudcluster_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.description.IdentityProvides_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.description.AgentDescription_Type;
import org.nimbustools.messaging.gt4_0.common.CommonUtil;
import org.oasis.wsrf.faults.BaseFaultType;

/**
 * Calls retrieve on the broker, acting like a context agent.  Something a
 * developer would use for looking at SOAP messages etc. (unless you are
 * developing a Java based context agent).
 *
 * Currently no way to impersonate different identities w/o a recompile.
 */
public class ContextAgentImpersonate extends Mode {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(ContextAgentImpersonate.class.getName());


    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private String nameToPrint;
    private Ctx_Retrieve ctxRetrieve;
    private boolean dryrun;


    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    public ContextAgentImpersonate(Print print,
                                   AllArguments arguments,
                                   StubConfigurator stubConfigurator) {
        super(print, arguments, stubConfigurator);
    }


    // -------------------------------------------------------------------------
    // extends Mode
    // -------------------------------------------------------------------------

    public String name() {
        return "Context-agent-impersonate";
    }

    public void validateOptionsImpl() throws ParameterProblem {

        this.validateEndpoint();
        this.validateArgs();
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
                    "Sending retrieve to context '" +
                            this.nameToPrint  + "'...";
            this.pr.infoln(PrCodes.ALL_SINGLESHOT_MODES__EXTRALINES, "");
            this.pr.info(PrCodes.ALL_SINGLESHOT_MODES__PRINT_WAITING_DOTS, msg);
            this.pr.flush();
        }

        try {
            if (this.ctxRetrieve != null) {
                this.ctxRetrieve.retrieve();
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
                logger.info("Retrieved from '" +
                            this.nameToPrint + "'");
            }
        }
    }
    

    // -------------------------------------------------------------------------
    // VALIDATION ETC
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

        this.nameToPrint = EPRUtils.getContextIdFromEPR(epr);

        this.ctxRetrieve =
                new Ctx_Retrieve(epr, this.stubConf, this.pr);
    }

    private void validateArgs() throws ParameterProblem {

        if (this.ctxRetrieve == null) {
            throw new ParameterProblem("expected that Ctx_Retrieve object " +
                    "would be set up before this method was called");
        }

        if (this.args.clusterForImpersonationPath == null) {
            throw new ParameterProblem("missing path to cluster file to use " +
                    "for ctx agent retrieve operation");
        }

        final AgentDescription_Type args = new AgentDescription_Type();
        
        IdentityProvides_Type[] ids = new IdentityProvides_Type[2];
        ids[0] = this.getFake_0();
        ids[1] = this.getFake_1();
        args.setIdentity(ids);

        final Cloudcluster_Type clusta;
        try {
            clusta = FileUtils.getClusterDocForRetrieve(
                            this.pr, this.args.clusterForImpersonationPath);
        } catch (Exception e) {
            throw new ParameterProblem("Problem reading in the cluster " +
                    "ctx doc: " + e.getMessage(), e);
        }

        args.setCluster(clusta);
        this.ctxRetrieve.setRetrieveSend(args);
    }

    private IdentityProvides_Type getFake_0() {
        final IdentityProvides_Type id = new IdentityProvides_Type();
        id.set_interface("publicnic");
        id.setIp("1.2.3.4");
        id.setHostname("example.com");
        id.setPubkey("pubkey text");
        return id;
    }

    private IdentityProvides_Type getFake_1() {
        final IdentityProvides_Type id = new IdentityProvides_Type();
        id.set_interface("localnic");
        id.setIp("7.8.9.0");
        id.setHostname("example2.com");
        id.setPubkey("pubkey text 2");
        return id;
    }
}
