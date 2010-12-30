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

package org.globus.workspace.scheduler.defaults;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.workspace.Lager;
import org.globus.workspace.LockAcquisitionFailure;
import org.globus.workspace.ProgrammingError;
import org.globus.workspace.persistence.WorkspaceDatabaseException;
import org.globus.workspace.persistence.PersistenceAdapter;
import org.globus.workspace.scheduler.NodeExistsException;
import org.globus.workspace.scheduler.NodeInUseException;
import org.globus.workspace.scheduler.NodeManagement;
import org.globus.workspace.scheduler.NodeNotFoundException;
import org.globus.workspace.scheduler.Reservation;
import org.globus.workspace.scheduler.Scheduler;
import org.globus.workspace.service.InstanceResource;
import org.globus.workspace.service.WorkspaceHome;
import org.globus.workspace.service.binding.vm.VirtualMachine;
import org.globus.workspace.service.binding.vm.VirtualMachineDeployment;
import org.nimbustools.api.services.rm.DoesNotExistException;
import org.nimbustools.api.services.rm.ManageException;
import org.nimbustools.api.services.rm.NotEnoughMemoryException;
import org.nimbustools.api.services.rm.ResourceRequestDeniedException;

/**
 * Needs dependency cleanups
 */
public class DefaultSlotManagement implements SlotManagement, NodeManagement {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------
    
    private static final Log logger =
        LogFactory.getLog(DefaultSlotManagement.class.getName());

    // See locking section below for explanation
    private static final ReentrantLock WHOLE_MANAGER_LOCK = new ReentrantLock(true);
    private static final ReentrantLock DESTRUCTION_LOCK = new ReentrantLock(true);


    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private final PersistenceAdapter db;
    private final Lager lager;
    private WorkspaceHome home;
    private PreemptableSpaceManager preempManager;    

    private boolean greedy;


    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    public DefaultSlotManagement(PersistenceAdapter db,
                                 Lager lager) {

        if (db == null) {
            throw new IllegalArgumentException("db may not be null");
        }
        this.db = db;

        if (lager == null) {
            throw new IllegalArgumentException("lager may not be null");
        }
        this.lager = lager;
    }

    
    // -------------------------------------------------------------------------
    // MODULE SET (avoids circular dependency problem)
    // -------------------------------------------------------------------------

    public void setHome(WorkspaceHome homeImpl) {
        if (homeImpl == null) {
            throw new IllegalArgumentException("homeImpl may not be null");
        }
        this.home = homeImpl;
    }
    
    public void setPreempManager(PreemptableSpaceManager preempManagerImpl) {
        if (preempManagerImpl == null) {
            throw new IllegalArgumentException("preempManagerImpl may not be null");
        }
        this.preempManager = preempManagerImpl;
    }    

    
    // -------------------------------------------------------------------------
    // SET
    // -------------------------------------------------------------------------


    public void setSelectionStrategy(String selectionStrategy) {

        // leave room for more options in the future
        final String RROBIN = "round-robin";
        final String GREEDY = "greedy";
        
        if (RROBIN.equalsIgnoreCase(selectionStrategy)) {
            this.greedy = false;
        } else if (GREEDY.equalsIgnoreCase(selectionStrategy)) {
            this.greedy = true;
        } else {
            throw new IllegalArgumentException(
                    "Unknown VMM selection strategy: '" + selectionStrategy + "'.  This " +
                            "scheduler only accepts: '" + RROBIN + "' and '" + GREEDY + '\'');
        }
    }

    // -------------------------------------------------------------------------
    // LOCKING
    // -------------------------------------------------------------------------

