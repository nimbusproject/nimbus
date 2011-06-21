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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.*;
import org.apache.log4j.varia.NullAppender;
import org.nimbustools.api.services.admin.RemoteAdminToolsManagement;

import java.rmi.RemoteException;
import java.util.*;


public class RemoteAdminToolsMain extends RMIConfig {

    private static final Log logger =
            LogFactory.getLog(RMIConfig.class.getName());

    public static final int EXIT_OK = 0;
    public static final int EXIT_PARAMETER_PROBLEM = 1;
    public static final int EXIT_EXECUTION_PROBLEM = 2;
    public static final int EXIT_UNKNOWN_PROBLEM = 3;

    private static final String PROP_RMI_BINDING_ADMINTOOLS_DIR = "rmi.binding.admintools";

    private ToolAction action;
    private RemoteAdminToolsManagement remoteAdminToolsManagement;
    private String user;
    private String vmID;
    private String seconds;
    private boolean allVMs = false;

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
            final RemoteAdminToolsMain ratm = new RemoteAdminToolsMain();
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

    public void run(String[] args)
            throws ExecutionProblem, ParameterProblem {

        this.loadArgs(args);
        super.loadConfig(PROP_RMI_BINDING_ADMINTOOLS_DIR);
        this.remoteAdminToolsManagement = (RemoteAdminToolsManagement) super.setupRemoting();
        switch (this.action) {
            case ListVMs:
                listVMs();
                break;
            case ShutdownVMs:
                shutdownVM(vmID, seconds);
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
                    System.out.println("parameter problem");
                    throw new ParameterProblem("You may only specify a single action");
                }
            }
        }

        if (tAction == null) {
            throw new ParameterProblem("You must specify an action");
        }

        this.action = tAction;
        logger.debug("Action: " + tAction);

        if(this.action == ToolAction.ListVMs) {
                if(line.hasOption(Opts.USER)) {
                    final String user = line.getOptionValue(Opts.USER);
                    if(user == null || user.trim().length() == 0) {
                        throw new ParameterProblem("User value is empty");
                    }
                    this.user = user;
                }
        }
        else if(this.action == ToolAction.ShutdownVMs) {
                if(line.hasOption(Opts.ALL_VMS))
                    allVMs = true;
                if(line.hasOption(Opts.ID) && !allVMs) {
                    final String id = line.getOptionValue(Opts.ID);
                    if(id == null || id.trim().length() == 0) {
                        throw new ParameterProblem("VM ID value is empty");
                    }
                    this.vmID = id;
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

        final List leftovers = line.getArgList();
		if (leftovers != null && !leftovers.isEmpty()) {
			throw new ParameterProblem("There are unrecognized arguments, check -h to make " +
					"sure you are doing the intended thing: " + leftovers.toString());
		}

    }

    private void listVMs() throws ExecutionProblem {
        try {
            Hashtable vms;
            if(this.user == null) {
                vms = this.remoteAdminToolsManagement.getAllRunningVMs();
            }
            else {
                vms = this.remoteAdminToolsManagement.getVMsByUser(user);
            }
            if(vms == null) {
                System.out.println("No Running vms found");
                return;
            }

            for(Enumeration<String> vmIds = vms.keys(); vmIds.hasMoreElements();) {
                int i = 0;
                String id = vmIds.nextElement();
                ArrayList al = (ArrayList) vms.get(id);
                System.out.println("ID: " + id);
                System.out.println("Group ID: " + al.get(i++));
                System.out.println("Creator: " + al.get(i++));
                System.out.println("State: " + al.get(i));
            }
        }
        catch (RemoteException e) {
            super.handleRemoteException(e);
        }
    }

    private void shutdownVM(String id, String seconds) throws ExecutionProblem {
        try {
            String result;
            if(allVMs) {
                result = this.remoteAdminToolsManagement.shutdownAllVMs(seconds);
                System.out.println(result);
            }
            else {
                result = this.remoteAdminToolsManagement.shutdownVM(id, seconds);
                System.out.println(result);
            }
        }
        catch (RemoteException e) {
            super.handleRemoteException(e);
        }
    }
}

enum ToolAction {
    ListVMs(Opts.LIST_VMS, null),
    ShutdownVMs(Opts.SHUTDOWN_VMS, null);


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


