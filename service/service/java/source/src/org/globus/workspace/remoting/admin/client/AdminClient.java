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

import org.apache.commons.cli.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;


public class AdminClient {

    private static final Log logger =
            LogFactory.getLog(AdminClient.class.getName());

    public static final int EXIT_OK = 0;
    public static final int EXIT_PARAMETER_PROBLEM = 1;
    public static final int EXIT_EXECUTION_PROBLEM = 2;
    public static final int EXIT_UNKNOWN_PROBLEM = 3;

    public static void main(String argv[]) {

        // early check for debug options
        boolean isDebug = false;
        final String debugFlag = "--" + Opts.DEBUG_LONG;
        for (String arg : argv) {
            if (debugFlag.equals(arg)) {
                isDebug = true;
                break;
            }
        }

        if (isDebug) {
            BasicConfigurator.configure();
            logger.info("Debug mode enabled");
        }

        Throwable anyError = null;
        ParameterProblem paramError = null;
        ExecutionProblem execError = null;
        int ret = EXIT_OK;
        try {

            mainImpl(argv);

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

    private static void mainImpl(String[] argv)
            throws ExecutionProblem, ParameterProblem {

        final CommandLineParser parser = new PosixParser();

        final Opts opts = new Opts();
        final CommandLine line;
        try {
            line = parser.parse(opts.getOptions(), argv);
        } catch (ParseException e) {
            throw new ParameterProblem(e.getMessage(), e);
        }

        if (line.hasOption(Opts.HELP)) {
            printHelp();
        }

        AdminClient client = new AdminClient();


    }

    private static void printHelp() {

    }
}

enum AdminAction {
    AddNodes, ListNodes, RemoveNodes, UpdateNodes
}

class Opts {

    private final Options options;

    public Opts() {
        options = new Options();
        for (Option o : ALL_ENABLED_OPTIONS) {
            options.addOption(o);
        }
    }

    public Options getOptions() {
        return options;
    }

    //*************************************************************************
    // GENERAL
    //*************************************************************************

    public static final String HELP = "h";
    public static final String HELP_LONG = "help";
    public final Option HELP_OPT =
            OptionBuilder.withLongOpt(HELP_LONG).create(HELP);

    public static final String DEBUG_LONG = "debug";
    public final Option DEBUG_OPT =
            OptionBuilder.withLongOpt(DEBUG_LONG).create();
    
    public static final String DRYRUN_LONG = "dryrun";
    public final Option DRYRUN_OPT =
            OptionBuilder.withLongOpt(DRYRUN_LONG).create();

    public static final String CONFIG = "c";
    public static final String CONFIG_LONG = "conf";
    public final Option CONFIG_OPT =
                OptionBuilder.withLongOpt(CONFIG_LONG).create(CONFIG);

    public static final String BATCH = "b";
    public static final String BATCH_LONG = "batch";
    public final Option BATCH_OPT =
                OptionBuilder.withLongOpt(BATCH_LONG).create(BATCH);

    public static final String REPORT = "r";
    public static final String REPORT_LONG = "report";
    public final Option REPORT_OPT =
                OptionBuilder.withLongOpt(REPORT_LONG).create(REPORT);

    public static final String JSON = "j";
    public static final String JSON_LONG = "json";
    public final Option JSON_OPT =
                OptionBuilder.withLongOpt(JSON_LONG).create(JSON);


    //*************************************************************************
    // ACTIONS
    //*************************************************************************

    public static final String ADD_NODES = "a";
    public static final String ADD_NODES_LONG = "add-nodes";
    public final Option ADD_NODES_OPT =
                OptionBuilder.withLongOpt(ADD_NODES_LONG).create(ADD_NODES);

    public static final String LIST_NODES = "l";
    public static final String LIST_NODES_LONG = "list-nodes";
    public final Option LIST_NODES_OPT =
                OptionBuilder.withLongOpt(LIST_NODES_LONG).create(LIST_NODES);

    public static final String REMOVE_NODES = "d";
    public static final String REMOVE_NODES_LONG = "remove-nodes";
    public final Option REMOVE_NODES_OPT =
                OptionBuilder.withLongOpt(REMOVE_NODES_LONG).create(REMOVE_NODES);

    public static final String UPDATE_NODES = "u";
    public static final String UPDATE_NODES_LONG = "update-nodes";
    public final Option UPDATE_NODES_OPT =
                OptionBuilder.withLongOpt(UPDATE_NODES_LONG).create(UPDATE_NODES);


    //*************************************************************************
    // NODE SETTINGS
    //*************************************************************************

    public static final String NETWORKS = "n";
    public static final String NETWORKS_LONG = "networks";
    public final Option NETWORKS_OPT =
                OptionBuilder.withLongOpt(NETWORKS_LONG).create(NETWORKS);

    public static final String MEMORY = "m";
    public static final String MEMORY_LONG = "memory";
    public final Option MEMORY_OPT =
                OptionBuilder.withLongOpt(MEMORY_LONG).create(MEMORY);

    public static final String POOL = "p";
    public static final String POOL_LONG = "pool";
    public final Option POOL_OPT =
                OptionBuilder.withLongOpt(POOL_LONG).create(POOL);


    public final Option[] ALL_ENABLED_OPTIONS = {
            HELP_OPT, DEBUG_OPT, DRYRUN_OPT, CONFIG_OPT, BATCH_OPT, REPORT_OPT,
            JSON_OPT, ADD_NODES_OPT, LIST_NODES_OPT, REMOVE_NODES_OPT,
            UPDATE_NODES_OPT, NETWORKS_OPT, MEMORY_OPT, POOL_OPT
    };

}
