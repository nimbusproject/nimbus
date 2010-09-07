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

package org.nimbustools.auto_config.confmgr;

import java.io.File;

public class AlterCommon {

    public void alter(String confname,
                      String keyword,
                      String newvalue) throws Exception {

        GenericConfUtil util = this.findGenericConfUtil();
        final File ssh = util.getWorkspConfFile(confname);
        util.setProperty(ssh, keyword, newvalue);
    }

    public GenericConfUtil findGenericConfUtil() throws Exception {
        final String glprop = System.getProperty("GLOBUS_LOCATION");
        if (glprop == null || glprop.trim().length() == 0) {
            throw new Exception("Could not determine GLOBUS_LOCATION");
        }

        final File gl = new File(glprop);
        if (!gl.exists()) {
            throw new Exception(
                    "The configured GLOBUS_LOCATION does not exist: " + glprop);
        }

        final File nimbusDir = new File(gl, "etc" + File.separator + "nimbus");
        if (!nimbusDir.exists()) {
            throw new Exception(
                    "Nimbus configuration directory does not exist: " + nimbusDir);
        }

        return new GenericConfUtil(nimbusDir.getAbsolutePath());
    }
}
