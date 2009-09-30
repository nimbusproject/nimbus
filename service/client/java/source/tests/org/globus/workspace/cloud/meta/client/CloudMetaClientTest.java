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
import org.junit.Ignore;
import org.globus.workspace.cloud.client.Props;
import org.globus.bootstrap.Bootstrap;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

public class CloudMetaClientTest extends FileCleanupTestFixture {

    @Test //@Ignore
    public void testRun() throws Throwable {

        // this test is really more for diagnostics and manually
        // testing right now. For it to work, you have to manually
        // configure GLOBUS_LOCATION and X509_CERT_DIR,
        // your grid proxy has to be initialized, you've got to
        // have a simple-cloud image in your repository, and
        // you've got to be wearing a red shirt.


        File tempDir = this.getTempDir();

        File propFile = new File(tempDir, "cloud.properties");
        writeSensibleUserPropsToFile(propFile);

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

        // okay these are both the same cloud right now. but it's the same
        // in principle, right? Right?!
        // Ok, no.
        writeACloudFile(cloudDir, "cloudA");
        writeACloudFile(cloudDir, "cloudB");


        final String DASHDASH = "--";

        String[] argv = new String[] {
            CloudMetaClient.class.getName(),
            DASHDASH+Opts.PROPFILE_OPT_STRING, propFile.getAbsolutePath(),
            DASHDASH+Opts.HISTORY_DIR_OPT_STRING, historyDir.getAbsolutePath(),
            DASHDASH+Opts.CLOUD_DIR_OPT_STRING, cloudDir.getAbsolutePath(),
            DASHDASH+Opts.RUN_OPT_STRING,
            DASHDASH+Opts.CLUSTER_OPT_STRING,clusterFile.getAbsolutePath(),
            DASHDASH+Opts.DEPLOY_OPT_STRING, deployFile.getAbsolutePath(),
            DASHDASH+Opts.HOURS_OPT_STRING, ".25"
        };

        Bootstrap.main(argv);
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
        return props;
    }

    private void writeSensibleUserPropsToFile(File propFile) throws IOException {
        Properties props = new Properties();
        props.put(Props.KEY_SSHFILE, "~/.ssh/id_rsa.pub");
        props.put(Props.KEY_BROKER_URL,
            "https://tp-vm1.ci.uchicago.edu:8445/wsrf/services/NimbusContextBroker");
        props.put(Props.KEY_BROKER_IDENTITY,
            "/O=Grid/OU=GlobusTest/OU=simple-workspace-ca/CN=host/tp-vm1.ci.uchicago.edu");

        TestUtil.writePropertiesToFile(props, propFile);
    }
}
