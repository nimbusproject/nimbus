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

import org.globus.workspace.client_core.ExecutionProblem;
import org.globus.workspace.client_core.utils.FileUtils;
import org.globus.workspace.client_core.utils.StringUtils;
import org.globus.workspace.common.print.Print;
import org.globus.wsrf.encoding.ObjectSerializer;
import org.nimbustools.ctxbroker.generated.gt4_0.description.BrokerContactType;
import org.nimbustools.ctxbroker.generated.gt4_0.description.Cloudcluster_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.description.Nimbusctx_Type;
import org.nimbustools.messaging.gt4_0.generated.metadata.VirtualWorkspace_Type;
import org.nimbustools.messaging.gt4_0.generated.negotiable.WorkspaceDeployment_Type;

import javax.xml.namespace.QName;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.NumberFormat;

public class HistoryUtil {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    public static final String SINGLE_EPR_FILE_NAME = "vw-epr.xml";
    public static final String ENSEMBLE_EPR_FILE_NAME = "cluster-epr.xml";
    public static final String CONTEXT_EPR_FILE_NAME = "context-epr.xml";

    public static final String LOG_SUFFIX = "-log.txt";

    public static final String historyDirPrefix = "vm-";
    public static final String historyClusterDirPrefix = "cluster-";
    public static final String historyMultiClusterDirPrefix = "multicluster-";
    public static final String historyEc2ClusterDirPrefix = "ec2cluster-";
    private static final String historyClusterMemberPrefix = "member-";

    // controls the naming scheme for rundir suffixes, if e.g. set to 3,
    // numbers will be at least 3 digits:  007, 021, 999, 1001, etc.
    public static final int NUM_SUFFIX_MIN_CHARACTERS = 3;

    static final NumberFormat format = NumberFormat.getInstance();
    static {
        format.setMinimumIntegerDigits(NUM_SUFFIX_MIN_CHARACTERS);
    }

    // controls the naming scheme for member suffixes, if e.g. set to 2,
    // numbers will be at least 2 digits:  07, 21, 99, 101, etc.
    public static final int NUM_MEMBER_SUFFIX_MIN_CHARACTERS = 2;

    static final NumberFormat memberFormat = NumberFormat.getInstance();
    static {
        memberFormat.setMinimumIntegerDigits(NUM_MEMBER_SUFFIX_MIN_CHARACTERS);
    }


    // -------------------------------------------------------------------------
    // UTILS
    // -------------------------------------------------------------------------

    public static String getMemberName(int number) {
        return historyClusterMemberPrefix + memberFormat.format(number);
    }

    public static String writeMetadata(File newdir,
                                       String metadata_fileName,
                                       VirtualWorkspace_Type vw)

            throws Exception {

        final String metadata =
                ObjectSerializer.toString(vw, MetadataXMLUtil.metadataQName);

        final File metadataFile = new File(newdir, metadata_fileName);

        FileWriter fw = null;
        try {
            fw = new FileWriter(metadataFile);
            fw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            fw.write(metadata);
            fw.flush();
        } catch (IOException e) {
            throw new ExecutionProblem(e.getMessage(), e);
        } finally {
            if (fw != null) {
                try {
                    fw.close();
                } catch (IOException e) {
                    throw new ExecutionProblem(e.getMessage(), e);
                }
            }
        }

        return metadataFile.getAbsolutePath();
    }

    public static String writeDeployment(File newdir,
                                         String deploymentRequest_fileName,
                                         WorkspaceDeployment_Type dep)
            throws Exception {

        final String metadata =
                ObjectSerializer.toString(dep,
                                          DeploymentXMLUtil.deploymentQName);

        final File reqFile = new File(newdir, deploymentRequest_fileName);

        FileWriter fw = null;
        try {
            fw = new FileWriter(reqFile);
            fw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            fw.write(metadata);
            fw.flush();
        } catch (IOException e) {
            throw new ExecutionProblem(e.getMessage(), e);
        } finally {
            if (fw != null) {
                try {
                    fw.close();
                } catch (IOException e) {
                    throw new ExecutionProblem(e.getMessage(), e);
                }
            }
        }

        return reqFile.getAbsolutePath();
    }

    public static String writeUserData(File newdir,
                                       String userdata_fileName,
                                       BrokerContactType brokerContact,
                                       Cloudcluster_Type cluster)
        throws ExecutionProblem {

        if (newdir == null) {
            throw new IllegalArgumentException("newdir may not be null");
        }
        if (userdata_fileName == null) {
            throw new IllegalArgumentException("userdata_fileName may not be null");
        }
        if (brokerContact == null) {
            throw new IllegalArgumentException("brokerContact may not be null");
        }
        if (cluster == null) {
            throw new IllegalArgumentException("cluster may not be null");
        }

        Nimbusctx_Type wrapper = new Nimbusctx_Type();
        wrapper.setCluster(cluster);
        wrapper.setContact(brokerContact);

        final QName qName = new QName("", "NIMBUS_CTX");

        File f = new File(newdir, userdata_fileName);

        try {
            final String data =
                    StringUtils.axisBeanToString(wrapper, qName);

            FileUtils.writeStringToFile(data, f.getAbsolutePath());

            return f.getAbsolutePath();

        } catch (Exception e) {
            throw new ExecutionProblem("Problem turning the cluster " +
                    "information into text that the context agents " +
                    "on the VMs can consume: " + e.getMessage(), e);
        }
    }

