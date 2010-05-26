package org.globus.workspace.sqlauthz;

import org.globus.workspace.RepoFileSystemAdaptor;
import org.globus.workspace.WorkspaceException;

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
        String                          publicName)
            throws WorkspaceException
    {
        return dl.translateExternaltoInternal(publicName);
    }

    public void unpropagationFinished(
        String                          publicName)
            throws WorkspaceException
    {
        dl.unpropagationFinished(publicName);
    }
}