    /*
        There are two locks: WHOLE_MANAGER_LOCK and DESTRUCTION_LOCK

        The WHOLE_MANAGER_LOCK is used in lieu of the synchronized keyword.  This protects
        the heart of the scheduling decisions from race conditions such as multiple allocation
        methods at once, nodes being removed by the administrator mid-flight, etc.  Most of
        the methods here are multi-step, relying on several pieces of information from the
        database.  So they need this lock.

        There is one situation where this lock gets in the way.  When new nodes are being
        allocated and there is preemptible space for it that is being used by backfill or
        spot instances: the preemptible slot manager will release these nodes but in doing
        so the slot manager is involved.  Importantly, it is done via other threads so it
        can not re-enter a normal synchronized lock (there is a group of requests sent
        simultaneously, using the DestroyFutureTask class).

        While relying on multiple tasks/threads, the group destruction method that is used
        is also waiting for all of those tasks to complete.  So during "reserveSpace" when
        it is deduced that asking the preemptible manager to destroy VMs will work to make
        way for the higher priority request, the method can release the DESTRUCTION_LOCK
        and allow for things to be destroyed while it waits.

        This allows other threads to "releaseSpace"

        It might not just be the preemptible manager that is causing destructions during
        this period, and that is OK.

        Internals note: if you want to change anything note that in order to acquire the
        WHOLE_MANAGER_LOCK you *must* acquire the DESTRUCTION_LOCK first.  This is
        because destruction events *only* need the DESTRUCTION_LOCK in order to carry out
        their business.  There is a race condition here where there could be starving
        if we don't do this.
     */

    // Read the long locking comment above before using/changing
    private void acquireWholeManagerLock() throws ResourceRequestDeniedException {
        // Acquiring whole manager lock requires that you also acquire the destruction lock
        // and also requires that you get it first.
        try {
            DESTRUCTION_LOCK.lockInterruptibly();
        } catch (InterruptedException e) {
            throw new ResourceRequestDeniedException(
                        new LockAcquisitionFailure(e));
        }
        try {
            WHOLE_MANAGER_LOCK.lockInterruptibly();
        } catch (InterruptedException e) {
            throw new ResourceRequestDeniedException(
                        new LockAcquisitionFailure(e));
        }
    }

    // Read the long locking comment above before using/changing
    private void _acquireDestructionLock() throws ResourceRequestDeniedException {
        try {
            DESTRUCTION_LOCK.lockInterruptibly();
        } catch (InterruptedException e) {
            throw new ResourceRequestDeniedException(
                        new LockAcquisitionFailure(e));
        }
    }

    // Read the long locking comment above before using/changing
    private void releaseWholeManagerLock() {
        // Releasing whole manager lock requires that you also release the destruction lock
        WHOLE_MANAGER_LOCK.unlock();
        DESTRUCTION_LOCK.unlock();
    }

    // Read the long locking comment above before using/changing
    private void _releaseDestructionLock() {
        DESTRUCTION_LOCK.unlock();
    }

    
    // -------------------------------------------------------------------------
    // implements SlotManagement
    // -------------------------------------------------------------------------
    
    /**
     * @param req a single workspace or homogenous group-workspace request
     * @return Reservation res
     * @throws ResourceRequestDeniedException exc
     */
    public Reservation reserveSpace(NodeRequest req, boolean preemptable)

            throws ResourceRequestDeniedException {

        this.acquireWholeManagerLock();
        try {
            if (req == null) {
                throw new IllegalArgumentException("req is null");
            }

            final int[] vmids = req.getIds();

            final String[] hostnames =
                    this.reserveSpace(vmids, req.getMemory(),
                                      req.getNeededAssociations(), preemptable);

            return new Reservation(vmids, hostnames);
        } finally {
            this.releaseWholeManagerLock();
        }
    }

    /**
     * @param requests an array of single workspace or homogenous
     *                 group-workspace requests
     * @param coschedid coscheduling (ensemble) ID
     * @return Reservation res
     * @throws ResourceRequestDeniedException exc
     */
    public Reservation reserveCoscheduledSpace(NodeRequest[] requests,
                                               String coschedid)
            throws ResourceRequestDeniedException {
        this.acquireWholeManagerLock();
        try {
            return this._reserveCoscheduledSpace(requests, coschedid);
        } finally {
            this.releaseWholeManagerLock();
        }
    }

