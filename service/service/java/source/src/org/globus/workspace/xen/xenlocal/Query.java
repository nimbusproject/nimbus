package org.globus.workspace.xen.xenlocal;

import org.globus.workspace.WorkspaceException;
import org.globus.workspace.xen.XenRequest;
import org.globus.workspace.xen.XenTask;
import org.globus.workspace.xen.XenUtil;

import java.util.ArrayList;

/**
 * @author Carla Souza
 * @date 07/06/11 00:16
 */
public class Query extends XenRequest {

    protected void init() throws WorkspaceException {
        this.name = "Query";
        this.doFakeLag = true;
        final ArrayList exe = XenUtil.constructQueryCommand();
        this.cmd = (String[]) exe.toArray(new String[exe.size()]);

    }


}
