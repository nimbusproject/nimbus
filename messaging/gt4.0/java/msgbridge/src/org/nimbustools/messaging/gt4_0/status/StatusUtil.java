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

import org.nimbustools.messaging.gt4_0.FaultUtil;
import org.nimbustools.messaging.gt4_0.generated.status.WorkspaceStatusFault;

public class StatusUtil {

    // -------------------------------------------------------------------------
    // FAULTS
    // -------------------------------------------------------------------------

    public static WorkspaceStatusFault makeStatusFault(String err,
                                                       Throwable t) {

        return (WorkspaceStatusFault)
                FaultUtil.makeFault(WorkspaceStatusFault.class, err, t);
    }
}
