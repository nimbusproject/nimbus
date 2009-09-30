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

package org.globus.workspace.cloud.meta.client;

import org.nimbustools.ctxbroker.generated.gt4_0.description.BrokerContactType;
import org.globus.workspace.cloud.client.tasks.RunTask;
import org.globus.workspace.cloud.client.util.ExecuteUtil;
import org.globus.workspace.cloud.client.util.HistoryUtil;
import org.globus.workspace.client_core.ExecutionProblem;
import org.globus.workspace.common.print.Print;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.io.File;


/**
 * The set of deployments going to a particular cloud
 */
public class CloudDeployment {

    private Cloud cloud;
    private ArrayList<MemberDeployment> members;
    private boolean hasCtx;

    public Cloud getCloud() {
        return cloud;
    }

    public List<MemberDeployment> getMembers() {
        return Collections.unmodifiableList(members);
    }

    public boolean hasContextualization() {
        return hasCtx;
    }

    public CloudDeployment(Cloud cloud) {

        if (cloud == null) {
            throw new IllegalArgumentException("cloud may not be null");
        }

        this.cloud = cloud;
        this.members = new ArrayList<MemberDeployment>();
        this.hasCtx = false;
    }

    public void addMember(MemberDeployment member) {
        if (member == null) {
            throw new IllegalArgumentException("member may not be null");
        }

        if (!hasCtx && member.getMember().getClusterForUserData() != null) {
            hasCtx = true;
        }

        this.members.add(member);

    }


    public RunTask[] generateRunTasks(BrokerContactType broker,
                                          String dirPath,
                                          String sshKeyPath,
                                          int durationMinutes,
                                          Print print)
        throws ExecutionProblem {

        if (members.isEmpty()) {
            throw new ExecutionProblem("this CloudDeployment has no members," +
                " it cannot be launched");
        }

        final File dir = new File(dirPath);
        if (!(dir.exists() && dir.isDirectory() && dir.canWrite())) {
            throw new IllegalArgumentException("Provided directory must" +
                " exist and be writeable");
        }


        final String eprIdIpDirPath = ExecuteUtil.makeDirectory(dirPath,
            "id-ip-dir", print);

        final String ensembleEprPath = new File(dir,
            HistoryUtil.ENSEMBLE_EPR_FILE_NAME)
            .getAbsolutePath();

        RunTask[] tasks = new RunTask[members.size()];

        for (int i = 0; i < members.size(); i++) {
            MemberDeployment member = members.get(i);

            boolean needsUserdata = (broker != null &&
                member.getMember().getClusterForUserData() != null);

            String memberName = member.getMember().getPrintName();
            if (memberName == null) {
                memberName = HistoryUtil.getMemberName(i+1);
            }

            String eprPath = new File(dir, memberName +"-epr").getAbsolutePath();
            if (member.getInstanceCount() == 1) {
                eprPath += ".xml";
            }

            String metadataPath;
            String deployPath;
            String userdataPath = null;

            try {
                metadataPath = HistoryUtil.writeMetadata(dir,
                    memberName+"-metadata.xml",
                    cloud.generateMetadata(member));

                deployPath = HistoryUtil.writeDeployment(dir,
                    memberName+"-deploy.xml",
                    cloud.generateDeployment(member, durationMinutes));

                if (needsUserdata) {
                    userdataPath = HistoryUtil.writeUserData(dir,
                        memberName+"-userdata.xml",
                        broker,
                        member.getMember().getClusterForUserData());
                }

            } catch (Exception e) {
                throw new ExecutionProblem("Problem writing data files for "+
                    memberName,e);
            }

            tasks[i] = new RunTask(
                eprPath,
                cloud.getWorkspaceFactoryURL(),
                metadataPath,
                deployPath,
                sshKeyPath,
                this.cloud.getPollTime(),
                true,
                memberName,
                this.cloud.getFactoryID(),
                null,
                ensembleEprPath,
                i == 0,   // first task will create the ensemble
                          // it MUST be run first
                true,
                userdataPath,
                eprIdIpDirPath,
                print
                );

        }

        return tasks;
    }


}
