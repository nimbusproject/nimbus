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

package org.globus.workspace.xen;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.globus.workspace.Lager;
import org.globus.workspace.RepoFileSystemAdaptor;
import org.globus.workspace.ReturnException;
import org.globus.workspace.WorkspaceConstants;
import org.globus.workspace.WorkspaceException;
import org.globus.workspace.WorkspaceUtil;
import org.globus.workspace.cmdutils.SSHUtil;
import org.globus.workspace.service.binding.vm.VirtualMachine;
import org.globus.workspace.service.binding.vm.VirtualMachineDeployment;
import org.globus.workspace.service.binding.vm.VirtualMachinePartition;
import org.globus.workspace.service.binding.vm.CustomizationNeed;

import org.nimbustools.api.repr.vm.NIC;

import java.util.ArrayList;
import java.util.Iterator;

public class XenUtil implements WorkspaceConstants {

    public static final String XEN_NAME_PREFIX = "wrksp-";

    private static final Log logger =
        LogFactory.getLog(XenUtil.class.getName());

    private static final String NO_WRKSP =
            "worksp executable path (for compute node) is not configured";

    private static final String ERR_1 =
            "Problem with RM framework or finding executables";

    private static final String ERR_2 =
            "Problem with parameters to workspace driver, incorrectly " +
                    "supplied or validation of request failed";

    private static final String ERR_3 =
            "Problem with workspace driver, error executing request " +
                    "operation (but validation succeeded)";

    private static final String ERR_9 =
            "Problem with workspace driver, executing request succeed " +
                    "but something wrong with backend driver after success";

    private static final String ERR_OTHER= "Unexpected issue";

    // should be more private and encapsulated, see notes in Binding
    // should also be checks that none of the contents already contain
    // the markers
    public static final String WC_FIELD_SEPARATOR = ";";
    public static final String WC_GROUP_SEPARATOR = ";;";

    /* The path on the compute or local node to workspace_control */
    private static String worksp;
    private static RepoFileSystemAdaptor nsTrans;


    public static void setWorksp(String path) {
        if (worksp != null) {
            return;
        }
        worksp = path;
    }

    public static void setRepoAdaptor(RepoFileSystemAdaptor nsT) {
        if (nsTrans != null) {
            return;
        }
        nsTrans = nsT;
    }

    public static RepoFileSystemAdaptor getNsTrans() {
        return nsTrans;
    }

    public static ArrayList constructRemoveCommand(VirtualMachine vw,
                                                   boolean trash)
            throws WorkspaceException {

        if (vw == null) {
            final String err = "vw is null";
            logger.error(err);
            throw new IllegalArgumentException(err);
        }

        if (worksp == null) {
            logger.error(NO_WRKSP);
            throw new WorkspaceException(NO_WRKSP);
        }

        final ArrayList cmd = new ArrayList(8);

        /* The python program can have teardown tasks associated with it
           or those can be overriden here if they can't be predetermined
           or if it is not convenient to predetermine them. */

        cmd.add(worksp);
        cmd.add("--remove");

        if (trash) {
            cmd.add("--deleteall");
        }

        cmd.add("--name");
        cmd.add(xenName(vw));

        if (logger.isDebugEnabled()) {
            cmd.add("--loglevel");
            cmd.add("DEBUG");
        }

        return cmd;
    }

    public static ArrayList constructPropagateCommand(VirtualMachine vm,
                                                      String notificationInfo)
            throws WorkspaceException {

        if (vm == null) {
            final String err = "vm is null";
            logger.error(err);
            throw new IllegalArgumentException(err);
        }

        if (worksp == null) {
            logger.error(NO_WRKSP);
            throw new WorkspaceException(NO_WRKSP);
        }

        final ArrayList cmd = new ArrayList(16);

        cmd.add(worksp);
        cmd.add("--propagate");

        cmd.add("--name");
        cmd.add(xenName(vm));

        // For right now, only supporting propagate for one image, the rootdisk.
        // Waiting on generalization of disk and propagation (both in WSDL and
        // file movement tools)
        final VirtualMachinePartition[] partitions = vm.getPartitions();
        if (partitions != null) {
            cmd.add("--images");
            for (int i = 0; i < partitions.length; i++) {
                if (partitions[i].isRootdisk()) {
                    String img = partitions[i].getImage();
                    if(nsTrans != null) {
                        img = nsTrans.translateExternaltoInternal(img, vm);
                    }
                    cmd.add("'"+img+"'");
                    break;
                }
            }
        }     

        if (notificationInfo != null) {
            cmd.add("--notify");
            cmd.add(notificationInfo);
        }

        return cmd;
    }

