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

package org.nimbustools.messaging.gt4_0.ensemble;

import org.nimbustools.api.repr.CannotTranslateException;
import org.nimbustools.api.repr.ReprFactory;
import org.nimbustools.api.repr.vm.VM;
import org.nimbustools.messaging.gt4_0.BaseTranslate;
import org.nimbustools.messaging.gt4_0.generated.types.CurrentState;
import org.nimbustools.messaging.gt4_0.generated.types.CurrentState_Enumeration;
import org.nimbustools.messaging.gt4_0.generated.types.OneReport_Type;
import org.nimbustools.messaging.gt4_0.generated.types.ReportResponse_Type;
import org.nimbustools.messaging.gt4_0.generated.types.ReportSend_Type;
import org.nimbustools.messaging.gt4_0.service.InstanceTranslate;

public class EnsembleTranslate extends BaseTranslate {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final OneReport_Type[] EMPTY_REPORTS = new OneReport_Type[0];

    
    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected final InstanceTranslate trinst;


    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    public EnsembleTranslate(ReprFactory reprFactory,
                             InstanceTranslate trInstance) {
        super(reprFactory);
        
        if (trInstance == null) {
            throw new IllegalArgumentException("trInstance may not be null");
        }
        this.trinst = trInstance;
    }


    // -------------------------------------------------------------------------
    // TRANSLATE TO: OneReport_Type[]
    // -------------------------------------------------------------------------

    public OneReport_Type[] getOneReport_Types(VM[] vms)
            throws CannotTranslateException {

        if (vms == null || vms.length == 0) {
            throw new IllegalArgumentException("vms null or len 0");
        }

        final OneReport_Type[] reports = new OneReport_Type[vms.length];

        for (int i = 0; i < vms.length; i++) {
            reports[i] = this.getOneReport(vms[i]);
        }

        return reports;
    }


    // -------------------------------------------------------------------------
    // TRANSLATE TO: ReportResponse_Type
    // -------------------------------------------------------------------------

    public OneReport_Type getOneReport(VM vm) throws CannotTranslateException {

        if (vm == null) {
            return null;
        }

        final OneReport_Type report = new OneReport_Type();

        report.setEpr(this.trinst.getEPR(vm.getID()));

        report.setCurrentState(this.trinst.getCurrentState(vm));

        report.setNetworking(this.trinst.getLogistics(vm).getNetworking());

        report.setSchedule(this.trinst.getSchedule_Type(vm));

        return report;
    }


    // -------------------------------------------------------------------------
    // TRANSLATE TO: ReportResponse_Type
    // -------------------------------------------------------------------------

    public ReportResponse_Type getReportResponse(OneReport_Type[] reports,
                                                 ReportSend_Type send) {

        if (reports == null) {
            throw new IllegalArgumentException("reports may not be null");
        }

        if (send == null) {
            throw new IllegalArgumentException("send may not be null");
        }

        final boolean returnOnlyIfCorrupt =
                            send.isReturnOnlyIfErrorPresent();

        final CurrentState_Enumeration returnOnlyIfAll =
                            send.getReturnOnlyIfAllAtState();


        if (returnOnlyIfCorrupt || returnOnlyIfAll != null) {

            boolean allAtState = true;
            boolean oneCorrupt = false;

            for (int i = 0; i < reports.length; i++) {
                if (reports[i] != null) {

                    if (isCorrupt(reports[i])) {
                        oneCorrupt = true;
                    }

                    if (returnOnlyIfAll != null) {
                        if (!isState(reports[i], returnOnlyIfAll)) {
                            allAtState = false;
                        }
                    }
                }
            }

            if (returnOnlyIfCorrupt && oneCorrupt) {
                return new ReportResponse_Type(reports);
            } else if (returnOnlyIfAll != null && allAtState) {
                return new ReportResponse_Type(reports);
            } else {
                return new ReportResponse_Type(EMPTY_REPORTS);
            }
        }

        return new ReportResponse_Type(reports);
    }

    private static boolean isState(OneReport_Type report,
                                   CurrentState_Enumeration target) {

        final CurrentState state = report.getCurrentState();
        if (state == null) {
            return false;
        }
        return CurrentState_Enumeration.Running.equals(state.getState());
    }

    private static boolean isCorrupt(OneReport_Type report) {
        final CurrentState state = report.getCurrentState();
        if (state == null) {
            return true;
        }
        final CurrentState_Enumeration stateString = state.getState();
        return CurrentState_Enumeration.Cancelled.equals(stateString) ||
                CurrentState_Enumeration.Corrupted.equals(stateString);
    }
}
