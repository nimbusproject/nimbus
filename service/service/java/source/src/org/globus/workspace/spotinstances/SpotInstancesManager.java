package org.globus.workspace.spotinstances;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.workspace.Lager;
import org.globus.workspace.persistence.PersistenceAdapter;
import org.globus.workspace.persistence.WorkspaceDatabaseException;

public class SpotInstancesManager {

    private static Integer BASIC_MACHINE_MEM = 128;
    
    private static final Integer MINIMUM_RESERVED_MEMORY = 128;
    private static final Double MAX_NON_PREEMP_UTILIZATION = 0.7;

    private static final Log logger =
        LogFactory.getLog(SpotInstancesManager.class.getName());
    
    protected Integer availableResources;

    protected Map<String, SIRequest> allRequests;

    protected PricingModel pricingModel;

    protected Double currentPrice;

    protected Lager lager;

    private PersistenceAdapter persistence;

    public SpotInstancesManager(PersistenceAdapter persistenceAdapter, Lager lager){
        this.allRequests = new HashMap<String, SIRequest>();
        this.currentPrice = PricingModelConstants.MINIMUM_PRICE;
        this.pricingModel = new MaximizeUtilizationPricingModel();
        this.persistence = persistenceAdapter;         
        this.lager = lager;
        this.availableResources = 0;
        
        calculateAvailableResources();
    }

    private void calculateAvailableResources() {
        
        if (this.lager.eventLog) {
            logger.info(Lager.ev(-1) + "[Spot Instances] Calculating available SI resources..");
        }        
        
        Integer resourceQuantity = 0;
        
        try {
            Integer availableMem = persistence.getTotalAvailableMemory();
            Integer maxMem = persistence.getTotalMaxMemory();
            Integer usedPreemptableMem = persistence.getTotalPreemptableMemory();             
           
            Integer usedNonPreemptableMem = maxMem - availableMem - usedPreemptableMem;
            
            //Formula derived from maximum_utilization =       usedNonPreemptable
            //                                           ---------------------------------
            //                                           usedNonPreemptable + reservedNonPreempMem
            Integer reservedNonPreempMem = (int)Math.round((1-MAX_NON_PREEMP_UTILIZATION)*usedNonPreemptableMem/MAX_NON_PREEMP_UTILIZATION);
            reservedNonPreempMem = Math.max(reservedNonPreempMem, MINIMUM_RESERVED_MEMORY);
            
            Integer siMem;
            if(availableMem >= reservedNonPreempMem){
                siMem = (availableMem - reservedNonPreempMem) + usedPreemptableMem;
            } else {
                if (this.lager.eventLog) {
                    logger.info(Lager.ev(-1) + "[Spot Instances] Not enough available memory. Decreasing SI resources.");
                }                
                siMem = Math.min(usedPreemptableMem-reservedNonPreempMem, 0);
            }
            
            if (this.lager.eventLog) {
                logger.info(Lager.ev(-1) + "[Spot Instances] Maximum site memory: " + maxMem);
                logger.info(Lager.ev(-1) + "[Spot Instances] Used non pre-emptable memory: " + usedNonPreemptableMem);
                logger.info(Lager.ev(-1) + "[Spot Instances] Used pre-emptable memory: " + usedPreemptableMem);                
                logger.info(Lager.ev(-1) + "[Spot Instances] Available site memory: " + availableMem);
                logger.info(Lager.ev(-1) + "[Spot Instances] Calculated memory for SI requests: " + siMem);
            }            
            
            resourceQuantity = siMem/BASIC_MACHINE_MEM;
            
        } catch (WorkspaceDatabaseException e) {
            e.printStackTrace();
        }
        
        if (this.lager.eventLog) {
            logger.info(Lager.ev(-1) + "[Spot Instances] Available basic SI instances (128MB RAM): " + resourceQuantity);
        }        

        setAvailableResources(resourceQuantity);
    }

    public void setAvailableResources(Integer resourceQuantity){
        if (this.lager.eventLog) {
            logger.info(Lager.ev(-1) + "[Spot Instances] RESOURCE QUANTITY CHANGED. OLD AVAILABLE RESOURCES = " + availableResources + ". NEW AVAILABLE RESOURCES = " + resourceQuantity);
        }              
        availableResources = resourceQuantity;
        changePrice();
    }