    public static ArrayList constructUnpropagateCommand(VirtualMachine vm,
                                                       String notificationInfo)
            throws WorkspaceException {

        if (vm == null) {
            final String err = "vm is null";
            logger.error(err);
            throw new IllegalArgumentException(err);
        }
                                                             
        if (worksp == null) {
            logger.error(NO_WRKSP);
            throw new WorkspaceException(NO_WRKSP);
        }

        final ArrayList cmd = new ArrayList(16);

        cmd.add(worksp);
        cmd.add("--unpropagate");

        cmd.add("--name");
        cmd.add(xenName(vm));

        // source is the new target: for read-only images, use file://
        // and a local or local-shared filesystem (and therefore no
        // propagation) or also use the requested shutdown state of
        // Shutdown-Trash (which would propagate the image but when
        // you're done, trash any changes)

        // alternate propagation target may be supplied now
        String altTargets = null;

        // for right now, only supporting propagate for one image, the rootdisk
        final VirtualMachinePartition[] partitions = vm.getPartitions();
        if (partitions != null) {
            cmd.add("--images");
            for (int i = 0; i < partitions.length; i++) {
                if (partitions[i].isRootdisk()) {
                    String img = partitions[i].getImage();
                    if(nsTrans != null) {
                        img = nsTrans.translateExternaltoInternal(img, vm);
                    }
                    cmd.add("'"+img+"'");
                    altTargets = partitions[i].getAlternateUnpropTarget();
                    break;
                }
            }
        }

        if (altTargets != null) {
            cmd.add("--unproptargets");
            String img = altTargets;
            if(nsTrans != null) {
                img = nsTrans.translateExternaltoInternal(altTargets, vm);
            }
            cmd.add("'"+img+"'");
        }

        if (notificationInfo != null) {
            cmd.add("--notify");
            cmd.add(notificationInfo);
        }

        return cmd;
    }

    public static ArrayList constructCreateCommand(VirtualMachine vw,
                                                   boolean startpaused)
            throws WorkspaceException {

        return constructCreateCommand(vw, startpaused, null);
    }


    private static String convertToAlreadyPropagated(String name, VirtualMachine vm)
            throws WorkspaceException {

        String img = name;
        if(nsTrans != null) {
            img = nsTrans.translateExternaltoInternal(img, vm);
        }
        int ndx = img.lastIndexOf('/');

        if(ndx > 0)
        {
            img = img.substring(ndx+1);
        }
        return "file://" + img;
    }

