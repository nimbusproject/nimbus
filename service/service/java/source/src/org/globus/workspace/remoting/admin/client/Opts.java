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
    public static final String ADD_NODES_LONG = "add";
    public final Option ADD_NODES_OPT =
                OptionBuilder.withLongOpt(ADD_NODES_LONG).hasArg().create(ADD_NODES);

    public static final String LIST_NODES = "l";
    public static final String LIST_NODES_LONG = "list";
    public final Option LIST_NODES_OPT =
                OptionBuilder.withLongOpt(LIST_NODES_LONG).hasOptionalArg().create(LIST_NODES);

    public static final String REMOVE_NODES = "d";
    public static final String REMOVE_NODES_LONG = "remove";
    public final Option REMOVE_NODES_OPT =
                OptionBuilder.withLongOpt(REMOVE_NODES_LONG).hasArg().create(REMOVE_NODES);

    public static final String UPDATE_NODES = "u";
    public static final String UPDATE_NODES_LONG = "update";
    public final Option UPDATE_NODES_OPT =
                OptionBuilder.withLongOpt(UPDATE_NODES_LONG).hasArg().create(UPDATE_NODES);

    public static final String POOL_AVAILABILITY = "N";
    public static final String POOL_AVAILABILITY_LONG = "allocation";
    public final Option POOL_AVAILABILITY_OPT =
                OptionBuilder.withLongOpt(POOL_AVAILABILITY_LONG).hasOptionalArg().create(POOL_AVAILABILITY);


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

    public static final String FREE = "F";
    public static final String FREE_LONG = "free";
    public final Option FREE_OPT =
                OptionBuilder.withLongOpt(FREE_LONG).create(FREE);

    public static final String USED = "U";
    public static final String USED_LONG = "used";
    public final Option USED_OPT =
                OptionBuilder.withLongOpt(USED_LONG).create(USED);


    //*************************************************************************
    // NIMBUS-ADMIN
    //*************************************************************************

    public static final String LIST_VMS = "l";
    public static final String LIST_VMS_LONG = "list";
    public final Option LIST_VMS_OPT =
                OptionBuilder.withLongOpt(LIST_VMS_LONG).hasOptionalArg().create(LIST_VMS);

    public static final String SHUTDOWN_VMS = "s";
    public static final String SHUTDOWN_VMS_LONG = "shutdown";
    public final Option SHUTDOWN_VMS_OPT =
                OptionBuilder.withLongOpt(SHUTDOWN_VMS_LONG).hasOptionalArg().create(SHUTDOWN_VMS);

    public static final String ALL_VMS = "a";
    public static final String ALL_VMS_LONG = "all";
    public final Option ALL_VMS_OPT =
                OptionBuilder.withLongOpt(ALL_VMS_LONG).hasOptionalArg().create(ALL_VMS);

    public static final String USER = "u";
    public static final String USER_LONG = "user";
    public final Option USER_OPT =
                OptionBuilder.withLongOpt(USER_LONG).hasOptionalArg().create(USER);

    public static final String DN = "d";
    public static final String DN_LONG = "dn";
    public final Option DN_OPT =
                OptionBuilder.withLongOpt(DN_LONG).hasOptionalArg().create(DN);

    public static final String GROUP_ID = "g";
    public static final String GROUP_ID_LONG = "gid";
    public final Option GROUP_ID_OPT =
                OptionBuilder.withLongOpt(GROUP_ID_LONG).hasOptionalArg().create(GROUP_ID);

    public static final String GROUP_NAME = "gn";
    public static final String GROUP_NAME_LONG= "gname";
    public final Option GROUP_NAME_OPT =
                OptionBuilder.withLongOpt(GROUP_NAME_LONG).hasOptionalArg().create(GROUP_NAME);

    public static final String ID = "i";
    public static final String ID_LONG = "id";
    public final Option ID_OPT =
                OptionBuilder.withLongOpt(ID_LONG).hasOptionalArg().create(ID);

    public static final String SECONDS = "n";
    public static final String SECONDS_LONG = "seconds";
    public final Option SECONDS_OPT =
                OptionBuilder.withLongOpt(SECONDS_LONG).hasOptionalArg().create(SECONDS);

    public static final String HOST = "hn";
    public static final String HOST_LONG = "host";
    public final Option HOST_OPT =
                OptionBuilder.withLongOpt(HOST_LONG).hasOptionalArg().create(HOST);

    public static final String NODE_LIST = "N";
    public static final String NODE_LIST_LONG = "nodes";
    public final Option NODE_LIST_OPT =
                OptionBuilder.withLongOpt(NODE_LIST_LONG).create(NODE_LIST);

    public final Option[] ALL_ENABLED_OPTIONS = {
            HELP_OPT, DEBUG_OPT, CONFIG_OPT, BATCH_OPT, DELIMITER_OPT,
            REPORT_OPT, JSON_OPT, OUTPUT_OPT, ADD_NODES_OPT, LIST_NODES_OPT,
            REMOVE_NODES_OPT, UPDATE_NODES_OPT, POOL_AVAILABILITY_OPT, NETWORKS_OPT, MEMORY_OPT, POOL_OPT,
            ACTIVE_OPT, INACTIVE_OPT, LIST_VMS_OPT, SHUTDOWN_VMS_OPT, USER_OPT, ID_OPT,
            SECONDS_OPT, ALL_VMS_OPT, HOST_OPT, DN_OPT, GROUP_ID_OPT,
            GROUP_NAME_OPT, FREE_OPT, USED_OPT, NODE_LIST_OPT
    };

}
