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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.workspace.Lager;
import org.globus.workspace.ProgrammingError;
import org.globus.workspace.persistence.PersistenceAdapter;
import org.globus.workspace.persistence.WorkspaceDatabaseException;
import org.nimbustools.api.services.rm.ResourceRequestDeniedException;
import org.nimbustools.api.services.rm.ManageException;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * TODO: move from static and passing in arguments etc.
 */
class ResourcepoolUtil {

    private static final Log logger =
        LogFactory.getLog(ResourcepoolUtil.class.getName());

    private static final Random randomGen = new SecureRandom();


    /**
     * Look at all the available nodes, pick the node with the least amount of available space
     * on it.  If there are multiple nodes with the same percentage available, choose a random
     * one. 
     * @param okNodes list of nodes sorted in ascending percentage memory available order
     * @param trace log trace messages
     * @return ResourcepoolEntry randomly selected from all nodes with least available space
     */
    private static ResourcepoolEntry randomLeastSpace(List<ResourcepoolEntry> okNodes,
                                                      boolean trace) {
        final List<Integer> candidateIndexes = new ArrayList<Integer>();
        final int percentEmpty = okNodes.get(0).percentEmpty();
        for (int i = 0; i < okNodes.size(); i++) {
            final ResourcepoolEntry okNode = okNodes.get(i);
            if (okNode.percentEmpty() > percentEmpty) {
                break;
            } else {
                candidateIndexes.add(i);
            }
        }

        // pick a random node from the list of equally utilized ones
        return randomSelect(candidateIndexes, okNodes, trace);
    }

    /**
     * Look at all the available nodes, pick the node with the most amount of available space
     * on it.  If there are multiple nodes with the same percentage available, choose a random
     * one. 
     * @param okNodes list of nodes sorted in ascending percentage memory available order
     * @param trace log trace messages
     * @return ResourcepoolEntry randomly selected from all nodes with highest available space
     */
    private static ResourcepoolEntry randomMostSpace(List<ResourcepoolEntry> okNodes,
                                                     boolean trace) {

        // choosing highest percent that is empty
        Collections.reverse(okNodes);

        final List<Integer> candidateIndexes = new ArrayList<Integer>();
        final int percentEmpty = okNodes.get(0).percentEmpty();
        for (int i = 0; i < okNodes.size(); i++) {
            final ResourcepoolEntry okNode = okNodes.get(i);
            if (okNode.percentEmpty() < percentEmpty) {
                break;
            } else {
                candidateIndexes.add(i);
            }
        }

        // pick a random node from the list of equally utilized ones
        return randomSelect(candidateIndexes, okNodes, trace);
    }

    private static ResourcepoolEntry randomSelect(Collection<Integer> indexes,
                                                  List<ResourcepoolEntry> okNodes,
                                                  boolean trace) {

        if (trace) {
            final StringBuilder buf = new StringBuilder("Final node choices:\n");
            for (Integer index : indexes) {
                buf.append("  Candidate #").append(index.toString()).append(": ")
                        .append(okNodes.get(index).getHostname()).append('\n');
            }
            logger.trace(buf.toString());
        }

        final int numIndexes = indexes.size();
        final int idx = randomGen.nextInt(numIndexes);
        return okNodes.get(idx);
    }

    /*
     * remove any candidates that don't support the right networks
     */
    private static void netFilter(List<ResourcepoolEntry> okNodes,
                                  String[] neededAssociations,
                                  boolean trace) {

        if (neededAssociations == null
                        || neededAssociations.length == 0) {
            return;
        }

        final Iterator iter = okNodes.iterator();
        while (iter.hasNext()) {
            
            final ResourcepoolEntry entry = (ResourcepoolEntry)iter.next();
            final String assocsStr = entry.getSupportedAssociations();

            if (assocsStr == null) {
                logger.warn("resource pool entry has no configured " +
                            "networks? entry '" + entry.getHostname() + '\'');
                iter.remove();
                continue;
            }

            if (assocsStr.equals("*")) {
                // Entry supports all associations, if this causes a failure hooking up
                // networking it's an administrator misconfiguration
                if (trace) {
                    logger.trace("'" + entry.getHostname() + "' supports all networks");
                }
                continue;
            }

            final String[] assocs = assocsStr.split(",");

            boolean allFound = true;

            for (String neededAssociation : neededAssociations) {

                boolean found = false;

                for (String assoc : assocs) {
                    if (assoc.equals(neededAssociation)) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    if (trace) {
                        logger.trace("not all networks were found in pool entry '" +
                                entry.getHostname() + "'");
                    }

                    allFound = false;
                    break;
                }
            }

            if (!allFound) {
                iter.remove();
            }
        }
    }

    
    /**
     * Note: Locking is assumed to be implemented above.
     *
     * @param mem needed memory
     * @param neededAssociations array of needed associations, can be null
     * @param db db
     * @param lager logging switches
     * @param vmid for logging
     * @param greedy true if VMs should stack up on VMMs first, false if round robin
     * @return node name can not be null
     * @throws ResourceRequestDeniedException exc
     * @throws WorkspaceDatabaseException exc
     */
    static String getResourcePoolEntry(int mem,
            String[] neededAssociations,
            final PersistenceAdapter db,
            Lager lager,
            int vmid,
            boolean greedy)
    throws ResourceRequestDeniedException,
    WorkspaceDatabaseException {

        if (db == null) {
            throw new IllegalArgumentException("null persistence adapter");
        }
        if (lager == null) {
            throw new IllegalArgumentException("lager may not be null");
        }

        final boolean eventLog = lager.eventLog;
        final boolean trace = lager.traceLog;

        if (trace) {
            traceLookingForResource(mem, neededAssociations, greedy);
        }        

        //availableEntries is never empty
        final List<ResourcepoolEntry> availableEntries =
                getAvailableEntries(mem, neededAssociations, db, trace);
        
        if (trace) {
            traceAvailableEntries(availableEntries);
        }

        ResourcepoolEntry entry;
        if (greedy) {
            entry = randomLeastSpace(availableEntries, trace);
        } else {
            entry = randomMostSpace(availableEntries, trace);
        }

        entry.addMemCurrent(-mem);
        db.replaceResourcepoolEntry(entry);

        if (eventLog) {
            logger.info(Lager.ev(vmid) + "'" + entry.getResourcePool() +
                    "' resource pool entry '" + entry.getHostname() +
                    "': " + mem + " MB reserved, " +
                    entry.getMemCurrent() + " MB left");
        }

        return entry.getHostname();            

    }

