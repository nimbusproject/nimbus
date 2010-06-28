package org.globus.workspace.spotinstances;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.workspace.Lager;
import org.globus.workspace.WorkspaceConstants;
import org.globus.workspace.creation.InternalCreationManager;
import org.globus.workspace.persistence.PersistenceAdapter;
import org.globus.workspace.persistence.WorkspaceDatabaseException;
import org.globus.workspace.service.InstanceResource;
import org.globus.workspace.service.WorkspaceGroupHome;
import org.globus.workspace.service.WorkspaceHome;
import org.globus.workspace.service.binding.vm.VirtualMachine;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.si.SIConstants;
import org.nimbustools.api.services.rm.DoesNotExistException;
import org.nimbustools.api.services.rm.ManageException;


public class SpotInstancesManagerImpl implements SpotInstancesManager {

    private static String MACHINE_TYPE = SIConstants.SI_TYPE_BASIC;
    private static Integer INSTANCE_MEM = SIConstants.getInstanceMem(MACHINE_TYPE);
    
    private static final Integer MINIMUM_RESERVED_MEMORY = 256;
    private static final Double MAX_NON_PREEMP_UTILIZATION = 0.7;

    private static final Log logger =
        LogFactory.getLog(SpotInstancesManagerImpl.class.getName());
    
    protected Integer availableResources;

    protected Map<String, SIRequest> allRequests;

    protected PricingModel pricingModel;

    protected Double currentPrice;


    protected PersistenceAdapter persistence;
    protected Lager lager;
    protected InternalCreationManager creationManager;
    protected final WorkspaceHome home;
    protected final WorkspaceGroupHome ghome;

    public SpotInstancesManagerImpl(PersistenceAdapter persistenceAdapterImpl,
                                    Lager lagerImpl,
                                    WorkspaceHome instanceHome,
                                    WorkspaceGroupHome groupHome){

        this.allRequests = new HashMap<String, SIRequest>();
        this.currentPrice = PricingModelConstants.MINIMUM_PRICE;
        this.pricingModel = new MaximizeUtilizationPricingModel();
        this.availableResources = 0;

        if (persistenceAdapterImpl == null) {
            throw new IllegalArgumentException("persistenceAdapterImpl may not be null");
        }
        this.persistence = persistenceAdapterImpl;

        if (lagerImpl == null) {
            throw new IllegalArgumentException("lagerImpl may not be null");
        }
        this.lager = lagerImpl;

        if (instanceHome == null) {
            throw new IllegalArgumentException("instanceHome may not be null");
        }
        this.home = instanceHome;

        if (groupHome == null) {
            throw new IllegalArgumentException("groupHome may not be null");
        }
        this.ghome = groupHome;
    }
    
    // -------------------------------------------------------------------------
    // Implements org.globus.workspace.spotinstances.SpotInstancesHome
    // -------------------------------------------------------------------------     
    
    public void addRequest(SIRequest request){
        allRequests.put(request.getId(), request);
        if (this.lager.eventLog) {
            logger.info(Lager.ev(-1) + "[Spot Instances] REQUEST ARRIVED: " + request.toString() + ". Changing price and reallocating requests.");
        }        
        changePriceAndReallocateRequests();
    }
    
    @Override
    public SIRequest cancelRequest(String reqID) throws DoesNotExistException {
        logger.info(Lager.ev(-1) + "[Spot Instances] Cancelling request with id: " + reqID + ".");                
        SIRequest siRequest = getRequest(reqID, false);
        siRequest.cancelRequest();
        if(siRequest.getStatus().isActive()){
            preempt(siRequest, siRequest.getAllocatedInstances());
        }
        return siRequest;
    }    
    
    public SIRequest getRequest(String id) throws DoesNotExistException {
        return this.getRequest(id, true);
    }    
    
