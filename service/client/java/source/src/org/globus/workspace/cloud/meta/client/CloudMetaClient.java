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
import org.nimbustools.messaging.gt4_0.common.CommonUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.io.IOException;
import java.io.PrintStream;

public class CloudMetaClient {

    private AllArgs args;
    private HashMap<String, ClusterMember> memberMap;
    private HashMap<String, CloudDeployment> cloudMap;

    private CloudManager cloudManager;
    private ExecuteUtil executeUtil = new ExecuteUtil();


    private final Print print;

    public Print getPrint() {
        return this.print;
    }

    public Map<String, ClusterMember> getMemberMap() {
        return this.memberMap;
    }


    public CloudMetaClient(Print pr) {
        if (pr == null) {
            throw new IllegalArgumentException("print may not be null");
        }
        this.print = pr;
        this.memberMap = new HashMap<String, ClusterMember>();
        this.cloudMap = new HashMap<String, CloudDeployment>();

    }

    public static void main(String[] argv) {

        // keeping it simple for now, but basically following the model of CloudClient

        // look for debug early, for diagnosing problems with parsing etc.
        PrintStream debug = null;
        if (CLIUtils.containsDebug(argv)) {
            debug = System.err;
        }


        //TODO wtf are print opts?
        PrintOpts prOpts = new PrintOpts(CloudClient.getOptInPrCodes());

        final Print print = new Print(prOpts, System.out, System.err, debug);
        CloudMetaClient client = new CloudMetaClient(print);

        // TODO needs better error printing
        Throwable anyError = null;
        try {
            mainImpl(client, argv);
        } catch (ParameterProblem e) {
            anyError = e;
        } catch (ExecutionProblem e) {
            anyError = e;
        } catch (ExitNow e) {
            anyError = e;
        }

        int exitCode;
        if (anyError != null) {
            print.err("Error:\n"+anyError.toString());
            exitCode = BaseClient.COMMAND_LINE_EXIT_CODE;
        } else {
            exitCode = BaseClient.SUCCESS_EXIT_CODE;
        }

        print.flush();
        print.close();
        System.exit(exitCode);
    }

    private static void mainImpl(CloudMetaClient client, String[] argv)
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

        //TODO push NIC names into args
        ClusterMember[] members = ClusterUtil.getClusterMembers(clusterPath,
            "priv", "pub", this.print);

        boolean hasInlineDeploy = false;
        boolean needsBroker = false;
        for (ClusterMember member : members) {
            this.memberMap.put(member.getPrintName(), member);

            if (member.hasDeploy()) {
                hasInlineDeploy = true;
            }
            if (member.getClusterForUserData() != null) {
                needsBroker = true;
            }
        }

        if (deployPath != null) {

            // we may want to enable this functionality in the future
            // but let's keep things simple for now

            if (hasInlineDeploy) {
                throw new ParameterProblem("Your cluster document includes <deploy> " +
                    "elements, which are not allowed when specifying an external " +
                    "deploy document");
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

        }

        if (needsBroker) {
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
            final ClusterMember member = this.memberMap.get(entry.getKey());


            if (member == null) {
                // we know that these exceptions will bubble up and abort
                // otherwise we should rollback changes already made

                throw new ParameterProblem("Deployment document contains "+
                    "a workspace:"+ entry.getKey()+" which is not present "+
                    "in the cluster definition");
            }

            // sanity check, this should never happen
            if (member.hasDeploy()) {
                throw new ParameterProblem("Cluster member "+entry.getKey()+
                    " already has deployments");
            }

            for (Clouddeploy_Type deploy : entry.getValue()) {
                final MemberDeployment md = new MemberDeployment(member, deploy);

                String cloudName = deploy.getCloud();
                if (cloudName != null) {
                    cloudName = cloudName.trim();
                }
                if (cloudName == null || cloudName.length() == 0) {
                    throw new ParameterProblem("Invalid <cloud> name");
                }

                CloudDeployment cloud = getDeploymentByName(cloudName);
                cloud.addMember(md);
            }
        }

        // we already know that every deployment workspace is a valid
        // ClusterMember, so if the sizes match we can be sure that every
        // ClusterMember is accounted for
        if (deployMap.size() != this.memberMap.size()) {
            throw new ParameterProblem("There must be a deployment entry "+
                "for each defined workspace.");
        }

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


    private void printHelp() {

        //TODO actually print something
    }


}
