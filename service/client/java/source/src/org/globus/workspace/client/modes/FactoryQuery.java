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

import org.globus.workspace.common.print.Print;
import org.globus.workspace.client_core.print.PrCodes;
import org.globus.workspace.client_core.ParameterProblem;
import org.globus.workspace.client_core.ExecutionProblem;
import org.globus.workspace.client_core.StubConfigurator;
import org.globus.workspace.client_core.repr.FactoryRPs;
import org.globus.workspace.client_core.actions.RPQueryFactory;
import org.globus.workspace.client_core.utils.EPRUtils;
import org.globus.workspace.client_core.utils.ScheduleUtils;
import org.globus.workspace.client.AllArguments;
import org.globus.wsrf.utils.AddressingUtils;
import org.apache.axis.message.addressing.EndpointReferenceType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.URL;
import java.net.MalformedURLException;

public class FactoryQuery extends Mode {

    private static final Log logger =
            LogFactory.getLog(FactoryQuery.class.getName());

    RPQueryFactory rpQuery;

    // Factory target:
    EndpointReferenceType factoryEPR;

    public FactoryQuery(Print print,
                        AllArguments arguments,
                        StubConfigurator stubConfigurator) {
        super(print, arguments, stubConfigurator);
    }

    public String name() {
        return "Factory-RP-Query";
    }

    protected void validateOptionsImpl() throws ParameterProblem {
        this.validateEndpoint();
        this.rpQuery = new RPQueryFactory(this.factoryEPR,
                                          this.stubConf,
                                          this.pr);
    }

    // TODO: this is duplicated code from Deploy mode
    private void validateEndpoint() throws ParameterProblem {

        if (this.stubConf.getEPR() == null) {

            final String urlString;
            if (this.args.targetServiceUrl == null) {
                urlString = EPRUtils.defaultFactoryUrlString;
            } else {
                urlString = this.args.targetServiceUrl;
            }

            final URL factoryURL;
            try {
                factoryURL = new URL(urlString);
            } catch (MalformedURLException e) {
                throw new ParameterProblem("Given factory service URL " +
                        "appears to be invalid: " + e.getMessage(), e);
            }

            if (this.pr.enabled()) {
                // address print
                final String msg = "Workspace Factory Service:\n    " +
                                        factoryURL.toString();
                if (this.pr.useThis()) {
                    this.pr.infoln(PrCodes.CREATE__FACTORY_ENDPOINT,
                                   msg);
                } else if (this.pr.useLogging()) {
                    logger.info(msg);
                }
            }

            try {
                this.factoryEPR = AddressingUtils.createEndpointReference(
                                       urlString, EPRUtils.defaultFactoryKey());
            } catch (Exception e) {
                final String err = "Problem creating factory endpoint: ";
                throw new ParameterProblem(err + e.getMessage(), e);
            }

            this.stubConf.setEPR(this.factoryEPR);

        } else {

            this.factoryEPR = this.stubConf.getEPR();

            final String eprStr;
            try {
                eprStr = EPRUtils.eprToString(this.factoryEPR);
            } catch (Exception e) {
                throw new ParameterProblem(e.getMessage(), e);
            }

            if (this.pr.enabled()) {
                // xml print
                final String dbg =
                        "\nWorkspace Factory Service EPR:" +
                                "\n------------------------------\n" +
                                    eprStr + "------------------------------\n";

                if (this.pr.useThis()) {
                    this.pr.dbg(dbg);
                } else if (this.pr.useLogging()) {
                    logger.debug(dbg);
                }
            }

            if (this.pr.enabled()) {
                // address print
                final String msg = "\nWorkspace Factory Service:\n    " +
                                this.factoryEPR.getAddress().toString() + "\n";
                if (this.pr.useThis()) {
                    this.pr.infoln(PrCodes.CREATE__FACTORY_ENDPOINT,
                                   msg);
                } else if (this.pr.useLogging()) {
                    logger.info(msg);
                }
            }
        }
    }


    public void runImpl() throws ParameterProblem, ExecutionProblem {

        if (this.rpQuery == null) {
            throw new ExecutionProblem("validation was not run?");
        }
        
        if (this.args.dryrun) {

            if (this.pr.enabled()) {
                final String msg = "Dryrun, done.";
                if (this.pr.useThis()) {
                    this.pr.infoln(PrCodes.FACTORYRPQUERY__DRYRUN, msg);
                } else if (this.pr.useLogging()) {
                    logger.info(msg);
                }
            }

            return; // *** EARLY RETURN ***
        }

        print(this.rpQuery.queryOnce());
    }

    protected void print(FactoryRPs rps) {
        this.printDefaultRunningSeconds(rps);
        this.printMaximumRunningSeconds(rps);
        this.printAssociations(rps);
        this.printVMM(rps);
        this.printCPUarch(rps);
    }

