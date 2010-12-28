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
import org.nimbustools.api.repr.SpotPriceEntry;
import org.nimbustools.api.services.rm.Manager;
import org.nimbustools.api.services.rm.ManageException;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.DescribeSpotPriceHistoryResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.DescribeSpotPriceHistoryType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.InstanceTypeSetItemType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.SpotPriceHistorySetItemType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.SpotPriceHistorySetType;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.general.ResourceAllocations;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.rm.DescribeSpotPriceHistory;

import java.rmi.RemoteException;

public class DefaultDescribeSpotPriceHistory implements DescribeSpotPriceHistory {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(DefaultDescribeSpotPriceHistory.class.getName());    

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------    
    
    protected final ResourceAllocations RAs;    
    
    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    public DefaultDescribeSpotPriceHistory(ResourceAllocations rasImpl) throws Exception {
        if (rasImpl == null) {
            throw new IllegalArgumentException("rasImpl may not be null");
        }
        this.RAs = rasImpl;
    }

    // -------------------------------------------------------------------------
    // DESCRIBE SPOT PRICE HISTORY OPERATION
    // -------------------------------------------------------------------------

    public DescribeSpotPriceHistoryResponseType describeSpotPriceHistory(
            DescribeSpotPriceHistoryType req, Manager manager)
            throws RemoteException {

        String supportedType = RAs.getSpotInstanceType();
        
        InstanceTypeSetItemType[] instanceType = req.getInstanceTypeSet().getItem();
        
        if(instanceType != null && instanceType.length > 0 && !supportedType.equals(instanceType[0].getInstanceType())){
            throw new RemoteException(
                    "Unsupported spot instance type: '" + instanceType[0] + "'." +
                            " Currently supported SI type: " + supportedType);             
        }
        
        SpotPriceEntry[] spotPriceHistory = null;
        
        try {
            spotPriceHistory = manager.getSpotPriceHistory(req.getStartTime(), req.getEndTime());
        } catch (ManageException e) {
            final String msg = "Problem retrieving spot price history : ";
            if (logger.isDebugEnabled()) {
                logger.error(msg + e.getMessage(), e);
            } else {
                logger.error(msg + e.getMessage());
            }
        }
        

        DescribeSpotPriceHistoryResponseType result = new DescribeSpotPriceHistoryResponseType();
        
        if(spotPriceHistory != null){
            SpotPriceHistorySetItemType[] items = new SpotPriceHistorySetItemType[spotPriceHistory.length];
            
            for (int i = 0; i < spotPriceHistory.length; i++) {
                SpotPriceEntry spotPriceEntry = spotPriceHistory[i];                
                
                SpotPriceHistorySetItemType item = new SpotPriceHistorySetItemType();
                item.setInstanceType(supportedType);
                item.setTimestamp(spotPriceEntry.getTimeStamp());
                item.setSpotPrice(spotPriceHistory[i].getSpotPrice().toString());
                
                items[i] = item;
            }

            SpotPriceHistorySetType spotPriceHistorySet = new SpotPriceHistorySetType();
            spotPriceHistorySet.setItem(items);

            result.setSpotPriceHistorySet(spotPriceHistorySet);
        }
        
        return result;
    }

}
