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

package org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.general.defaults;

import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.general.Networks;

public class DefaultNetworks implements Networks {

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected String publicNetwork;
    protected String privateNetwork;
    protected String noPublicNetwork;
    protected String noPrivateNetwork;


    // -------------------------------------------------------------------------
    // IoC INIT METHOD
    // -------------------------------------------------------------------------

    void validate() throws Exception {

        if (this.privateNetwork == null
                || this.privateNetwork.trim().length() == 0) {
            throw new Exception("Invalid: Missing manager network name " +
                    "to use for elastic 'private' addresses");
        }

        if (this.publicNetwork == null
                || this.publicNetwork.trim().length() == 0) {
            throw new Exception("Invalid: Missing manager network name " +
                    "to use for elastic 'public' addresses");
        }
    }


    // -------------------------------------------------------------------------
    // SET
    // -------------------------------------------------------------------------

    public void setPublicNetwork(String publicNetwork) {
        this.publicNetwork = publicNetwork;
    }

    public void setPrivateNetwork(String privateNetwork) {
        this.privateNetwork = privateNetwork;
    }

    public void setNoPublicNetwork(String noPublicNetwork) {
        this.noPublicNetwork = noPublicNetwork;
    }

    public void setNoPrivateNetwork(String noPrivateNetwork) {
        this.noPrivateNetwork = noPrivateNetwork;
    }
    

    // -------------------------------------------------------------------------
    // implements Networks
    // -------------------------------------------------------------------------

    public String getManagerPublicNetworkName() {
        return this.publicNetwork;
    }

    public String getManagerPrivateNetworkName() {
        return this.privateNetwork;
    }

    public boolean isPublicNetwork(String networkName) {
        return this.publicNetwork != null
                && this.publicNetwork.equals(networkName);
    }

    public boolean isPrivateNetwork(String networkName) {
        return this.privateNetwork != null
                && this.privateNetwork.equals(networkName);
    }

    public String getNoPublicNetwork() {
        if (this.noPublicNetwork == null) {
            return BACKUP_UNKNOWN;
        } else {
            return this.noPublicNetwork;
        }
    }

    public String getNoPrivateNetwork() {
        if (this.noPrivateNetwork == null) {
            return BACKUP_UNKNOWN;
        } else {
            return this.noPrivateNetwork;
        }
    }
}
