package org.globus.workspace.spotinstances;

import org.globus.workspace.service.binding.vm.VirtualMachine;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.ctx.Context;
import org.nimbustools.api.repr.vm.NIC;

public class SIRequest implements Comparable<SIRequest>{
 
    private String id;
    private Double maxBid;
    private Integer requestedInstances;
    private Integer allocatedInstances;
    private Integer fulfilledInstances;
    private boolean persistent;
    private SIRequestStatus status;
    
    private Caller caller;
    private VirtualMachine[] bindings;
    private Context context;
    private NIC[] requestedNics;
    private String groupID;

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

    public SIRequest(String id, Double spotPrice, boolean persistent,
            Caller caller, String groupID, VirtualMachine[] bindings, Context context,
            NIC[] requestedNics) {
        this.requestedInstances = bindings.length;
        this.allocatedInstances = 0;
        this.fulfilledInstances = 0;
        this.status = SIRequestStatus.OPEN;        
        this.id = id;
        this.maxBid = spotPrice;
        this.persistent = persistent;
        this.bindings = bindings;
        this.context = context;
        this.requestedNics = requestedNics;
        this.groupID = groupID;
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

    @Override
    public int compareTo(SIRequest o) {
        return getMaxBid().compareTo(o.getMaxBid());
    }

    public Caller getCaller() {
        return caller;
    }

    public VirtualMachine[] getBindings() {
        return bindings;
    }

    public Context getContext() {
        return context;
    }

    public NIC[] getRequestedNics() {
        return requestedNics;
    }

    public String getGroupID() {
        return groupID;
    }    

    @Override
    public String toString() {
        return "SIRequest [id " + id + ", status= " + status + ", requestedInstances="
                + requestedInstances + ", allocatedInstances=" + allocatedInstances
                + ", maxBid=" + maxBid + ", persistent=" + persistent 
                + ", caller=" + caller + "]";
    }
    
    
}