    public void addRequest(SIRequest request){
        allRequests.put(request.getId(), request);
        if (this.lager.eventLog) {
            logger.info(Lager.ev(-1) + "[Spot Instances] REQUEST ARRIVED: " + request.toString());
        }        
        changePrice();
    }

    public SIRequest removeRequest(String requestID){
        SIRequest request = allRequests.get(requestID);
        if (this.lager.eventLog) {
            logger.info(Lager.ev(-1) + "[Spot Instances] REQUEST REMOVED: " + request.toString());
        }        
        if(request.getStatus().isActive()){
            this.changeAllocation(request, 0);
        }
        changePrice();
        return request;
    }

    public Double getCurrentPrice(){
        return currentPrice;
    }

    protected void changePrice(){
        Double oldPrice = this.currentPrice;
        this.currentPrice = pricingModel.getNextPrice(availableResources, getAliveRequests(), currentPrice);
        if (this.lager.eventLog) {
            logger.info(Lager.ev(-1) + "[Spot Instances] PRICE CHANGED. OLD PRICE = " + oldPrice + ". NEW PRICE = " + this.currentPrice);
        }        
        this.reallocateRequests();
    }


    private void reallocateRequests() {

        preemptLowerBidRequests();

        allocateEqualBidRequests();

        allocateHigherBidRequests();
    }

    private void allocateHigherBidRequests() {
        
        if (this.lager.eventLog) {
            logger.info(Lager.ev(-1) + "[Spot Instances] ALLOCATING HIGHER BID REQUESTS.");
        }                 
        
        Collection<SIRequest> aliveRequests = getHigherBidAliveRequests();
        
        for (SIRequest aliveRequest : aliveRequests) {
            changeAllocation(aliveRequest, aliveRequest.getNeededInstances());
        }
    }

    private void allocateEqualBidRequests() {

        if (this.lager.eventLog) {
            logger.info(Lager.ev(-1) + "[Spot Instances] ALLOCATING EQUAL BID REQUESTS.");
        }         
        
        Integer greaterBidResources = getGreaterBidResourcesCount();

        Integer availableResources = this.availableResources - greaterBidResources;

        Collection<SIRequest> activeRequests = getEqualBidActiveRequests();

        LinkedList<SIRequest> partiallyAttendedReqs = new LinkedList<SIRequest>();

        boolean hasLogged = false;
        
      //Tries to satisfy currently allocated requests (pre-empts allocated instances that exceeds capacity)
        for (SIRequest activeRequest : activeRequests) {
            Integer allocatedInstances = activeRequest.getAllocatedInstances();
            if(availableResources >= allocatedInstances){
                availableResources -= allocatedInstances;
                if(!activeRequest.needsMoreInstances()){
                    partiallyAttendedReqs.add(activeRequest);
                }
            } else {
                if (!hasLogged && this.lager.eventLog) {
                    logger.info(Lager.ev(-1) + "[Spot Instances] No more resources for equal bid requests. Pre-empting.");
                    hasLogged = true;
                }                
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
        
        this.currentPrice = pricingModel.getNextPrice(availableResources, getAliveRequests(), currentPrice);
        if (this.lager.eventLog) {
            logger.info(Lager.ev(-1) + "[Spot Instances] PRE-EMPTING LOWER BID REQUESTS.");
        } 
        
        Collection<SIRequest> inelegibleRequests = getLowerBidActiveRequests();
        for (SIRequest inelegibleRequest : inelegibleRequests) {
            changeAllocation(inelegibleRequest, 0);
        }
    }

    private void changeAllocation(SIRequest siRequest, Integer newAllocation) {

        
        if (this.lager.eventLog) {
            logger.info(Lager.ev(-1) + "[Spot Instances] CHANGING ALLOCATION - BEFORE: " + siRequest.toString());
        } 
        
        Integer oldAllocation = siRequest.getAllocatedInstances();

        Integer delta = newAllocation - oldAllocation;

        if(delta > 0){
            allocate(siRequest, delta);
        } else if (delta < 0) {
            preempt(siRequest, -delta);
        }

        siRequest.setAllocatedInstances(newAllocation);

        if (this.lager.eventLog) {
            logger.info(Lager.ev(-1) + "[Spot Instances] CHANGING ALLOCATION - AFTER: " + siRequest.toString());
        }
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
    
    public static void main(String[] args) {
        System.out.println(255/128);
    }
}
