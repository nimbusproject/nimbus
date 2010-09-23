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

import org.nimbustools.ctxbroker.generated.gt4_0.description.Cloudcluster_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.description.Clouddeploy_Type;

public class ClusterMember {

    static final String[] EMPTY = new String[0];

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private final String printName;
    private final String imageName;
    private final int quantity;
    private final ClusterMemberNic[] nics;
    private final boolean oneLoginFlagPresent; // at least one present
    private final Cloudcluster_Type clusterForUserData;
    private final Clouddeploy_Type[] deploys;
    
    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    public ClusterMember(String printName,
                         String imageName,
                         int quantity,
                         ClusterMemberNic[] nics,
                         Cloudcluster_Type clusterForUserData,
                         Clouddeploy_Type[] deploys) {

        this.printName = printName;
        
        if (imageName == null) {
            throw new IllegalArgumentException("imageName may not be null");
        }
        if (nics == null) {
            throw new IllegalArgumentException("nics may not be null");
        }

        this.imageName = imageName;
        this.quantity = quantity;
        this.nics = nics;
        this.clusterForUserData = clusterForUserData; // may be null
        this.deploys = deploys; // may be null

        boolean loginPresent = false;
        for (ClusterMemberNic nic : nics) {
            if (nic.loginDesired) {
                loginPresent = true;
                break;
            }
        }
        this.oneLoginFlagPresent = loginPresent;
    }

    // -------------------------------------------------------------------------
    // FIELDS
    // -------------------------------------------------------------------------

    public String getPrintName() {
        return this.printName;
    }

    public String getImageName() {
        return this.imageName;
    }

    public int getQuantity() {
        return this.quantity;
    }

    public ClusterMemberNic[] getNics() {
        return this.nics;
    }

    // can be null
    public Cloudcluster_Type getClusterForUserData() {
        return this.clusterForUserData;
    }

    public Clouddeploy_Type[] getDeploys() {
        return this.deploys;
    }

    public boolean isOneLoginFlagPresent() {
        return this.oneLoginFlagPresent;
    }

    // -------------------------------------------------------------------------
    // OTHER
    // -------------------------------------------------------------------------

    public String[] getAssociations() {
        return this.getStringArray(false);
    }

    public String[] getIfaceNames() {
        return this.getStringArray(true);
    }

    private String[] getStringArray(boolean iface) {
        if (this.nics == null || this.nics.length == 0) {
            return EMPTY;
        }

        final String[] ret = new String[this.nics.length];

        for (int i = 0; i < this.nics.length; i++) {
            final ClusterMemberNic nic = this.nics[i];
            if (nic == null) {
                ret[i] = null;
            } else {
                if (iface) {
                    ret[i] = nic.iface;
                } else {
                    ret[i] = nic.association;
                }
            }
        }
        return ret;
    }

    public boolean hasDeploy() {
        return deploys != null && deploys.length > 0;
    }
}
