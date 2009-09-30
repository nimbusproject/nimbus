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

package org.globus.workspace.cloud.client.util;

import edu.emory.mathcs.backport.java.util.concurrent.ExecutionException;
import edu.emory.mathcs.backport.java.util.concurrent.ExecutorService;
import edu.emory.mathcs.backport.java.util.concurrent.Executors;
import edu.emory.mathcs.backport.java.util.concurrent.FutureTask;
import org.apache.axis.types.URI;
import org.apache.axis.message.addressing.EndpointReferenceType;
import org.globus.workspace.client_common.BaseClient;
import org.globus.workspace.client_core.ExecutionProblem;
import org.globus.workspace.client_core.ExitNow;
import org.globus.workspace.client_core.ParameterProblem;
import org.globus.workspace.client_core.print.PrCodes;
import org.globus.workspace.client_core.utils.FileUtils;
import org.globus.workspace.client_core.utils.StringUtils;
import org.globus.workspace.client_core.utils.EPRUtils;
import org.globus.workspace.cloud.client.Opts;
import org.globus.workspace.cloud.client.cluster.KnownHostsTask;
import org.globus.workspace.cloud.client.tasks.ClusterDoneTask;
import org.globus.workspace.cloud.client.tasks.ClusterMonitorTask;
import org.globus.workspace.cloud.client.tasks.ContextMonitorTask;
import org.globus.workspace.cloud.client.tasks.CreateContextTask;
import org.globus.workspace.cloud.client.tasks.DestroyTask;
import org.globus.workspace.cloud.client.tasks.FactoryQueryTask;
import org.globus.workspace.cloud.client.tasks.QueryTask;
import org.globus.workspace.cloud.client.tasks.RunTask;
import org.globus.workspace.cloud.client.tasks.SaveTask;
import org.globus.workspace.common.client.CommonPrint;
import org.globus.workspace.common.print.Print;
import org.globus.workspace.common.print.PrintOpts;
import org.nimbustools.ctxbroker.generated.gt4_0.description.BrokerContactType;
import org.nimbustools.ctxbroker.generated.gt4_0.description.Cloudcluster_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.description.Nimbusctx_Type;
import org.nimbustools.messaging.gt4_0.generated.metadata.VirtualWorkspace_Type;
import org.nimbustools.messaging.gt4_0.generated.negotiable.WorkspaceDeployment_Type;
import org.nimbustools.messaging.gt4_0.common.CommonUtil;

import javax.xml.namespace.QName;
import java.io.File;
import java.io.PrintStream;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;

public class ExecuteUtil {

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    final ExecutorService executor;
    final boolean okToShutdownExecutorService;


    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    public ExecuteUtil() {
        this.executor = Executors.newCachedThreadPool();
        this.okToShutdownExecutorService = true;
    }

    public ExecuteUtil(ExecutorService executorService, boolean okToShutdown) {
        if (executorService == null) {
            throw new IllegalArgumentException(
                                "executorService may not be null");
        }
        this.executor = executorService;
        this.okToShutdownExecutorService = okToShutdown;
    }

    
    // -------------------------------------------------------------------------
    // GENERAL
    // -------------------------------------------------------------------------

    public synchronized void stopExecutorService() {

        if (!this.okToShutdownExecutorService) {
            return;
        }

        if (!this.executor.isShutdown()) {
            this.executor.shutdownNow();
        }
    }


    // -------------------------------------------------------------------------
    // SEND FILE
    // -------------------------------------------------------------------------

    public void sendFile(String sourceUrlString,
                         String destUrlString,
                         long timeoutMinutes,
                         String identAuthz,
                         PrintStream info,
                         PrintStream debug)

            throws ExecutionProblem {

        RepositoryUtil.sendFile(sourceUrlString, destUrlString, timeoutMinutes,
                                identAuthz, info, debug, this.executor);
    }

    // -------------------------------------------------------------------------
    // DELETE FILE
    // -------------------------------------------------------------------------

    public void deleteFile(String delUrlString,
                           String identAuthz,
                           PrintStream info,
                           PrintStream debug)

            throws ExecutionProblem {

        RepositoryUtil.deleteFile(delUrlString, identAuthz, info, debug);
    }

    // -------------------------------------------------------------------------
    // DOWNLOAD FILE
    // -------------------------------------------------------------------------

    public void downloadFile(String sourceUrlString,
                             String destUrlString,
                             long timeoutMinutes,
                             String identityAuthorization,
                             PrintStream info,
                             PrintStream debug)

            throws ExecutionProblem {

        // same impl
        this.sendFile(sourceUrlString,
                      destUrlString,
                      timeoutMinutes,
                      identityAuthorization,
                      info,
                      debug);
    }

    // -------------------------------------------------------------------------
    // LIST FILES
    // -------------------------------------------------------------------------

    public FileListing[] listFiles(String url,
                                   String identAuthz,
                                   PrintStream info,
                                   PrintStream err,
                                   PrintStream debug) throws ExecutionProblem {

        return RepositoryUtil.listFiles(url, identAuthz, info, err, debug);
    }

    // -------------------------------------------------------------------------
    // STATUS QUERY (query one)
    // -------------------------------------------------------------------------

    public void statusQuery(String eprPath,
                            String idAuthz,
                            Print print)

            throws ExecutionProblem, ExitNow {

        String shortName = null;
        final File epr = new File(eprPath);
        final String dirName = epr.getParentFile().getName();

        if (dirName.startsWith(HistoryUtil.historyClusterDirPrefix)) {
            throw new ExecutionProblem("Cluster handles are not supported, " +
                    "use the \"--" + Opts.STATUS_CHECK_OPT_STRING +
                    "\" option by itself instead.");
        }

        if (dirName.startsWith(HistoryUtil.historyDirPrefix)) {

            print.debugln("Surmising from EPR parent directory that " +
                          "short name is '" + dirName +
                          "' (used for printing only)");

            shortName = dirName;
        }

        print.infoln("Querying for '" + dirName + "' information:");
        print.infoln("  - Workspace handle (EPR): '" + eprPath + "'");

        final FutureTask task =
                new FutureTask(
                        new QueryTask(eprPath, idAuthz, shortName,
                                      print));
        
        this.executor.submit(task);

        try {
            final Integer retCode = (Integer) task.get();
            if (retCode.intValue() != BaseClient.SUCCESS_EXIT_CODE) {
                throw new ExitNow(retCode.intValue());
            }
        } catch (InterruptedException e) {
            throw new ExecutionProblem(e.getMessage(), e);
        } catch (ExecutionException e) {
            throw new ExecutionProblem(e.getMessage(), e);
        }
    }