    protected SIRequest getRequest(String id, boolean log) throws DoesNotExistException {
        logger.info(Lager.ev(-1) + "[Spot Instances] Retrieving request with id: " + id + ".");                
        SIRequest siRequest = allRequests.get(id);
        if(siRequest != null){
            return siRequest;
        } else {
            throw new DoesNotExistException("Spot instance request with id " + id + " does not exists.");
        }
    }

    public SIRequest[] getRequests(Caller caller) {
        logger.info(Lager.ev(-1) + "[Spot Instances] Retrieving requests from caller: " + caller.getIdentity() + ".");        
        ArrayList<SIRequest> requestsByCaller = new ArrayList<SIRequest>();
        for (SIRequest siRequest : allRequests.values()) {
            if(siRequest.getCaller().equals(caller)){
                requestsByCaller.add(siRequest);
            }
        }
        return requestsByCaller.toArray(new SIRequest[0]);
    }

    public Double getSpotPrice() {
        return this.currentPrice;
    }    

//    public SIRequest removeRequest(String requestID){
//        SIRequest request = allRequests.get(requestID);
//        if(request != null){
//            if (this.lager.eventLog) {
//                logger.info(Lager.ev(-1) + "[Spot Instances] REQUEST REMOVED: " + request.toString() + ". Changing price and reallocating requests.");
//            }        
//            if(request.getStatus().isActive()){
//                //Pre-empt active VMs
//                this.changeAllocation(request, 0);
//            }
//            changePriceAndReallocateRequests();
//        }
//        return request;
//    }    

    protected synchronized void changePriceAndReallocateRequests(){
        Double newPrice = pricingModel.getNextPrice(availableResources, getAliveRequests(), currentPrice);
        if(!newPrice.equals(this.currentPrice)){
            if (this.lager.eventLog) {
                logger.info(Lager.ev(-1) + "[Spot Instances] PRICE CHANGED. OLD PRICE = " + this.currentPrice + ". NEW PRICE = " + newPrice);
            }
            this.currentPrice = newPrice;
        }
        
        //TODO Put this call in a separate thread to avoid blocking
        //TODO Create timer schema to avoid multiple calls to this method in a small time frame
        reallocateRequests();
        
        if(availableResources == 0){
            newPrice = pricingModel.getNextPrice(availableResources, getAliveRequests(), currentPrice);
            if(!newPrice.equals(this.currentPrice)){
                if (this.lager.eventLog) {
                    logger.info(Lager.ev(-1) + "[Spot Instances] PRICE CHANGED. OLD PRICE = " + this.currentPrice + ". NEW PRICE = " + newPrice);
                }
                this.currentPrice = newPrice;
            }
        }
    }

    // -------------------------------------------------------------------------
    // ALLOCATION
    // ------------------------------------------------------------------------- 

    protected void reallocateRequests() {

        preemptLowerBidRequests();

        allocateEqualBidRequests();

        allocateHigherBidRequests();
    }

    private void preemptLowerBidRequests() {
        
        this.currentPrice = pricingModel.getNextPrice(availableResources, getAliveRequests(), currentPrice);
        if (this.lager.eventLog) {
            logger.info(Lager.ev(-1) + "[Spot Instances] PRE-EMPTING LOWER BID REQUESTS.");
        } 
        
        Collection<SIRequest> inelegibleRequests = getLowerBidActiveRequests();
        for (SIRequest inelegibleRequest : inelegibleRequests) {
            preempt(inelegibleRequest, inelegibleRequest.getAllocatedInstances());
        }
    }    
    
    private void allocateHigherBidRequests() {
        
        if (this.lager.eventLog) {
            logger.info(Lager.ev(-1) + "[Spot Instances] ALLOCATING HIGHER BID REQUESTS.");
        }                 
        
        Collection<SIRequest> aliveRequests = getHigherBidAliveRequests();
        
        for (SIRequest aliveRequest : aliveRequests) {
            if(aliveRequest.needsMoreInstances()){
                allocate(aliveRequest, aliveRequest.getUnallocatedInstances());
            }
        }
    }