    private Reservation _reserveCoscheduledSpace(NodeRequest[] requests,
                                                 String coschedid)
            throws ResourceRequestDeniedException {

        if (requests == null || requests.length == 0) {
            throw new IllegalArgumentException("requests null or length 0?");
        }

        final ArrayList idInts = new ArrayList(64);
        final ArrayList allHostnames = new ArrayList(64);
        final ArrayList allDurations = new ArrayList(64);
        final ArrayList allMemory = new ArrayList(64); // for backouts if needed

        try {
            for (int i = 0; i < requests.length; i++) {

                final NodeRequest request = requests[i];

                final int[] ids = request.getIds();
                if (ids == null) {
                    throw new ResourceRequestDeniedException(
                            "Cannot proceed, no ids in NodeRequest (?)");
                }

                final String[] hostnames =
                        this.reserveSpace(ids,
                                          request.getMemory(),
                                          request.getNeededAssociations(), false);

                final Integer duration = new Integer(request.getDuration());

                for (int j = 0; j < ids.length; j++) {
                    idInts.add(new Integer(ids[j]));
                    allHostnames.add(hostnames[j]);
                    allDurations.add(duration);
                    allMemory.add(new Integer(request.getMemory()));
                }
            }
        } catch (Exception e) {
            String msg = "Problem reserving space for coscheduling group '" +
                         coschedid + "': " + e.getMessage();

            if (logger.isDebugEnabled()) {
                logger.error(msg, e);
            } else {
                logger.error(msg);
            }

            if (allHostnames.size() != allMemory.size()) {
                logger.fatal("Could not back reservations out, no matching " +
                        "memory recordings (?)");
                throw new ResourceRequestDeniedException(msg);
                }

            final String[] justReservedNodes = (String[])
                    allHostnames.toArray(new String[allHostnames.size()]);
            final Integer[] justReservedMemory = (Integer[])
                    allMemory.toArray(new Integer[allMemory.size()]);

            for (int i = 0; i < justReservedNodes.length; i++) {
                try {

                    ResourcepoolUtil.retireMem(justReservedNodes[i],
                                               justReservedMemory[i],
                                               this.db,
                                               this.lager.eventLog,
                                               this.lager.traceLog,
                                               -1, false);
            } catch (Exception ee) {
                    logger.error(ee.getMessage());
                }
            }

            throw new ResourceRequestDeniedException(msg);
        }

        final int length = idInts.size();

        final int[] all_ids = new int[length];
        final String[] all_hostnames = new String[length];
        final int[] all_durations = new int[length];

        for (int i = 0; i < length; i++) {
            all_ids[i] = ((Number)idInts.get(i)).intValue();
            all_hostnames[i] = (String)allHostnames.get(i);
            all_durations[i] = ((Integer)allDurations.get(i)).intValue();
        }

        return new Reservation(all_ids, all_hostnames, all_durations);
    }

