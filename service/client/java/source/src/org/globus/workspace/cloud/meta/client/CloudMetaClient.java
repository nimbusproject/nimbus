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

package org.globus.workspace.cloud.meta.client;

import org.globus.workspace.common.print.Print;
import org.globus.workspace.common.print.PrintOpts;
import org.globus.workspace.common.client.CLIUtils;
import org.globus.workspace.common.client.CommonPrint;
import org.globus.workspace.client_core.ParameterProblem;
import org.globus.workspace.client_core.ExecutionProblem;
import org.globus.workspace.client_core.ExitNow;
import org.globus.workspace.client_core.utils.StringUtils;
import org.globus.workspace.client_common.BaseClient;
import org.globus.workspace.cloud.client.cluster.ClusterUtil;
import org.globus.workspace.cloud.client.cluster.ClusterMember;
import org.globus.workspace.cloud.client.util.ExecuteUtil;
import org.globus.workspace.cloud.client.util.CloudClientUtil;
import org.globus.workspace.cloud.client.CloudClient;
import org.globus.workspace.cloud.client.Props;
import org.globus.wsrf.encoding.DeserializationException;
import org.nimbustools.ctxbroker.generated.gt4_0.description.Clouddeployment_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.description.Clouddeploy_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.description.Cloudcluster_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.description.Cloudworkspace_Type;
import org.nimbustools.messaging.gt4_0.common.CommonUtil;

