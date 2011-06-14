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

import org.globus.workspace.WorkspaceException;
import org.globus.workspace.cmdutils.SSHUtil;
import org.globus.workspace.service.binding.vm.VirtualMachine;
import org.globus.workspace.xen.XenTask;
import org.globus.workspace.xen.XenUtil;

import java.util.ArrayList;

/**
 * We don't have Spring based task configuration, but rather "command sets" in the
 * {@link org.globus.workspace.service.impls.async.RequestFactoryImpl} class (also see
 * {@link org.globus.workspace.service.impls.async.RequestFactory}).  So instead of
 * reworking that to use IoC for easy mocking, we added this mock implementation to
 * a special command set.
 */
public class MockShutdownTrash extends XenTask {

    private static boolean fail = false;
    private static int failCount = 0;
    private static long msAtLastAttempt = 0;

    // Point of control from tests, ANY created task object will respect this static field
    // when the init() method is called -- so creating a number of instances simultaneously
    // where only a few of them fail is not possible.
    public static void setFail(boolean doFail) {
        fail = doFail;
    }
    public static int getFailCount() {
        return failCount;
    }
    public static long getMsAtLastAttempt() {
        return msAtLastAttempt;
    }

    public static void resetFailCount() {
        failCount = 0;
        msAtLastAttempt = 0;
    }

    protected void init() throws WorkspaceException {
        this.name = "MOCK-Shutdown-Trash";
        this.doFakeLag = true;

        final VirtualMachine vm = this.ctx.getVm();
        if (vm == null) {
            throw new WorkspaceException("no VirtualMachine in request " +
                    "context, can not " + this.name);
        }
        
        final ArrayList ssh = SSHUtil.constructSshCommand(vm.getNode());
        final ArrayList exe = XenUtil.constructRemoveCommand(vm, true);
        ssh.addAll(exe);

        if (fail) {
            this.doFakeFail = true;
            logger.warn(this.name + " forced to fail.");
            failCount += 1;
        }
        msAtLastAttempt = System.currentTimeMillis();
        this.cmd = (String[]) ssh.toArray(new String[ssh.size()]);
    }
}
