package org.globus.workspace.sqlauthz;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.workspace.groupauthz.DecisionLogic;
import org.globus.workspace.groupauthz.GroupRights;
import org.globus.workspace.persistence.WorkspaceDatabaseException;
import org.globus.workspace.service.binding.vm.VirtualMachinePartition;
import org.nimbustools.api.services.rm.AuthorizationException;
import org.nimbustools.api.services.rm.ResourceRequestDeniedException;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by John Bresnahan
 * User: bresnaha
 * Date: May 20, 2010
 * Time: 6:41:26 AM
 * <p/>
 * org.globus.workspace.sqlauthz
 */
public class AuthzDecisionLogic extends DecisionLogic
{
    private static final Log logger =
            LogFactory.getLog(AuthzDecisionLogic.class.getName());
    protected AuthzDBAdapter            authDB;
    protected SqlAuthz                  sqlA;

    public  AuthzDecisionLogic(
        AuthzDBAdapter                  dbAdapter,
        SqlAuthz                        sqlA)
    {
        this.authDB = dbAdapter;
        this.sqlA = sqlA;
        logger.debug("BuzzTroll AuthzDecider");
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
        logger.debug("BuzzTrol checkImages entered'");                

        for (int i = 0; i < parts.length; i++)
        {
            if (!parts[i].isPropRequired() && !parts[i].isUnPropRequired())
            {
                logger.debug("groupauthz not examining '" +
                                parts[i].getImage() + "': no prop/unprop needed");
                continue;
            }

            String incomingImageName = parts[i].getImage();
            String unPropImageName = parts[i].getAlternateUnpropTarget();

            logger.debug("BuzzTroll image " + incomingImageName + " requested");
            logger.debug("BuzzTroll unprop image " + unPropImageName + " requested");
            try
            {
                String newImageName = checkUrlAndTranslate(incomingImageName, dn, false);

                logger.debug("BuzzTroll setting new image name " + newImageName);
                parts[i].setImage(newImageName);

                if(unPropImageName != null)
                {
                    newImageName = checkUrlAndTranslate(unPropImageName, dn, true);
                    logger.debug("BuzzTroll setting new unprop name " + newImageName);
                    parts[i].setAlternateUnpropTarget(newImageName);
                }
            }
            catch (Exception e)
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

    private String checkUrlAndTranslate(
        String                          url,
        String                          userId,
        boolean                         write)
            throws AuthorizationException, WorkspaceDatabaseException
    {
        String scheme;
        String hostname;
        String objectName;
        String remaining;
        int    fileId;

        String [] results = url.split("://", 2);
        if(results == null || results.length != 2)
        {
            throw new  AuthorizationException("Poorly formed repository url, no scheme " + url + " " + results.length);
        }
        scheme = results[0];
        remaining = results[1];

        results = remaining.split("/", 2);
        if(results == null || results.length != 2)
        {
            throw new  AuthorizationException("Poorly formed repository url, no host separator " + url);
        }
        hostname = results[0];
        objectName = results[1];

        logger.debug("User requesting the " + scheme + " " + hostname + " " + objectName);
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
            // get the parent object id and filename
            results = objectName.split("/", 2);
            if(results == null || results.length != 2)
            {
                throw new AuthorizationException("Invalid bucket/key " + objectName);
            }
            logger.debug("Finding the fileID for " + results[0] + " " + results[1]);
            fileId = authDB.getFileID(results[0], results[1], schemeType);
        }
        else if (scheme.equals("file"))
        {
            return url;
        }
        else
        {
            throw new AuthorizationException("scheme of: " + scheme + " is not supported.");
        }

        String canUser = authDB.getCanonicalUserIdFromDn(userId);
        String perms = authDB.getPermissions(fileId, canUser);

        int ndx = perms.indexOf('r');
        if(ndx < 0)
        {
            throw new AuthorizationException("user " + userId + " canonical ID " + canUser + " does not have read access to " + url);
        }
        if(write)
        {
            ndx = perms.indexOf('w');
            if(ndx < 0)
            {
                throw new AuthorizationException("user " + userId + " does not have write access to " + url);
            }
        }

        String repoFile = authDB.getDataKey(fileId);
        String repoScheme = sqlA.getRepoScheme();
        String repoHost = sqlA.getReopHost();
        String repoDir = sqlA.getRepoDir();
        String rc = repoScheme + repoHost + repoDir + "/" + repoFile;

        return rc;        
    }

}