    // -------------------------------------------------------------------------
    // ASSOCIATION QUERY
    // -------------------------------------------------------------------------

    public void associationQuery(String workspaceFactoryURL,
                                 String factoryID,
                                 Print print)

            throws ExecutionProblem, ExitNow {

        print.infoln("Querying networks.");

        // don't mess with the main print system, make a new one
        final int[] printThese = {PrCodes.FACTORYRPQUERY__ASSOCS};
        final PrintOpts newOpts = new PrintOpts(printThese);
        final Print newprint = new Print(newOpts,
                                         print.getInfoProxy(),
                                         print.getErrProxy(),
                                         print.getDebugProxy());

        final FutureTask task =
                new FutureTask(
                        new FactoryQueryTask(workspaceFactoryURL,
                                             factoryID,
                                             newprint));

        this.executor.submit(task);

        try {
            final Integer retCode = (Integer) task.get();
            if (retCode.intValue() != BaseClient.SUCCESS_EXIT_CODE) {
                throw new ExitNow(retCode.intValue());
            }
        } catch (InterruptedException e) {
            throw new ExecutionProblem(e.getMessage(), e);
        } catch (ExecutionException e) {
            throw new ExecutionProblem(e.getMessage(), e);
        }
    }


    // -------------------------------------------------------------------------
    // SHUTDOWN-SAVE
    // -------------------------------------------------------------------------

    public void save(String newSaveTargetPrintName,
                     String newSaveTargetURL,
                     String eprPath,
                     long pollMs,
                     boolean useNotifications,
                     String identityAuthorization,
                     Print print)

            throws ExecutionProblem, ExitNow {

        print.infoln("\nSaving workspace.");
        print.infoln("  - Workspace handle (EPR): '" + eprPath + "'");
        if (newSaveTargetPrintName != null) {
            print.infoln("  - New name: '" + newSaveTargetPrintName + "'");
        }

        final File epr = new File(eprPath);

        String shortName = null;

        final String dirName = epr.getParentFile().getName();
        if (dirName.startsWith(HistoryUtil.historyClusterDirPrefix)
                || dirName.startsWith(HistoryUtil.historyDirPrefix)) {
            
            print.debugln("Surmising from EPR parent directory that " +
                          "short name is '" + dirName +
                          "' (used for printing only)");

            shortName = dirName;
        }

        boolean disableAllStateChecks = false;
        if (pollMs < 1 && !useNotifications) {
            disableAllStateChecks = true;
        }

        final SaveTask sTask = new SaveTask(eprPath,
                                            newSaveTargetURL,
                                            identityAuthorization,
                                            pollMs,
                                            disableAllStateChecks,
                                            shortName,
                                            print);

        final FutureTask task = new FutureTask(sTask);
        this.executor.submit(task);

        try {
            final Integer retCode = (Integer) task.get();
            if (retCode.intValue() != BaseClient.SUCCESS_EXIT_CODE) {
                throw new ExitNow(retCode.intValue());
            }
        } catch (InterruptedException e) {
            throw new ExecutionProblem(e.getMessage(), e);
        } catch (ExecutionException e) {
            throw new ExecutionProblem(e.getMessage(), e);
        }
    }


    // -------------------------------------------------------------------------
    // DESTROY
    // -------------------------------------------------------------------------

    public void destroy(String eprPath,
                        String identityAuthorization,
                        boolean isClusterHandle,
                        Print print)

            throws ExecutionProblem, ExitNow {

        if (isClusterHandle) {
            print.infoln("\nTerminating cluster.");
            print.infoln("  - Cluster handle (EPR): '" + eprPath + "'");
        } else {
            print.infoln("\nTerminating workspace.");
            print.infoln("  - Workspace handle (EPR): '" + eprPath + "'");
        }

        final File epr = new File(eprPath);

        String dirName = epr.getParentFile().getName();
        if (dirName.startsWith(HistoryUtil.historyClusterDirPrefix)
                || dirName.startsWith(HistoryUtil.historyDirPrefix)) {
            print.debugln("Surmising from EPR parent directory that " +
                   "short name is '" + dirName + "' (used for printing only)");
        } else {
            dirName = null;
        }

        final DestroyTask dTask = new DestroyTask(eprPath,
                                                  identityAuthorization,
                                                  dirName,
                                                  print);

        final FutureTask task = new FutureTask(dTask);
        this.executor.submit(task);

        try {
            final Integer retCode = (Integer) task.get();
            if (retCode.intValue() != BaseClient.SUCCESS_EXIT_CODE) {
                throw new ExitNow(retCode.intValue());
            }
        } catch (InterruptedException e) {
            throw new ExecutionProblem(e.getMessage(), e);
        } catch (ExecutionException e) {
            throw new ExecutionProblem(e.getMessage(), e);
        }
    }

    
    // -------------------------------------------------------------------------
    // START VIRTUAL CLUSTER
    // -------------------------------------------------------------------------

    public void startWorkspaceCluster(String workspaceFactoryURL,
                                      KnownHostsTask[] knownHostTasks,
                                      VirtualWorkspace_Type[] metadatas,
                                      String[] metadata_fileNames,
                                      WorkspaceDeployment_Type[] deploymentRequests,
                                      String[] deploymentRequest_fileNames,
                                      String[] printNames,
                                      Cloudcluster_Type[] clustersForUserData,
                                      boolean doContextLock,
                                      String sshfile,
                                      String ssh_hostsfile,
                                      String historyDir,
                                      long pollMs,
                                      String identityAuthorization,
                                      Print print,
                                      String brokerURL,
                                      String brokerIdentityAuthorization)