    private void allocateEqualBidRequests() {

        if (this.lager.eventLog) {
            logger.info(Lager.ev(-1) + "[Spot Instances] ALLOCATING EQUAL BID REQUESTS.");
        }         
        
        Integer greaterBidResources = getGreaterBidResourcesCount();

        Integer availableResources = this.availableResources - greaterBidResources;

        List<SIRequest> activeRequests = getEqualBidActiveRequests();

        Integer allocatedVMs = 0;
        for (SIRequest activeRequest : activeRequests) {
            allocatedVMs += activeRequest.getAllocatedInstances();
        }
        
        if(allocatedVMs <= availableResources){
            availableResources -= allocatedVMs;
        } else {
            Integer needToPreempt = allocatedVMs - availableResources;
            if (this.lager.eventLog) {
                logger.info(Lager.ev(-1) + "[Spot Instances] No more resources for equal bid requests. Pre-empting " + needToPreempt + " VMs.");   
            }
            preemptProportionaly(activeRequests, needToPreempt, allocatedVMs);
            return;
        }

        allocateEvenly(availableResources);
    }

    /**
     * Allocates equal bid requests in a balanced manner.
     * 
     * This means allocating the same number of VMs to each request
     * until all requests are satisfied, or there are no available
     * resources to distribute.
     * 
     * @param availableResources the number of VMs available
     *                           for allocation
     */
    private void allocateEvenly(Integer availableResources) {
        List<SIRequest> hungryRequests = getEqualBidHungryRequests();
        Collections.sort(hungryRequests, getAllocationComparator());
        
        Map<SIRequest, Integer> allocations = new HashMap<SIRequest, Integer>();
        for (SIRequest hungryRequest : hungryRequests) {
            allocations.put(hungryRequest, 0);
        }
        
        while(availableResources > 0 && !hungryRequests.isEmpty()){
            Integer vmsPerRequest = Math.max(availableResources/hungryRequests.size(), 1);
            
            Iterator<SIRequest> iterator = hungryRequests.iterator();
            while(availableResources > 0 && iterator.hasNext()){
                vmsPerRequest = Math.min(vmsPerRequest, availableResources);
                
                SIRequest siRequest = (SIRequest) iterator.next();
                Integer vmsToAllocate = allocations.get(siRequest);
                
                Integer stillNeeded = siRequest.getNeededInstances() - vmsToAllocate;
                if(stillNeeded <= vmsPerRequest){
                    allocations.put(siRequest, vmsToAllocate+stillNeeded);
                    availableResources -= stillNeeded;
                    iterator.remove();
                    continue;
                }
                
                allocations.put(siRequest, vmsToAllocate+vmsPerRequest);
                availableResources -= vmsPerRequest;
            }
            
            for (Entry<SIRequest, Integer> allocationEntry : allocations.entrySet()) {
                SIRequest siRequest = allocationEntry.getKey();
                allocate(siRequest, allocationEntry.getValue());
            }            
        }
    } 
    
