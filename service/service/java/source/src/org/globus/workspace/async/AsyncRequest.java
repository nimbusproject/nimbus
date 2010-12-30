package org.globus.workspace.async;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;

import org.globus.workspace.service.binding.vm.VirtualMachine;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.ctx.Context;
import org.nimbustools.api.repr.vm.NIC;

public class AsyncRequest implements Comparable<AsyncRequest>, Serializable {
 
    private String id;
    private boolean spot;

    private Double maxBid;
    private boolean persistent;
    private AsyncRequestStatus status;
    
    private Caller caller;
    private VirtualMachine[] bindings;
    private Context context;
    private NIC[] requestedNics;
    private String groupID;
    private String sshKeyName;

    private Throwable problem = null;

    private LinkedHashSet<Integer> allocatedVMs = new LinkedHashSet<Integer>();
    private LinkedHashSet<Integer> finishedVMs = new LinkedHashSet<Integer>();
    private LinkedHashSet<Integer> toBePreempted = new LinkedHashSet<Integer>();
    private Calendar creationTime;

    //Test-only
    public AsyncRequest(String id, Double highestPrice, VirtualMachine[] bindings) {
        this(id, highestPrice, false, null, null, bindings, null, null, null, null);
    }    

    /**
     * Constructor for Spot Instance requests
     * @param id
     * @param spotPrice
     * @param persistent
     * @param caller
     * @param groupID
     * @param bindings
     * @param context
     * @param requestedNics
     * @param creationTime
     */
    public AsyncRequest(String id, Double spotPrice, boolean persistent,
            Caller caller, String groupID, VirtualMachine[] bindings, Context context,
            NIC[] requestedNics, String sshKeyName, Calendar creationTime) {
        this(id, true, spotPrice, persistent, caller, groupID, bindings, context, requestedNics, sshKeyName, creationTime);
    }
    
    /**
     * Constructor for backfill requests
     * @param id
     * @param spotPrice
     * @param persistent
     * @param caller
     * @param groupID
     * @param bindings
     * @param context
     * @param requestedNics
     * @param creationTime
     */    
    public AsyncRequest(String id, Caller caller, String groupID, VirtualMachine[] bindings, Context context,
            NIC[] requestedNics, Calendar creationTime) {
       this(id, false, -1.0, true, caller, groupID, bindings, context, requestedNics, null, creationTime);
    }    

    public AsyncRequest(String id, boolean spotinstances, Double spotPrice, boolean persistent,
            Caller caller, String groupID, VirtualMachine[] bindings, Context context,
            NIC[] requestedNics, String sshKeyName, Calendar creationTime) {
        this.status = AsyncRequestStatus.OPEN;
        this.id = id;
        this.spot = spotinstances;
        this.maxBid = spotPrice;
        this.persistent = persistent;
        this.bindings = bindings;
        this.context = context;
        this.requestedNics = requestedNics;
        this.groupID = groupID;
        this.caller = caller;
        this.creationTime = creationTime;
        this.sshKeyName = sshKeyName;
    }
    
    public Double getMaxBid() {
        return maxBid;
    }
    
    public Integer getNeededInstances(){
        if(this.status.isCancelled()){
            return this.getAllocatedInstances();
        } 
        
        if(this.persistent){
            return this.getRequestedInstances();
        } else {
            return this.getRequestedInstances()-this.finishedVMs.size();
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
    }
    
    public Integer getUnallocatedInstances(){
        return this.getNeededInstances() - getAllocatedInstances();
    }
    
    public Boolean needsMoreInstances(){
        return this.statusIsOpenOrActive() && !this.getUnallocatedInstances().equals(0);
    }

    public String getId() {
        return id;
    }

    public boolean isPersistent() {
        return persistent;
    }

    public AsyncRequestStatus getStatus() {
        return status;
    }

    public boolean isAlive(){
        return this.statusIsOpenOrActive() || (this.status.isCancelled() && !allocatedVMs.isEmpty());
    }
    
    public boolean setStatus(AsyncRequestStatus status) {
        if(statusIsOpenOrActive()){
            this.status = status;
            return true;
        }
        return false;
    }

    private boolean statusIsOpenOrActive() {
        return this.status.isOpen() || this.status.isActive();
    }

    public int compareTo(AsyncRequest o) {
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
    
    public String getSshKeyName() {
        return sshKeyName;
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
                + getRequestedInstances() + ", allocatedInstances=" + getAllocatedInstances()
                + ", maxBid=" + maxBid + ", persistent=" + persistent 
                + ", caller=" + caller + "]";
    }

    public boolean finishVM(int vmid) {
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
        }
        return toBePreempted.remove(vmid);
    }
    
    public VirtualMachine[] getUnallocatedVMs(int quantity) throws AsyncRequestException{
        if(this.getUnallocatedInstances() < quantity){
            throw new AsyncRequestException("Requested " + quantity + " unallocated VMs, but there are only " + this.getUnallocatedInstances() + ".");
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
    
    public int[] getAllocatedVMs(int quantity) throws AsyncRequestException{
        if(this.getAllocatedInstances() < quantity){
            throw new AsyncRequestException("Requested " + quantity + " allocated VMs, but there are only " + getAllocatedInstances() + ".");
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

    public Integer getRequestedInstances() {
        return bindings.length;
    }

    public void preemptAll() {
        for (int i : allocatedVMs) {
            toBePreempted.add(i);
        }
    }
    
    public void preempt(int[] preemptionList) {
        for (int i : preemptionList) {
            toBePreempted.add(i);
        }
    }
    
    public boolean isSpotRequest() {
        return spot;
    }    
}
