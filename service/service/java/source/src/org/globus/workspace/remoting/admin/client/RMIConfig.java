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
 */

package org.globus.workspace.remoting.admin.client;

import com.google.gson.Gson;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.varia.NullAppender;
import org.globus.workspace.remoting.RemotingClient;
import org.nimbustools.api.brain.NimbusHomePathResolver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Properties;

/**
 * This class handles the basic configuration for an RMI connection
 */
public class RMIConfig {

    protected static final Log logger =
            LogFactory.getLog(RMIConfig.class.getName());

    public static final int EXIT_OK = 0;
    public static final int EXIT_PARAMETER_PROBLEM = 1;
    public static final int EXIT_EXECUTION_PROBLEM = 2;
    public static final int EXIT_UNKNOWN_PROBLEM = 3;

    private static final String PROP_SOCKET_DIR = "socket.dir";

    protected final Gson gson = new Gson();
    protected Reporter reporter;
    protected OutputStream outStream;
    protected Properties properties;
    protected String configPath;

    private File socketDirectory;
    private String bindingName;


    protected void setupDebug(String args[]) {
        boolean isDebug = false;
        final String debugFlag = "--" + Opts.DEBUG_LONG;
        for (String arg : args) {
            if (debugFlag.equals(arg)) {
                isDebug = true;
                break;
            }
        }

        if (isDebug) {

            final PatternLayout layout = new PatternLayout("%C{1}:%L - %m%n");
            final ConsoleAppender consoleAppender = new ConsoleAppender(layout, "System.err");
            BasicConfigurator.configure(consoleAppender);

            logger.info("Debug mode enabled");
        }
        else {
            BasicConfigurator.configure(new NullAppender());
        }
    }

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

    protected static String[] parseFields(String fieldsString, AdminEnum action)
            throws ParameterProblem {
        final String[] fieldsArray = fieldsString.trim().split("\\s*,\\s*");
        if (fieldsArray.length == 0) {
            throw new ParameterProblem("Report fields list is empty");
        }

        for (String field : fieldsArray) {
            boolean found = false;
            for (String actionField : action.fields()) {
                if (field.equals(actionField)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new ParameterProblem("Report field '"+ field +
                        "' is not allowed for this action. Allowed fields are: " +
                        csvString(action.fields()));
            }
        }

        return fieldsArray;
    }

    private static String csvString(String[] fields) {
        final StringBuilder sb = new StringBuilder();
        for (String f : fields) {
            if (sb.length() != 0) {
                sb.append(", ");
            }
            sb.append(f);
        }
        return sb.toString();
    }

    protected void handleRemoteException(RemoteException e) throws ExecutionProblem {
        throw new ExecutionProblem(
                "Failed to connect to Nimbus service over domain sockets. "+
                "Is the service running?\n\nSocket directory: " +
                this.socketDirectory.getAbsolutePath() + "\n\nError: " +
                e.getMessage(), e);
    }
}
