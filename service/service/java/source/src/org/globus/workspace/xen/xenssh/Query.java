package org.globus.workspace.xen.xenssh;

import org.globus.workspace.WorkspaceException;
import org.globus.workspace.cmdutils.SSHUtil;
import org.globus.workspace.service.binding.vm.VirtualMachine;
import org.globus.workspace.xen.XenTask;
import org.globus.workspace.xen.XenUtil;

import java.util.ArrayList;

/**
 * @author Carla Souza
 * @date 07/06/11 00:16
 */
public class Query extends XenTask {

    protected void init() throws WorkspaceException {
        this.name = "Query";
        this.doFakeLag = true;

        final VirtualMachine vm = this.ctx.getVm();
        final ArrayList ssh = SSHUtil.constructSshCommand(vm.getNode());
        final ArrayList exe = XenUtil.constructQueryCommand();
        ssh.addAll(exe);
        this.cmd = (String[]) ssh.toArray(new String[ssh.size()]);

    }


}
