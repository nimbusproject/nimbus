package org.globus.workspace.spotinstances;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SIRequestUtils {

    public static List<SIRequest> getRequestsEqualPrice(
            Double price, Collection<SIRequest> allRequests) {
        
        List<SIRequest> offersEqualPrice = new ArrayList<SIRequest>();
        
        for (SIRequest siRequest : allRequests) {
            if(siRequest.getMaxBid() == price){
                offersEqualPrice.add(siRequest);
            }
        }
        
        return offersEqualPrice;
    }

    public static List<SIRequest> getRequestsAbovePrice(Double price,
            Collection<SIRequest> allRequests) {

        List<SIRequest> offersAbovePrice = new ArrayList<SIRequest>();
        
        for (SIRequest siRequest : allRequests) {
            if(siRequest.getMaxBid() > price){
                offersAbovePrice.add(siRequest);
            }
        }
        
        return offersAbovePrice;          
    }
    
    public static List<SIRequest> filterActiveRequestsBelowPrice(Double price,
            Collection<SIRequest> allRequests) {

        List<SIRequest> activeRequestsBelowPrice = new ArrayList<SIRequest>();
        
        for (SIRequest siRequest : allRequests) {
            if(siRequest.getStatus().isActive() && siRequest.getMaxBid() < price){
                activeRequestsBelowPrice.add(siRequest);
            }
        }
        
        return activeRequestsBelowPrice;          
    }

    public static List<SIRequest> filterActiveRequestsEqualPrice(
            Double price, Collection<SIRequest> allRequests) {
        
        List<SIRequest> activeRequestsEqualPrice = new ArrayList<SIRequest>();
        
        for (SIRequest siRequest : allRequests) {
            if(siRequest.getStatus().isActive() && siRequest.getMaxBid().equals(price)){
                activeRequestsEqualPrice.add(siRequest);
            }
        }
        
        return activeRequestsEqualPrice;
    }
    
    public static List<SIRequest> filterAliveRequestsAbovePrice(
            Double currentPrice, Collection<SIRequest> allRequests) {
        List<SIRequest> aliveRequestsAbovePrice = new ArrayList<SIRequest>();
        
        for (SIRequest siRequest : allRequests) {
            if(siRequest.getStatus().isAlive() && siRequest.getMaxBid() > currentPrice){
                aliveRequestsAbovePrice.add(siRequest);
            }
        }
        
        return aliveRequestsAbovePrice;
    }

    public static List<SIRequest> filterAliveRequests(
            Collection<SIRequest> allRequests) {
        
        List<SIRequest> aliveRequests = new ArrayList<SIRequest>();
        
        for (SIRequest siRequest : allRequests) {
            if(siRequest.getStatus().isAlive()){
                aliveRequests.add(siRequest);
            }
        }
        
        return aliveRequests;
    }

    public static List<SIRequest> filterHungryAliveRequestsEqualPrice(
            Double price, Collection<SIRequest> allRequests) {
        
        List<SIRequest> hungryRequests = new ArrayList<SIRequest>();
        
        for (SIRequest siRequest : allRequests) {
            if(siRequest.getMaxBid().equals(price) && siRequest.needsMoreInstances() && siRequest.getStatus().isAlive()){
                hungryRequests.add(siRequest);
            }
        }
        
        return hungryRequests;
    }    
      
    
}
