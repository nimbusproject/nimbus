package org.globus.workspace;

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
        String                          publicName)
            throws WorkspaceException
    {
         return publicName;
    }

    public void unpropagationFinished(
        String                          publicName)
            throws WorkspaceException
    {

    }
}
