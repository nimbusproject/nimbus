package org.globus.workspace.async;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AsyncRequestFilter {

    public static List<AsyncRequest> getRequestsEqualPrice(
            Double price, Collection<AsyncRequest> allRequests) {
        
        List<AsyncRequest> offersEqualPrice = new ArrayList<AsyncRequest>();
        
        for (AsyncRequest siRequest : allRequests) {
            if(siRequest.getMaxBid() == price){
                offersEqualPrice.add(siRequest);
            }
        }
        
        return offersEqualPrice;
    }

    public static List<AsyncRequest> getRequestsAbovePrice(Double price,
            Collection<AsyncRequest> allRequests) {

        List<AsyncRequest> offersAbovePrice = new ArrayList<AsyncRequest>();
        
        for (AsyncRequest siRequest : allRequests) {
            if(siRequest.getMaxBid() > price){
                offersAbovePrice.add(siRequest);
            }
        }
        
        return offersAbovePrice;          
    }
    
    public static List<AsyncRequest> filterAllocatedRequestsBelowPrice(Double price,
            Collection<AsyncRequest> allRequests) {

        List<AsyncRequest> allocatedRequestsBelowPrice = new ArrayList<AsyncRequest>();
        
        for (AsyncRequest siRequest : allRequests) {
            if(siRequest.isSpotRequest() && siRequest.getAllocatedInstances() > 0 && siRequest.getMaxBid() < price){
                allocatedRequestsBelowPrice.add(siRequest);
            }
        }
        
        return allocatedRequestsBelowPrice;          
    }

    public static List<AsyncRequest> filterAliveRequestsEqualPrice(
            Double price, Collection<AsyncRequest> allRequests) {
        
        List<AsyncRequest> activeRequestsEqualPrice = new ArrayList<AsyncRequest>();
        
        for (AsyncRequest siRequest : allRequests) {
            if(siRequest.isSpotRequest() && siRequest.isAlive() && siRequest.getMaxBid().equals(price)){
                activeRequestsEqualPrice.add(siRequest);
            }
        }
        
        return activeRequestsEqualPrice;
    }
    
    public static List<AsyncRequest> filterAliveRequestsAbovePrice(
            Double currentPrice, Collection<AsyncRequest> allRequests) {
        List<AsyncRequest> aliveRequestsAbovePrice = new ArrayList<AsyncRequest>();
        
        for (AsyncRequest siRequest : allRequests) {
            if(siRequest.isSpotRequest() && siRequest.isAlive() && siRequest.getMaxBid() > currentPrice){
                aliveRequestsAbovePrice.add(siRequest);
            }
        }
        
        return aliveRequestsAbovePrice;
    }

    public static List<AsyncRequest> filterAliveRequestsAboveOrEqualPrice(
            Double minPrice, Collection<AsyncRequest> allRequests) {
        
        List<AsyncRequest> aliveRequests = new ArrayList<AsyncRequest>();
        
        for (AsyncRequest siRequest : allRequests) {
            if(siRequest.isSpotRequest() && siRequest.isAlive()){
                aliveRequests.add(siRequest);
            }
        }
        
        return aliveRequests;
    }

    public static List<AsyncRequest> filterAliveBackfillRequests(
            Collection<AsyncRequest> allRequests) {
        List<AsyncRequest> activeRequests = new ArrayList<AsyncRequest>();
        
        for (AsyncRequest siRequest : allRequests) {
            if(!siRequest.isSpotRequest() && siRequest.isAlive()){
                activeRequests.add(siRequest);
            }
        }
        
        return activeRequests;
    } 
    
}
