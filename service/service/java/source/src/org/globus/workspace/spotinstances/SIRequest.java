package org.globus.workspace.spotinstances;

import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;

import org.globus.workspace.service.binding.vm.VirtualMachine;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.ctx.Context;
import org.nimbustools.api.repr.vm.NIC;



public class SIRequest implements Comparable<SIRequest>{
 
    private String id;
    private Double maxBid;
    private Integer requestedInstances;
    private boolean persistent;
    private SIRequestStatus status;
    
    private Caller caller;
    private VirtualMachine[] bindings;
    private Context context;
    private NIC[] requestedNics;
    private String groupID;
    
    private Throwable problem = null;

    private LinkedHashSet<Integer> allocatedVMs = new LinkedHashSet<Integer>();
    private LinkedHashSet<Integer> finishedVMs = new LinkedHashSet<Integer>();
    private Calendar creationTime;

    public SIRequest(String id, Double highestPrice, Integer requestedInstances) {
        this(id, highestPrice, requestedInstances, false);
    }    
    
    public SIRequest(String id, Double highestPrice, Integer requestedInstances, boolean persistent) {
        this.id = id;
        this.maxBid = highestPrice;
        this.requestedInstances = requestedInstances;
        this.persistent = persistent;
        this.status = SIRequestStatus.OPEN;
    }  

    public SIRequest(String id, Double spotPrice, boolean persistent,
            Caller caller, String groupID, VirtualMachine[] bindings, Context context,
            NIC[] requestedNics, Calendar creationTime) {
        this.requestedInstances = bindings.length;
        this.status = SIRequestStatus.OPEN;        
        this.id = id;
        this.maxBid = spotPrice;
        this.persistent = persistent;
        this.bindings = bindings;
        this.context = context;
        this.requestedNics = requestedNics;
        this.groupID = groupID;
        this.caller = caller;
        this.creationTime = creationTime;
    }

    public Double getMaxBid() {
        return maxBid;
    }
    
    public void setMaxBid(Double maxBid) {
        this.maxBid = maxBid;
    }
    
    public Integer getNeededInstances(){
        if(this.persistent){
            return this.requestedInstances;
        } else {
            return this.requestedInstances-this.finishedVMs.size();
        }
    }
    
    public Integer getAllocatedInstances() {
        return allocatedVMs.size();
    }
    
    public Collection<Integer> getVMIds(){
        return Collections.unmodifiableCollection(this.allocatedVMs);
    }

    public void addAllocatedVM(int createdId) {
        this.allocatedVMs.add(createdId);
        this.setStatus(SIRequestStatus.ACTIVE);
    }
    
    public Integer getUnallocatedInstances(){
        return this.getNeededInstances() - getAllocatedInstances();
    }
    
    public Boolean needsMoreInstances(){
        return !this.getUnallocatedInstances().equals(0);
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
        if(this.status.isAlive()){
            this.status = status;
        }
    }

    @Override
    public int compareTo(SIRequest o) {
        int compareBid = getMaxBid().compareTo(o.getMaxBid());
        
        if(compareBid == 0){
            //Older created requests have preference over newly created ones
            return this.creationTime.compareTo(o.creationTime);
        }
        
        return compareBid;
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
    
    public boolean isAllocatedVM(Integer vmid){        
        return allocatedVMs.contains(vmid);
    }
    
    public Throwable getProblem() {
        return problem;
    }

    public void setProblem(Throwable problem) {
        this.problem = problem;
    }

    @Override
    public String toString() {
        return "SIRequest [id " + id + ", status= " + status + ", requestedInstances="
                + requestedInstances + ", allocatedInstances=" + getAllocatedInstances()
                + ", maxBid=" + maxBid + ", persistent=" + persistent 
                + ", caller=" + caller + "]";
    }

    public void finishVM(int vmid, Double spotPrice) {
        if(this.allocatedVMs.remove(vmid)){
            if(this.persistent){
                //If the request is persistent, it is going to be considered again
                //for fulfillment, so, clone VirtualMachine and unset ID to avoid 
                //garbage from previous object
                for (int i = 0; i < bindings.length; i++) {
                    VirtualMachine vm = bindings[i];
                    if(vm.getID() == vmid){
                        try {
                            //TODO: Check if this procedure is OK
                            bindings[i] = VirtualMachine.cloneOne(vm);
                            bindings[i].setID(-1);
                        } catch (Exception e) {
                            // won't happen
                        }
                        break;
                    }
                }
            }
            
            finishedVMs.add(vmid);
            if(status.isAlive()){
                changeStatus(spotPrice);
            }
        }
    }

    private void changeStatus(Double spotPrice) {
        if(spotPrice != null && this.allocatedVMs.isEmpty()){
            if(!this.persistent && (!needsMoreInstances() || spotPrice > this.maxBid)){
                this.setStatus(SIRequestStatus.CLOSED);
            } else {
                this.setStatus(SIRequestStatus.OPEN);
            }
        }
    }
    
    public VirtualMachine[] getUnallocatedVMs(int quantity) throws SIRequestException{
        if(this.getUnallocatedInstances() < quantity){
            throw new SIRequestException("Requested " + quantity + " unallocated VMs, but there are only " + this.getUnallocatedInstances() + ".");
        }        
        
        VirtualMachine[] result = new VirtualMachine[quantity];
        int j = 0;
        
        for (int i = 0; i < bindings.length && j < quantity; i++) {
            VirtualMachine current = bindings[i];
            if(current.getID().equals(-1)){
                result[j++] = current;
            }
        }
        
        return result;
    }
    
    public int[] getAllocatedVMs(int quantity) throws SIRequestException{
        if(this.getAllocatedInstances() < quantity){
            throw new SIRequestException("Requested " + quantity + " allocated VMs, but there are only " + getAllocatedInstances() + ".");
        }
        
        int[] result = new int[quantity];
                
        LinkedList<Integer> allocations = new LinkedList<Integer>(allocatedVMs);
        for (int i = 0; i < quantity; i++) {
            if(!allocations.isEmpty()){
                //Since this will be used for pre-emption
                //getting more recent allocations (last on the list)
                result[i] = allocations.removeLast();
            }
        }
        
        return result;
    }

    public Calendar getCreationTime() {
        return this.creationTime;
    }

    public void cancelRequest() {
        for (Integer id : allocatedVMs) {
            finishVM(id, null);
        }
        this.setStatus(SIRequestStatus.CANCELLED);
    }    
}