            throws ExecutionProblem, ExitNow {

        final File topdir;
        try {
            topdir = CloudClientUtil.getHistoryDir(historyDir);
        } catch (ParameterProblem e) {
            throw new ExecutionProblem(e.getMessage(), e);
        }

        final int nextnum = HistoryUtil.findNextClusterNumber(topdir, print);

        final String suffix = HistoryUtil.format.format(nextnum);
        final String newDirName = HistoryUtil.historyClusterDirPrefix + suffix;
        final String clusterHandle = newDirName; // redundant, but clearer later

        print.debugln("Next directory: " + newDirName);

        final File newdir = new File(topdir, newDirName);
        final String newdirPath = newdir.getAbsolutePath();
        if (newdir.mkdir()) {
            print.debugln("Created directory: " + newdirPath);
        } else {
            // could be a race condition on the name, or odd perm problem
            // (note we checked parent dir was writeable)
            throw new ExecutionProblem(
                    "Could not create directory '" + newdirPath + "'");
        }

        final File runLog =
                HistoryUtil.newLogFile(newdir, RunTask.LOG_FILE_NAME, print);
        if (runLog != null) {
            try {
                print.getOpts().setInfoErrFile(runLog.getAbsolutePath());
            } catch (Exception e) {
                print.errln("Problem setting InfoErrFile: " + e.getMessage());
                // carry on
            }
        }

        final File debugLog =
               HistoryUtil.newLogFile(newdir, RunTask.DEBUG_LOG_FILE_NAME, print);
        if (debugLog != null) {
            try {
                print.getOpts().setAllOutFile(debugLog.getAbsolutePath());
            } catch (Exception e) {
                print.errln("Problem setting AllOutFile: " + e.getMessage());
                // carry on
            }
        }

        final File newdir2 = new File(newdirPath, "reports-vm");
        final String newdir2Path = newdir2.getAbsolutePath();
        if (newdir2.mkdir()) {
            print.debugln("Created directory: " + newdir2Path);
        } else {
            throw new ExecutionProblem(
                    "Could not create directory '" + newdir2Path + "'");
        }

        final File newdir5 = new File(newdirPath, "id-ip-dir");
        final String eprIdIpDirPath = newdir5.getAbsolutePath();
        if (newdir5.mkdir()) {
            print.debugln("Created directory: " + eprIdIpDirPath);
        } else {
            throw new ExecutionProblem(
                    "Could not create directory '" + eprIdIpDirPath + "'");
        }

        String knownHostsDirPath = null;
        if (knownHostTasks != null) {
            for (KnownHostsTask knownHostTask : knownHostTasks) {
                if (knownHostTask.perHostDir) {
                    final File newdir6 = new File(newdirPath, "knownhosts-dir");
                    knownHostsDirPath = newdir6.getAbsolutePath();
                    if (newdir6.mkdir()) {
                        print.debugln("Created directory: " + knownHostsDirPath);
                    } else {
                        throw new ExecutionProblem(
                                "Could not create directory '" +
                                        knownHostsDirPath + "'");
                    }
                    break;
                }
            }
        }

        // see comments inside loop about ensemble and context EPRs
        final File ensFile = new File(newdir,
                                      HistoryUtil.ENSEMBLE_EPR_FILE_NAME);
        final String ensembleEprPath = ensFile.getAbsolutePath();

        // do we need to handle contextualization?
        boolean usingContextBroker = false;
        for (Cloudcluster_Type oneclusta : clustersForUserData) {
            if (oneclusta != null) {
                usingContextBroker = true;
                break;
            }
        }

        
        final String ctxEprPath;
        final String ctxReportsDirPath;
        final String[] ctxUserDataPaths = new String[metadatas.length];

        if (!usingContextBroker) {
            ctxEprPath = null;
            ctxReportsDirPath = null;
        } else {

            final File ctxFile = new File(newdir,
                                          HistoryUtil.CONTEXT_EPR_FILE_NAME);
            ctxEprPath = ctxFile.getAbsolutePath();

            final File newdir3 = new File(newdirPath, "reports-ctx");
            ctxReportsDirPath = newdir3.getAbsolutePath();
            if (newdir3.mkdir()) {
                print.debugln("Created directory: " + ctxReportsDirPath);
            } else {
                throw new ExecutionProblem(
                        "Could not create directory '" + ctxReportsDirPath + "'");
            }

            final File newdir4 = new File(newdirPath, "ctx-tmp");
            final String ctxTempDirPath = newdir4.getAbsolutePath();
            if (newdir4.mkdir()) {
                print.debugln("Created directory: " + ctxTempDirPath);
            } else {
                throw new ExecutionProblem(
                        "Could not create directory '" + ctxTempDirPath + "'");
            }

            if (brokerURL == null) {
                int dx = workspaceFactoryURL.indexOf("WorkspaceFactoryService");
                brokerURL = workspaceFactoryURL.substring(0,dx);
                brokerURL += "NimbusContextBroker";
                print.debugln("No context broker URL was explicitly supplied," +
                        " so it has been deduced from the nimbus factory URL." +
                        " Using: " + brokerURL);

                // and so ID scheme for service must be copied
                if (brokerIdentityAuthorization == null
                        && identityAuthorization != null) {
                    brokerIdentityAuthorization = identityAuthorization;
                }
            }

            final BrokerContactType brokerContact;
            try {
                brokerContact = this.createCtx(brokerURL,
                                               brokerIdentityAuthorization,
                                               ctxEprPath,
                                               newdir4,
                                               !doContextLock,
                                               print);
            } catch (Exception e) {
                throw new ExecutionProblem("Problem creating new context at " +
                        "the context broker: " + e.getMessage(), e);
            }

            // prepare user data
            for (int i = 0; i < clustersForUserData.length; i++) {

                final Cloudcluster_Type oneCtx = clustersForUserData[i];

                if (oneCtx == null) {
                    ctxUserDataPaths[i] = null;
                    continue;
                }

                final File datafile = new File(newdir4, "userdata-" + i);

                ctxUserDataPaths[i] = datafile.getAbsolutePath();

                Nimbusctx_Type wrapper = new Nimbusctx_Type();
                wrapper.setCluster(oneCtx);
                wrapper.setContact(brokerContact);

                final QName qName = new QName("", "NIMBUS_CTX");

                try {
                    final String data =
                            StringUtils.axisBeanToString(wrapper, qName);

                    FileUtils.writeStringToFile(data, ctxUserDataPaths[i]);

                } catch (Exception e) {
                    throw new ExecutionProblem("Problem turning the cluster " +
                            "information into text that the context agents " +
                            "on the VMs can consume: " + e.getMessage(), e);
                }
            }
        }

        final String[] allEPRpaths = new String[metadatas.length];

        final FutureTask[] tasks = new FutureTask[metadatas.length];
        for (int i = 0; i < metadatas.length; i++) {

            final String memberName;
            if (printNames[i] == null) {
                memberName = HistoryUtil.getMemberName(i+1);
            } else {
                memberName = printNames[i];
            }

            print.debugln("Creating runtask for " + memberName);
            print.debugln("");

            final String eprPath;
            if (deploymentRequests[i].getNodeNumber() > 1) {

                final File eprFile = new File(newdir, memberName + "-epr");
                eprPath = eprFile.getAbsolutePath();
                print.debugln("Group EPR will be written to:");
                print.debugln("  - '" + eprPath + "'");
                print.debugln("");

            } else {

                final File eprFile = new File(newdir, memberName + "-epr.xml");
                eprPath = eprFile.getAbsolutePath();
                print.debugln("EPR will be written to:");
                print.debugln("  - '" + eprPath + "'");
                print.debugln("");
            }
            allEPRpaths[i] = eprPath;


            /* ensemble related */

            // EPR path is the same for each, the different thing is whether
            // we trigger new ensemble/ctx creation (creating an EPR file) or
            // have the deployment join a previously created ensemble/ctx (using
            // path to the ensemble/ctx EPR file)
            //
            // If you were to not go one by one below and launch all requests
            // at once, the first would still need to be launched first.  The
            // rest would need to wait until the ensemble (and ctx) EPR file
            // appeared on the filesystem.

            boolean newEnsemble = false;
            if (i == 0) {
                newEnsemble = true;
            }

            tasks[i] = this.getWorkspaceTask(workspaceFactoryURL,
                                             eprPath,
                                             metadatas[i],
                                             metadata_fileNames[i],
                                             deploymentRequests[i],
                                             deploymentRequest_fileNames[i],
                                             sshfile,
                                             newdir,
                                             memberName,
                                             pollMs,
                                             identityAuthorization,
                                             true,
                                             null,
                                             ensembleEprPath,
                                             newEnsemble,
                                             true,
                                             print,
                                             ctxUserDataPaths[i],
                                             eprIdIpDirPath);
        }


        // Ensemble requests won't get time assignments, remove the
        // "will be set after lease is secured" messages when registering
        print.getOpts().codeRemove(
                PrCodes.CREATE__INSTANCE_CREATING_INIITIAL_START_TIME);
        print.getOpts().codeRemove(
                PrCodes.CREATE__INSTANCE_CREATING_INIITIAL_SHUTDOWN_TIME);
        print.getOpts().codeRemove(
                PrCodes.CREATE__INSTANCE_CREATING_INIITIAL_TERMINATION_TIME);
        print.getOpts().codeRemove(PrCodes.CREATE__GROUP_ID_PRINT);
        print.getOpts().codeRemove(PrCodes.ENSMONITOR__SINGLE_REPORT_NAMES);
        print.getOpts().codeRemove(PrCodes.CTXMONITOR__SINGLE_REPORT_NAMES);

        this.startAllMembersWithBackout(tasks,
                                        ensembleEprPath,
                                        identityAuthorization,
                                        clusterHandle,
                                        print);

        try {

            print.infoln("\nWaiting for launch updates.");

            final long launch_start = System.currentTimeMillis();

            // all started, launch ensemble monitor and wait for all
            // workspaces to move to state 'Running'
            this.clusterMonitor(ensembleEprPath,
                                identityAuthorization,
                                clusterHandle,
                                newdir2Path,
                                pollMs,
                                print);

            final long launch_stop = System.currentTimeMillis();
            print.debugln(timeStr("Hearing about launch",
                                  launch_stop - launch_start));

            if (!usingContextBroker) {
                print.debugln("\ndone (not using context broker)");
                return; // *** EARLY RETURN ***
            }

            print.infoln("\nWaiting for context broker updates.");

            // find specific EPR IPs in order to key the known_hosts request
            // correctly.
            final KnownHostsTask[] sendTasks =
                    hostTaskIDs(knownHostTasks, allEPRpaths,
                                print, eprIdIpDirPath, knownHostsDirPath);

            final long ctx_start = System.currentTimeMillis();
            
            // all 'Running', launch context monitor and wait for all
            // workspaces to report 'OK' to the context broker
            this.ctxMonitor(ctxEprPath,
                            brokerIdentityAuthorization,
                            clusterHandle,
                            ctxReportsDirPath,
                            sendTasks,
                            ssh_hostsfile,
                            pollMs,
                            print);

            final long ctx_stop = System.currentTimeMillis();
            print.debugln(timeStr("Hearing about contextualization",
                                  ctx_stop - ctx_start)
                        + " more after hearing about launch");

        } catch (ExitNow e) {

            print.errln("\nProblem, attempting to destroy cluster.");
            this.destroyCluster(ensembleEprPath,
                                identityAuthorization,
                                clusterHandle,
                                print);
            print.errln("\nDestroyed '" + clusterHandle + "'");

            throw e;

        } catch (ExecutionProblem e) {

            print.errln("\nProblem, attempting to destroy cluster.");
            this.destroyCluster(ensembleEprPath,
                                identityAuthorization,
                                clusterHandle,
                                print);
            print.errln("\nDestroyed '" + clusterHandle + "'");

            throw e;
        }
    }


