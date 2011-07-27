package org.globus.workspace.service.impls.async;

import org.globus.workspace.WorkspaceException;

public interface VMMRequest {

    public String execute() throws WorkspaceException;

    public void setRequestContext(VMMRequestContext requestContext);


}
