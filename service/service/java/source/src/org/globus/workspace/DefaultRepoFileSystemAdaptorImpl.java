package org.globus.workspace;

import org.globus.workspace.service.binding.vm.VirtualMachine;

/**
 * Created by John Bresnahan
 * User: bresnaha
 * Date: May 24, 2010
 * Time: 12:50:27 PM
 * <p/>
 * org.globus.workspace
 */
public class DefaultRepoFileSystemAdaptorImpl implements RepoFileSystemAdaptor
{
    public String translateExternaltoInternal(
        String                          publicName,
        VirtualMachine                  vm)
            throws WorkspaceException
    {
         return publicName;
    }

    public String getTranslatedChecksum(
        String                          publicUrl)
    {
        return null;
    }

    public void unpropagationFinished(
        String                          publicName,
        String                          creatorID,
        VirtualMachine                  vm)
            throws WorkspaceException
    {

    }
}
