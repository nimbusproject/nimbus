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

package org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.rm.defaults;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.SpotRequestInfo;
import org.nimbustools.api.services.rm.Manager;
import org.nimbustools.api.services.rm.ManageException;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.CancelSpotInstanceRequestsResponseSetItemType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.CancelSpotInstanceRequestsResponseSetType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.CancelSpotInstanceRequestsResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.CancelSpotInstanceRequestsType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.SpotInstanceRequestIdSetItemType;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.rm.CancelSI;

import java.rmi.RemoteException;

public class DefaultCancelSI implements CancelSI {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(DefaultCancelSI.class.getName());    

    // -------------------------------------------------------------------------
    // BACKOUT
    // -------------------------------------------------------------------------

    public void backOutRequestSpotInstances(SpotRequestInfo result,
                                    Caller caller,
                                    Manager manager) {

        if (manager == null) {
            throw new IllegalArgumentException("manager may not be null");
        }
        
        try {
            manager.cancelBackfillRequests(new String[]{result.getRequestID()}, caller);
        } catch (Throwable t) {
            final String msg = "Problem backing out SI request: " + result.getRequestID() + ": ";
            if (logger.isDebugEnabled()) {
                logger.error(msg + t.getMessage(), t);
            } else {
                logger.error(msg + t.getMessage());
            }
        }
    }

    // -------------------------------------------------------------------------
    // CANCEL OPERATION
    // -------------------------------------------------------------------------
    
    public CancelSpotInstanceRequestsResponseType cancelSIRequests(CancelSpotInstanceRequestsType req,
                                                    Caller caller,
                                                    Manager manager)
            throws RemoteException {

        if (manager == null) {
            throw new IllegalArgumentException("manager may not be null");
        }
        if (caller == null) {
            throw new IllegalArgumentException("caller may not be null");
        }
        if (req == null) {
            throw new IllegalArgumentException("req may not be null");
        }

        SpotInstanceRequestIdSetItemType[] siris = req.getSpotInstanceRequestIdSet().getItem();
        String[] reqIds = new String[siris.length];
        
        for (int i = 0; i < siris.length; i++) {
            reqIds[i] = siris[i].getSpotInstanceRequestId().trim();
        }
        
        
        SpotRequestInfo[] spotRequests = null;
        try {
            spotRequests = manager.cancelSpotInstanceRequests(reqIds, caller);
        } catch (ManageException e) {
            if (logger.isDebugEnabled()) {
                logger.error(e.getMessage(), e);
            } else {
                logger.error(e.getMessage());
            }            e.printStackTrace();
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
            throw new RemoteException("Invalid request ID.");
        }
        
        CancelSpotInstanceRequestsResponseType csirst = new CancelSpotInstanceRequestsResponseType();
        
        if(spotRequests != null){
            CancelSpotInstanceRequestsResponseSetItemType[] csirrst = new CancelSpotInstanceRequestsResponseSetItemType[spotRequests.length];
            
            for (int i = 0; i < spotRequests.length; i++) {
                SpotRequestInfo info = spotRequests[i];
                
                CancelSpotInstanceRequestsResponseSetItemType item = new CancelSpotInstanceRequestsResponseSetItemType();
                item.setSpotInstanceRequestId(info.getRequestID());
                item.setState(info.getState().getStateStr());
                csirrst[i] = item;
            }
            
            CancelSpotInstanceRequestsResponseSetType sirst = new CancelSpotInstanceRequestsResponseSetType();
            sirst.setItem(csirrst);
            
            csirst.setSpotInstanceRequestSet(sirst);
        }
        
        return csirst;
    }

}
