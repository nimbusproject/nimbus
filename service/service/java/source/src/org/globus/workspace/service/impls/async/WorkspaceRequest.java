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

package org.globus.workspace.service.impls.async;

import org.globus.workspace.WorkspaceException;

// TODO: move to executor
public interface WorkspaceRequest {

    /**
     * All error reporting and request setup must happen out of band.
     *
     * WorkspaceRequestQueue just knows how to dequeue work requests
     * and call execute.
     */
    public void execute();

    /**
     * Information needed by request is supplied in context
     * @param requestContext request context
     */
    public void setRequestContext(WorkspaceRequestContext requestContext);

}
