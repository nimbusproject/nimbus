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
import org.globus.workspace.common.print.Print;
import org.globus.wsrf.encoding.ObjectDeserializer;
import org.globus.wsrf.encoding.DeserializationException;
import org.xml.sax.InputSource;
import org.nimbustools.ctxbroker.generated.gt4_0.description.Cloudcluster_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.description.Cloudworkspace_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.description.Contextualization_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.description.Cloudnic_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.description.Provides_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.description.IdentityProvides_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.description.Provides_TypeRole;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;

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
        
        return new ClusterMember(name, image, quantity, nics, newcluster);
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
}
