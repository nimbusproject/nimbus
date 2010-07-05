package org.globus.workspace.sqlauthz;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.workspace.RepoFileSystemAdaptor;
import org.globus.workspace.WorkspaceException;
import org.globus.workspace.groupauthz.DecisionLogic;
import org.globus.workspace.groupauthz.GroupRights;
import org.globus.workspace.persistence.WorkspaceDatabaseException;
import org.globus.workspace.service.binding.vm.VirtualMachinePartition;
import org.nimbustools.api.services.rm.AuthorizationException;
import org.nimbustools.api.services.rm.ResourceRequestDeniedException;
import org.nimbus.authz.AuthzDBAdapter;
import org.nimbus.authz.AuthzDBException;


import javax.sql.DataSource;
import java.io.File;

/**
 * Created by John Bresnahan
 * User: bresnaha
 * Date: May 20, 2010
 * Time: 6:41:26 AM
 * <p/>
 * org.globus.workspace.sqlauthz
 */
public class AuthzDecisionLogic extends DecisionLogic
    implements RepoFileSystemAdaptor
{
    private static final Log logger =
            LogFactory.getLog(AuthzDecisionLogic.class.getName());
    protected AuthzDBAdapter            authDB;
    private String                      repoScheme = null;
    private String                      repoHost = null;
    private String                      repoDir = null;
    public  AuthzDecisionLogic(
        DataSource ds)
    {
        this.authDB = new AuthzDBAdapter(ds);
    }

    public String translateExternaltoInternal(
        String                          publicUrl)
            throws WorkspaceException
    {
        String rc = null;
        try
        {
            String [] urlParts = parseUrl(publicUrl);

            String scheme = urlParts[0];
            String hostport = urlParts[1];
            String objectname = urlParts[2];

            // hinge on scheme, perhaps set up fancy interface plugin decision later
            if(scheme.equals("cumulus"))
            {
                rc = this.translateCumulus(hostport, objectname);
            }
            else
            {
                rc = publicUrl;
            }
        }
        catch(Exception ex)
        {
            throw new WorkspaceException("error translating cumulus name", ex);
        }

        if(rc == null)
        {
            throw new WorkspaceException("external image scheme " + publicUrl + " is not supported");
        }
        return rc;
    }

    protected String translateCumulus(
        String                          hostport,
        String                          objectName)
            throws AuthorizationException
    {
        try
        {
            int [] fileIds = this.cumulusGetFileID(hostport, objectName);

            if(fileIds[1] < 0)
            {
                throw new AuthorizationException("The file is not found and thus cannot be translated " + objectName);
            }

            String dataKey = this.authDB.getDataKey(fileIds[1]);
            String rc = this.getRepoScheme() + this.getRepoHost() + "/" + dataKey;

            logger.debug("converted " + objectName + " to " + rc);

            return rc;
        }
        catch(AuthzDBException wsdbex)
        {
            logger.error("iternal db problem", wsdbex);
            throw new AuthorizationException("Internal problem with the data base " + wsdbex.toString()); 
        }
    }

    private String [] parseUrl(
        String                          url)
            throws AuthorizationException
    {
        String [] results = url.split("://", 2);
        if(results == null || results.length != 2)
        {
            throw new  AuthorizationException("Poorly formed repository url, no scheme " + url);
        }
        String scheme = results[0];
        String remaining = results[1];

        results = remaining.split("/", 2);
        if(results == null || results.length != 2)
        {
            throw new  AuthorizationException("Poorly formed repository url, no host separator " + url);
        }
        String hostname = results[0];
        String objectName = results[1];

        results = new String[3];
        results[0] = scheme;
        results[1] = hostname;
        results[2] = objectName;

        return results;
    }

    private int [] cumulusGetFileID(
        String                          hostport,
        String                          objectName)
            throws AuthorizationException
    {
        String bucketName;
        String keyName;

        String [] results = objectName.split("/", 2);
        if(results == null || results.length != 2)
        {
            throw new  AuthorizationException("Poorly formed bucket/key " + objectName);
        }
        bucketName = results[0];
        keyName = results[1];

        try
        {
            int parentId = authDB.getFileID(bucketName, -1, AuthzDBAdapter.OBJECT_TYPE_S3);
            if (parentId < 0)
            {
                throw new AuthorizationException("No such bucket " + bucketName);
            }
            int fileId = authDB.getFileID(keyName, parentId, AuthzDBAdapter.OBJECT_TYPE_S3);
            int [] rc = new int[2];
            rc[0] = parentId;
            rc[1] = fileId;
            
            return rc;
        }
        catch(AuthzDBException wsdbex)
        {
            logger.error("trouble looking up the cumulus information ", wsdbex);
            throw new AuthorizationException("Trouble with the database " + wsdbex.toString());
        }
    }

    protected void checkImages(
        VirtualMachinePartition[]       parts,
        GroupRights                     rights,
        StringBuffer                    buf,
        String                          dn,
        String                          dnhash)
            throws AuthorizationException,
                   ResourceRequestDeniedException
    {
        boolean different_target = false;
        String unPropImageName = null;

        for (int i = 0; i < parts.length; i++)
        {
            if (!parts[i].isPropRequired() && !parts[i].isUnPropRequired())
            {
                logger.debug("groupauthz not examining '" +
                                parts[i].getImage() + "': no prop/unprop needed");
                continue;
            }

            String incomingImageName = parts[i].getImage();
            
            if(parts[i].isPropRequired())
            {
                unPropImageName = parts[i].getAlternateUnpropTarget();
                if(unPropImageName == null)
                {
                    unPropImageName = incomingImageName;
                }
                else
                {
                    different_target = true;
                }
            }            

            logger.debug("Image " + incomingImageName + " requested");
            logger.debug("Unprop image " + unPropImageName + " requested");
            try
            {
                boolean check_write;
                check_write = incomingImageName.equals(unPropImageName);
                
                // just authorize the image
                long size = checkUrl(incomingImageName, dn, check_write, 0);

                // if the names were different and we are unpropagating we need to check write on the new file
                if(unPropImageName != null && !check_write)
                {                    
                    checkUrl(unPropImageName, dn, true, size);
                }
            }
            catch (WorkspaceDatabaseException e)
            {
                final String msg = "ERROR: Partition in " +
                    "binding is not a valid URI? Can't make decision. " +
                        " Error message: " + e.getMessage();
                buf.append(msg);
                logger.error(buf.toString(), e);
                throw new AuthorizationException(msg);
            }
        }
    }

    private long checkUrl(
        String                          url,
        String                          userId,
        boolean                         write,
        long                            expectedSize)
            throws AuthorizationException, WorkspaceDatabaseException
    {
        int    fileId;
        String [] urlParts = this.parseUrl(url);
        String scheme = urlParts[0];
        String hostport = urlParts[1];
        String objectName = urlParts[2];

        int schemeType = -1;
        // Here would be a good place to hindge on scheme.  We could make a plugin interface
        // that allowed namespace conversion based on scheme and hostname:port.  Hostname is
        // also needed because it is possible to want to do something different for cumulus://hostA/file
        // than cumulus://hostB/file... for that matter we may also want to hindge on file path so we probably
        // need an interface to do regex matching.  all of that is a bit over engineered for now it will
        // be just cumulus and file
        if(scheme.equals("cumulus"))
        {
            schemeType = AuthzDBAdapter.OBJECT_TYPE_S3;
            try
            {
                String canUser = authDB.getCanonicalUserIdFromDn(userId);
                int [] fileIds = this.cumulusGetFileID(hostport, objectName);
                String [] results = objectName.split("/", 2);
                String bucketName = results[0];
                String keyName = results[1];
                boolean checkSpace = false;

                if(fileIds[0] < 0)
                {
                    throw new AuthorizationException("The bucket name " + bucketName + " was not found.");
                }
                String perms = "";
                if(fileIds[1] < 0 && write)
                {
                    String dataKey = this.getRepoDir() + "/" + objectName.replace("/", "__");
                    logger.debug("Adding new datakey " + dataKey);
                    fileIds[1] = authDB.newFile(keyName, fileIds[0], canUser, dataKey, schemeType);
                }
                perms = authDB.getPermissions(fileIds[1], canUser);
                if(fileIds[1] < 0)
                {
                    throw new AuthorizationException("the object " + objectName + " was not found.");
                }
                
                int ndx = perms.indexOf('r');
                if(ndx < 0)
                {
                    throw new AuthorizationException("user " + userId + " canonical ID " + canUser + " does not have read access to " + url);
                }
                long size = authDB.getFileSize(fileIds[1]);
                if(write)
                {
                    ndx = perms.indexOf('w');
                    if(ndx < 0)
                    {
                        throw new AuthorizationException("user " + userId + " does not have write access to " + url);
                    }

                    // expected size is only zero when replacing the original file.  in this case we assume it will
                    // fit
                    if(expectedSize != 0)
                    {
                        // deduct the size of the file that already exists from the expected size.  it
                        // is ok if it goes negative
                        long canFitSize = expectedSize - size;
                        boolean quota = authDB.canStore(canFitSize, canUser, schemeType);
                        if(!quota)
                        {
                            throw new AuthorizationException("You do not have enough storage space for the new image.  Please free up some storage and try again");
                        }
                    }
                }

                return size;
            }
            catch(AuthzDBException wsdbex)
            {
                logger.error("iternal db problem", wsdbex);
                throw new AuthorizationException("Internal problem with the data base " + wsdbex.toString());
            }
        }
        else if (scheme.equals("file"))
        {
            return 0;
        }
        else
        {
            throw new AuthorizationException("scheme of: " + scheme + " is not supported.");
        }
    }

    public void setRepoScheme(String repoScheme)
    {
        this.repoScheme = repoScheme;
    }

    public String getRepoScheme()
    {
        return this.repoScheme;
    }

    public void setRepoHost(String repoHost)
    {
        this.repoHost = repoHost;
    }

    public String getRepoHost()
    {
        return this.repoHost;
    }

    public void setRepoDir(String repoDir)
    {
        this.repoDir = repoDir;
    }

    public String getRepoDir()
    {
        return this.repoDir;
    }

    public void unpropagationFinished(
        String                          publicName)
        throws WorkspaceException
    {
        try
        {
            String [] urlParts = this.parseUrl(publicName);
            String scheme = urlParts[0];
            String hostport = urlParts[1];
            String objectName = urlParts[2];

            int schemeType = -1;
            if(scheme.equals("cumulus"))
            {
                schemeType = AuthzDBAdapter.OBJECT_TYPE_S3;
                int [] fileIds = this.cumulusGetFileID(hostport, objectName);
                String datakey = authDB.getDataKey(fileIds[1]);

                // need to calculate the md5sum and set the size
                // for now lets just set the size
                File f = new File(datakey);
                long size = f.length();
                long expectedSize = authDB.getFileSize(fileIds[1]);
                long sizeDiff = size - expectedSize;

                // if the size of the file grew from what it was expected to be we must make sure the quota
                // is not busted
                if(sizeDiff > 0)
                {
                    String canUser = authDB.getFileOwner(fileIds[1]);
                    boolean hasRoom = authDB.canStore(sizeDiff, canUser, schemeType);
                    if(!hasRoom)
                    {
                        logger.error("FOR TIMF callout happens here");
                    }
                }

                authDB.setFileSize(fileIds[1], size);
            }
            else
            {
                return;
            }
        }
        catch(AuthorizationException authex)
        {
            throw new WorkspaceException("Authorization exception occured ", authex);
        }
        catch(AuthzDBException wsdbex)
        {
            throw new WorkspaceException("Workspace database exception occured ", wsdbex);
        }
    }
}
