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

package org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.rm.defaults;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.services.rm.Manager;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.RebootInstancesInfoType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.RebootInstancesItemType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.RebootInstancesType;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.rm.IDMappings;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.rm.Reboot;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class DefaultReboot implements Reboot {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(DefaultReboot.class.getName());
    

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected final IDMappings ids;


    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    public DefaultReboot(IDMappings idsImpl) {
        if (idsImpl == null) {
            throw new IllegalArgumentException("idsImpl may not be null");
        }
        this.ids = idsImpl;
    }

    // -------------------------------------------------------------------------
    // implements Reboot
    // -------------------------------------------------------------------------


    public boolean reboot(RebootInstancesType req,
                          Caller caller,
                          Manager manager) {

        if (req == null) {
            throw new IllegalArgumentException("req may not be null");
        }
        if (caller == null) {
            throw new IllegalArgumentException("caller may not be null");
        }
        if (manager == null) {
            throw new IllegalArgumentException("manager may not be null");
        }

        final List instanceIDs = new LinkedList();
        
        final RebootInstancesInfoType rebootSet = req.getInstancesSet();
        if (rebootSet != null) {
            final RebootInstancesItemType[] riits = rebootSet.getItem();
            if (riits != null) {
                for (int i = 0; i < riits.length; i++) {
                    final RebootInstancesItemType riit = riits[i];
                    if (riit != null) {
                        final String instanceID = riit.getInstanceId();
                        if (instanceID != null) {
                            final String add = instanceID.trim();
                            if (add.length() != 0) {
                                instanceIDs.add(add);
                            }
                        }
                    }
                }
            }
        }

        int numReboots = 0;

        final Iterator iter = instanceIDs.iterator();
        while (iter.hasNext()) {
            final String elasticInstanceID = (String) iter.next();
            final String mgrID =
                    this.ids.instanceToManager(elasticInstanceID);

            if (mgrID == null) {
                logger.warn("unknown elastic instance ID " +
                                "in reboot request: " + elasticInstanceID);
            } else {
                try {
                    manager.reboot(mgrID, Manager.INSTANCE, null, caller);
                    numReboots += 1;
                } catch (Exception e) {
                    if (logger.isDebugEnabled()) {
                        logger.error(e.getMessage(), e);
                    } else {
                        logger.error(e.getMessage());
                    }
                }
            }
        }
        
        return numReboots == instanceIDs.size();

        // TODO: figure out what semantics should be, is result true if ALL
        //       succeed or just one?  Choosing "all" version for now.

        // TODO: also, the underlying management operation is ASYNCHRONOUS
        //       so to know if it really succeeded ultimately, we would need
        //       to babysit each command with a status check (even then, it
        //       could be the case that something else successfully rebooted
        //       it and this operation did not).
    }
}
