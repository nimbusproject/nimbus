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

package org.nimbustools.auto_config.confmgr;

import java.io.File;

/**
 * Assumes no one altered the "networksDir" setting in the spring configs...
 */
public class GetNetworkDirectory extends AlterCommon {

    public String getNetworkDirectory() throws Exception {
        GenericConfUtil util = this.findGenericConfUtil();
        final String path = util.getWorkspaceServiceConfDirAbsolutePath();
        final File confdir = new File(path);
        final File netDir = new File(confdir, "network-pools");

        final String netDirPath = netDir.getAbsolutePath();
        if (!netDir.exists()) {
            throw new Exception(
                    "Directory does not exist: " + netDirPath);
        }
        if (!netDir.canWrite()) {
            throw new Exception(
                    "Directory is not writable: " + netDirPath);
        }
        return netDirPath;
    }

    public static void mainImpl() throws Exception {
        System.out.println(new GetNetworkDirectory().getNetworkDirectory());
    }

    public static void main(String[] args) {
        try {
            mainImpl();
        } catch (Throwable t) {
            System.err.println("Problem: " + t.getMessage());
            System.exit(1);
        }
    }
}