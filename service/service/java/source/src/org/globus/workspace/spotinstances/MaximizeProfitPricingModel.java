package org.globus.workspace.spotinstances;

import java.util.Collection;
import java.util.LinkedList;

public class MaximizeProfitPricingModel extends AbstractPricingModel {

    @Override
    public Double getNextPriceImpl(Integer totalReservedResources, Collection<AsyncRequest> requests, Double currentPrice) {
                
        LinkedList<Double> priceCandidates = getOrderedPriceCandidates(requests);
        
        Double highestProfitPrice = PricingModelConstants.NEGATIVE_INFINITY;
        Double highestProfit = PricingModelConstants.NEGATIVE_INFINITY;
        
        for (Double priceCandidate : priceCandidates) {
            Double profit = getProfit(priceCandidate, totalReservedResources, requests);
            if(profit > highestProfit){
                highestProfit = profit;
                highestProfitPrice = priceCandidate;
            }
        }
        
        if(highestProfitPrice == PricingModelConstants.NEGATIVE_INFINITY){
            Double highestPrice = priceCandidates.getLast();
            return highestPrice+1;
        }
        
        return highestProfitPrice;
    }


    private static Double getProfit(Double priceCandidate, Integer availableResources, Collection<AsyncRequest> allRequests){
        
        Collection<AsyncRequest> priorityOffers = SIRequestUtils.getRequestsAbovePrice(priceCandidate, allRequests);
        Collection<AsyncRequest> limitOffers = SIRequestUtils.getRequestsEqualPrice(priceCandidate, allRequests);
        
        Collection<AsyncRequest> eligibleOffers = union(priorityOffers, limitOffers);
        
        Double priorityUtilization = getUtilization(priorityOffers, availableResources);
        
        if(!priorityOffers.isEmpty() && priorityUtilization >= 1.0){
            return PricingModelConstants.NEGATIVE_INFINITY;
        }
        
        return getProfitFromEligibleOffers(eligibleOffers, priceCandidate);
    }

    private static Double getProfitFromEligibleOffers(Collection<AsyncRequest> eligibleOffers, Double priceCandidate) {
        
        Double totalProfit = 0.0;
        
        for (AsyncRequest siRequest : eligibleOffers) {
            totalProfit += siRequest.getNeededInstances()*priceCandidate;
        }
        
        return totalProfit;
    }

    private static Collection<AsyncRequest> union(
            Collection<AsyncRequest> priorityOffers,
            Collection<AsyncRequest> limitOffers) {
        
        LinkedList<AsyncRequest> union = new LinkedList<AsyncRequest>(priorityOffers);
        union.addAll(limitOffers);
        
        return union;
    }

    private static Double getUtilization(Collection<AsyncRequest> bids, Integer offeredResources){
        
        Double demand = 0.0;
        
        for (AsyncRequest siRequest : bids) {
            demand += siRequest.getNeededInstances();
        }
        
        return demand/offeredResources;
    }

}
