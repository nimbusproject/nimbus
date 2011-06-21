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
import org.apache.log4j.varia.NullAppender;
import org.globus.workspace.remoting.admin.NodeReport;
import org.globus.workspace.remoting.admin.VmmNode;
import org.nimbustools.api.services.admin.RemoteNodeManagement;

import java.io.*;
import java.rmi.RemoteException;
import java.util.*;


public class AdminClient extends RMIConfig {

    private static final Log logger =
            LogFactory.getLog(RMIConfig.class.getName());

    public static final int EXIT_OK = 0;
    public static final int EXIT_PARAMETER_PROBLEM = 1;
    public static final int EXIT_EXECUTION_PROBLEM = 2;
    public static final int EXIT_UNKNOWN_PROBLEM = 3;

    private static final String PROP_RMI_BINDING_NODEMGMT_DIR = "rmi.binding.nodemgmt";
    private static final String PROP_DEFAULT_MEMORY = "node.memory.default";
    private static final String PROP_DEFAULT_NETWORKS = "node.networks.default";
    private static final String PROP_DEFAULT_POOL = "node.pool.default";

    private static final String FIELD_HOSTNAME = "hostname";
    private static final String FIELD_POOL = "pool";
    private static final String FIELD_MEMORY = "memory";
    private static final String FIELD_NETWORKS = "networks";
    private static final String FIELD_ACTIVE = "active";
    private static final String FIELD_IN_USE = "in_use";
    private static final String FIELD_RESULT = "result";


    final static String[] NODE_FIELDS = new String[] {
            FIELD_HOSTNAME, FIELD_POOL, FIELD_MEMORY, FIELD_NETWORKS,
            FIELD_IN_USE, FIELD_ACTIVE };


    final static String[] NODE_REPORT_FIELDS = new String[] {
            FIELD_HOSTNAME, FIELD_POOL, FIELD_MEMORY, FIELD_NETWORKS,
            FIELD_IN_USE, FIELD_ACTIVE, FIELD_RESULT,
    };

    final static String[] NODE_REPORT_FIELDS_SHORT = new String[] {
            FIELD_HOSTNAME, FIELD_RESULT};

    private final Gson gson = new Gson();

    private AdminAction action;
    private List<String> hosts;

    // node options for adding/updating
    private int nodeMemory;
    private boolean nodeMemoryConfigured;
    private String nodeNetworks;
    private String nodePool;
    private boolean nodeActive = true;
    private boolean nodeActiveConfigured;

    private RemoteNodeManagement remoteNodeManagement;
    private Reporter reporter;
    private OutputStream outStream;

    public static void main(String args[]) {

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
        } else {
            logger.debug("Got error", anyError);
        }

        if (paramError != null) {
            System.err.println("Parameter Problem:\n\n" + paramError.getMessage());
            System.err.println("See --help");

        } else if (execError != null) {
            System.err.println(execError.getMessage());
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
            System.out.println(getHelpText());
            return;
        }