    protected void printDefaultRunningSeconds(FactoryRPs rps) {
        
        if (rps == null) {
            throw new IllegalArgumentException("rps may not be null");
        }

        if (!this.pr.enabled()) {
            return; // *** EARLY RETURN ***
        }

        String defaultRunning = "Default Running Time: ";
        final int defaultRunningSeconds = rps.getDefaultRunningSeconds();
        final int defaultRunningMinutes = defaultRunningSeconds / 60;
        if (ScheduleUtils.isWholeMinute(defaultRunningSeconds)) {
            defaultRunning += defaultRunningMinutes + " minutes.";
        } else {
            defaultRunning += "~" + defaultRunningMinutes +
                    " minutes (" + defaultRunningSeconds + " seconds).";
        }

        if (this.pr.useThis()) {
            this.pr.infoln(PrCodes.FACTORYRPQUERY__DEFAULT_MINUTES,
                           defaultRunning);
        } else if (this.pr.useLogging()) {
            logger.info(defaultRunning);
        }
    }

    protected void printMaximumRunningSeconds(FactoryRPs rps) {

        if (rps == null) {
            throw new IllegalArgumentException("rps may not be null");
        }

        if (!this.pr.enabled()) {
            return; // *** EARLY RETURN ***
        }

        String maxRunning = "Maximum Running Time: ";
        final int maxRunningSeconds = rps.getMaximumRunningSeconds();
        final int maxRunningMinutes = maxRunningSeconds / 60;
        if (ScheduleUtils.isWholeMinute(maxRunningSeconds)) {
            maxRunning += maxRunningMinutes + " minutes.";
        } else {
            maxRunning += "~" + maxRunningMinutes +
                    " minutes (" + maxRunningSeconds + " seconds).";
        }

        if (this.pr.useThis()) {
            this.pr.infoln(PrCodes.FACTORYRPQUERY__MAX_MINUTES,
                           maxRunning);
        } else if (this.pr.useLogging()) {
            logger.info(maxRunning);
        }
    }
    
    protected void printAssociations(FactoryRPs rps) {

        if (rps == null) {
            throw new IllegalArgumentException("rps may not be null");
        }

        if (!this.pr.enabled()) {
            return; // *** EARLY RETURN ***
        }

        // never null
        final String[] assocs = rps.getAssociations();
        
        if (assocs.length == 0) {
            
            final String msg = "No networks.";
            if (this.pr.useThis()) {
                this.pr.infoln(PrCodes.FACTORYRPQUERY__ASSOCS,
                               msg);
            } else if (this.pr.useLogging()) {
                logger.info(msg);
            }

            return; // *** EARLY RETURN ***
        }

        final StringBuffer buf = new StringBuffer("Network(s): ");
        buf.append(assocs[0]);

        for (int i = 1; i < assocs.length; i++) {
            buf.append(", ")
               .append(assocs[i]);
        }

        if (this.pr.useThis()) {
            this.pr.infoln(PrCodes.FACTORYRPQUERY__ASSOCS,
                           buf.toString());
        } else if (this.pr.useLogging()) {
            logger.info(buf.toString());
        }
    }

    protected void printVMM(FactoryRPs rps) {

        if (rps == null) {
            throw new IllegalArgumentException("rps may not be null");
        }

        if (!this.pr.enabled()) {
            return; // *** EARLY RETURN ***
        }

        if (rps.getVMM() == null) {
            return; // *** EARLY RETURN ***
        }

        final String msg = "VMM: " + rps.getVMM();
        if (this.pr.useThis()) {
            this.pr.infoln(PrCodes.FACTORYRPQUERY__VMM, msg);
        } else if (this.pr.useLogging()) {
            logger.info(msg);
        }

        // never null
        final String[] versions = rps.getVmmVersions();
        
        if (versions.length == 0) {
            return; // *** EARLY RETURN ***
        }

        final StringBuffer buf = new StringBuffer(" - VMM version(s): ");
        buf.append(versions[0]);

        for (int i = 1; i < versions.length; i++) {
            buf.append(", ")
               .append(versions[i]);
        }

        if (this.pr.useThis()) {
            this.pr.infoln(PrCodes.FACTORYRPQUERY__VMM_VERSIONS,
                           buf.toString());
        } else if (this.pr.useLogging()) {
            logger.info(buf.toString());
        }
    }

    protected void printCPUarch(FactoryRPs rps) {

        if (rps == null) {
            throw new IllegalArgumentException("rps may not be null");
        }

        if (!this.pr.enabled()) {
            return; // *** EARLY RETURN ***
        }

        if (rps.getCpuArchitectureName() == null) {
            return; // *** EARLY RETURN ***
        }

        final String msg = "CPU architecture: " + rps.getCpuArchitectureName();
        if (this.pr.useThis()) {
            this.pr.infoln(PrCodes.FACTORYRPQUERY__CPU_ARCH, msg);
        } else if (this.pr.useLogging()) {
            logger.info(msg);
        }
    }
}
