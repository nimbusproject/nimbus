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

import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.CannotTranslateException;
import org.nimbustools.api.repr.vm.VM;
import org.nimbustools.api.services.rm.DoesNotExistException;
import org.nimbustools.api.services.rm.ManageException;
import org.nimbustools.api.services.rm.Manager;
import org.nimbustools.api.services.rm.CoSchedulingException;
import org.nimbustools.messaging.gt4_0.generated.types.OneReport_Type;
import org.nimbustools.messaging.gt4_0.generated.types.ReportResponse_Type;
import org.nimbustools.messaging.gt4_0.generated.types.ReportSend_Type;
import org.nimbustools.messaging.gt4_0.GeneralPurposeResource;
import org.globus.wsrf.config.ConfigException;

public class EnsembleResource extends GeneralPurposeResource {

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected EnsembleTranslate translate;
    

    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    /**
     * @param idString instance key
     * @param manager Manager impl
     * @param secDescPath path to resource-security descriptor template
     * @param translate move between GT/Axis1 and VWS
     * @throws ConfigException problem with secDescPath
     * @throws DoesNotExistException gone (race with a destroyer)
     * @throws ManageException general problem
     */
    public EnsembleResource(String idString,
                            Manager manager,
                            String secDescPath,
                            EnsembleTranslate translate)
            throws ConfigException, ManageException, DoesNotExistException {

        super(idString, Manager.COSCHEDULED, manager, secDescPath, translate);

        if (translate == null) {
            throw new IllegalArgumentException("translate may not be null");
        }
        this.translate = translate;
    }


    // -------------------------------------------------------------------------
    // REMOTE CLIENT INVOCATIONS - MUTATIVE
    // -------------------------------------------------------------------------

    /**
     * Only invoked from WS client via Ensemble Service
     *
     * @param callerDN caller name
     * @throws ManageException general
     * @throws DoesNotExistException gone (race with a destroyer)
     * @throws CoSchedulingException problem particular to scheduling
     */
    public void done(String callerDN) throws ManageException,
                                             DoesNotExistException,
                                             CoSchedulingException {

        final Caller caller = this.translate.getCaller(callerDN);
        this.manager.coscheduleDone(this.id, caller);
    }


    // -------------------------------------------------------------------------
    // INFORMATION QUERIES
    // -------------------------------------------------------------------------

    /**
     * Only invoked from WS client via Ensemble Service
     *
     * @param send request
     * @return ReportResponse_Type
     * @throws ManageException general
     * @throws DoesNotExistException gone (race with a destroyer)
     * @throws CannotTranslateException problem with implementation
     */
    public ReportResponse_Type reports(ReportSend_Type send)
            throws ManageException,
                   CannotTranslateException,
                   DoesNotExistException {

        // todo: in the future, allow getAll to be aware of ReportSend_Type
        //       parameters.  It could go faster if able to cut out early.
        final VM[] vms = this.manager.getAll(this.id, Manager.COSCHEDULED);

        if (vms == null || vms.length == 0) {
            // manager impl should throw this but in case it doesn't
            throw new DoesNotExistException(
                    "no VMs found in ensemble '" + this.id + "'");
        }

        final OneReport_Type[] reports =
                this.translate.getOneReport_Types(vms);

        return this.translate.getReportResponse(reports, send);
    }
}
