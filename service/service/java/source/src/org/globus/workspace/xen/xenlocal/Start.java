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

package org.globus.workspace.xen.xenlocal;

import org.globus.workspace.WorkspaceException;
import org.globus.workspace.PathConfigs;
import org.globus.workspace.persistence.WorkspaceDatabaseException;
import org.globus.workspace.service.binding.vm.VirtualMachine;
import org.globus.workspace.service.binding.vm.CustomizationNeed;
import org.globus.workspace.xen.XenTask;
import org.globus.workspace.xen.XenUtil;

import java.util.ArrayList;

public class Start extends XenTask {

    protected void init() throws WorkspaceException {
        this.name = "Start";
        this.doFakeLag = true;

        final VirtualMachine vm = this.ctx.getVm();
        if (vm != null) {
            final ArrayList exe =
                    XenUtil.constructCreateCommand(vm, false);
            this.cmd = (String[]) exe.toArray(new String[exe.size()]);
        } else {
            throw new WorkspaceException("no VirtualMachine in request " +
                    "context, can not " + this.name);
        }
    }

    protected Exception preExecute(boolean fake) {

        final boolean eventLog = this.ctx.lager().eventLog;
        final boolean traceLog = this.ctx.lager().traceLog;

        if (traceLog) {
            logger.trace("Beginning start pre-execute");
        }

        // init would have thrown exception if null
        final VirtualMachine vm = this.ctx.getVm();


        final CustomizationNeed[] needs = vm.getCustomizationNeeds();
        if (needs == null || needs.length == 0) {
            if (traceLog) {
                logger.debug("customization file push: nothing to do");
            }
            return null;
        }

        final PathConfigs paths = this.ctx.getLocator().getPathConfigs();
        final String backendDirectory = paths.getBackendTempDirPath();
        final String localDirectory = paths.getLocalTempDirPath();

        try {
            XenUtil.doFilePushLocalTarget(vm,
                                          localDirectory,
                                          backendDirectory,
                                          fake,
                                          eventLog,
                                          traceLog);
        } catch (Exception e) {
            return e;
        }

        // todo: do not like this concept (waiting for ORM overhaul)
        final int vmid = vm.getID().intValue();
        for (int i = 0; i < needs.length; i++) {
            try {
                needs[i].setSent(true);
                this.ctx.getLocator().getPersistenceAdapter().
                                        setCustomizeTaskSent(vmid, needs[i]);
            } catch (WorkspaceDatabaseException e) {
                logger.error("", e);
            }
        }

        // everything is OK
        return null;
    }
}
