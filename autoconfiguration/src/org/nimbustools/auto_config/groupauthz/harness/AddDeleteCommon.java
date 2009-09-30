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
import org.nimbustools.auto_config.TextFile;

import java.io.File;

public class AddDeleteCommon extends Action {

    public AddDeleteCommon(String confPath, String[] args, boolean debug)
            throws Exception {
        super(confPath, args, debug);
    }

    protected String dnInQuestion(String action) throws Exception {
        if (this.args.length > 1) {
            throw new Exception(
                    "Requires just one argument, the DN to " + action +
                            "(you may need to quote DN with spaces?).");
        }

        if (this.args.length == 0 || this.args[0].trim().length() == 0) {
            throw new Exception(
                    "Requires just one argument, the DN to " + action + ".");
        }

        return this.args[0].trim();
    }

    protected Group[] getGroups() throws Exception {
        Group[] groups = this.groupAuthz.getGroups();
        if (groups == null || groups.length == 0) {
            throw new Exception("The authz module has no groups " +
                    "configured, can not proceed.");
        }
        return groups;
    }

    protected boolean scanAll(String dn,
                              Group[] groups) throws Exception {
        
        for (int i = 0; i < groups.length; i++) {
            final Group group = groups[i];
            if (group != null && this.findID(dn, group)) {
                return true;
            }
        }
        return false;
    }

    // returns true if something was removed
    protected boolean deleteAllInstances(String dn,
                                         Group[] groups) throws Exception {
        boolean ret = false;
        for (int i = 0; i < groups.length; i++) {
            final Group group = groups[i];
            if (group != null && this.findID(dn, group)) {
                this.deleteDN(dn,
                              group.getIdentitiesFilePath(),
                              group.getName());
                ret = true;
            }
        }
        return ret;
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
                if (line.trim().equals(dn)) {
                    textFile.remove(i);
                    return true;
                }
            }
        }
        return false;
    }

    protected void deleteDN(String dn,
                            String filePath,
                            String groupName) throws Exception {

        if (dn == null || dn.trim().length() == 0) {
            throw new Exception("unexpected, dn is empty or null");
        }
        if (filePath == null || filePath.trim().length() == 0) {
            throw new Exception("unexpected, filePath is empty or null");
        }

        final File file = new File(filePath);
        if (!file.exists()) {
            throw new Exception(
                    "unexpected, file does not exist: '" + filePath + "'");
        }

        final TextFile textFile = new TextFile(filePath);
        if (textFile.isEmpty()) {
            return;
        }

        if (!file.canWrite()) {
            throw new Exception(
                    "This file is not writable: '" + filePath + "'");
        }

        int count = 0;

        // see note for _removeAttempt
        while (_removeAttempt(textFile, dn)) {
            count++;
        }

        if (count > 0) {
            textFile.writeFile(file);
            if (count == 1) {
                System.out.println("Deleted from group '" + groupName + "'");
            } else {
                System.out.println("Deleted " + count +
                        " instances of the DN from group '" + groupName + "'");
            }
            System.out.println(
                    "  - access list: '" + file.getAbsolutePath() + "'");
        }
    }

    protected boolean findID(String DN, Group group) {

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
