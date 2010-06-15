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

import org.globus.workspace.common.print.Print;
import org.apache.commons.cli.CommandLine;

public class AllArguments {

    // --------------------------------------------------------------------------
    // GIVEN VALUES
    // --------------------------------------------------------------------------

    // only booleans (flag present) and Strings (flag argument) are possible

    public boolean mode_factoryRpQuery;
    public boolean mode_deploy;
    public boolean mode_destroy;
    public boolean mode_pause;
    public boolean mode_shutdown;
    public boolean mode_shutdown_save;
    public boolean mode_start;
    public boolean mode_reboot;
    public boolean mode_subscribe;
    public boolean mode_rpquery;
    public boolean mode_doneEnsemble;
    public boolean mode_monitorEnsemble;
    public boolean mode_monitorContext;
    public boolean mode_noMoreContextInjections;
    public boolean mode_ctxPending;
    public boolean mode_injectContextData;
    public boolean mode_createContext;
    public boolean mode_createInjectableContext;
    public boolean mode_impersonateContextAgent;

    // help modes
    public boolean mode_help;
    public boolean mode_extraUsage;

    public String shortName;
    public String listenerOverride;
    public String delegationFactoryUrl;
    public String targetServiceUrl;

    public boolean lastInEnsemble;
    public boolean trashAtShutdown;
    public boolean dryrun;
    public boolean delegationXferCredToo;
    public boolean subscriptions = true;
    public boolean autodestroy = true;
    public boolean printLikeGroup;

    public String metadataPath;
    public String eprFile;
    public String groupEprFile;
    public String joinEnsembleEprFile;
    public String newEnsembleEprFile;
    public String contextDataInjectFile;
    public String contextDataInjectName;
    public String ctxContactXmlPath;
    public String reportDir;
    public String depRequestFilePath;
    public String optionalParametersPath;
    public String sshKeyPath;
    public String sshHostsPath;
    public String sshHostsDirPath;
    public String adjustSshHostsList;
    public String saveTarget;
    public String mdUserDataPath;
    public String clusterForImpersonationPath;
    public String eprIdDir;

    public String exitStateString;
    public String veryTerseNotifyStateString; // unused currently
    public String pollDelayString;
    public String pollMaxThreadsString;
    public String delegationLifetimeString;
    public String deploy_MemoryString;
    public String deploy_DurationString;
    public String deploy_NumNodesString;
    public String deploy_StateString;

    // -------------------------------------------------------------------------
    // INTAKE
    // -------------------------------------------------------------------------

    private final Print pr;

    public AllArguments(Print print) {
        if (print == null) {
            throw new IllegalArgumentException("print may not be null");
        }
        this.pr = print;
    }

    public void intake(CommandLine line) {
        
        // note debug/help were already recognized and configured, keeping this
        // block here for gotCmdLine logging
        if (line.hasOption("d")) {
            this.gotCmdLine("debug", "enabled");
        }

        if (line.hasOption("h")) {
            this.gotCmdLine("help", "enabled");
        }

        this.intakeActions(line);
        this.intakeNonactions(line);

        // this will be gone when moving away from wsrf BaseClient
        if (line.hasOption("s")) {
            this.targetServiceUrl = line.getOptionValue("s");
            this.gotCmdLine("service", this.targetServiceUrl);
        }
    }

    private void gotCmdLine(String optionName, String value) {
        this.pr.dbg("[*] Received '" + optionName + "' from command line, " +
                        "value: '" + value + "'");
    }
    
