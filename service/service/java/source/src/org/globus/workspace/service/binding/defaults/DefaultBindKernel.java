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

import org.globus.workspace.service.binding.BindKernel;
import org.globus.workspace.service.binding.vm.VirtualMachine;
import org.nimbustools.api.repr.vm.Kernel;
import org.nimbustools.api.services.rm.CreationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.URI;

public class DefaultBindKernel implements BindKernel {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(DefaultBindKernel.class.getName());


    // -------------------------------------------------------------------------
    // implements BindKernel
    // -------------------------------------------------------------------------

    public void consume(VirtualMachine vm, Kernel kernel)
            throws CreationException {

        if (vm == null) {
            throw new IllegalArgumentException("vm may not be null");
        }

        if (kernel == null) {
            final String dbg = "kernel specification is not present in '" +
                    vm.getName() + "', setting default";
            logger.debug(dbg);
            vm.setKernel(null);
            vm.setKernelParameters(null);
            return; // *** EARLY RETURN ***
        }

        final URI uri = kernel.getKernel();
        if (uri == null) {
            final String dbg = "kernel specification is not present in '" +
                    vm.getName() + "', setting default";
            logger.debug(dbg);
            vm.setKernel(null);            
        } else if (!uri.getScheme().equals("file")) {
            // not supporting propagating kernels currently
            throw new CreationException("supplied, non-default kernel " +
                    "cannot currently be a remote file");
        } else {
            final String image = uri.toASCIIString();
            logger.trace("found kernel image: '" + image + "'");
            vm.setKernel(image);
        }

        final String params = kernel.getParameters();
        if (params != null) {
            logger.trace("found kernel parameters: '" + params + "'");
            vm.setKernelParameters(params);
        }
    }
}
