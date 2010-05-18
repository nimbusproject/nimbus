/*
 * Copyright 1999-2008 University of Chicago
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.globus.workspace.sqlauthz;

import org.globus.workspace.persistence.WorkspaceDatabaseException;
import org.globus.workspace.service.binding.authorization.CreationAuthorizationCallout;
import org.globus.workspace.service.binding.authorization.Decision;
import org.globus.workspace.service.binding.authorization.PostTaskAuthorization;
import org.globus.workspace.service.binding.vm.VirtualMachine;
import org.globus.workspace.service.binding.vm.VirtualMachinePartition;
import org.nimbustools.api.services.rm.AuthorizationException;
import org.nimbustools.api.services.rm.ResourceRequestDeniedException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.sql.*;
import javax.security.auth.Subject;
import javax.sql.DataSource;
import java.net.URI;
import java.util.Vector;

public class SqlAuthz
    implements CreationAuthorizationCallout,
        PostTaskAuthorization 
{
    private static final Log logger =
            LogFactory.getLog(SqlAuthz.class.getName());
    private AuthzDBAdapter                  authDB = null;
    private DataSource                      dataSource = null;
    private String                          repoScheme = null;
    private String                          repoHost = null;
    private String                          repoDir = null;

    public SqlAuthz(DataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    public boolean isEnabled()
    {
        return true;
    }
    
    public void validate() throws Exception
    {
        logger.debug("Initializing sql authorization");
        this.authDB = new AuthzDBAdapter(this.dataSource);
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
        // convert url to type
        if(scheme.equals("cumulus"))
        {
            schemeType = AuthzDBAdapter.OBJECT_TYPE_S3;
            // get the parent object id and filename
            results = objectName.split("/", 2);
            if(results == null || results.length != 2)
            {
                throw new AuthorizationException("Invalid bucket/key " + objectName);
            }
            fileId = authDB.getFileID(results[0], results[1], schemeType);
        }
        else
        {
            throw new AuthorizationException("scheme of: " + scheme + " is not supported.");
        }

        String perms = authDB.getPermissions(fileId, userId);

        int ndx = perms.indexOf('r');
        if(ndx < 0)
        {
            throw new AuthorizationException("user " + userId + " does not have read access to " + url);
        }
        if(write)
        {
            ndx = perms.indexOf('w');
            if(ndx < 0)
            {
                throw new AuthorizationException("user " + userId + " does not have write access to " + url);
            }
        }

        String newZooRevue = this.repoScheme + this.repoHost + this.repoDir + authDB.getDataKey(fileId);
        return newZooRevue;
    }

    public Integer isPermitted(VirtualMachine[] bindings,
                               String callerDN,
                               Subject subject,
                               Long elapsedMins,
                               Long reservedMins,
                               int numWorkspaces)

            throws AuthorizationException,
                   ResourceRequestDeniedException
    {

        logger.debug("BuzzTroll isPermitted() " + callerDN + " " + subject + " ");
        try
        {
            String userId = authDB.getCanonicalUserIdFromAlias(callerDN, AuthzDBAdapter.ALIAS_TYPE_DN);

            for(int i = 0; i < bindings.length; i++)
            {
                VirtualMachinePartition[] parts = bindings[i].getPartitions();

                for(int j = 0; j < parts.length; j++)
                {
                    String image = parts[j].getImage();

                    logger.debug("BuzzTroll isPermitted() image = " + image);
                    String newName = this.checkUrlAndTranslate(image, userId, false);
                    logger.debug("BuzzTroll isPermitted() setting new image to = " + newName);
                    parts[j].setImage(newName);
                }
            }
        }
        catch (WorkspaceDatabaseException wdex)
        {
            throw new AuthorizationException("error with database object " + wdex.toString());
        }

        return Decision.PERMIT;
    }

    public String isRootPartitionUnpropTargetPermittedAndChange(URI target,
                                                        String caller)
            throws AuthorizationException
    {
        logger.debug("BuzzTroll isRootPartitionUnpropTargetPermitted() " + caller + " " + target.toString());
        // there may be null values in this.groups, see getRights method
        try
        {
            String newName = this.checkUrlAndTranslate(target.toString(), caller, false);
            logger.debug("BuzzTroll UnProp newName = " + newName);
            return newName;
        }
        catch (WorkspaceDatabaseException wdex)
        {
            throw new AuthorizationException("error with database object " + wdex.toString());
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

    public String getReopHost()
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
}