    // -------------------------------------------------------------------------
    // EC2 CLUSTER HELP
    // -------------------------------------------------------------------------

    public void ec2ClusterHelp(String workspaceFactoryURL,
                               Cloudcluster_Type[] clustersForUserData,
                               boolean doContextLock,
                               String historyDir,
                               long pollMs,
                               String identityAuthorization,
                               Print print,
                               String brokerURL,
                               String brokerIdentityAuthorization,
                               String[] amiNames,
                               int[] amiNums,
                               String ec2ScriptPath)

            throws ExecutionProblem, ExitNow {

        if (amiNames == null || amiNames.length == 0) {
            throw new ExecutionProblem("no amiNames");
        }
        if (amiNums == null || amiNums.length == 0) {
            throw new ExecutionProblem("no amiNums");
        }

        final File topdir;
        try {
            topdir = CloudClientUtil.getHistoryDir(historyDir);
        } catch (ParameterProblem e) {
            throw new ExecutionProblem(e.getMessage(), e);
        }

        final int nextnum = HistoryUtil.findNextEc2ClusterNumber(topdir, print);

        final String suffix = HistoryUtil.format.format(nextnum);
        final String newDirName =
                HistoryUtil.historyEc2ClusterDirPrefix + suffix;
        final String clusterHandle = newDirName; // redundant, but clearer later

        print.debugln("Next directory: " + newDirName);

        final File newdir = new File(topdir, newDirName);
        final String newdirPath = newdir.getAbsolutePath();
        if (newdir.mkdir()) {
            print.debugln("Created directory: " + newdirPath);
        } else {
            // could be a race condition on the name, or odd perm problem
            // (note we checked parent dir was writeable)
            throw new ExecutionProblem(
                    "Could not create directory '" + newdirPath + "'");
        }

        final File runLog =
                HistoryUtil.newLogFile(newdir, RunTask.LOG_FILE_NAME, print);
        if (runLog != null) {
            try {
                print.getOpts().setInfoErrFile(runLog.getAbsolutePath());
            } catch (Exception e) {
                print.errln("Problem setting InfoErrFile: " + e.getMessage());
                // carry on
            }
        }

        final File debugLog =
               HistoryUtil.newLogFile(newdir, RunTask.DEBUG_LOG_FILE_NAME, print);
        if (debugLog != null) {
            try {
                print.getOpts().setAllOutFile(debugLog.getAbsolutePath());
            } catch (Exception e) {
                print.errln("Problem setting AllOutFile: " + e.getMessage());
                // carry on
            }
        }

        // do we need to handle contextualization?
        boolean usingContextBroker = false;
        for (Cloudcluster_Type oneclusta : clustersForUserData) {
            if (oneclusta != null) {
                usingContextBroker = true;
                break;
            }
        }

        final String ctxEprPath;
        final String ctxReportsDirPath;
        final String[] ctxUserDataPaths = new String[amiNames.length];

        if (!usingContextBroker) {
            ctxEprPath = null;
            ctxReportsDirPath = null;
        } else {

            final File ctxFile = new File(newdir,
                                          HistoryUtil.CONTEXT_EPR_FILE_NAME);
            ctxEprPath = ctxFile.getAbsolutePath();

            final File newdir3 = new File(newdirPath, "reports-ctx");
            ctxReportsDirPath = newdir3.getAbsolutePath();
            if (newdir3.mkdir()) {
                print.debugln("Created directory: " + ctxReportsDirPath);
            } else {
                throw new ExecutionProblem(
                    "Could not create directory '" + ctxReportsDirPath + "'");
            }

            final File newdir4 = new File(newdirPath, "ctx-tmp");
            final String ctxTempDirPath = newdir4.getAbsolutePath();
            if (newdir4.mkdir()) {
                print.debugln("Created directory: " + ctxTempDirPath);
            } else {
                throw new ExecutionProblem(
                        "Could not create directory '" + ctxTempDirPath + "'");
            }

            if (brokerURL == null) {
                int dx = workspaceFactoryURL.indexOf("WorkspaceFactoryService");
                brokerURL = workspaceFactoryURL.substring(0,dx);
                brokerURL += "NimbusContextBroker";
                print.debugln("No context broker URL was explicitly supplied," +
                        " so it has been deduced from the nimbus factory URL." +
                        " Using: " + brokerURL);

                // and so ID scheme for service must be copied
                if (brokerIdentityAuthorization == null
                        && identityAuthorization != null) {
                    brokerIdentityAuthorization = identityAuthorization;
                }
            }

            final BrokerContactType brokerContact;
            try {
                brokerContact = this.createCtx(brokerURL,
                                               brokerIdentityAuthorization,
                                               ctxEprPath,
                                               newdir4,
                                               !doContextLock,
                                               print);
            } catch (Exception e) {
                throw new ExecutionProblem("Problem creating new context at " +
                        "the context broker: " + e.getMessage(), e);
            }

            // prepare user data
            for (int i = 0; i < clustersForUserData.length; i++) {

                final Cloudcluster_Type oneCtx = clustersForUserData[i];

                if (oneCtx == null) {
                    ctxUserDataPaths[i] = null;
                    continue;
                }

                final File datafile = new File(newdir4, "userdata-" + i);

                ctxUserDataPaths[i] = datafile.getAbsolutePath();

                Nimbusctx_Type wrapper = new Nimbusctx_Type();
                wrapper.setCluster(oneCtx);
                wrapper.setContact(brokerContact);

                final QName qName = new QName("", "NIMBUS_CTX");

                try {
                    final String data =
                            StringUtils.axisBeanToString(wrapper, qName);

                    FileUtils.writeStringToFile(data, ctxUserDataPaths[i]);

                } catch (Exception e) {
                    throw new ExecutionProblem("Problem turning the cluster " +
                            "information into text that the context agents " +
                            "on the VMs can consume: " + e.getMessage(), e);
                }
            }
        }

        // write the ec2 commands to both screen and a file

        final StringBuffer buf = new StringBuffer();

        if (amiNames.length != amiNums.length) {
            throw new IllegalArgumentException("amiNames.length must equal" +
                    " amiNums.length");
        }
        for (int i = 0; i < amiNames.length; i++) {
            buf.append("\nec2-run-instances")
               .append(" --instance-count ")
               .append(amiNums[i]);

            if (usingContextBroker && ctxUserDataPaths[i] != null) {
                buf.append(" --user-data-file ")
                   .append(ctxUserDataPaths[i]);
            }
            
            buf.append(" --kernel aki-a71cf9ce --ramdisk ari-a51cf9cc");

            // m1.small, m1.large, m1.xlarge, c1.medium, and c1.xlarge
            buf.append(" --instance-type m1.small ");
            buf.append(" --key default ");
            buf.append(amiNames[i]);
            buf.append("\n");
        }

        final String cmds = buf.toString();

        print.infoln("\n** Sample EC2 commands:\n" + cmds);

        if (ec2ScriptPath != null) {

            final String toFile = "#!/bin/sh\n\n" + cmds + "\n";

            try {
                FileUtils.writeStringToFile(toFile, ec2ScriptPath);
            } catch (Exception e) {
                throw new ExecutionProblem("Problem writing sample EC2 " +
                        "commands to file: " + e.getMessage(), e);
            }

            print.infoln("\n** Wrote sample EC2 commands to '" +
                    ec2ScriptPath + "'\n");
        }

        if (!usingContextBroker) {
            print.debugln("\ndone (not using context broker)");
            return; // *** EARLY RETURN ***
        }

        print.infoln("\nWaiting for context broker updates.");

        final long ctx_start = System.currentTimeMillis();

        // all 'Running', launch context monitor and wait for all
        // EC2 VMs to report 'OK' to the context broker
        this.ctxMonitor(ctxEprPath,
                        brokerIdentityAuthorization,
                        clusterHandle,
                        ctxReportsDirPath,
                        null,
                        null,
                        pollMs,
                        print);

        final long ctx_stop = System.currentTimeMillis();
        print.debugln(timeStr("Hearing about contextualization",
                              ctx_stop - ctx_start)
                    + " more after hearing about launch");

    }

