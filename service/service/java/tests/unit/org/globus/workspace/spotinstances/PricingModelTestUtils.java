package org.globus.workspace.spotinstances;

import java.util.Collection;
import java.util.LinkedList;

import edu.emory.mathcs.backport.java.util.Collections;

public class PricingModelTestUtils {

    public static boolean checkPricingModelConstraints(Double nextPrice, Integer totalReservedResources, Collection<SIRequest> requests){
        
        if(!checkMinimumPriceCOnstant(nextPrice)){
            return false;
        }
        
        return checkSpotInstancesConstraint(nextPrice, totalReservedResources, requests);
    }

    private static boolean checkMinimumPriceCOnstant(Double nextPrice) {
        return nextPrice >= PricingModelConstants.MINIMUM_PRICE;
    }

    private static boolean checkSpotInstancesConstraint(Double nextPrice,
            Integer totalReservedResources, Collection<SIRequest> requests) {
        
        LinkedList<SIRequest> reverseOrderedRequests = new LinkedList<SIRequest>(requests);
        Collections.sort(reverseOrderedRequests, Collections.reverseOrder());

        Integer availableResources = totalReservedResources;
        
        for (SIRequest siRequest : reverseOrderedRequests) {
            if(availableResources > 0){
                if(siRequest.getMaxBid() >= nextPrice){
                    availableResources -= siRequest.getNeededInstances();
                } else {
                    //There are no more eligible bids (fine)
                    break;
                }
            } else {
                if(siRequest.getMaxBid() > nextPrice){
                    //There are no more resources and next bids are higher than spot price (spot instance basic constraint check failed)                  
                    return false;
                } else {
                    //There are no more resources and next bids are not higher than spot price (fine)
                    break;
                }
            }
        }
        
        return true;
    }
}
