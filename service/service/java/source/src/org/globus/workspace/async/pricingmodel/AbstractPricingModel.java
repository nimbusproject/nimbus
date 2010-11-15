package org.globus.workspace.async.pricingmodel;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import org.globus.workspace.async.AsyncRequest;

public abstract class AbstractPricingModel implements PricingModel{
    
    public static final Double DEFAULT_MIN_PRICE = 0.1;
    protected Double minPrice;
    
    public AbstractPricingModel(){
        this.minPrice = DEFAULT_MIN_PRICE;
    }
    
    public void setMinPrice(Double minPrice){
        this.minPrice = minPrice;
    }
    
    public Double getMinPrice(){
        return this.minPrice;
    }
    
    public Double getNextPrice(Integer totalReservedResources, Collection<AsyncRequest> requests, Double currentPrice) {
        
        if(requests.isEmpty()){
            return minPrice;
        }
        
        LinkedList<Double> priceCandidates = getOrderedPriceCandidates(requests);
        
        if(totalReservedResources < 1 && !priceCandidates.isEmpty()){
            Double highestPrice = priceCandidates.getLast();
            return highestPrice+0.1;
        }
        
        return getNextPriceImpl(totalReservedResources, requests, currentPrice);
    }
    
    
    protected abstract Double getNextPriceImpl(Integer totalReservedResources,
            Collection<AsyncRequest> requests, Double currentPrice);


    protected LinkedList<Double> getOrderedPriceCandidates(Collection<AsyncRequest> requests) {
        
        LinkedList<Double> priceCandidates = new LinkedList<Double>();
        
        for (AsyncRequest siRequest : requests) {
            Double requestBid = siRequest.getMaxBid();
            if(requestBid >= minPrice){
                priceCandidates.add(requestBid);
            }
        }
        
        Collections.sort(priceCandidates);
        
        return priceCandidates;
    }
}
