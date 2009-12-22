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

package org.nimbustools.api.defaults.repr.vm;

import org.nimbustools.api._repr.vm._Kernel;

import java.net.URI;

public class DefaultKernel implements _Kernel {

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------
    
    private URI kernel;
    private String parameters;
    
    
    // -------------------------------------------------------------------------
    // implements org.nimbustools.api.repr.vm.Kernel
    // -------------------------------------------------------------------------
    
    public URI getKernel() {
        return this.kernel;
    }

    public String getParameters() {
        return this.parameters;
    }


    // -------------------------------------------------------------------------
    // implements org.nimbustools.api._repr.vm._Kernel
    // -------------------------------------------------------------------------
      
    public void setKernel(URI kernel) {
        this.kernel = kernel;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    
    // -------------------------------------------------------------------------
    // DEBUG STRING
    // -------------------------------------------------------------------------

    public String toString() {
        return "DefaultKernel{" +
                "kernel='" + kernel +
                "', parameters='" + parameters + '\'' +
                '}';
    }
}
