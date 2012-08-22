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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.globus.workspace.remoting.admin.VMTranslation;
import org.nimbustools.api.services.admin.RemoteAdminToolsManagement;

import java.io.*;
import java.rmi.RemoteException;
import java.util.*;


/**
 * This class runs the nimbus-admin tool and connects to the main service over rmi binding
 * RMI setup and config is handled by parent class RMIConfig
 */
public class RemoteAdminToolsMain extends RMIConfig {

    private static final String PROP_RMI_BINDING_ADMINTOOLS_DIR = "rmi.binding.admintools";

    private static final String FIELD_ID = "id";
    private static final String FIELD_NODE = "node";
    private static final String FIELD_GROUP_ID = "group_id";
    private static final String FIELD_GROUP_NAME = "group_name";
    private static final String FIELD_CREATOR = "creator";
    private static final String FIELD_STATE = "state";
    private static final String FIELD_START = "start time";
    private static final String FIELD_END = "end time";
    private static final String FIELD_MEMORY = "memory";
    private static final String FIELD_CPU_COUNT = "cpu count";
    private static final String FIELD_URI = "uri";

    final static String[] ADMIN_FIELDS = new String[] {
            FIELD_ID, FIELD_NODE, FIELD_GROUP_ID, FIELD_GROUP_NAME, FIELD_CREATOR, FIELD_STATE, FIELD_START,
                FIELD_END, FIELD_MEMORY, FIELD_CPU_COUNT, FIELD_URI};

    final static String[] NODE_LIST_FIELDS = new String[] {
            FIELD_NODE, FIELD_ID
    };

    private ToolAction action;
    private RemoteAdminToolsManagement remoteAdminToolsManagement;
    private String user;
    private String userDN;
    private String groupId;
    private String groupName;
    private String hostname;
    private String seconds;
    private String state;
    private List<String> vmIDs;
    private List<String> userList;
    private List<String> DNList;
    private List<String> gidList;
    private List<String> gnameList;
    private List<String> hostList;
    private boolean allVMs = false;
    private int numOpts = 0;
    private boolean force = false;

