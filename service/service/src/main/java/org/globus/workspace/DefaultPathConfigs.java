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

    private String localTempDirPath;
    private String backendTempDirPath;


    // -------------------------------------------------------------------------
    // GET/SET
    // -------------------------------------------------------------------------

    public String getLocalTempDirPath() {
        return this.localTempDirPath;
    }

    public void setLocalTempDirPath(String localTempDirPath) {
        this.localTempDirPath = localTempDirPath;
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
        if (this.localTempDirPath != null
                && this.backendTempDirPath == null) {
            throw new Exception("Local tmpfiles directory is configured " +
                        "but backend tmpfiles path is not.");
        }

        if (this.localTempDirPath != null) {
            logger.debug("Checking on local tmpfiles directory: '" +
                                            this.localTempDirPath + "'");
            final File dir = new File(this.localTempDirPath);
            if (!dir.exists()) {
                throw new Exception("Local tmpfiles directory does not " +
                        "exist. Configuration is '" +
                        this.localTempDirPath + "'");
            }
            if (!dir.isDirectory()) {
                throw new Exception("Local tmpfiles directory exists but is " +
                        "not a directory?  Configuration is '" +
                        this.localTempDirPath + "'");
            }
        } else {
            logger.warn("No local tmpfiles directory is configured, " +
                        "some functionality is disabled as a result.");
        }
    }
}
