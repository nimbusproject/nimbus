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

import org.nimbustools.api.services.rm.Manager;
import org.nimbustools.api.services.rm.ManageException;
import org.nimbustools.api.repr.vm.VM;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.CannotTranslateException;
import org.nimbustools.api.repr.Usage;
import org.nimbustools.api.repr.Advertised;
import org.nimbustools.messaging.gt4_0.generated.status.CurrentWorkspaces_Type;
import org.nimbustools.messaging.gt4_0.generated.status.UsedAndReservedTime_Type;
import org.nimbustools.messaging.gt4_0.common.Constants_GT4_0;
import org.globus.wsrf.Resource;
import org.globus.wsrf.ResourceProperties;
import org.globus.wsrf.ResourcePropertySet;
import org.globus.wsrf.impl.SimpleResourcePropertySet;

public class StatusResource implements Resource,
                                       ResourceProperties {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    public static final Integer BAD_CHARGE_GRANULARITY =
                                        new Integer(Integer.MIN_VALUE);

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected final Manager manager;
    protected final StatusTranslate translate;
    protected ResourcePropertySet props;


    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    public StatusResource(Manager manager,
                          StatusTranslate translate) {
        if (manager == null) {
            throw new IllegalArgumentException("manager may not be null");
        }
        if (translate == null) {
            throw new IllegalArgumentException("translate may not be null");
        }
        this.manager = manager;
        this.translate = translate;

        this.props = new SimpleResourcePropertySet(
                                    Constants_GT4_0.STATUS_RP_SET);
        
        this.props.add(new RP_ChargeGranularity(this));
    }


    // -------------------------------------------------------------------------
    // INFORMATION QUERIES
    // -------------------------------------------------------------------------

    public CurrentWorkspaces_Type queryCurrentWorkspaces(String callerDN)
            throws ManageException, CannotTranslateException {
        
        final Caller caller = this.translate.getCaller(callerDN);
        final VM[] vms = this.manager.getAllByCaller(caller);
        return this.translate.getCurrentWorkspaces_Type(vms);
    }

    public UsedAndReservedTime_Type queryUsedAndReservedTime(String callerDN)
            throws ManageException, CannotTranslateException {

        final Caller caller = this.translate.getCaller(callerDN);
        final Usage usage = this.manager.getCallerUsage(caller);
        return this.translate.getUsedAndReserved(usage);
    }


    // -------------------------------------------------------------------------
    // ADVERTISEMENTS
    // -------------------------------------------------------------------------

    public Integer getChargeGranularity() {
        final Advertised adv = this.manager.getAdvertised();
        if (adv == null) {
            return BAD_CHARGE_GRANULARITY;
        }
        final int gran = adv.getChargeGranularity();
        if (gran < 1) {
            return BAD_CHARGE_GRANULARITY;
        }
        return new Integer(gran);
    }
    

    // -------------------------------------------------------------------------
    // implements ResourceProperties
    // -------------------------------------------------------------------------

    public ResourcePropertySet getResourcePropertySet() {
        return this.props;
    }
}
