package org.globus.workspace.spotinstances;

public class SIRequest {
 
    private Double maxBid;
    private Integer quantity; 
    
    public SIRequest(Double highestPrice, Integer quantity) {
        this.maxBid = highestPrice;
        this.quantity = quantity;
    }
    
    public Double getMaxBid() {
        return maxBid;
    }
    
    public void setMaxBid(Double maxBid) {
        this.maxBid = maxBid;
    }
    
    public Integer getQuantity() {
        return quantity;
    }
    
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
    
    
    
}
