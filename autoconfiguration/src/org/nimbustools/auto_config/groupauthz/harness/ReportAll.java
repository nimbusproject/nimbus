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
import org.globus.workspace.groupauthz.GroupRights;

import java.util.ArrayList;

public class ReportAll extends Action {

    public ReportAll(String confPath, String[] args, boolean debug) throws Exception {
        super(confPath, args, debug);
    }

    public void run() throws Exception {

        Group[] groups = this.groupAuthz.getGroups();
        if (groups == null || groups.length == 0) {
            System.out.println("The authz module has no groups configured, " +
                    "nothing to examine.");

            return; // *** EARLY RETURN ***
        }

        final ArrayList seenDNs = new ArrayList();
        
        for (int i = 0; i < groups.length; i++) {
            final Group group = groups[i];
            if (group != null) {
                System.out.println(
                    "\n------------------------------------------------\n");
                this.printGroup(group);
                System.out.println("");
                this.printIDs(group, seenDNs);
            }
        }
        System.out.println(
                    "\n------------------------------------------------\n");

    }

    public void printGroup(Group group) {

        if (group == null) {
            return;
        }

        final GroupRights rights = group.getRights();
        if (rights == null) {
            return;
        }

        System.out.println("GROUP: '" + group.getName() + "'");
        System.out.println(policyBrief(rights, "  "));
    }

    public static String policyBrief(GroupRights rights,
                                     String indent) {
        
        if (rights == null) {
            return null;
        }

        final StringBuffer buf = new StringBuffer();

        final long allMax = rights.getMaxElapsedReservedMinutes();
        String allMaxStr = allMax + " minutes";
        if (allMax == 0) {
            allMaxStr = "unlimited";
        }
        buf.append(indent)
           .append("- All-time max allowed usage: ")
           .append(allMaxStr);


        final long maxSimul = rights.getMaxReservedMinutes();
        String maxSimulStr = maxSimul + " minutes";
        if (maxSimul == 0) {
            maxSimulStr = "unlimited";
        }
        buf.append("\n")
           .append(indent)
           .append("- Max simultaneous reserved: ")
           .append(maxSimulStr);

        final long maxSimulVMs = rights.getMaxWorkspaceNumber();
        String maxSimulVMsStr = maxSimulVMs + "";
        if (maxSimulVMs == 0) {
            maxSimulVMsStr = "unlimited";
        }
        buf.append("\n")
           .append(indent)
           .append("- Max simultaneous VMs: ")
           .append(maxSimulVMsStr);


        final long maxGroupSize = rights.getMaxWorkspacesInGroup();
        String maxGroupSizeStr = maxGroupSize + "";
        if (maxGroupSize == 0) {
            maxGroupSizeStr = "unlimited";
        }
        buf.append("\n")
           .append(indent)
           .append("- Max VMs in one group request: ")
           .append(maxGroupSizeStr);

        return buf.toString();
    }

    public void printIDs(Group group, ArrayList seenDNs) throws Exception {
        if (group == null || seenDNs == null) {
            System.err.println("illegal, null arg");
            return;
        }
        String[] ids = group.getIdentities();
        if (ids == null || ids.length == 0) {
            System.out.println("[NO GROUP MEMBERS]");
            return;
        }

        for (int i = 0; i < ids.length; i++) {
            if (ids[i] != null) {
                final String dn = ids[i].trim();
                if (dn.length() != 0) {

                    if (seenDNs.contains(dn)) {
                        System.out.println(
                                "[[ *** duplicate, not in effect ]] \"" + dn + "\"");
                    } else {
                        System.out.println(
                                "[[ " + Hash.hash(dn) + " ]] \"" + dn + "\"");
                        seenDNs.add(dn);
                    }
                }
            }
        }
    }
}
