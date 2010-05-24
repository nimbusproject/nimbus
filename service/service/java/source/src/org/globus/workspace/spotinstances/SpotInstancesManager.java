package org.globus.workspace.spotinstances;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

public class SpotInstancesManager {

    protected Integer totalReservedResources;

    protected Map<String, SIRequest> allRequests;

    protected PricingModel pricingModel;

    protected Double currentPrice;

    public SpotInstancesManager(){
        this.totalReservedResources = 0;
        this.allRequests = new HashMap<String, SIRequest>();
        this.currentPrice = AbstractPricingModel.MINIMUM_PRICE;
        this.pricingModel = new MaximizeUtilizationPricingModel();
    }

    public void addReservedResource(Integer resourceQuantity){
        totalReservedResources += resourceQuantity;
        changePrice();
    }

    public void removeReservedResource(Integer resourceQuantity){
        totalReservedResources -= resourceQuantity;
        changePrice();
    }

    public void addRequest(String requestID, SIRequest request){
        allRequests.put(requestID, request);
        changePrice();
    }

    public SIRequest removeRequest(String requestID){
        SIRequest request = allRequests.get(requestID);
        changePrice();
        return request;
    }

    public Double getCurrentPrice(){
        return currentPrice;
    }

    protected void changePrice(){
        this.currentPrice = pricingModel.getNextPrice(totalReservedResources, getAliveRequests(), currentPrice);
        this.reallocateRequests();
    }


    private void reallocateRequests() {

        preemptLowerBidRequests();

        allocateEqualBidRequests();

        allocateHigherBidRequests();
    }

    private void allocateHigherBidRequests() {
        
        Collection<SIRequest> aliveRequests = getHigherBidAliveRequests();
        
        for (SIRequest aliveRequest : aliveRequests) {
            changeAllocation(aliveRequest, aliveRequest.getNeededInstances());
        }
    }

    private void allocateEqualBidRequests() {

        Integer greaterBidResources = getGreaterBidResourcesCount();

        Integer availableResources = this.totalReservedResources - greaterBidResources;

        Collection<SIRequest> activeRequests = getEqualBidActiveRequests();

        LinkedList<SIRequest> partiallyAttendedReqs = new LinkedList<SIRequest>();

        
      //Tries to satisfy currently allocated requests (pre-empts allocated instances that exceeds capacity)
        for (SIRequest activeRequest : activeRequests) {
            Integer allocatedInstances = activeRequest.getAllocatedInstances();
            if(availableResources >= allocatedInstances){
                availableResources -= allocatedInstances;
                if(!activeRequest.needsMoreInstances()){
                    partiallyAttendedReqs.add(activeRequest);
                }
            } else {
                changeAllocation(activeRequest, availableResources);
                availableResources = 0;
            }
        }

        //Tries to satisfy partially attended requests
        Iterator<SIRequest> iterator = partiallyAttendedReqs.iterator();
        while(availableResources > 0 && iterator.hasNext()){
            SIRequest partialReq = iterator.next();
            Integer unallocatedInstances = partialReq.getPendingInstances();

            Integer extraInstances = (unallocatedInstances < availableResources) ? unallocatedInstances : availableResources;

            changeAllocation(partialReq, partialReq.getAllocatedInstances()+extraInstances);
            availableResources -= extraInstances;            
        }


        //Remaining open equal-bid requests is best effort
        Collection<SIRequest> openRequests = getEqualBidOpenRequests();
        iterator = openRequests.iterator();
        while(availableResources > 0 && iterator.hasNext()){
            SIRequest openReq = iterator.next();

            Integer requestedInstances = openReq.getNeededInstances();

            Integer alocatedInstances = (requestedInstances < availableResources) ? requestedInstances : availableResources;

            changeAllocation(openReq, alocatedInstances);
            availableResources -= alocatedInstances;                
        }

    }

    private Integer getGreaterBidResourcesCount() {
        Collection<SIRequest> priorityRequests = getHigherBidAliveRequests();

        Integer resourceCount = 0;

        for (SIRequest siRequest : priorityRequests) {
            resourceCount += siRequest.getNeededInstances();
        }

        return resourceCount;
    }

    private void preemptLowerBidRequests() {
        Collection<SIRequest> inelegibleRequests = getLowerBidActiveRequests();
        for (SIRequest inelegibleRequest : inelegibleRequests) {
            changeAllocation(inelegibleRequest, 0);
        }
    }

    private void changeAllocation(SIRequest siRequest, Integer newAllocation) {

        Integer oldAllocation = siRequest.getAllocatedInstances();

        Integer delta = newAllocation - oldAllocation;

        if(delta > 0){
            allocate(siRequest, delta);
        } else if (delta < 0) {
            preempt(siRequest, -delta);
        }

        siRequest.setAllocatedInstances(newAllocation);
    }    

    private void preempt(SIRequest siRequest, Integer instancesToPreempt) {
        
        if(instancesToPreempt.equals(siRequest.getAllocatedInstances())){
            if(siRequest.isPersistent()){
                siRequest.setStatus(SIRequestStatus.OPEN);
            } else {
                siRequest.setStatus(SIRequestStatus.CLOSED);
            }
        } else if(!siRequest.isPersistent()){
            siRequest.addFulfilledInstances(instancesToPreempt);
        }
    }

    private void allocate(SIRequest siRequest, Integer instancesToAllocate) {
        siRequest.setStatus(SIRequestStatus.ACTIVE);
    }

    private Collection<SIRequest> getLowerBidActiveRequests() {
        return SIRequestUtils.filterActiveRequestsBelowPrice(this.currentPrice, this.allRequests.values());
    }

    private Collection<SIRequest> getEqualBidActiveRequests(){
        return SIRequestUtils.filterActiveRequestsEqualPrice(this.currentPrice, this.allRequests.values());
    }

    private Collection<SIRequest> getEqualBidOpenRequests() {
        return SIRequestUtils.filterOpenRequestsEqualPrice(this.currentPrice, this.allRequests.values());
    }    
    
    private Collection<SIRequest> getHigherBidAliveRequests() {
        return SIRequestUtils.filterAliveRequestsAbovePrice(this.currentPrice, this.allRequests.values());
    }
    
    private Collection<SIRequest> getAliveRequests() {
        return SIRequestUtils.filterAliveRequests(this.allRequests.values());
    }    
}
