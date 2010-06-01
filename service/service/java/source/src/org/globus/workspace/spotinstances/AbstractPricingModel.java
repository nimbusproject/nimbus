package org.globus.workspace.spotinstances;

import java.util.Collection;
import java.util.TreeSet;

public abstract class AbstractPricingModel implements PricingModel{
    
    @Override
    public Double getNextPrice(Integer totalReservedResources, Collection<SIRequest> requests, Double currentPrice) {
        
        if(requests.isEmpty()){
            return PricingModelConstants.MINIMUM_PRICE;
        }
        
        TreeSet<Double> priceCandidates = getPriceCandidates(requests);
        
        if(totalReservedResources < 1){
            Double highestPrice = priceCandidates.last();
            return highestPrice+1;
        }
        
        return getNextPriceImpl(totalReservedResources, requests, currentPrice);
    }
    
    
    protected abstract Double getNextPriceImpl(Integer totalReservedResources,
            Collection<SIRequest> requests, Double currentPrice);


    protected TreeSet<Double> getPriceCandidates(Collection<SIRequest> requests) {
        
        TreeSet<Double> priceCandidates = new TreeSet<Double>();
        
        for (SIRequest siRequest : requests) {
            Double requestBid = siRequest.getMaxBid();
            if(requestBid >= PricingModelConstants.MINIMUM_PRICE){
                priceCandidates.add(requestBid);
            }
        }
        
        return priceCandidates;
    }
}