    private static List<ResourcepoolEntry> getAvailableEntries(int mem,
            String[] neededAssociations, final PersistenceAdapter db,
            final boolean trace) throws WorkspaceDatabaseException,
            ResourceRequestDeniedException {
        
        final List<ResourcepoolEntry> availableEntries =
                db.getAvailableEntriesSortedByFreeMemoryPercentage(mem);

        if(availableEntries.isEmpty()){
            String err = "No resource is available for this request (based on memory).";
            logger.error(err);
            throw new ResourceRequestDeniedException(err);
        }

        netFilter(availableEntries, neededAssociations, trace);

        if(availableEntries.isEmpty()){
            String err = "No resource can support the requested network(s).";
            logger.error(err);
            throw new ResourceRequestDeniedException(err);
        }
        
        return availableEntries;
    }

    private static void traceAvailableEntries(final List<ResourcepoolEntry> availableEntries) {
        for (ResourcepoolEntry okNode : availableEntries) {
            logger.trace("available host: " + okNode.getHostname() +
                    ", mem: " + okNode.getMemCurrent() +
                    ", percent available: " + okNode.percentEmpty());
        }
    }

    private static void traceLookingForResource(int mem,
                                                String[] neededAssociations,
                                                boolean greedy) {
        
        final StringBuilder buf =
            new StringBuilder("Looking for resource. Mem = ");

        buf.append(mem);

        if (greedy) {
            buf.append(", greedy selection strategy");
        } else {
            buf.append(", round robin selection strategy");
        }

        buf.append(", needed networks: ");
        if (neededAssociations == null) {
            buf.append("null");
        } else {
            for (String neededAssociation : neededAssociations) {
                buf.append(neededAssociation).append(' ');
            }
        }

        logger.trace(buf.toString());
    }    

    /**
     * NOTE: a node may not be in more than one resource pool, will
     * result in inccorect behavior.  Pools can be used with authorization
     * policies to give good nodes to good clients etc.  In the future,
     * we might allow splitting one node's RAM across pools, but not now.
     *
     * Also, currently the scheduler sends "any" for every request,
     * authorization policies wouldn't enforce a certain pool on a
     * certain client until the scheduler (or the authorization callout)
     * gets that functionality.
     *
     * @param hostname hostname
     * @param mem memory
     * @param db db
     * @param eventLog log events
     * @param traceLog log traces
     * @param vmid for tracking in logs
     * @throws ManageException exc
     */
    static void retireMem(String hostname,
                          int mem,
                          PersistenceAdapter db,
                          boolean eventLog,
                          boolean traceLog,
                          int vmid)

            throws ManageException {

        if (traceLog) {
            logger.trace("retireMem()");
        }

        if (db == null) {
            throw new IllegalArgumentException("null persistence adapter");
        }

        if (hostname == null) {
            throw new IllegalArgumentException("hostname may not be null");
        }

        final ResourcepoolEntry entry = db.getResourcepoolEntry(hostname);

        if (entry != null) {

            final String poolname = entry.getResourcePool();

            entry.addMemCurrent(mem);

            // If the node's memory capacity was changed during this VM's
            // deployment, there can be a situation when this addition
            // will make the current memory exceed the maximum.  If this
            // happens, the current memory is adjusted to be the maximum.
            if (entry.getMemCurrent() > entry.getMemMax()) {
                entry.setMemCurrent(entry.getMemMax());
            }

            db.replaceResourcepoolEntry(entry);

            if (eventLog) {
                logger.info(Lager.ev(vmid) + "'" + poolname +
                        "' resource pool entry '" + hostname + "': " + mem +
                        " MB given back, now has " + entry.getMemCurrent() +
                        " MB available");
            }

        } else {

            logger.warn(Lager.id(vmid) +
                    "could not find a resource pool entry to return " +
                    "lease -- the only way this should be possible is " +
                    "if a new configuration was loaded and a resource was " +
                    "deleted from the pools");
        }
    }
}
