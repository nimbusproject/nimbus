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

package org.globus.workspace.service.binding.defaults;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.workspace.service.binding.BindVMM;
import org.globus.workspace.service.binding.vm.VirtualMachine;
import org.nimbustools.api.repr.vm.RequiredVMM;
import org.nimbustools.api.services.rm.CreationException;

public class DefaultBindVMM implements BindVMM {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(DefaultBindVMM.class.getName());

    
    // -------------------------------------------------------------------------
    // implements BindVMM
    // -------------------------------------------------------------------------

    public void consume(VirtualMachine vm, RequiredVMM requiredVMM)
            throws CreationException {

        if (requiredVMM == null) {
            logger.warn("no VMM in requirements");
        } else {
            final String type = requiredVMM.getType();
            if (type != null) {
                vm.setVmm(type);
            }

            final String[] versions = requiredVMM.getVersions();
            if (versions != null && versions.length > 0) {
                // todo: support specification of a list of compatible versions
                vm.setVmmVersion(versions[0]);
            }
        }
    }
}