    public static File newLogFile(File dir,
                                  String baseName,
                                  Print print) {
        try {
            return newLogFileImpl(dir, baseName, print);
        } catch (Throwable t) {
            print.errln("newLogFile problem: " + t.getMessage());
            print.debugln("newLogFile problem: " + t.getMessage());
            t.printStackTrace(print.getDebugProxy());
            return null;
        }
    }

    public static int findNextEc2ClusterNumber(File topdir, Print print)
            throws ExecutionProblem {
        return findNextNumber(topdir, print, true, true);
    }

    public static int findNextClusterNumber(File topdir, Print print)
            throws ExecutionProblem {
        return findNextNumber(topdir, print, true, false);
    }

    public static int findNextSingleNumber(File topdir, Print print)
            throws ExecutionProblem {
        return findNextNumber(topdir, print, false, false);
    }


    // -------------------------------------------------------------------------
    // IMPL (private)
    // -------------------------------------------------------------------------

    private static int findNextNumber(File topdir,
                                      Print print,
                                      boolean cluster,
                                      boolean ec2)
            throws ExecutionProblem {

        if (!cluster && ec2) {
            throw new IllegalArgumentException("cluster=false, ec2=true is " +
                    "not accepted");
        }

        final String[] subdirs = topdir.list(new dirFilter());
        if (subdirs == null) {
            throw new ExecutionProblem("Problem examining history dir '" +
                    topdir.getAbsolutePath() + "'");
        }

        print.debugln("history subdirs length: " + subdirs.length);

        final String PREFIX;
        final int PREFIXLEN;
        if (ec2) {
            PREFIX = historyEc2ClusterDirPrefix;
            PREFIXLEN = historyEc2ClusterDirPrefix.length();
        } else if (cluster) {
            PREFIX = historyClusterDirPrefix;
            PREFIXLEN = historyClusterDirPrefix.length();
        } else {
            PREFIX = historyDirPrefix;
            PREFIXLEN = historyDirPrefix.length();
        }

        int highestNumber = 0;
        for (String subdir : subdirs) {

            // this gets annoying
            //if (debug != null) {
            //    debug.println("examining history subdir: " + subdirs[i]);
            //}

            if (subdir.startsWith(PREFIX)) {
                try {
                    final String intPart =
                            subdir.substring(PREFIXLEN);
                    final int number = Integer.parseInt(intPart);
                    if (number > highestNumber) {
                        highestNumber = number;
                    }
                } catch (Throwable t) {
                    print.debugln("  Problem: " + t.getMessage());
                }
            }
        }

        return highestNumber + 1;
    }

    private static class dirFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            final File test = new File(dir, name);
            return test.isDirectory();
        }
    }

    private static File newLogFileImpl(File dir,
                                       String baseName,
                                       Print print) {

        final String fileName = baseName + LOG_SUFFIX;
        final File logFile = new File(dir, fileName);

        print.debugln("Attempting to find newLogFile for '" + baseName + "'");
        print.debugln("First trying '" + logFile.getAbsolutePath() + "'");

        IOException exc = null;
        boolean created = false;
        try {
            created = logFile.createNewFile();
        } catch (IOException e) {
            exc = e;
        }

        if (created) {
            print.debugln("That works.");
            return logFile;
        }

        final int attemptNum = nextLogNum(dir, baseName, print);

        print.debugln("\nnextLogNum finds: " + attemptNum);

        IOException exc2 = null;
        boolean created2 = false;
        if (attemptNum > 0) {
            final String newSuffix = format.format(attemptNum) + LOG_SUFFIX;
            final String fileName2 = baseName + "-" + newSuffix;
            final File logFile2 = new File(dir, fileName2);

            print.debugln("Second file creation attempt, trying '" +
                                        logFile2.getAbsolutePath() + "'");

            try {
                created2 = logFile2.createNewFile();
            } catch (IOException e) {
                exc2 = e;
            }

            if (created2) {
                print.debugln("That works.");
                return logFile2;
            }

        }

        if (exc != null || exc2 != null) {

            String msg = "";
            if (exc != null) {
                msg += " [ " + exc.getMessage() + " ]";
            }
            if (exc2 != null) {
                msg += " [ " + exc2.getMessage() + " ]";
            }

            print.errln("Problem creating " + fileName + ": " + msg);
        } else {
            print.errln("Problem creating " + fileName);
        }

        return null;
    }

    private static int nextLogNum(File dir,
                                  String baseName,
                                  Print print) {

        final File[] files = dir.listFiles();
        int highestNumber = 1;
        for (int i = 0; i < files.length; i++) {

            final String name = files[i].getName();
            if (name.startsWith(baseName)) {
                try {
                    if (name.length() <=
                            baseName.length() + LOG_SUFFIX.length()) {
                        continue;
                    }
                    final int suffidx = name.length() - LOG_SUFFIX.length();
                    final String noSuffix = name.substring(0, suffidx);
                    final String intPart =
                            noSuffix.substring(baseName.length() + 1);
                    final int number = Integer.parseInt(intPart);
                    if (number > highestNumber) {
                        highestNumber = number;
                    }

                } catch (Throwable t) {
                    print.debugln("(lognum resolving) " + t.getMessage());
                }
            }
        }
        return highestNumber + 1;
    }

}