    private void intakeActions(CommandLine line) {

        if (line.hasOption("h")) {
            this.mode_help = true;
            this.gotCmdLine("help", "enabled");
        }

        if (line.hasOption(Opts.EXTRAHELP_OPT_STRING)) {
            this.mode_extraUsage = true;
            this.gotCmdLine(Opts.EXTRAHELP_OPT_STRING, "enabled");
        }

        if (line.hasOption(Opts.FACTORYRP_OPT_STRING)) {
            this.mode_factoryRpQuery = true;
            this.gotCmdLine(Opts.FACTORYRP_OPT_STRING, "enabled");
        }

        if (line.hasOption(Opts.DEPLOY_OPT_STRING)) {
            this.mode_deploy = true;
            this.gotCmdLine(Opts.DEPLOY_OPT_STRING, "enabled");
        }

        if (line.hasOption(Opts.DESTROY_OPT_STRING)) {
            this.mode_destroy = true;
            this.gotCmdLine(Opts.DESTROY_OPT_STRING, "enabled");
        }

        if (line.hasOption(Opts.ENSEMBLE_DONE_OPT_STRING)) {
            this.mode_doneEnsemble = true;
            this.gotCmdLine(Opts.ENSEMBLE_DONE_OPT_STRING, "enabled");
        }

        if (line.hasOption(Opts.ENSEMBLE_MONITOR_OPT_STRING)) {
            this.mode_monitorEnsemble = true;
            this.gotCmdLine(Opts.ENSEMBLE_MONITOR_OPT_STRING, "enabled");
        }

        if (line.hasOption(Opts.CTX_MONITOR_OPT_STRING)) {
            this.mode_monitorContext = true;
            this.gotCmdLine(Opts.CTX_MONITOR_OPT_STRING, "enabled");
        }

        if (line.hasOption(Opts.CTX_DATA_OPT_STRING)) {
            this.mode_injectContextData = true;
            this.gotCmdLine(Opts.CTX_DATA_OPT_STRING, "enabled");
        }

        if (line.hasOption(Opts.CTX_CREATE_OPT_STRING)) {
            this.mode_createContext = true;
            this.gotCmdLine(Opts.CTX_CREATE_OPT_STRING, "enabled");
        }

        if (line.hasOption(Opts.CTX_CREATE_INJECTABLE_OPT_STRING)) {
            this.mode_createInjectableContext = true;
            this.gotCmdLine(Opts.CTX_CREATE_INJECTABLE_OPT_STRING, "enabled");
        }

        if (line.hasOption(Opts.CTX_NO_MORE_INJECTIONS_OPT_STRING)) {
            this.mode_noMoreContextInjections = true;
            this.gotCmdLine(Opts.CTX_NO_MORE_INJECTIONS_OPT_STRING, "enabled");
        }

        if (line.hasOption(Opts.CTX_PENDING_OPT_STRING)) {
            this.mode_ctxPending = true;
            this.gotCmdLine(Opts.CTX_PENDING_OPT_STRING, "enabled");
        }

        if (line.hasOption(Opts.IMPERSONATE_CTX_AGENT_OPT_STRING)) {
            this.mode_impersonateContextAgent = true;
            this.gotCmdLine(Opts.IMPERSONATE_CTX_AGENT_OPT_STRING, "enabled");
        }

        if (line.hasOption(Opts.PAUSE_OPT_STRING)) {
            this.mode_pause = true;
            this.gotCmdLine(Opts.PAUSE_OPT_STRING, "enabled");
        }

        if (line.hasOption(Opts.SHUTDOWN_SAVE_OPT_STRING)) {
            this.mode_shutdown_save = true;
            this.gotCmdLine(Opts.SHUTDOWN_SAVE_OPT_STRING, "enabled");
        }

        if (line.hasOption(Opts.SHUTDOWN_OPT_STRING)) {
            this.mode_shutdown = true;
            this.gotCmdLine(Opts.SHUTDOWN_OPT_STRING, "enabled");
        }

        if (line.hasOption(Opts.START_OPT_STRING)) {
            this.mode_start = true;
            this.gotCmdLine(Opts.START_OPT_STRING, "enabled");
        }

        if (line.hasOption(Opts.REBOOT_OPT_STRING)) {
            this.mode_reboot = true;
            this.gotCmdLine(Opts.REBOOT_OPT_STRING, "enabled");
        }

        if (line.hasOption(Opts.SUBSCRIBE_OPT_STRING)) {
            this.mode_subscribe = true;
            this.gotCmdLine(Opts.SUBSCRIBE_OPT_STRING, "enabled");
        }

        if (line.hasOption(Opts.RPQUERY_OPT_STRING)) {
            this.mode_rpquery = true;
            this.gotCmdLine(Opts.RPQUERY_OPT_STRING, "enabled");
        }
    }