    public static ArrayList constructCreateCommand(VirtualMachine vm,
                                                   boolean startpaused,
                                                   String notificationInfo)
            throws WorkspaceException {

        if (vm == null) {
            final String err = "vm is null";
            logger.error(err);
            throw new IllegalArgumentException(err);
        }

        // not possible, Home initializes
        if (worksp == null) {
            logger.error(NO_WRKSP);
            throw new WorkspaceException(NO_WRKSP);
        }

        final ArrayList cmd = new ArrayList(16);

        cmd.add(worksp);
        cmd.add("--create");

        if (startpaused) {
            cmd.add("--startpaused");
        }

        cmd.add("--name");
        cmd.add(xenName(vm));

        if (vm.getDeployment() != null) {

            final VirtualMachineDeployment dep = vm.getDeployment();

            // the service does not currently support a default memory
            // config, but workspace_control does -- in the future,
            // decision to allow a default memory config will be policy
            // driven at service level
            if (dep.getIndividualPhysicalMemory() > 0) {
                cmd.add("--memory");
                cmd.add(Integer.toString(dep.getIndividualPhysicalMemory()));
            }

            if (dep.getIndividualCPUCount() > 0) {
                cmd.add("--vcpus");
                cmd.add(Integer.toString(dep.getIndividualCPUCount()));
            }
        }

        if (vm.getKernel() != null) {
            cmd.add("--kernel");
            cmd.add(vm.getKernel());
        }

        if (vm.getKernelParameters() != null) {
            cmd.add("--kernelargs");
            cmd.add(vm.getKernelParameters());
        }

        if (vm.getNetwork() != null) {
            cmd.add("--networking");
            final String net = "'" + hackStatic(vm.getNetwork()) + "'";
            cmd.add(net);
        }

        // sources:
        VirtualMachinePartition rootdisk = null;
        final ArrayList regularPartitions = new ArrayList();
        final ArrayList blankPartitions = new ArrayList();

        // results to send:
        final ArrayList images = new ArrayList();
        final ArrayList imagemounts = new ArrayList();

        final VirtualMachinePartition[] partitions = vm.getPartitions();

        if (partitions == null || partitions.length == 0) {
            final String err = "should be at least one partition, Binding " +
                    "should have caught at the outset";
            logger.error(err);
            throw new WorkspaceException(err);
        } else {
            for (int i = 0; i < partitions.length; i++) {
                if (partitions[i].isRootdisk()) {
                    rootdisk = partitions[i];
                } else if (partitions[i].getBlankspace() > 0) {
                    blankPartitions.add(partitions[i]);
                } else {
                    regularPartitions.add(partitions[i]);
                }
            }
        }


        // todo: generalize when propagating more than just rootdisk
        if (rootdisk != null) {
            
            String rootImageURI = rootdisk.getImage();

            // We know that if Propagate was required and notificationInfo
            // is null that this is a create command following a successful
            // Propagate-only command, so we let the backend know it can
            // find the file in its workspace-specific secureimage directory
            // by setting this file URL to a relative path -- relative paths
            // hitting the backend always cause the workspace-specific
            // secureimage directory to be consulted first

            if (vm.isPropagateRequired() && notificationInfo == null) {
                final String newURI = convertToAlreadyPropagated(rootImageURI, vm);

                logger.debug("turned '" + rootImageURI + "' into '" +
                            newURI + "' because file was already propagated");

                // not handling readonly root partition yet
                images.add(newURI);
            } else {
                // not handling readonly root partition yet
                images.add(rootImageURI);
            }
            imagemounts.add(rootdisk.getImagemount());
        }

        if (!blankPartitions.isEmpty()) {
            int blankNum = 0;
            final Iterator iter = blankPartitions.iterator();

            while (iter.hasNext()) {
                final VirtualMachinePartition blank =
                                (VirtualMachinePartition) iter.next();
                final int megs = blank.getBlankspace();
                //When unpropagate support is added for blank partitions,
                // this file will likely be unpropagated to the image node 
                // with the originally supplied filename as target -- and
                // perhaps some schema change will allow the user to specify
                // whether or not it should be saved at all (when serializing
                // it would have to be)
                images.add("blankcreate://blankpartition" + blankNum
                                                            + "-size-" + megs);
                imagemounts.add(blank.getImagemount());
                blankNum += 1;

                // (assuming blank partition will always be readwrite)
            }
        }
        
        if (!regularPartitions.isEmpty()) {
            final Iterator iter = regularPartitions.iterator();
            while (iter.hasNext()) {
                final VirtualMachinePartition regular =
                                (VirtualMachinePartition) iter.next();
                String imgStr = regular.getImage();
                if (!regular.isReadwrite()) {
                    imgStr += WC_FIELD_SEPARATOR + "ro";
                }
                images.add(imgStr);
                imagemounts.add(regular.getImagemount());
            }
        }

        if (images.isEmpty()) {
            final String err = "should be at least one image here...";
            logger.error(err);
            throw new WorkspaceException(err);
        }
        Iterator iter = images.iterator();
        String imageString = "";

        imageString += (String) iter.next();

        while (iter.hasNext()) {
            imageString += WC_GROUP_SEPARATOR;
            imageString += (String) iter.next();
        }

        cmd.add("--images");
        cmd.add("'" + imageString + "'");


        if (imagemounts.isEmpty()) {
            final String err = "should be at least one image and hence at least " +
                    "one imagemount string here...";
            logger.error(err);
            throw new WorkspaceException(err);
        }
        iter = imagemounts.iterator();
        String imagemountString = "";

        imagemountString += (String) iter.next();

        while (iter.hasNext()) {
            imagemountString += WC_GROUP_SEPARATOR;
            imagemountString += (String) iter.next();
        }

        cmd.add("--imagemounts");
        cmd.add("'" + imagemountString + "'");

        if (notificationInfo != null) {
            cmd.add("--notify");                                           
            cmd.add(notificationInfo);
        }

        final CustomizationNeed[] needs = vm.getCustomizationNeeds();
        if (needs != null) {

            if (!vm.isCustomizationAllDone()) {
                
                boolean oneBeingSent = false;
                final StringBuffer tasks = new StringBuffer("'");
                for (int i = 0; i < needs.length; i++) {

                    if (!needs[i].isSent()) {

                        if (oneBeingSent) {
                            tasks.append(WC_GROUP_SEPARATOR);
                        }

                        oneBeingSent = true;

                        tasks.append(needs[i].sourcePath)
                             .append(WC_FIELD_SEPARATOR)
                             .append(needs[i].destPath);

                        logger.debug(Lager.id(vm.getID()) +
                                    ": customization task '" +
                                    needs[i].sourcePath + "' --> '" +
                                    needs[i].destPath + "'");
                    }
                }

                tasks.append("'");

                if (oneBeingSent) {
                    cmd.add("--mnttasks");
                    cmd.add(tasks.toString());
                }
            }
        }

        if (logger.isDebugEnabled()) {
            cmd.add("--loglevel");
            cmd.add("DEBUG");
        }
        return cmd;
    }

