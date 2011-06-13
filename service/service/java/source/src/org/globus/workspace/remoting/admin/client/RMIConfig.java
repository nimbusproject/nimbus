package org.globus.workspace.remoting.admin.client;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.workspace.remoting.RemotingClient;
import org.nimbustools.api.brain.NimbusHomePathResolver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Properties;

/**
 * Copyright 1999-2010 University of Chicago
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
 *
 * User: rrusnak
 */

/**
 * This class handles the basic configuration for an RMI connection
 */
public class RMIConfig {

    private static final Log logger =
            LogFactory.getLog(RMIConfig.class.getName());

    private static final String PROP_SOCKET_DIR = "socket.dir";

    protected Properties properties;
    private File socketDirectory;
    protected String configPath;

    private String bindingName;


    protected void loadConfig(String bindingDir) throws ParameterProblem, ExecutionProblem {
        if(configPath == null)
            throw new ParameterProblem("Config path is invalid.");

        final File configFile = new File(configPath);
        logger.debug("Loading config file: " + configFile.getAbsolutePath());

        if (!configFile.canRead()) {
            throw new ParameterProblem(
                    "Specified config file path does not exist or is not readable: " +
                            configFile.getAbsolutePath());
        }

        final Properties props = new Properties();
        this.properties = props;
        try {
            FileInputStream inputStream = null;
            try {
                inputStream = new FileInputStream(configFile);
                props.load(inputStream);
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }
            }
        }
        catch (IOException e) {
            logger.debug("Caught error reading config file " + configFile.getAbsolutePath(), e);
            throw new ParameterProblem("Failed to load config file: " +
                    configFile.getAbsolutePath() + ": " + e.getMessage(), e);
        }

        final String sockDir = props.getProperty(PROP_SOCKET_DIR);
        if (sockDir == null) {
            throw new ExecutionProblem("Configuration file is missing "+
                    PROP_SOCKET_DIR + " entry: " + configFile.getAbsolutePath());
        }

        final NimbusHomePathResolver resolver = new NimbusHomePathResolver();
        String path = resolver.resolvePath(sockDir);
        if (path == null) {
            path = sockDir;
        }
        this.socketDirectory = new File(path);

        final String binding = props.getProperty(bindingDir);
        if (binding == null) {
            throw new ExecutionProblem("Configuration file is missing " +
                    bindingDir + " entry: "+
                    configFile.getAbsolutePath());
        }
        this.bindingName = binding;
    }

    protected Remote setupRemoting() throws ExecutionProblem {

        final RemotingClient client = new RemotingClient();
        client.setSocketDirectory(this.socketDirectory);

        try {
            client.initialize();
        }
        catch (RemoteException e) {
            handleRemoteException(e);
        }

        try {
            final Remote remote = client.lookup(this.bindingName);
            logger.debug("Found remote object " + remote.toString());
            return remote;
        }
        catch (RemoteException e) {
            handleRemoteException(e);
            return null;
        }
        catch (NotBoundException e) {
            throw new ExecutionProblem("Failed to bind to object '" +
                    this.bindingName +
                    "'. There may be a configuration problem between the "+
                    "client and service. Error: "+ e.getMessage(), e);
        }
    }

    protected void handleRemoteException(RemoteException e) throws ExecutionProblem {
        throw new ExecutionProblem(
                "Failed to connect to Nimbus service over domain sockets. "+
                "Is the service running?\n\nSocket directory: " +
                this.socketDirectory.getAbsolutePath() + "\n\nError: " +
                e.getMessage(), e);
    }
}
