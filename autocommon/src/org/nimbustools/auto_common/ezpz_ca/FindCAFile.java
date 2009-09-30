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

package org.nimbustools.auto_common.ezpz_ca;

import java.io.File;

public class FindCAFile {

    public String findCAPubFile(String dir) throws Exception {
        return this.findCAFile(dir, true);
    }

    public String findCAPrivFile(String dir) throws Exception {
        return this.findCAFile(dir, false);
    }

    private String findCAFile(String dir, boolean returnPub) throws Exception {

        final File dirFile = new File(dir);
        if (!dirFile.exists()) {
            throw new Exception("Directory does not exist: '" + dir + "'");
        }

        if (!dirFile.isDirectory()) {
            throw new Exception("Not a directory: '" + dir + "'");
        }

        final String[] files = dirFile.list();
        if (files == null) {
            throw new Exception("Problem examining directory contents of '" +
                    dirFile.getAbsolutePath() + "'");
        }

        if (files.length < 2) {
            throw new Exception("Directory contains less than two files: '" +
                    dirFile.getAbsolutePath() + "'");
        }

        int privkeyCount = 0;
        File privpemFile = null;

        for (int i = 0; i < files.length; i++) {
            final String file = files[i];
            if (file != null) {
                final File oneFile = new File(file);
                if (oneFile.getName().startsWith("private-key-")) {
                    privkeyCount += 1;
                    privpemFile = new File(dir, oneFile.getName());
                }
            }
        }

        if (privkeyCount > 1) {
            throw new Exception("Too many files that start " +
                    "with 'private-key-' in this directory, sorry.");
        }

        if (privkeyCount < 1) {
            throw new Exception("No files that start with 'private-key-' in " +
                    "this directory, can not consume CA certificate");
        }

        if (privpemFile == null) {
            throw new Exception(
                    "(satisfy code warning systems this is non-null)");
        }

        final String privname = privpemFile.getName();
        final String pubname = privname.substring(12);

        final File pubpemFile = new File(privpemFile.getParentFile(), pubname);
        
        if (!pubpemFile.exists()) {
            throw new Exception("Found private pem file '" + privname +
                    "' but not the matching public pem file '" + pubname + "'");
        }

        if (returnPub) {
            return pubpemFile.getAbsolutePath();
        } else {
            return privpemFile.getAbsolutePath();
        }
    }
}
