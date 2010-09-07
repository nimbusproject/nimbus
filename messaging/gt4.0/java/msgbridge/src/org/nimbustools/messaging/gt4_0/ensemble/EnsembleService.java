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

package org.nimbustools.messaging.gt4_0.ensemble;

import org.nimbustools.messaging.gt4_0.generated.types.ReportResponse_Type;
import org.nimbustools.messaging.gt4_0.generated.types.ReportSend_Type;
import org.nimbustools.messaging.gt4_0.generated.types.WorkspaceUnknownFault;
import org.nimbustools.messaging.gt4_0.generated.ensemble.WorkspaceEnsembleFault;
import org.nimbustools.messaging.gt4_0.generated.ensemble.VoidType;
import org.nimbustools.messaging.gt4_0.service.InstanceUtil;
import org.nimbustools.messaging.gt4_0.common.CommonUtil;
import org.nimbustools.messaging.gt4_0.FaultUtil;
import org.globus.wsrf.ResourceException;
import org.globus.wsrf.ResourceContextException;
import org.globus.wsrf.ResourceContext;
import org.globus.wsrf.security.SecurityManager;
import org.nimbustools.api.services.rm.DoesNotExistException;
import org.nimbustools.api.repr.CannotTranslateException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class EnsembleService {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(EnsembleService.class.getName());


    // -------------------------------------------------------------------------
    // HELPER
    // -------------------------------------------------------------------------

    protected EnsembleResource getEnsembleResource()
            throws ResourceException, ResourceContextException {
        final ResourceContext context =
                    ResourceContext.getResourceContext();
        return (EnsembleResource)context.getResource();
    }
    

    // -------------------------------------------------------------------------
    // methods corresponding to WorkspaceEnsemblePortType
    // -------------------------------------------------------------------------
    
    public VoidType done(VoidType none)
            throws WorkspaceEnsembleFault, WorkspaceUnknownFault {

        final String callerDN = SecurityManager.getManager().getCaller();

        if (callerDN == null) {
            logger.error("ENS-DONE: no caller identity");
            throw EnsembleUtil.makeEnsembleFault("no caller identity", null);
        }

        try {
            final EnsembleResource resource = getEnsembleResource();
            resource.done(callerDN);
        } catch (DoesNotExistException e) {
            final String err = CommonUtil.genericExceptionMessageWrapper(e);
            throw InstanceUtil.makeUnknownFault(err);
        } catch (Exception e) {
            logger.error(e);
            throw EnsembleUtil.makeEnsembleFault(e.getMessage(), e);
        } catch (Throwable t) {
            throw EnsembleUtil.makeEnsembleFault(
                    FaultUtil.unknown(t, "ensemble.done"), null);
        }

        return new VoidType();
    }

    public ReportResponse_Type report(ReportSend_Type send)
            throws WorkspaceEnsembleFault, WorkspaceUnknownFault {

        try {
            final EnsembleResource resource = getEnsembleResource();
            return resource.reports(send);
        } catch (DoesNotExistException e) {
            final String err = CommonUtil.genericExceptionMessageWrapper(e);
            throw InstanceUtil.makeUnknownFault(err);
        } catch (CannotTranslateException e) {
            logger.fatal(e.getMessage(), e);
            throw EnsembleUtil.makeEnsembleFault("Internal service error",
                                                 null);
        } catch (Exception e) {
            logger.error(e);
            throw EnsembleUtil.makeEnsembleFault(e.getMessage(), e);
        } catch (Throwable t) {
            logger.fatal(t.getMessage(), t);
            throw EnsembleUtil.makeEnsembleFault(
                    FaultUtil.unknown(t, "ensemble.report"), null);
        }
    }
}
