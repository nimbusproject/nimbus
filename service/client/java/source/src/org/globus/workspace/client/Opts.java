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

package org.globus.workspace.client;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

public class Opts {

    public static final String EXTRAHELP_OPT_STRING = "extrahelp";
    public final Option EXTRAHELP_OPT =
        OptionBuilder.withLongOpt(EXTRAHELP_OPT_STRING).create();

    public static final String DESTROY_OPT_STRING = "destroy";
    public final Option DESTROY_OPT =
        OptionBuilder.withLongOpt(DESTROY_OPT_STRING).create();

    public static final String FACTORYRP_OPT_STRING = "factoryrp";
    public final Option FACTORYRP_OPT =
        OptionBuilder.withLongOpt(FACTORYRP_OPT_STRING).create();

    public static final String DEPLOY_OPT_STRING = "deploy";
    public final Option DEPLOY_OPT =
        OptionBuilder.withLongOpt(DEPLOY_OPT_STRING).create();

    public static final String PAUSE_OPT_STRING = "pause";
    public final Option PAUSE_OPT =
        OptionBuilder.withLongOpt(PAUSE_OPT_STRING).create();

    public static final String SHUTDOWN_OPT_STRING = "shutdown";
    public final Option SHUTDOWN_OPT =
        OptionBuilder.withLongOpt(SHUTDOWN_OPT_STRING).create();

    public static final String SHUTDOWN_SAVE_OPT_STRING = "shutdown-save";
    public final Option SHUTDOWN_SAVE_OPT =
        OptionBuilder.withLongOpt(SHUTDOWN_SAVE_OPT_STRING).create();

    public static final String START_OPT_STRING = "start";
    public final Option START_OPT =
        OptionBuilder.withLongOpt(START_OPT_STRING).create();

    public static final String REBOOT_OPT_STRING = "reboot";
    public final Option REBOOT_OPT =
        OptionBuilder.withLongOpt(REBOOT_OPT_STRING).create();

    public static final String SUBSCRIBE_OPT_STRING = "subscribe";
    public final Option SUBSCRIBE_OPT =
        OptionBuilder.withLongOpt(SUBSCRIBE_OPT_STRING).create();

    public static final String METADATA_OPT_STRING = "w";
    public static final String METADATA_OPT_STRING_LONG = "metadata";
    public final Option METADATA_OPT =
        OptionBuilder.hasArg().withLongOpt(METADATA_OPT_STRING_LONG)
                              .create(METADATA_OPT_STRING);

    public static final String OPTIONAL_OPT_STRING = "o";
    public static final String OPTIONAL_OPT_STRING_LONG = "optional";
    public final Option OPTIONAL_OPT =
        OptionBuilder.hasArg().withLongOpt(OPTIONAL_OPT_STRING_LONG)
                              .create(OPTIONAL_OPT_STRING);

    public static final String DELEGATE_XF_OPT_STRING = "t";
    public static final String DELEGATE_XF_OPT_STRING_LONG = "delegateXf";
    public final Option DELEGATE_XF_OPT =
        OptionBuilder.withLongOpt(DELEGATE_XF_OPT_STRING_LONG)
                     .create(DELEGATE_XF_OPT_STRING);

    public static final String DELEGATE_OPT_STRING = "u";
    public static final String DELEGATE_OPT_STRING_LONG = "delegate";
    public final Option DELEGATE_OPT =
        OptionBuilder.hasArg().withLongOpt(DELEGATE_OPT_STRING_LONG)
                              .create(DELEGATE_OPT_STRING);

    public static final String DELEGATE_TIME_OPT_STRING = "q";
    public static final String DELEGATE_TIME_OPT_STRING_LONG = "delegatetime";
    public final Option DELEGATE_TIME_OPT =
        OptionBuilder.hasArg().withLongOpt(DELEGATE_TIME_OPT_STRING_LONG)
                              .create(DELEGATE_TIME_OPT_STRING);

    public static final String REQUEST_OPT_STRING = "r";
    public static final String REQUEST_OPT_STRING_LONG = "request";
    public final Option REQUEST_OPT =
        OptionBuilder.hasArg().withLongOpt(REQUEST_OPT_STRING_LONG)
                              .create(REQUEST_OPT_STRING);

    public static final String FILE_OPT_STRING = "file";
    public final Option FILE_OPT =
        OptionBuilder.hasArg().withLongOpt(FILE_OPT_STRING).create();

    public static final String GROUPFILE_OPT_STRING = "groupfile";
    public final Option GROUPFILE_OPT =
        OptionBuilder.hasArg().withLongOpt(GROUPFILE_OPT_STRING).create();

