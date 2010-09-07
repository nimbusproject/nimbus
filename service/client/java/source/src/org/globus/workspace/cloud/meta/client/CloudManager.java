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

import org.globus.workspace.client_core.ParameterProblem;

import java.util.List;
import java.util.Properties;
import java.util.ArrayList;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;

public class CloudManager {

    static final String FILE_SUFFIX = ".properties";

    final private File configDirectory;
    final private ArrayList<Cloud> clouds;

    private Properties defaultProps;

    public CloudManager(String configDirPath) throws ParameterProblem {

        if (configDirPath == null) {
            throw new IllegalArgumentException("configDirPath cannot be null");
        }

        this.configDirectory = new File(configDirPath);

        if (!this.configDirectory.isDirectory()) {
            throw new ParameterProblem("specified cloud configuration"+
                "path must be a directory.");
        }

        this.clouds = new ArrayList<Cloud>();

        this.defaultProps = null;
    }

    public synchronized Cloud getCloudByName(String name) throws ParameterProblem {

        if (name == null) {
            throw new IllegalArgumentException("name cannot be null");
        }

        for (Cloud cloud : this.clouds) {
            if (cloud.getName().equals(name)) {
                return cloud;
            }
        }

        if (defaultProps == null) {
            try {
                loadDefaultProperties();
            } catch (IOException e) {
                throw new ParameterProblem("failed to load default properties", e);
            }
        }

        Cloud cloud;
        try {
            cloud = loadFromProps(name);
        } catch (IOException e) {
            throw new ParameterProblem("Error reading configuration file for "+
                "cloud '"+name+"'",e);
        }

        this.clouds.add(cloud);
        return cloud;
    }

    private void loadDefaultProperties() throws IOException {
        defaultProps = Cloud.loadDefaultProps();
    }

    private Cloud loadFromProps(String name) throws ParameterProblem, IOException {

        File f = new File(configDirectory, name+FILE_SUFFIX);

        if (!f.exists()) {
            throw new ParameterProblem("Could not find configuration for "+
                "cloud '"+name+"'. Path: "+f.getAbsolutePath());
        }

        Properties props = new Properties(defaultProps);
        InputStream is = null;
        try {
            is = new FileInputStream(f);
            props.load(is);

            return Cloud.createFromProps(name, props);

        } finally {
            if (is != null) {
                is.close();
            }
        }
    }


}
