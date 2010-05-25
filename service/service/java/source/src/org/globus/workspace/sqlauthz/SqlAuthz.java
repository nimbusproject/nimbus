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

package org.globus.workspace.sqlauthz;

import org.globus.workspace.groupauthz.DecisionLogic;
import org.globus.workspace.groupauthz.GroupAuthz;
import org.globus.workspace.persistence.WorkspaceDatabaseException;
import org.globus.workspace.service.binding.authorization.CreationAuthorizationCallout;
import org.globus.workspace.service.binding.authorization.Decision;
import org.globus.workspace.service.binding.authorization.PostTaskAuthorization;
import org.globus.workspace.service.binding.vm.VirtualMachine;
import org.globus.workspace.service.binding.vm.VirtualMachinePartition;
import org.nimbustools.api.services.rm.AuthorizationException;
import org.nimbustools.api.services.rm.ResourceRequestDeniedException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.sql.*;
import javax.security.auth.Subject;
import javax.sql.DataSource;
import java.net.URI;
import java.util.Vector;

public class SqlAuthz extends GroupAuthz
{
    private static final Log logger =
            LogFactory.getLog(SqlAuthz.class.getName());
    private AuthzDecisionLogic                  authD = null;

    public SqlAuthz(AuthzDecisionLogic authd)
    {
        this.authD = authd;
    }
    
    public void validate() throws Exception
    {
        super.initializeCallout();
        this.theDecider = this.authD;
    }
    
}
