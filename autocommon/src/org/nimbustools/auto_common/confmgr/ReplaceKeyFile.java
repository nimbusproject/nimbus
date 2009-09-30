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

package org.nimbustools.auto_common.confmgr;

public class ReplaceKeyFile {

    // -------------------------------------------------------------------------
    // REPLACE
    // -------------------------------------------------------------------------

    public void replaceKeyFile(String newval,
                               String configPath) throws Exception {

        if (newval == null) {
            throw new IllegalArgumentException("newval may not be null");
        }
        if (configPath == null) {
            throw new IllegalArgumentException("configPath may not be null");
        }

        new SecDescConfigReplace().replace(configPath,
                                           "key-file",
                                           newval);
    }


    // -------------------------------------------------------------------------
    // MAIN
    // -------------------------------------------------------------------------

    public static void main(String[] args) {

        if (args == null || args.length != 2) {
            System.err.println("Needs these arguments:\n" +
                    "1 - the new key-file path value\n" +
                    "2 - the global_security_descriptor.xml path");
            System.exit(1);
        }

        try {
            new ReplaceKeyFile().replaceKeyFile(args[0], args[1]);
        } catch (Exception e) {
            System.err.println("Problem replacing key-file value: " + e.getMessage());
            System.exit(1);
        }
    }
}
