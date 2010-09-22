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

public class Report extends ReportAll {

    public Report(String confPath, String[] args, boolean debug) throws Exception {
        super(confPath, args, debug);
    }

    public void run() throws Exception {

        if (this.args.length > 1) {
            throw new Exception(
                    "Requires just one argument, the DN to search for " +
                            "(you may need to quote DN with spaces?).");
        }

        if (this.args.length == 0 || this.args[0].trim().length() == 0) {
            throw new Exception(
                    "Requires just one argument, the DN to search for.");
        }

        Group[] groups = this.groupAuthz.getGroups();
        if (groups == null || groups.length == 0) {
            throw new Exception(
                    "The authz module has no groups configured, DN not found.");
        }

        System.out.println("\nSearching for '" + this.args[0] + "'");

        int numFound = 0;

        final String dn = this.args[0].trim();

        for (int i = 0; i < groups.length; i++) {
            final Group group = groups[i];
            if (group != null
                    && this.findID(dn, group)) {
                
                numFound += 1;

                final String groupName = "group #" + (i+1) +
                            " '" + group.getName() + "'";

                if (numFound > 1) {
                    System.out.println(
                            "Also found in " + groupName + " (NOT in effect)");
                } else {
                    System.out.println("\nFound in " + groupName + "\n");
                    this.printGroup(group);
                    System.out.println("\nHash: " + Hash.hash(dn) + "\n");
                }
            }
        }

        if (numFound == 0) {
            throw new Exception("Not found.");
        }
    }

    public boolean findID(String DN, Group group) {
        
        if (group == null) {
            return false;
        }

        String[] ids = group.getIdentities();
        if (ids == null || ids.length == 0) {
            return false;
        }
        
        for (int i = 0; i < ids.length; i++) {
            if (ids[i] != null && ids[i].trim().length() != 0) {
                if (ids[i].equals(DN)) {
                    return true;
                }
            }
        }
        return false;
    }
}