    private BrokerContactType createCtx(String brokerURL,
                                        String brokerID,
                                        String createdContextEprPath,
                                        File ctxTempDir,
                                        boolean expectInjections,
                                        Print print)
            throws ExitNow, ExecutionProblem {

        final String sectionTitle = "ACTION: CREATE CONTEXT";
        CommonPrint.printDebugSection(print, sectionTitle);

        final File brokerContactFile = new File(ctxTempDir, "contact.xml");
        final String contactPath = brokerContactFile.getAbsolutePath();

        final CreateContextTask ccTask =
                new CreateContextTask(brokerURL,
                                      createdContextEprPath,
                                      contactPath,
                                      expectInjections,
                                      brokerID,
                                      print);

        final FutureTask task = new FutureTask(ccTask);
        

        this.executor.submit(task);

        final BrokerContactType brokerContact;
        try {
            final Integer retCode = (Integer) task.get();
            if (retCode.intValue() == BaseClient.SUCCESS_EXIT_CODE) {
                brokerContact =
                        FileUtils.getBrokerContactFromFile(print,
                                                           contactPath);
                print.infoln("\nCreated new context with broker.");
            } else {
                print.errln("\nProblem creating new context with broker.");
                throw new ExitNow(retCode.intValue());
            }
        } catch (InterruptedException e) {
            throw new ExecutionProblem(e.getMessage(), e);
        } catch (Exception e) {
            final String err = "Problem creating new context with " +
                    "broker: " + e.getMessage();
            throw new ExecutionProblem(err, e);
        }

        CommonPrint.printDebugSectionEnd(print, sectionTitle);

        return brokerContact;
    }

