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

package org.nimbustools.messaging.gt4_0.ctx;

import org.nimbustools.messaging.gt4_0.generated.types.VoidType;
import org.nimbustools.messaging.gt4_0.generated.types.WorkspaceContextualizationFault;
import org.nimbustools.messaging.gt4_0.generated.types.RetrieveResponse_Type;
import org.nimbustools.messaging.gt4_0.generated.types.RetrieveSend_Type;
import org.nimbustools.messaging.gt4_0.generated.types.InjectData_Type;
import org.nimbustools.messaging.gt4_0.generated.types.ErrorExitingSend_Type;
import org.nimbustools.messaging.gt4_0.generated.types.OkExitingSend_Type;
import org.nimbustools.messaging.gt4_0.generated.types.IdentitiesResponse_Type;
import org.nimbustools.messaging.gt4_0.generated.types.IdentitiesSend_Type;

public interface ContextBrokerService {

    public VoidType lock(VoidType none)
            throws WorkspaceContextualizationFault;

    public RetrieveResponse_Type retrieve(RetrieveSend_Type sent)
            throws WorkspaceContextualizationFault;

    public VoidType injectdata(InjectData_Type parameters)
            throws WorkspaceContextualizationFault;
    
    public VoidType errorExiting(ErrorExitingSend_Type parameters)
            throws WorkspaceContextualizationFault;
    
    public VoidType okExiting(OkExitingSend_Type parameters)
            throws WorkspaceContextualizationFault;

    public IdentitiesResponse_Type identities(IdentitiesSend_Type parameters)
            throws WorkspaceContextualizationFault;

    // todo: add ops for standalone brokers
}
