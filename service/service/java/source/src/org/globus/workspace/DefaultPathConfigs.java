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

package org.globus.workspace;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.Resource;

import java.io.File;

public class DefaultPathConfigs implements PathConfigs {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(DefaultPathConfigs.class.getName());


    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private Resource localTempDirResource;
    private String localTempDirAbsolutePath;
    private String backendTempDirPath;


    // -------------------------------------------------------------------------
    // GET/SET
    // -------------------------------------------------------------------------

    public String getLocalTempDirPath() {
        return this.localTempDirAbsolutePath;
    }

    public void setLocalTempDirResource(Resource localTempDirResource) {
        this.localTempDirResource = localTempDirResource;
    }

    public String getBackendTempDirPath() {
        return this.backendTempDirPath;
    }

    public void setBackendTempDirPath(String backendTempDirPath) {
        this.backendTempDirPath = backendTempDirPath;
    }


    // -------------------------------------------------------------------------
    // IoC INIT METHOD
    // -------------------------------------------------------------------------

    public void validate() throws Exception {

        // tied to each other
        if (this.localTempDirResource != null
                && this.backendTempDirPath == null) {
            throw new Exception("Local tmpfiles directory is configured " +
                        "but backend tmpfiles path is not.");
        }

        if (this.localTempDirResource != null) {
            final File dir = this.localTempDirResource.getFile();
            this.localTempDirAbsolutePath = dir.getAbsolutePath();

            logger.debug("Checking on local tmpfiles directory: '" +
                                            this.localTempDirAbsolutePath + '\'');
            
            if (!dir.exists()) {
                throw new Exception("Local tmpfiles directory does not " +
                        "exist. Configuration is '" +
                        this.localTempDirAbsolutePath + "'");
            }
            if (!dir.isDirectory()) {
                throw new Exception("Local tmpfiles directory exists but is " +
                        "not a directory?  Configuration is '" +
                        this.localTempDirAbsolutePath + "'");
            }
        } else {
            logger.warn("No local tmpfiles directory is configured, " +
                        "some functionality is disabled as a result.");
        }
    }
}
