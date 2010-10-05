/*
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
 */
package org.globus.workspace.remoting.admin.client;

import com.google.gson.Gson;
import org.apache.commons.cli.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.*;
import org.globus.workspace.remoting.RemotingClient;
import org.globus.workspace.remoting.admin.RemoteNodePool;
import org.globus.workspace.scheduler.VmmNode;
import org.globus.workspace.scheduler.defaults.DefaultVmmNode;
import org.nimbustools.api.brain.NimbusHomePathResolver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;


public class AdminClient {

    private static final Log logger =
            LogFactory.getLog(AdminClient.class.getName());


    public static final int EXIT_OK = 0;
    public static final int EXIT_PARAMETER_PROBLEM = 1;
    public static final int EXIT_EXECUTION_PROBLEM = 2;
    public static final int EXIT_UNKNOWN_PROBLEM = 3;

    public static final String PROP_SOCKET_DIR = "socket.dir";
    public static final String PROP_RMI_BINDING_NODEPOOL_DIR = "rmi.binding.nodepool";
    private static final String PROP_DEFAULT_MEMORY = "node.memory.default";
    private static final String PROP_DEFAULT_NETWORKS = "node.networks.default";
    private static final String PROP_DEFAULT_POOL = "node.pool.default";

    private final Gson gson = new Gson();

    private AdminAction action;
    private List<String> hosts;
    private int nodeMemory;
    private boolean nodeMemoryConfigured;
    private List<String> nodeNetworks;
    private String nodePool;
    private String configPath;
    private boolean debug;
    private File socketDirectory;
    private RemotingClient remotingClient;
    private String nodePoolBindingName;
    private RemoteNodePool remoteNodePool;

    public static void main(String args[]) {

        // early check for debug options
        boolean isDebug = false;
        final String debugFlag = "--" + Opts.DEBUG_LONG;
        for (String arg : args) {
            if (debugFlag.equals(arg)) {
                isDebug = true;
                break;
            }
        }

        if (isDebug) {
            System.out.println("sandwiches");

            //TODO uggggh something is configuring log4j first
            BasicConfigurator.resetConfiguration();

            final PatternLayout layout = new PatternLayout("%C{1}:%L - %m%n");
            BasicConfigurator.configure(new ConsoleAppender(layout));

            logger.info("Debug mode enabled");
        }

        Throwable anyError = null;
        ParameterProblem paramError = null;
        ExecutionProblem execError = null;
        int ret = EXIT_OK;
        try {
            final AdminClient adminClient = new AdminClient();
            adminClient.run(args);

        } catch (ParameterProblem e) {
            paramError = e;
            anyError = e;
            ret = EXIT_PARAMETER_PROBLEM;
        } catch (ExecutionProblem e) {
            execError = e;
            anyError = e;
            ret = EXIT_EXECUTION_PROBLEM;
        } catch (Throwable t) {
            anyError = t;
            ret = EXIT_UNKNOWN_PROBLEM;
        }

        if (anyError == null) {
            System.exit(ret);
        }

        if (paramError != null) {
            System.err.println("Parameter Problem: " + paramError.getMessage());
            System.err.println("See --help");

        } else if (execError != null) {
            System.err.println("Execution Problem: " + execError.getMessage());
        } else {
            System.err.println("An unexpected error was encountered. Please report this!");
            System.err.println(anyError.getMessage());
            System.err.println();
            System.err.println("Stack trace:");
            anyError.printStackTrace(System.err);
        }

        System.exit(ret);

    }

    public void run(String[] args)
            throws ExecutionProblem, ParameterProblem {

        this.loadArgs(args);

        if (this.action == AdminAction.Help) {
            printHelp();
            return;
        }

        this.loadConfig(this.configPath);
        this.setupRemoting();

        switch (this.action) {
            case AddNodes:
                run_addNodes();
                break;
            case ListNodes:
                run_listNodes();
                break;
            case RemoveNodes:
                run_removeNodes();
                break;
            case UpdateNodes:
                run_updateNodes();
                break;
        }
    }


    private void run_addNodes() throws ParameterProblem, ExecutionProblem {
        if (!this.nodeMemoryConfigured) {
            throw new ParameterProblem(
                    "Node max memory must be specified as an argument ("+
                            Opts.MEMORY_LONG +") or config value");
        }

        if (this.nodeNetworks == null) {
            throw new ParameterProblem(
                    "Node network associations must be specified as an argument ("+
                            Opts.NETWORKS_LONG +") or config value");
        }

        if (this.nodePool == null) {
            throw new ParameterProblem(
                    "Node pool name must be specified as an argument ("+
                            Opts.POOL_LONG +") or config value");
        }

        final List<VmmNode> nodes = new ArrayList<VmmNode>(this.hosts.size());
        for (String hostname : this.hosts) {
            nodes.add(new DefaultVmmNode(hostname, this.nodePool,
                    this.nodeMemory, this.nodeNetworks, true));
        }
        try {
            this.remoteNodePool.addNodes(gson.toJson(nodes));
        } catch (IOException e) {
            handleRemoteException(e);
        }

        reportNodes(nodes.toArray(new VmmNode[nodes.size()]));
    }

