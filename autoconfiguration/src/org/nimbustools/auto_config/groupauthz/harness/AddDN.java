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

import org.nimbustools.auto_config.UserQuestions;
import org.nimbustools.auto_config.TextFile;
import org.globus.workspace.groupauthz.Group;

import java.text.NumberFormat;
import java.text.DateFormat;
import java.io.File;
import java.util.Calendar;

public class AddDN extends AddDeleteCommon {

    public AddDN(String confPath, String[] args, boolean debug)
            throws Exception {
        super(confPath, args, debug);
    }
    
    protected String dnToAdd() throws Exception {
        return this.dnInQuestion("add");
    }

    public void run() throws Exception {

        final String dn = this.dnToAdd();
        
        System.out.println("Adding '" + dn + "'\n");

        if (this.scanAll(dn, this.getGroups())) {
            if (this.warn(dn)) {
                System.out.println("");
                this.deleteAllInstances(dn, this.getGroups());
                System.out.println("\n---------------------------------------");
            } else {
                throw new Exception("Goodbye!");
            }
        }

        this.add(dn);
    }

    /**
     * @param dn to print
     * @return true if it is OK to proceed
     * @throws Exception exc
     */
    public boolean warn(String dn) throws Exception {
        System.out.println("Already exists in a policy, here's the report:");
        final String[] args = {dn};
        System.out.println("---------------------------------------");
        new Report(this.confPath,
                   args,
                   this.debug).run();
        System.out.println("---------------------------------------");
        
        System.out.println(
                "\nDelete current settings for this DN and then continue " +
                        "as a fresh addition? y/n");
        return new UserQuestions().getUserYesNo();
    }

    protected int getGroupChoice(Group[] groups) throws Exception {
        
        final NumberFormat memberFormat = NumberFormat.getInstance();
        int numgroups = groups.length;
        int digitlen = 0;
        if (numgroups < 10) {
            digitlen = 1;
        } else if (numgroups < 100) {
            digitlen = 2;
        } else {
            digitlen = 3;
        }

        memberFormat.setMinimumIntegerDigits(digitlen);

        String indent = "   ";
        for (int i = 0; i < digitlen; i++) {
            indent = indent + " ";
        }

        int numValid = 0;

        final StringBuffer promptbuf =
                new StringBuffer("\nPick a group to add this DN to:\n");

        for (int i = 0; i < groups.length; i++) {
            final Group group = groups[i];
            if (group != null) {

                numValid++;

                promptbuf.append("\n[")
                         .append(memberFormat.format(i+1))
                         .append("] - Group: '")
                         .append(group.getName())
                         .append("'");
                promptbuf.append("\n");
                promptbuf.append(
                        ReportAll.policyBrief(
                                group.getRights(), indent));
                promptbuf.append("\n");
            }
        }

        if (numValid == 0) {
            throw new Exception("No valid groups, can not add.");
        }

        promptbuf.append("\nChoose a number: ");

        return new UserQuestions().getInt(promptbuf.toString(), 1, numValid);
    }

    protected void addImpl(String dn,
                           Group group) throws Exception {

        String filePath = group.getIdentitiesFilePath();

        if (filePath == null || filePath.trim().length() == 0) {
            throw new Exception("unexpected, filePath is empty or null");
        }

        final File file = new File(filePath);
        if (!file.exists()) {
            throw new Exception(
                    "unexpected, file does not exist: '" + filePath + "'");
        }

        final TextFile textFile = new TextFile(filePath);

        if (!file.canWrite()) {
            throw new Exception(
                    "This file is not writable: '" + filePath + "'");
        }

        textFile.add(dn);
        textFile.writeFile(file);
    }

    public void add(String dn) throws Exception {

        if (dn == null || dn.trim().length() == 0) {
            throw new Exception("DN was not supplied");
        }
        
        final Group[] groups = this.getGroups();
        if (groups == null || groups.length == 0) {
            throw new Exception("No groups exist, can not add.");
        }

        final int choice = this.getGroupChoice(groups);

        try {
            if (groups[choice-1] == null) {
                throw new Exception("Invalid choice.");
            }
        } catch (Throwable t) {
            // out of bounds, etc., if user gets tricky
            throw new Exception("Invalid choice.");
        }

        final Group chosenGroup = groups[choice-1];
        this.addImpl(dn, chosenGroup);

        System.out.println("\nSUCCESS");
        System.out.println("\nAdded DN: '" + dn + "'");
        System.out.println("To group: '" + chosenGroup.getName() + "'");
        System.out.println("\nGroup policies:");
        System.out.println(ReportAll.policyBrief(chosenGroup.getRights(), ""));
        System.out.println(
            "\nAccess list: '" + chosenGroup.getIdentitiesFilePath() + "'\n");

        final DateFormat format = DateFormat.getDateTimeInstance();
        final String time = format.format(Calendar.getInstance().getTime());

        System.out.println("Time: " + time + "\n");
    }
}
