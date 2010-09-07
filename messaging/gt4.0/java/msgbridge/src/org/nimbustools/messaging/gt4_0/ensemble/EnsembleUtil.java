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

package org.nimbustools.messaging.gt4_0.ensemble;

import org.globus.wsrf.ResourceKey;
import org.globus.wsrf.impl.SimpleResourceKey;
import org.nimbustools.messaging.gt4_0.common.Constants_GT4_0;
import org.nimbustools.messaging.gt4_0.FaultUtil;
import org.nimbustools.messaging.gt4_0.generated.ensemble.WorkspaceEnsembleFault;
import org.apache.axis.message.addressing.EndpointReferenceType;
import org.apache.axis.message.addressing.ReferencePropertiesType;
import org.apache.axis.message.MessageElement;

import javax.xml.namespace.QName;

public class EnsembleUtil {

    // -------------------------------------------------------------------------
    // EPR RELATED
    // -------------------------------------------------------------------------

    public static String getResourceID(final EndpointReferenceType epr)
            throws Exception {

        if (epr == null) {
            throw new Exception("epr is null");
        }

        final ReferencePropertiesType properties = epr.getProperties();

        if (properties == null) {
            throw new Exception("epr properties are null");
        }

        final QName ensembleKeyQName =
                Constants_GT4_0.ENSEMBLE_RESOURCE_KEY_QNAME;

        final MessageElement elem = properties.get(ensembleKeyQName);
        if (elem == null) {
            throw new Exception("key not present in EPR");
        }

        final String id = elem.getValue();
        if (id == null || id.trim().length() == 0) {
            throw new Exception("key present in EPR but had no content");
        }

        return id;
    }

    public static ResourceKey getResourceKey(final EndpointReferenceType epr)
            throws Exception {
        final QName keyQName = Constants_GT4_0.ENSEMBLE_RESOURCE_KEY_QNAME;
        return new SimpleResourceKey(keyQName, getResourceID(epr));
    }


    // -------------------------------------------------------------------------
    // FAULTS
    // -------------------------------------------------------------------------

    public static WorkspaceEnsembleFault makeEnsembleFault(String err,
                                                           Exception e) {

        return (WorkspaceEnsembleFault)
                FaultUtil.makeFault(WorkspaceEnsembleFault.class, err, e);
    }
}
