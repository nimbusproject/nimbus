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

package org.nimbustools.messaging.gt4_0;

import org.globus.wsrf.impl.SimpleResourcePropertyMetaData;

import java.util.Calendar;

public class RP_TerminationTime extends BasicRP {

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected final GeneralPurposeResource rsrc;


    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    public RP_TerminationTime(GeneralPurposeResource resource) {
        super(SimpleResourcePropertyMetaData.TERMINATION_TIME);
        if (resource == null) {
            throw new IllegalArgumentException("resource may not be null");
        }
        this.rsrc = resource;
    }


    // -------------------------------------------------------------------------
    // extends BasicRP
    // -------------------------------------------------------------------------

    public Object getValue() {
        return this.rsrc.getTerminationTime();
    }

    public void setValue(Object value) {
        this.rsrc.setTerminationTime((Calendar) value);
    }
}
