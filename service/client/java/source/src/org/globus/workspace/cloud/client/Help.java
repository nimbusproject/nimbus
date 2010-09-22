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

package org.globus.workspace.cloud.client;

import org.globus.workspace.client_core.utils.StringUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * The help printing via BaseClient and commons CLI is not helpful.
 */
public class Help {

    public String getHelpString() throws IOException {
        return this.getStringFromJar("cloud-help.txt");
    }

    public String getExtraHelpString() throws IOException {
        return this.getStringFromJar("cloud-help-extra.txt");
    }

    public String getUsageString() throws IOException {
        return this.getStringFromJar("cloud-usage.txt");
    }

    private String getStringFromJar(String path) throws IOException {
        InputStream is = null;
        try {
            is = this.getClass().getResourceAsStream(path);
            if (is == null) {
                return "Sorry, cannot find '" + path + "' in the jar file " +
                        "alongside " + this.getClass();
            }
            return StringUtils.getTextFileViaInputStream(is);
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }
}
