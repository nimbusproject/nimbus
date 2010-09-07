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

package org.nimbustools.auto_config.groupauthz.harness;

import org.globus.workspace.groupauthz.HashUtil;

public class Hash extends Action {

    public Hash(String confPath, String[] args, boolean debug)
            throws Exception {
        super(confPath, args, debug);
    }

    public static String hash(String DN) throws Exception {
        if (!HashUtil.isInitialized()) {
            throw new Exception(
                    "Hashing system did not initialize properly.");
        }
        return HashUtil.hashDN(DN);
    }

    public void run() throws Exception {

        if (this.args.length > 1) {
            throw new Exception(
                    "Requires just one argument, the DN to hash " +
                            "(you may need to quote DN with spaces?).");
        }

        if (this.args.length == 0 || this.args[0].trim().length() == 0) {
            throw new Exception(
                    "Requires just one argument, the DN to hash.");
        }

        System.out.println(hash(this.args[0]));
    }
}