    /**
     * Pre-empts equal bid requests more-or-less proportional 
     * to the number of allocations that the request currently has.
     * 
     * NOTE: Each ACTIVE request must have at least one
     * VM pre-empted in order to ensure the needed 
     * quantity will be pre-empted.
     * 
     * Example:
     * 
     * Req A: 3 allocations (33.33%)
     * Req B: 1 allocation (11.11%)
     * Req C: 5 allocations (55.55%)
     * 
     * If 6 machines needs to be pre-empted, the pre-emptions will be:
     * 
     * Req A: 2 pre-emptions (33.33%)
     * Req B: 1 pre-emption (11.11%)
     * Req C: 3 pre-emptions (55.55%)
     * 
     * @param activeRequests ACTIVE requests with bid equal to the current spot price
     * @param needToPreempt the number of VMs that needs to be pre-empted
     * @param allocatedVMs the number of currently allocated VMs in <b>activeRequests</b>
     */
    private void preemptProportionaly(List<SIRequest> activeRequests, Integer needToPreempt, Integer allocatedVMs) {
        
        Collections.sort(activeRequests, getPreemptionComparator());
        
        Integer stillToPreempt = needToPreempt;
        
        Iterator<SIRequest> iterator = activeRequests.iterator();
        while(iterator.hasNext() && stillToPreempt > 0){
            SIRequest siRequest = iterator.next();
            Double allocatedProportion = (double)siRequest.getAllocatedInstances()/allocatedVMs;
            
            //Minimum deserved pre-emption is 1
            Integer deservedPreemption = Math.max((int)Math.round(allocatedProportion*needToPreempt), 1);
            
            Integer realPreemption = Math.min(deservedPreemption, stillToPreempt); 
            preempt(siRequest, realPreemption);
            stillToPreempt -= realPreemption;
        }
        
        
        //This may never happen. But just in case.
        if(stillToPreempt > 0){
            logger.error("Unable to pre-empt VMs proportionally. Still " + stillToPreempt + 
                         " VMs to pre-empt. Pre-empting best-effort.");
            
            iterator = activeRequests.iterator();
            while(iterator.hasNext() && stillToPreempt > 0){
                SIRequest siRequest = iterator.next();
                Integer allocatedInstances = siRequest.getAllocatedInstances();
                if(allocatedInstances > 0){
                    if(allocatedInstances > stillToPreempt){
                        preempt(siRequest, stillToPreempt);
                        stillToPreempt = 0;
                    } else {
                        preempt(siRequest, allocatedInstances);
                        stillToPreempt -= allocatedInstances;                        
                    }
                }
            }
        }
    }

    private Comparator<SIRequest> getPreemptionComparator() {
        return new Comparator<SIRequest>() {

            @Override
            public int compare(SIRequest o1, SIRequest o2) {
                
                //Requests with more allocated instances come first
                int compareTo = o2.getAllocatedInstances().compareTo(o1.getAllocatedInstances());
                
                if(compareTo == 0){
                    //Newer requests come first
                    compareTo = o2.getCreationTime().compareTo(o1.getCreationTime());
                }
                
                return compareTo;
            }
        };
    }
    
    private Comparator<SIRequest> getAllocationComparator() {
        return new Comparator<SIRequest>() {

            @Override
            public int compare(SIRequest o1, SIRequest o2) {
                
                //Requests with less allocated instances come first
                int compareTo = o1.getAllocatedInstances().compareTo(o2.getAllocatedInstances());
                
                if(compareTo == 0){
                    //Older requests come first
                    compareTo = o1.getCreationTime().compareTo(o2.getCreationTime());
                }
                
                return compareTo;
            }
        };
    }    

    protected void preempt(SIRequest siRequest, int quantity) {
        if (this.lager.eventLog) {
            logger.info(Lager.ev(-1) + "[Spot Instances] Pre-empting " + quantity + " VMs for request: " + siRequest.getId());
        }
        
        SIRequestStatus oldStatus = siRequest.getStatus();        
        
        int[] allocatedVMs = null;
        try {
            allocatedVMs = siRequest.getAllocatedVMs(quantity);
        } catch (SIRequestException e) {
            logger.fatal("[Spot Instances] " + e.getMessage(), e);
            return;
        }
        
        //FIXME: temporary, just for testing
        for (int i = 0; i < allocatedVMs.length; i++) {
            siRequest.finishVM(allocatedVMs[i], this.currentPrice);
        }
        
        if (!oldStatus.equals(siRequest.getStatus()) && this.lager.eventLog) {
            logger.info(Lager.ev(-1) + "[Spot Instances] Request '" + siRequest.getId() + "' changed status from " + oldStatus + " to " + siRequest.getStatus());
        }           
    }

