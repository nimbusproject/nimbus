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

package org.nimbustools.ctxbroker.service;

import org.nimbustools.ctxbroker.generated.gt4_0.broker.NimbusContextualizationFault;
import org.globus.wsrf.utils.FaultHelper;
import org.oasis.wsrf.faults.BaseFaultType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ContextFault {

    private static final Log logger =
            LogFactory.getLog(ContextFault.class.getName());

    public static NimbusContextualizationFault makeCtxFault(String err,
                                                               Exception e) {

        return (NimbusContextualizationFault)
                makeFault(NimbusContextualizationFault.class, err, e);
    }

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