    public static void main(String args[]) {

        Throwable anyError = null;
        ParameterProblem paramError = null;
        ExecutionProblem execError = null;
        int ret = EXIT_OK;
        try {
            final RemoteAdminToolsMain ratm = new RemoteAdminToolsMain();
            ratm.setupDebug(args);
            ratm.run(args);

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

    public void run(String[] args) throws ExecutionProblem, ParameterProblem {
        this.loadArgs(args);

        if (this.action == ToolAction.Help) {
            InputStream is = RemoteAdminToolsMain.class.getResourceAsStream("adminHelp.txt");
            System.out.println(super.getHelpText(is));
            return;
        }

        super.loadConfig(PROP_RMI_BINDING_ADMINTOOLS_DIR);
        this.remoteAdminToolsManagement = (RemoteAdminToolsManagement) super.setupRemoting();
        switch (this.action) {
            case CleanupVMs:
                cleanupVMs();
                break;
            case ListVMs:
                listVMs();
                break;
            case ListNodes:
                listNodes();
                break;
            case ShutdownVMs:
                shutdownVM();
                break;
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
        ToolAction tAction = null;
        for (ToolAction t : ToolAction.values()) {
            if (line.hasOption(t.option())) {
                if (tAction == null) {
                    tAction = t;
                } else {
                    throw new ParameterProblem("You may only specify a single action");
                }
            }
        }

        if (tAction == null) {
            throw new ParameterProblem("You must specify an action");
        }

        this.action = tAction;
        logger.debug("Action: " + tAction);

        // short circuit for --help arg
        if (tAction == ToolAction.Help) {
            return;
        }

        //numOpts just makes sure you don't have non-compatible options running at the same time
        if(this.action == ToolAction.ListVMs) {
                if(line.hasOption(Opts.USER)) {
                    final String user = line.getOptionValue(Opts.USER);
                    if(user == null || user.trim().length() == 0) {
                        throw new ParameterProblem("User value is empty");
                    }
                    this.user = user;
                    numOpts++;
                }
                if(line.hasOption(Opts.DN)) {
                    final String dn = line.getOptionValue(Opts.DN);
                    if(dn == null || dn.trim().length() == 0) {
                        throw new ParameterProblem("User DN value is empty");
                    }
                    this.userDN = dn;
                    numOpts++;
                }
                if(line.hasOption(Opts.GROUP_ID)) {
                    final String gid = line.getOptionValue(Opts.GROUP_ID);
                    if(gid == null || gid.trim().length() == 0) {
                        throw new ParameterProblem("Group id value is empty");
                    }
                    this.groupId = gid;
                    numOpts++;
                }
                if(line.hasOption(Opts.GROUP_NAME)) {
                    final String gname = line.getOptionValue(Opts.GROUP_NAME);
                    if(gname == null || gname.trim().length() == 0) {
                        throw new ParameterProblem("Group name value is empty");
                    }
                    this.groupName = gname;
                    numOpts++;
                }
                if(line.hasOption(Opts.HOST)) {
                    final String hostname = line.getOptionValue(Opts.HOST);
                    if(hostname == null || hostname.trim().length() == 0) {
                        throw new ParameterProblem("Host value is empty");
                    }
                    this.hostname = hostname;
                    numOpts++;
                }
                if(line.hasOption(Opts.STATE)) {
                    final String state = line.getOptionValue(Opts.STATE);
                    if(state == null || state.trim().length() == 0) {
                        throw new ParameterProblem("State value is empty");
                    }
                    this.state = state;
                    numOpts++;
                }
        }
        else if(this.action == ToolAction.ShutdownVMs ||
            this.action == ToolAction.CleanupVMs) {
                if(line.hasOption(Opts.ALL_VMS)) {
                    allVMs = true;
                    numOpts++;
                }
                if(line.hasOption(Opts.ID)) {
                    final String id = line.getOptionValue(Opts.ID);
                    if(id == null || id.trim().length() == 0) {
                        throw new ParameterProblem("VM ID value is empty");
                    }
                    this.vmIDs = parseValues(id);
                    numOpts++;
                }
                if(line.hasOption(Opts.USER)) {
                    final String user = line.getOptionValue(Opts.USER);
                    if(user == null || user.trim().length() == 0) {
                        throw new ParameterProblem("User value is empty");
                    }
                    this.userList = parseValues(user);
                    numOpts++;
                }
                if(line.hasOption(Opts.DN)) {
                    final String dn = line.getOptionValue(Opts.DN);
                    if(dn == null || dn.trim().length() == 0) {
                        throw new ParameterProblem("DN value is empty");
                    }
                    this.DNList = parseValues(dn);
                    numOpts++;
                }
                if(line.hasOption(Opts.GROUP_ID)) {
                    final String gid = line.getOptionValue(Opts.GROUP_ID);
                    if(gid == null || gid.trim().length() == 0) {
                        throw new ParameterProblem("Group id value is empty");
                    }
                    this.gidList = parseValues(gid);
                    numOpts++;
                }
                if(line.hasOption(Opts.GROUP_NAME)) {
                    final String gname = line.getOptionValue(Opts.GROUP_NAME);
                    if(gname == null || gname.trim().length() == 0) {
                        throw new ParameterProblem("Group name value is empty");
                    }
                    this.gnameList = parseValues(gname);
                    numOpts++;
                }
                if(line.hasOption(Opts.HOST)) {
                    final String hostname = line.getOptionValue(Opts.HOST);
                    if(hostname == null || hostname.trim().length() == 0) {
                        throw new ParameterProblem("Hostname value is empty");
                    }
                    this.hostList = parseValues(hostname);
                    numOpts++;
                }
                if(line.hasOption(Opts.SECONDS)) {
                    final String seconds = line.getOptionValue(Opts.SECONDS);
                    if(seconds == null || seconds.trim().length() == 0) {
                        throw new ParameterProblem("Seconds value is empty");
                    }
                    this.seconds = seconds;
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
        force = line.hasOption(Opts.FORCE);

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
            fields = parseFields(line.getOptionValue(Opts.REPORT), tAction);
        } else {
            fields = tAction.fields();
        }

        String delimiter = null;
        if (line.hasOption(Opts.DELIMITER)) {
            delimiter = line.getOptionValue(Opts.DELIMITER);
        }

        if(fields != null)
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

    private void listVMs() throws ExecutionProblem {
        try {
            VMTranslation[] vms;
            if(numOpts > 1) {
                System.err.println("You may select only one of --user, --dn, --gid, --gname, or --host");
                return;
            }
            if(this.user != null) {
                final String vmsJson = this.remoteAdminToolsManagement.getVMsByUser(user);
                if(vmsJson == null) {
                    System.err.println("No vms with user " + user + " found");
                    return;
                }
                vms = gson.fromJson(vmsJson, VMTranslation[].class);
            }
            else if(this.userDN != null) {
                final String vmsJson = this.remoteAdminToolsManagement.getVMsByDN(userDN);
                if(vmsJson == null) {
                    System.err.println("No vms with DN " + userDN + " found");
                    return;
                }
                vms = gson.fromJson(vmsJson, VMTranslation[].class);
            }
            else if(this.groupId != null) {
                final String vmsJson = this.remoteAdminToolsManagement.getAllVMsByGroupId(groupId);
                if(vmsJson == null) {
                    System.err.println("No vms with group id " + groupId + " found");
                    return;
                }
                vms = gson.fromJson(vmsJson, VMTranslation[].class);
            }
            else if(this.groupName != null) {
                final String vmsJson = this.remoteAdminToolsManagement.getAllVMsByGroupName(groupName);
                if(vmsJson == null) {
                    System.err.println("No vms with group name " + groupName + " found");
                    return;
                }
                vms = gson.fromJson(vmsJson, VMTranslation[].class);
            }
            else if(this.hostname != null) {
                final String vmsJson = this.remoteAdminToolsManagement.getAllVMsByHost(hostname);
                if(vmsJson == null) {
                    System.err.println("No vms with host " + hostname + " found");
                    return;
                }
                vms = gson.fromJson(vmsJson, VMTranslation[].class);
            }
            else if(this.state != null) {
                final String vmsJson = this.remoteAdminToolsManagement.getVMsByState(state);
                if(vmsJson == null) {
                    System.err.println("No vms with state " + state + " found");
                    return;
                }
                vms = gson.fromJson(vmsJson, VMTranslation[].class);
            }
            else {
                final String vmsJson = this.remoteAdminToolsManagement.getAllRunningVMs();
                vms = gson.fromJson(vmsJson, VMTranslation[].class);
            }
            if(vms == null) {
                System.err.println("No Running vms found");
                return;
            }
            reporter.report(vmsToMaps(vms), this.outStream);
        }
        catch (RemoteException e) {
            super.handleRemoteException(e);
        }
        catch (IOException e) {
            throw new ExecutionProblem("Problem writing output: " + e.getMessage(), e);
        }
    }

    private void listNodes() throws ExecutionProblem {
        try {
            Hashtable ht = this.remoteAdminToolsManagement.showVMsForAllHosts();
            if(ht == null)
                System.err.println("No nodes with running VMs found");
            else
                reporter.report(nodesToMaps(ht), this.outStream);
        }
        catch(RemoteException e) {
            System.err.println(e.getMessage());
        }
        catch(IOException e) {
            throw new ExecutionProblem("Problem writing output: " + e.getMessage(), e);
        }
    }

    private void shutdownVM() {
        try {
            String result = "";
            String feedback;
            if(numOpts > 1) {
                result = "You must select only one of --all, --id, --user, --dn, --gid, --gname, or --host";
                System.err.println(result);
                return;
            }
            if(allVMs) {
                result = this.remoteAdminToolsManagement.shutdown(
                        RemoteAdminToolsManagement.SHUTDOWN_ALL, null, seconds, force);
            }
            else if(vmIDs != null) {
                for(int i = 0; i < vmIDs.size(); i++) {
                    feedback = this.remoteAdminToolsManagement.shutdown(
                        RemoteAdminToolsManagement.SHUTDOWN_ID, vmIDs.get(i), seconds, force);
                    if(feedback != null)
                        result += feedback + "\n";
                }
            }
            else if(userList != null) {
                for(int i = 0; i < userList.size(); i++) {
                    feedback = this.remoteAdminToolsManagement.shutdown(
                        RemoteAdminToolsManagement.SHUTDOWN_UNAME, userList.get(i), seconds, force);
                    if(feedback != null)
                        result += feedback + "\n";
                }
            }
            else if(DNList != null) {
                for(int i = 0; i < DNList.size(); i++) {
                    feedback = this.remoteAdminToolsManagement.shutdown(
                        RemoteAdminToolsManagement.SHUTDOWN_DN, DNList.get(i), seconds, force);
                    if(feedback != null)
                        result += feedback + "\n";
                }
            }
            else if(gidList != null) {
                for(int i = 0; i < gidList.size(); i++) {
                    feedback = this.remoteAdminToolsManagement.shutdown(
                        RemoteAdminToolsManagement.SHUTDOWN_GID, gidList.get(i), seconds, force);
                    if(feedback != null)
                        result += feedback + "\n";
                }
            }
            else if(gnameList != null) {
                for(int i = 0; i < gnameList.size(); i++) {
                    feedback = this.remoteAdminToolsManagement.shutdown(
                        RemoteAdminToolsManagement.SHUTDOWN_GNAME, gnameList.get(i), seconds, force);
                    if(feedback != null)
                        result += feedback + "\n";
                }
            }
            else if(hostList != null) {
                for(int i = 0; i < hostList.size(); i++) {
                    feedback = this.remoteAdminToolsManagement.shutdown(
                        RemoteAdminToolsManagement.SHUTDOWN_HOST, hostList.get(i), seconds, force);
                    if(feedback != null)
                        result += feedback + "\n";
                }
            }
            else {
                result = "Shutdown requires either --all, --id, --user, --dn, --gid, --gname, or --host option";
            }
            if(result != null && !result.isEmpty())
                System.err.println(result);
        }
        catch (RemoteException e) {
            System.err.println(e.getMessage());
        }
    }

    private void cleanupVMs() {
        try {
            String result = "";
            String feedback;
            if(numOpts > 1) {
                result = "You must select only one of --all, --id, --user, --dn, --gid, --gname, or --host";
                System.err.println(result);
                return;
            }
            if(allVMs) {
                result = this.remoteAdminToolsManagement.cleanup(
                        RemoteAdminToolsManagement.CLEANUP_ALL, null);
            }
            else if(vmIDs != null) {
                for(int i = 0; i < vmIDs.size(); i++) {
                    feedback = this.remoteAdminToolsManagement.cleanup(
                        RemoteAdminToolsManagement.CLEANUP_ID, vmIDs.get(i));
                    if(feedback != null)
                        result += feedback + "\n";
                }
            }
            else if(userList != null) {
                for(int i = 0; i < userList.size(); i++) {
                    feedback = this.remoteAdminToolsManagement.cleanup(
                        RemoteAdminToolsManagement.CLEANUP_UNAME, userList.get(i));
                    if(feedback != null)
                        result += feedback + "\n";
                }
            }
            else if(DNList != null) {
                for(int i = 0; i < DNList.size(); i++) {
                    feedback = this.remoteAdminToolsManagement.cleanup(
                        RemoteAdminToolsManagement.CLEANUP_DN, DNList.get(i));
                    if(feedback != null)
                        result += feedback + "\n";
                }
            }
            else if(gidList != null) {
                for(int i = 0; i < gidList.size(); i++) {
                    feedback = this.remoteAdminToolsManagement.cleanup(
                        RemoteAdminToolsManagement.CLEANUP_GID, gidList.get(i));
                    if(feedback != null)
                        result += feedback + "\n";
                }
            }
            else if(gnameList != null) {
                for(int i = 0; i < gnameList.size(); i++) {
                    feedback = this.remoteAdminToolsManagement.cleanup(
                        RemoteAdminToolsManagement.CLEANUP_GNAME, gnameList.get(i));
                    if(feedback != null)
                        result += feedback + "\n";
                }
            }
            else if(hostList != null) {
                for(int i = 0; i < hostList.size(); i++) {
                    feedback = this.remoteAdminToolsManagement.cleanup(
                        RemoteAdminToolsManagement.CLEANUP_HOST, hostList.get(i));
                    if(feedback != null)
                        result += feedback + "\n";
                }
            }
            else {
                result = "Cleanup requires either --all, --id, --user, --dn, --gid, --gname, or --host option";
            }
            if(result != null && !result.isEmpty())
                System.err.println(result);
        }
        catch (RemoteException e) {
            System.err.println(e.getMessage());
        }
    }

    private static List<Map<String,String>> vmsToMaps(VMTranslation[] vmts) {
        List<Map<String,String>> maps = new ArrayList<Map<String, String>>(vmts.length);
        for (VMTranslation vmt : vmts) {
            maps.add(vmToMap(vmt));
        }
        return maps;
    }

    private static Map<String, String> vmToMap(VMTranslation vmt) {
        final HashMap<String, String> map = new HashMap(11);
        map.put(FIELD_ID, vmt.getId());
        map.put(FIELD_NODE, vmt.getNode());
        map.put(FIELD_GROUP_ID, vmt.getGroupId());
        map.put(FIELD_GROUP_NAME, vmt.getGroupName());
        map.put(FIELD_CREATOR, vmt.getCallerIdentity());
        map.put(FIELD_STATE, vmt.getState());
        map.put(FIELD_START, vmt.getStartTime());
        map.put(FIELD_END, vmt.getEndTime());
        map.put(FIELD_MEMORY, vmt.getMemory());
        map.put(FIELD_CPU_COUNT, vmt.getCpuCount());
        map.put(FIELD_URI, vmt.getUri());
        return map;
    }

    private static List<Map<String,String>> nodesToMaps(Hashtable<String, String[]> ht) {
        List<Map<String,String>> maps = new ArrayList<Map<String, String>>(ht.size());

        Enumeration<String> hosts = ht.keys();
        while(hosts.hasMoreElements()) {
            String host = hosts.nextElement();
            String[] ids = ht.get(host);
            String idList = "";
            for(int i = 0; i < ids.length; i++) {
                if(i+1 != ids.length)
                    idList += ids[i] + ", ";
                else
                    idList += ids[i];
            }
            final HashMap<String, String> map = new HashMap(2);
            map.put(FIELD_NODE, host);
            map.put(FIELD_ID, idList);
            maps.add(map);
        }
        return maps;
    }

    private static List<String> parseValues(String valueString) throws ParameterProblem {
        if (valueString == null) {
            throw new ParameterProblem("list is invalid");
        }

        final String[] valueArray = valueString.trim().split("\\s*,\\s*");
        if (valueArray.length == 0) {
            throw new ParameterProblem("list is empty");
        }

        final List<String> values = new ArrayList<String>(valueArray.length);
        for (final String value : valueArray) {
            values.add(value);
        }
        return values;
    }
}

enum ToolAction implements AdminEnum {
    CleanupVMs(Opts.CLEANUP_VMS, null),
    ListVMs(Opts.LIST_VMS, RemoteAdminToolsMain.ADMIN_FIELDS),
    ListNodes(Opts.NODE_LIST, RemoteAdminToolsMain.NODE_LIST_FIELDS),
    ShutdownVMs(Opts.SHUTDOWN_VMS, null),
    Help(Opts.HELP, null);

    private final String option;
    private final String[] fields;

    ToolAction(String option, String[] fields) {
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
