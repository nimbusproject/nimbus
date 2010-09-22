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

import org.apache.axis.MessageContext;
import org.globus.wsrf.impl.security.authentication.Constants;

import javax.security.auth.Subject;

public class IdentityUtil {

    // -------------------------------------------------------------------------
    // STATIC UTILITIES
    // -------------------------------------------------------------------------

    /**
     * @return subject, might be null
     */
    public static Subject discoverSubject() {
        
        final MessageContext msgContext = MessageContext.getCurrentContext();

        if (msgContext == null) {
            return null;
        }

        final Object object = msgContext.getProperty(Constants.PEER_SUBJECT);
        if (object instanceof Subject) {
            return (Subject) object;
        } else {
            return null;
        }
    }
}
