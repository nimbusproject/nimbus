package org.globus.workspace.spotinstances;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SIRequestUtils {

    public static Collection<SIRequest> getRequestsEqualPrice(
            Double price, Collection<SIRequest> allRequests) {
        
        List<SIRequest> offersEqualPrice = new ArrayList<SIRequest>();
        
        for (SIRequest siRequest : allRequests) {
            if(siRequest.getMaxBid() == price){
                offersEqualPrice.add(siRequest);
            }
        }
        
        return offersEqualPrice;
    }

    public static Collection<SIRequest> getRequestsAbovePrice(Double price,
            Collection<SIRequest> allRequests) {

        List<SIRequest> offersAbovePrice = new ArrayList<SIRequest>();
        
        for (SIRequest siRequest : allRequests) {
            if(siRequest.getMaxBid() > price){
                offersAbovePrice.add(siRequest);
            }
        }
        
        return offersAbovePrice;          
    }
    
    public static Collection<SIRequest> filterActiveRequestsBelowPrice(Double price,
            Collection<SIRequest> allRequests) {

        List<SIRequest> activeRequestsBelowPrice = new ArrayList<SIRequest>();
        
        for (SIRequest siRequest : allRequests) {
            if(siRequest.getStatus().equals(SIRequestStatus.ACTIVE) && siRequest.getMaxBid() < price){
                activeRequestsBelowPrice.add(siRequest);
            }
        }
        
        return activeRequestsBelowPrice;          
    }

    public static Collection<SIRequest> filterActiveRequestsEqualPrice(
            Double price, Collection<SIRequest> allRequests) {
        
        List<SIRequest> activeRequestsEqualPrice = new ArrayList<SIRequest>();
        
        for (SIRequest siRequest : allRequests) {
            if(siRequest.getStatus().equals(SIRequestStatus.ACTIVE) && siRequest.getMaxBid().equals(price)){
                activeRequestsEqualPrice.add(siRequest);
            }
        }
        
        return activeRequestsEqualPrice;
    }
    
    public static Collection<SIRequest> filterOpenRequestsEqualPrice(
            Double price, Collection<SIRequest> allRequests) {
        
        List<SIRequest> inactiveRequestsEqualPrice = new ArrayList<SIRequest>();
        
        for (SIRequest siRequest : allRequests) {
            if(siRequest.getStatus().equals(SIRequestStatus.OPEN) && siRequest.getMaxBid().equals(price)){
                inactiveRequestsEqualPrice.add(siRequest);
            }
        }
        
        return inactiveRequestsEqualPrice;
    }

    public static Collection<SIRequest> filterAliveRequestsAbovePrice(
            Double currentPrice, Collection<SIRequest> allRequests) {
        List<SIRequest> aliveRequestsAbovePrice = new ArrayList<SIRequest>();
        
        for (SIRequest siRequest : allRequests) {
            if(siRequest.isAlive() && siRequest.getMaxBid() > currentPrice){
                aliveRequestsAbovePrice.add(siRequest);
            }
        }
        
        return aliveRequestsAbovePrice;
    }

    public static Collection<SIRequest> filterAliveRequests(
            Collection<SIRequest> allRequests) {
        
        List<SIRequest> aliveRequests = new ArrayList<SIRequest>();
        
        for (SIRequest siRequest : allRequests) {
            if(siRequest.isAlive()){
                aliveRequests.add(siRequest);
            }
        }
        
        return aliveRequests;
    }    
      
    
}
