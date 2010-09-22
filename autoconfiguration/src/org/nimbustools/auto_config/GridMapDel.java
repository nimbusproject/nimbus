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

import java.io.File;

public class GridMapDel {

    public void del(String path,
                    String DN) throws Exception {

        if (DN == null || DN.trim().length() == 0) {
            throw new Exception("no DN");
        }

        final String dn = DN.trim();

        System.out.println("\nExamining grid-mapfile '" + path + "'");

        int count = 0;
        final TextFile textFile = new TextFile(path);

        // see note for _removeAttempt
        while (_removeAttempt(textFile, dn)) {
            count++;
        }

        if (count == 0) {
            System.out.println(
                   "Nothing to do, could not find this DN in the grid-mapfile");
            return;
        }

        if (count == 1) {
            System.out.println("\nDelete that line?");
        } else {
            System.out.println("\nDelete those " + count + " lines?");
        }

        if (!new UserQuestions().getUserYesNo()) {
            System.out.println("\nOK, exiting.");
            return;
        }

        File file = new File(path);
        if (!file.canWrite()) {
            throw new Exception(
                    "This file is not writable: '" + path + "'");
        }

        textFile.writeFile(file);

        System.out.println("\nDeleted.");
    }

    // removing a line changes the arraylist size (TextFile is an arraylist),
    // so to make sure all entries are gone, we need to iterate again from
    // scratch if one is removed.
    // Returns true if there was a removal
    private boolean _removeAttempt(TextFile textFile,
                                   String dn) {

        final int size = textFile.size();
        for (int i = 0; i < size; i++) {
            final String line = (String) textFile.get(i);
            if (line != null && line.trim().length() > 0) {
                if (line.trim().startsWith("\"" + dn)) {
                    System.out.println("\nFOUND GRID-MAP LINE: " + line.trim());
                    textFile.remove(i);
                    return true;
                }
            }
        }
        return false;
    }

    public static void mainImpl(String[] args) throws Exception {
        if (args == null || args.length != 2) {
            throw new Exception(
                    "You need to supply three and only three arguments:"
                  + "\n  1 - path to existing grid-mapfile"
                  + "\n  2 - DN");
        }

        new GridMapDel().del(args[0], args[1]);
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