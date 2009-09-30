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

package org.nimbustools.auto_config;

import org.globus.security.gridmap.GridMap;

import java.io.File;

public class GridMapAdd {

    public void add(String path,
                    String DN,
                    String user) throws Exception {

        if (DN == null || DN.trim().length() == 0) {
            throw new Exception("no DN");
        }

        if (user == null || user.trim().length() == 0) {
            throw new Exception("no user to map to");
        }

        final String dn = DN.trim();
        final String userMapping = user.trim();

        System.out.println("Adding DN '" + dn + "'");
        System.out.println(" ... with user mapping '" + userMapping + "'");
        System.out.println(" ... to grid-mapfile '" + path + "'");

        final GridMap gridmap = new GridMap();
        gridmap.load(path);

        boolean foundOne = false;
        final String[] userids = gridmap.getUserIDs(dn);
        if (userids != null && userids.length > 0) {
            for (int i = 0; i < userids.length; i++) {
                String userid = userids[i];
                if (userid == null) {
                    throw new Exception("invalid grid-mapfile, user mapping " +
                            "is present but null?");
                }
                if (!userid.trim().equalsIgnoreCase(userMapping)) {
                    throw new Exception("There is a username mapping " +
                        "present already for this DN that does not match " +
                        "your requested userid '" + userMapping + "', exiting");
                }
                foundOne = true;
            }
        }

        if (foundOne) {
            System.out.println(
                    "Nothing to do, the DN is already added to '" + path + "'");
            return;
        }

        File file = new File(path);
        if (!file.canWrite()) {
            throw new Exception(
                    "This file is not writable: '" + path + "'");
        }

        final String newline = "\"" + dn + "\" " + userMapping;

        final TextFile textFile = new TextFile(path);
        textFile.add(newline);
        textFile.writeFile(file);
    }

    public static void mainImpl(String[] args) throws Exception {
        if (args == null || args.length != 3) {
            throw new Exception(
                    "You need to supply three and only three arguments:"
                  + "\n  1 - path to existing grid-mapfile"
                  + "\n  2 - DN"
                  + "\n  3 - user to map to");
        }

        new GridMapAdd().add(args[0], args[1], args[2]);
    }

    public static void main(String[] args) {
        try {
            mainImpl(args);
        } catch (Throwable t) {
            System.err.println("Problem: " + t.getMessage());
            System.exit(1);
        }
    }
}
