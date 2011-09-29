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

package org.nimbustools.messaging.gt4_0.status;

import org.nimbustools.api.repr.CannotTranslateException;
import org.nimbustools.api.repr.ReprFactory;
import org.nimbustools.api.repr.Usage;
import org.nimbustools.api.repr.vm.VM;
import org.nimbustools.messaging.gt4_0.BaseTranslate;
import org.nimbustools.messaging.gt4_0.common.CommonUtil;
import org.nimbustools.messaging.gt4_0.generated.status.CurrentWorkspaces_Type;
import org.nimbustools.messaging.gt4_0.generated.status.OneCurrentWorkspace_Type;
import org.nimbustools.messaging.gt4_0.generated.status.UsedAndReservedTime_Type;
import org.nimbustools.messaging.gt4_0.service.InstanceTranslate;

public class StatusTranslate extends BaseTranslate {

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected final InstanceTranslate trinst;


    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    public StatusTranslate(ReprFactory reprFactory,
                           InstanceTranslate trInstance) {

        super(reprFactory);

        if (trInstance == null) {
            throw new IllegalArgumentException("trInstance may not be null");
        }
        this.trinst = trInstance;
    }


    // -------------------------------------------------------------------------
    // TRANSLATE TO: CurrentWorkspaces_Type
    // -------------------------------------------------------------------------

    public CurrentWorkspaces_Type getCurrentWorkspaces_Type(VM[] vms)
            throws CannotTranslateException {

        if (vms == null || vms.length == 0) {
            return new CurrentWorkspaces_Type(); // *** EARLY RETURN ***
        }

        final OneCurrentWorkspace_Type[] currents =
                new OneCurrentWorkspace_Type[vms.length];

        for (int i = 0; i < vms.length; i++) {
            currents[i] = this.getOneCurrentWorkspace(vms[i]);
        }

        return new CurrentWorkspaces_Type(currents);
    }


    // -------------------------------------------------------------------------
    // TRANSLATE TO: OneCurrentWorkspace_Type
    // -------------------------------------------------------------------------

    public OneCurrentWorkspace_Type getOneCurrentWorkspace(VM vm)
            throws CannotTranslateException {

        if (vm == null) {
            throw new IllegalArgumentException("vm may not be null");
        }

        final OneCurrentWorkspace_Type one = new OneCurrentWorkspace_Type();

        one.setEpr(this.trinst.getEPR(vm.getID()));
        one.setCurrentState(this.trinst.getCurrentState(vm));
        one.setLogistics(this.trinst.getLogistics(vm));
        one.setResourceAllocation(this.trinst.getResourceAllocation_Type(vm));
        one.setSchedule(this.trinst.getSchedule_Type(vm));
        one.setDetails(this.trinst.getDetails(vm));

        return one;
    }


    // -------------------------------------------------------------------------
    // TRANSLATE TO: UsedAndReservedTime_Type
    // -------------------------------------------------------------------------

    public UsedAndReservedTime_Type getUsedAndReserved(Usage usage)
            throws CannotTranslateException {

        if (usage == null) {
            throw new CannotTranslateException("usage is missing");
        }

        final UsedAndReservedTime_Type ret = new UsedAndReservedTime_Type();

        // exceeds 6 million years of usage?
        final long reserved = usage.getReservedMinutes();
        if (reserved > Integer.MAX_VALUE) {
            throw new CannotTranslateException(
                    "Reserved time exceeds maximum integer size");
        }
        ret.setReservedTime(CommonUtil.minutesToDuration((int)reserved));

        // exceeds 6 million years of usage?
        final long elapsed = usage.getElapsedMinutes();
        if (elapsed > Integer.MAX_VALUE) {
            throw new CannotTranslateException(
                    "Elapsed time exceeds maximum integer size");
        }
        ret.setUsedTime(CommonUtil.minutesToDuration((int)elapsed));

        return ret;
    }
}
