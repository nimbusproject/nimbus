package org.globus.workspace.sqlauthz;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.workspace.RepoFileSystemAdaptor;
import org.globus.workspace.WorkspaceException;
import org.globus.workspace.groupauthz.DecisionLogic;
import org.globus.workspace.groupauthz.GroupRights;
import org.globus.workspace.persistence.WorkspaceDatabaseException;
import org.globus.workspace.service.binding.authorization.Decision;
import org.globus.workspace.service.binding.vm.VirtualMachine;
import org.globus.workspace.service.binding.vm.VirtualMachinePartition;
import org.nimbustools.api.brain.NimbusHomePathResolver;
import org.nimbustools.api.services.rm.AuthorizationException;
import org.nimbustools.api.services.rm.ResourceRequestDeniedException;
import org.nimbus.authz.AuthzDBAdapter;
import org.nimbus.authz.AuthzDBException;
import org.springframework.core.io.Resource;
import javax.sql.DataSource;
import java.io.*;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
    private boolean                     schemePassthrough;
    private String                      passthroughSchemes = null;

    public  AuthzDecisionLogic(
        DataSource ds,
        String schemePassthroughStr)
    {
        // String nh = System.getProperty(NimbusHomePathResolver.NIMBUS_HOME_ENV_NAME);
       // might want to set default path some time
        this.authDB = new AuthzDBAdapter(ds);
        this.schemePassthrough =
                schemePassthroughStr != null
                    && schemePassthroughStr.trim().equalsIgnoreCase("true");
    }

    private String [] getPassThroughSchemeList()
    {
        String [] list = passthroughSchemes.split(",");
        return list;
    }

     public String getTranslatedChecksum(
         String                          publicUrl)    
            throws WorkspaceException
    {
        try
        {
            String [] urlParts = parseUrl(publicUrl);

            String scheme = urlParts[0];
            String hostport = urlParts[1];
            String objectname = urlParts[2];

            int [] fileIds = this.cumulusGetFileID(hostport, objectname);

            String md5sum = this.authDB.getMd5sum(fileIds[1]);

            return md5sum;
        }
        catch(AuthzDBException wsdbex)
        {
            logger.error("trouble looking up the cumulus information ", wsdbex);
            throw new WorkspaceException("Trouble with the database " + wsdbex.toString());
        }
        catch(Exception ex)
        {
            throw new WorkspaceException("error finding the checksum " + publicUrl, ex);
        }
    }

    public String translateExternaltoInternal(
        String                          publicUrl,
        VirtualMachine                  vm)
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
                rc = this.translateCumulus(hostport, objectname, vm);
            }
            else
            {
                String [] sl = getPassThroughSchemeList();
                for(int i = 0; i < sl.length; i++)
                {
                    if(scheme.equals(sl[i]))
                    {
                        rc = publicUrl;
                    }
                }
            }
        }
        catch(Exception ex)
        {
            throw new WorkspaceException("error translating cumulus name " + publicUrl, ex);
        }

        if(rc == null)
        {
            throw new WorkspaceException("external image scheme " + publicUrl + " is not supported");
        }
        return rc;
    }

    protected String translateCumulus(
        String                          hostport,
        String                          objectName,
        VirtualMachine                  vm)
            throws AuthorizationException
    {
        try
        {
            int [] fileIds = this.cumulusGetFileID(hostport, objectName);
            String rc = null;
            String scheme = this.getRepoScheme();
            String dataKey;

            if(fileIds[1] < 0)
            {
                // if the file doesnt exist create  new one
                dataKey = this.getRepoDir() + "/" + objectName.replace("/", "__");
//
//                File dirF = new File(this.getRepoDir());
//
//                try
//                {
//                    File tmpF = File.createTempFile("", objectName.replace("/", "__"), dirF);
//                    dataKey  = tmpF.getAbsolutePath();
//                }
//                catch(IOException ioex)
//                {
//                    String msg = "could not create unpropagate file " + objectName;
//                    logger.error(msg, ioex);
//                    throw new AuthorizationException(msg  + ioex.toString());
//                }
            }
            else
            {
                dataKey = this.authDB.getDataKey(fileIds[1]);
                String md5sum = this.authDB.getMd5sum(fileIds[1]);
                
            }
            rc = scheme + "://" + this.getRepoHost() + "/" + dataKey;
            logger.debug("converted " + objectName + " to " + rc + "scheme " + scheme);

            return rc;
        }
        catch(AuthzDBException wsdbex)
        {
            logger.error("internal db problem", wsdbex);
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
        String ownerID;

        try
        {
            ownerID = this.authDB.getCanonicalUserIdFromDn(dn);
        }
        catch(AuthzDBException aex)
        {
            throw new AuthorizationException("Could not find the user " + dn, aex);
        }

        for (int i = 0; i < parts.length; i++)
        {
            if (!parts[i].isPropRequired() && !parts[i].isUnPropRequired())
            {
                logger.debug("groupauthz not examining '" +
                                parts[i].getImage() + "': no prop/unprop needed");
                continue;
            }

            String incomingImageName = parts[i].getImage();

            if(parts[i].isUnPropRequired())
            {
                unPropImageName = parts[i].getAlternateUnpropTarget();
                if(unPropImageName == null)
                {
                    unPropImageName = incomingImageName;
                    
                    String commonPath = "/common/";
                    if(incomingImageName.indexOf(commonPath) > 0)
                    {
                        // replace common path with user path
                        String userPath = "/" + ownerID + "/";
                        unPropImageName = unPropImageName.replaceFirst(commonPath, userPath);
                        parts[i].setAlternateUnpropTarget(unPropImageName);
                    }                                                                        
                }
                else
                {
                    different_target = true;
                }
            }            

            if (different_target) {
                logger.debug("Image '" + incomingImageName + "' requested, unpropagation " +
                        "image is different: '" + unPropImageName + "'");
            } else {
                logger.debug("Image '" + incomingImageName + "' requested (unprop is same)");
            }
            
            try
            {
                // see if we are allowed to read the image
                long size = checkUrl(incomingImageName, dn, false, 0);

                // if unpropagting, see if we are allowed to write to the unprop name
                if(unPropImageName != null)
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
            throws AuthorizationException, ResourceRequestDeniedException, WorkspaceDatabaseException
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

                boolean checkSpace = false;
                String perms = "";
                long size = 0;

                if(fileIds[0] < 0)
                {
                    throw new ResourceRequestDeniedException("The bucket name " + bucketName + " was not found.");
                }                
                if(fileIds[1] < 0 && write)
                {
                    String pubPerms = perms + authDB.getPermissionsPublic(fileIds[0]);
                    perms = authDB.getPermissions(fileIds[0], canUser) + pubPerms;
                    int ndx = perms.indexOf('w');
                    if(ndx < 0)
                    {
                        throw new ResourceRequestDeniedException("user " + userId + " does not have write access the bucket " + url);
                    }
                }                
                else if(fileIds[1] < 0)
                {                                                                                                 
                    throw new ResourceRequestDeniedException("the object " + objectName + " was not found.");
                }
                else
                {
                    String pubPerms = perms + authDB.getPermissionsPublic(fileIds[1]);
                    perms = authDB.getPermissions(fileIds[1], canUser) + pubPerms;
                    int ndx = perms.indexOf('r');
                    if(ndx < 0)
                    {
                        throw new ResourceRequestDeniedException("user " + userId + " canonical ID " + canUser + " does not have read access to " + url);
                    }
                    size = authDB.getFileSize(fileIds[1]);
                    if(write)
                    {
                        ndx = perms.indexOf('w');
                        if(ndx < 0)
                        {
                            throw new ResourceRequestDeniedException("user " + userId + " does not have write access to " + url);
                        }
                    }
                }
                if(write)
                {
                    // expected size is only zero when replacing the original file.  in this case we assume it will
                    // fit
                    if(expectedSize != 0)
                    {
                        // deduct the size of the file that already exists from the expected size.  it
                        // is ok if it goes negative
                        long canFitSize = expectedSize - size;
                        boolean quota = authDB.canStore(canFitSize, canUser, schemeType);
                        if (!quota)
                        {
                            throw new ResourceRequestDeniedException("You do not have enough storage space for the new image.  Please free up some storage and try again");
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
        else if (this.schemePassthrough)
        {
            return 0;
        }
        else
        {
            throw new ResourceRequestDeniedException("scheme of: " + scheme + " is not supported.");
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

    public void setPassthroughSchemes(String passthroughSchemes)
    {
        this.passthroughSchemes = passthroughSchemes;
    }

    public String getPassthroughSchemes()
    {
        return this.passthroughSchemes;
    }

    public void unpropagationFinished(
        String                          publicName,
        String                          creatorID,
        VirtualMachine                  vm)
            throws WorkspaceException
    {
        try
        {
            String [] urlParts = this.parseUrl(publicName);
            String scheme = urlParts[0];
            String hostport = urlParts[1];
            String objectName = urlParts[2];
            String [] results = objectName.split("/", 2);
            String bucketName = results[0];
            String keyName = results[1];


            int schemeType = -1;
            if(scheme.equals("cumulus"))
            {
                schemeType = AuthzDBAdapter.OBJECT_TYPE_S3;
                String canUser = authDB.getCanonicalUserIdFromDn(creatorID);
                int [] fileIds = this.cumulusGetFileID(hostport, objectName);
                String datakey;
                boolean new_file = false;
                long expectedSize = 0;

                if (fileIds[1] < 0) {
                    new_file = true;
                    String unpropurl = translateExternaltoInternal(publicName, vm);
                    String [] unpropurlParts = this.parseUrl(unpropurl);
                    datakey = unpropurlParts[2];
                }
                else
                {
                    datakey = authDB.getDataKey(fileIds[1]);
                    expectedSize = authDB.getFileSize(fileIds[1]);
                }
                File f = new File(datakey);               
                long size = f.length();
                long sizeDiff = size - expectedSize;

                // if the size of the file grew from what it was expected to be we must make sure the quota
                // is not busted
                if(sizeDiff > 0)
                {
                    boolean hasRoom = authDB.canStore(sizeDiff, canUser, schemeType);
                    if(!hasRoom)
                    {
                        logger.error("Client exceeded quota on this unpropagation, this can " +
                                "happen if they chose to unpropagate an image that is bigger " +
                                "than the one propagated. We are letting this one slide to " +
                                "avoid data loss (user '" + canUser + "').");
                    }
                }
                String md5string = "";
                try
                {
                    InputStream fis =  new FileInputStream(f);
                    byte[] md5_buffer = new byte[1024];
                    MessageDigest md5er = MessageDigest.getInstance("MD5");

                    int rc = fis.read(md5_buffer);
                    while (rc > 0)
                    {
                        md5er.update(md5_buffer, 0, rc);
                        rc = fis.read(md5_buffer);
                    }
                    fis.close();
                    byte [] md5b = md5er.digest();
                    StringBuffer hexString = new StringBuffer();
                    for (int i=0;i<md5b.length;i++)
                    {
                        String tmpS = Integer.toHexString(0xFF & md5b[i]);
                        while(tmpS.length() != 2)
                        {
                            tmpS = "0" + tmpS;
                        }
                        hexString.append(tmpS);
                    }
                    md5string = hexString.toString();                    
                }
                catch(FileNotFoundException fnf)
                {
                    throw new WorkspaceException("Unpropagated file not found", fnf);  
                }
                catch(NoSuchAlgorithmException nsa)
                {
                    logger.error("There is no md5 digest", nsa);
                }
                catch(IOException ioe)
                {
                    logger.error("Error dealing with the unpropgated file", ioe);
                }

                if(new_file)
                {
                    fileIds[1] = authDB.newFile(keyName, fileIds[0], canUser, datakey, schemeType);
                }
                authDB.setFileSize(fileIds[1], size, md5string);
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

    public Integer checkNewAltTargetURI(
        GroupRights rights,
        URI altTargetURI,
        String dn)
            throws AuthorizationException
    {

        final String unPropImageName = altTargetURI.toASCIIString();
        try
        {
            // if unpropagting, see if we are allowed to write to the unprop name
            checkUrl(unPropImageName, dn, true, 0);
        }
        catch (WorkspaceDatabaseException e)
        {
            final String msg = "ERROR: Partition in " +
                "binding is not a valid URI? Can't make decision. " +
                    " Error message: " + e.getMessage();
            logger.error(msg, e);
            throw new AuthorizationException(msg);
        } catch (ResourceRequestDeniedException e) {
            logger.error(e.getMessage());
            return Decision.DENY;
        }

        return Decision.PERMIT;
    }
}
