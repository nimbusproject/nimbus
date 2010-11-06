package org.globus.workspace.sqlauthz;

import org.globus.workspace.RepoFileSystemAdaptor;
import org.globus.workspace.WorkspaceException;
import org.globus.workspace.service.binding.vm.VirtualMachine;

/**
 * Created by John Bresnahan
 * User: bresnaha
 * Date: May 24, 2010
 * Time: 12:56:49 PM
 * <p/>
 * org.globus.workspace
 */
public class CumulusRepoFileSystemAdaptor implements RepoFileSystemAdaptor
{
    protected AuthzDecisionLogic dl;

    public CumulusRepoFileSystemAdaptor(
        AuthzDecisionLogic dl)
    {
        this.dl = dl;
    }

    public String translateExternaltoInternal(
        String                          publicName,
        VirtualMachine                  vm)
            throws WorkspaceException
    {
        return dl.translateExternaltoInternal(publicName, vm);
    }

    public void unpropagationFinished(
        String                          publicName,
        String                          creatorID,
        VirtualMachine                  vm)
            throws WorkspaceException
    su        dl.unpropagationFinished(publicName, creatorID, vm);
    }
}

