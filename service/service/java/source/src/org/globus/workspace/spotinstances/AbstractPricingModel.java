package org.globus.workspace.spotinstances;

import java.util.Collection;
import java.util.LinkedList;

import edu.emory.mathcs.backport.java.util.Collections;

public abstract class AbstractPricingModel implements PricingModel{
    
    public Double getNextPrice(Integer totalReservedResources, Collection<SIRequest> requests, Double currentPrice) {
        
        if(requests.isEmpty()){
            return PricingModelConstants.MINIMUM_PRICE;
        }
        
        LinkedList<Double> priceCandidates = getOrderedPriceCandidates(requests);
        
        if(totalReservedResources < 1 && !priceCandidates.isEmpty()){
            Double highestPrice = priceCandidates.getLast();
            return highestPrice+0.1;
        }
        
        return getNextPriceImpl(totalReservedResources, requests, currentPrice);
    }
    
    
    protected abstract Double getNextPriceImpl(Integer totalReservedResources,
            Collection<SIRequest> requests, Double currentPrice);


    protected LinkedList<Double> getOrderedPriceCandidates(Collection<SIRequest> requests) {
        
        LinkedList<Double> priceCandidates = new LinkedList<Double>();
        
        for (SIRequest siRequest : requests) {
            Double requestBid = siRequest.getMaxBid();
            if(requestBid >= PricingModelConstants.MINIMUM_PRICE){
                priceCandidates.add(requestBid);
            }
        }
        
        Collections.sort(priceCandidates);
        
        return priceCandidates;
    }
}
