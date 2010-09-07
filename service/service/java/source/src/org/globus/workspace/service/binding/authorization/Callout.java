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

package org.globus.workspace.service.binding.authorization;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.workspace.service.binding.vm.VirtualMachine;
import org.nimbustools.api.services.rm.ResourceRequestDeniedException;
import org.nimbustools.api.services.rm.AuthorizationException;

import javax.security.auth.Subject;

public class Callout {

    private static final Log logger =
                        LogFactory.getLog(Callout.class.getName());

    /*
     * Currently, this translates INDETERMINATE to PERMIT
     */
    public static boolean isPermitted(CreationAuthorizationCallout callout,
                                      String callerDN,
                                      Subject peerSubject,
                                      VirtualMachine[] bindings,
                                      Long elapsedMins,
                                      Long reservedMins,
                                      int numWorkspaces)
            throws ResourceRequestDeniedException,
                   AuthorizationException {

        Integer result = callout.isPermitted(bindings,
                                             callerDN,
                                             peerSubject,
                                             elapsedMins,
                                             reservedMins,
                                             numWorkspaces);

        if (result.equals(Decision.PERMIT)) {
            logger.debug("authorization callout: PERMIT");
            return true;
        } else if (result.equals(Decision.DENY)) {
            //TODO: richer server-side log?
            final String msg = "authorzation callout: DENY";
            logger.warn(msg);
            return false;
        } else if (result.equals(Decision.INDETERMINATE)) {
            logger.debug("authorization callout: INDETERMINATE");
            return true;
        } else {
            final String msg = "authorzation callout plugin returned " +
                    "unknown decision code: " + result;
            logger.error(msg);
            // this msg gets seen by web services client....
            throw new AuthorizationException("internal " +
                    "server error: authorization DENIED");
        }
    }
}
