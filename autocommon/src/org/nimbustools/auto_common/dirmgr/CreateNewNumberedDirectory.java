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

package org.nimbustools.auto_common.dirmgr;

import java.io.FilenameFilter;
import java.io.File;
import java.text.NumberFormat;

public class CreateNewNumberedDirectory {

    public static final int NUM_MEMBER_SUFFIX_MIN_CHARACTERS = 2;

    static final NumberFormat memberFormat = NumberFormat.getInstance();
    static {
        memberFormat.setMinimumIntegerDigits(NUM_MEMBER_SUFFIX_MIN_CHARACTERS);
    }

    private static class dirFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            final File test = new File(dir, name);
            return test.isDirectory();
        }
    }

    public String createNewDirectory(String topdirPath,
                                     String prefix) throws Exception {

        if (topdirPath == null) {
            throw new IllegalArgumentException("topdir may not be null");
        }

        if (prefix == null || prefix.trim().length() == 0) {
            throw new IllegalArgumentException("expecting a prefix");
        }

        final File topdirFile = new File(topdirPath);

        if (!topdirFile.exists()) {
            if (!topdirFile.mkdir()) {
                throw new Exception(
                        "Could not create directory: '" + topdirPath + "'");
            }
        }

        if (!topdirFile.isDirectory()) {
            throw new Exception("Not a directory: '" + topdirPath + "'");
        }
        
        final String[] subdirs = topdirFile.list(new dirFilter());
        if (subdirs == null) {
            throw new Exception("Problem examining dir '" +
                    topdirFile.getAbsolutePath() + "'");
        }

        final int prefixLength = prefix.length();

        int highestNumber = 0;
        for (int i = 0; i < subdirs.length; i++) {

            // this gets annoying
            //if (debug != null) {
            //    debug.println("examining history subdir: " + subdirs[i]);
            //}

            if (subdirs[i].startsWith(prefix)) {
                final String intPart = subdirs[i].substring(prefixLength);
                final int number = Integer.parseInt(intPart);
                if (number > highestNumber) {
                    highestNumber = number;
                }
            }
        }

        final int numberToUse = highestNumber + 1;
        final String newdirName = prefix + memberFormat.format(numberToUse);
        final File newdir =
                new File(topdirPath + File.separator + newdirName);

        if (!newdir.mkdir()) {
            throw new Exception("Could not create directory: '" +
                    newdir.getAbsolutePath() + "'");
        }

        return newdir.getAbsolutePath();
    }

    public static void main(String[] args) {

        if (args == null || args.length != 2) {
            System.err.println("Needs these arguments:\n" +
                    "1 - base directory\n" +
                    "2 - prefix");
            System.exit(1);
        }

        try {
            System.out.println(
                new CreateNewNumberedDirectory().createNewDirectory(args[0],
                                                                    args[1]));

        } catch (Exception e) {
            System.err.println(
                    "Problem creating new directory: " + e.getMessage());
            System.exit(1);
        }
    }
}