    private void run_listNodes() throws ExecutionProblem {
        try {
            final String nodesJson = this.remoteNodePool.listNodes();
            final VmmNode[] nodes = this.gson.fromJson(nodesJson, DefaultVmmNode[].class);
            this.reportNodes(nodes);
        } catch (IOException e) {
            handleRemoteException(e);
        }
    }

    private void run_removeNodes() throws ExecutionProblem {
        try {
            final String[] hostnames = this.hosts.toArray(new String[this.hosts.size()]);
            this.remoteNodePool.removeNodes(hostnames);
        } catch (IOException e) {
            handleRemoteException(e);
        }
    }

    private void run_updateNodes() throws ExecutionProblem {

        //TODO

        final List<VmmNode> nodes = new ArrayList<VmmNode>(this.hosts.size());
        for (String hostname : this.hosts) {
            nodes.add(new DefaultVmmNode(hostname, this.nodePool,
                    this.nodeMemory, this.nodeNetworks, true));
        }
        try {
            this.remoteNodePool.updateNodes(gson.toJson(nodes));
        } catch (IOException e) {
            handleRemoteException(e);
        }
    }

    private void handleRemoteException(Throwable t) throws ExecutionProblem {
        throw new ExecutionProblem("Failed to communicate with Nimbus service. "+
                "Is it running? Error: " + t.getMessage(), t);
    }

