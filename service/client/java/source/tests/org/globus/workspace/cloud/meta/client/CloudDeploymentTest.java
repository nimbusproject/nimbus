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

import org.junit.Test;
import static org.junit.Assert.*;

import org.globus.workspace.cloud.client.Props;
import org.globus.workspace.cloud.client.tasks.RunTask;
import org.globus.workspace.cloud.client.cluster.ClusterUtil;
import org.globus.workspace.cloud.client.cluster.ClusterMember;
import org.globus.workspace.common.print.Print;
import org.nimbustools.ctxbroker.generated.gt4_0.description.BrokerContactType;
import org.nimbustools.ctxbroker.generated.gt4_0.description.Cloudcluster_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.description.Clouddeploy_Type;

import java.util.Properties;

public class CloudDeploymentTest extends FileCleanupTestFixture {

    @Test
    public void testGenerateRunTasks() throws Exception {
        Cloud cloud = getACloud();
        CloudDeployment deployment = new CloudDeployment(cloud);

        for (MemberDeployment mem : getMemberDeployments()) {
            deployment.addMember(mem);
        }

        BrokerContactType broker = new BrokerContactType();

        final RunTask[] runTasks = deployment.generateRunTasks(broker,
            this.getTempDir().getAbsolutePath(),
            "/home/david/.ssh/id_rsa.pub",
            60, new Print());

        assertEquals(deployment.getMembers().size(),  runTasks.length);

    }
    
    private MemberDeployment[] getMemberDeployments() throws Exception {
        final Cloudcluster_Type cluster = TestUtil.getSampleCluster();

        final ClusterMember[] members =
            ClusterUtil.getClusterMembers(cluster, "priv", "pub", new Print());

        final MemberDeployment[] deploys = new MemberDeployment[members.length];
        for (int i = 0; i < deploys.length; i++) {
            deploys[i] = new MemberDeployment(members[i], new Clouddeploy_Type());
        }

        return deploys;

    }

    private Cloud getACloud() throws Exception {
        Properties props = Cloud.loadDefaultProps();

        props.put(Props.KEY_FACTORY_HOSTPORT, "tp-vm1.ci.uchicago.edu:8445");
        props.put(Props.KEY_FACTORY_IDENTITY, "/O=Grid/OU=GlobusTest/OU=" +
            "simple-workspace-ca/CN=host/tp-vm1.ci.uchicago.edu");
        props.put(Props.KEY_XFER_HOSTPORT, "tp-vm1.ci.uchicago.edu:2811");
        props.put(Props.KEY_GRIDFTP_IDENTITY, "/O=Grid/OU=GlobusTest/OU=" +
            "simple-workspace-ca/CN=host/tp-vm1.ci.uchicago.edu");

        return Cloud.createFromProps("sandwich",props);
    }

}
