/*
 * Copyright 1999-2008 University of Chicago
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

package org.globus.workspace.persistence;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.List;

import org.globus.workspace.async.AsyncRequest;
import org.globus.workspace.async.backfill.Backfill;
import org.globus.workspace.creation.IdempotentReservation;
import org.globus.workspace.network.AssociationEntry;
import org.globus.workspace.scheduler.defaults.ResourcepoolEntry;
import org.globus.workspace.service.CoschedResource;
import org.globus.workspace.service.GroupResource;
import org.globus.workspace.service.InstanceResource;
import org.nimbustools.api.repr.SpotPriceEntry;
import org.globus.workspace.service.binding.vm.FileCopyNeed;
import org.nimbustools.api.services.rm.DoesNotExistException;

/**
 * TODO: each module implementation needs to encapsulate its own persistence,
 *       the persistence package will go away entirely
 */
public interface PersistenceAdapter {

    public void setState(int id, int state, Throwable t)

            throws WorkspaceDatabaseException;

    public void setTargetState(int id, int targetState)

            throws WorkspaceDatabaseException;

    public void setOpsEnabled(int id, boolean enabled)

            throws WorkspaceDatabaseException;

    public void setNetwork(int id, String network)

            throws WorkspaceDatabaseException;

    public void setVMMaccessOK(int resourceID, boolean vmmAccessOK)

            throws WorkspaceDatabaseException;

    public void setHostname(int id, String hostname)

            throws WorkspaceDatabaseException;

    public void setRootUnpropTarget(int id, String path)

            throws WorkspaceDatabaseException;

    public void setFileCopyOnImage(int id, FileCopyNeed need)

            throws WorkspaceDatabaseException;

    public void setStartTime(int id, Calendar startTime)

            throws WorkspaceDatabaseException;

    public void setTerminationTime(int id, Calendar termTime)

            throws WorkspaceDatabaseException;

    public void remove(int id, InstanceResource resource)

            throws WorkspaceDatabaseException;

    public void removeGroup(String id)

            throws WorkspaceDatabaseException;

    public void removeEnsemble(String id)

            throws WorkspaceDatabaseException;

    public int[] findActiveWorkspacesIDs()

            throws WorkspaceDatabaseException;

    public boolean isActiveWorkspaceID(int id)

            throws WorkspaceDatabaseException;

    public int[] findVMsInGroup(String groupID)

            throws WorkspaceDatabaseException;

    public int[] findVMsInEnsemble(String ensembleID)

            throws WorkspaceDatabaseException;

    public int[] findVMsByOwner(String ownerID)

            throws WorkspaceDatabaseException;

    public void add(InstanceResource resource)

            throws WorkspaceDatabaseException;

    public void addGroup(GroupResource resource)

            throws WorkspaceDatabaseException;

    public void addEnsemble(CoschedResource resource)

            throws WorkspaceDatabaseException;

    public void load(int id, InstanceResource resource)

            throws DoesNotExistException,
                   WorkspaceDatabaseException;

    public void loadGroup(String id, GroupResource resource)

            throws DoesNotExistException,
                   WorkspaceDatabaseException;

    public void loadEnsemble(String id, CoschedResource resource)

            throws DoesNotExistException,
                   WorkspaceDatabaseException;

    public void replaceAssocations(Hashtable associations)

            throws WorkspaceDatabaseException;

    public void replaceAssociationEntry(String name,
                                        AssociationEntry entry)

            throws WorkspaceDatabaseException;

    public Hashtable currentAssociations()

            throws WorkspaceDatabaseException;

    public Hashtable currentAssociations(boolean cachedIsFine)

            throws WorkspaceDatabaseException;

    public void updateResourcepoolEntryAvailableMemory(String hostname,
                                                       int newAvailMemory, 
                                                       int preemptibleMemory)

            throws WorkspaceDatabaseException;

    public int memoryUsedOnPoolnode(String poolnode)

            throws WorkspaceDatabaseException;

    public List<ResourcepoolEntry> currentResourcepoolEntries() throws WorkspaceDatabaseException;

    public ResourcepoolEntry getResourcepoolEntry(String hostname)

            throws WorkspaceDatabaseException;

    public void addResourcepoolEntry(ResourcepoolEntry entry)

            throws WorkspaceDatabaseException;

    public boolean removeResourcepoolEntry(String hostname)

            throws WorkspaceDatabaseException;