    /**
     * Only handling one slot per VM for now, will change in the future
     * (multiple layers).
     *
     * @param vmids array of IDs.  If array length is greater than one, it is
     *        up to the implementation (and its configuration etc) to decide
     *        if each must map to its own node or not.  In the case where more
     *        than one VM is mapped to the same node, the returned node
     *        assignment array will include duplicates.
     * @param memory megabytes needed
     * @param assocs array of needed associations, can be null
     * @param preemptable indicates if the space can be pre-empted by higher priority reservations
     * @return Names of resources.  Must match length of vmids input and caller
     *         assumes the ordering in the assignemnt array maps to the input
     *         vmids array.
     *
     * @throws ResourceRequestDeniedException can not fulfill request
     */
    private String[] reserveSpace(final int[] vmids,
                                  final int memory,
                                  final String[] assocs, 
                                  boolean preemptable)
                  throws ResourceRequestDeniedException {


        if (vmids == null) {
            throw new IllegalArgumentException("no vmids");
        }

        String msg = "request for " + vmids.length + " space(s) with " +
                "mem = " + memory;

        if (lager.traceLog) {

            if (assocs == null) {
                msg += ", needed networks null";
            } else if (assocs.length == 0) {
                msg += ", needed networks = zero length";
            } else {
                msg += ", needed networks = ";
                for (int i = 0; i < assocs.length; i++) {
                    msg += "[" + i + "] " + assocs[i];
                    if (i != assocs.length-1) {
                        msg += ", ";
                    }
                }
            }
            logger.trace(msg);
        } else {
            logger.debug(msg);
        }

        // in future, getResourcepoolEntry() will take a group request
        // (and as much as we will move down to SQL for efficiency)

        final String[] nodes = new String[vmids.length];
        int bailed = -1;
        Throwable failure = null;
        
        for (int i = 0; i < vmids.length; i++) {

            try {
                nodes[i] = ResourcepoolUtil.getResourcePoolEntry(memory,
                                                                 assocs,
                                                                 this.db,
                                                                 this.lager,
                                                                 vmids[i],
                                                                 greedy,
                                                                 preemptable);
                if (nodes[i] == null) {
                    throw new ProgrammingError(
                                    "returned node should not be null");
                }
            } catch (NotEnoughMemoryException e) {
                if(!preemptable){
                    try {
                        //If there isn't available memory
                        //for a non-preemptable reservation
                        //ask preemptable space manager
                        //to free needed space from
                        //preemptable (lower priority)
                        //reservations
                        
                        Integer availableMemory = this.db.getTotalAvailableMemory();    
                        Integer usedPreemptable = this.db.getTotalPreemptableMemory();
                        
                        Integer realAvailable = availableMemory + usedPreemptable;
                        
                        Integer neededMem = (vmids.length-i)*memory;
                        
                        if(realAvailable >= neededMem){
                            //There will be sufficient space to
                            //fulfill this reservation
                            //so, free preemptable space
                            //and decrease i value, so 
                            //previous entry can be reconsidered

                            // Read the long locking comment above before using/changing
                            this._releaseDestructionLock();
                            preempManager.releaseSpace(neededMem);
                            this._acquireDestructionLock();
                            i--;
                        } else {
                            throw e;
                        }
                    } catch (Throwable t) {
                        bailed = i;
                        failure = t;
                        break;
                    }                    
                }
            } catch (Throwable t) {
                bailed = i;
                failure = t;
                break;
            }
        }

        if (failure == null) {
            return nodes;
        }

        // nothing to back out:
        if (bailed < 1) {
            if (failure instanceof ResourceRequestDeniedException) {
                throw (ResourceRequestDeniedException) failure;
            } else {
                throw new ResourceRequestDeniedException(
                                                failure.getMessage());
            }
        }

        final String clientMsg;
        if (bailed == 1) {
            clientMsg = "Problem reserving enough space (did get enough " +
                        "for one VM)";
        } else {
            clientMsg = "Problem reserving enough space (did get enough " +
                        "for " + bailed + " VMs)";
        }

        // back out
        for (int i = 0; i < bailed; i++) {
            try {
                ResourcepoolUtil.retireMem(nodes[i], memory, this.db,
                                           this.lager.eventLog,
                                           this.lager.traceLog,
                                           vmids[i], preemptable);
            } catch (Throwable t) {
                if (logger.isDebugEnabled()) {
                    logger.error(
                            "Error with one backout: " + t.getMessage(), t);
                } else {
                    logger.error(
                            "Error with one backout: " + t.getMessage());
                }
                // continue trying to backout anyhow
            }
        }

        throw new ResourceRequestDeniedException(clientMsg);
    }

    public boolean isBestEffort() {
        return false;
    }

    public boolean isEvacuationStrict() {
        return false;
    }

    public boolean isNeededAssociationsSupported() {
        return true;
    }
    
    public boolean canCoSchedule() {
        return true;
    }

    public void releaseSpace(final int vmid) throws ManageException {
        try {
            // Read the long locking comment above before using/changing
            this._acquireDestructionLock();
        } catch (ResourceRequestDeniedException e) {
            throw new ManageException("", e);
        }
        try {
            this._releaseSpace(vmid);
        } finally {
            this._releaseDestructionLock();
        }
    }

    private void _releaseSpace(final int vmid) throws ManageException {

        if (lager.traceLog) {
            logger.trace("releaseSpace(): " + Lager.id(vmid));
        }

        // get the necessary information from the resource, no reason (yet) to
        // double track it here even if that is in principle more encapsulated 

        // assuming resource vm correlation as usual
        final InstanceResource resource;
        try {
            // find will not return null
            resource = this.home.find(vmid);
        } catch (DoesNotExistException e) {
            logger.error("resource pool management received releaseSpace " +
                    "for workspace " + Lager.id(vmid)
                    + ", but WorkspaceHome failed to find it", e);
            return;
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.error(e.getMessage(), e);
            } else {
                logger.error(e.getMessage());
            }
            throw new ManageException(e);
        }

