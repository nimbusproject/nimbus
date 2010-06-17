package org.globus.workspace.spotinstances;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.workspace.Lager;
import org.globus.workspace.WorkspaceConstants;
import org.globus.workspace.persistence.PersistenceAdapter;
import org.globus.workspace.persistence.WorkspaceDatabaseException;
import org.globus.workspace.service.binding.vm.VirtualMachine;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.si.SIConstants;
import org.nimbustools.api.services.rm.DoesNotExistException;
import org.nimbustools.api.services.rm.ManageException;

public class SpotInstancesManagerImpl implements SpotInstancesManager {

    private static String MACHINE_TYPE = SIConstants.SI_TYPE_BASIC;
    private static Integer INSTANCE_MEM = SIConstants.getInstanceMem(MACHINE_TYPE);
    
    private static final Integer MINIMUM_RESERVED_MEMORY = 128;
    private static final Double MAX_NON_PREEMP_UTILIZATION = 0.7;

    private static final Log logger =
        LogFactory.getLog(SpotInstancesManagerImpl.class.getName());
    
    protected Integer availableResources;

    protected Map<String, SIRequest> allRequests;

    protected PricingModel pricingModel;

    protected Double currentPrice;

    protected Lager lager;

    private PersistenceAdapter persistence;

    public SpotInstancesManagerImpl(PersistenceAdapter persistenceAdapter, Lager lager){
        this.allRequests = new HashMap<String, SIRequest>();
        this.currentPrice = PricingModelConstants.MINIMUM_PRICE;
        this.pricingModel = new MaximizeUtilizationPricingModel();
        this.persistence = persistenceAdapter;         
        this.lager = lager;
        this.availableResources = 0;
    }
    
    // Implements org.globus.workspace.spotinstances.SpotInstancesHome
    
    public void addRequest(SIRequest request){
        allRequests.put(request.getId(), request);
        if (this.lager.eventLog) {
            logger.info(Lager.ev(-1) + "[Spot Instances] REQUEST ARRIVED: " + request.toString() + ". Changing price and reallocating requests.");
        }        
        changePriceAndReallocateRequests();
    }
    
    public SIRequest getRequest(String id) throws DoesNotExistException {
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
    }

    // Allocation    

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
            changeAllocation(inelegibleRequest, 0);
        }
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
                if(activeRequest.needsMoreInstances()){
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
            Integer unallocatedInstances = partialReq.getUnallocatedInstances();

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

    protected void changeAllocation(SIRequest siRequest, Integer newAllocation) {

        
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

        siRequest.addCreatedVMs(new int[newAllocation]);

        if (this.lager.eventLog) {
            logger.info(Lager.ev(-1) + "[Spot Instances] CHANGING ALLOCATION - AFTER: " + siRequest.toString());
        }
    }    

    protected void preempt(SIRequest siRequest, int delta) {
        int[] allocatedVMs = null;
        try {
            allocatedVMs = siRequest.getAllocatedVMs(delta);
        } catch (SIRequestException e) {
            logger.fatal("[Spot Instances] " + e.getMessage(), e);
            return;
        }
        
        //TODO: temporary, just for testing
        for (int i = 0; i < allocatedVMs.length; i++) {
            siRequest.fulfillVM(allocatedVMs[i], this.currentPrice);
        }
    }

    protected void allocate(SIRequest siRequest, Integer quantity) {
        VirtualMachine[] unallocatedVMs = null;
        try {
            unallocatedVMs = siRequest.getUnallocatedVMs(quantity);
        } catch (SIRequestException e) {
            logger.fatal("[Spot Instances] " + e.getMessage(), e);
            return;
        }
        
        //TODO: temporary, just for testing
        int[] fakeIDs = new int[quantity];
        for (int i = 0; i < quantity; i++) {
            fakeIDs[i] = (int)(Math.random()*10000);
            unallocatedVMs[i].setID(fakeIDs[i]);
        }
        siRequest.addCreatedVMs(fakeIDs);
    }

    //Define available resources
    
    protected synchronized void calculateAvailableResources() {
        
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
                    logger.info(Lager.ev(-1) + "[Spot Instances] Not enough available memory for Spot Instances. Trying to satisfy currently active requests.");
                }                
                siMem = Math.max(usedPreemptableMem-reservedNonPreempMem, 0);
            }
            
            if (this.lager.eventLog) {
                logger.info(Lager.ev(-1) + "[Spot Instances] Maximum site memory: " + maxMem + "MB");
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
    
    // Util
    
    protected Collection<SIRequest> getLowerBidActiveRequests() {
        return SIRequestUtils.filterActiveRequestsBelowPrice(this.currentPrice, this.allRequests.values());
    }

    protected Collection<SIRequest> getEqualBidActiveRequests(){
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
    
    // Implements org.globus.workspace.StateChangeInterested

    public void stateNotification(int vmid, int state) throws ManageException {
        if(state == WorkspaceConstants.STATE_DESTROYING){
            SIRequest siRequest = this.getSIRequest(vmid);
            if(siRequest != null){
                if (this.lager.eventLog) {
                    logger.info(Lager.ev(-1) + "[Spot Instances] VM '" + vmid + "' from request '" + siRequest.getId() + "' finished. Changing price and reallocating requests.");
                }                
                siRequest.fulfillVM(vmid, this.currentPrice);
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
}
