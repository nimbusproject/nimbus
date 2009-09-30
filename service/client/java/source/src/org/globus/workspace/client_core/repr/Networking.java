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

package org.globus.workspace.client_core.repr;

import org.nimbustools.messaging.gt4_0.generated.metadata.logistics.VirtualNetwork_Type;
import org.nimbustools.messaging.gt4_0.generated.metadata.logistics.Nic_Type;

import java.util.ArrayList;

/**
 * Serves as a way to begin encapsulating protocol/implementations but
 * also importantly serves as a copy... to use in group request per-instance
 * tracking for example.
 */
public class Networking {

    private final Nic[] nics;

    public Networking(VirtualNetwork_Type networking) {
        
        if (networking == null) {
            throw new IllegalArgumentException("networking may not be null, " +
                    "use null " + this.getClass().getName() +
                    " reference instead");
        }

        final Nic_Type[] xmlNics = networking.getNic();
        if (xmlNics == null || xmlNics.length == 0) {
            throw new IllegalArgumentException("no nics, " +
                    "use null " + this.getClass().getName() +
                    " reference instead");
        }

        final ArrayList nicList = new ArrayList(xmlNics.length);
        for (int i = 0; i < xmlNics.length; i++) {
            final Nic nic = new Nic(xmlNics[i]);
            nicList.add(nic);
        }
        
        this.nics = (Nic[]) nicList.toArray(new Nic[nicList.size()]);
    }

    public int nicNum() {
        return this.nics.length;
    }

    public Nic[] nics() {
        return this.nics;
    }
    
}