    public static final String NUMNODES_OPT_STRING = "n";
    public static final String NUMNODES_OPT_STRING_LONG = "numnodes";
    public final Option NUMNODES_OPT =
        OptionBuilder.hasArg().withLongOpt(NUMNODES_OPT_STRING_LONG)
                              .create(NUMNODES_OPT_STRING);

    public static final String TRASH_OPT_STRING = "trash-at-shutdown";
    public final Option TRASH_OPT =
        OptionBuilder.withLongOpt(TRASH_OPT_STRING).create();

    public static final String NONOTIFY_OPT_STRING = "nosubscriptions";
    public final Option NONOTIFY_OPT =
        OptionBuilder.withLongOpt(NONOTIFY_OPT_STRING).create();

    /* unused currently
    public static final String VERYTERSENOTIFY_OPT_STRING = "veryterse-group-subscribe";
    public final Option VERYTERSENOTIFY_OPT =
            OptionBuilder.hasArg().withLongOpt(VERYTERSENOTIFY_OPT_STRING)
                              .create();
    */

    public static final String NOAUTODESTROY_OPT_STRING = "no-auto-destroy";
    public final Option NOAUTODESTROY_OPT =
        OptionBuilder.withLongOpt(NOAUTODESTROY_OPT_STRING).create();

    public static final String DEPLOY_DURATION_OPT_STRING = "deploy-duration";
    public final Option DEPLOY_DURATION_OPT =
        OptionBuilder.hasArg().withLongOpt(DEPLOY_DURATION_OPT_STRING).create();

    public static final String DEPLOY_STATE_OPT_STRING = "deploy-state";
    public final Option DEPLOY_STATE_OPT =
        OptionBuilder.hasArg().withLongOpt(DEPLOY_STATE_OPT_STRING).create();

    public static final String DEPLOY_MEMORY_OPT_STRING = "deploy-mem";
    public final Option DEPLOY_MEMORY_OPT =
        OptionBuilder.hasArg().withLongOpt(DEPLOY_MEMORY_OPT_STRING).create();

    public static final String EXIT_STATE_OPT_STRING = "exit-state";
    public final Option EXIT_STATE_OPT =
        OptionBuilder.hasArg().withLongOpt(EXIT_STATE_OPT_STRING).create();

    public static final String POLL_DELAY_OPT_STRING = "poll-delay";
    public final Option POLL_DELAY_OPT =
        OptionBuilder.hasArg().withLongOpt(POLL_DELAY_OPT_STRING).create();

    public static final String POLL_MAXTHREADS_OPT_STRING = "poll-maxthreads";
    public final Option POLL_MAXTHREADS_OPT =
        OptionBuilder.hasArg().withLongOpt(POLL_MAXTHREADS_OPT_STRING).create();

    public static final String RPQUERY_OPT_STRING = "rpquery";
    public final Option RPQUERY_OPT =
        OptionBuilder.withLongOpt(RPQUERY_OPT_STRING).create();

    public static final String DRYRUN_OPT_STRING = "dryrun";
    public final Option DRYRUN_OPT =
        OptionBuilder.withLongOpt(DRYRUN_OPT_STRING).create();

    public static final String DISPLAY_NAME_OPT_STRING = "displayname";
    public final Option DISPLAY_NAME_OPT =
        OptionBuilder.hasArg().withLongOpt(DISPLAY_NAME_OPT_STRING).create();

    public static final String GROUP_PRINT_OPT_STRING = "groupprint";
    public final Option GROUP_PRINT_OPT =
        OptionBuilder.withLongOpt(GROUP_PRINT_OPT_STRING).create();

    public static final String LISTENER_OVERRIDE_OPT_STRING = "override-listener-address";
    public final Option LISTENER_OVERRIDE_OPT =
        OptionBuilder.hasArg().withLongOpt(LISTENER_OVERRIDE_OPT_STRING).create();

    public static final String SSHFILE_OPT_STRING = "sshfile";
    public final Option SSHFILE_OPT =
        OptionBuilder.hasArg().withLongOpt(SSHFILE_OPT_STRING).create();

    public static final String SSHHOSTS_OPT_STRING = "sshhosts";
    public final Option SSHHOSTS_OPT =
        OptionBuilder.hasArg().withLongOpt(SSHHOSTS_OPT_STRING).create();

    public static final String ADJUST_SSHHOSTS_OPT_STRING = "adjusthosts";
    public final Option ADJUST_SSHHOSTS_OPT =
        OptionBuilder.hasArg().withLongOpt(ADJUST_SSHHOSTS_OPT_STRING).create();

    public static final String SAVE_TARGET_OPT_STRING = "save-target";
    public final Option SAVE_TARGET_OPT =
        OptionBuilder.hasArg().withLongOpt(SAVE_TARGET_OPT_STRING).create();

