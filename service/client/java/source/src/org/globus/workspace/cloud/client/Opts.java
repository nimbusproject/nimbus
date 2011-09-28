/*
 * Copyright 1999-2008 University of Chicago
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

package org.globus.workspace.cloud.client;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

public class Opts {
    
    /* ACTIONS */

    public static final String HELP_OPT_STRING = "h";
    public static final String HELP_OPT_STRING_LONG = "help";
    public final Option HELP_OPT =
        OptionBuilder.withLongOpt(HELP_OPT_STRING_LONG)
                     .create(HELP_OPT_STRING);

    public static final String EXTRAHELP_OPT_STRING = "extrahelp";
    public final Option EXTRAHELP_OPT =
        OptionBuilder.withLongOpt(EXTRAHELP_OPT_STRING).create();

    public static final String USAGE_OPT_STRING = "u";
    public static final String USAGE_OPT_STRING_LONG = "usage";
    public final Option USAGE_OPT =
        OptionBuilder.withLongOpt(USAGE_OPT_STRING_LONG)
                     .create(USAGE_OPT_STRING);
        
    public static final String TRANSFER_OPT_STRING = "transfer";
    public final Option TRANSFER_OPT =
        OptionBuilder.withLongOpt(TRANSFER_OPT_STRING).create();

    public static final String LIST_OPT_STRING = "list";
    public final Option LIST_OPT =
        OptionBuilder.withLongOpt(LIST_OPT_STRING).create();

    public static final String DELETE_OPT_STRING = "delete";
    public final Option DELETE_OPT =
        OptionBuilder.withLongOpt(DELETE_OPT_STRING).create();

    public static final String DOWNLOAD_OPT_STRING = "download";
    public final Option DOWNLOAD_OPT =
        OptionBuilder.withLongOpt(DOWNLOAD_OPT_STRING).create();

    public static final String RUN_OPT_STRING = "run";
    public final Option RUN_OPT =
        OptionBuilder.withLongOpt(RUN_OPT_STRING).create();
    
    public static final String PRINT_TARGET_OPT_STRING = "print-file-URL";
    public final Option PRINT_TARGET_OPT =
        OptionBuilder.withLongOpt(PRINT_TARGET_OPT_STRING).create();

    public static final String PRINT_SERVICE_OPT_STRING = "print-service-URL";
    public final Option PRINT_SERVICE_OPT =
        OptionBuilder.withLongOpt(PRINT_SERVICE_OPT_STRING).create();

    public static final String SECURITY_OPT_STRING = "security";
    public final Option SECURITY_OPT =
        OptionBuilder.withLongOpt(SECURITY_OPT_STRING).create();

    public static final String SAVE_OPT_STRING = "save";
    public final Option SAVE_OPT =
        OptionBuilder.withLongOpt(SAVE_OPT_STRING).create();

    public static final String DESTROY_OPT_STRING = "terminate";
    public final Option DESTROY_OPT =
        OptionBuilder.withLongOpt(DESTROY_OPT_STRING).create();

    public static final String STATUS_CHECK_OPT_STRING = "status";
    public final Option STATUS_CHECK_OPT =
        OptionBuilder.withLongOpt(STATUS_CHECK_OPT_STRING).create();

    public static final String ASSOC_QUERY_OPT_STRING = "networks";
    public final Option ASSOC_QUERY_OPT =
        OptionBuilder.withLongOpt(ASSOC_QUERY_OPT_STRING).create();

    public static final String HASH_PRINT_OPT_STRING = "hash-print";
    public final Option HASH_PRINT_OPT =
        OptionBuilder.hasArg().withLongOpt(HASH_PRINT_OPT_STRING).create();

    public static final String INIT_CTX_OPT_STRING = "init-context";
    public final Option INIT_CTX_OPT =
        OptionBuilder.hasArg().withLongOpt(INIT_CTX_OPT_STRING).create();

    public static final String PRINT_CTX_STATUS_OPT_STRING = "print-ctx-status";
    public final Option PRINT_CTX_STATUS_OPT =
        OptionBuilder.withLongOpt(PRINT_CTX_STATUS_OPT_STRING).create();

    /* OPTIONS */

    public static final String PROPFILE_OPT_STRING = "conf";
    public final Option PROPFILE_OPT =
        OptionBuilder.hasArg().withLongOpt(PROPFILE_OPT_STRING).create();

    public static final String CAHASH_OPT_STRING = "cahash";
    public final Option CAHASH_OPT =
        OptionBuilder.hasArg().withLongOpt(CAHASH_OPT_STRING).create();

    /* do not change text of this arg w/o duplicating @ Bootstrap.ARG_SEARCH */
    public static final String CADIR_OPT_STRING = "append-cadir";
    public final Option CADIR_OPT =
        OptionBuilder.hasArg().withLongOpt(CADIR_OPT_STRING).create();

    public static final String FACTORY_OPT_STRING = "factory";
    public final Option FACTORY_OPT =
        OptionBuilder.hasArg().withLongOpt(FACTORY_OPT_STRING).create();

    public static final String GRIDFTP_OPT_STRING = "gridftp";
    public final Option GRIDFTP_OPT =
        OptionBuilder.hasArg().withLongOpt(GRIDFTP_OPT_STRING).create();

    public static final String FACTORY_ID_OPT_STRING = "factory-id";
    public final Option FACTORY_ID_OPT =
        OptionBuilder.hasArg().withLongOpt(FACTORY_ID_OPT_STRING).create();

    public static final String GRIDFTP_ID_OPT_STRING = "gridftp-id";
    public final Option GRIDFTP_ID_OPT =
        OptionBuilder.hasArg().withLongOpt(GRIDFTP_ID_OPT_STRING).create();

    public static final String TARGETDIR_OPT_STRING = "targetdir";
    public final Option TARGETDIR_OPT =
        OptionBuilder.hasArg().withLongOpt(TARGETDIR_OPT_STRING).create();

    public static final String SOURCEFILE_OPT_STRING = "sourcefile";
    public final Option SOURCEFILE_OPT =
        OptionBuilder.hasArg().withLongOpt(SOURCEFILE_OPT_STRING).create();

    public static final String NAME_OPT_STRING = "name";
    public final Option NAME_OPT =
        OptionBuilder.hasArg().withLongOpt(NAME_OPT_STRING).create();

    public static final String NEWNAME_OPT_STRING = "newname";
    public final Option NEWNAME_OPT =
        OptionBuilder.hasArg().withLongOpt(NEWNAME_OPT_STRING).create();

    public static final String HANDLE_OPT_STRING = "handle";
    public final Option HANDLE_OPT =
        OptionBuilder.hasArg().withLongOpt(HANDLE_OPT_STRING).create();

    public static final String HOURS_OPT_STRING = "hours";
    public final Option HOURS_OPT =
        OptionBuilder.hasArg().withLongOpt(HOURS_OPT_STRING).create();

    public static final String DEBUG_OPT_STRING = "d";
    public static final String DEBUG_OPT_STRING_LONG = "debug";
    public final Option DEBUG_OPT =
        OptionBuilder.withLongOpt(DEBUG_OPT_STRING_LONG)
                     .create(DEBUG_OPT_STRING);

    public static final String TIMEOUT_OPT_STRING = "timeout";
    public final Option TIMEOUT_OPT =
        OptionBuilder.hasArg().withLongOpt(TIMEOUT_OPT_STRING).create();

    public static final String COMMON_OPT_STRING = "common";
    public final Option COMMON_OPT =
        OptionBuilder.withLongOpt(COMMON_OPT_STRING).create();


    public static final String IMAGE_DESC_OPT_STRING = "imagedesc";
    public final Option IMAGE_DESC_OPT =
            OptionBuilder.hasArg().withLongOpt(IMAGE_DESC_OPT_STRING).create();

    public static final String NOSPINNER_OPT_STRING = "nospinner";
    public final Option NOSPINNER_OPT =
        OptionBuilder.withLongOpt(NOSPINNER_OPT_STRING).create();

    public static final String SSH_FILE_OPT_STRING = "ssh-pubkey";
    public final Option SSH_FILE_OPT =
        OptionBuilder.hasArg().withLongOpt(SSH_FILE_OPT_STRING).create();

    public static final String POLL_INTERVAL_OPT_STRING = "poll-interval";
    public final Option POLL_INTERVAL_OPT =
        OptionBuilder.hasArg().withLongOpt(POLL_INTERVAL_OPT_STRING).create();

    public static final String NOTIFICATIONS_OPT_STRING = "notifications";
    public final Option NOTIFICATIONS_OPT =
        OptionBuilder.withLongOpt(NOTIFICATIONS_OPT_STRING).create();

    public static final String HISTORY_DIR_OPT_STRING = "history-dir";
    public final Option HISTORY_DIR_OPT =
        OptionBuilder.hasArg().withLongOpt(HISTORY_DIR_OPT_STRING).create();

    public static final String HISTORY_SUBDIR_OPT_STRING = "history-subdir";
    public final Option HISTORY_SUBDIR_OPT =
        OptionBuilder.hasArg().withLongOpt(HISTORY_SUBDIR_OPT_STRING).create();

    public static final String EPR_FILE_OPT_STRING = "epr-file";
    public final Option EPR_FILE_OPT =
        OptionBuilder.hasArg().withLongOpt(EPR_FILE_OPT_STRING).create();

    public static final String LOCAL_FILE_OPT_STRING = "localfile";
    public final Option LOCAL_FILE_OPT =
        OptionBuilder.hasArg().withLongOpt(LOCAL_FILE_OPT_STRING).create();

    public static final String CLUSTER_OPT_STRING = "cluster";
    public final Option CLUSTER_OPT =
        OptionBuilder.hasArg().withLongOpt(CLUSTER_OPT_STRING).create();

    public static final String EC2SCRIPT_OPT_STRING = "ec2script";
    public final Option EC2SCRIPT_OPT =
        OptionBuilder.hasArg().withLongOpt(EC2SCRIPT_OPT_STRING).create();

    public static final String BROKER_URL_OPT_STRING = "broker-url";
    public final Option BROKER_URL_OPT =
        OptionBuilder.hasArg().withLongOpt(BROKER_URL_OPT_STRING).create();

    public static final String BROKER_ID_OPT_STRING = "broker-id";
    public final Option BROKER_ID_OPT =
        OptionBuilder.hasArg().withLongOpt(BROKER_ID_OPT_STRING).create();

    public static final String NOCTXLOCK_OPT_STRING = "no-ctx-lock";
    public final Option NOCTXLOCK_OPT =
        OptionBuilder.withLongOpt(NOCTXLOCK_OPT_STRING).create();

	public static final String KERNEL_OPT_STRING = "kernel";
    public final Option KERNEL_OPT =
        OptionBuilder.hasArg().withLongOpt(KERNEL_OPT_STRING).create();

    // undocumented options:

    public static final String DEBUGGER_HANG_OPT_STRING = "debuggerhang";
    public final Option DEBUGGER_HANG_OPT =
        OptionBuilder.withLongOpt(DEBUGGER_HANG_OPT_STRING).create();

    public static final String HOSTKEYDIR_OPT_STRING = "hostkeydir";
    public final Option HOSTKEYDIR_OPT =
        OptionBuilder.withLongOpt(HOSTKEYDIR_OPT_STRING).create();

    
    /* "export" */

    public final Option[] ALL_ENABLED_OPTIONS = {this.HELP_OPT,
                                                 this.EXTRAHELP_OPT,
                                                 this.USAGE_OPT,
                                                 this.TRANSFER_OPT,
                                                 this.LIST_OPT,
                                                 this.DELETE_OPT,
                                                 this.DOWNLOAD_OPT,
                                                 this.RUN_OPT,
                                                 this.PRINT_TARGET_OPT,
                                                 this.PRINT_SERVICE_OPT,
                                                 this.SECURITY_OPT,
                                                 this.SAVE_OPT,
                                                 this.DESTROY_OPT,
                                                 this.STATUS_CHECK_OPT,
                                                 this.ASSOC_QUERY_OPT,
                                                 this.HASH_PRINT_OPT,
                                                 this.PROPFILE_OPT,
                                                 this.CAHASH_OPT,
                                                 this.CADIR_OPT,
                                                 this.FACTORY_OPT,
                                                 this.GRIDFTP_OPT,
                                                 this.TARGETDIR_OPT,
                                                 this.SOURCEFILE_OPT,
                                                 this.NAME_OPT,
                                                 this.NEWNAME_OPT,
                                                 this.HANDLE_OPT,
                                                 this.HOURS_OPT,
                                                 this.DEBUG_OPT,
                                                 this.TIMEOUT_OPT,
                                                 this.COMMON_OPT,
                                                 this.IMAGE_DESC_OPT,
                                                 this.NOSPINNER_OPT,
                                                 this.SSH_FILE_OPT,
                                                 this.POLL_INTERVAL_OPT,
                                                 this.NOTIFICATIONS_OPT,
                                                 this.HISTORY_DIR_OPT,
                                                 this.HISTORY_SUBDIR_OPT,
                                                 this.EPR_FILE_OPT,
                                                 this.LOCAL_FILE_OPT,
                                                 this.DEBUGGER_HANG_OPT,
                                                 this.CLUSTER_OPT,
                                                 this.EC2SCRIPT_OPT,
                                                 this.BROKER_URL_OPT,
                                                 this.BROKER_ID_OPT,
                                                 this.NOCTXLOCK_OPT,
                                                 this.HOSTKEYDIR_OPT,
												 this.KERNEL_OPT,
                                                 this.PRINT_CTX_STATUS_OPT,
                                                 this.INIT_CTX_OPT
                                                 };
}