        logger.debug("found resource = " + resource);

        final VirtualMachine vm = resource.getVM();
        if (vm == null) {
            throw new ProgrammingError("vm is null");
        }

        final String node = vm.getNode();
        if (node == null) {
            logger.warn(Lager.id(vmid) + " assuming no node assignment yet " +
                    "because of ensemble not-done");
            return;
        }

        final VirtualMachineDeployment vmdep = vm.getDeployment();
        if (vmdep == null) {
            throw new ProgrammingError("deployment is null");
        }
        
        boolean preemptable = vm.isPreemptable();

        final int mem = vmdep.getIndividualPhysicalMemory();

        logger.debug("releaseSpace() retiring mem = " + mem +
                    ", node = '" + node + "' from " + Lager.id(vmid) + ". Preemptable: " + preemptable);

        ResourcepoolUtil.retireMem(node, mem, this.db,
                                   this.lager.eventLog, this.lager.traceLog,
                                   vmid, preemptable);
    }

    public void setScheduler(Scheduler adapter) {
        // ignored
    }


    // -------------------------------------------------------------------------
    // IoC INIT METHOD
    // -------------------------------------------------------------------------

    public void validate() throws Exception {
        if (this.home == null) {
            throw new Exception("home was not set");
        }
        if (this.preempManager == null) {
            throw new Exception("preempManager was not set");
        }
    }


    public ResourcepoolEntry addNode(String hostname,
                                     String pool,
                                     String associations,
                                     int memory,
                                     boolean active)
            throws NodeExistsException, WorkspaceDatabaseException {
        try {
            this.acquireWholeManagerLock();
        } catch (ResourceRequestDeniedException e) {
            throw new WorkspaceDatabaseException(e.getMessage(), e);
        }
        try {
            return this._addNode(hostname, pool, associations, memory, active);
        } finally {
            this.releaseWholeManagerLock();
        }
    }

    private ResourcepoolEntry _addNode(String hostname,
                                       String pool,
                                       String associations,
                                       int memory,
                                       boolean active)
            throws NodeExistsException, WorkspaceDatabaseException {

        if (hostname == null) {
            throw new IllegalArgumentException("hostname may not be null");
        }
        hostname = hostname.trim();
        if (hostname.length() == 0) {
            throw new IllegalArgumentException("hostname may not be empty");
        }

        final ResourcepoolEntry existing =
                this.db.getResourcepoolEntry(hostname);

        if (existing != null) {
            throw new NodeExistsException("A VMM node with the hostname "+
                    hostname+" already exists in the pool");
        }

        // This will catch the corner case of one or many VMs being started
        // on a node, the node being deleted from the configuration, the node
        // being RE-inserted into the configuration, all the while with no
        // VM memory being retired.

        final int memInUse =
                    this.db.memoryUsedOnPoolnode(hostname);

        final int correctCurrentMem = memory - memInUse;

        if (correctCurrentMem == memory) {
            if (lager.traceLog) {
                logger.trace("curmem for VMM '" + hostname +
                        "' matches VM records");
            }
        } else {
            logger.info("Reconfiguration corner case, current " +
                    "memory-in-use record for VMM '" +
                    hostname +
                    "' was wrong, old value was " +
                    "0 MB, new value is " +
                    correctCurrentMem + " MB.");
        }

        final ResourcepoolEntry entry =
                new ResourcepoolEntry(pool, hostname, memory,
                        correctCurrentMem, 0, associations, active);

        //check then act protected by lock
        this.db.addResourcepoolEntry(entry);
        this.poolChanged();
        return entry;
    }

    public List<ResourcepoolEntry> getNodes() throws WorkspaceDatabaseException {
        return this.db.currentResourcepoolEntries();
    }

    public ResourcepoolEntry getNode(String hostname) throws WorkspaceDatabaseException {
        if (hostname == null) {
            throw new IllegalArgumentException("hostname may not be null");
        }
        hostname = hostname.trim();
        if (hostname.length() == 0) {
            throw new IllegalArgumentException("hostname may not be empty");
        }
        return this.db.getResourcepoolEntry(hostname);
    }

    /**
     * Updates an existing pool entry.
     *
     * Null values for any of the parameters mean no update to that field.
     * But at least one field must be specified.
     * @param hostname the node to be updated, required
     * @param pool the new resourcepool name, can be null
     * @param networks the new networks association list, can be null
     * @param memory the new max memory value for the node, can be null
     * @param active the new active state for the node, can be null
     * @return the updated ResourcepoolEntry
     * @throws NodeInUseException
     * @throws NodeNotFoundException
     */
    
    public ResourcepoolEntry updateNode(
            String hostname,
            String pool,
            String networks,
            Integer memory,
            Boolean active)
            throws NodeInUseException, NodeNotFoundException, WorkspaceDatabaseException {

        try {
            this.acquireWholeManagerLock();
        } catch (ResourceRequestDeniedException e) {
            throw new WorkspaceDatabaseException(e.getMessage(), e);
        }
        try {

            Integer availMemory = null;
            if (memory != null) {
                final ResourcepoolEntry entry = getNode(hostname);
                if (entry == null) {
                    throw new NodeNotFoundException();
                }

                if (!entry.isVacant()) {
                    logger.info("Refusing to update VMM node "+ hostname+
                            " memory max while VMs are running");
                    throw new NodeInUseException();
                }
                availMemory = memory;
            }

            boolean updated = this.db.updateResourcepoolEntry(hostname,
                    pool, networks, memory, availMemory, active);
            if (!updated) {
                throw new NodeNotFoundException();
            }

            ResourcepoolEntry result = getNode(hostname);
            this.poolChanged();
            return result;
        } finally {
            this.releaseWholeManagerLock();
        }
    }

    public boolean removeNode(String hostname)
            throws NodeInUseException, WorkspaceDatabaseException {
        if (hostname == null) {
            throw new IllegalArgumentException("hostname may not be null");
        }
        hostname = hostname.trim();
        if (hostname.length() == 0) {
            throw new IllegalArgumentException("hostname may not be empty");
        }

        try {
            this.acquireWholeManagerLock();
        } catch (ResourceRequestDeniedException e) {
            throw new WorkspaceDatabaseException(e.getMessage(), e);
        }
        boolean result;
        try {
            final ResourcepoolEntry entry =
                    this.db.getResourcepoolEntry(hostname);

            if (entry == null) {
                return false;
            }

            if (!entry.isVacant()) {
                throw new NodeInUseException("The VMM node "+ hostname +
                        " is in use and cannot be removed from the pool");
            }

            result = this.db.removeResourcepoolEntry(hostname);
            
        } finally {
            this.releaseWholeManagerLock();
        }
        // needs to be triggered after the lock is released
        this.poolChanged();
        return result;
    }

    private void poolChanged() {
        this.preempManager.recalculateAvailableInstances();
    }

    public String getVMMReport() {
        try {
            return this._getVMMReport();
        } catch (WorkspaceDatabaseException e) {
            logger.error(e.getMessage());
            return e.getMessage();
        }
    }

    private String _getVMMReport() throws WorkspaceDatabaseException {
        final StringBuilder sb = new StringBuilder();
        List<ResourcepoolEntry> relist = this.db.currentResourcepoolEntries();
        for (ResourcepoolEntry re : relist) {
            sb.append("\n-------------------------------------------------");
            sb.append("\n    Hostname: ").append(re.getHostname());
            sb.append("\n      Active: ").append(re.isActive());
            sb.append("\n      Vacant: ").append(re.isVacant());
            sb.append("\n     Max mem: ").append(re.getMemMax());
            sb.append("\n  Avail. mem: ").append(re.getMemCurrent());
            sb.append("\n  Percentage: ").append(re.percentEmpty());
            sb.append("\n Preemptable: ").append(re.getMemPreemptable()).append("\n");
        }
        return sb.toString();
    }
}