    public static ArrayList constructUnpauseCommand(VirtualMachine vw)
                                                 throws WorkspaceException {

        if (vw == null) {
            String err = "vw is null";
            logger.error(err);
            throw new IllegalArgumentException(err);
        }

        if (worksp == null) {
            logger.error(NO_WRKSP);
            throw new WorkspaceException(NO_WRKSP);
        }

        final ArrayList cmd = new ArrayList(8);

        cmd.add(worksp);
        cmd.add("--unpause");

        cmd.add("--name");
        cmd.add(xenName(vw));


        if (logger.isDebugEnabled()) {
            cmd.add("--loglevel");
            cmd.add("DEBUG");
        }

        return cmd;
    }


    public static ArrayList constructPauseCommand(VirtualMachine vw)
                                                 throws WorkspaceException {

        if (vw == null) {
            final String err = "vw is null";
            logger.error(err);
            throw new IllegalArgumentException(err);
        }

        // not possible, Home initializes
        if (worksp == null) {
            logger.error(NO_WRKSP);
            throw new WorkspaceException(NO_WRKSP);
        }

        final ArrayList cmd = new ArrayList(6);

        cmd.add(worksp);
        cmd.add("--pause");

        cmd.add("--name");
        cmd.add(xenName(vw));

        if (logger.isDebugEnabled()) {
            cmd.add("--loglevel");
            cmd.add("DEBUG");
        }

        return cmd;
    }

    public static ArrayList constructRebootCommand(VirtualMachine vw)
                                                 throws WorkspaceException {

        if (vw == null) {
            final String err = "vw is null";
            logger.error(err);
            throw new IllegalArgumentException(err);
        }

        // not possible, Home initializes
        if (worksp == null) {
            logger.error(NO_WRKSP);
            throw new WorkspaceException(NO_WRKSP);
        }

        final ArrayList cmd = new ArrayList(6);

        cmd.add(worksp);
        cmd.add("--reboot");

        cmd.add("--name");
        cmd.add(xenName(vw));

        if (logger.isDebugEnabled()) {
            cmd.add("--loglevel");
            cmd.add("DEBUG");
        }

        return cmd;
    }

    // TODO: in the future, make more things pluggable
    public static String xenName(VirtualMachine vw) {
        return XEN_NAME_PREFIX + vw.getID();
    }

    public static int xenNameToId(String name) {

        if (name == null) {
            return -1065;
        }

        final String cmp = name.trim();

        if (!cmp.startsWith(XEN_NAME_PREFIX)) {
            return -1066;
        }

        try {
            return Integer.parseInt(cmp.substring(XEN_NAME_PREFIX.length()));
        } catch (Throwable t) {
            return -1067;
        }
    }

    // sad
    public static String extractFirstIP(String net) {

        if (net == null) {
            return null;
        }

        logger.debug("received net = '" + net + "'");

        // bad code dup
        
        final String[] nicsStr = net.split(WC_GROUP_SEPARATOR);
        if (nicsStr.length == 0) {
            logger.debug("nicsStr.length == 0?");
            return null;
        }

        final String[] nicPropertiesStr = nicsStr[0].split(WC_FIELD_SEPARATOR);

        if (nicPropertiesStr.length < 6) {
            logger.fatal("nicPropertiesStr.length < 6?");
            return null;
        }

        return nicPropertiesStr[5];
    }


