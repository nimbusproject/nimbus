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

package org.globus.workspace.service.impls.site;

import org.globus.workspace.service.binding.vm.VirtualMachine;

import java.util.ArrayList;

public interface PropagationAdapter {

    public void prePropagate() throws Exception;

    /**
     * Returns a command without no transport prefixes (for example,
     * no ssh etc., just the command as if this were localhost.
     *
     * In the future we could add network based tasks as an alternative
     * interface and always return a commandline task.
     *
     * @param vm vm
     * @return commandline task
     */
    public ArrayList constructPropagateCommand(VirtualMachine vm);

    /**
     * Returns a command without no transport prefixes (for example,
     * no ssh etc., just the command as if this were localhost.
     *
     * This is a fishy interface because most propagate + start implementations
     * will require tight coordination between the VMM command implementations
     * and the propagation implementation (like our workspace-control program)
     *
     * In the future we could add network based tasks as an alternative
     * interface and always return a commandline task.
     *
     * @param vm vm
     * @return commandline task
     */
    public ArrayList constructPropagateToStartCommand(VirtualMachine vm);


    /**
     * @see #constructPropagateToStartCommand
     * @param vm vm
     * @return commandline task
     */
    public ArrayList constructPropagateToPauseCommand(VirtualMachine vm);

    /**
     * @param vm vm
     * @return commandline task
     */
    public ArrayList constructUnpropagateCommand(VirtualMachine vm);


    // no cancel called here, currently we rely on receiving a cancelled
    // notification when the cancel-propagation-* commands are accomplished
    // will likely change for other propagation implementations, we won't
    // design good general interfaces until we have many propagation systems...


}
