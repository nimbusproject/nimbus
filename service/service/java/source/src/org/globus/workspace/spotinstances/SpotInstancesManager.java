package org.globus.workspace.spotinstances;

import java.util.HashMap;
import java.util.Map;

public class SpotInstancesManager {

    protected Integer totalReservedResources;
        
    protected Map<String, SIRequest> requests;
    
    protected PricingModel pricingModel;
    
    protected Double currentPrice;
    
    public SpotInstancesManager(){
        this.totalReservedResources = 0;
        this.requests = new HashMap<String, SIRequest>();
        this.currentPrice = MaximizeProfitPricingModel.MINIMUM_PRICE;
        this.pricingModel = new MaximizeProfitPricingModel();
    }
    
    public void addReservedResource(Integer resourceQuantity){
        totalReservedResources += resourceQuantity;
        setPrice();
    }
    
    public void removeReservedResource(Integer resourceQuantity){
        totalReservedResources -= resourceQuantity;
        setPrice();
    }
    
    public void addRequest(String requestID, SIRequest request){
        requests.put(requestID, request);
        setPrice();
    }
    
    public SIRequest removeRequest(String requestID){
        SIRequest request = requests.get(requestID);
        setPrice();
        return request;
    }
    
    public Double getCurrentPrice(){
        return currentPrice;
    }
    
    protected void setPrice(){
        this.currentPrice = pricingModel.getNextPrice(totalReservedResources, requests.values(), currentPrice);
    }
}
