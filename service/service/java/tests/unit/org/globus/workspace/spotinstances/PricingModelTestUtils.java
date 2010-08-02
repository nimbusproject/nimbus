package org.globus.workspace.spotinstances;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

public class PricingModelTestUtils {

    public static boolean checkPricingModelConstraints(Double minPrice, Double nextPrice, Integer totalReservedResources, Collection<AsyncRequest> requests){
        
        if(nextPrice < minPrice){
            return false;
        }
        
        return checkSpotInstancesConstraint(nextPrice, totalReservedResources, requests);
    }

    private static boolean checkSpotInstancesConstraint(Double nextPrice,
            Integer totalReservedResources, Collection<AsyncRequest> requests) {
        
        LinkedList<AsyncRequest> reverseOrderedRequests = new LinkedList<AsyncRequest>(requests);
        Collections.sort(reverseOrderedRequests, Collections.reverseOrder());

        Integer availableResources = totalReservedResources;
        
        for (AsyncRequest siRequest : reverseOrderedRequests) {
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