    public void addCustomizationNeed(int id, FileCopyNeed need)

            throws WorkspaceDatabaseException;

    public int readPropagationCounter()

            throws WorkspaceDatabaseException;

    public long currentCursorPosition()

            throws WorkspaceDatabaseException;

    public void updatePropagationCounter(int n)

            throws WorkspaceDatabaseException;

    public void updateCursorPosition(long currentPosition)

            throws WorkspaceDatabaseException;
    
    //SQL processing
    
    public List<ResourcepoolEntry> getAvailableEntriesSortedByFreeMemoryPercentage(int requestedMem, String resourcePool)
    
            throws WorkspaceDatabaseException;

    public String[] getResourcePools()  throws WorkspaceDatabaseException;

    // returns true if memory request is bigger than any VMM could ever handle
    public boolean isInfeasibleRequest(int requestedMem) throws WorkspaceDatabaseException;
    
    //Spot Instances
    
    public Integer getTotalMaxMemory()
    
            throws WorkspaceDatabaseException;
    
    public Integer getTotalAvailableMemory()
    
            throws WorkspaceDatabaseException;    
    
    /**
     * Gets the total available memory as
     * a sum of integer available chunks from 
     * each  resource pool entry
     * 
     * This is useful for knowing the exact
     * amount of memory that is readily available
     * for allocations of that chunk size (ie. 128MB),
     * and not incurring the risk of having
     * 64MB in one VMM and 64MB in another,
     * what will not suffice to allocate a 128MB
     * VM (although 128MB are theoretically
     * available)
     * 
     * @param multipleOf size of the chunk
     * @return the total available memory as a 
     * multiple of the chunk size (ie: result%multipleOf = 0)
     * @throws WorkspaceDatabaseException DB error
     */
    public Integer getTotalAvailableMemory(Integer multipleOf)
    
            throws WorkspaceDatabaseException;
    
    public Integer getTotalPreemptableMemory()
    
            throws WorkspaceDatabaseException;   
    
    public Integer getUsedNonPreemptableMemory()
    
            throws WorkspaceDatabaseException;

    public void addSpotPriceHistory(Calendar timeStamp, 
                                  Double newPrice)
    
            throws WorkspaceDatabaseException;

    public List<SpotPriceEntry> getSpotPriceHistory(Calendar startDate,
                                                    Calendar endDate)
                                                  
            throws WorkspaceDatabaseException;
    
    public Double getLastSpotPrice()
            throws WorkspaceDatabaseException;

    boolean updateResourcepoolEntry(String hostname,
                                        String pool,
                                        String networks,
                                        Integer memoryMax,
                                        Integer memoryAvail,
                                        Boolean active)
            throws WorkspaceDatabaseException;


    /**
     * Returns stored backfill settings from previous launch or null if there was nothing
     * stored (for example in a clean install).
     * 
     * @return stored settings or null
     * @throws WorkspaceDatabaseException DB error
     */
    public Backfill getStoredBackfill() throws WorkspaceDatabaseException;

    /**
     * @param backfill current backfill settings, may not be null
     * @throws WorkspaceDatabaseException
     */
    public void setBackfill(Backfill backfill) throws WorkspaceDatabaseException;

    /**
     * Retrieves idempotency reservation
     * @param creatorId initiating client user
     * @param clientToken client-provided idempotency token
     * @return stored reservation, or null of not found
     * @throws WorkspaceDatabaseException DB error
     */
    public IdempotentReservation getIdempotentReservation(String creatorId, String clientToken)
        throws WorkspaceDatabaseException;


    /**
     * Stores idempotency reservation
     * @param reservation the reservation to store
     * @throws WorkspaceDatabaseException DB error
     */
    public void addIdempotentReservation(IdempotentReservation reservation)
        throws WorkspaceDatabaseException;

    /**
     * Removes existing idempotency reservation
     * @param creatorId initiating client user
     * @param clientToken client-provided idempotency token
     * @throws WorkspaceDatabaseException DB error
     */
    public void removeIdempotentReservation(String creatorId, String clientToken)
        throws WorkspaceDatabaseException;

    public void addAsyncRequest(AsyncRequest asyncRequest)
        throws WorkspaceDatabaseException;

    public AsyncRequest getAsyncRequest(String id)
            throws WorkspaceDatabaseException;

    public ArrayList<AsyncRequest> getAllAsyncRequests()
            throws WorkspaceDatabaseException;
}