    protected void allocate(SIRequest siRequest, Integer quantity) {
        if (this.lager.eventLog) {
            logger.info(Lager.ev(-1) + "[Spot Instances] Allocating " + quantity + " VMs for request: " + siRequest.getId());
        }
        
        SIRequestStatus oldStatus = siRequest.getStatus();
        
        VirtualMachine[] unallocatedVMs = null;
        try {
            unallocatedVMs = siRequest.getUnallocatedVMs(quantity);
        } catch (SIRequestException e) {
            logger.fatal("[Spot Instances] " + e.getMessage(), e);
            return;
        }
        
        try {
            InstanceResource[] createdVMs = creationManager.createVMs(unallocatedVMs, siRequest.getRequestedNics(), siRequest.getCaller(), siRequest.getContext(), siRequest.getGroupID(), null, true);
            for (InstanceResource resource : createdVMs) {
                siRequest.addAllocatedVM(resource.getID());
            }
        } catch (Exception e) {
            siRequest.setStatus(SIRequestStatus.FAILED);
            siRequest.setProblem(e);
            logger.warn(Lager.ev(-1) + "[Spot Instances] Error while allocating VMs for request: " + siRequest.getId() + ". Setting state to FAILED. Problem: " + e.getMessage());
        }
        
        if (!oldStatus.equals(siRequest.getStatus()) && this.lager.eventLog) {
            logger.info(Lager.ev(-1) + "[Spot Instances] Request " + siRequest.getId() + " changed status from " + oldStatus + " to " + siRequest.getStatus());
        }        
    }

    // -------------------------------------------------------------------------
    // DEFINE SPOT INSTANCES CAPACITY
    // -------------------------------------------------------------------------        
    
    protected synchronized void calculateAvailableResources() {
        
        if (this.lager.eventLog) {
            logger.info(Lager.ev(-1) + "[Spot Instances] Calculating available SI resources..");
        }        
        
        Integer resourceQuantity = 0;
        
        try {
            Integer availableMem = persistence.getTotalAvailableMemory(INSTANCE_MEM);
            Integer usedPreemptableMem = persistence.getTotalPreemptableMemory();             
            Integer usedNonPreemptableMem = persistence.getUsedNonPreemptableMemory();
            
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
                    logger.info(Lager.ev(-1) + "[Spot Instances] Not enough available memory for Spot Instances. Trying to satisfy currently active requests.");
                }                
                siMem = Math.max(usedPreemptableMem-reservedNonPreempMem, 0);
            }
            
            if (this.lager.eventLog) {
                logger.info(Lager.ev(-1) + "[Spot Instances] REAL available site memory: " + persistence.getTotalAvailableMemory(1) + "MB");
                logger.info(Lager.ev(-1) + "[Spot Instances] Available site memory: " + availableMem + "MB");                
                logger.info(Lager.ev(-1) + "[Spot Instances] Used non pre-emptable memory: " + usedNonPreemptableMem + "MB");
                logger.info(Lager.ev(-1) + "[Spot Instances] Reserved non pre-emptable memory: " + reservedNonPreempMem + "MB");
                logger.info(Lager.ev(-1) + "[Spot Instances] Used pre-emptable memory: " + usedPreemptableMem + "MB");                
                logger.info(Lager.ev(-1) + "[Spot Instances] Calculated memory for SI requests: " + siMem + "MB");
            }            
            
