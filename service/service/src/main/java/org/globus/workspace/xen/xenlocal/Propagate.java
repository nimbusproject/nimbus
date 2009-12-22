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
import org.globus.workspace.service.binding.vm.VirtualMachine;
import org.globus.workspace.service.impls.site.PropagationAdapter;
import org.globus.workspace.xen.XenTask;

import java.util.ArrayList;

public class Propagate extends XenTask {

    protected void init() throws WorkspaceException {
        this.name = "Propagate-Only";
        this.doFakeLag = true;
        this.async = true;

        final VirtualMachine vm = this.ctx.getVm();
        if (vm != null) {
            final ArrayList exe = this.ctx.getLocator().
                         getPropagationAdapter().constructPropagateCommand(vm);
            this.cmd = (String[]) exe.toArray(new String[exe.size()]);
        } else {
            throw new WorkspaceException("no VirtualMachine in request " +
                    "context, can not " + this.name);
        }
    }

    protected Exception preExecute() {
        return _preExecute(
                    this.ctx.getLocator().getGlobalPolicies().isFake(),
                    this.ctx.getLocator().getPropagationAdapter());
    }

    private static Exception _preExecute(boolean fake,
                                         PropagationAdapter adapter) {

        // propagate requests add to a counter that the notification
        // polling thread uses to know if it should continue polling

        if (fake) {
            return null;
        }

        if (adapter == null) {
            return new Exception("propagation adapter is null");
        }

        try {
            adapter.prePropagate();
            return null;
        } catch (Exception e) {
            return e;
        }
    }
}
