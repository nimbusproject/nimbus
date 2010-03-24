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

import org.apache.axis.message.addressing.EndpointReferenceType;
import org.apache.axis.types.URI;
import org.apache.commons.cli.ParseException;
import org.globus.gsi.GlobusCredential;
import org.globus.workspace.client_common.BaseClient;
import org.globus.workspace.client_core.ExecutionProblem;
import org.globus.workspace.client_core.ExitNow;
import org.globus.workspace.client_core.ParameterProblem;
import org.globus.workspace.client_core.actions.Status_QueryAll;
import org.globus.workspace.client_core.print.PrCodes;
import org.globus.workspace.client_core.repr.Workspace;
import org.globus.workspace.cloud.client.cluster.ClusterMember;
import org.globus.workspace.cloud.client.cluster.ClusterUtil;
import org.globus.workspace.cloud.client.cluster.KnownHostsTask;
import org.globus.workspace.cloud.client.security.CertUtil;
import org.globus.workspace.cloud.client.security.SecurityPrinter;
import org.globus.workspace.cloud.client.security.TrustedCAs;
import org.globus.workspace.cloud.client.util.CloudClientUtil;
import org.globus.workspace.cloud.client.util.DeploymentXMLUtil;
import org.globus.workspace.cloud.client.util.ExecuteUtil;
import org.globus.workspace.cloud.client.util.FileListing;
import org.globus.workspace.cloud.client.util.HistoryUtil;
import org.globus.workspace.cloud.client.util.MetadataXMLUtil;
import org.globus.workspace.common.SecurityUtil;
import org.globus.workspace.common.client.CLIUtils;
import org.globus.workspace.common.client.CommonPrint;
import org.globus.workspace.common.print.Print;
import org.globus.workspace.common.print.PrintOpts;
import org.globus.workspace.status.client.WorkspaceStatusClient;
import org.globus.wsrf.impl.security.authorization.HostAuthorization;
import org.globus.wsrf.impl.security.authorization.IdentityAuthorization;
import org.globus.wsrf.utils.AddressingUtils;
import org.nimbustools.ctxbroker.generated.gt4_0.description.Cloudcluster_Type;
import org.nimbustools.messaging.gt4_0.common.CommonUtil;
import org.nimbustools.messaging.gt4_0.generated.metadata.VirtualWorkspace_Type;
import org.nimbustools.messaging.gt4_0.generated.negotiable.WorkspaceDeployment_Type;
import org.nimbustools.messaging.gt4_0.generated.status.WorkspaceStatusFault;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class CloudClient {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    public static final int SUCCESS_EXIT_CODE = 0;
    public static final int COMMAND_LINE_EXIT_CODE = 1;
    public static final int APPLICATION_EXIT_CODE = 2;
    public static final int UNKNOWN_ERROR_EXIT_CODE = 3;
    
    public static final String credURL =
            "http://www.globus.org/toolkit/docs/4.0/security/prewsaa/" +
                    "Pre_WS_AA_Public_Interfaces.html#prewsaa-env-credentials";
    

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private AllArgs args;
    
    // set if handle is a cluster handle
    private boolean isClusterHandle;

    private ExecuteUtil executeUtil = new ExecuteUtil();

    private final Print print;

    /* derived */
    private String remoteUserBaseURLString;
    private String remoteUserBaseDir;
    private GlobusCredential proxyUsed;
    private String specificEPRpath;
    private String[] trustedCertDirs;
    private String workspaceFactoryURL;
    private EndpointReferenceType statusServiceEPR;
    private String transferSourceURL;
    private String transferTargetURL;
    private String _transferTargetURL; // for printing only
    private String newUnpropTargetURL;
    private String deleteURL;
    private ClusterMember[] clusterMembers; 
	private URI kernelURI;

    
    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    /**
     * @param pr must be non-null
     */
    public CloudClient(Print pr) {
        if (pr == null) {
            throw new IllegalArgumentException("print may not be null");
        }
        this.print = pr;
    }

    
    // -------------------------------------------------------------------------
    // ENTRY POINTS
    // -------------------------------------------------------------------------

    // null means all
    public static int[] getOptInPrCodes() {
        final int[] optOuts = { PrCodes.OPTIONALPARAM__FILE_READ,
                                PrCodes.DEPREQ__FILE_READ,
                                PrCodes.METADATA__FILE_READ,
                                PrCodes.SSH__FILE_READ,
                                PrCodes.LISTENER_STATECHANGE__INSTANCE_STATE_CHANGE,
                                PrCodes.CREATE__EPRFILE_WRITES,
                                PrCodes.CREATE__INSTANCE_ID_PRINT,
                                PrCodes.CREATE__INSTANCE_CREATING_INIITIAL_DURATION,
                                PrCodes.CREATE__INSTANCE_CREATING_NET_ASSOCIATION,
                                PrCodes.CREATE__INSTANCE_CREATING_NET_BROADCAST,
                                PrCodes.CREATE__INSTANCE_CREATING_NET_GATEWAY,
                                PrCodes.CREATE__INSTANCE_CREATING_NET_MAC,
                                PrCodes.CREATE__INSTANCE_CREATING_NET_MASK,
                                PrCodes.CREATE__INSTANCE_CREATING_NET_NETWORK,
                                PrCodes.CREATE__INSTANCE_CREATING_NET_NAME,
                                PrCodes.CREATE__ENSEMBLE_ID_PRINT,
                                PrCodes.CREATE__CONTEXT_ID_PRINT,
                                PrCodes.CREATE__EXTRALINES,
                                PrCodes.MD_SSH__FILE_READ,
                                PrCodes.MD_USERDATA__FILE_READ,
								PrCodes.LISTENER_TERMINATION__INSTANCE_ID_PRINT,
								PrCodes.LISTENER_AUTODESTROY,
                                PrCodes.CREATE__CTXBROKER_CONTACTINF};
        return PrCodes.getAllCodesExcept(optOuts);
    }
    
    public static void main(String[] argv) {
        // look for debug early, for diagnosing problems with parsing etc.
        PrintStream debug = null;
        if (CLIUtils.containsDebug(argv)) {
            debug = System.err;
        }
        final PrintOpts pOpts = new PrintOpts(getOptInPrCodes());
        final Print print = new Print(pOpts, System.out, System.err, debug);
        final int retCode = mainImpl(argv, print);
        print.flush();
        print.close();
        System.exit(retCode);
    }


    public static int mainImpl(String[] argv, Print pr) {

        final CloudClient client = new CloudClient(pr);

        // used:
        ParameterProblem parameterProblem = null;
        ExitNow exitNow = null;
        Throwable any = null;

        // unused currently:
        //Throwable throwable = null;
        //ExecutionProblem executionProblem = null;

        int retCode;
        try {
            retCode = _mainImpl(argv, client);
        } catch (ExitNow e) {
            exitNow = e;
            any = e;
            retCode = exitNow.exitCode;
        } catch (ParameterProblem e) {
            parameterProblem = e;
            any = e;
            retCode = BaseClient.COMMAND_LINE_EXIT_CODE;
        } catch (ExecutionProblem e) {
            //executionProblem = e;
            any = e;
            retCode = BaseClient.APPLICATION_EXIT_CODE;
        } catch (Throwable t) {
            //throwable = t;
            any = t;
            retCode = BaseClient.UNKNOWN_EXIT_CODE;
        }

        if (!pr.enabled()) {
            // the rest of this method is for printing
            return retCode; // *** EARLY RETURN ***
        }

        if (exitNow != null) {
            pr.debugln("[exiting via exitnow system]");
            pr.debugln(BaseClient.retCodeDebugStr(retCode));
            return retCode; // *** EARLY RETURN ***
        }

        if (any == null) {
            pr.debugln(BaseClient.retCodeDebugStr(retCode));
            return retCode; // *** EARLY RETURN ***
        }

        CommonPrint.printDebugSection(pr, "PROBLEM");

        final String message = CommonUtil.genericExceptionMessageWrapper(any);

        String err = "Problem: " + message;

        if (parameterProblem != null && !pr.useLogging()) {
            err += "\nSee help (-h).";
        }

        pr.errln(err);

        pr.debugln("\n");

        final String sectionTitle = "STACKTRACE";
        CommonPrint.printDebugSection(pr, sectionTitle);

        any.printStackTrace(pr.getDebugProxy());

        CommonPrint.printDebugSectionEnd(pr, sectionTitle);

        pr.debugln("\n");

        pr.debugln("Stacktrace was from: " + any.getMessage());

        pr.debugln(BaseClient.retCodeDebugStr(retCode));

        return retCode;
    }

    private static int _mainImpl(String[] argv, CloudClient client)

            throws ParameterProblem, ExecutionProblem, ExitNow {

        // (for development only, to attach a remote debugger etc)
        if (CLIUtils.containsDebuggerHang(argv)) {
            try {
                CLIUtils.hangForInput(client.getPrint());
            } catch (IOException e) {
                throw new ExecutionProblem("", e);
            }
        }

        CommonPrint.logArgs(argv, client.getPrint());

        final AllArgs allArgs = new AllArgs(client.getPrint());

        try {
            allArgs.intakeCmdlineOptions(argv);
        } catch (ParseException e) {
            // in all likelihood due to issue with given parameter
            throw new ParameterProblem(e.getMessage(), e);
        }
        
        try {
            allArgs.intakeUserProperties();
            allArgs.intakeDefaultProperties();
        } catch (IOException e) {
            // in all likelihood due to issue with given parameter
            throw new ParameterProblem(e.getMessage(), e);
        }

        client.run(allArgs);
        
        return BaseClient.SUCCESS_EXIT_CODE;
    }

    // -------------------------------------------------------------------------
    // RUN() WRAPPER
    // -------------------------------------------------------------------------

    /**
     * @param allArguments may not be null
     * @throws ParameterProblem problem with inputs
     * @throws ExecutionProblem problem going through with something
     * @throws ExitNow early exit (see source code...)
     */
    public void run(AllArgs allArguments) throws ParameterProblem,
                                                 ExecutionProblem,
                                                 ExitNow {

        if (allArguments == null) {
            throw new IllegalArgumentException("allArguments may not be null");
        }

        this.args = allArguments;

        try {
            this.parameterCheck();
        } catch (Exception e) {
            throw new ParameterProblem(e.getMessage(), e);
        }

        try {
            this.runNoParameterCheck();
        } finally {
            if (this.executeUtil != null) {
                this.executeUtil.stopExecutorService();
            }
        }
    }
    
    protected Print getPrint() {
        return this.print;
    }
    
    
    // -------------------------------------------------------------------------
    // PARAMETER CHECKS
    // -------------------------------------------------------------------------
    
    void parameterCheck() throws Exception {

        final List actions = this.args.getActions();

        if (actions == null || actions.isEmpty()) {
            throw new ParameterProblem("Give at least one action.");
        }

        if (actions.contains(AllArgs.ACTION_HELP) ||
                actions.contains(AllArgs.ACTION_EXTRAHELP) ||
                    actions.contains(AllArgs.ACTION_USAGE)) {
            // nothing else matters
            return; // *** EARLY RETURN ***
        }

        if (actions.contains(AllArgs.ACTION_SECURITY_PRINT)) {
            final String sectionTitle = "PARAMETER CHECK: --" +
                                                Opts.SECURITY_OPT_STRING;
            CommonPrint.printDebugSection(this.print, sectionTitle);
            this.parameterCheck_securityPrint();
            CommonPrint.printDebugSectionEnd(this.print, sectionTitle);
        }

        if (actions.contains(AllArgs.ACTION_TARGET_PRINT)) {
            final String sectionTitle = "PARAMETER CHECK: --" +
                                                Opts.PRINT_TARGET_OPT_STRING;
            CommonPrint.printDebugSection(this.print, sectionTitle);
            this.parameterCheck_targetPrint();
            CommonPrint.printDebugSectionEnd(this.print, sectionTitle);
        }

        if (actions.contains(AllArgs.ACTION_HASH_PRINT)) {
            final String sectionTitle = "PARAMETER CHECK: --" +
                                                Opts.HASH_PRINT_OPT_STRING;
            CommonPrint.printDebugSection(this.print, sectionTitle);
            this.parameterCheck_hashPrint();
            CommonPrint.printDebugSectionEnd(this.print, sectionTitle);
        }

        if (actions.contains(AllArgs.ACTION_LIST)) {
            final String sectionTitle = "PARAMETER CHECK: --" +
                                                Opts.LIST_OPT_STRING;
            CommonPrint.printDebugSection(this.print, sectionTitle);
            this.parameterCheck_list();
            CommonPrint.printDebugSectionEnd(this.print, sectionTitle);
        }

        if (actions.contains(AllArgs.ACTION_TRANSFER)) {
            final String sectionTitle = "PARAMETER CHECK: --" +
                                                Opts.TRANSFER_OPT_STRING;
            CommonPrint.printDebugSection(this.print, sectionTitle);
            this.parameterCheck_transfer();
            CommonPrint.printDebugSectionEnd(this.print, sectionTitle);
        }

        if (actions.contains(AllArgs.ACTION_DELETE)) {
            final String sectionTitle = "PARAMETER CHECK: --" +
                                                Opts.DELETE_OPT_STRING;
            CommonPrint.printDebugSection(this.print, sectionTitle);
            this.parameterCheck_delete();
            CommonPrint.printDebugSectionEnd(this.print, sectionTitle);
        }

        if (actions.contains(AllArgs.ACTION_DOWNLOAD)) {
            final String sectionTitle = "PARAMETER CHECK: --" +
                                                Opts.DOWNLOAD_OPT_STRING;
            CommonPrint.printDebugSection(this.print, sectionTitle);
            this.parameterCheck_download();
            CommonPrint.printDebugSectionEnd(this.print, sectionTitle);
        }

        if (actions.contains(AllArgs.ACTION_SERVICE_PRINT)) {
            final String sectionTitle = "PARAMETER CHECK: --" +
                                                Opts.PRINT_SERVICE_OPT_STRING;
            CommonPrint.printDebugSection(this.print, sectionTitle);
            this.parameterCheck_servicePrint();
            CommonPrint.printDebugSectionEnd(this.print, sectionTitle);
        }

        if (actions.contains(AllArgs.ACTION_DESTROY)) {
            final String sectionTitle = "PARAMETER CHECK: --" +
                                                Opts.DESTROY_OPT_STRING;
            CommonPrint.printDebugSection(this.print, sectionTitle);
            this.parameterCheck_destroy();
            CommonPrint.printDebugSectionEnd(this.print, sectionTitle);
        }

        if (actions.contains(AllArgs.ACTION_STATUS_CHECK)) {
            final String sectionTitle = "PARAMETER CHECK: --" +
                                                Opts.STATUS_CHECK_OPT_STRING;
            CommonPrint.printDebugSection(this.print, sectionTitle);
            this.parameterCheck_status();
            CommonPrint.printDebugSectionEnd(this.print, sectionTitle);
        }

        if (actions.contains(AllArgs.ACTION_ASSOC_QUERY)) {
            final String sectionTitle = "PARAMETER CHECK: --" +
                                                Opts.ASSOC_QUERY_OPT_STRING;
            CommonPrint.printDebugSection(this.print, sectionTitle);
            this.parameterCheck_assocquery();
            CommonPrint.printDebugSectionEnd(this.print, sectionTitle);
        }

        if (actions.contains(AllArgs.ACTION_SAVE)) {
            final String sectionTitle = "PARAMETER CHECK: --" +
                                                Opts.SAVE_OPT_STRING;
            CommonPrint.printDebugSection(this.print, sectionTitle);
            this.parameterCheck_save();
            CommonPrint.printDebugSectionEnd(this.print, sectionTitle);
        }
        
        if (actions.contains(AllArgs.ACTION_RUN)) {
            final String sectionTitle = "PARAMETER CHECK: --" +
                                                Opts.RUN_OPT_STRING;
            CommonPrint.printDebugSection(this.print, sectionTitle);
            this.parameterCheck_run();
            CommonPrint.printDebugSectionEnd(this.print, sectionTitle);
        }

        if (actions.contains(AllArgs.ACTION_INIT_CONTEXT)) {
            final String sectionTitle = "PARAMETER CHECK: --" +
                                                Opts.INIT_CTX_OPT_STRING;
            CommonPrint.printDebugSection(this.print, sectionTitle);
            this.parameterCheck_initCtx();
            CommonPrint.printDebugSectionEnd(this.print, sectionTitle);
        }
    }

    void parameterCheck_initCtx() throws ParameterProblem {
        this._parameterCheck_clusterfile();
        this._parameterCheck_initContext();
    }

    void parameterCheck_list() throws ParameterProblem {

        this._checkCredential("Image listing");
        
        this._checkGridFTPGeneric(Opts.LIST_OPT_STRING);
    }

    void parameterCheck_hashPrint() throws ParameterProblem {

        if (this.args.getHashPrintDN() == null) {
            throw new ParameterProblem("Hash print requires DN to hash");
        }
    }

    void parameterCheck_securityPrint() throws ParameterProblem {
        this._checkCredential("Security printing");
    }

    void parameterCheck_servicePrint() throws ParameterProblem {
        this._checkServiceURL(Opts.PRINT_SERVICE_OPT_STRING);
    }

    void parameterCheck_destroy() throws ParameterProblem {

        if (this.args.getActions().contains(AllArgs.ACTION_RUN)) {
            throw new ParameterProblem(
                    "You cannot create (--" + Opts.RUN_OPT_STRING +
                            ") and destroy in the same invocation");
        }

        final String actionString = "Terminating";
        this._checkCredential(actionString);
        this._translateHandle(actionString);
        this._checkSpecificEPR(actionString);
    }

    void parameterCheck_status() throws ParameterProblem {

        if (this.args.getActions().contains(AllArgs.ACTION_RUN)) {
            throw new ParameterProblem(
                    "You cannot create (--" + Opts.RUN_OPT_STRING +
                            ") and run a status check (on a previously" +
                            "created workspace) in the same invocation");
        }

        final String actionString = "Checking status";
        this._checkCredential(actionString);
        this._translateHandle(actionString);

        if (this.args.getHistorySubDir() != null ||
                this.args.getEprGivenFilePath() != null) {
            this._checkSpecificEPR("Checking status of one workspace");
        } else {
            this._checkStatusServiceEPR("Checking status of all workspaces");
        }
    }

    void parameterCheck_assocquery() throws ParameterProblem {
        final String actionString = "Querying networks";
        this._checkCredential(actionString);
        this._checkServiceURL(actionString);
    }

    void parameterCheck_save() throws ParameterProblem {

        if (this.args.getActions().contains(AllArgs.ACTION_RUN)) {
            throw new ParameterProblem(
                  "You cannot create (--" + Opts.RUN_OPT_STRING + ") and " +
                  "save a previously created workspace in the same invocation");
        }

        final String actionString = "Saving";
        this._checkCredential(actionString);
        this._translateHandle(actionString);
        this._checkSpecificEPR(actionString);
        this._checkGridFTPGeneric(actionString);

        final String newname = this.args.getNewname();
        if (newname != null) {
            this.print.debugln(
                    "save called with newname '" + newname + "'");
            try {
                this.newUnpropTargetURL = getDerivedImageURL(newname);
            } catch (Exception e) {
                throw new ParameterProblem("Problem with save's newname '" +
                        newname + "': " + e.getMessage(), e);
            }
        } else {
            this.print.debugln("save called with no newname");
        }
    }

    void parameterCheck_targetPrint() throws ParameterProblem {

        this._checkCredential("Target printing",
                              " (because target path is partially derived " +
                                      "from your credential).");
        
        this._checkGridFTPGeneric(Opts.TARGETDIR_OPT_STRING);

        final String sourcefile = this.args.getSourcefile();
        final String name = this.args.getName();
        if (sourcefile == null && name == null) {
            throw new ParameterProblem(Opts.TARGETDIR_OPT_STRING +
                                       " requires either '" +
                                       Opts.SOURCEFILE_OPT_STRING + "' or '" +
                                       Opts.NAME_OPT_STRING + "'");
        }

        if (sourcefile != null) {
            final File f = new File(sourcefile);
            this._transferTargetURL =
                    this.remoteUserBaseURLString + f.getName();
        }
        if (name != null) {
            this._transferTargetURL =
                    this.remoteUserBaseURLString + name;
        }
    }

    void parameterCheck_transfer() throws ParameterProblem {

        this._checkCredential("Transferring");

        this._checkGridFTPGeneric(Opts.TRANSFER_OPT_STRING);

        final String sourcefile = this.args.getSourcefile();
        if (sourcefile == null) {
            throw new ParameterProblem(Opts.TRANSFER_OPT_STRING +
                                       " requires '" +
                                       Opts.SOURCEFILE_OPT_STRING + "'");
        }

        this._checkSourcefile();

        this.transferSourceURL = CloudClientUtil.sourceURL(sourcefile);

        final File f = new File(sourcefile);
        this.transferTargetURL = this.remoteUserBaseURLString + f.getName();
    }

    void parameterCheck_delete() throws ParameterProblem {

        this._checkCredential("Deleting");

        this._checkGridFTPGeneric(Opts.DELETE_OPT_STRING);

        final String name = this.args.getName();
        if (name == null) {
            throw new ParameterProblem("Deleting requires '" +
                    Opts.NAME_OPT_STRING + "' (the name " +
                    "of the file in your remote personal directory)");
        }

        this.deleteURL = this.remoteUserBaseURLString + name;
    }

    void parameterCheck_download() throws ParameterProblem {

        this._checkCredential("Downloading");

        this._checkGridFTPGeneric(Opts.DOWNLOAD_OPT_STRING);

        final String localfile = this.args.getLocalfile();
        final String name = this.args.getName();

        if (localfile == null) {
            throw new ParameterProblem(Opts.DOWNLOAD_OPT_STRING +
                                       " requires '" +
                                       Opts.LOCAL_FILE_OPT_STRING + "'");
        }

        if (name == null) {
            throw new ParameterProblem("Downloading requires '" +
                    Opts.NAME_OPT_STRING + "' (the name " +
                    "of the file in your remote personal directory)");
        }

        this._checkLocalfile();

        this.transferSourceURL = this.remoteUserBaseURLString + name;

        // local URL
        this.transferTargetURL = CloudClientUtil.localTargetURL(localfile);
    }

    /* SINGLE RUN/CLUSTER RUN CHECKS: */

    void parameterCheck_run() throws ParameterProblem {

        final String sourcefile = this.args.getSourcefile();
        final String clusterPath = this.args.getClusterPath();
        final String name = this.args.getName();
        final String ec2ScriptPath = this.args.getEc2ScriptPath();


        // Differentiate between run and cluster-run
        
        if (sourcefile == null && name == null && clusterPath == null) {

            throw new ParameterProblem("Running requires either '" +
                                       Opts.SOURCEFILE_OPT_STRING + "', '" +
                                       Opts.NAME_OPT_STRING + "', or '" +
                                       Opts.CLUSTER_OPT_STRING + "'");
        }

        if (sourcefile != null && name != null) {
            throw new ParameterProblem("You may not specify both '" +
                                       Opts.SOURCEFILE_OPT_STRING + "' and '" +
                                       Opts.NAME_OPT_STRING + "'");
        }

        if (sourcefile == null && name == null && ec2ScriptPath != null) {
            this.args.getActions().add(AllArgs.ACTION_EC2_CLUSTER);
            this.print.debugln("\n*** EC2 CLUSTER HELPER\n");
            this._parameterCheck_runec2cluster();
            this._parameterCheck_clusterfile();
        } else if (sourcefile == null && name == null) {
            this.args.getActions().add(AllArgs.ACTION_RUN_CLUSTER);
            this.print.debugln("\n*** RUN CLUSTER\n");
            this._parameterCheck_runcommon();
            this._parameterCheck_clusterfile();
        } else {
            this.args.getActions().add(AllArgs.ACTION_RUN_SINGLE);
            this.print.debugln("\n*** RUN SINGLE\n");
            this._parameterCheck_runcommon();
        }
    }

    void _parameterCheck_clusterfile() throws ParameterProblem {

        String clusterPath = this.args.getClusterPath();

        this.clusterMembers = ClusterUtil.getClusterMembers(clusterPath,
            this.args.getBrokerLocalNicPrefix(),
            this.args.getBrokerPublicNicPrefix(),
            this.print
        );

        // true if at least one contextualization section is present
        boolean oneContextualization = false;

        // true if at least one adjustment is requested
        boolean adjustKnownHosts = false;

        String ssh_hostsfile = this.args.getSsh_hostsfile();
        
        for (int i = 0; i < this.clusterMembers.length; i++) {

            final ClusterMember member = this.clusterMembers[i];

            if (member.isOneLoginFlagPresent()) {
                if (member.getClusterForUserData() == null
                        && ssh_hostsfile != null) {
                    this.print.errln("  - Warning: Host SSH pubkey(s) will " +
                            "not be available for this member " +
                            "(it's not involved in contextualization)");
                } else {
                    adjustKnownHosts = true;
                }
            }

            if (member.getClusterForUserData() != null) {
                oneContextualization = true;
            }
        }

        if (this.args.isNoContextLock() && !oneContextualization) {
            this.print.errln("  - Warning: You gave the --" +
                    Opts.NOCTXLOCK_OPT_STRING + " flag but there are no " +
                    "contextualization sections in the cluster definition.");
        }

        // at least one adjustment is requested
        if (adjustKnownHosts && ssh_hostsfile != null) {
            
            String newHostsFile = ClusterUtil.expandSshHostsFile(ssh_hostsfile,
                this.print);
            this.args.setSsh_hostsfile(newHostsFile);
        }

    }

    void _parameterCheck_runcommon() throws ParameterProblem {

        this._checkCredential("Running");

        this._checkServiceURL(Opts.RUN_OPT_STRING);

        String sshfile = this.args.getSshfile();

        if (sshfile == null) {
            this.print.errln("WARNING: no SSH public key is configured");
        }

        if (this.args.getHistoryDirectory() == null) {
            throw new ParameterProblem("Running requires '" +
                                       Opts.HISTORY_DIR_OPT_STRING + "'");
        }

        this._checkHistoryDirectory("Running");

        if (!this.args.isDurationMinutesConfigured()) {
            throw new ParameterProblem("Running requires '" +
                                       Opts.HOURS_OPT_STRING + "'");
        }

        this.print.debugln("Checking on repository URL for running " +
                    "because we are going to derive propagation string " +
                    "for the client");

        if (this.args.getPropagationScheme() == null) {
            throw new ParameterProblem(
                    "Running requires propagation scheme configuration");
        }

        this._checkGridFTPGeneric(Opts.RUN_OPT_STRING);

        final String newname = this.args.getNewname();
        if (newname != null) {
            this.print.debugln(
                    "run called with newname '" + newname + "'");
            try {
                this.newUnpropTargetURL =
                        this.getDerivedImageURL(newname);

            } catch (Exception e) {
                throw new ParameterProblem("Problem with run's newname '" +
                        newname + "': " + e.getMessage(), e);
            }
        }

        if (sshfile != null) {

            sshfile = CloudClientUtil.expandSshPath(sshfile);

            final File f = new File(sshfile);
            sshfile = f.getAbsolutePath();
            this.args.setSshfile(sshfile);

            this.print.debugln("Examining '" + sshfile + "'");

            if (!CloudClientUtil.fileExistsAndReadable(sshfile)) {
                throw new ParameterProblem("SSH public key file does not " +
                        "exist or is not readable: '" + sshfile + "'");
            }

            this.print.debugln("Exists and readable: '" + sshfile + "'");
        }

		if (this.args.getKernel() != null) {
			final String kernel = this.args.getKernel().trim();
			if (kernel.length() == 0) {
				throw new ParameterProblem("empty kernel string?");
			}

			if (kernel.indexOf('/') >= 0) {
				throw new ParameterProblem("kernel may not contain any /");
			}
			if (kernel.indexOf("..") >= 0) {
				throw new ParameterProblem("kernel may not contain any '..'");
			}

			// already-propagated kernel is implied
			try {
				this.kernelURI = new URI("file://" + kernel);
			} catch (URI.MalformedURIException e) {
				throw new ParameterProblem(e.getMessage(), e);
			}
		}
    }

    private String getDerivedImageURL(String imageName) throws Exception {

        this.print.debugln("Translating image name '" + imageName + "' into " +
                "metadata URL");

        final String url = CloudClientUtil.deriveImageURL(
            this.args.getGridftpHostPort(), imageName,
            this.remoteUserBaseDir,
            this.args.getPropagationScheme(),
            this.args.isPropagationKeepPort());
        this.print.debugln("Derived image URL: '" + url + "'");

        return url;
    }

    void _parameterCheck_initContext() throws ParameterProblem {
        this._checkCredential("Context init helper (which talks to the " +
                "context broker)");

        if (this.args.getInitCtxDir() == null) {
            throw new ParameterProblem("Must specify ctx output directory");
        }
    }

    void _parameterCheck_runec2cluster() throws ParameterProblem {

        this._checkCredential("EC2 cluster helper (which talks to the " +
                "context broker)");

        final String brokerURL = this.args.getBrokerURL();
        if (brokerURL == null) {
            final String factoryHostPort = this.args.getFactoryHostPort();
            if (factoryHostPort == null) {
                throw new ParameterProblem(" requires either '--" +
                        Opts.BROKER_URL_OPT_STRING + "' or '--" +
                        Opts.FACTORY_OPT_STRING + "'");
            }

            final String url = CloudClientUtil.serviceURL(factoryHostPort);
            if (!CloudClientUtil.validURL(url, this.print.getDebugProxy())) {
                throw new ParameterProblem("Derived service URL is not a " +
                        "valid URL: '" + url + "'");
            }

            this.workspaceFactoryURL = url;
        }

        if (this.args.getHistoryDirectory() == null) {
            throw new ParameterProblem("This requires '" +
                                       Opts.HISTORY_DIR_OPT_STRING + "'");
        }

        this._checkHistoryDirectory("EC2 cluster helper (which talks to the " +
                "context broker)");
    }


    /* SHARED CHECKS: */

    void _checkCredential(String action) throws ParameterProblem {
        this._checkCredential(action, null);
    }

    void _checkCredential(String action, String tail) throws ParameterProblem {
        try {
            this.getProxyBeingUsed();
        } catch (Exception e) {

            String actionTxt = action;

            if (action == null) {
                actionTxt = "This action";
            }

            String msg = actionTxt + " requires credential";

            if (tail != null) {
                msg += tail;
                msg += "\nSee:\n";
            } else {
                msg += ", see:\n";
            }
            msg += "  - " + credURL + "\n";
            msg += "  - README.txt\n";
            msg += "  - ./bin/grid-proxy-init.sh";
            throw new ParameterProblem(msg);
        }
    }

    void _checkServiceURL(String action) throws ParameterProblem {

        final String factoryHostPort = this.args.getFactoryHostPort();

        if (factoryHostPort == null) {
            throw new ParameterProblem(action + " requires '" +
                                       Opts.FACTORY_OPT_STRING + "'");
        }

        final String url = CloudClientUtil.serviceURL(factoryHostPort);
        if (!CloudClientUtil.validURL(url, this.print.getDebugProxy())) {
            throw new ParameterProblem("Service URL is not a valid URL: '" +
                                       url + "'");
        }

        this.workspaceFactoryURL = url;
        this.print.debugln("Derived workspace factory URL: '" + url + "'");
    }

    void _checkStatusServiceEPR(String action) throws ParameterProblem {

        final String factoryHostPort = this.args.getFactoryHostPort();

        if (factoryHostPort == null) {
            throw new ParameterProblem(action + " requires '" +
                                       Opts.FACTORY_OPT_STRING + "'");
        }

        final String url = CloudClientUtil.statusServiceURL(factoryHostPort);
        if (!CloudClientUtil.validURL(url, this.print.getDebugProxy())) {
            throw new ParameterProblem(
                    "Status Service URL is not a valid URL: '" + url + "'");
        }

        try {
            this.statusServiceEPR =
                    AddressingUtils.createEndpointReference( url,
                                     WorkspaceStatusClient.defaultResourceKey);
        } catch (Exception e) {
            final String err = "Problem deriving status service EPR: ";
            throw new ParameterProblem(err + e.getMessage(), e);
        }

        this.print.debugln("Derived workspace status service URL: '" + url + "'");
    }

    void _checkHistoryDirectory(String action) throws ParameterProblem {

        final String historyDirectory = this.args.getHistoryDirectory();

        if (historyDirectory == null) {
            throw new ParameterProblem(action + " requires '" +
                                       Opts.HISTORY_DIR_OPT_STRING + "'");
        }

        CloudClientUtil.verifyHistoryDir(historyDirectory, true, true);
    }

    void _translateHandle(String action) throws ParameterProblem {

        if (this.args.getHandle() == null) {
            this.print.debugln("no handle supplied");
            return;
        }

        if (this.args.getHistorySubDir() != null) {
            this.print.debugln("handle supplied, but presence of history " +
                    "sub-directory argument overrides it");
            return;
        }

        if (this.args.getHistoryDirectory() == null) {
            throw new ParameterProblem("Not able to use handle to locate the " +
                    "necessary, stored information: no top-level history " +
                    "directory argument was supplied.");
        }

        this._checkHistoryDirectory(action);

        // translate history directory and handle argument into history subdir,
        // the result will be checked later by whatever needs it
        final String historySubDir = this.args.getHistoryDirectory() +
                File.separator + this.args.getHandle();
        this.args.setHistorySubDir(historySubDir);
    }

    void _checkSpecificEPR(String action)

            throws ParameterProblem {

        final String historySubDir = this.args.getHistorySubDir();
        final String eprGivenFilePath = this.args.getEprGivenFilePath();

        if (historySubDir == null && eprGivenFilePath == null) {

            // This is a message present to human.  An unmentioned, third option
            // is technically possible.  It's more esoteric, but one could
            // specify Opts.HISTORY_SUBDIR_OPT_STRING directly.

            throw new ParameterProblem(action + " requires either " +
                    "'" + Opts.HANDLE_OPT_STRING +
                    "' or path to specific EPR file using '" +
                    Opts.EPR_FILE_OPT_STRING + "'");
        }

        if (historySubDir != null && eprGivenFilePath != null) {
            throw new ParameterProblem(action + " will use either '" +
                                       Opts.HISTORY_SUBDIR_OPT_STRING +
                                       "' or '" + Opts.EPR_FILE_OPT_STRING +
                                       "', but you've provided BOTH options.");
        }

        File f;
        if (historySubDir != null) {
            CloudClientUtil.verifyHistoryDir(historySubDir, false, false);
            f = new File(historySubDir, HistoryUtil.SINGLE_EPR_FILE_NAME);
            if (!CloudClientUtil.fileExistsAndReadable(f)) {
                f = new File(historySubDir, HistoryUtil.ENSEMBLE_EPR_FILE_NAME);
                if (CloudClientUtil.fileExistsAndReadable(f)) {
                    this.isClusterHandle = true;
                } else {
                    throw new ParameterProblem("Cannot find or read any " +
                            "instance or cluster EPRs under '" +
                            historySubDir + "'\nPerhaps this launch did " +
                            "not succeed?");
                }
            }
        } else {
            f = new File(eprGivenFilePath);
            if (!CloudClientUtil.fileExistsAndReadable(f)) {
                throw new ParameterProblem("Cannot find or read '" +
                        eprGivenFilePath + "'");
            }
        }

        String old = null;
        if (this.specificEPRpath != null) {
            old = this.specificEPRpath;
        }

        this.specificEPRpath = f.getAbsolutePath();

        if (old != null) {
            if (!old.equals(this.specificEPRpath)) {
                throw new ParameterProblem(
                        "Unexpected with any input: specific EPR that was " +
                                "previously set is '" + old + "' but it is " +
                                "not the same as a repeat derivation which " +
                                "is '" + this.specificEPRpath + "'. Please " +
                                "send debug output file (in the history " +
                                "directory) to developers, thankyou.");
            }
        }
    }

    void _checkSourcefile() throws ParameterProblem {
        final String sourcefile = this.args.getSourcefile();
        if (!CloudClientUtil.fileExistsAndReadable(sourcefile)) {
            throw new ParameterProblem("Specified source file does not " +
                    "exist or is not readable: '" + sourcefile + "'");
        }
    }

    void _checkLocalfile() throws ParameterProblem {
        final String localfile = this.args.getLocalfile();
        if (CloudClientUtil.fileExists(localfile)) {
            throw new ParameterProblem("Specified local target file already " +
                    "exists: '" + localfile + "'");
        }
    }

    void _checkGridFTPGeneric(String action) throws ParameterProblem {
        
        if (this.args.getGridftpHostPort() == null) {
            throw new ParameterProblem(action + " requires '" +
                                       Opts.GRIDFTP_OPT_STRING + "'");
        }
        if (this.args.getTargetBaseDirectory() == null) {
            throw new ParameterProblem(action + " requires '" +
                                       Opts.TARGETDIR_OPT_STRING + "'");
        }

        final String url;
        try {
            url = this.getRemoteUserBaseURLString();
        } catch (Exception e) {
            throw new ParameterProblem("Issue deriving target image " +
                    "repository URL: " + e.getMessage());
        }
        if (!CloudClientUtil.validURL(url, this.print.getDebugProxy())) {
            throw new ParameterProblem("Derived target image " +
                    "repository URL is not a valid URL: '" + url + "'");
        }
    }

    void _checkDepReqDefaults(String action) throws ParameterProblem {
        this.__checknull(action, "deployment request file name",
                         this.args.getDeploymentRequest_fileName());
    }

    void _checkMetadataDefaults(String action) throws ParameterProblem {
        this.__checknull(action, "association name", this.args.getMetadata_association());
        this.__checknull(action, "cpu type", this.args.getMetadata_cpuType());
        this.__checknull(action, "metadata filename", this.args.getMetadata_fileName());
        this.__checknull(action, "partition mount-as", this.args.getMetadata_mountAs());
        this.__checknull(action, "NIC name", this.args.getMetadata_nicName());
        this.__checknull(action, "VMM type", this.args.getMetadata_vmmType());
        this.__checknull(action, "VMM version", this.args.getMetadata_vmmVersion());
    }

    void __checknull(String action,
                     String propname,
                     String val) throws ParameterProblem {
        if (val == null) {
            throw new ParameterProblem(action + " requires " + propname);
        }
    }
    
    
    /* DERIVED FIELDS */

    // only used for printing and/or presence-detect
    GlobusCredential getProxyBeingUsed() throws Exception {
        if (this.proxyUsed != null) {
            return this.proxyUsed;
        }
        this.proxyUsed = GlobusCredential.getDefaultCredential();
        if (this.proxyUsed == null) {
            throw new Exception("Could not find current credential");
        }
        return this.proxyUsed;
    }

    String getRemoteUserBaseURLString() throws Exception {

        if (this.remoteUserBaseURLString != null) {
            return this.remoteUserBaseURLString;
        }

        final String hash = SecurityUtil.hashGlobusCredential(
                                               this.getProxyBeingUsed(),
                                               this.print.getDebugProxy());
        if (hash == null) {
            throw new Exception("Could not obtain hash of current " +
                        "credential to generate directory name");
        }

        this.remoteUserBaseDir =
                CloudClientUtil.destUserBaseDir(this.args.getTargetBaseDirectory(),
                                                hash);

        this.remoteUserBaseURLString =
                CloudClientUtil.destUserBaseURL(this.args.getGridftpHostPort(),
                                                this.args.getTargetBaseDirectory(),
                                                hash);

        this.print.debugln("\nDerived user base dir: " +
                                            this.remoteUserBaseDir);
        this.print.debugln("\nDerived user base URL: " +
                                            this.remoteUserBaseURLString);

        return this.remoteUserBaseURLString;
    }

    // -------------------------------------------------------------------------
    // EXECUTE
    // -------------------------------------------------------------------------

    void runNoParameterCheck() throws ExecutionProblem, ExitNow {
        
        if (this.action_help()) {
            return; // if a help action ran, cut out early
        }

        this.action_hashPrint();
        this.trustedCAs();
        this.action_justPrinting();
        this.action_assoc_query();
        this.action_list();
        this.action_status();
        this.action_save();
        this.action_destroy();
        this.action_transfer();
        this.action_run_single();
        this.action_run_cluster();
        this.action_run_ec2cluster();
        this.action_init_context();
        this.action_download();
        this.action_delete();
    }

    void trustedCAs() throws ExecutionProblem {

        final String sectionTitle = "TRUSTED CAs CHECK";
        CommonPrint.printDebugSection(this.print, sectionTitle);

        final String caAppendDir = this.args.getCaAppendDir();
        
        try {
            if (caAppendDir != null) {
                this.print.debugln("caAppendDir = '" + caAppendDir + "'");
                TrustedCAs.addTrustedCaDirectory(caAppendDir);
                this.print.debugln("Added '" + caAppendDir + "' to " +
                                            "trusted certificate directories");
            } else {
                this.print.debugln("caAppendDir not present");
            }

            this.trustedCertDirs =
                    CertUtil.trustedCertificateDirectories(
                                                this.print.getDebugProxy());
        } catch (Exception e) {
            throw new ExecutionProblem(e.getMessage(), e); // ...
        }

        CommonPrint.printDebugSectionEnd(this.print, sectionTitle);
    }

    // returns true if a help action ran
    boolean action_help() throws ExecutionProblem {
        try {
            if (this.args.getActions().contains(AllArgs.ACTION_USAGE)) {
                this.print.infoln(new Help().getUsageString());
                return true;
            }

            if (this.args.getActions().contains(AllArgs.ACTION_HELP)) {
                this.print.infoln(new Help().getHelpString());
                return true;
            }

            if (this.args.getActions().contains(AllArgs.ACTION_EXTRAHELP)) {
                this.print.infoln(new Help().getExtraHelpString());
                return true;
            }
        } catch (IOException e) {
            throw new ExecutionProblem("Unexpected problem with help system: "
                                + e.getMessage(), e);
        }
        return false;
    }

    void action_hashPrint() throws ExecutionProblem {
        if (this.args.getActions().contains(AllArgs.ACTION_HASH_PRINT)) {
            //this.print.infoln("  DN: " + this.hashPrintDN);
            try {
                //this.print.infoln("HASH: " +
                //            SecurityUtil.hashDN(this.hashPrintDN));
                this.print.infoln(SecurityUtil.hashDN(this.args.getHashPrintDN()));
            } catch (NoSuchAlgorithmException e) {
                throw new ExecutionProblem(e.getMessage(), e);
            }
        }
    }

    void action_justPrinting() throws ExecutionProblem {
        try {
            if (this.args.getActions().contains(AllArgs.ACTION_SECURITY_PRINT)) {

                final String sectionTitle = "ACTION: SECURITY PRINT";
                CommonPrint.printDebugSection(this.print, sectionTitle);

                final SecurityPrinter sp =
                        new SecurityPrinter(this.proxyUsed,
                                            this.trustedCertDirs);
                sp.print(this.args.getCaHash(),
                         this.print.getInfoProxy(),
                         this.print.getDebugProxy());

                CommonPrint.printDebugSectionEnd(this.print, sectionTitle);
            }

            if (this.args.getActions().contains(AllArgs.ACTION_SERVICE_PRINT)) {

                final String sectionTitle = "ACTION: SERVICE PRINT";
                CommonPrint.printDebugSection(this.print, sectionTitle);

                this.print.infoln(this.workspaceFactoryURL);

                CommonPrint.printDebugSectionEnd(this.print, sectionTitle);
            }

            if (this.args.getActions().contains(AllArgs.ACTION_TARGET_PRINT)) {

                final String sectionTitle = "ACTION: TARGET URL PRINT";
                CommonPrint.printDebugSection(this.print, sectionTitle);

                if (this.transferTargetURL != null) {
                    this.print.infoln(this.transferTargetURL);
                } else if (this._transferTargetURL != null) {
                    this.print.infoln(this._transferTargetURL);
                } else {
                    throw new ExecutionProblem(
                             "print target configured but no target URL?");
                }

                CommonPrint.printDebugSectionEnd(this.print, sectionTitle);
            }
        } catch (Exception e) {
            throw new ExecutionProblem(e.getMessage(), e); // ...
        }
    }

    void action_list() throws ExecutionProblem {
        if (this.args.getActions().contains(AllArgs.ACTION_LIST)) {
            final String sectionTitle = "ACTION: LIST";
            CommonPrint.printDebugSection(this.print, sectionTitle);

            final String url;
            try {
                url = this.getRemoteUserBaseURLString();
            } catch (Exception e) {
                throw new ExecutionProblem(e.getMessage(), e);
            }

            final FileListing[] files =
                    this.executeUtil.listFiles(url,
                                               this.args.getGridftpID(),
                                               this.print.getInfoProxy(),
                                               this.print.getErrProxy(),
                                               this.print.getDebugProxy());
            
            CloudClientUtil.printFileList(files, this.print);

            CommonPrint.printDebugSectionEnd(this.print, sectionTitle);
        }
    }

    void action_destroy() throws ExecutionProblem, ExitNow {
        if (this.args.getActions().contains(AllArgs.ACTION_DESTROY)) {
            final String sectionTitle = "ACTION: DESTROY";
            CommonPrint.printDebugSection(this.print, sectionTitle);

            this.executeUtil.destroy(this.specificEPRpath,
                                     this.args.getFactoryID(),
                                     this.isClusterHandle,
                                     this.print);

            CommonPrint.printDebugSectionEnd(this.print, sectionTitle);
        }
    }

    void action_status() throws ExecutionProblem, ExitNow {
        if (this.args.getActions().contains(AllArgs.ACTION_STATUS_CHECK)) {
            if (this.specificEPRpath == null) {
                this._action_status_all();
            } else {
                this._action_status_one();
            }
        }
    }

    private void _action_status_one() throws ExecutionProblem, ExitNow {
        final String sectionTitle = "ACTION: STATUS CHECK (one workspace)";
        CommonPrint.printDebugSection(this.print, sectionTitle);
        
        this.executeUtil.statusQuery(this.specificEPRpath,
                                     this.args.getFactoryID(),
                                     this.print);

        CommonPrint.printDebugSectionEnd(this.print, sectionTitle);
    }

    private void _action_status_all() throws ExecutionProblem, ExitNow {
        final String sectionTitle = "ACTION: STATUS CHECK (all workspaces)";
        CommonPrint.printDebugSection(this.print, sectionTitle);

        final StubConf conf = new StubConf();

        conf.setEPR(this.statusServiceEPR);

        if (this.args.getFactoryID() != null) {
            conf.setAuthorization(
                    new IdentityAuthorization(this.args.getFactoryID()));
        } else {
            conf.setAuthorization(HostAuthorization.getInstance());
        }

        final Status_QueryAll queryAll =
                new Status_QueryAll(this.statusServiceEPR, conf, this.print);

        this.print.infoln("Querying for ALL instances.\n");

        try {
            final Workspace[] workspaces = queryAll.queryAll();
            CloudClientUtil.printCurrent(workspaces,
                                         this.print,
                                         this.args.getHistoryDirectory(),
                                         this.statusServiceEPR);
        } catch (WorkspaceStatusFault e) {
            throw new ExecutionProblem(e.getMessage(), e);
        } catch (ParameterProblem e) {
            throw new ExecutionProblem(e.getMessage(), e);
        }

        CommonPrint.printDebugSectionEnd(this.print, sectionTitle);
    }

    void action_save() throws ExecutionProblem, ExitNow {
        if (this.args.getActions().contains(AllArgs.ACTION_SAVE)) {
            final String sectionTitle = "ACTION: SAVE";
            CommonPrint.printDebugSection(this.print, sectionTitle);

            this.executeUtil.save(this.args.getNewname(),
                                  this.newUnpropTargetURL,
                                  this.specificEPRpath,
                                  this.args.getPollMs(),
                                  this.args.isUseNotifications(),
                                  this.args.getFactoryID(),
                                  this.print);

            CommonPrint.printDebugSectionEnd(this.print, sectionTitle);
        }
    }

    void action_transfer() throws ExecutionProblem {
        if (this.args.getActions().contains(AllArgs.ACTION_TRANSFER)) {
            final String sectionTitle = "ACTION: TRANSFER";
            CommonPrint.printDebugSection(this.print, sectionTitle);

            this.executeUtil.sendFile(this.transferSourceURL,
                                      this.transferTargetURL,
                                      this.args.getTimeoutMinutes(),
                                      this.args.getGridftpID(),
                                      this.print.getInfoProxy(),
                                      this.print.getDebugProxy());

            CommonPrint.printDebugSectionEnd(this.print, sectionTitle);
        }
    }

    void action_assoc_query() throws ExecutionProblem, ExitNow {
        if (!this.args.getActions().contains(AllArgs.ACTION_ASSOC_QUERY)) {
            return; // *** EARLY RETURN ***
        }

        final String sectionTitle = "ACTION: NETWORK QUERY";
        CommonPrint.printDebugSection(this.print, sectionTitle);

        this.executeUtil.associationQuery(this.workspaceFactoryURL,
                                          this.args.getFactoryID(),
                                          this.print);

        CommonPrint.printDebugSectionEnd(this.print, sectionTitle);
    }

    void action_run_single() throws ExecutionProblem, ExitNow {

        if (!this.args.getActions().contains(AllArgs.ACTION_RUN_SINGLE)) {
            return; // *** EARLY RETURN ***
        }

        final String sectionTitle = "ACTION: RUN SINGLE";
        CommonPrint.printDebugSection(this.print, sectionTitle);

        String imageName = this.args.getName();

        if (imageName == null) {
            final File sfile = new File(this.args.getSourcefile());
            imageName = sfile.getName();

            this.print.debugln("Derived image name to run from " +
                        "given sourcefile: '" + imageName + "'");

        } else {
            this.print.debugln("Using given image name: '" + imageName + "'");
        }

        final String imageURL;
        try {
            imageURL = getDerivedImageURL(imageName);
        } catch (Exception e) {
            throw new ExecutionProblem("Problem with image name '" +
                    imageName + "': " + e.getMessage(), e);
        }

        final URI imageURI;
        try {
            imageURI = new URI(imageURL);
        } catch (URI.MalformedURIException e) {
            throw new ExecutionProblem(e.getMessage(), e);
        }

        final String[] associations = {this.args.getMetadata_association()};
        final String[] nicnames = {this.args.getMetadata_nicName()};

        // runName (3rd method paramater, being set to null) will be set in
        // startOneWorkspace once rundir is known
        final VirtualWorkspace_Type metadata =
                MetadataXMLUtil.constructMetadata(imageURI,
                                                  this.args.getMetadata_mountAs(),
                                                  null,
                                                  associations,
                                                  nicnames,
                                                  this.args.getMetadata_cpuType(),
                                                  this.args.getMetadata_vmmVersion(),
                                                  this.args.getMetadata_vmmType(),
												  this.kernelURI);

        final WorkspaceDeployment_Type deploymentRequest =
                DeploymentXMLUtil.constructDeployment(this.args.getDurationMinutes(),
                                                      this.args.getMemory(),
                                                      this.newUnpropTargetURL);

        this.executeUtil.startOneWorkspace(this.workspaceFactoryURL,
                                           metadata,
                                           this.args.getMetadata_fileName(),
                                           deploymentRequest,
                                           this.args.getDeploymentRequest_fileName(),
                                           this.args.getSshfile(),
                                           this.args.getHistoryDirectory(),
                                           this.args.getPollMs(),
                                           this.args.isUseNotifications(),
                                           this.args.getFactoryID(),
                                           this.print);

        CommonPrint.printDebugSectionEnd(this.print, sectionTitle);
    }

    void action_run_cluster() throws ExecutionProblem, ExitNow {

        if (!this.args.getActions().contains(AllArgs.ACTION_RUN_CLUSTER)) {
            return; // *** EARLY RETURN ***
        }

        final String sectionTitle = "ACTION: RUN CLUSTER";
        CommonPrint.printDebugSection(this.print, sectionTitle);

        final int len = this.clusterMembers.length;

        final VirtualWorkspace_Type[] metadatas =
                                            new VirtualWorkspace_Type[len];
        final WorkspaceDeployment_Type[] deploymentRequests =
                                            new WorkspaceDeployment_Type[len];
        
        final String[] metadata_fileNames = new String[len];
        final String[] deploymentRequest_fileNames = new String[len];

        for (int i = 0; i < len; i++) {

            final ClusterMember member = this.clusterMembers[i];
            if (member == null) {
                throw new IllegalStateException(
                        "valid clusterMembers must be present here");
            }

            final String imageName = member.getImageName();

            final String imageURL;
            try {
                imageURL = this.getDerivedImageURL(imageName);
            } catch (Exception e) {
                throw new ExecutionProblem("Problem with image name '" +
                        imageName + "': " + e.getMessage(), e);
            }

            final URI imageURI;
            try {
                imageURI = new URI(imageURL);
            } catch (URI.MalformedURIException e) {
                throw new ExecutionProblem(e.getMessage(), e);
            }

            // runName (3rd method paramater, being set to null) will be set
            // in startWorkspaceCluster once rundir is known
            metadatas[i] =
                MetadataXMLUtil.constructMetadata(imageURI,
                                                  this.args.getMetadata_mountAs(),
                                                  null,
                                                  member.getAssociations(),
                                                  member.getIfaceNames(),
                                                  this.args.getMetadata_cpuType(),
                                                  this.args.getMetadata_vmmVersion(),
                                                  this.args.getMetadata_vmmType(),
												  this.kernelURI);
            
            deploymentRequests[i] =
                DeploymentXMLUtil.constructDeployment(this.args.getDurationMinutes(),
                                                      this.args.getMemory(),
                                                      this.newUnpropTargetURL,
                                                      member.getQuantity());

            metadata_fileNames[i] =
                    HistoryUtil.getMemberName(i+1) + "-" +
                            this.args.getMetadata_fileName();
            
            deploymentRequest_fileNames[i] =
                    HistoryUtil.getMemberName(i+1) + "-" +
                            this.args.getDeploymentRequest_fileName();
        }

        ClusterUtil.printClusterInfo(this.clusterMembers, this.print);

        final String[] printNames = new String[this.clusterMembers.length];

        final Cloudcluster_Type[] clustersForUserData =
                new Cloudcluster_Type[this.clusterMembers.length];

        for (int i = 0; i < this.clusterMembers.length; i++) {
            final int memberIndex = i;
            final ClusterMember member = this.clusterMembers[memberIndex];

            printNames[i] = member.getPrintName(); // may be null


            // will be null if not involved in contextualization
            clustersForUserData[memberIndex] = member.getClusterForUserData();
            printNames[i] = member.getPrintName(); // may be null
        }

        final KnownHostsTask[] knownHostTasks =
            ClusterUtil.constructKnownHostTasks(this.clusterMembers,
                this.args.isHostkeyDir());

        this.executeUtil.startWorkspaceCluster(this.workspaceFactoryURL,
                                               knownHostTasks,
                                               metadatas,
                                               metadata_fileNames,
                                               deploymentRequests,
                                               deploymentRequest_fileNames,
                                               printNames,
                                               clustersForUserData,
                                               !this.args.isNoContextLock(),
                                               this.args.getSshfile(),
                                               this.args.getSsh_hostsfile(),
                                               this.args.getHistoryDirectory(),
                                               this.args.getPollMs(),
                                               this.args.getFactoryID(),
                                               this.print,
                                               this.args.getBrokerURL(),
                                               this.args.getBrokerID());

        CommonPrint.printDebugSectionEnd(this.print, sectionTitle);
    }

    class CtxMemberInfo {
        private final String image;
        private final int quantity;
        private final ClusterMember clusterMember;

        public CtxMemberInfo(ClusterMember member) throws ExecutionProblem {
            if (member == null) {
                throw new IllegalArgumentException("member may not be null");
        }

            this.clusterMember = member;

            this.image = member.getImageName();
            if (this.image == null ||
                    this.image.trim().length() == 0) {
                throw new ExecutionProblem("image name missing from " +
                        "cluster document");
            }
            this.quantity = member.getQuantity();

        }
    }

    void validateClusterMembersForCtxClient() throws ExecutionProblem {
        if (this.clusterMembers == null) {
            throw new ExecutionProblem("no clusterMembers provided");
        }
        for (ClusterMember member : this.clusterMembers) {
            if (member == null) {
                throw new IllegalStateException(
                        "valid clusterMembers must be present here");
            }

            final String imageName = member.getImageName();
            if (imageName == null || imageName.trim().length() == 0) {
                throw new ExecutionProblem("No AMI in cluster file");
            }

        }

    }

    void action_init_context() throws ExecutionProblem, ExitNow {
        if (!this.args.getActions().contains(AllArgs.ACTION_INIT_CONTEXT)) {
            return; // *** EARLY RETURN ***
        }

        String brokerURL = this.args.getBrokerURL();
        String brokerIdentityAuthorization = this.args.getBrokerID();
        if (brokerURL == null) {
            int dx = this.workspaceFactoryURL.indexOf("WorkspaceFactoryService");
            brokerURL = this.workspaceFactoryURL.substring(0,dx);
            brokerURL += "NimbusContextBroker";
            print.debugln("No context broker URL was explicitly supplied," +
                    " so it has been deduced from the nimbus factory URL." +
                    " Using: " + brokerURL);

            // and so ID scheme for service must be copied
            if (brokerIdentityAuthorization == null
                    && this.args.getFactoryID() != null) {
                brokerIdentityAuthorization = this.args.getFactoryID();
            }
        }

        this.validateClusterMembersForCtxClient();

        this.executeUtil.ctxClusterHelp(this.clusterMembers,
                brokerURL, brokerIdentityAuthorization,
                !this.args.isNoContextLock(),
                this.args.getInitCtxDir(),
                this.print);
    }

    void action_run_ec2cluster() throws ExecutionProblem, ExitNow {

        if (!this.args.getActions().contains(AllArgs.ACTION_EC2_CLUSTER)) {
            return; // *** EARLY RETURN ***
        }

        this.validateClusterMembersForCtxClient();

        final String sectionTitle = "ACTION: EC2 CLUSTER HELP";
        CommonPrint.printDebugSection(this.print, sectionTitle);

        print.infoln("\nEC2 cluster:");
        for (int i = 0; i < this.clusterMembers.length; i++) {
            final ClusterMember member = this.clusterMembers[i];
            String inststr = " instance";
            if (member.getQuantity() > 1) {
                inststr += "s";
            }

            final String mname;
            if (member.getPrintName() == null) {
                mname = HistoryUtil.getMemberName(i+1);
            } else {
                mname = member.getPrintName();
            }

            this.print.infoln("  - " + mname + ": AMI '" +
                    member.getImageName() + "', " + member.getQuantity() +
                    inststr);
        }

        String brokerURL = this.args.getBrokerURL();
        String brokerIdentityAuthorization = this.args.getBrokerID();
        if (brokerURL == null) {
            int dx = this.workspaceFactoryURL.indexOf("WorkspaceFactoryService");
            brokerURL = this.workspaceFactoryURL.substring(0,dx);
            brokerURL += "NimbusContextBroker";
            print.debugln("No context broker URL was explicitly supplied," +
                    " so it has been deduced from the nimbus factory URL." +
                    " Using: " + brokerURL);

            // and so ID scheme for service must be copied
            if (brokerIdentityAuthorization == null
                    && this.args.getFactoryID() != null) {
                brokerIdentityAuthorization = this.args.getFactoryID();
            }
        }

        this.executeUtil.ec2ClusterHelp(this.clusterMembers,
                brokerURL, brokerIdentityAuthorization,
                                        !this.args.isNoContextLock(),
                                        this.args.getPollMs(),
                this.args.getEc2ScriptPath(),
                this.args.getHistoryDirectory(),
                this.print
        );

        CommonPrint.printDebugSectionEnd(this.print, sectionTitle);
    }

    void action_download() throws ExecutionProblem {
        if (this.args.getActions().contains(AllArgs.ACTION_DOWNLOAD)) {
            final String sectionTitle = "ACTION: DOWNLOAD";
            CommonPrint.printDebugSection(this.print, sectionTitle);

            this.executeUtil.downloadFile(this.transferSourceURL,
                                          this.transferTargetURL,
                                          this.args.getTimeoutMinutes(),
                                          this.args.getGridftpID(),
                                          this.print.getInfoProxy(),
                                          this.print.getDebugProxy());

            CommonPrint.printDebugSectionEnd(this.print, sectionTitle);
        }
    }

    void action_delete() throws ExecutionProblem {
        if (this.args.getActions().contains(AllArgs.ACTION_DELETE)) {
            final String sectionTitle = "ACTION: DELETE";
            CommonPrint.printDebugSection(this.print, sectionTitle);

            this.executeUtil.deleteFile(this.deleteURL,
                                        this.args.getGridftpID(),
                                        this.print.getInfoProxy(),
                                        this.print.getDebugProxy());

            CommonPrint.printDebugSectionEnd(this.print, sectionTitle);
        }
    }
}
