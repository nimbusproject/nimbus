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
import org.globus.workspace.client_core.ExitNow;
import org.globus.workspace.client_core.StubConfigurator;
import org.globus.workspace.client_core.utils.ScheduleUtils;
import org.globus.workspace.client_core.repr.Workspace;
import org.globus.workspace.client_core.repr.State;
import org.globus.workspace.client_core.repr.Schedule;
import org.globus.workspace.client_core.actions.RPQueryInstance;
import org.globus.workspace.client.AllArguments;
import org.globus.workspace.client.modes.aux.SingleShotMode;
import org.globus.workspace.client.modes.aux.CommonLogs;
import org.nimbustools.messaging.gt4_0.common.CommonUtil;
import org.globus.workspace.client_common.CommonStrings;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.oasis.wsrf.faults.BaseFaultType;

public class InstanceQuery extends SingleShotMode {

    private static final Log logger =
            LogFactory.getLog(InstanceQuery.class.getName());

    public InstanceQuery(Print print,
                         AllArguments arguments,
                         StubConfigurator stubConfigurator) {
        super(print, arguments, stubConfigurator);
    }

    public String name() {
        return "RP-Query";
    }

    protected void setInstanceAction() {
        this.instanceAction = new RPQueryInstance(this.epr,
                                                  this.stubConf,
                                                  this.pr);
    }

    protected void setGroupAction() throws ParameterProblem {
        throw new ParameterProblem("Can not query with group EPR.");
    }

    public void runImpl() throws ParameterProblem, ExecutionProblem, ExitNow {

        if (this.args.dryrun) {

            if (this.pr.enabled()) {
                final String msg = "Dryrun, done.";
                if (this.pr.useThis()) {
                    this.pr.infoln(PrCodes.INSTANCERPQUERY__DRYRUN, msg);
                } else if (this.pr.useLogging()) {
                    logger.info(msg);
                }
            }

            return; // *** EARLY RETURN ***
        }

        try {
            final Workspace workspace =
                    ((RPQueryInstance)this.instanceAction).queryOnce();
            this.print(workspace);
        } catch (BaseFaultType e) {
            final String err = CommonStrings.faultStringOrCommonCause(e);
            throw new ExecutionProblem(err, e);
        }
    }

    protected void print(Workspace workspace) {
        CommonLogs.printNetwork(workspace, this.pr, logger);
        this.printSchedule(workspace);
        this.printState(workspace);
    }

    protected void printSchedule(Workspace workspace) {

        if (workspace == null) {
            throw new IllegalArgumentException("workspace may not be null");
        }

        if (!this.pr.enabled()) {
            return; // *** EARLY RETURN ***
        }

        final Schedule schedule = workspace.getCurrentSchedule();

        if (this.pr.useThis()) {
            final String msg = "\nSchedule:";
            this.pr.infoln(PrCodes.INSTANCERPQUERY__SCHEDBANNER, msg);
        }

        final String dash = "  - ";
        
        if (this.pr.useThis()) {
            final String startTime =
                    ScheduleUtils.getStartTimeMessage(schedule, true);
            this.pr.infoln(PrCodes.INSTANCERPQUERY__START_TIME,
                           dash + startTime);
        } else if (this.pr.useLogging()) {
            logger.info(ScheduleUtils.getStartTimeMessage(schedule));
        }

        if (this.pr.useThis()) {
            final String duration =
                    ScheduleUtils.getDurationMessage(schedule, true);
            this.pr.infoln(PrCodes.INSTANCERPQUERY__DURATION, dash + duration);
        } else if (this.pr.useLogging()) {
            logger.info(ScheduleUtils.getDurationMessage(schedule));
        }

        if (this.pr.useThis()) {
            final String downTime =
                    ScheduleUtils.getShutdownMessage(schedule, true);
            this.pr.infoln(PrCodes.INSTANCERPQUERY__SHUTDOWN_TIME,
                           dash + downTime);
        } else if (this.pr.useLogging()) {
            logger.info(ScheduleUtils.getShutdownMessage(schedule));
        }

        if (this.pr.useThis()) {
            final String termTime =
                    ScheduleUtils.getTerminationMessage(schedule, true);
            this.pr.infoln(PrCodes.INSTANCERPQUERY__TERM_TIME,
                           dash + termTime);
        } else if (this.pr.useLogging()) {
            logger.info(ScheduleUtils.getTerminationMessage(schedule));
        }

    }

    protected void printState(Workspace workspace) {

        if (workspace == null) {
            throw new IllegalArgumentException("workspace may not be null");
        }

        if (!this.pr.enabled()) {
            return; // *** EARLY RETURN ***
        }

        final State state = workspace.getCurrentState();

        if (state == null) {
            final String err = "Problem: no state present in workspace query";
            if (this.pr.useThis()) {
                this.pr.errln(PrCodes.INSTANCERPQUERY__STATE_ERROR, err);
            } else if (this.pr.useLogging()) {
                logger.error(err);
            }
            return; // *** EARLY RETURN ***
        }

        final String msg = "State: " + state.getState();
        String err = null;
        final Exception error = state.getProblem();
        if (error != null) {
            err = "Problem: " +
                    CommonUtil.genericExceptionMessageWrapper(error);
        }

        if (this.pr.useThis()) {
            this.pr.infoln();
            this.pr.infoln(PrCodes.INSTANCERPQUERY__STATE, msg);
            if (err != null) {
                this.pr.errln();
                this.pr.errln(PrCodes.INSTANCERPQUERY__STATE_ERROR, err);
            }
        } else if (this.pr.useLogging()) {
            if (err != null) {
                logger.error(msg + ", " + err);
            } else {
                logger.info(msg);
            }
        }

    }
}