    public static WorkspaceException
                        translateReturnException(ReturnException e) {

        /* 1 is reserved for errors before reaching the workspace
           controller on the compute node.  Examples: if the srun/ssh
           program is not present, if it fails to talk to slurmctld,
           if slurmctld fails to talk to specified nodes, if slurmd
           cannot run worksp program, etc. */

        if (e.retval == 1) {
            return throwErr(ERR_1, e);
        }

        /* 2 is from workspace driver, wrong parameters or validation
           problem */

        if (e.retval == 2) {
            return throwErr(ERR_2, e);
        }

        /* 3 is from workspace driver, runtime problem.  Validation
           succeeded but something went wrong with execution of
           requested operation. */

        if (e.retval == 3) {
            return throwErr(ERR_3, e);
        }

        /* 9 is from workspace driver, problem with post-execution
           tasks, but too late to recover.  Client (us) must decide
           to invoke again to fix (for instance, if something post-create
           failed, it may be the policy to call destroy (this could be
           configurable)).  Just logging for now. */

        if (e.retval == 9) {
            // candidate for admin log/trigger of severe issues
            loge9Err(e);
            return null; // success still ...
        }

        /* anything else is uncategorized error */
        return throwErr(ERR_OTHER, e);
    }

    private static WorkspaceException
            throwErr(String baseErr, ReturnException e) {

        final String err;
        if (e.stderr != null) {
            err = baseErr + "\nSTDERR: " + e.stderr;
        } else {
            err = baseErr;
        }
        return new WorkspaceException(err, e.getCause());
    }

    private static void loge9Err(ReturnException e) {

        String err = ERR_9;
        if (e.stderr != null) {
            err += "\nSTDERR: " + e.stderr;
            if (e.stdout != null) {
                err += "\nSTDOUT: " + e.stdout;
            }
        } else if (e.stdout != null) {
            err += "\nSTDOUT: " + e.stdout;
        }
        logger.error(err);
    }

    // we'll be relieved of this nonsense when networking info
    // is handled better (which it now needs to be).  XenUtil should
    // take an object and convert it into necessary arguments.
    private static String hackStatic(String network) {
        String x = network.replaceAll(
                NIC.ACQUISITION_AllocateAndConfigure, "Static");
        x = x.replaceAll(NIC.ACQUISITION_AcceptAndConfigure, "Static");
        x = x.replaceAll(NIC.ACQUISITION_Advisory, "Independent");
        return x;
    }

    // todo: relieve need for loglevels
    public static void doFilePushLocalTarget(VirtualMachine vm,
                                             String localDirectory,
                                             String backendTargetDir,
                                             boolean fake,
                                             boolean eventLog,
                                             boolean traceLog) throws Exception {
        filePush(vm, localDirectory, backendTargetDir, fake, false, eventLog, traceLog);
    }

    // todo: relieve need for loglevels
    public static void doFilePushRemoteTarget(VirtualMachine vm,
                                              String localDirectory,
                                              String backendTargetDir,
                                              boolean fake,
                                              boolean eventLog,
                                              boolean traceLog) throws Exception {
        filePush(vm, localDirectory, backendTargetDir, fake, true, eventLog, traceLog);
    }

    private static void filePush(VirtualMachine vm,
                                 String localDirectory,
                                 String backendTargetDir,
                                 boolean fake,
                                 boolean remoteTarget,
                                 boolean eventLog,
                                 boolean traceLog) throws Exception {

        if (localDirectory == null) {
            throw new Exception("localDirectory needed but missing");
        }
        if (backendTargetDir == null) {
            throw new Exception("backendTargetDir needed but missing");
        }

        final CustomizationNeed[] needs = vm.getCustomizationNeeds();
        if (needs == null || needs.length == 0) {
            logger.warn("file push: nothing to do?");
            return;
        }

        final ArrayList cmd;
        if (remoteTarget) {
            cmd = SSHUtil.constructScpCommandPrefix();
        } else {
            cmd = new ArrayList(needs.length + 3);
            cmd.add("cp"); // hardcoded... TODO
            cmd.add("-p");
        }

        for (int i = 0; i < needs.length; i++) {
            // TODO: clean up
            final String path = localDirectory + "/" + needs[i].sourcePath;
            cmd.add(path);
        }

        if (remoteTarget) {
            cmd.addAll(SSHUtil.constructScpCommandSuffix(
                                            vm.getNode(), backendTargetDir));
        } else {
            cmd.add(backendTargetDir);
        }

        final String[] send = (String[]) cmd.toArray(new String[cmd.size()]);

        if (fake) {
            logger.debug("Would have run this for file push: " +
                    WorkspaceUtil.printCmd(send));
        } else {
            WorkspaceUtil.runCommand(send, eventLog, traceLog, vm.getID().intValue());
        }
    }

}
