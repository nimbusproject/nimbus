/*
 * Copyright 1999-2010 University of Chicago
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
package org.nimbustools.api.brain;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;

public class NimbusHomePathResolver {

    private static final Log logger =
            LogFactory.getLog(NimbusHomePathResolver.class.getName());

    private static String NIMBUS_HOME_ENV_NAME = "NIMBUS_HOME";
    private static String PATH_PREFIX = "$"+NIMBUS_HOME_ENV_NAME;

    private File nimbusHome;

    private void initializeNimbusHome() {
        final String nimbusHomePath = System.getenv(NIMBUS_HOME_ENV_NAME);

        if (nimbusHomePath == null || nimbusHomePath.trim().length() == 0) {
            throw new RuntimeException("The " + NIMBUS_HOME_ENV_NAME +
                    " environment variable is not set or is empty. " +
                    "It should be set to the root Nimbus installation " +
                    "directory by the Nimbus bootstrap scripts."
            );
        }

        this.nimbusHome = new File(nimbusHomePath);

        if (!(this.nimbusHome.isDirectory() && this.nimbusHome.canRead())) {
            throw new RuntimeException("\"The" + NIMBUS_HOME_ENV_NAME +
                    " environment variable ("+ nimbusHomePath +") refers" +
                    " to a path that does not exist, is not a directory, or" +
                    " is not readable."
            );
        }

    }

    public String resolvePath(String path) {
        if (path == null) {
            throw new IllegalArgumentException("path may not be null");
        }

        final String givenPath = path;

        final String possibleScheme = "file:";
        if (path.startsWith(possibleScheme)) {
            path = path.substring(possibleScheme.length());
        }

        if (path.startsWith(PATH_PREFIX)) {

            if (this.nimbusHome == null) {
                this.initializeNimbusHome();
            }

            final String relPath = path.substring(PATH_PREFIX.length());
            final String newPath = this.nimbusHome.getAbsolutePath() + relPath;

            logger.debug("Resource path changed from '" + givenPath +
                    "' into '" + newPath + '\'');

            return newPath;
        }
        return null;
    }
}
