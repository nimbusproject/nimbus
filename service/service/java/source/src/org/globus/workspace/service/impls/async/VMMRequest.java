package org.globus.workspace.service.impls.async;

import org.globus.workspace.WorkspaceException;

/**
 * Created by IntelliJ IDEA.
 * User: carla
 * Date: 17/07/11
 * Time: 10:37
 * To change this template use File | Settings | File Templates.
 */
public interface VMMRequest {

    public String execute() throws WorkspaceException;

}
