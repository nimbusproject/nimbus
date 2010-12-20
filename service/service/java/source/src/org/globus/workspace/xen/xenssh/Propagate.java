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

package org.globus.workspace.xen.xenssh;

import org.globus.workspace.PathConfigs;
import org.globus.workspace.WorkspaceException;
import org.globus.workspace.cmdutils.SSHUtil;
import org.globus.workspace.persistence.WorkspaceDatabaseException;
import org.globus.workspace.service.binding.vm.FileCopyNeed;
import org.globus.workspace.service.binding.vm.VirtualMachine;
import org.globus.workspace.service.impls.site.PropagationAdapter;
import org.globus.workspace.xen.XenTask;
import org.globus.workspace.xen.XenUtil;

import java.util.ArrayList;

public class Propagate extends XenTask {

    protected void init() throws WorkspaceException {
        this.name = "Propagate-Only";
        this.doFakeLag = true;
        this.async = true;

        final VirtualMachine vm = this.ctx.getVm();
        if (vm != null) {
            final ArrayList ssh = SSHUtil.constructSshCommand(vm.getNode());
            final ArrayList exe = this.ctx.getLocator().
                         getPropagationAdapter().constructPropagateCommand(vm);
            ssh.addAll(exe);

            final String credentialName = vm.getCredentialName();
            if (credentialName != null) {

                ssh.add("--prop-extra-args");
                ssh.add("'credential=" + credentialName + "'");
            }

            this.cmd = (String[]) ssh.toArray(new String[ssh.size()]);
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

        final FileCopyNeed[] needs = vm.getFileCopyNeeds();
        if (needs == null || needs.length == 0) {
            if (traceLog) {
                logger.debug("FileCopy push: nothing to do");
            }
            return null;
        }

        final PathConfigs paths = this.ctx.getLocator().getPathConfigs();
        final String backendDirectory = paths.getBackendTempDirPath();
        final String localDirectory = paths.getLocalTempDirPath();

        try {
            XenUtil.doFilePushRemoteTarget(vm,
                                           localDirectory,
                                           backendDirectory,
                                           fake,
                                           eventLog,
                                           traceLog);
        } catch (Exception e) {
            return e;
        }

        return _preExecute(fake,
                    this.ctx.getLocator().getPropagationAdapter());
    }

    private static Exception _preExecute(boolean fake,
                                         PropagationAdapter adapter) {

        // propagate requests add to a counter that the notification
        // polling thread uses to know if it should continue polling

        if (fake) {
            return null;
        }

        try {
            adapter.prePropagate();
            return null;
        } catch (Exception e) {
            return e;
        }
    }
}
