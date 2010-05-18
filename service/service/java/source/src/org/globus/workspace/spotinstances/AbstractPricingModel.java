package org.globus.workspace.spotinstances;

import java.util.Collection;
import java.util.TreeSet;

public abstract class AbstractPricingModel implements PricingModel{

    protected static final double BASE_PRICE = 1.0;
    
    protected static final double DISCOUNT_PERCENTAGE = 0.5;
    
    public static final Double MINIMUM_PRICE = DISCOUNT_PERCENTAGE*BASE_PRICE;

    protected static final Double NEGATIVE_INFINITY = -1.0;
    
    
    @Override
    public Double getNextPrice(Integer totalReservedResources, Collection<SIRequest> requests, Double currentPrice) {
        
        if(requests.isEmpty()){
            return MINIMUM_PRICE;
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
            if(requestBid >= MINIMUM_PRICE){
                priceCandidates.add(requestBid);
            }
        }
        
        return priceCandidates;
    }
}
