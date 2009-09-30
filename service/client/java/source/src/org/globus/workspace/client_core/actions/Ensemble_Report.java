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

package org.globus.workspace.client_core.actions;

import org.apache.axis.message.addressing.EndpointReferenceType;
import org.globus.workspace.client_core.ExecutionProblem;
import org.globus.workspace.client_core.ParameterProblem;
import org.globus.workspace.client_core.StubConfigurator;
import org.globus.workspace.client_core.WSAction_Ensemble;
import org.globus.workspace.client_core.repr.Networking;
import org.globus.workspace.client_core.repr.Schedule;
import org.globus.workspace.client_core.repr.State;
import org.globus.workspace.client_core.repr.Workspace;
import org.globus.workspace.client_core.utils.RMIUtils;
import org.globus.workspace.client_core.utils.EPRUtils;
import org.globus.workspace.common.print.Print;
import org.nimbustools.messaging.gt4_0.common.CommonUtil;
import org.nimbustools.messaging.gt4_0.generated.ensemble.WorkspaceEnsembleFault;
import org.nimbustools.messaging.gt4_0.generated.ensemble.WorkspaceEnsemblePortType;
import org.nimbustools.messaging.gt4_0.generated.metadata.logistics.VirtualNetwork_Type;
import org.nimbustools.messaging.gt4_0.generated.types.OneReport_Type;
import org.nimbustools.messaging.gt4_0.generated.types.ReportResponse_Type;
import org.nimbustools.messaging.gt4_0.generated.types.ReportSend_Type;
import org.nimbustools.messaging.gt4_0.generated.types.Schedule_Type;
import org.nimbustools.messaging.gt4_0.generated.types.WorkspaceUnknownFault;
import org.nimbustools.messaging.gt4_0.generated.types.CurrentState_Enumeration;

import java.rmi.RemoteException;
import java.util.Calendar;
import java.text.DateFormat;

public class Ensemble_Report extends WSAction_Ensemble {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final DateFormat localFormat =
            DateFormat.getDateTimeInstance();

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected boolean responseOnlyIfError;
    protected CurrentState_Enumeration returnOnlyIfAll;

    private static final Workspace[] EMPTY_RESPONSE = new Workspace[0];

    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    /**
     * @see WSAction_Ensemble
     */
    public Ensemble_Report(EndpointReferenceType epr,
                           StubConfigurator stubConf,
                           Print debug) {
        super(epr, stubConf, debug);
    }

    /**
     * @see WSAction_Ensemble
     */
    public Ensemble_Report(WorkspaceEnsemblePortType ensemblePortType,
                           Print debug) {
        super(ensemblePortType, debug);
    }

    // -------------------------------------------------------------------------
    // GET/SET OPTIONS
    // -------------------------------------------------------------------------

    public boolean isResponseOnlyIfError() {
        return this.responseOnlyIfError;
    }

    public void setResponseOnlyIfError(boolean responseOnlyIfError) {
        this.responseOnlyIfError = responseOnlyIfError;
    }

    public CurrentState_Enumeration getReturnOnlyIfAll() {
        return this.returnOnlyIfAll;
    }

    public void setReturnOnlyIfAll(CurrentState_Enumeration returnOnlyIfAll) {
        this.returnOnlyIfAll = returnOnlyIfAll;
    }

    // -------------------------------------------------------------------------
    // EXECUTE
    // -------------------------------------------------------------------------

    /**
     * Calls report()
     *
     * @return Workspace[], never null (may be empty)
     * @throws Exception see report()
     * @see #report()
     */
    protected Object action() throws Exception {
        this.report();
        return null;
    }

    /**
     * Calls 'report' on ensemble resource.
     *
     * @throws org.globus.workspace.client_core.ParameterProblem validation problem
     * @throws org.globus.workspace.client_core.ExecutionProblem general problem running (connection errors etc)
     * @throws WorkspaceEnsembleFault severe
     * @throws WorkspaceUnknownFault gone
     * @return Workspace[] result, never null (may be empty)
     */
    public Workspace[] report() throws ParameterProblem,
                                 ExecutionProblem,
                                 WorkspaceUnknownFault,
                                 WorkspaceEnsembleFault {

        this.validateAll();

        final ReportResponse_Type response;
        try {
            final ReportSend_Type send = new ReportSend_Type();
            send.setReturnOnlyIfErrorPresent(this.responseOnlyIfError);
            if (this.returnOnlyIfAll != null) {
                send.setReturnOnlyIfAllAtState(this.returnOnlyIfAll);
            }
            final String now =
                    localFormat.format(Calendar.getInstance().getTime());
            this.pr.debugln("Querying for report @ " + now);
            response = ((WorkspaceEnsemblePortType) this.portType).report(send);
        } catch (WorkspaceEnsembleFault e) {
            throw e;
        } catch (RemoteException e) {
            throw RMIUtils.generalRemoteException(e);
        }

        if (response == null) {
            throw new ExecutionProblem("null report response?");
        }

        final OneReport_Type[] reports = response.getReport();
        if (reports == null) {
            return EMPTY_RESPONSE;
        }

        final Workspace[] ret = new Workspace[reports.length];
        for (int i = 0; i < reports.length; i++) {
            if (reports[i] == null) {
                throw new ExecutionProblem("null report entry?");
            }
            try {
                ret[i] = this.convert(reports[i]);
            } catch (Exception e) {
                throw new ExecutionProblem(e.getMessage(), e); // ...
            }
        }

        return ret;
    }

    protected Workspace convert(OneReport_Type report) throws Exception {
        
        if (report == null) {
            throw new IllegalArgumentException("report may not be null");
        }

        final Workspace workspace = new Workspace();

        final EndpointReferenceType anepr = report.getEpr();

        final State state =
                State.fromCurrentState_Type(report.getCurrentState());

        if (anepr == null) {
            this.pr.debugln("Workspace # NONE");
        } else {
            this.pr.debugln("Workspace # " +
                                EPRUtils.getIdFromEPR(report.getEpr()));
        }

        if (state == null) {
            this.pr.debugln("  - state: NONE");
        } else {
            this.pr.debugln("  - state: " + state.getState());
            final Exception e = state.getProblem();
            if (e != null) {
                this.pr.debugln("  - error: " +
                        CommonUtil.genericExceptionMessageWrapper(e));
            }
        }

        workspace.setEpr(anepr);
        workspace.setCurrentState(state);

        final Schedule_Type xmlSchedule = report.getSchedule();
        if (xmlSchedule != null) {
            workspace.setCurrentSchedule(new Schedule(xmlSchedule));
        }

        final VirtualNetwork_Type xmlNetwork = report.getNetworking();
        if (xmlNetwork != null) {
            workspace.setCurrentNetworking(new Networking(xmlNetwork));
        }

        return workspace;
    }
}
