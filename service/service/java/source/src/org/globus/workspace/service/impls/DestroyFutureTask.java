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

package org.globus.workspace.service.impls;

import edu.emory.mathcs.backport.java.util.concurrent.Callable;
import org.globus.workspace.service.WorkspaceHome;
import org.globus.workspace.service.InstanceResource;
import org.nimbustools.api.services.rm.DoesNotExistException;
import org.nimbustools.api.services.rm.ManageException;

public class DestroyFutureTask implements Callable {

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private final int id;
    private final WorkspaceHome home;


    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------
    
    public DestroyFutureTask(int vmid,
                             WorkspaceHome whome) {
        this.id = vmid;
        if (whome == null) {
            throw new IllegalArgumentException("whome may not be null");
        }
        this.home = whome;
    }


    // -------------------------------------------------------------------------
    // implements Callable
    // -------------------------------------------------------------------------
    
    public Object call() throws Exception {

        final InstanceResource resource;
        try {
            resource = this.home.find(this.id);
        } catch (DoesNotExistException e) {
            return "Workspace #" + this.id + " removed already.\n";
        } catch (ManageException e) {
            return "Problem finding workspace #" + this.id + ": " +
                    e.getMessage() + "\n";
        }

        // not moving to group terminate yet on backend
        resource.setLastInGroup(false);
        resource.setPartOfGroupRequest(false);

        this.home.destroy(this.id);

        return null;
    }
}
