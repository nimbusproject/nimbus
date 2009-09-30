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

import org.globus.workspace.groupauthz.Group;

public class FindHash extends Report {


    public FindHash(String confPath, String[] args, boolean debug)
            throws Exception {
        super(confPath, args, debug);
    }

    public void run() throws Exception {

        if (this.args.length > 1) {
            throw new Exception(
                    "Requires just one argument, the hash to search for.");
        }

        if (this.args.length == 0 || this.args[0].trim().length() == 0) {
            throw new Exception(
                    "Requires just one argument, the hash to search for.");
        }

        Group[] groups = this.groupAuthz.getGroups();
        if (groups == null || groups.length == 0) {
            throw new Exception(
                    "The authz module has no groups configured, hash not found.");
        }

        for (int i = 0; i < groups.length; i++) {
            if (groups[i] != null) {
                final String[] dns = groups[i].getIdentities();
                if (dns == null) {
                    continue;
                }
                
                for (int j = 0; j < dns.length; j++) {
                    if (dns[j] != null) {
                        final String dn = dns[j].trim();
                        final String hash = Hash.hash(dn);
                        if (this.args[0].equals(hash)) {
                            System.out.println(dn);
                            return;
                        }
                    }
                }
            }
        }
        throw new Exception("Hash not found: '" + this.args[0] + "'");
    }
}