    public static final String MD_USERDATA_OPT_STRING = "mdUserdata";
    public final Option MD_USERDATA_OPT =
        OptionBuilder.hasArg().withLongOpt(MD_USERDATA_OPT_STRING).create();

    // ensemble related:

    public static final String ENSEMBLE_NEW_OPT_STRING = "new-ensemble";
    public final Option ENSEMBLE_NEW_OPT =
        OptionBuilder.hasArg().withLongOpt(ENSEMBLE_NEW_OPT_STRING).create();

    public static final String ENSEMBLE_JOIN_OPT_STRING = "join-ensemble";
    public final Option ENSEMBLE_JOIN_OPT =
        OptionBuilder.hasArg().withLongOpt(ENSEMBLE_JOIN_OPT_STRING).create();

    public static final String ENSEMBLE_LAST_OPT_STRING = "last-in-ensemble";
    public final Option ENSEMBLE_LAST_OPT =
        OptionBuilder.withLongOpt(ENSEMBLE_LAST_OPT_STRING).create();

    public static final String ENSEMBLE_DONE_OPT_STRING = "ens-done";
    public final Option ENSEMBLE_DONE_OPT =
        OptionBuilder.withLongOpt(ENSEMBLE_DONE_OPT_STRING).create();

    public static final String ENSEMBLE_MONITOR_OPT_STRING = "ens-monitor";
    public final Option ENSEMBLE_MONITOR_OPT =
        OptionBuilder.withLongOpt(ENSEMBLE_MONITOR_OPT_STRING).create();

    public static final String REPORTDIR_OPT_STRING = "reportdir";
    public final Option REPORTDIR_OPT =
        OptionBuilder.hasArg().withLongOpt(REPORTDIR_OPT_STRING).create();

    // context broker related:

    public static final String CTX_MONITOR_OPT_STRING = "ctx-monitor";
    public final Option CTX_MONITOR_OPT =
        OptionBuilder.withLongOpt(CTX_MONITOR_OPT_STRING).create();

    public static final String CTX_NO_MORE_INJECTIONS_OPT_STRING =
                                                    "ctx-no-more-injections";
    public final Option CTX_NO_MORE_INJECTIONS_OPT =
        OptionBuilder.withLongOpt(CTX_NO_MORE_INJECTIONS_OPT_STRING).create();

    public static final String CTX_DATA_OPT_STRING = "ctx-data";
    public final Option CTX_DATA_OPT =
        OptionBuilder.withLongOpt(CTX_DATA_OPT_STRING).create();

    public static final String CTX_CREATE_OPT_STRING = "ctx-create";
    public final Option CTX_CREATE_OPT =
        OptionBuilder.withLongOpt(CTX_CREATE_OPT_STRING).create();

    public static final String CTX_PENDING_OPT_STRING = "ctx-pending";
    public final Option CTX_PENDING_OPT =
        OptionBuilder.withLongOpt(CTX_PENDING_OPT_STRING).create();

    public static final String CTX_CREATE_INJECTABLE_OPT_STRING =
                                                    "ctx-create-injectable";
    public final Option CTX_CREATE_INJECTABLE_OPT =
        OptionBuilder.withLongOpt(CTX_CREATE_INJECTABLE_OPT_STRING).create();

    public static final String CTX_CONTACT_PATH_OPT_STRING =
                                                    "ctx-contact-xml";
    public final Option CTX_CONTACT_PATH_OPT =
        OptionBuilder.hasArg().withLongOpt(CTX_CONTACT_PATH_OPT_STRING).create();

    public static final String CTX_DATANAME_OPT_STRING = "dataname";
    public final Option CTX_DATANAME_OPT =
        OptionBuilder.hasArg().withLongOpt(CTX_DATANAME_OPT_STRING).create();
    
    /* undocumented options: */

    public static final String DEBUGGER_HANG_OPT_STRING = "debuggerhang";
    public final Option DEBUGGER_HANG_OPT =
        OptionBuilder.withLongOpt(DEBUGGER_HANG_OPT_STRING).create();

    // (...)
    public static final String EPR_ID_DIR_OPT_STRING = "epr-ip-dir";
    public final Option EPR_ID_DIR_OPT =
        OptionBuilder.hasArg().withLongOpt(EPR_ID_DIR_OPT_STRING).create();

    // because of static Option in wsrf BaseClient (really about time we
    // got rid of using that altogether)
    public static final String EPRFILE2_OPT_STRING = "eprFile2";
    public final Option EPRFILE2_OPT =
        OptionBuilder.hasArg().withLongOpt(EPRFILE2_OPT_STRING).create();