            resourceQuantity = siMem/INSTANCE_MEM;
            
        } catch (WorkspaceDatabaseException e) {
            e.printStackTrace();
        }
        
        if (this.lager.eventLog) {
            logger.info(Lager.ev(-1) + "[Spot Instances] Available basic SI instances (128MB RAM): " + resourceQuantity);
        }        

        setAvailableResources(resourceQuantity);
    }

    protected void setAvailableResources(Integer resourceQuantity){
        if(resourceQuantity != availableResources){
            if (this.lager.eventLog) {
                logger.info(Lager.ev(-1) + "[Spot Instances] RESOURCE QUANTITY CHANGED. OLD AVAILABLE RESOURCES = " + availableResources + ". NEW AVAILABLE RESOURCES = " + resourceQuantity);
            }            
            availableResources = resourceQuantity;
            changePriceAndReallocateRequests();
        }
    }    
    
    // -------------------------------------------------------------------------
    // Implements org.globus.workspace.StateChangeInterested
    // -------------------------------------------------------------------------    
    
    public void stateNotification(int vmid, int state) throws ManageException {
        if(state == WorkspaceConstants.STATE_DESTROYING){
            SIRequest siRequest = this.getSIRequest(vmid);
            if(siRequest != null){
                if (this.lager.eventLog) {
                    logger.info(Lager.ev(-1) + "[Spot Instances] VM '" + vmid + "' from request '" + siRequest.getId() + "' finished. Changing price and reallocating requests.");
                }                
                siRequest.finishVM(vmid, this.currentPrice);
                this.changePriceAndReallocateRequests();
            } else {
                if (this.lager.eventLog) {
                    logger.info(Lager.ev(-1) + "[Spot Instances] A non-preemptable VM was destroyed. Recalculating available resources.");
                }
                this.calculateAvailableResources();
            }
        }
    }    
    
    public void stateNotification(int[] vmids, int state) {
        //assume just non-preemptable VM's are being notified here 
        if(state == WorkspaceConstants.STATE_FIRST_LEGAL){
            if (this.lager.eventLog) {
                logger.info(Lager.ev(-1) + "[Spot Instances] " + vmids.length + " non-preemptable VMs created. Recalculating available resources.");
            }            
            this.calculateAvailableResources();
        }
    }
    
    // -------------------------------------------------------------------------
    // UTILS
    // -------------------------------------------------------------------------  
    
    private List<SIRequest> getEqualBidHungryRequests() {
        return SIRequestUtils.filterHungryAliveRequestsEqualPrice(this.currentPrice, this.allRequests.values());
    }
    
    protected Collection<SIRequest> getLowerBidActiveRequests() {
        return SIRequestUtils.filterActiveRequestsBelowPrice(this.currentPrice, this.allRequests.values());
    }

    protected List<SIRequest> getEqualBidActiveRequests(){
        return SIRequestUtils.filterActiveRequestsEqualPrice(this.currentPrice, this.allRequests.values());
    }

    protected Collection<SIRequest> getEqualBidOpenRequests() {
        return SIRequestUtils.filterOpenRequestsEqualPrice(this.currentPrice, this.allRequests.values());
    }    
    
    protected Collection<SIRequest> getHigherBidAliveRequests() {
        return SIRequestUtils.filterAliveRequestsAbovePrice(this.currentPrice, this.allRequests.values());
    }
    
    protected Collection<SIRequest> getAliveRequests() {
        return SIRequestUtils.filterAliveRequests(this.allRequests.values());
    }    

    protected Integer getGreaterBidResourcesCount() {
        Collection<SIRequest> priorityRequests = getHigherBidAliveRequests();

        Integer resourceCount = 0;

        for (SIRequest siRequest : priorityRequests) {
            resourceCount += siRequest.getNeededInstances();
        }

        return resourceCount;
    }    
    
    protected SIRequest getSIRequest(int vmid) {
        for (SIRequest request : allRequests.values()) {
            if(request.isAllocatedVM(vmid)){
                return request;
            }
        }
        
        return null;
    }    
    
    // -------------------------------------------------------------------------
    // MODULE SET (avoids circular dependency problem)
    // -------------------------------------------------------------------------
    public void setCreationManager(InternalCreationManager creationManagerImpl) {
        if (creationManagerImpl == null) {
            throw new IllegalArgumentException("creationManagerImpl may not be null");
        }
        this.creationManager = creationManagerImpl;
    }

    // -------------------------------------------------------------------------
    // TEST UTILS
    // -------------------------------------------------------------------------
    
    public Integer getAvailableResources() {
        return availableResources;
    }
}
