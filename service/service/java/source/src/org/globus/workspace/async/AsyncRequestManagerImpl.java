/*
 * Copyright 1999-2010 University of Chicago
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.globus.workspace.async;

import java.util.ArrayList;
import java.util.Calendar;
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
import org.globus.workspace.async.pricingmodel.PricingModel;
import org.globus.workspace.creation.InternalCreationManager;
import org.globus.workspace.persistence.PersistenceAdapter;
import org.globus.workspace.persistence.WorkspaceDatabaseException;
import org.globus.workspace.scheduler.defaults.PreemptableSpaceManager;
import org.globus.workspace.scheduler.defaults.SlotManagement;
import org.globus.workspace.service.InstanceResource;
import org.globus.workspace.service.WorkspaceGroupHome;
import org.globus.workspace.service.WorkspaceHome;
import org.globus.workspace.service.binding.vm.VirtualMachine;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.SpotPriceEntry;
import org.nimbustools.api.services.rm.DoesNotExistException;
import org.nimbustools.api.services.rm.ManageException;
import org.nimbustools.api.services.rm.ResourceRequestDeniedException;

public class AsyncRequestManagerImpl implements AsyncRequestManager {
    
    //TODO: Set by Spring IoC
    private Integer minReservedMem;
    private Double maxUtilization;
    private Integer instanceMem;

    // If both of these are false, this module will do nothing at all.
    
    // True if non-superuser requests are honored (SI)
    protected final boolean remoteEnabled;
    // True if superuser requests (backfill) are honored, can be entirely disabled.
    protected final boolean backfillEnabled;

    private static final Log logger =
        LogFactory.getLog(AsyncRequestManagerImpl.class.getName());
    
    protected Integer maxVMs;

    protected AsyncRequestMap asyncRequestMap;
    protected PricingModel pricingModel;

    protected Double currentPrice;

    protected final Lager lager;
    protected final PersistenceAdapter persistence;
    protected final WorkspaceHome home;
    protected final WorkspaceGroupHome ghome;
    
    protected InternalCreationManager creationManager;
    private Double minPrice;    

    public AsyncRequestManagerImpl(PersistenceAdapter persistenceAdapterImpl,
                                    Lager lagerImpl,
                                    WorkspaceHome instanceHome,
                                    WorkspaceGroupHome groupHome,
                                    Double minPrice,
                                    PricingModel pricingModelImpl,
                                    AsyncRequestMap asyncRequestMap,
                                    boolean remoteEnabled,
                                    boolean backfillEnabled){

        this.maxVMs = 0;
   
        if (persistenceAdapterImpl == null) {
            throw new IllegalArgumentException("persistenceAdapterImpl may not be null");
        }
        this.persistence = persistenceAdapterImpl;        
       
        if (pricingModelImpl == null) {
            throw new IllegalArgumentException("pricingModelImpl may not be null");
        }
        this.pricingModel = pricingModelImpl; 
        
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
        
        if (minPrice == null){
            throw new IllegalArgumentException("minPrice may not be null");
        }
        this.minPrice = minPrice;

        if (asyncRequestMap == null) {
            throw new IllegalArgumentException("asyncRequestMap is missing");
        }
        this.asyncRequestMap = asyncRequestMap;
        
        Double previousSpotPrice = null;
        try {
            previousSpotPrice = this.persistence.getLastSpotPrice();
        } catch (WorkspaceDatabaseException e) {
            logger.warn("Error while retrieving previous spot price from database.",e);
        }
        
        if(previousSpotPrice != null){
            logger.info("Loading previous spot price: " + previousSpotPrice);                        
            this.currentPrice = previousSpotPrice;
        } else {
            logger.info("Setting first spot price: " + minPrice);
            setPrice(minPrice);
        }

        this.pricingModel.setMinPrice(minPrice);

        this.remoteEnabled = remoteEnabled;
        this.backfillEnabled = backfillEnabled;
    }
    
    // -------------------------------------------------------------------------
    // Implements org.globus.workspace.async.AsyncRequestManager
    // -------------------------------------------------------------------------         
    
    /**
     * Adds an asynchronous request to this manager
     * @param request the request to be added
     * @throws ResourceRequestDeniedException If this type of request is disabled
     */
    public void addRequest(AsyncRequest request) throws ResourceRequestDeniedException {

        this.asyncRequestMap.addOrReplace(request);

        if(request.isSpotRequest()){
            if (!this.remoteEnabled) {
                throw new ResourceRequestDeniedException("Spot instances are disabled");
            }

            if(this.lager.eventLog){
                logger.info(Lager.ev(-1) + "Spot Instance request arrived: " + request.toString() + ". Changing price and reallocating requests.");
            }
            changePriceAndAllocateRequests();
        } else {
            if (!this.backfillEnabled) {
                throw new ResourceRequestDeniedException("Backfill is disabled");
            }
            if(this.lager.eventLog){
                logger.info(Lager.ev(-1) + "Backfill request arrived: " + request.toString() + ".");
            }
            allocateBackfillRequests();
        }
    }
    
    // -------------------------------------------------------------------------
    // Implements org.globus.workspace.async.AsyncRequestHome
    // -------------------------------------------------------------------------     

    /**
     * Cancels an asynchronous request
     * @param reqID the id of the request to be canceled
     * @return the canceled request
     * @throws DoesNotExistException in case the id argument does not map
     *                               to any asynchronous request
     */    
    public AsyncRequest cancelRequest(String reqID) throws DoesNotExistException {
        return this.cancelRequest(reqID, true);
    }

    private AsyncRequest cancelRequest(String reqID, boolean recalculate) throws DoesNotExistException {
        logger.info(Lager.ev(-1) + "Cancelling request with id: " + reqID + ".");                
        AsyncRequest request = getRequest(reqID, false);

        AsyncRequestStatus prevStatus = request.getStatus();
        changeStatus(request, AsyncRequestStatus.CANCELLED);
        
        // From http://docs.amazonwebservices.com/AWSEC2/latest/APIReference/ApiReference-soap-CancelSpotInstanceRequests.html
        // Canceling a Spot Instance request does not terminate running Spot Instances
        // associated with the request.
        // But, with backfill we want to withdraw, so the async manager will do that for these
        // requests (that are marked backfill by "isSpotRequest()" being false.

        if(!request.isSpotRequest()) {
            if (prevStatus.isActive()) {
                preempt(request, request.getAllocatedInstances());
                if (recalculate) {
                    allocateBackfillRequests();
                }
            }
        }
        
        return request;
    }

    public AsyncRequest[] cancelRequests(String[] reqID) throws DoesNotExistException {
        if (reqID == null || reqID.length == 0) {
            return new AsyncRequest[0];
        }
        final AsyncRequest[] ret = new AsyncRequest[reqID.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = cancelRequest(reqID[i], false);
        }
        this.allocateBackfillRequests();
        return ret;
    }

    /**
     * Retrieves an asynchronous request and its related information
     * @param id the id of the request to be retrieved
     * @return the wanted request
     * @throws DoesNotExistException in case the id argument does not map
     *                               to any asynchronous request
     */
    public AsyncRequest getRequest(String id) throws DoesNotExistException {
        return this.getRequest(id, true);
    }    

    /**
     * Retrieves all asynchronous requests from a caller
     * @param caller the owner of the asynchronous' requests
     * @return an array of asynchronous requests from this caller
     */   
    public AsyncRequest[] getRequests(Caller caller, boolean spot) {
        logger.info(Lager.ev(-1) + "Retrieving requests from caller: " + caller.getIdentity() + ".");        
        ArrayList<AsyncRequest> requestsByCaller = new ArrayList<AsyncRequest>();
        for (AsyncRequest request : this.asyncRequestMap.getAll()) {
            if(request.isSpotRequest() == spot && request.getCaller().equals(caller)){
                requestsByCaller.add(request);
            }
        }
        return requestsByCaller.toArray(new AsyncRequest[0]);
    }

    /**
     * Retrieves current spot price
     * @return current spot price
     */    
    public Double getSpotPrice() {
        return this.currentPrice;
    }

    /**
     * Retrieves the spot price history
     * @param startDate the date the history should start. <b>null</n>
     * indicates there is no start date.
     * @param endDate the date the history should end. <b>null</n>
     * indicates there is no end date.
     * @return a list of spot price entries from the start date until the end date
     * @throws WorkspaceDatabaseException in case there is an error 
     * in the databsae, while obtaining the history data
     */    
    public List<SpotPriceEntry> getSpotPriceHistory(Calendar startDate, Calendar endDate)
        throws WorkspaceDatabaseException {
        
        return persistence.getSpotPriceHistory(startDate, endDate);
    }     
    
    // -------------------------------------------------------------------------
    // Implements org.globus.workspace.scheduler.defaults.PreemptableSpaceManager
    // -------------------------------------------------------------------------       
    
    /**
     * Initalizes this {@link PreemptableSpaceManager}.
     * 
     * This method is called after the associated
     * {@link SlotManagement} was initialized, indicating
     * that the resource pool DB was already populated and
     * can be queried
     */
    public void init() {
        this.calculateMaxVMs();
    }    
    
    /**
     * Releases the needed amount of space from
     * pre-emptables reservations.
     * 
     * This method is called when the {@link SlotManagement} 
     * doesn't find sufficient space for a non-preemptable 
     * reservation, so it tries to fulfill that request with 
     * space currently allocated to a non-preemptable workspace.
     * 
     * This method should block until the process of
     * releasing the needed memory is completed, what
     * means that who is calling might assume that
     * the needed space is already released by the end
     * of this method's execution.
     * 
     * @param memoryToFree the minimum amount
     * of memory that should be released from
     * pre-emptable reservations. In case this value
     * is higher than the amount of space currently
     * managed by this {@link PreemptableSpaceManager},
     * all the pre-emptable space currently allocated 
     * must be released.
     * 
     */    
    public void releaseSpace(Integer memoryToFree) {
        if (this.lager.eventLog) {
            logger.info(Lager.ev(-1) + "" + memoryToFree + 
                    "MB RAM have to be freed to give space to higher priority requests");
        }
        
        Integer usedMemory = this.getMaxVMs()*instanceMem;

        if(memoryToFree > usedMemory){
            logger.warn(Lager.ev(-1) + "Asynchronous requests are consuming " + usedMemory + 
                    "MB RAM , but AsyncRequestManager was requested to free " + memoryToFree + "MB RAM. " +
                            "Freeing " + usedMemory + "MB RAM.");            
            memoryToFree = usedMemory;
        }

        Integer availableMemory = usedMemory - memoryToFree;
        
        //Since available memory has decreased,
        //this will cause lower bid workspaces 
        //to be pre-empted
        changeMaxVMs(availableMemory); 
    }        
    
    // -------------------------------------------------------------------------
    // Implements org.globus.workspace.StateChangeInterested
    // -------------------------------------------------------------------------    
    
    /**
     * This notification allows modules to be autonomous
     * from the service layer's actions if it wants to be (instead
     * of allowing the resource states to progress, it could time
     * every state transition by continually re-adjusting the
     * resource's target state when it is time to transition it).
     *
     * The first state notification (always preceded by a call to
     * schedule) signals that creation process has finished.  
     * This allows the service layer to finalize creation 
     * before a module (ie. scheduler) can act on a a resouce.
     *
     * @param vmid id
     * @param state STATE_* in WorkspaceConstants
     * @throws ManageException problem
     */
    public void stateNotification(int vmid, int state) throws ManageException {
        if(state == WorkspaceConstants.STATE_DESTROYING){
            AsyncRequest request = this.getRequestFromVM(vmid);
            if(request != null){
                if (this.lager.eventLog) {
                    logger.info(Lager.ev(-1) + "VM '" + vmid + "' from request '" + request.getId() + "' finished.");
                }

                boolean fin = request.finishVM(vmid);
                this.asyncRequestMap.addOrReplace(request);
                if(!fin){
                    if(request.getAllocatedInstances().equals(0)){
                        allVMsFinished(request);
                    }
                    
                    //Will just change price and reallocate requests
                    //if this was not a pre-emption                    
                    if(request.isSpotRequest()){
                        this.changePriceAndAllocateRequests();
                    } else {
                        this.allocateBackfillRequests();
                    }                        
                }
            } else {
                if (this.lager.eventLog) {
                    logger.info(Lager.ev(-1) + "A non-preemptable VM was destroyed. Recalculating maximum instances.");
                }
                this.calculateMaxVMs();
            }
        }
    }    
    
    /**
     * Batch state notification
     * 
     * NOTE: This version doesn't throw exception when
     * an error occurs during the notification. If error 
     * conditions need to be treated, use 
     * {@code stateNotification(int vmid, int state)}
     * instead. However, implementations of this 
     * interface are recommended to log possible errors.
     * 
     * @param vmids ids of vms
     * @param state STATE_* in WorkspaceConstants
     */    
    public void stateNotification(int[] vmids, int state) {
        //assume just non-preemptable VM's are being notified here 
        if(state == WorkspaceConstants.STATE_FIRST_LEGAL){
            if (this.lager.eventLog) {
                logger.info(Lager.ev(-1) + "" + vmids.length + " non-preemptable VMs created. Recalculating maximum instances.");
            }            
            this.calculateMaxVMs();
        }
    }    
    
    // -------------------------------------------------------------------------
    // PRICE SETTING
    // -------------------------------------------------------------------------     
    
    /**
     * Updates the spot price, and
     * allocate/preempts the requests
     * 
     * This method is called every time the
     * number of maximum instances,
     * current requests or allocated instances
     * changes. This happens when:
     * * A SI request is added
     * * The number of maximum instances changes
     * * An SI instance is terminated
     * * A SI Request is canceled
     * 
     */
    protected synchronized void changePriceAndAllocateRequests(){
        changePrice();

        allocateRequests();

        if(this.getMaxVMs() == 0){
            changePrice();
        }
    }

    /**
     * Invokes the associated PricingModel in order
     * to calculate the next price (given current
     * OPEN and ACTIVE requests), and changes the
     * price in case the new price is different
     */
    private void changePrice() {
        Double newPrice = pricingModel.getNextPrice(this.getMaxVMs(), getAliveSpotRequests(), currentPrice);
        if(!newPrice.equals(this.currentPrice)){
            logger.info(Lager.ev(-1) + "Spot price has changed. " +
                    "Previous price = " + this.currentPrice + ". " +
                    "Current price = " + newPrice);
            setPrice(newPrice);
        }
    }

    // -------------------------------------------------------------------------
    // ALLOCATION
    // ------------------------------------------------------------------------ 

    /**
     * Performs a series of allocations
     * and pre-emptions in order to satisfy
     * Spot Price, Maximum VMs and backfill
     * constraints
     */
    protected synchronized void allocateRequests() {
                
        preemptAllocatedLowerBidRequests();
        
        allocateBackfillRequests();
        
        allocateEqualBidRequests();

        allocateHigherBidRequests();
        
    }

    /**
     * Preempts all ACTIVE requests that have bid
     * below the current spot price
     */
    private void preemptAllocatedLowerBidRequests() {
        
        Collection<AsyncRequest> inelegibleRequests = getLowerBidRequests();
        
        if(inelegibleRequests.isEmpty()){
            return;
        }
        
        if(this.lager.eventLog){
            logger.info(Lager.ev(-1) + "Pre-empting " + 
                        inelegibleRequests.size() + " lower bid requests.");
        }
        
        for (AsyncRequest inelegibleRequest : inelegibleRequests) {
            preempt(inelegibleRequest, inelegibleRequest.getAllocatedInstances());
        }
    }    
    
    /**
     * Allocates lower priority requests if there are 
     * available VMs, pre-empt them otherwise
     */
    private void allocateLowerPriorityRequests(Integer higherPriorityVMs,
                                               List<AsyncRequest> aliveRequests,
                                               String requestType) {
        
        Integer availableVMs = Math.max(this.getMaxVMs() - higherPriorityVMs, 0);

        Integer allocatedVMs = 0;
        for (AsyncRequest aliveRequest : aliveRequests) {
            allocatedVMs += aliveRequest.getAllocatedInstances();
        }
        
        if(allocatedVMs == availableVMs){
            return;
        }
        
        if(allocatedVMs > availableVMs){
            Integer needToPreempt = allocatedVMs - availableVMs;
            if (this.lager.eventLog) {
                logger.info(Lager.ev(-1) + "No more resources for " + requestType + " requests. " +
                                           "Pre-empting " + needToPreempt + " VMs.");   
            }
            List<AsyncRequest> activeRequests = getActiveRequests(aliveRequests);
            preemptProportionaly(activeRequests, needToPreempt, allocatedVMs);            
        } else {
            availableVMs -= allocatedVMs;
            
            List<AsyncRequest> hungryRequests = getHungryRequests(aliveRequests);
            
            if(!hungryRequests.isEmpty()){
                allocateEvenly(hungryRequests, availableVMs);
            }            
        }
    }    
    
    /**
     * Allocates all requests that have bid
     * above the current spot price
     */
    private synchronized void allocateHigherBidRequests() {
        
        Collection<AsyncRequest> aliveRequests = getAliveHigherBidRequests();
        
        if(aliveRequests.isEmpty()){
            return;
        }
        
        int count = 0;
        
        for (AsyncRequest aliveRequest : aliveRequests) {
            if(aliveRequest.needsMoreInstances()){
                allocate(aliveRequest, aliveRequest.getUnallocatedInstances());
                count++;
            }
        }
        
        if(count > 0 && this.lager.eventLog){
            logger.info(Lager.ev(-1) + "Allocated " + 
                        count + " higher bid requests.");
        }        
    }

    /**
     * Allocates all requests that have bid
     * equal to the current spot price
     */
    private synchronized void allocateEqualBidRequests() {
        allocateLowerPriorityRequests(getGreaterBidVMCount(), getAliveEqualBidRequests(), "equal bid");
    }

    /**
     * Allocate backfill requests
     */
    private synchronized void allocateBackfillRequests() {
        allocateLowerPriorityRequests(getGreaterOrEqualBidVMCount(), getAliveBackfillRequests(), "backfill");
    }    
    
    /**
     * Allocates requests in a balanced manner.
     * 
     * This means allocating the same number of VMs to each request
     * until all requests are satisfied, or there are no available
     * resources to distribute.
     * 
     * @param availableInstances the number of VMs available
     *                           for allocation
     */
    private void allocateEvenly(List<AsyncRequest> hungryRequests, Integer availableInstances) {
        
        if(availableInstances == 0){
            return;
        }
        
        if(hungryRequests.isEmpty()){
            return;
        } else {
            if (this.lager.eventLog) {
                logger.info(Lager.ev(-1) + "Allocating " + Math.min(availableInstances, hungryRequests.size()) + " requests.");
            }    
        }
        
        Map<AsyncRequest, Integer> allocations = new HashMap<AsyncRequest, Integer>();
        for (AsyncRequest hungryRequest : hungryRequests) {
            allocations.put(hungryRequest, 0);
        }
        
        while(availableInstances > 0 && !hungryRequests.isEmpty()){
            Integer vmsPerRequest = Math.max(availableInstances/hungryRequests.size(), 1);
            
            Iterator<AsyncRequest> iterator = hungryRequests.iterator();
            while(availableInstances > 0 && iterator.hasNext()){
                vmsPerRequest = Math.min(vmsPerRequest, availableInstances);
                
                AsyncRequest request = (AsyncRequest) iterator.next();
                Integer vmsToAllocate = allocations.get(request);
                
                Integer stillNeeded = request.getUnallocatedInstances() - vmsToAllocate;
                if(stillNeeded <= vmsPerRequest){
                    allocations.put(request, vmsToAllocate+stillNeeded);
                    availableInstances -= stillNeeded;
                    iterator.remove();
                    continue;
                }
                
                allocations.put(request, vmsToAllocate+vmsPerRequest);
                availableInstances -= vmsPerRequest;
            }            
        }
        
        for (Entry<AsyncRequest, Integer> allocationEntry : allocations.entrySet()) {
            AsyncRequest request = allocationEntry.getKey();
            allocate(request, allocationEntry.getValue());
        }
    } 
    
    private List<AsyncRequest> getHungryRequests(
            List<AsyncRequest> aliveRequests) {
        
        for (Iterator<AsyncRequest> iterator = aliveRequests.iterator(); iterator.hasNext();) {
            AsyncRequest asyncRequest = iterator.next();
            if(!asyncRequest.needsMoreInstances()){
                iterator.remove();
            }
        }
        
        Collections.sort(aliveRequests, getAllocationComparator());
        
        return aliveRequests;
    }
    
    private List<AsyncRequest> getActiveRequests(
            List<AsyncRequest> aliveRequests) {
        
        for (Iterator<AsyncRequest> iterator = aliveRequests.iterator(); iterator.hasNext();) {
            AsyncRequest asyncRequest = iterator.next();
            if(!asyncRequest.getStatus().isActive()){
                iterator.remove();
            }
        }
        
        Collections.sort(aliveRequests, getPreemptionComparator());
        
        return aliveRequests;
    }    

    /**
     * Pre-empts requests more-or-less proportional 
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
     * Req A: 2 pre-emptions (~33.33%)
     * Req B: 1 pre-emption (~11.11%)
     * Req C: 3 pre-emptions (~55.55%)
     * 
     * @param activeRequests ACTIVE requests with bid equal to the current spot price
     * @param needToPreempt the number of VMs that needs to be pre-empted
     * @param allocatedVMs the number of currently allocated VMs in <b>activeRequests</b>
     */
    private void preemptProportionaly(List<AsyncRequest> activeRequests, Integer needToPreempt, Integer allocatedVMs) {
                
        Integer stillToPreempt = needToPreempt;
        
        Iterator<AsyncRequest> iterator = activeRequests.iterator();
        while(iterator.hasNext() && stillToPreempt > 0){
            AsyncRequest request = iterator.next();
            Double allocatedProportion = (double)request.getAllocatedInstances()/allocatedVMs;

            //Minimum deserved pre-emption is 1
            Integer deservedPreemption = Math.max((int)Math.round(allocatedProportion*needToPreempt), 1);

            Integer realPreemption = Math.min(deservedPreemption, stillToPreempt); 
            preempt(request, realPreemption);
            stillToPreempt -= realPreemption;                
        }
        
        
        //This may never happen. But just in case.
        if(stillToPreempt > 0){
            logger.warn("Unable to pre-empt VMs proportionally. Still " + stillToPreempt + 
                         " VMs to pre-empt. Pre-empting best-effort.");
            
            iterator = activeRequests.iterator();
            while(iterator.hasNext() && stillToPreempt > 0){
                AsyncRequest request = iterator.next();
                Integer allocatedInstances = request.getAllocatedInstances();
                if(allocatedInstances > 0){
                    if(allocatedInstances > stillToPreempt){
                        preempt(request, stillToPreempt);
                        stillToPreempt = 0;
                    } else {
                        preempt(request, allocatedInstances);
                        stillToPreempt -= allocatedInstances;                        
                    }
                }
            }
        }
    }

    /**
     * Creates a AsyncRequest comparator that
     * prioritizes recent requests with more
     * allocated instances to be pre-empted
     * first
     * @return the generated comparator
     */
    private Comparator<AsyncRequest> getPreemptionComparator() {
        return new Comparator<AsyncRequest>() {

            public int compare(AsyncRequest o1, AsyncRequest o2) {
                
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
    
    /**
     * Creates a AsyncRequest comparator that
     * prioritizes older requests with less
     * allocated instances to be allocated
     * first
     * @return the generated comparator
     */    
    private Comparator<AsyncRequest> getAllocationComparator() {
        return new Comparator<AsyncRequest>() {

            public int compare(AsyncRequest o1, AsyncRequest o2) {
                
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

    /**
     * Preempts (ie. destroys) the desired quantity
     * of VMs from a given request
     * @param request the request to be pre-empted
     * @param quantity the quantity to be pre-empted
     */
    protected void preempt(AsyncRequest request, int quantity) { 
                
        if(request.getAllocatedInstances() == quantity){
            allVMsFinished(request);
        }
        
        try{
            if(request.getRequestedInstances() > 1 && !request.isAlive()){
                if (this.lager.eventLog) {
                    logger.info(Lager.ev(-1) + "All VMs from asynchronous request '" + request.getId() + 
                            "' will be destroyed. Destroying group: " + request.getGroupID());
                }
                request.preemptAll();
                this.asyncRequestMap.addOrReplace(request);
                ghome.destroy(request.getGroupID());
            } else {
                int[] preemptionList = request.getAllocatedVMs(quantity);

                if (this.lager.eventLog) {
                    String logStr = Lager.ev(-1) + "Pre-empting following VMs for request " + request.getId() + ": ";
                    for (int i = 0; i < preemptionList.length; i++) {
                        logStr += preemptionList[i] + " ";
                    }
                    logger.info(logStr.trim());
                }

                request.preempt(preemptionList);
                this.asyncRequestMap.addOrReplace(request);

                final String sourceStr = "via async-Manager-preempt, request " +
                "id = '" + request.getId() + "'";
                String errorStr = home.destroyMultiple(preemptionList, sourceStr);
                if(errorStr != null && errorStr.length() != 0){
                    failRequest("pre-empting", request, errorStr, null);
                }
            }            
        } catch(Exception e){
            failRequest("pre-empting", request, e.getMessage(), e);
        }
        
    }

    /**
     * Trigger a status change after
     * all VMs from a given request are finished
     * @param request
     */
    private void allVMsFinished(AsyncRequest request){
        if(!request.isPersistent() && (!request.needsMoreInstances() || currentPrice > request.getMaxBid())){
            changeStatus(request, AsyncRequestStatus.CLOSED);
        } else {
            changeStatus(request, AsyncRequestStatus.OPEN);
        }
    }
    
    /**
     * Changes the status of a given request to FAILED,
     * and sets the cause of the problem
     * @param action the action that caused the request to fail (log purposes)
     * @param request the request that has failed
     * @param errorStr the error message
     * @param problem the problem that caused the request to fail
     */
    private void failRequest(String action, AsyncRequest request, String errorStr, Throwable problem) {
        logger.warn(Lager.ev(-1) + "Error while " + action + " VMs for request: " +
                request.getId() + ". Setting state to FAILED. Problem: " +
                errorStr);
        changeStatus(request, AsyncRequestStatus.FAILED);
        if(problem != null){
            request.setProblem(problem);
            this.asyncRequestMap.addOrReplace(request);
        }
    }

    /**
     * Allocates the desired quantity
     * of VMs to a given request
     * @param request the request to be pre-empted
     * @param quantity the quantity to be pre-empted
     */
    protected void allocate(AsyncRequest request, Integer quantity) {

        if(quantity < 1){
            logger.error(Lager.ev(-1) + "Number of instances to allocate has to be larger than 0. " +
            		                    "Requested quantity: " + quantity);
            return;
        }

        if (this.lager.eventLog) {
            logger.info(Lager.ev(-1) + "Allocating " + quantity + " VMs for request: " + request.getId());
        }

        VirtualMachine[] unallocatedVMs = null;
        try {
            unallocatedVMs = request.getUnallocatedVMs(quantity);
        } catch (AsyncRequestException e) {
            logger.fatal("" + e.getMessage(), e);
            return;
        }
        
        try {
            double chargeRatio = request.getMaxBid();
            if (chargeRatio < 0) {
                chargeRatio = 0;
            } else if (chargeRatio > 1.0) {
                chargeRatio = 1.0;
            }
            InstanceResource[] createdVMs =
                    creationManager.createVMs(unallocatedVMs, request.getRequestedNics(),
                                              request.getCaller(), request.getContext(),
                                              request.getGroupID(), null, null, true,
                                              chargeRatio);
            for (InstanceResource resource : createdVMs) {
                request.addAllocatedVM(resource.getID());
                this.asyncRequestMap.addOrReplace(request);
            }
        } catch (Exception e) {
            failRequest("allocating", request, e.getMessage(), e);
            return;
        }
                
        if(request.getStatus().isOpen()){
            changeStatus(request, AsyncRequestStatus.ACTIVE);
        }
    }

    /**
     * Changes the status of an asynchronous request
     * @param request the request that will change status
     * @param newStatus the new status
     */
    private void changeStatus(AsyncRequest request, AsyncRequestStatus newStatus) {
        AsyncRequestStatus oldStatus = request.getStatus();
        boolean changed = request.setStatus(newStatus);
        this.asyncRequestMap.addOrReplace(request);
        if (changed && this.lager.eventLog) {
            logger.info(Lager.ev(-1) + "Request " + request.getId() + " changed status from " + oldStatus + " to " + newStatus);
        }
    }

    // -------------------------------------------------------------------------
    // DEFINE ASYNCHRONOUS REQUEST CAPACITY
    // -------------------------------------------------------------------------


    public void recalculateAvailableInstances() {
        if (!this.backfillEnabled && !this.remoteEnabled) {
            return;
        }
        this.calculateMaxVMs();
    }

    /**
     * Calculates the maximum number of instances
     * the Asynchronous Request module can allocate
     * 
     * The amount of memory available for SI and backfill 
     * requests will depend on the free reserved capacity
     * for non-preemptable reservations, that is based
     * on non-preemptable resources' utilization.
     * For this reason, every time the utilization of
     * non-preemptable resources change this method
     * must be called:
     * 
     *  * Initialization
     *  * Creation of non-preemptable VMs
     *  * Destructions of non-preemptable VMs
     * 
     */
    protected synchronized void calculateMaxVMs() {
        
        logger.debug("Going to calculate maximum VMs for SI and backfill requests");

        Integer siMem = 0;
        Integer availableMem = 0;
        Integer usedPreemptableMem = 0;
        Integer usedNonPreemptableMem = 0;
        
        try {
            Integer totalMaxMemory = persistence.getTotalMaxMemory();
            if (totalMaxMemory > 0) {
                availableMem = persistence.getTotalAvailableMemory(instanceMem);
                usedPreemptableMem = persistence.getTotalPreemptableMemory();
                usedNonPreemptableMem = persistence.getUsedNonPreemptableMemory();
            }
            
            //Formula derived from maximum_utilization =       usedNonPreemptable
            //                                           -----------------------------------------
            //                                           usedNonPreemptable + reservedNonPreempMem
            Integer reservedNonPreempMem = (int)Math.round((1-maxUtilization)*usedNonPreemptableMem/maxUtilization);
            reservedNonPreempMem = Math.max(reservedNonPreempMem, minReservedMem);
            
            siMem = Math.max((availableMem+usedPreemptableMem)-reservedNonPreempMem, 0);
            
            if (this.lager.eventLog) {
                final StringBuilder sb =
                        new StringBuilder("Spot and backfill memory:\n");
                sb.append("Site memory - Total: " + totalMaxMemory + "MB\n");
                sb.append("              Available: " + availableMem + "MB\n");
                sb.append("Non-preemptable memory - Used: " + usedNonPreemptableMem + "MB\n");
                sb.append("                         Reserved: " + reservedNonPreempMem + "MB\n");
                sb.append("Preemtable memory - Used: " + usedPreemptableMem + "MB\n");
                sb.append("                    Unused: " + siMem + "MB\n");

                logger.info(Lager.ev(-1) + sb.toString());
            }
        } catch (WorkspaceDatabaseException e) {
            changeMaxVMs(0);
            logger.error(Lager.ev(-1) + "Error while calculating maximum instances: " + e.getMessage());
            return;
        }
        
        changeMaxVMs(siMem);
    }

    /**
     * Changes the maximum allowed number of SI and backfill instances.
     * In case the maximum number changes, the
     * {@code changePriceAndAllocateRequests()} method
     * is called
     * @param availableMemory the new amount of memory
     * available for SI and backfill requests
     */
    protected synchronized void changeMaxVMs(Integer availableMemory){

        if(availableMemory == null || availableMemory < 0){
            return;
        }

        Integer newMaxVMs = availableMemory/instanceMem;

        //TODO Also take available network associations 
        //     into account        
        
        if(newMaxVMs != maxVMs){
            if (this.lager.eventLog) {
                logger.info(Lager.ev(-1) + "Maximum instances changed. Previous maximum instances = " + maxVMs 
                                         + ". Current maximum instances = " + newMaxVMs + ".");
            }            
            this.maxVMs = newMaxVMs;
            changePriceAndAllocateRequests();
        }
    }    
    
    // -------------------------------------------------------------------------
    // UTILS - Candidates for moving down to SQL
    // -------------------------------------------------------------------------  
    
    private void setPrice(Double newPrice) {
        this.currentPrice = newPrice;
        try {
            persistence.addSpotPriceHistory(Calendar.getInstance(), newPrice);
        } catch (WorkspaceDatabaseException e) {
            logger.error(Lager.ev(-1) + "Error while persisting " +
                                        "spot price history: " + e.getMessage());
            return;
        }
    }    
    
    /**
     * Retrieves a Spot Instance request and its related information
     * @param id the id of the request to be retrieved
     * @param log wether the retrieval is logged or not
     * @return the wanted request
     * @throws DoesNotExistException in case the id argument does not map
     *                               to any asynchronous request
     */
    protected AsyncRequest getRequest(String id, boolean log) throws DoesNotExistException {
        if(log){
            logger.info(Lager.ev(-1) + "Retrieving request with id: " + id + ".");
        } 
        AsyncRequest request = this.asyncRequestMap.getByID(id);
        if(request != null){
            return request;
        } else {
            throw new DoesNotExistException("Asynchronous request with id " + id + " does not exist.");
        }
    }    
    
    /**
     * Retrieves the Asynchronous request associated with
     * this Virtual Machine ID
     * @param vmid the id of the vm 
     * @return the request that has this VM allocated
     */
    public AsyncRequest getRequestFromVM(int vmid) {
        for (AsyncRequest request : this.asyncRequestMap.getAll()) {
            if(request.isAllocatedVM(vmid)){
                return request;
            }
        }
        
        return null;
    }         
    
    /**
     * Retrieves ACTIVE or OPEN equal or higher bid requests
     * @return list of alive equal or higher bid requests
     */    
    private List<AsyncRequest> getAliveEqualOrHigherBidRequests() {
        return AsyncRequestFilter.filterAliveRequestsAboveOrEqualPrice(this.currentPrice,
                                                                       this.asyncRequestMap.getAll());
    }        
    
    /**
     * Retrieves ACTIVE or OPEN equal bid requests
     * @return list of alive equal bid requests
     */
    private List<AsyncRequest> getAliveEqualBidRequests(){
        return AsyncRequestFilter.filterAliveRequestsEqualPrice(this.currentPrice,
                                                                this.asyncRequestMap.getAll());
    }  
    
    /**
     * Retrieves ACTIVE or OPEN higher bid requests
     * @return list of alive higher bid requests
     */
    private List<AsyncRequest> getAliveHigherBidRequests() {
        return AsyncRequestFilter.filterAliveRequestsAbovePrice(this.currentPrice,
                                                                this.asyncRequestMap.getAll());
    }
    
    /**
     * Retrieves ACTIVE or OPEN backfill requests
     * @return list of alive backfill requests
     */
    private List<AsyncRequest> getAliveBackfillRequests(){
        return AsyncRequestFilter.filterAliveBackfillRequests(this.asyncRequestMap.getAll());
    }     
    
    /**
     * Retrieves alive spot instance requests
     * @return list of alive requests
     */
    private List<AsyncRequest> getAliveSpotRequests() {
        return AsyncRequestFilter.filterAliveRequestsAboveOrEqualPrice(minPrice,
                                                                       this.asyncRequestMap.getAll());
    }
    
    /**
     * Retrieves allocated lower bid requests
     * @return list of lower bid active requests
     */
    private List<AsyncRequest> getLowerBidRequests() {
        return AsyncRequestFilter.filterAllocatedRequestsBelowPrice(this.currentPrice,
                                                                    this.asyncRequestMap.getAll());
    }    
    
    /**
     * Retrieves the number of needed VMs by greater bid requests
     * @return number of needed VMs
     */
    protected Integer getGreaterBidVMCount() {
        Collection<AsyncRequest> priorityRequests = getAliveHigherBidRequests();

        Integer instanceCount = 0;

        for (AsyncRequest request : priorityRequests) {
            instanceCount += request.getNeededInstances();
        }

        return instanceCount;
    } 
    
    /**
     * Retrieves the number of needed VMs by greater or equal bid requests
     * @return number of needed VMs
     */
    protected Integer getGreaterOrEqualBidVMCount() {
        Collection<AsyncRequest> elegibleRequests = getAliveEqualOrHigherBidRequests();

        Integer instanceCount = 0;

        for (AsyncRequest request : elegibleRequests) {
            instanceCount += request.getNeededInstances();
        }

        return instanceCount;
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
    // Spring IoC setters
    // -------------------------------------------------------------------------    
    
    public void setMinReservedMem(Integer minReservedMem) {
        this.minReservedMem = minReservedMem;
    }

    public void setMaxUtilization(Double maxUtilization) {
        this.maxUtilization = maxUtilization;
    }

    public void setInstanceMem(Integer instanceMem) {
        this.instanceMem = instanceMem;
    }    

    // -------------------------------------------------------------------------
    // GETTERS
    // -------------------------------------------------------------------------   
    
    public synchronized Integer getMaxVMs() {
        return maxVMs;
    }

}
