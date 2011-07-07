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

public interface RequestFactory {

    public void disablePropagate();

    public void disableReadyTransport();

    /* Factory returns implementations.
       All return null if functionality is not implemented. */

    public WorkspaceRequest cancelUnpropagated();
    public WorkspaceRequest propagate();
    public WorkspaceRequest cancelPropagating();
    public WorkspaceRequest propagateAndStart();
    public WorkspaceRequest cancelPropagatingToStart();
    public WorkspaceRequest propagateAndPause();
    public WorkspaceRequest cancelPropagatingToPause();
    public WorkspaceRequest start();
    public WorkspaceRequest startPaused();
    public WorkspaceRequest unpause();
    public WorkspaceRequest reboot();
    public WorkspaceRequest pause();
    public WorkspaceRequest shutdownNormal();
    public WorkspaceRequest shutdownSerialize();
    public WorkspaceRequest unserialize();
    public WorkspaceRequest shutdownTrash();
    public WorkspaceRequest query();

    public WorkspaceRequest readyForTransport();
    public WorkspaceRequest cancelReadyingForTransport();
    public WorkspaceRequest cancelReadyForTransport();
    
    /*
     To cancel any state between propagated and before readyingForTransport
     */
    public WorkspaceRequest cancelAllAtVMM();
}
