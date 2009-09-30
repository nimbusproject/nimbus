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

package org.nimbustools.messaging.gt4_0.factory;

import org.nimbustools.messaging.gt4_0.generated.types.WorkspaceCreationFault;
import org.nimbustools.messaging.gt4_0.generated.types.WorkspaceMetadataFault;
import org.nimbustools.messaging.gt4_0.generated.types.WorkspaceSchedulingFault;
import org.nimbustools.messaging.gt4_0.generated.types.WorkspaceResourceRequestDeniedFault;
import org.nimbustools.messaging.gt4_0.generated.ensemble.WorkspaceEnsembleFault;
import org.nimbustools.messaging.gt4_0.FaultUtil;

public class FactoryUtil {

    public static WorkspaceCreationFault
                            makeCreationFault(String err, Exception e) {
        
        return (WorkspaceCreationFault)
                    FaultUtil.makeFault(WorkspaceCreationFault.class, err, e);
    }

    public static WorkspaceMetadataFault
                            makeMetadataFault(String err, Exception e) {
        
        return (WorkspaceMetadataFault)
                    FaultUtil.makeFault(WorkspaceMetadataFault.class, err, e);
    }

    public static WorkspaceSchedulingFault
                            makeSchedulingFault(String err, Exception e) {
        
        return (WorkspaceSchedulingFault)
                    FaultUtil.makeFault(WorkspaceSchedulingFault.class, err, e);
    }

    public static WorkspaceResourceRequestDeniedFault
                            makeDeniedFault(String err, Exception e) {
        
        return (WorkspaceResourceRequestDeniedFault)
                    FaultUtil.makeFault(WorkspaceResourceRequestDeniedFault.class, err, e);
    }

    public static WorkspaceEnsembleFault
                            makeCoschedulingFault(String err, Exception e) {

        return (WorkspaceEnsembleFault)
                    FaultUtil.makeFault(WorkspaceEnsembleFault.class, err, e);
    }
}