        this.loadAdminClientConfig();
        this.remoteNodeManagement = (RemoteNodeManagement) super.setupRemoting();

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
            nodes.add(new VmmNode(hostname, this.nodeActive, this.nodePool,
                    this.nodeMemory, this.nodeNetworks, true));
        }
        final String nodesJson = gson.toJson(nodes);
        NodeReport[] reports = null;
        try {
            final String reportJson = this.remoteNodeManagement.addNodes(nodesJson);
            reports = gson.fromJson(reportJson, NodeReport[].class);
        } catch (RemoteException e) {
            handleRemoteException(e);
        }

        try {
            reporter.report(nodeReportsToMaps(reports), this.outStream);
        } catch (IOException e) {
            throw new ExecutionProblem("Problem writing output: " + e.getMessage(), e);
        }
    }

    private void run_listNodes() throws ExecutionProblem {
        VmmNode[] nodes = null;
        try {
            final String nodesJson = this.remoteNodeManagement.listNodes();
            nodes = this.gson.fromJson(nodesJson, VmmNode[].class);
        } catch (RemoteException e) {
            handleRemoteException(e);
        }

        try {
            reporter.report(nodesToMaps(nodes), this.outStream);
        } catch (IOException e) {
            throw new ExecutionProblem("Problem writing output: " + e.getMessage(), e);
        }
    }

    private void run_removeNodes() throws ExecutionProblem {
        NodeReport[] reports = null;
        try {
            final String[] hostnames = this.hosts.toArray(new String[this.hosts.size()]);
            final String reportJson = this.remoteNodeManagement.removeNodes(hostnames);
            reports = gson.fromJson(reportJson, NodeReport[].class);
        } catch (RemoteException e) {
            handleRemoteException(e);
        }

        try {
            reporter.report(nodeReportsToMaps(reports), this.outStream);
        } catch (IOException e) {
            throw new ExecutionProblem("Problem writing output: " + e.getMessage(), e);
        }
    }

    private void run_updateNodes() throws ExecutionProblem {

        final String[] hostnames = this.hosts.toArray(new String[this.hosts.size()]);
        final Boolean active = this.nodeActiveConfigured ? this.nodeActive : null;
        final String resourcepool = this.nodePool;
        final Integer memory = this.nodeMemoryConfigured ? this.nodeMemory : null;
        final String networks = this.nodeNetworks;


        NodeReport[] reports = null;
        try {
            final String reportJson = this.remoteNodeManagement.updateNodes(
                    hostnames, active, resourcepool, memory, networks);
            reports = gson.fromJson(reportJson, NodeReport[].class);
        } catch (RemoteException e) {
            handleRemoteException(e);
        }

        try {
            reporter.report(nodeReportsToMaps(reports), this.outStream);
        } catch (IOException e) {
            throw new ExecutionProblem("Problem writing output: " + e.getMessage(), e);
        }
    }


    private void loadAdminClientConfig() throws ParameterProblem, ExecutionProblem {

        super.loadConfig(PROP_RMI_BINDING_NODEMGMT_DIR);

        // only need node parameter values if doing add-nodes
        if (this.action == AdminAction.AddNodes) {
            if (!this.nodeMemoryConfigured) {
                final String memString = properties.getProperty(PROP_DEFAULT_MEMORY);
                if (memString != null) {
                    this.nodeMemory = parseMemory(memString);
                    this.nodeMemoryConfigured = true;
                }
            }

            if (this.nodeNetworks == null) {
                this.nodeNetworks = properties.getProperty(PROP_DEFAULT_NETWORKS);
            }

            if (this.nodePool == null) {
                // if missing or invalid, error will come later if this value is actually needed
                this.nodePool = properties.getProperty(PROP_DEFAULT_POOL);
            }
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
                this.nodeNetworks = line.getOptionValue(Opts.NETWORKS);
            }

            if (line.hasOption(Opts.POOL)) {
                String pool = line.getOptionValue(Opts.POOL);
                if (pool == null || pool.trim().length() == 0) {
                    throw new ParameterProblem("Node pool value is empty");
                }
                this.nodePool = pool.trim();
            }

            final boolean active = line.hasOption(Opts.ACTIVE);
            final boolean inactive = line.hasOption(Opts.INACTIVE);

            if (active && inactive) {
                throw new ParameterProblem("You cannot specify both " +
                        Opts.ACTIVE_LONG + " and " + Opts.INACTIVE_LONG);
            }

            if (active) {
                this.nodeActiveConfigured = true;
            }
            if (inactive) {
                this.nodeActive = false;
                this.nodeActiveConfigured = true;
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
        super.configPath = config.trim();

        final boolean batchMode = line.hasOption(Opts.BATCH);
        final boolean json = line.hasOption(Opts.JSON);

        final Reporter.OutputMode mode;
        if (batchMode && json) {
            throw new ParameterProblem("You cannot specify both " +
                    Opts.BATCH_LONG + " and " + Opts.JSON_LONG);
        } else if (batchMode) {
            mode = Reporter.OutputMode.Batch;
        } else if (json) {
            mode = Reporter.OutputMode.Json;
        } else {
            mode = Reporter.OutputMode.Friendly;
        }

        final String[] fields;
        if (line.hasOption(Opts.REPORT)) {
            fields = parseFields(line.getOptionValue(Opts.REPORT), theAction);
        } else {
            fields = theAction.fields();
        }

        String delimiter = null;
        if (line.hasOption(Opts.DELIMITER)) {
            delimiter = line.getOptionValue(Opts.DELIMITER);
        }

        this.reporter = new Reporter(mode, fields, delimiter);

        if (line.hasOption(Opts.OUTPUT)) {
            final String filename = line.getOptionValue(Opts.OUTPUT);
            final File f = new File(filename);
            try {
                this.outStream = new FileOutputStream(f);
            } catch (FileNotFoundException e) {
                throw new ParameterProblem(
                        "Specified output file could not be opened for writing: " +
                                f.getAbsolutePath(), e);
            }
        } else {
            this.outStream = System.out;
        }

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

    private static String[] parseFields(String fieldsString, AdminAction action)
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
    private static List<String> parseHosts(String hostString)
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

    private static String getHelpText() {

        InputStream is = null;
        BufferedInputStream bis = null;
        try {
            is = AdminClient.class.getResourceAsStream("help.txt");
            if (is == null) {
                return "";
            }

            bis = new BufferedInputStream(is);
            StringBuilder sb = new StringBuilder();
            byte[] chars = new byte[1024];
            int bytesRead;
            while( (bytesRead = bis.read(chars)) > -1){
                sb.append(new String(chars, 0, bytesRead));
            }
            return sb.toString();

        } catch (IOException e) {
            logger.error("Error reading help text", e);
            return "";
        } finally {
            try {
            if (bis != null) {
                bis.close();
            }

            if (is != null) {
                is.close();
            }
            } catch (IOException e) {
                logger.error("Error reading help text", e);
            }
        }
    }

    private static List<Map<String,String>> nodesToMaps(VmmNode[] nodes) {
        List<Map<String,String>> maps = new ArrayList<Map<String, String>>(nodes.length);
        for (VmmNode node : nodes) {
            maps.add(nodeToMap(node));
        }
        return maps;
    }

    private static Map<String,String> nodeToMap(VmmNode node) {
        final HashMap<String, String> map =
                new HashMap<String, String>(5);
        map.put(FIELD_HOSTNAME, node.getHostname());
        map.put(FIELD_POOL, node.getPoolName());
        map.put(FIELD_MEMORY, String.valueOf(node.getMemory()));
        map.put(FIELD_NETWORKS, node.getNetworkAssociations());
        map.put(FIELD_IN_USE, String.valueOf(!node.isVacant()));
        map.put(FIELD_ACTIVE, String.valueOf(node.isActive()));
        return map;
    }

    private static List<Map<String,String>> nodeReportsToMaps(NodeReport[] nodeReports) {
        List<Map<String,String>> maps = new ArrayList<Map<String, String>>(nodeReports.length);
        for (NodeReport nodeReport : nodeReports) {
            maps.add(nodeReportToMap(nodeReport));
        }
        return maps;
    }

    private static Map<String,String> nodeReportToMap(NodeReport nodeReport) {
        final HashMap<String, String> map =
                new HashMap<String, String>(2);
        map.put(FIELD_HOSTNAME, nodeReport.getHostname());
        map.put(FIELD_RESULT, nodeReport.getState());
        final VmmNode node = nodeReport.getNode();
        if (node == null) {
            map.put(FIELD_POOL, null);
            map.put(FIELD_MEMORY, null);
            map.put(FIELD_NETWORKS, null);
            map.put(FIELD_IN_USE, null);
            map.put(FIELD_ACTIVE, null);
        } else {
            map.put(FIELD_POOL, node.getPoolName());
            map.put(FIELD_MEMORY, String.valueOf(node.getMemory()));
            map.put(FIELD_NETWORKS, node.getNetworkAssociations());
            map.put(FIELD_IN_USE, String.valueOf(!node.isVacant()));
            map.put(FIELD_ACTIVE, String.valueOf(node.isActive()));
        }
        return map;
    }
}

enum AdminAction {
    AddNodes(Opts.ADD_NODES, AdminClient.NODE_REPORT_FIELDS),
    ListNodes(Opts.LIST_NODES, AdminClient.NODE_FIELDS),
    RemoveNodes(Opts.REMOVE_NODES, AdminClient.NODE_REPORT_FIELDS_SHORT),
    UpdateNodes(Opts.UPDATE_NODES, AdminClient.NODE_REPORT_FIELDS),
    Help(Opts.HELP, null);


    private final String option;
    private final String[] fields;

    AdminAction(String option, String[] fields) {
        this.option = option;
        this.fields = fields;
    }

    public String option() {
        return option;
    }

    public String[] fields() {
        return fields;
    }
}