    private static String timeStr(String opname, long ms) {
        final long sec = ms/1000;
        final long min = sec/60;
        final long secRemaining = sec % 60;
        return "\n[[ " + opname + " took around " + min + " minutes, " +
                secRemaining + " seconds (" + sec  + " seconds)";
    }

    private static KnownHostsTask[] hostTaskIDs(KnownHostsTask[] tasks,
                                                String[] allEPRPaths,
                                                Print pr,
                                                String eprIdIpDirPath,
                                                String knownHostsDirPath) {

        if (tasks == null || tasks.length == 0) {
            return null;
        }

        if (allEPRPaths == null || allEPRPaths.length == 0) {
            throw new IllegalArgumentException("no allEPRPaths?");
        }

        final List newTaskList = new ArrayList(64);

        final IdIpPair[] pairs = getAllIdIpMappings(eprIdIpDirPath);
        if (pairs == null || pairs.length == 0) {
            pr.errln("Could not find IP addresses in order to obtain " +
                     "SSH hostkeys from the context broker.");
            return null;
        }

        // get all workspace IDs and IPs for each member
        for (final KnownHostsTask task : tasks) {
            // eprID was overloaded as member index
            final int[] allids = allIDs(allEPRPaths[task.eprID], pr);
            for (int j = 0; j < allids.length; j++) {

                String printName = task.printName;
                if (printName != null && allids.length > 1) {
                    // append number
                    printName = printName + " #" + j;
                }

                for (IdIpPair pair : pairs) {
                    if (pair.id == allids[j]) {
                        newTaskList.add(
                                new KnownHostsTask(allids[j],
                                                   pair.ip,
                                                   task.interfaceName,
                                                   printName,
                                                   knownHostsDirPath != null,
                                                   knownHostsDirPath));
                        break;
                    }
                }
            }
        }

        return (KnownHostsTask[]) newTaskList.toArray(
                                    new KnownHostsTask[newTaskList.size()]);
    }

    private static IdIpPair[] getAllIdIpMappings(String eprIdIpDirPath) {
        if (eprIdIpDirPath == null) {
            return new IdIpPair[0];
        }
        final File dir = new File(eprIdIpDirPath);
        if (!dir.exists()) {
            return new IdIpPair[0];
        }
        final File[] indvFiles = dir.listFiles();
        if (indvFiles == null) {
            return new IdIpPair[0];
        }

        final ArrayList all = new ArrayList();
        for (File indvFile : indvFiles) {
            String fName = indvFile.getName();
            String[] parts = fName.split("-");
            if (parts == null || parts.length != 2) {
                continue;
            }
            final int id = Integer.parseInt(parts[0]);
            all.add(new IdIpPair(id, parts[1].trim()));
        }

        return (IdIpPair[]) all.toArray(new IdIpPair[all.size()]);
    }

    private static class IdIpPair {
        private final int id;
        private final String ip;
        private IdIpPair(int id, String ip) {
            this.id = id;
            this.ip = ip;
        }
    }

