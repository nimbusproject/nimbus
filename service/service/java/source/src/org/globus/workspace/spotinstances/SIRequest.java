package org.globus.workspace.spotinstances;

public class SIRequest implements Comparable<SIRequest>{
 
    private String id;
    private Double maxBid;
    private Integer requestedInstances;
    private Integer allocatedInstances;
    private Integer fulfilledInstances;
    private boolean persistent;
    private SIRequestStatus status;

    public SIRequest(String id, Double highestPrice, Integer requestedInstances) {
        this(id, highestPrice, requestedInstances, false);
    }    
    
    public SIRequest(String id, Double highestPrice, Integer requestedInstances, boolean persistent) {
        this.id = id;
        this.maxBid = highestPrice;
        this.requestedInstances = requestedInstances;
        this.persistent = persistent;
        this.allocatedInstances = 0;
        this.fulfilledInstances = 0;
        this.status = SIRequestStatus.OPEN;
    }  
    
    public Double getMaxBid() {
        return maxBid;
    }
    
    public void setMaxBid(Double maxBid) {
        this.maxBid = maxBid;
    }
    
    public Integer getRequestedInstances(){
        return this.requestedInstances;
    }    
    
    public Integer getNeededInstances(){
        if(this.persistent){
            return this.requestedInstances;
        } else {
            return this.requestedInstances-this.fulfilledInstances;
        }
    }
    
    public Integer getAllocatedInstances() {
        return allocatedInstances;
    }

    public void setAllocatedInstances(Integer allocatedInstances) {
        this.allocatedInstances = allocatedInstances;
    }
    
    public Integer getPendingInstances(){
        return this.getNeededInstances() - this.allocatedInstances;
    }
    
    public Boolean needsMoreInstances(){
        return !this.getPendingInstances().equals(0);
    }

    public String getId() {
        return id;
    }

    public boolean isPersistent() {
        return persistent;
    }

    public void setPersistent(boolean persistent) {
        this.persistent = persistent;
    }

    public SIRequestStatus getStatus() {
        return status;
    }

    public void setStatus(SIRequestStatus status) {
        this.status = status;
    }
    
    public void addFulfilledInstances(Integer quantity){
        if((this.fulfilledInstances+quantity) >= this.requestedInstances){
            this.fulfilledInstances = requestedInstances;
        } else {
            fulfilledInstances += quantity;
        }  
    }
    
    public boolean isAlive(){
        return this.status.equals(SIRequestStatus.OPEN) || this.status.equals(SIRequestStatus.ACTIVE);
    }
    

    @Override
    public int compareTo(SIRequest o) {
        return getMaxBid().compareTo(o.getMaxBid());
    }    
    
}
