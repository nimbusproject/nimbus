package org.globus.workspace.service.impls.async;

import org.globus.workspace.WorkspaceException;

/**
 * @author Carla Souza <contact@carlasouza.com>
 */
public interface VMMRequest {

    public String execute() throws WorkspaceException;

    public void setRequestContext(VMMRequestContext requestContext);


}
