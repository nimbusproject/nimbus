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

package org.globus.workspace.cloud.client.cluster;

import org.globus.workspace.client_core.ParameterProblem;
import org.globus.workspace.cloud.client.util.CloudClientUtil;
import org.globus.workspace.cloud.client.util.HistoryUtil;
import org.globus.workspace.common.print.Print;
import org.globus.wsrf.encoding.DeserializationException;
import org.globus.wsrf.encoding.ObjectDeserializer;
import org.nimbustools.ctxbroker.generated.gt4_0.description.*;
import org.xml.sax.InputSource;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClusterUtil {

    // -------------------------------------------------------------------------
    // File intake
    // -------------------------------------------------------------------------

    /**
     * @param path path to file
     * @return deserialized
     * @throws DeserializationException problem with parsing
     * @throws IOException problem with file etc.
     */
    public static Cloudcluster_Type parseClusterDocument(String path)
            throws DeserializationException, IOException {

        Cloudcluster_Type cluster = null;
        BufferedInputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(path));
            cluster = (Cloudcluster_Type)
                            ObjectDeserializer.deserialize(
                                new InputSource(in), Cloudcluster_Type.class);
        } finally {
            if (in != null) {
                in.close();
            }
        }
        return cluster;
    }

    // -------------------------------------------------------------------------
    // Deployment file intake
    // -------------------------------------------------------------------------

    /**
     * @param path path to file
     * @return deserialized
     * @throws DeserializationException problem with parsing
     * @throws IOException problem with file etc.
     */
    public static Clouddeployment_Type parseDeployDocument(String path)
            throws DeserializationException, IOException {

        Clouddeployment_Type deployment = null;
        BufferedInputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(path));
            deployment = (Clouddeployment_Type)
                            ObjectDeserializer.deserialize(
                                new InputSource(in), Clouddeployment_Type.class);
        } finally {
            if (in != null) {
                in.close();
            }
        }
        return deployment;
    }

    /**
     * Builds a map of workspace names -> deployment lists from a
     * deployment document
     * @param deployment
     * @return
     * @throws ParameterProblem
     */
    public static Map<String,Clouddeploy_Type[]> parseDeployment(
        Clouddeployment_Type deployment) throws ParameterProblem {

        if (deployment == null) {
            throw new IllegalArgumentException("deployment cannot be null");
        }

        Clouddeployworkspace_Type[] workspaces = deployment.getWorkspace();


        final HashMap<String,Clouddeploy_Type[]> map =
            new HashMap<String, Clouddeploy_Type[]>(workspaces.length);

        for (Clouddeployworkspace_Type ws : workspaces) {

            String wsName = ws.getName();
            if (wsName == null || wsName.trim().length() == 0) {
                throw new ParameterProblem(
                    "Workspace name cannot be empty or null");
            }

            Clouddeploy_Type[] deploys = ws.getDeploy();
            if (deploys == null || deploys.length == 0) {
                throw new ParameterProblem("Each provided deployment "+
                    "workspace must have at least one deploy element");
            }


           if (map.put(wsName, deploys) != null) {
               throw new ParameterProblem("Each deployment workspace entry"+
                   "must have a unique name");
           }

        }

        return map;
    }

    // -------------------------------------------------------------------------
    // XML intake
    // -------------------------------------------------------------------------

    /**
     * @param cluster deserialized object, may not be null
     * @param memberIndex which member
     * @param print pr
     * @param brokerLocalNicPrefix search string for rigging broker nic names
     * @param brokerPublicNicPrefix search string for rigging broker nic names
     * @return valid ClusterMember object, never null
     * @throws ParameterProblem problem
     */
    public static ClusterMember parseRequest(Cloudcluster_Type cluster,
                                             int memberIndex,
                                             Print print,
                                             String brokerLocalNicPrefix,
                                             String brokerPublicNicPrefix)
            throws ParameterProblem {

        if (cluster == null) {
            throw new IllegalArgumentException("cluster may not be null");
        }

        final Cloudworkspace_Type[] members =
                shallowCloneCluster(cluster.getWorkspace(), print);

        final Cloudworkspace_Type member = members[memberIndex];

        String name = member.getName();
        if (name != null && name.trim().length() == 0) {
            name = null;
        }

        final String image = member.getImage();
        if (image == null || image.trim().length() == 0) {
            throw new ParameterProblem("Cluster member has no image");
        }

        final short quantity = member.getQuantity();
        if (quantity < 1) {
            throw new ParameterProblem("Illegal, requested less than 1 " +
                    "instance of cluster member '" + name + "'");
        }

        final Cloudnic_Type[] xmlnics = member.getNic();
        if (xmlnics == null || xmlnics.length == 0) {
            throw new ParameterProblem("Cluster member '" + name +
                                            "' has no nic definition.");
        }

        final ClusterMemberNic[] nics = new ClusterMemberNic[xmlnics.length];

        final boolean nicNamesRequired = nics.length > 1;

        for (int i = 0; i < nics.length; i++) {
            
            if (xmlnics[i] == null) {
                throw new ParameterProblem("Cluster member '" + name +
                        "' has null nic element"); // ??
            }

            final String assoc = xmlnics[i].get_value();
            if (assoc == null || assoc.trim().length() == 0) {
                throw new ParameterProblem("Cluster member '" + name +
                        "' has \"<nic>\" element with no network name.");
            }

            String iface = xmlnics[i].get_interface();
            
            if (iface == null || iface.trim().length() == 0) {

                if (nicNamesRequired) {

                    throw new ParameterProblem("Cluster member '" + name +
                        "' has multiple NICs and a \"<nic>\" element with " +
                        "empty or missing interface name.  With multiple " +
                        "NICs you need to explicitly name each one (either" +
                        "'publicnic' or 'localnic'), see " +
                        "samples.");

                } else {

                    final String assocComp = assoc.toLowerCase().trim();
                    final String pubComp;
                    if (brokerPublicNicPrefix != null) {
                        pubComp = brokerPublicNicPrefix.toLowerCase().trim();
                    } else {
                        pubComp = "";
                    }
                    final String privComp;
                    if (brokerLocalNicPrefix != null) {
                        privComp = brokerLocalNicPrefix.toLowerCase().trim();
                    } else {
                        privComp = "";
                    }

                    final String nicName;
                    if (assocComp.startsWith(pubComp)) {
                        nicName = "publicnic";
                    } else if (assocComp.startsWith(privComp)) {
                        nicName = "localnic";
                    } else {
                        throw new ParameterProblem("Cannot pick a broker " +
                                "NIC name for the network '" + assocComp + "'");
                    }

                    iface = doctorContext(nicName,
                                          member.getCtx(),
                                          print,
                                          "Cluster member '" + name + "'");
                }
            }

            if (member.getCtx() != null) {
                if (!iface.equalsIgnoreCase("publicnic")
                        && !iface.equalsIgnoreCase("localnic")) {
                    print.errln("\n*** Warning: you are using broker NIC " +
                            "names in the cluster contextualization " +
                            "document that will probably not be resolvable " +
                            "by the ctx-agent.\nThe ctx-agent expects only " +
                            "'publicnic' or 'localnic' if there is more than " +
                            "one NIC.\nYou have however provided an " +
                            "interface name '" + iface + "'");
                }
            }

            boolean login = false;
            final Boolean wantLogin = xmlnics[i].getWantlogin();
            if (wantLogin != null && wantLogin) {
                login = true;
            }

            nics[i] = new ClusterMemberNic(iface, assoc, login);
        }


        final Cloudcluster_Type newcluster;
        if (member.getCtx() != null) {
            // active flag is bolted in for "new style" contextualization:

            // The whole point of the shallow copy. This sets a unique active
            // flag for this cluster member: the user data the VM's context
            // agent gets will have a unique active flag in the ctx document
            members[memberIndex].setActive(Boolean.TRUE);

            newcluster = new Cloudcluster_Type();
            newcluster.setWorkspace(members);
        } else {
            newcluster = null;
        }
        
        Clouddeploy_Type[] deploy = member.getDeploy();

        return new ClusterMember(name, image, quantity, nics, newcluster, deploy);
    }

    // shallow: enough to make each ACTIVE flag unique, other stuff is OK to be
    // duplicate references
    private static Cloudworkspace_Type[] shallowCloneCluster(
            Cloudworkspace_Type[] workspace, Print print) {

        if (workspace == null) {
            throw new IllegalArgumentException(
                    "workspace array may not be null");
        }

        Cloudworkspace_Type[] newarr =
                new Cloudworkspace_Type[workspace.length];

        for (int i = 0; i < workspace.length; i++) {
            newarr[i] = shallowCloneOneClusterWorkspace(workspace[i],
                                                        print);
        }
        
        return newarr;
    }

    // shallow: enough to make each member's ACTIVE flag unique, other stuff
    // is OK to be duplicate references
    private static Cloudworkspace_Type shallowCloneOneClusterWorkspace(
            Cloudworkspace_Type one, Print print) {

        if (one == null) {
            throw new IllegalArgumentException("'one' may not be null");
        }

        Cloudworkspace_Type newone = new Cloudworkspace_Type();
        newone.setImage(one.getImage());
        newone.setName(one.getName());
        newone.setNic(one.getNic());
        newone.setQuantity(one.getQuantity());


        // contextualization: THIS CAN BE NULL
        newone.setCtx(one.getCtx());

        // unlikely, person that did this would be some kind of developer etc.
        if (Boolean.TRUE.equals(one.getActive())) {
            print.errln("Warning: you have set an active=true " +
                    "flag in the cluster definition provided to the cloud " +
                    "client?  Why did you do that? " +
                    "It's going to be set to false now.");
        }

        // ALL start with False
        newone.setActive(Boolean.FALSE);

        return newone;
    }

    private static String doctorContext(String iface,
                                        Contextualization_Type ctx,
                                        Print print,
                                        String memberName)
            throws ParameterProblem {

        /*
            Situations:

            A. No ctx section: do nothing
            B. No provided identities in ctx: make it the given iface
            C. More than one provided identity in ctx: error, needs explicit nics
            D. The one provided identity in ctx has a name: return matching name
         */

        if (ctx == null) {
            return iface; // *** EARLY RETURN ***
        }

        final Print pr;
        if (print == null) {
            pr = new Print();
        } else {
            pr = print;
        }

        final Provides_Type provides = ctx.getProvides();

        if (provides == null) {
            throw new ParameterProblem("no provides element?");
        }

        final IdentityProvides_Type[] idents = provides.getIdentity();
        if (idents == null || idents.length == 0) {
            final IdentityProvides_Type ident = new IdentityProvides_Type();
            ident.set_interface(iface);
            final IdentityProvides_Type[] newidents = {ident};
            provides.setIdentity(newidents);

            pr.debugln(memberName + ": found no provides identities, created " +
                    "new, empty one with iface '" + iface + "'");

            return iface; // *** EARLY RETURN ***
        }

        if (idents.length > 1) {
            throw new ParameterProblem(memberName + ": found multiple " +
                    "identities in context 'provides' section, but " +
                    "only one NIC is defined in the cluster definition");
        }

        final String ctxIface = idents[0].get_interface();

        if (ctxIface != null) {
            pr.debugln(memberName + ": found interface name " +
                       "in the identity in context 'provides' section, but " +
                        "there is an explicit name for it -- ClusterMemberNic " +
                        "will now match this ('" + ctxIface + "')");
            return ctxIface;
        } else {
            idents[0].set_interface(iface);
            doctorContextProvides(ctx, iface, pr);
            return iface;
        }
    }

    private static void doctorContextProvides(Contextualization_Type ctx,
                                              String iface,
                                              Print pr) {

        final Provides_Type provides = ctx.getProvides();
        final Provides_TypeRole[] roles = provides.getRole();
        if (roles == null || roles.length == 0) {
            return;
        }
        for (final Provides_TypeRole role : roles) {
            pr.debugln("Role '" + role.get_value() + "' doctored. " +
                    "Interface was '" + role.get_interface() +
                    "' and is now '" + iface + "'");
            role.set_interface(iface);
        }
    }

    /**
     * Resolves absolute path to SSH hosts file; ensures it exists with proper permissions
     * @param ssh_hostsfile path to hosts file
     * @param print
     * @return possibly modified path
     * @throws ParameterProblem
     */
    public static String expandSshHostsFile(String ssh_hostsfile, Print print)
        throws ParameterProblem {

        if (ssh_hostsfile == null) {
            throw new IllegalArgumentException("ssh_hostsfile cannot be null");
        }

        if (ssh_hostsfile.startsWith("~")) {

            final String homedir = System.getProperty("user.home");

            if (homedir == null || homedir.trim().length() == 0) {
                throw new ParameterProblem("Need to replace tilde in " +
                        "SSH known hosts file, but cannot determine " +
                        "user home directory.  Please hardcode, see " +
                        "properties file.");
            }

            print.debugln("\n(tilde expansion necessary)");
            print.debugln("$user.home = " + homedir);

            final String result =
                    ssh_hostsfile.replaceFirst("~", homedir);

            print.infoln("SSH known_hosts contained tilde:");
            print.infoln("  - '" + ssh_hostsfile + "' --> '" +
                                            result + "'");

            ssh_hostsfile = result;
        }

        final File f2 = new File(ssh_hostsfile);
        ssh_hostsfile = f2.getAbsolutePath();

        print.debugln("Examining '" + ssh_hostsfile + "'");

        if (!CloudClientUtil.
                fileExistsAndReadwritable(ssh_hostsfile)) {

            throw new ParameterProblem("SSH known_hosts file does " +
                    "not exist or is not read+writable: '" +
                    ssh_hostsfile + "'");
        }

        print.debugln("Exists, readable, and writable: '" +
                                        ssh_hostsfile + "'");
        return ssh_hostsfile;
    }

    /**
     * Parses and validates cluster members in the provided cluster definition file
     * @param clusterPath path to cluster definition
     * @param brokerLocalNicPrefix search string for rigging broker nic names
     * @param brokerPublicNicPrefix search string for rigging broker nic names
     * @param print
     * @return array of cluster members
     * @throws ParameterProblem
     */
    public static ClusterMember[] getClusterMembers(String clusterPath,
                                                    String brokerLocalNicPrefix,
                                                    String brokerPublicNicPrefix,
                                                    Print print)
        throws ParameterProblem {

        final Cloudcluster_Type cluster = getCluster(clusterPath, print);

        return getClusterMembers(cluster, brokerLocalNicPrefix, brokerPublicNicPrefix, print);
    }

    public static Cloudcluster_Type getCluster(String clusterPath, Print print)
        throws ParameterProblem {

        if (clusterPath == null) {
            throw new IllegalArgumentException("clusterPath may not be null");
        }

        final File f = new File(clusterPath);
        clusterPath = f.getAbsolutePath();

        print.debugln("Examining cluster definition @ '" +
                                        clusterPath + "'");

        if (!CloudClientUtil.fileExistsAndReadable(clusterPath)) {
            throw new ParameterProblem("Given cluster description file " +
                    "does not exist or is not readable: '" +
                    clusterPath + "'");
        }

        print.debugln("Exists and readable: '" + clusterPath + "'");

        print.infoln();

        final Cloudcluster_Type cluster;
        try {
            cluster = parseClusterDocument(clusterPath);
            if (cluster == null) {
                throw new DeserializationException("No parsing result?");
            }
        } catch (DeserializationException e) {
            final String msg = "Could not parse the contents of the cluster " +
                    "definition file you provided.\n - Path: '" +
                    clusterPath + "'\n - Is it legal XML?  Try " +
                    "diffing your file with one of the example files or see " +
                    "the online instructions.\n - Error: " + e.getMessage();
            throw new ParameterProblem(msg, e);
        } catch (IOException e) {
            final String msg = "Problem with the cluster definition file: " +
                    e.getMessage();
            throw new ParameterProblem(msg, e);
        }
        return cluster;
    }

    public static ClusterMember[] getClusterMembers(Cloudcluster_Type cluster,
                                                    String brokerLocalNicPrefix,
                                                    String brokerPublicNicPrefix,
                                                    Print print)
        throws ParameterProblem {

        final Cloudworkspace_Type[] members = cluster.getWorkspace();
        if (members == null || members.length == 0) {
            throw new ParameterProblem("No members of the cluster described " +
                    "in document");
        }

        print.debugln("Found " + members.length + " cluster members");

        ClusterMember[] clusterMembers = new ClusterMember[members.length];

        for (int i = 0; i < members.length; i++) {

            if (members[i] == null) {
                throw new ParameterProblem(
                        "Cluster member (xml) unexpectedly null");
            }

            clusterMembers[i] =
                parseRequest(cluster, i, print,
                    brokerLocalNicPrefix,
                    brokerPublicNicPrefix);

        }

        // duplicate names would hose EPR writing
        for (int i = 0; i < members.length; i++) {
            final String thisName = members[i].getName();
            if (thisName != null) {
                for (int j = 0; j < members.length; j++) {
                    if (j == i) {
                        continue;
                    }
                    if (thisName.equals(members[j].getName())) {
                        throw new ParameterProblem("Found a duplicate " +
                                "nickname in the cluster definition ('" +
                                thisName + "').  Each <workspace> section " +
                                "needs a unique name.  You can leave out " +
                                "the <name> tag of one or more to have it " +
                                "autogenerated if you like.");
                    }
                }
            }
        }
        return clusterMembers;
    }

    public static KnownHostsTask[] constructKnownHostTasks(
        ClusterMember[] clusterMembers,
        boolean perHostDir) {

        final List knownHostsList = new ArrayList(clusterMembers.length);
        for (int i = 0; i < clusterMembers.length; i++) {

            final int memberIndex = i;
            final ClusterMember member = clusterMembers[memberIndex];

            if (member.isOneLoginFlagPresent()) {

                final ClusterMemberNic[] nics = member.getNics();
                for (int j = 0; j < nics.length; j++) {
                    if (nics[j].loginDesired) {
                        knownHostsList.add(
                                new KnownHostsTask(memberIndex,
                                                   null,
                                                   nics[j].iface,
                                                   member.getPrintName(),
                                                   perHostDir,
                                                   null));
                    }
                }
            }

        }


        final KnownHostsTask[] knownHostTasks;
        if (knownHostsList.isEmpty()) {
            knownHostTasks = null;
        } else {
            knownHostTasks =
                    (KnownHostsTask[]) knownHostsList.toArray(
                            new KnownHostsTask[knownHostsList.size()]);
        }
        return knownHostTasks;
    }

    public static void printClusterInfo(ClusterMember[] clusterMembers, Print print) {
        print.infoln("\nRequesting cluster.");
        for (int i = 0; i < clusterMembers.length; i++) {
            final ClusterMember member = clusterMembers[i];
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

            print.infoln("  - " + mname + ": image '" +
                    member.getImageName() + "', " + member.getQuantity() +
                    inststr);
        }
    }
}
