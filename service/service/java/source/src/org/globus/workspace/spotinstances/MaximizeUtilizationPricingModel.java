package org.globus.workspace.spotinstances;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

public class MaximizeUtilizationPricingModel extends AbstractPricingModel {

    private boolean setMinPrice;

    public MaximizeUtilizationPricingModel(){
        this(true);
    }    
    
    public MaximizeUtilizationPricingModel(boolean setMinPrice){
        this.setMinPrice = setMinPrice;
    }
    
    protected Double getNextPriceImpl(Integer totalReservedResources,
            Collection<AsyncRequest> requests, Double currentPrice) {
       
        LinkedList<AsyncRequest> reverseOrderedRequests = new LinkedList<AsyncRequest>(requests);
        Collections.sort(reverseOrderedRequests, Collections.reverseOrder());
        
        Double nextPrice = this.minPrice;
        Integer availableResources = totalReservedResources;
        
        for (AsyncRequest siRequest : reverseOrderedRequests) {
            if(siRequest.getMaxBid() >= this.minPrice){
                nextPrice = siRequest.getMaxBid();
                availableResources -= siRequest.getNeededInstances();
                if(availableResources <= 0){
                    break;
                }
            }
        }
        
        if(availableResources > 0 && this.setMinPrice){
            nextPrice = this.minPrice;
        }
        
        return nextPrice;
    }


}