    // because of static Option in wsrf BaseClient (really about time we
    // got rid of using that altogether)
    public static final String SERVICE2_OPT_STRING = "service2";
    public final Option SERVICE2_OPT =
        OptionBuilder.hasArg().withLongOpt(SERVICE2_OPT_STRING).create();

    // an undocumented option that makes the java client send the ctx agent's
    // 'retrieve' WS operation -- mostly this allows the developer to quickly
    // see a correct SOAP message on the wire.  Takes an argument, the path
    // to cluster ctx file that an agent will get in user data.
    public static final String IMPERSONATE_CTX_AGENT_OPT_STRING =
            "impersonate-ctx-agent";
    public final Option IMPERSONATE_CTX_AGENT_OPT =
        OptionBuilder.hasArg().withLongOpt(IMPERSONATE_CTX_AGENT_OPT_STRING).create();


    public static final String SSHHOSTSDIR_OPT_STRING = "sshhosts-dir";
    public final Option SSHHOSTSDIR_OPT =
        OptionBuilder.hasArg().withLongOpt(SSHHOSTSDIR_OPT_STRING).create();

    /* public final String TS01PATH_OPT_STRING = "ts01path";
    public final Option TS01PATH_OPT =
        OptionBuilder.hasArg().withLongOpt(TS01PATH_OPT_STRING).create(); */

    
    public final Option[] ALL_ENABLED_OPTIONS = {this.EXTRAHELP_OPT,
                                                 this.FACTORYRP_OPT,
                                                 this.DEPLOY_OPT,
                                                 this.DESTROY_OPT,
                                                 this.PAUSE_OPT,
                                                 this.START_OPT,
                                                 this.SHUTDOWN_OPT,
                                                 this.SHUTDOWN_SAVE_OPT,
                                                 this.REBOOT_OPT,
                                                 this.SUBSCRIBE_OPT,
                                                 this.METADATA_OPT,
                                                 this.OPTIONAL_OPT,
                                                 this.DELEGATE_OPT,
                                                 this.DELEGATE_XF_OPT,
                                                 this.DELEGATE_TIME_OPT,
                                                 this.FILE_OPT,
                                                 this.GROUPFILE_OPT,
                                                 this.REQUEST_OPT,
                                                 this.NONOTIFY_OPT,
                                                 this.DEPLOY_DURATION_OPT,
                                                 this.DEPLOY_STATE_OPT,
                                                 this.DEPLOY_MEMORY_OPT,
                                                 this.EXIT_STATE_OPT,
                                                 this.POLL_DELAY_OPT,
                                                 this.POLL_MAXTHREADS_OPT,
                                                 this.RPQUERY_OPT,
                                                 this.DRYRUN_OPT,
                                                 this.DISPLAY_NAME_OPT,
                                                 this.GROUP_PRINT_OPT,
                                                 this.LISTENER_OVERRIDE_OPT,
                                                 this.SSHFILE_OPT,
                                                 this.SSHHOSTS_OPT,
                                                 this.MD_USERDATA_OPT,
                                                 this.ADJUST_SSHHOSTS_OPT,
                                                 this.SAVE_TARGET_OPT,
                                                 this.NOAUTODESTROY_OPT,
                                                 //this.VERYTERSENOTIFY_OPT,
                                                 this.NUMNODES_OPT,
                                                 this.TRASH_OPT,
                                                 this.ENSEMBLE_JOIN_OPT,
                                                 this.ENSEMBLE_LAST_OPT,
                                                 this.ENSEMBLE_NEW_OPT,
                                                 this.ENSEMBLE_DONE_OPT,
                                                 this.ENSEMBLE_MONITOR_OPT,
                                                 this.CTX_NO_MORE_INJECTIONS_OPT,
                                                 this.CTX_CREATE_OPT,
                                                 this.CTX_PENDING_OPT,
                                                 this.CTX_CREATE_INJECTABLE_OPT,
                                                 this.CTX_CONTACT_PATH_OPT,
                                                 this.CTX_DATA_OPT,
                                                 this.CTX_DATANAME_OPT,
                                                 this.CTX_MONITOR_OPT,
                                                 this.REPORTDIR_OPT,
                                                 this.EPR_ID_DIR_OPT,
                                                 this.EPRFILE2_OPT,
                                                 this.SERVICE2_OPT,
                                                 this.SSHHOSTSDIR_OPT,
                                                 this.IMPERSONATE_CTX_AGENT_OPT,
                                                 this.DEBUGGER_HANG_OPT};

    // via base client
    public static final String DEBUG_OPT_STRING = "debug";
    public static final String EPRFILE_OPT_STRING = "eprFile";
    public static final String SERVICE_OPT_STRING = "service";
    public static final String AUTHORIZATION_OPT_STRING = "authorization";

}
