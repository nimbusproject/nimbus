package org.globus.workspace.spotinstances;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

public class MaximizeProfitPricingModel extends AbstractPricingModel {

    
    @Override
    public Double getNextPriceImpl(Integer totalReservedResources, Collection<SIRequest> requests, Double currentPrice) {
                
        TreeSet<Double> priceCandidates = getPriceCandidates(requests);
        
        Double highestProfitPrice = NEGATIVE_INFINITY;
        Double highestProfit = NEGATIVE_INFINITY;
        
        for (Double priceCandidate : priceCandidates) {
            Double profit = getProfit(priceCandidate, totalReservedResources, requests);
            if(profit > highestProfit){
                highestProfit = profit;
                highestProfitPrice = priceCandidate;
            }
        }
        
        if(highestProfitPrice == NEGATIVE_INFINITY){
            Double highestPrice = priceCandidates.last();
            return highestPrice+1;
        }
        
        return highestProfitPrice;
    }


    private static Double getProfit(Double priceCandidate, Integer availableResources, Collection<SIRequest> allRequests){
        
        Collection<SIRequest> priorityOffers = getOffersAbovePrice(priceCandidate, allRequests);
        Collection<SIRequest> limitOffers = getOffersEqualPrice(priceCandidate, allRequests);
        
        Collection<SIRequest> eligibleOffers = union(priorityOffers, limitOffers);
        
        Double priorityUtilization = getUtilization(priorityOffers, availableResources);
        
        if(!priorityOffers.isEmpty() && priorityUtilization >= 1.0){
            return NEGATIVE_INFINITY;
        }
        
        return getProfitFromEligibleOffers(eligibleOffers, priceCandidate);
    }

    private static Double getProfitFromEligibleOffers(Collection<SIRequest> eligibleOffers, Double priceCandidate) {
        
        Double totalProfit = 0.0;
        
        for (SIRequest siRequest : eligibleOffers) {
            totalProfit += siRequest.getQuantity()*priceCandidate;
        }
        
        return totalProfit;
    }

    private static Collection<SIRequest> union(
            Collection<SIRequest> priorityOffers,
            Collection<SIRequest> limitOffers) {
        
        LinkedList<SIRequest> union = new LinkedList<SIRequest>(priorityOffers);
        union.addAll(limitOffers);
        
        return union;
    }

    private static Double getUtilization(Collection<SIRequest> bids, Integer offeredResources){
        
        Double demand = 0.0;
        
        for (SIRequest siRequest : bids) {
            demand += siRequest.getQuantity();
        }
        
        return demand/offeredResources;
    }
    
    
    
    private static Collection<SIRequest> getOffersEqualPrice(
            Double priceCandidate, Collection<SIRequest> allRequests) {
        
        List<SIRequest> offersEqualPrice = new ArrayList<SIRequest>();
        
        for (SIRequest siRequest : allRequests) {
            if(siRequest.getMaxBid() == priceCandidate){
                offersEqualPrice.add(siRequest);
            }
        }
        
        return offersEqualPrice;
    }

    private static Collection<SIRequest> getOffersAbovePrice(Double priceCandidate,
            Collection<SIRequest> allRequests) {

        List<SIRequest> offersAbovePrice = new ArrayList<SIRequest>();
        
        for (SIRequest siRequest : allRequests) {
            if(siRequest.getMaxBid() > priceCandidate){
                offersAbovePrice.add(siRequest);
            }
        }
        
        return offersAbovePrice;        
        
    }

}
