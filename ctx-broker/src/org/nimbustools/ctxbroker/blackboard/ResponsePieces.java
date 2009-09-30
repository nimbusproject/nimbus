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

package org.nimbustools.ctxbroker.blackboard;

import org.nimbustools.ctxbroker.generated.gt4_0.description.Requires_TypeRole;
import org.nimbustools.ctxbroker.generated.gt4_0.description.Requires_TypeIdentity;

public class ResponsePieces {

    public final Requires_TypeRole[] roles;
    public final Requires_TypeIdentity[] identities;

    public ResponsePieces(Requires_TypeRole[] roles,
                          Requires_TypeIdentity[] identities) {
        this.roles = roles;
        this.identities = identities;
    }
}