    private void intakeNonactions(CommandLine line) {

        if (line.hasOption(Opts.DISPLAY_NAME_OPT_STRING)) {
            this.shortName =
                    line.getOptionValue(Opts.DISPLAY_NAME_OPT_STRING);
            this.gotCmdLine(Opts.DISPLAY_NAME_OPT_STRING,
                            this.shortName);

        }

        if (line.hasOption(Opts.LISTENER_OVERRIDE_OPT_STRING)) {
            this.listenerOverride =
                   line.getOptionValue(Opts.LISTENER_OVERRIDE_OPT_STRING);
            this.gotCmdLine(Opts.LISTENER_OVERRIDE_OPT_STRING,
                            this.listenerOverride);
        }

        if (line.hasOption(Opts.NOAUTODESTROY_OPT_STRING)) {
            this.autodestroy = false;
            this.gotCmdLine(Opts.NOAUTODESTROY_OPT_STRING,
                            "enabled (autodestroy disabled)");
        }

        if (line.hasOption(Opts.METADATA_OPT_STRING)) {
            this.metadataPath =
                   line.getOptionValue(Opts.METADATA_OPT_STRING);
            this.gotCmdLine(Opts.METADATA_OPT_STRING_LONG,
                            this.metadataPath);
        }

        if (line.hasOption(Opts.FILE_OPT_STRING)) {
            this.eprFile =
                    line.getOptionValue(Opts.FILE_OPT_STRING);
            this.contextDataInjectFile = this.eprFile;
            this.gotCmdLine(Opts.FILE_OPT_STRING,
                            this.eprFile);
        }

        if (line.hasOption(Opts.CTX_DATANAME_OPT_STRING)) {
            this.contextDataInjectName =
                    line.getOptionValue(Opts.CTX_DATANAME_OPT_STRING);
            this.gotCmdLine(Opts.CTX_DATANAME_OPT_STRING,
                            this.contextDataInjectName);
        }

        if (line.hasOption(Opts.SSHFILE_OPT_STRING)) {
            this.sshKeyPath =
                        line.getOptionValue(Opts.SSHFILE_OPT_STRING);
            this.gotCmdLine(Opts.SSHFILE_OPT_STRING,
                            this.sshKeyPath);
        }

        if (line.hasOption(Opts.SSHHOSTS_OPT_STRING)) {
            this.sshHostsPath =
                        line.getOptionValue(Opts.SSHHOSTS_OPT_STRING);
            this.gotCmdLine(Opts.SSHHOSTS_OPT_STRING,
                            this.sshHostsPath);
        }

        if (line.hasOption(Opts.SSHHOSTSDIR_OPT_STRING)) {
            this.sshHostsDirPath =
                        line.getOptionValue(Opts.SSHHOSTSDIR_OPT_STRING);
            this.gotCmdLine(Opts.SSHHOSTSDIR_OPT_STRING,
                            this.sshHostsDirPath);
        }

        if (line.hasOption(Opts.ADJUST_SSHHOSTS_OPT_STRING)) {
            this.adjustSshHostsList =
                        line.getOptionValue(Opts.ADJUST_SSHHOSTS_OPT_STRING);
            this.gotCmdLine(Opts.ADJUST_SSHHOSTS_OPT_STRING,
                            this.adjustSshHostsList);
        }

        if (line.hasOption(Opts.SAVE_TARGET_OPT_STRING)) {
            this.saveTarget =
                        line.getOptionValue(Opts.SAVE_TARGET_OPT_STRING);
            this.gotCmdLine(Opts.SAVE_TARGET_OPT_STRING,
                            this.saveTarget);
        }

        if (line.hasOption(Opts.MD_USERDATA_OPT_STRING)) {
            this.mdUserDataPath =
                        line.getOptionValue(Opts.MD_USERDATA_OPT_STRING);
            this.gotCmdLine(Opts.MD_USERDATA_OPT_STRING,
                            this.mdUserDataPath);
        }

        if (line.hasOption(Opts.EPR_ID_DIR_OPT_STRING)) {
            this.eprIdDir = line.getOptionValue(Opts.EPR_ID_DIR_OPT_STRING);
            this.gotCmdLine(Opts.EPR_ID_DIR_OPT_STRING,
                            this.eprIdDir);
        }

        if (line.hasOption(Opts.EXIT_STATE_OPT_STRING)) {
            this.exitStateString =
                    line.getOptionValue(Opts.EXIT_STATE_OPT_STRING);
            this.gotCmdLine(Opts.EXIT_STATE_OPT_STRING,
                            this.exitStateString);
        }

        if (line.hasOption(Opts.POLL_DELAY_OPT_STRING)) {
            this.pollDelayString =
                    line.getOptionValue(Opts.POLL_DELAY_OPT_STRING);
            this.gotCmdLine(Opts.POLL_DELAY_OPT_STRING,
                            this.pollDelayString);
        }

        if (line.hasOption(Opts.POLL_MAXTHREADS_OPT_STRING)) {
            this.pollMaxThreadsString =
                    line.getOptionValue(Opts.POLL_MAXTHREADS_OPT_STRING);
            this.gotCmdLine(Opts.POLL_MAXTHREADS_OPT_STRING,
                            this.pollMaxThreadsString);
        }

        if (line.hasOption(Opts.GROUPFILE_OPT_STRING)) {
            this.groupEprFile =
                    line.getOptionValue(Opts.GROUPFILE_OPT_STRING);
            this.gotCmdLine(Opts.GROUPFILE_OPT_STRING,
                            this.groupEprFile);
        }

        if (line.hasOption(Opts.ENSEMBLE_JOIN_OPT_STRING)) {
            this.joinEnsembleEprFile =
                    line.getOptionValue(Opts.ENSEMBLE_JOIN_OPT_STRING);
            this.gotCmdLine(Opts.ENSEMBLE_JOIN_OPT_STRING,
                            this.joinEnsembleEprFile);
        }

        if (line.hasOption(Opts.ENSEMBLE_NEW_OPT_STRING)) {
            this.newEnsembleEprFile =
                    line.getOptionValue(Opts.ENSEMBLE_NEW_OPT_STRING);
            this.gotCmdLine(Opts.ENSEMBLE_NEW_OPT_STRING,
                            this.newEnsembleEprFile);
        }

        if (line.hasOption(Opts.ENSEMBLE_LAST_OPT_STRING)) {
            this.lastInEnsemble = true;
            this.gotCmdLine(Opts.ENSEMBLE_LAST_OPT_STRING, "enabled");
        }

        if (line.hasOption(Opts.REPORTDIR_OPT_STRING)) {
            this.reportDir =
                    line.getOptionValue(Opts.REPORTDIR_OPT_STRING);
            this.gotCmdLine(Opts.REPORTDIR_OPT_STRING,
                            this.reportDir);
        }

        if (line.hasOption(Opts.REQUEST_OPT_STRING)) {
            this.depRequestFilePath =
                    line.getOptionValue(Opts.REQUEST_OPT_STRING);
            this.gotCmdLine(Opts.REQUEST_OPT_STRING_LONG,
                            this.depRequestFilePath);
        }

        if (line.hasOption(Opts.DEPLOY_DURATION_OPT_STRING)) {
            this.deploy_DurationString =
                    line.getOptionValue(Opts.DEPLOY_DURATION_OPT_STRING);
            this.gotCmdLine(Opts.DEPLOY_DURATION_OPT_STRING,
                            this.deploy_DurationString);
        }

        if (line.hasOption(Opts.DEPLOY_MEMORY_OPT_STRING)) {
            this.deploy_MemoryString =
                    line.getOptionValue(Opts.DEPLOY_MEMORY_OPT_STRING);
            this.gotCmdLine(Opts.DEPLOY_MEMORY_OPT_STRING,
                            this.deploy_MemoryString);
        }

        if (line.hasOption(Opts.TRASH_OPT_STRING)) {
            this.trashAtShutdown = true;
            this.gotCmdLine(Opts.TRASH_OPT_STRING, "enabled");
        }

        if (line.hasOption(Opts.OPTIONAL_OPT_STRING)) {
            this.optionalParametersPath =
                    line.getOptionValue(Opts.OPTIONAL_OPT_STRING);
            this.gotCmdLine(Opts.OPTIONAL_OPT_STRING_LONG,
                            this.optionalParametersPath);
        }

        if (line.hasOption(Opts.DELEGATE_OPT_STRING)) {
            this.delegationFactoryUrl =
                    line.getOptionValue(Opts.DELEGATE_OPT_STRING);
            this.gotCmdLine(Opts.DELEGATE_OPT_STRING_LONG,
                            this.delegationFactoryUrl);
        }

        if (line.hasOption(Opts.DELEGATE_TIME_OPT_STRING)) {
            this.delegationLifetimeString =
                    line.getOptionValue(Opts.DELEGATE_TIME_OPT_STRING);
            this.gotCmdLine(Opts.DELEGATE_TIME_OPT_STRING_LONG,
                            this.delegationLifetimeString);
        }

        if (line.hasOption(Opts.DELEGATE_XF_OPT_STRING)) {
            this.delegationXferCredToo = true;
            this.gotCmdLine(Opts.DELEGATE_XF_OPT_STRING, "enabled");
        }

        if (line.hasOption(Opts.NONOTIFY_OPT_STRING)) {
            this.subscriptions = false;
            this.gotCmdLine(Opts.NONOTIFY_OPT_STRING,
                            "enabled (subscriptions disabled)");
        }

        if (line.hasOption(Opts.CTX_CONTACT_PATH_OPT_STRING)) {
            this.ctxContactXmlPath =
                    line.getOptionValue(Opts.CTX_CONTACT_PATH_OPT_STRING);
            this.gotCmdLine(Opts.CTX_CONTACT_PATH_OPT_STRING,
                            this.ctxContactXmlPath);
        }

        // unused currently
        /*
        if (line.hasOption(Opts.VERYTERSENOTIFY_OPT_STRING)) {
            this.veryTerseNotifyStateString =
                    line.getOptionValue(Opts.VERYTERSENOTIFY_OPT_STRING);
            this.gotCmdLine(Opts.VERYTERSENOTIFY_OPT_STRING,
                            this.veryTerseNotifyStateString);
        }
        */

        if (line.hasOption(Opts.NUMNODES_OPT_STRING)) {
            this.deploy_NumNodesString =
                    line.getOptionValue(Opts.NUMNODES_OPT_STRING);
            this.gotCmdLine(Opts.NUMNODES_OPT_STRING,
                            this.deploy_NumNodesString);
        }

        if (line.hasOption(Opts.DRYRUN_OPT_STRING)) {
            this.dryrun = true;
            this.gotCmdLine(Opts.DRYRUN_OPT_STRING, "enabled");
        }

        if (line.hasOption(Opts.GROUP_PRINT_OPT_STRING)) {
            this.printLikeGroup = true;
            this.gotCmdLine(Opts.GROUP_PRINT_OPT_STRING, "enabled");
        }

        if (line.hasOption(Opts.IMPERSONATE_CTX_AGENT_OPT_STRING)) {
            this.clusterForImpersonationPath =
                    line.getOptionValue(Opts.IMPERSONATE_CTX_AGENT_OPT_STRING);
            this.gotCmdLine(Opts.IMPERSONATE_CTX_AGENT_OPT_STRING,
                            this.clusterForImpersonationPath);
        }
    }
}
