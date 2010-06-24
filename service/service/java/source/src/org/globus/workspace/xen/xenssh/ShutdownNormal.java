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

import org.globus.workspace.RepoFileSystemAdaptor;
import org.globus.workspace.WorkspaceException;
import org.globus.workspace.cmdutils.SSHUtil;
import org.globus.workspace.service.binding.vm.VirtualMachine;
import org.globus.workspace.service.binding.vm.VirtualMachinePartition;
import org.globus.workspace.xen.XenTask;
import org.globus.workspace.xen.XenUtil;

import java.util.ArrayList;

public class ShutdownNormal extends XenTask {

    protected void init() throws WorkspaceException {
        this.name = "Shutdown-Normal";
        this.doFakeLag = true;

        final VirtualMachine vm = this.ctx.getVm();
        if (vm != null) {
            final ArrayList ssh = SSHUtil.constructSshCommand(vm.getNode());
            final ArrayList exe = XenUtil.constructRemoveCommand(vm, false);
            ssh.addAll(exe);
            this.cmd = (String[]) ssh.toArray(new String[ssh.size()]);
        } else {
            throw new WorkspaceException("no VirtualMachine in request " +
                    "context, can not " + this.name);
        }
    }

    protected Exception postExecute(Exception e, boolean fake) {
        e = super.postExecute(e, fake);
        if(e != null) {
            return e;
        }

        try {
            VirtualMachine vm = this.ctx.getVm();
            
            RepoFileSystemAdaptor nsTrans = XenUtil.getNsTrans();

            VirtualMachinePartition[] parts = vm.getPartitions();            

            for(int i = 0; i < parts.length; i++) {
                if (parts[i].isRootdisk()) {
                    String img = parts[i].getImage();
                    if(parts[i].getAlternateUnpropTarget() != null)
                    {
                        img = parts[i].getAlternateUnpropTarget();
                    }
                    
                    if(nsTrans != null) {
                        nsTrans.unpropagationFinished(img);                        
                    }
                    break;
                }
            }
        } catch(Exception ex) {
            return ex;
        }
        return null;
    }
}
