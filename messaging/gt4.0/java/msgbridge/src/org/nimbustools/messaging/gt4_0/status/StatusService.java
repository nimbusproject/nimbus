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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.wsrf.ResourceContext;
import org.globus.wsrf.security.SecurityManager;
import org.nimbustools.messaging.gt4_0.generated.status.UsedAndReservedTime_Type;
import org.nimbustools.messaging.gt4_0.generated.status.VoidType;
import org.nimbustools.messaging.gt4_0.generated.status.WorkspaceStatusFault;
import org.nimbustools.messaging.gt4_0.generated.status.CurrentWorkspaces_Type;
import org.nimbustools.messaging.gt4_0.FaultUtil;

public class StatusService {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(StatusService.class.getName());


    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    // singleton right now, cache it
    private StatusResource resource;


    // -------------------------------------------------------------------------
    // HELPER
    // -------------------------------------------------------------------------

    protected StatusResource getStatusResource() throws Exception {
        final ResourceContext context =
                    ResourceContext.getResourceContext();
        return (StatusResource)context.getResource();
    }


    // -------------------------------------------------------------------------
    // WS OPERATION: queryUsedAndReservedTime
    // -------------------------------------------------------------------------

    public UsedAndReservedTime_Type queryUsedAndReservedTime(VoidType none)
            throws WorkspaceStatusFault {

        final String callerDN = SecurityManager.getManager().getCaller();

        if (callerDN == null) {
            logger.error("VWS-QUERY-U+R: no caller identity");
            throw StatusUtil.makeStatusFault("no caller identity", null);
        }

        // race is ok
        if (this.resource == null) {
            try {
                this.resource = this.getStatusResource();
            } catch (Exception e) {
                logger.error("", e);
                throw StatusUtil.makeStatusFault("", e);
            }
        }

        try {
            return this.resource.queryUsedAndReservedTime(callerDN);
        } catch (Exception e) {
            throw StatusUtil.makeStatusFault(e.getMessage(), e);
        } catch (Throwable t) {
            throw StatusUtil.makeStatusFault(
                    FaultUtil.unknown(t, "status.queryU+R"), t);
        }
    }

    // -------------------------------------------------------------------------
    // WS OPERATION: queryCurrentWorkspaces
    // -------------------------------------------------------------------------

    public CurrentWorkspaces_Type queryCurrentWorkspaces(VoidType none)
            throws WorkspaceStatusFault {

        final String callerDN = SecurityManager.getManager().getCaller();

        if (callerDN == null) {
            logger.error("VWS-QUERY-CURRENT: no caller identity");
            throw StatusUtil.makeStatusFault("no caller identity", null);
        }

        // race is ok
        if (this.resource == null) {
            try {
                this.resource = this.getStatusResource();
            } catch (Exception e) {
                logger.error("", e);
                throw StatusUtil.makeStatusFault("", e);
            }
        }

        try {
            return this.resource.queryCurrentWorkspaces(callerDN);
        } catch (Throwable t) {
            throw StatusUtil.makeStatusFault(
                    FaultUtil.unknown(t, "status.querycurr"), t);
        }
    }
}
