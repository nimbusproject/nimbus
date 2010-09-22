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

import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.general.Validity;

public class DefaultValidity implements Validity {

    protected static final char[] legalIDchars =
                            {'0', '1', '2', '3', '4', '5', '6', '7',
                             '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    // example instance id:    i-936e83fa
    // example reservation id: r-602bca09

    // -------------------------------------------------------------------------
    // implements Validity
    // -------------------------------------------------------------------------

    public boolean isValidInstanceID(String instanceID) {
        return instanceID != null
                && instanceID.startsWith("i-")
                    && this.areCharsValid(instanceID);
    }

    public boolean isValidReservationID(String reservationID) {
        return reservationID != null
                && reservationID.startsWith("r-")
                    && this.areCharsValid(reservationID);
    }


    // -------------------------------------------------------------------------
    // IMPL
    // -------------------------------------------------------------------------

    protected boolean areCharsValid(String id) {
        if (id == null) {
            return false;
        }
        final char[] chars = id.toCharArray();
        if (chars.length != 10) {
            return false;
        }
        for (int i = 2; i < 10; i++) {
            boolean valid = false;
            for (int j = 0; j < legalIDchars.length; j++) {
                final char legalIDchar = legalIDchars[j];
                if (chars[i] == legalIDchar) {
                    valid = true;
                    break;
                }
            }
            if (!valid) {
                return false;
            }
        }
        return true;
    }
}
