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

package org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.general.defaults;

import org.nimbustools.api.brain.ModuleLocator;
import org.nimbustools.api.services.rm.Manager;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.general.AvailabilityZones;

public class DefaultAvailabilityZones implements AvailabilityZones {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final String[] EMPTY = new String[0];
    

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private String[] availableZones = EMPTY;
    private ModuleLocator locator = null;
    protected Manager manager; // the real RM


    public DefaultAvailabilityZones(ModuleLocator locatorImpl) throws Exception {

        if (locatorImpl == null) {
            throw new IllegalArgumentException("locator may not be null");
        }
        this.locator = locatorImpl;
        this.manager = locator.getManager();
    }


    // -------------------------------------------------------------------------
    // GET/SET
    // -------------------------------------------------------------------------

    public String[] getAvailabilityZones() {
        return this.manager.getResourcePools();
    }
}
