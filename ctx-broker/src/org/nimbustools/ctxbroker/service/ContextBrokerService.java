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

import org.nimbustools.ctxbroker.generated.gt4_0.types.VoidType;
import org.nimbustools.ctxbroker.generated.gt4_0.types.RetrieveResponse_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.types.InjectData_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.types.ErrorExitingSend_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.types.OkExitingSend_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.types.IdentitiesResponse_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.types.IdentitiesSend_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.types.CreateContext_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.types.CreateContextResponse_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.broker.NimbusContextualizationFault;
import org.nimbustools.ctxbroker.generated.gt4_0.description.AgentDescription_Type;

public interface ContextBrokerService {

    public CreateContextResponse_Type create(CreateContext_Type create)
            throws NimbusContextualizationFault;

    public VoidType noMoreInjections(VoidType none)
            throws NimbusContextualizationFault;

    public RetrieveResponse_Type retrieve(AgentDescription_Type sent)
            throws NimbusContextualizationFault;

    public VoidType injectdata(InjectData_Type parameters)
            throws NimbusContextualizationFault;

    public VoidType errorExiting(ErrorExitingSend_Type parameters)
            throws NimbusContextualizationFault;

    public VoidType okExiting(OkExitingSend_Type parameters)
            throws NimbusContextualizationFault;

    public IdentitiesResponse_Type identities(IdentitiesSend_Type parameters)
            throws NimbusContextualizationFault;

}