    private void reportNodes(VmmNode[] nodes) {
        StringBuilder sb = new StringBuilder();
        for (VmmNode node : nodes) {
            for (String network : node.getNetworkAssociations()) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(network);
            }
            if (sb.length() == 0) {
                sb.append("*");
            }

            System.out.println("Host:\t" + node.getHostname());
            System.out.println("Pool:\t" + node.getPoolName());
            System.out.println("Memory:\t" + node.getMemory());
            System.out.println("Networks:\t" + sb.toString());
            System.out.println();

            sb.delete(0, sb.length());
        }
    }

    private void setupRemoting() throws ExecutionProblem {
        final RemotingClient client = new RemotingClient();
        client.setSocketDirectory(this.socketDirectory);

        try {
            client.initialize();
        } catch (RemoteException e) {
            handleRemoteException(e);
        }

        this.remotingClient = client;

        try {
            this.remoteNodePool = (RemoteNodePool) client.lookup(this.nodePoolBindingName);
        } catch (RemoteException e) {
            handleRemoteException(e);
        } catch (NotBoundException e) {
            throw new ExecutionProblem("Failed to bind to object '" +
                    this.nodePoolBindingName +
                    "'. There may be a configuration problem between the "+
                    "client and service. Error: "+ e.getMessage(), e);
        }
    }

    private void handleRemoteException(RemoteException e) throws ExecutionProblem {
        throw new ExecutionProblem("Failed to connect to remoting socket. "+
                "Is the service running? Socket directory: '" +
                this.socketDirectory.getAbsolutePath() + "'. Error: " +
                e.getMessage(), e);
    }

    private void loadConfig(String configPath)
            throws ParameterProblem, ExecutionProblem {
        if (configPath == null) {
            throw new ParameterProblem("Config path is invalid");
        }
        final File configFile = new File(configPath);

        if (!configFile.canRead()) {
            throw new ParameterProblem(
                    "Specified config file path does not exist or is not readable: " +
                            configFile.getAbsolutePath());
        }

        final Properties props = new Properties();
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
        } catch (IOException e) {
            throw new ParameterProblem("Failed to load config file: " +
                    configFile.getAbsolutePath() + ": " + e.getMessage(), e);
        }

        final String sockDir = props.getProperty(PROP_SOCKET_DIR);
        if (sockDir == null) {
            throw new ExecutionProblem("Configuration file is missing "+
                    PROP_SOCKET_DIR + " entry: " + configFile.getAbsolutePath());
        }

        final NimbusHomePathResolver resolver = new NimbusHomePathResolver();
         this.socketDirectory = new File(resolver.resolvePath(sockDir));

        final String nodePoolBinding = props.getProperty(PROP_RMI_BINDING_NODEPOOL_DIR);
        if (nodePoolBinding == null) {
            throw new ExecutionProblem("Configuration file is missing " +
                    PROP_RMI_BINDING_NODEPOOL_DIR + " entry: "+
                    configFile.getAbsolutePath());
        }
        this.nodePoolBindingName = nodePoolBinding;

        if (!this.nodeMemoryConfigured) {
            final String memString = props.getProperty(PROP_DEFAULT_MEMORY);
            if (memString != null) {
                this.nodeMemory = parseMemory(memString);
                this.nodeMemoryConfigured = true;
            }
        }

        if (this.nodeNetworks == null) {
            final String networks = props.getProperty(PROP_DEFAULT_NETWORKS);
            if (networks != null) {
                this.nodeNetworks = parseNetworks(networks);
            }
        }

        if (this.nodePool == null) {
            // if missing or invalid, error will come later if this value is actually needed
            this.nodePool = props.getProperty(PROP_DEFAULT_POOL);
        }
    }

    private void loadArgs(String[] args) throws ParameterProblem {

        logger.debug("Parsing command line arguments");
        final CommandLineParser parser = new PosixParser();

        final Opts opts = new Opts();
        final CommandLine line;
        try {
            line = parser.parse(opts.getOptions(), args);
        } catch (ParseException e) {
            throw new ParameterProblem(e.getMessage(), e);
        }

        // figure action first
        AdminAction theAction = null;
        for (AdminAction a : AdminAction.values()) {
            if (line.hasOption(a.option())) {
                if (theAction == null) {
                    theAction = a;
                } else {
                    throw new ParameterProblem("You may only specify a single action");
                }
            }
        }

        if (theAction == null) {
            throw new ParameterProblem("You must specify an action");
        }

        this.action = theAction;
        logger.debug("Action: " + theAction);


        // short circuit for --help arg
        if (theAction == AdminAction.Help) {
            return;
        }

        // then action-specific arguments
        if (theAction == AdminAction.AddNodes || theAction == AdminAction.UpdateNodes) {
            this.hosts = parseHosts(line.getOptionValue(theAction.option()));
            
            if (line.hasOption(Opts.MEMORY)) {
                final String memString = line.getOptionValue(Opts.MEMORY);
                if (memString == null || memString.trim().length() == 0) {
                    throw new ParameterProblem("Node memory value is empty");
                }
                this.nodeMemory = parseMemory(memString);
                this.nodeMemoryConfigured = true;
            }

            if (line.hasOption(Opts.NETWORKS)) {
                this.nodeNetworks = parseNetworks(line.getOptionValue(Opts.NETWORKS));
            }

            if (line.hasOption(Opts.POOL)) {
                String pool = line.getOptionValue(Opts.POOL);
                if (pool == null || pool.trim().length() == 0) {
                    throw new ParameterProblem("Node pool value is empty");
                }
                this.nodePool = pool.trim();
            }
        } else if (theAction == AdminAction.RemoveNodes) {
            this.hosts = parseHosts(line.getOptionValue(theAction.option()));
        } else if (theAction == AdminAction.ListNodes) {
            final String hostArg = line.getOptionValue(AdminAction.ListNodes.option());
            if (hostArg != null) {
                this.hosts = parseHosts(hostArg);
            }
        }

        //finally everything else
        if (!line.hasOption(Opts.CONFIG)) {
            throw new ParameterProblem(Opts.CONFIG_LONG + " option is required");
        }
        String config = line.getOptionValue(Opts.CONFIG);
        if (config == null || config.trim().length() == 0) {
            throw new ParameterProblem("Config file path is invalid");
        }
        this.configPath = config.trim();

        this.debug = line.hasOption(Opts.DEBUG_LONG);

        final List leftovers = line.getArgList();
		if (leftovers != null && !leftovers.isEmpty()) {
			throw new ParameterProblem("There are unrecognized arguments, check -h to make " +
					"sure you are doing the intended thing: " + leftovers.toString());
		}

    }

    private int parseMemory(String memoryString) throws ParameterProblem {
        final int memory;
        try {
            memory = Integer.valueOf(memoryString.trim());
        } catch (NumberFormatException e) {
            throw new ParameterProblem("Node memory value must be numeric");
        }
        if (memory < 0) {
            throw new ParameterProblem("Node memory value must be non-negative");
        }
        return memory;
    }

    public static List<String> parseNetworks(String networkString)
            throws ParameterProblem {
        if (networkString == null) {
            throw new ParameterProblem("Network list is invalid");
        }

        final String[] networkArray = networkString.trim().split("\\s*,\\s*");
        if (networkArray.length == 0) {
            throw new ParameterProblem("Network list is empty");
        }

        final List<String> networks = new ArrayList<String>(networkArray.length);
        for (final String network : networkArray) {
            //validation?
            networks.add(network);
        }
        return networks;
    }

    public static List<String> parseHosts(String hostString)
            throws ParameterProblem {
        if (hostString == null) {
            throw new ParameterProblem("Hosts list is invalid");
        }

        final String[] hostArray = hostString.trim().split("\\s*,\\s*");
        if (hostArray.length == 0) {
            throw new ParameterProblem("Hosts list is empty");
        }

        final List<String> hosts = new ArrayList<String>(hostArray.length);
        for (final String host : hostArray) {
            if (!host.matches("^[\\w-\\.]+$")) {
                throw new ParameterProblem("Invalid node hostname specified: " + host);
            }
            hosts.add(host);
        }
        return hosts;
    }

    private static void printHelp() {
        // TODO
        System.out.println("TODO print help text");

    }
}

enum AdminAction {

    AddNodes(Opts.ADD_NODES),
    ListNodes(Opts.LIST_NODES),
    RemoveNodes(Opts.REMOVE_NODES),
    UpdateNodes(Opts.UPDATE_NODES),
    Help(Opts.HELP);

    private final String option;

    AdminAction(String option) {
        this.option = option;
    }

    public String option() {
        return option;
    }
}

