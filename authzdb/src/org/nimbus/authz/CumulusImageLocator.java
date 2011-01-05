/*
 * Copyright 1999-2011 University of Chicago
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

package org.nimbus.authz;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.sql.DataSource;

public class CumulusImageLocator implements RepositoryImageLocator {

    // -----------------------------------------------------------------------------------------
    // STATIC VARIABLES
    // -----------------------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(CumulusImageLocator.class.getName());


    // -----------------------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -----------------------------------------------------------------------------------------

    protected final AuthzDBAdapter authDB;

    protected String cumulusHost = null;
    protected String repoBucket = null;
    protected String prefix = null;
    protected String rootFileMountAs = null;


    // -----------------------------------------------------------------------------------------
    // CONSTRUCTORS
    // -----------------------------------------------------------------------------------------

    public CumulusImageLocator(DataSource dataSource) {
        if (dataSource == null) {
            throw new IllegalArgumentException("dataSource is missing");
        }
        this.authDB = new AuthzDBAdapter(dataSource);
    }


    // -----------------------------------------------------------------------------------------
    // GET/SET
    // -----------------------------------------------------------------------------------------

    public void setCumulusHost(String cumulusHost) {
        this.cumulusHost = cumulusHost;
    }

    public void setRepoBucket(String repoBucket) {
        this.repoBucket = repoBucket;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void setRootFileMountAs(String rootFileMountAs) {
        this.rootFileMountAs = rootFileMountAs;
    }

    
    // -----------------------------------------------------------------------------------------
    // IoC init
    // -----------------------------------------------------------------------------------------

    void validate() throws Exception {
        if (this.cumulusHost == null) {
            throw new Exception("Invalid: Missing 'cumulus host' string");
        }
        if (this.repoBucket == null) {
            throw new Exception("Missing the 'repoBucket' setting");
        }
        if (this.prefix == null) {
            throw new Exception("Missing the 'prefix' setting");
        }
        if (this.rootFileMountAs == null) {
            throw new Exception("Missing the 'rootFileMountAs' setting");
        }
    }

    // -----------------------------------------------------------------------------------------
    // implements RepositoryImageLocator
    // -----------------------------------------------------------------------------------------

    public String getImageLocation(String DN, String vmname) throws Exception {
        
        String ownerID = this.authDB.getCanonicalUserIdFromDn(DN);
        if (ownerID == null) {
            throw new Exception("No caller/ownerID?");
        }
        
        try {
            int parentId = authDB.getFileID(this.repoBucket , -1, AuthzDBAdapter.OBJECT_TYPE_S3);
            if (parentId < 0) {
                throw new Exception("No such bucket " + this.repoBucket);
            }

            String userKeyName = this.prefix + "/" + ownerID;
            String commonKeyName = this.prefix + "/common";
            String keyName = userKeyName;

            int fileId = authDB.getFileID(userKeyName + "/" + vmname, parentId, AuthzDBAdapter.OBJECT_TYPE_S3);
            if(fileId < 0) {
                fileId = authDB.getFileID(commonKeyName + "/" + vmname, parentId, AuthzDBAdapter.OBJECT_TYPE_S3);
                if(fileId >= 0) {
                    keyName = commonKeyName;
                }
            }
            return "cumulus://" + this.cumulusHost + "/" + this.repoBucket + "/" + keyName;
            
        } catch(AuthzDBException wsdbex) {
            logger.error("trouble looking up the cumulus information: "
                                 + wsdbex.getMessage(), wsdbex);
            throw new Exception("Trouble with the database " + wsdbex.toString());
        }
    }
}
