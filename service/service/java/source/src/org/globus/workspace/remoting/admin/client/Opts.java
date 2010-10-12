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

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

public class Opts {

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

    public static final String CONFIG = "c";
    public static final String CONFIG_LONG = "conf";
    public final Option CONFIG_OPT =
                OptionBuilder.withLongOpt(CONFIG_LONG).hasArg().create(CONFIG);

    public static final String BATCH = "b";
    public static final String BATCH_LONG = "batch";
    public final Option BATCH_OPT =
                OptionBuilder.withLongOpt(BATCH_LONG).create(BATCH);

    public static final String DELIMITER = "D";
    public static final String DELIMITER_LONG = "delimiter";
    public final Option DELIMITER_OPT =
                OptionBuilder.withLongOpt(DELIMITER_LONG).hasArg().create(DELIMITER);

    public static final String REPORT = "r";
    public static final String REPORT_LONG = "report";
    public final Option REPORT_OPT =
                OptionBuilder.withLongOpt(REPORT_LONG).hasArg().create(REPORT);

    public static final String JSON = "j";
    public static final String JSON_LONG = "json";
    public final Option JSON_OPT =
                OptionBuilder.withLongOpt(JSON_LONG).create(JSON);

    public static final String OUTPUT = "o";
    public static final String OUTPUT_LONG = "output";
    public final Option OUTPUT_OPT =
                OptionBuilder.withLongOpt(OUTPUT_LONG).hasArg().create(OUTPUT);


    //*************************************************************************
    // ACTIONS
    //*************************************************************************

    public static final String ADD_NODES = "a";
    public static final String ADD_NODES_LONG = "add-nodes";
    public final Option ADD_NODES_OPT =
                OptionBuilder.withLongOpt(ADD_NODES_LONG).hasArg().create(ADD_NODES);

    public static final String LIST_NODES = "l";
    public static final String LIST_NODES_LONG = "list-nodes";
    public final Option LIST_NODES_OPT =
                OptionBuilder.withLongOpt(LIST_NODES_LONG).hasOptionalArg().create(LIST_NODES);

    public static final String REMOVE_NODES = "d";
    public static final String REMOVE_NODES_LONG = "remove-nodes";
    public final Option REMOVE_NODES_OPT =
                OptionBuilder.withLongOpt(REMOVE_NODES_LONG).hasArg().create(REMOVE_NODES);

    public static final String UPDATE_NODES = "u";
    public static final String UPDATE_NODES_LONG = "update-nodes";
    public final Option UPDATE_NODES_OPT =
                OptionBuilder.withLongOpt(UPDATE_NODES_LONG).hasArg().create(UPDATE_NODES);


    //*************************************************************************
    // NODE SETTINGS
    //*************************************************************************

    public static final String ACTIVE = "A";
    public static final String ACTIVE_LONG = "active";
    public final Option ACTIVE_OPT =
                OptionBuilder.withLongOpt(ACTIVE_LONG).create(ACTIVE);

    public static final String INACTIVE = "i";
    public static final String INACTIVE_LONG = "inactive";
    public final Option INACTIVE_OPT =
                OptionBuilder.withLongOpt(INACTIVE_LONG).create(INACTIVE);

    public static final String NETWORKS = "n";
    public static final String NETWORKS_LONG = "networks";
    public final Option NETWORKS_OPT =
                OptionBuilder.withLongOpt(NETWORKS_LONG).hasArg().create(NETWORKS);

    public static final String MEMORY = "m";
    public static final String MEMORY_LONG = "memory";
    public final Option MEMORY_OPT =
                OptionBuilder.withLongOpt(MEMORY_LONG).hasArg().create(MEMORY);

    public static final String POOL = "p";
    public static final String POOL_LONG = "pool";
    public final Option POOL_OPT =
                OptionBuilder.withLongOpt(POOL_LONG).hasArg().create(POOL);


    public final Option[] ALL_ENABLED_OPTIONS = {
            HELP_OPT, DEBUG_OPT, CONFIG_OPT, BATCH_OPT, DELIMITER_OPT,
            REPORT_OPT, JSON_OPT, OUTPUT_OPT, ADD_NODES_OPT, LIST_NODES_OPT,
            REMOVE_NODES_OPT, UPDATE_NODES_OPT, NETWORKS_OPT, MEMORY_OPT, POOL_OPT,
            ACTIVE_OPT, INACTIVE_OPT,
    };

}
