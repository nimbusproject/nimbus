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
import org.globus.workspace.cloud.client.Props;
import org.globus.workspace.client_core.ParameterProblem;

import java.io.File;
import java.util.Properties;

import static org.junit.Assert.*;

public class CloudManagerTest extends FileCleanupTestFixture {


    @Test
    public void testBadConfDir() throws Exception {
        ParameterProblem expected = null;

        CloudManager manager;
        try {
            manager = new CloudManager(
                this.getTempDir()+ File.separator+"notarealdir");
        } catch (ParameterProblem e) {
            expected = e;
        }
        assertNotNull(expected);
    }

    @Test
    public void testGetCloudByName() throws Exception {
        CloudManager manager = getManager();

        final String cloudName1 = "sandwich";
        final String cloudName2 = "taco";

        createFakeCloud(cloudName1);
        createFakeCloud(cloudName2);

        Cloud cloud1 = manager.getCloudByName(cloudName1);
        assertNotNull(cloud1);
        assertEquals(cloudName1, cloud1.getName());

        Cloud cloud1Copy = manager.getCloudByName(cloudName1);
        assertSame(cloud1,cloud1Copy);

        Cloud cloud2 = manager.getCloudByName(cloudName2);
        assertNotNull(cloud2);
        assertEquals(cloudName2, cloud2.getName());

        ParameterProblem expected = null;
        try {
            manager.getCloudByName("notarealcloud");
        } catch (ParameterProblem e) {
            expected = e;
        }
        assertNotNull(expected);
    }

    private CloudManager getManager() throws ParameterProblem {
        return new CloudManager(this.getTempDir().getPath());
    }

    private void createFakeCloud(String name) throws Exception {
        File f = new File(this.getTempDir(), name+CloudManager.FILE_SUFFIX);
        if (f.exists()) {
            throw new Exception("fake cloud already exists");
        }

        Properties props = new Properties();
        props.put(Props.KEY_FACTORY_HOSTPORT, "sandwich");
        props.put(Props.KEY_FACTORY_IDENTITY, "sandwich");
        props.put(Props.KEY_GRIDFTP_HOSTPORT, "sandwich");
        props.put(Props.KEY_GRIDFTP_IDENTITY, "sandwich");

        TestUtil.writePropertiesToFile(props, f);
    }

}
