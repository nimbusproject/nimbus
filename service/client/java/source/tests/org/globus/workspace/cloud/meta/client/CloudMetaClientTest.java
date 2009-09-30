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
import org.globus.workspace.common.print.Print;
import org.globus.workspace.cloud.client.Props;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

public class CloudMetaClientTest extends FileCleanupTestFixture {
    @Test
    public void testRun() throws Throwable {

        File tempDir = this.getTempDir();
        Print print = new Print();
        CloudMetaClient client = new CloudMetaClient(print);

        File clusterFile = new File(tempDir, "cluster.xml");
        TestUtil.writeSampleClusterToFile(clusterFile);

        File deployFile = new File(tempDir, "deploy.xml");
        TestUtil.writeSampleDeployToFile(deployFile);

        File historyDir = new File(tempDir, "history");
        if (!historyDir.mkdir()) {
            throw new Exception("failed to create history dir");
        }

        File cloudDir = new File(tempDir, "clouds");
        if (!cloudDir.mkdir()) {
            throw new Exception("failed to create cloud dir");
        }

        writeACloudFile(cloudDir, "cloudA");
        writeACloudFile(cloudDir, "cloudB");


        AllArgs args = new AllArgs(print);

        final String DASHDASH = "--";

        String[] argv = new String[] {
            DASHDASH+Opts.HISTORY_DIR_OPT_STRING, historyDir.getAbsolutePath(),
            DASHDASH+Opts.CLOUDDIR_OPT_STRING, cloudDir.getAbsolutePath(),
            DASHDASH+Opts.RUN_OPT_STRING,
            DASHDASH+Opts.CLUSTER_OPT_STRING,clusterFile.getAbsolutePath(),
            DASHDASH+Opts.DEPLOY_OPT_STRING, deployFile.getAbsolutePath(),
            DASHDASH+Opts.HOURS_OPT_STRING, ".5"
        };

        args.intakeCmdlineOptions(argv);
        client.run(args);

    }

    private void writeACloudFile(File cloudDir, String cloudName)
        throws IOException {
        Properties props = getCloudProps();

        File cloudFile = new File(cloudDir, cloudName);
        TestUtil.writePropertiesToFile(props, cloudFile);
    }

    private Properties getCloudProps() throws IOException {
        Properties props = new Properties();
        props.put(Props.KEY_FACTORY_HOSTPORT, "tp-vm1.ci.uchicago.edu:8445");
        props.put(Props.KEY_FACTORY_IDENTITY, "/O=Grid/OU=GlobusTest/OU=" +
            "simple-workspace-ca/CN=host/tp-vm1.ci.uchicago.edu");
        props.put(Props.KEY_GRIDFTP_HOSTPORT, "tp-vm1.ci.uchicago.edu:2811");
        props.put(Props.KEY_GRIDFTP_IDENTITY, "/O=Grid/OU=GlobusTest/OU=" +
            "simple-workspace-ca/CN=host/tp-vm1.ci.uchicago.edu");
        props.put(Props.KEY_SSHFILE, "~/.ssh/id_rsa.pub");
        return props;
    }
}