    private static int[] allIDs(String pathPrefix, Print pr) {

        pr.debugln("\nFind allIDs:\n");

        final File f = new File(pathPrefix);
        final File dir = f.getParentFile();

        pr.debugln("f = " + f.getAbsolutePath());
        pr.debugln("dir = " + dir.getAbsolutePath());
        pr.debugln("prefixname = " + f.getName());

        final String[] eprFiles = dir.list(new prefixFilter(f.getName()));
        pr.debugln("eprFiles.length " + eprFiles.length);

        final List intList = new ArrayList(32);

        for (int i = 0; i < eprFiles.length; i++) {
            final File eprFile = new File(dir, eprFiles[i]);
            pr.debugln("onefile = " + eprFile.getAbsolutePath());
            try {
                final EndpointReferenceType epr =
                        FileUtils.getEPRfromFile(eprFile);
                pr.debugln("Found epr:\n" + epr.toString());
                final int id = EPRUtils.getIdFromEPR(epr);
                pr.debugln("\nFound id: " + id);
                intList.add(new Integer(id));
            } catch (Throwable t) {
                final String err = CommonUtil.genericExceptionMessageWrapper(t);
                pr.debugln("Problem examining EPR file @ '" +
                        eprFile.getAbsolutePath() + "': " + err);
                // keep going.
            }
        }

        final int[] ids = new int[intList.size()];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = ((Number)intList.get(i)).intValue();
        }
        return ids;
    }
    private static class prefixFilter implements FilenameFilter {
        private final String prefix;
        private prefixFilter(String prefix) {
            this.prefix = prefix;
        }
        public boolean accept(File dir, String name) {
            final File test = new File(dir, name);
            if (prefix == null) {
                return true;
            }
            return test.getName().startsWith(this.prefix);
        }
    }

    private void startAllMembersWithBackout(FutureTask[] tasks,
                                            String ensembleEprPath,
                                            String identityAuthorization,
                                            String clusterName,
                                            Print print)
            throws ExitNow, ExecutionProblem {

        // Because this is an ensemble request, we go one by one with
        // the workspace service requests to make screen printing nice.
        // Because we've forced no subscriptions, the workspace service
        // requests will return immediately after creation

        // Once all the requests are sent to the workspace service, only then
        // does the ensemble "go" signal get sent in a separate operation
        // (this.clusterDone)


        // If not firstSucceeded, there is nothing to back out
        boolean firstSucceeded = false;

        try {

            for (int i = 0; i < tasks.length; i++) {

                this.executor.submit(tasks[i]);

                final String memberName = HistoryUtil.getMemberName(i+1);

                try {
                    final Integer retCode = (Integer) tasks[i].get();
                    if (retCode.intValue() == BaseClient.SUCCESS_EXIT_CODE) {
                        print.debugln("* Registered " + memberName);
                    } else {
                        print.errln("\nProblem registering " + memberName);
                        throw new ExitNow(retCode.intValue());
                    }
                } catch (InterruptedException e) {
                    throw new ExecutionProblem(e.getMessage(), e);
                } catch (ExecutionException e) {
                    throw new ExecutionProblem(e.getMessage(), e);
                }

                if (i == 0) {
                    firstSucceeded = true;
                    
                    // only print factory URL once
                    print.getOpts().codeRemove(
                            PrCodes.CREATE__FACTORY_ENDPOINT);
                    print.infoln();
                }
            }

            this.clusterDone(ensembleEprPath,
                             identityAuthorization,
                             clusterName,
                             print);

        } catch (ExitNow e) {

            if (firstSucceeded) {
                print.errln("Problem, attempting to destroy cluster.");
                this.destroyCluster(ensembleEprPath,
                                    identityAuthorization,
                                    clusterName,
                                    print);
                print.errln("\nDestroyed '" + clusterName + "'");
            }

            throw e;

        } catch (ExecutionProblem e) {

            if (firstSucceeded) {
                print.errln("Problem, attempting to destroy cluster.");
                this.destroyCluster(ensembleEprPath,
                                    identityAuthorization,
                                    clusterName,
                                    print);
                print.errln("\nDestroyed '" + clusterName + "'");
            }

            throw e;
        }
    }

    private void destroyCluster(String ensembleEprPath,
                                String identityAuthorization,
                                String clusterName,
                                Print print) throws ExitNow, ExecutionProblem {

        final DestroyTask dTask = new DestroyTask(ensembleEprPath,
                                                  identityAuthorization,
                                                  clusterName,
                                                  print);

        final FutureTask task = new FutureTask(dTask);
        this.executor.submit(task);

        try {
            final Integer retCode = (Integer) task.get();
            if (retCode.intValue() != BaseClient.SUCCESS_EXIT_CODE) {
                throw new ExitNow(retCode.intValue());
            }
        } catch (InterruptedException e) {
            throw new ExecutionProblem(e.getMessage(), e);
        } catch (ExecutionException e) {
            throw new ExecutionProblem(e.getMessage(), e);
        }
    }

    private void clusterDone(String ensembleEprPath,
                             String identityAuthorization,
                             String clusterName,
                             Print print) throws ExitNow, ExecutionProblem {

        final ClusterDoneTask dTask =
                new ClusterDoneTask(ensembleEprPath,
                                    identityAuthorization,
                                    clusterName,
                                    print);

        final FutureTask task = new FutureTask(dTask);
        this.executor.submit(task);

        try {
            final Integer retCode = (Integer) task.get();
            if (retCode.intValue() != BaseClient.SUCCESS_EXIT_CODE) {
                throw new ExitNow(retCode.intValue());
            }
        } catch (InterruptedException e) {
            throw new ExecutionProblem(e.getMessage(), e);
        } catch (ExecutionException e) {
            throw new ExecutionProblem(e.getMessage(), e);
        }
    }

    private void clusterMonitor(String ensembleEprPath,
                                String identityAuthorization,
                                String clusterName,
                                String reportDirectory,
                                long pollMs,
                                Print print) throws ExitNow, ExecutionProblem {

        final ClusterMonitorTask mTask =
                new ClusterMonitorTask(ensembleEprPath,
                                       identityAuthorization,
                                       clusterName,
                                       reportDirectory,
                                       pollMs,
                                       print);

        final FutureTask task = new FutureTask(mTask);
        this.executor.submit(task);

        try {
            final Integer retCode = (Integer) task.get();
            if (retCode.intValue() != BaseClient.SUCCESS_EXIT_CODE) {
                throw new ExitNow(retCode.intValue());
            }
        } catch (InterruptedException e) {
            throw new ExecutionProblem(e.getMessage(), e);
        } catch (ExecutionException e) {
            throw new ExecutionProblem(e.getMessage(), e);
        }
    }

    private void ctxMonitor(String ctxEprPath,
                            String identityAuthorization,
                            String clusterName,
                            String reportDirectory,
                            KnownHostsTask[] knownHostTasks,
                            String sshKnownHostsFile,
                            long pollMs,
                            Print print) throws ExitNow, ExecutionProblem {

        final ContextMonitorTask mTask =
                new ContextMonitorTask(ctxEprPath,
                                       identityAuthorization,
                                       clusterName,
                                       reportDirectory,
                                       sshKnownHostsFile,
                                       knownHostTasks,
                                       pollMs,
                                       print);

        final FutureTask task = new FutureTask(mTask);
        this.executor.submit(task);

        try {
            final Integer retCode = (Integer) task.get();
            if (retCode.intValue() != BaseClient.SUCCESS_EXIT_CODE) {
                throw new ExitNow(retCode.intValue());
            }
        } catch (InterruptedException e) {
            throw new ExecutionProblem(e.getMessage(), e);
        } catch (ExecutionException e) {
            throw new ExecutionProblem(e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // START ONE WORKSPACE
    // -------------------------------------------------------------------------

    public void startOneWorkspace(String workspaceFactoryURL,
                                  VirtualWorkspace_Type metadata,
                                  String metadata_fileName,
                                  WorkspaceDeployment_Type deploymentRequest,
                                  String deploymentRequest_fileName,
                                  String sshfile,
                                  String historyDir,
                                  long pollMs,
                                  boolean useNotifications,
                                  String identityAuthorization,
                                  Print print)
            
            throws ExecutionProblem, ExitNow {

        boolean disableAllStateChecks = false;
        if (pollMs < 1 && !useNotifications) {
            disableAllStateChecks = true;
        }

        print.infoln("\nLaunching workspace.");

        final File topdir;
        try {
            topdir = CloudClientUtil.getHistoryDir(historyDir);
        } catch (ParameterProblem e) {
            throw new ExecutionProblem(e.getMessage(), e);
        }

        final int nextnum = HistoryUtil.findNextSingleNumber(topdir, print);

        final String suffix = HistoryUtil.format.format(nextnum);
        final String newDirName = HistoryUtil.historyDirPrefix + suffix;

        print.debugln("Next directory: " + newDirName);

        final File newdir = new File(topdir, newDirName);
        final String newdirPath = newdir.getAbsolutePath();
        if (newdir.mkdir()) {
            print.debugln("Created directory: " + newdirPath);
        } else {
            // could be a race condition on the name, or odd perm problem
            // (note we checked parent dir was writeable)
            throw new ExecutionProblem(
                    "Could not create directory '" + newdirPath + "'");
        }

        final File runLog =
                HistoryUtil.newLogFile(newdir, RunTask.LOG_FILE_NAME, print);
        if (runLog != null) {
            try {
                print.getOpts().setInfoErrFile(runLog.getAbsolutePath());
            } catch (Exception e) {
                print.errln("Problem setting InfoErrFile: " + e.getMessage());
                // carry on
            }
        }

        final File debugLog =
               HistoryUtil.newLogFile(newdir, RunTask.DEBUG_LOG_FILE_NAME, print);
        if (debugLog != null) {
            try {
                print.getOpts().setAllOutFile(debugLog.getAbsolutePath());
            } catch (Exception e) {
                print.errln("Problem setting AllOutFile: " + e.getMessage());
                // carry on
            }
        }

        final File eprFile = new File(newdir, HistoryUtil.SINGLE_EPR_FILE_NAME);
        final String eprPath = eprFile.getAbsolutePath();
        print.debugln("EPR will be written to:");
        print.debugln("  - '" + eprPath + "'");
        print.debugln("");

        final FutureTask task =
                  this.getWorkspaceTask(workspaceFactoryURL,
                                        eprPath,
                                        metadata,
                                        metadata_fileName,
                                        deploymentRequest,
                                        deploymentRequest_fileName,
                                        sshfile,
                                        newdir,
                                        newDirName,
                                        pollMs,
                                        identityAuthorization,
                                        disableAllStateChecks,
                                        "Running",
                                        null,
                                        false,
                                        false,
                                        print,
                                        null,
                                        null);

        this.executor.submit(task);

        try {
            final Integer retCode = (Integer) task.get();
            if (retCode.intValue() == BaseClient.SUCCESS_EXIT_CODE) {
                print.infoln("\nRunning: '" + newDirName + "'");
            } else {
                print.errln("\nProblem running '" + newDirName + "'.");
                throw new ExitNow(retCode.intValue());
            }
        } catch (InterruptedException e) {
            throw new ExecutionProblem(e.getMessage(), e);
        } catch (ExecutionException e) {
            throw new ExecutionProblem(e.getMessage(), e);
        }
    }


    // -------------------------------------------------------------------------
    // START WORKSPACE TASK
    // -------------------------------------------------------------------------

    private FutureTask getWorkspaceTask(String workspaceFactoryURL,
                                        String eprPath,
                                        VirtualWorkspace_Type metadata,
                                        String metadata_fileName,
                                        WorkspaceDeployment_Type deploymentRequest,
                                        String deploymentRequest_fileName,
                                        String sshfile,
                                        File newdir,
                                        String newDirName,
                                        long pollMs,
                                        String identityAuthorization,
                                        boolean disableAllStateChecks,
                                        String exitStateStr,
                                        String ensembleEprPath,
                                        boolean newEnsemble,
                                        boolean forceGroupPrint,
                                        Print print,
                                        String mdUserDataPath,
                                        String ipIdDir)

            throws ExecutionProblem, ExitNow {

        if (identityAuthorization == null) {
            print.debugln("Using host-based authorization of remote server");
        } else {
            print.debugln("Using identity-based authorization of remote " +
                                    "server: '" + identityAuthorization + "'");
        }

        String hostport;
        try {
            final URL url = new URL(workspaceFactoryURL);
            hostport = url.getHost();
            hostport += ":";
            hostport += url.getPort();
        } catch (MalformedURLException e) {
            throw new ExecutionProblem(e.getMessage(), e);
        }

        try {
            final URI runName =
                    new URI("https://" + hostport + "/" + newDirName);
            metadata.setName(runName);
        } catch (URI.MalformedURIException e) {
            throw new ExecutionProblem(e.getMessage(), e);
        }

        final String metadataPath;
        try {
            metadataPath = HistoryUtil.writeMetadata(newdir,
                                                     metadata_fileName,
                                                     metadata);
        } catch (Exception e) {
            throw new ExecutionProblem(e.getMessage(), e);
        }

        print.debugln("Created workspace description:");
        print.debugln("  - '" + metadataPath + "'");

        final String deploymentPath;
        try {
            deploymentPath =
                    HistoryUtil.writeDeployment(newdir,
                                                deploymentRequest_fileName,
                                                deploymentRequest);
        } catch (Exception e) {
            throw new ExecutionProblem(e.getMessage(), e);
        }

        print.debugln("Created deployment request:");
        print.debugln("  - '" + deploymentPath + "'");

        final RunTask runTask = new RunTask(eprPath,
                                            workspaceFactoryURL,
                                            metadataPath,
                                            deploymentPath,
                                            sshfile,
                                            pollMs,
                                            disableAllStateChecks,
                                            newDirName,
                                            identityAuthorization,
                                            exitStateStr,
                                            ensembleEprPath,
                                            newEnsemble,
                                            forceGroupPrint,
                                            mdUserDataPath,
                                            ipIdDir,
                                            print);

        return new FutureTask(runTask);
    }

}
