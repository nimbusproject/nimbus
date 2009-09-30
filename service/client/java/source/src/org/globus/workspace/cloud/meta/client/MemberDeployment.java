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

import org.globus.workspace.cloud.client.cluster.ClusterMember;
import org.nimbustools.ctxbroker.generated.gt4_0.description.Clouddeploy_Type;

/**
 * Maps a single ClusterMember (workspace) to a single deployment
 */
class MemberDeployment {

    public ClusterMember getMember() {
        return member;
    }

    public Clouddeploy_Type getDeploy() {
        return deploy;
    }

    private final ClusterMember member;
    private final Clouddeploy_Type deploy;

    public MemberDeployment(ClusterMember member, Clouddeploy_Type deploy) {

        if (member == null) {
            throw new IllegalArgumentException("member may not be null");
        }
        if (deploy == null) {
            throw new IllegalArgumentException("deploy may not be null");
        }

        this.member = member;
        this.deploy = deploy;
    }

    public String getImageName() {
        if (this.deploy.getImage() != null) {
            return this.deploy.getImage();
        } else {
            return this.member.getImageName();
        }
    }

    public int getInstanceCount() {
        return deploy.getQuantity();
    }
}