import java.io.IOException;
import java.io.PrintStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class CloudMetaClient {

    private AllArgs args;
    private HashMap<String, CloudDeployment> cloudMap;
    private Cloudcluster_Type cluster;
    private ArrayList<Cloudworkspace_Type> workspaceList;
    private boolean needsBroker = false;


    private CloudManager cloudManager;
    private ExecuteUtil executeUtil = new ExecuteUtil();


    private final Print print;

    public Print getPrint() {
        return this.print;
    }


    public CloudMetaClient(Print pr) {
        if (pr == null) {
            throw new IllegalArgumentException("print may not be null");
        }
        this.print = pr;
        this.workspaceList = new ArrayList<Cloudworkspace_Type>();
        this.cloudMap = new LinkedHashMap<String, CloudDeployment>();

    }

    public static void main(String[] argv) {

        // keeping it simple for now, but basically following the model of CloudClient

        // look for debug early, for diagnosing problems with parsing etc.
        PrintStream debug = null;
        if (CLIUtils.containsDebug(argv)) {
            debug = System.err;
        }

        PrintOpts prOpts = new PrintOpts(CloudClient.getOptInPrCodes());

        final Print print = new Print(prOpts, System.out, System.err, debug);
        CloudMetaClient client = new CloudMetaClient(print);

        ParameterProblem parameterProblem = null;
        ExitNow exitNow = null;
        Throwable anyError = null;
        try {
            mainImpl(client, argv);
        } catch (ParameterProblem e) {
            anyError = e;
            parameterProblem = e;
        } catch (ExecutionProblem e) {
            anyError = e;
        } catch (ExitNow e) {
            exitNow = e;
        } catch (Throwable e) {
            anyError = e;
        }

        int exitCode;

        if (exitNow != null) {
            print.debugln("[exiting via exitnow system]");
            exitCode = exitNow.exitCode;

        } else if (anyError == null) {
            exitCode = BaseClient.SUCCESS_EXIT_CODE;
        } else {
            exitCode = BaseClient.COMMAND_LINE_EXIT_CODE;

            final String message =
                CommonUtil.genericExceptionMessageWrapper(anyError);

            String err = "Problem: " + message;

            if (parameterProblem != null && !print.useLogging()) {
                err += "\nSee help (-h).";
            }

            print.errln(err);

            print.debugln("\n");

            final String sectionTitle = "STACKTRACE";
            CommonPrint.printDebugSection(print, sectionTitle);

            anyError.printStackTrace(print.getDebugProxy());

            CommonPrint.printDebugSectionEnd(print, sectionTitle);

            print.debugln("\n");

            print.debugln("Stacktrace was from: " + anyError.getMessage());
        }

        print.flush();
        print.close();
        System.exit(exitCode);
    }

    static void mainImpl(CloudMetaClient client, String[] argv)
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

        AllArgs args = AllArgs.create(argv, client.getPrint());

        client.run(args);

    }

    public void run(AllArgs args)
        throws ParameterProblem, ExecutionProblem, ExitNow {

        if (args == null)
            throw new IllegalArgumentException("args cannot be null");

        this.args = args;

        this.handleParameters();

        final Set<AllArgs.Action> actions = this.args.getActions();

        try {

            if (actions.contains(AllArgs.Action.HELP)) {
                printHelp();
                return;
            }

            if (actions.contains(AllArgs.Action.RUN)) {
                doDeploy();
            }

        } finally {
            if (this.executeUtil != null) {
                this.executeUtil.stopExecutorService();
            }
        }

    }

    void handleParameters() throws ParameterProblem {

        Set<AllArgs.Action> actions = this.args.getActions();

        if (actions.isEmpty()) {
            throw new ParameterProblem("At least one action must be specified.");
        }

        if (actions.contains(AllArgs.Action.HELP)) {
            return;
        }

        handleCommonParameters();

        if (actions.contains(AllArgs.Action.RUN)) {
            handleRunParameters();
        }

    }

    void handleCommonParameters() throws ParameterProblem {

        String clouddirPath = this.args.getCloudConfDir();

        if (clouddirPath == null) {
            throw new ParameterProblem("You must specify a cloud configuration" +
                " directory with --"+Opts.CLOUD_DIR_OPT_STRING);
        }
        this.cloudManager = new CloudManager(clouddirPath);

        String historyDirectory = this.args.getHistoryDirectory();
         if (historyDirectory == null) {
            throw new ParameterProblem("You must specify a cloud history dir "+
                                       " with --"+Opts.HISTORY_DIR_OPT_STRING);
        }

        CloudClientUtil.verifyHistoryDir(historyDirectory, true, true);


    }

    void handleRunParameters() throws ParameterProblem {

        String clusterPath = this.args.getClusterPath();
        String deployPath = this.args.getDeployPath();

        String sshFile = this.args.getSshfile();
        String brokerUrl = this.args.getBrokerURL();
        String brokerId = this.args.getBrokerID();
        int duration = this.args.getDurationMinutes();
        int pollMs = this.args.getPollMs();


        if (clusterPath == null) {
            throw new ParameterProblem("You must specify a cluster " +
                "document with --" + Opts.CLUSTER_OPT_STRING);
        }

        this.cluster = ClusterUtil.getCluster(clusterPath, this.print);

        Cloudworkspace_Type[] workspaces = this.cluster.getWorkspace();
        if (workspaces == null || workspaces.length == 0) {
            throw new ParameterProblem("The cluster document must contain "+
                "at least one workspace");
        }

        for (Cloudworkspace_Type ws : workspaces) {

            final Clouddeploy_Type[] deploys = ws.getDeploy();
            if (deploys != null && deploys.length > 0) {
                // we may want to enable this functionality in the future
                // but let's keep things simple for now
                throw new ParameterProblem("Your cluster document includes <deploy> " +
                    "elements, which are not allowed at this time.");
            }

            String name = ws.getName();
            if (name == null || name.trim().length() == 0) {
                throw new ParameterProblem("Every workspace must have a unique "+
                    "name");
            }
            name = name.trim();

            for (Cloudworkspace_Type existingWs : this.workspaceList) {
                String existingWsName = existingWs.getName().trim();

                if (existingWsName.equals(name)) {
                    throw new ParameterProblem("Every workspace must have a " +
                        "unique name");
                }
            }

            this.workspaceList.add(ws);
        }

        if (deployPath == null) {
            throw new ParameterProblem("You must specify a deployment document " +
                "using the --"+Opts.DEPLOY_OPT_STRING+" option");
        }

        final Map<String, Clouddeploy_Type[]> deployMap;

        try {
            final Clouddeployment_Type deployDoc =
                ClusterUtil.parseDeployDocument(deployPath);
            deployMap = ClusterUtil.parseDeployment(deployDoc);

        } catch (DeserializationException de) {
            throw new ParameterProblem("Failed to parse deployment " +
                "document: " + de.getMessage());
        } catch (IOException ie) {
            throw new ParameterProblem("Failed to read deployment "+
                "document: "+ ie.getMessage());
        }

        handleDeploymentMap(deployMap);

        if (this.needsBroker) {
            if (brokerUrl == null || brokerId == null) {
                throw new ParameterProblem("You must specify a valid Context " +
                    "Broker URL and identity string in your properties file ("+
                    this.args.getPropertiesPath()+") keys: "+
                    Props.KEY_BROKER_URL+" and " + Props.KEY_BROKER_IDENTITY
                );
            }
        }

        if (sshFile == null) {
            this.print.info("\nWarning: a SSH public key was not specified. " +
                "It will not be installed into cluster.");
        }

        if (duration <= 0) {
            throw new ParameterProblem("Please specify a valid duration (--"+
                Opts.HOURS_OPT_STRING+")");
        }

        if (pollMs <= 0) {
            throw new ParameterProblem("Please specify a valid polling " +
                "frequency in the properties file ("+
                this.args.getPropertiesPath()+")");
        }

    }

    void handleDeploymentMap(Map<String, Clouddeploy_Type[]> deployMap)
        throws ParameterProblem {

        for (Map.Entry<String,Clouddeploy_Type[]> entry : deployMap.entrySet()) {

            final String workspaceName = entry.getKey();
            int workspaceIndex = getWorkspaceIndex(workspaceName);
            if (workspaceIndex == -1) {
                throw new ParameterProblem("Deployment document contains "+
                    "a workspace: '"+ entry.getKey()+"' which is not present "+
                    "in the cluster definition");
            }

            final Cloudworkspace_Type workspace =
                this.cluster.getWorkspace(workspaceIndex);

            final short workspaceQuantity =
                workspace.getQuantity();

            short foundQuantity = 0;

            for (Clouddeploy_Type deploy : entry.getValue()) {

                String cloudName = deploy.getCloud();
                if (cloudName != null) {
                    cloudName = cloudName.trim();
                }
                if (cloudName == null || cloudName.length() == 0) {
                    throw new ParameterProblem("Invalid <cloud> name");
                }

                foundQuantity += deploy.getQuantity();

                CloudDeployment cloud = getDeploymentByName(cloudName);

                final String localNicPrefix =
                    cloud.getCloud().getBrokerLocalNicPrefix();

                final String publicNicPrefix =
                    cloud.getCloud().getBrokerPublicNicPrefix();

                ClusterMember member = ClusterUtil.parseRequest(this.cluster,
                    workspaceIndex, this.print, localNicPrefix, publicNicPrefix);

                if (member.getClusterForUserData() != null) {
                    needsBroker = true;
                }

                final MemberDeployment md = new MemberDeployment(member, deploy);
                cloud.addMember(md);
            }

            if (foundQuantity != workspaceQuantity) {
                throw new ParameterProblem("The workspace '"+workspaceName+
                    "' has a quantity of "+workspaceQuantity+" but the total "+
                    "quantity of all deployments of this workspace is "+
                    foundQuantity);
            }
        }

        // we already know that every deployment workspace is a valid
        // ClusterMember, so if the sizes match we can be sure that every
        // ClusterMember is accounted for
        if (deployMap.size() != this.workspaceList.size()) {
            throw new ParameterProblem("There must be a deployment entry "+
                "for each defined workspace.");
        }


    }

    private int getWorkspaceIndex(String key) {
        for (int i = 0; i < workspaceList.size(); i++) {
            Cloudworkspace_Type ws = workspaceList.get(i);
            if (ws.getName().trim().equals(key)) {
                return i;
            }
        }
        return -1;
    }

    private synchronized CloudDeployment getDeploymentByName(String name)
        throws ParameterProblem {
        CloudDeployment cloud = this.cloudMap.get(name);
        if (cloud == null) {
            cloud = new CloudDeployment(this.cloudManager.getCloudByName(name));
            this.cloudMap.put(name, cloud);
        }
        return cloud;
    }

    private void doDeploy() throws ExecutionProblem, ExitNow {

        printDeploymentInfo();

        executeUtil.startMultiCloudCluster(
            this.cloudMap.values(),
            this.args.getHistoryDirectory(),
            this.args.getPollMs(),
            this.print,
            this.args.getBrokerURL(),
            this.args.getBrokerID(),
            this.args.getSshfile(),
            this.args.getDurationMinutes()
        );

    }

    private void printDeploymentInfo() {
        if (this.cloudMap.size() == 1) {
            this.print.infoln("\nRequesting single cloud cluster:");
        } else {
            this.print.infoln("\nRequesting multi cloud cluster:");
        }

        for (CloudDeployment cloud : this.cloudMap.values()) {
            this.print.infoln("Cloud '"+cloud.getCloud().getName()+"'");

            for (MemberDeployment member : cloud.getMembers()) {
                String inststr = " instance";
                if (member.getInstanceCount() > 1) {
                    inststr += "s";
                }

                this.print.infoln("  - "+member.getMember().getPrintName()+
                    ": image '"+member.getImageName()+"', "+
                    member.getInstanceCount() +inststr);
            }
        }
    }


    private void printHelp() throws ExecutionProblem {


        String help = null;
        try {
            help = getHelpString();
        } catch (IOException e) {
            throw new ExecutionProblem("Failed to get help string", e);
        }

        this.print.infoln(help);

    }

    private String getHelpString() throws IOException {
        InputStream is = null;
        String help = "";
        try {
            is = this.getClass().getResourceAsStream("meta-cloud-help.txt");
            help = StringUtils.getTextFileViaInputStream(is);
        } finally {
            if (is != null) {
                is.close();
            }
        }
        return help;
    }


}
