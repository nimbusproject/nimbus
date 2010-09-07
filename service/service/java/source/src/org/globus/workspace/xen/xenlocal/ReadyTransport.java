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

import java.util.ArrayList;

public class ReadyTransport extends Propagate {

    protected void init() throws WorkspaceException {
        this.name = "Ready-For-Transport";
        this.doFakeLag = true;
        this.async = true;

        final VirtualMachine vm = this.ctx.getVm();
        if (vm != null) {
            
            final ArrayList exe =
                    this.ctx.getLocator().getPropagationAdapter().
                            constructUnpropagateCommand(vm);

            this.cmd = (String[]) exe.toArray(new String[exe.size()]);

        } else {
            throw new WorkspaceException("no VirtualMachine in request " +
                    "context, can not " + this.name);
        }
    }
}

