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

package org.nimbustools.messaging.gt4_0.factory;

import org.nimbustools.api.repr.ReprFactory;
import org.nimbustools.api._repr._CreateRequest;
import org.nimbustools.api.repr.CannotTranslateException;
import org.nimbustools.api._repr.vm._NIC;
import org.nimbustools.messaging.gt4_0.generated.metadata.logistics.VirtualNetwork_Type;
import org.nimbustools.messaging.gt4_0.generated.metadata.logistics.Nic_Type;
import org.nimbustools.messaging.gt4_0.generated.metadata.logistics.IPConfig_TypeAcquisitionMethod;
import org.nimbustools.messaging.gt4_0.generated.metadata.logistics.IPConfig_Type;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;

public class TranslateNetImpl implements TranslateNet {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    protected static final Log logger =
            LogFactory.getLog(TranslateNetImpl.class.getName());

    protected static final HashMap acqMethodMap = new HashMap(8);
    static {
        acqMethodMap.put(IPConfig_TypeAcquisitionMethod.AllocateAndConfigure,
                         _NIC.ACQUISITION_AllocateAndConfigure);
        acqMethodMap.put(IPConfig_TypeAcquisitionMethod.AcceptAndConfigure,
                         _NIC.ACQUISITION_AcceptAndConfigure);
        acqMethodMap.put(IPConfig_TypeAcquisitionMethod.Advisory,
                         _NIC.ACQUISITION_Advisory);
    }
    
    
    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected final ReprFactory repr;


    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    public TranslateNetImpl(ReprFactory reprFactory) {
        if (reprFactory == null) {
            throw new IllegalArgumentException("reprFactory may not be null");
        }
        this.repr = reprFactory;
    }


    // -------------------------------------------------------------------------
    // TRANSLATE
    // -------------------------------------------------------------------------

    public void translateNetworkingRelated(_CreateRequest req,
                                           VirtualNetwork_Type net)
         throws CannotTranslateException {

        if (req == null) {
            throw new IllegalArgumentException("req may not be null");
        }
        if (net == null) {
            throw new IllegalArgumentException("net may not be null");
        }

        final Nic_Type[] wsnics = net.getNic();
        if (wsnics == null || wsnics.length == 0) {
            logger.warn("no NIC requests");
            return; // *** EARLY RETURN ***
        }

        final _NIC[] nics = new _NIC[wsnics.length];

        for (int i = 0; i < wsnics.length; i++) {
            final Nic_Type wsnic = wsnics[i];
            if (wsnic == null) {
                throw new CannotTranslateException(
                        "WS nic in request list was missing?");
            }
            nics[i] = this.translateNIC(wsnic);
        }
        
        req.setRequestedNics(nics);
    }

    protected _NIC translateNIC(Nic_Type wsnic) throws CannotTranslateException {

        if (wsnic == null) {
            throw new IllegalArgumentException("wsnic may not be null");
        }
        
        final _NIC nic = this.repr._newNIC();
        
        nic.setNetworkName(wsnic.getAssociation());
        nic.setMAC(wsnic.getMAC());
        nic.setName(wsnic.getName());

        final IPConfig_Type ipconfig = wsnic.getIpConfig();
        if (ipconfig == null) {
            throw new CannotTranslateException(
                        "IPConfig_Type in nic request was missing?");
        }

        final String method =
                (String) acqMethodMap.get(ipconfig.getAcquisitionMethod());
        if (method == null) {
            throw new CannotTranslateException("unrecognized acquisition " +
                    "type (IPConfig_TypeAcquisitionMethod enumeration)");
        }

        nic.setAcquisitionMethod(method);

        nic.setBroadcast(ipconfig.getBroadcast());
        nic.setGateway(ipconfig.getGateway());
        nic.setHostname(ipconfig.getHostname());
        nic.setIpAddress(ipconfig.getIpAddress());
        nic.setNetmask(ipconfig.getNetmask());
        nic.setNetwork(ipconfig.getNetwork());

        return nic;
    }
}
