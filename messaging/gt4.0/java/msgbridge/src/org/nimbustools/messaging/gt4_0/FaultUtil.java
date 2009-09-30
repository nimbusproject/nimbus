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

import org.oasis.wsrf.faults.BaseFaultType;
import org.globus.wsrf.utils.FaultHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.safehaus.uuid.UUIDGenerator;

public class FaultUtil {

    private static final Log logger =
        LogFactory.getLog(FaultUtil.class.getName());


    public static final String UNCAUGHT_ERRORS = "Unexpected internal " +
            "service problem.  If this is an issue for you, please send " +
            "the administrator this key along with support request: ";

    public static String unknown(Throwable t, String opName) {
        final String uuid = UUIDGenerator.getInstance()
                                .generateRandomBasedUUID().toString();
        final String err =
                "UNKNOWN-ERROR '" + uuid + "' from OP:" + opName + " -- ";
        if (t != null) {
            logger.fatal(err + t.getMessage(), t);
        } else {
            logger.fatal(err + "[[NULL THROWABLE]]");
        }
        return UNCAUGHT_ERRORS + uuid;
    }


    /**
     * Helps with generic faults and chained exceptions.
     * @param faultClass class
     * @param errMsg error message
     * @param t throwable to include in fault
     * @return WorkspaceFaultType
     */
    public static BaseFaultType makeFault(Class faultClass,
                                          String errMsg,
                                          Throwable t) {

        BaseFaultType fault = null;
        try {
            fault = (BaseFaultType) faultClass.newInstance();
            final FaultHelper faultHelper = new FaultHelper(fault);
            if (errMsg != null) {
                faultHelper.addDescription(errMsg);
            }

            if (t != null) {
                final BaseFaultType faultCause;
                if (t instanceof BaseFaultType) {
                    faultCause = (BaseFaultType) t;
                } else {
                    faultCause = FaultHelper.toBaseFault(t);
                }
                faultHelper.addFaultCause(faultCause);
            }

        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.error(e.getMessage(), e);
            } else {
                logger.error(e.getMessage());
            }
        }

        return fault;
    }
}
