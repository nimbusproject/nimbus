package org.globus.workspace.remoting.admin.client;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nimbustools.api.services.admin.RemoteAdminToolsManagement;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

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
 * Main class for nimbus-admin tool in /bin
 */
public class RemoteAdminToolsMain extends RMIConfig {

    private static final Log logger =
            LogFactory.getLog(RemoteAdminToolsMain.class.getName());

    private static final String PROP_RMI_BINDING_ADMINTOOLS_DIR = "rmi.binding.admintools";

    public static final int EXIT_OK = 0;
    public static final int EXIT_PARAMETER_PROBLEM = 1;
    public static final int EXIT_EXECUTION_PROBLEM = 2;
    public static final int EXIT_UNKNOWN_PROBLEM = 3;

    private RemoteAdminToolsManagement remoteAdminToolsManagement;

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

        Throwable anyError = null;
        ParameterProblem paramError = null;
        ExecutionProblem execError = null;
        int ret = EXIT_OK;

        try {
            final RemoteAdminToolsMain ratm = new RemoteAdminToolsMain();
            ratm.run(args);
        }
        catch (ParameterProblem e) {
            paramError = e;
            anyError = e;
            ret = EXIT_PARAMETER_PROBLEM;
        }
        catch (ExecutionProblem e) {
            execError = e;
            anyError = e;
            ret = EXIT_EXECUTION_PROBLEM;
        }
    }

    public void run(String[] args) throws ParameterProblem, ExecutionProblem {
        this.loadArgs(args);
        super.loadConfig(PROP_RMI_BINDING_ADMINTOOLS_DIR);
        this.remoteAdminToolsManagement = (RemoteAdminToolsManagement) super.setupRemoting();
        this.listRunningVMs();
    }

    private void loadArgs(String[] args) throws ParameterProblem {
        logger.debug("Parsing command line arguments");
        final CommandLineParser parser = new PosixParser();

        final Opts opts = new Opts();
        final CommandLine line;
        try {
            line = parser.parse(opts.getOptions(), args);
        }
        catch (ParseException e) {
            throw new ParameterProblem(e.getMessage(), e);
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
    }

    private void listRunningVMs() throws ExecutionProblem {
        try {
            Hashtable vms = this.remoteAdminToolsManagement.getAllRunningVMs();

            for(Enumeration<String> vmIds = vms.keys(); vmIds.hasMoreElements();) {
                int i = 0;
                String id = vmIds.nextElement();
                ArrayList al = (ArrayList) vms.get(id);
                System.out.println("ID: " + id);
                System.out.println("Group ID: " + al.get(i++));
                System.out.println("Creator: " + al.get(i++));
                System.out.println("State: " + al.get(i++));
            }
        }
        catch (RemoteException e) {
            super.handleRemoteException(e);
        }
    }
}
