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

package org.globus.workspace.scheduler.defaults;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.workspace.Lager;
import org.globus.workspace.ProgrammingError;
import org.globus.workspace.persistence.PersistenceAdapter;
import org.globus.workspace.persistence.WorkspaceDatabaseException;
import org.nimbustools.api.services.rm.ResourceRequestDeniedException;
import org.nimbustools.api.services.rm.ManageException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileNotFoundException;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * TODO: move from static and passing in arguments etc.
 */
class ResourcepoolUtil {

    private static final Log logger =
        LogFactory.getLog(ResourcepoolUtil.class.getName());

    private static final String COMMENT_CHAR = "#";

    /**
     * Note: Locking is assumed to be implemented above.
     *
     * @param name can be resource pool name or null which means "any"
     * @param mem needed memory
     * @param neededAssociations array of needed associations, can be null
     * @param db db
     * @param lager logging switches
     * @param vmid for logging
     * @return node name can not be null
     * @throws ResourceRequestDeniedException exc
     * @throws WorkspaceDatabaseException exc
     */
    static String getResourcepoolEntry(String name,
                                       int mem,
                                       String[] neededAssociations,
                                       final PersistenceAdapter db,
                                       Lager lager,
                                       int vmid)
               throws ResourceRequestDeniedException,
                      WorkspaceDatabaseException {
        
        if (db == null) {
            throw new IllegalArgumentException("null persistence adapter");
        }
        if (lager == null) {
            throw new IllegalArgumentException("lager may not be null");
        }

        final boolean eventLog = lager.eventLog;
        final boolean traceLog = lager.traceLog;

        final Hashtable resourcepools = db.currentResourcepools();

        ResourcepoolEntry entry;

        if (name == null) {

            // null means "any"

            final Enumeration e = resourcepools.keys();
            while (e.hasMoreElements()) {
                final String aPoolName = (String)e.nextElement();

                // Try to find an empty slot first, then go back and look for
                // a slice.  Slower this way (but a little less horrible).
                entry = nextApplicableEmptyResourcepoolEntry(aPoolName,
                                                             resourcepools,
                                                             mem,
                                                             neededAssociations,
                                                             traceLog);

                if (entry == null) {
                    entry = nextApplicableResourcepoolEntry(aPoolName,
                                                            resourcepools,
                                                            mem,
                                                            neededAssociations,
                                                            traceLog);
                }

                if (entry != null) {
                    entry.addMemCurrent(-mem);
                    db.replaceResourcepoolEntry(aPoolName, entry);

                    if (eventLog) {
                        logger.info(Lager.ev(vmid) + "'" + aPoolName +
                              "' resource pool entry '" + entry.getHostname() +
                              "': " + mem + " MB reserved, " +
                              entry.getMemCurrent() + " MB left");
                    }

                    return entry.getHostname();
                }
            }

            String err = "No resource pool has an applicable entry";
            logger.error(err);
            throw new ResourceRequestDeniedException(err);

        } else {

            // Try to find an empty slot first, then go back and look for
            // a slice.  Slower this way (but a little less horrible).
            entry = nextApplicableEmptyResourcepoolEntry(name,
                                                         resourcepools,
                                                         mem,
                                                         neededAssociations,
                                                         traceLog);

            if (entry == null) {
                entry = nextApplicableResourcepoolEntry(name,
                                                        resourcepools,
                                                        mem,
                                                        neededAssociations,
                                                        traceLog);
            }
            
            if (entry != null) {
                entry.addMemCurrent(-mem);
                db.replaceResourcepoolEntry(name, entry);
                
                if (eventLog) {
                    logger.info(Lager.ev(-1) + "'" + name + "' resource " +
                    "pool entry '" + entry.getHostname() + "': " + mem +
                    " MB reserved, " + entry.getMemCurrent() + " MB left");
                }
                
                return entry.getHostname();

            } else {
                
                String err = "resourcepool '" + name
                                    + "' has no applicable entry";
                logger.error(err);
                throw new ResourceRequestDeniedException(err);
            }
        }
    }

    private static ResourcepoolEntry nextApplicableResourcepoolEntry(
                                                String name,
                                                Hashtable resourcepools,
                                                int mem,
                                                String[] neededAssociations,
                                                boolean traceLog)

            throws ResourceRequestDeniedException {

        return nextApplicableResourcepoolEntryImpl(name,
                                                   resourcepools,
                                                   mem,
                                                   neededAssociations,
                                                   false,
                                                   traceLog);
    }

    private static ResourcepoolEntry nextApplicableEmptyResourcepoolEntry(
                                                String name,
                                                Hashtable resourcepools,
                                                int mem,
                                                String[] neededAssociations,
                                                boolean traceLog)

            throws ResourceRequestDeniedException {

        return nextApplicableResourcepoolEntryImpl(name,
                                                   resourcepools,
                                                   mem,
                                                   neededAssociations,
                                                   true,
                                                   traceLog);
    }

    private static ResourcepoolEntry nextApplicableResourcepoolEntryImpl(
                                                String name, 
                                                Hashtable resourcepools,
                                                int mem,
                                                String[] neededAssociations,
                                                boolean emptyOnly,
                                                boolean traceLog)

            throws ResourceRequestDeniedException {

        final Resourcepool resourcepool =
                                (Resourcepool)resourcepools.get(name);

        if (resourcepool == null) {
            String err = "'" + name + "' is not a valid resourcepool name (?)";
            logger.error(err);
            throw new ResourceRequestDeniedException(err);
        }

        if (resourcepool.getEntries() == null) {
            logger.debug("no entries in resourcepool");
            return null;
        }

        if (traceLog) {

            StringBuffer buf =
                    new StringBuffer("Looking for resource.  Name = ");

            buf.append(name)
               .append(", mem = ")
               .append(mem)
               .append(", emptyOnly = ")
               .append(emptyOnly)
               .append(", needed networks: ");
            
            if (neededAssociations == null) {
                buf.append("null");
            } else {
                for (int i = 0; i < neededAssociations.length; i++) {
                    buf.append(neededAssociations[i])
                       .append(" ");
                }
            }

            logger.trace(buf.toString());
        }

        final Enumeration e = resourcepool.getEntries().elements();
        ResourcepoolEntry entry;

        // for scaling, actual RM impl should use straight SQL of course

        while (e.hasMoreElements()) {
            entry = (ResourcepoolEntry)e.nextElement();

            if (emptyOnly && entry.getMemCurrent() != entry.getMemMax()) {
                continue;
            }

            if (entry.getMemCurrent() >= mem) {

                if (neededAssociations == null
                        || neededAssociations.length == 0) {

                    // VM doesn't need a NIC
                    return entry;
                }

                String assocsStr = entry.getSupportedAssociations();
                if (assocsStr == null) {

                    // VM requires at least one association, continue looking
                    // at other entries
                    logger.warn("resource pool entry has no configured " +
                                "networks? entry '" + entry.getHostname() +
                                "' from pool '" + name + "'");
                    continue;
                }


                if (traceLog) {
                    logger.trace("found resource pool entry with enough " +
                            "memory ('" +  entry.getHostname() +
                            "'), checking networks it supports");
                }

                if (assocsStr.equals("*")) {
                    // (Perpetuating the non-relational method)

                    // Entry supports all associations, if this causes
                    // a failure hooking up networking it's an
                    // administrator misconfiguration

                    if (traceLog) {
                        logger.trace("'" + entry.getHostname() +
                            "' supports all networks, so choosing it");
                    }

                    return entry;
                }

                final String[] assocs = assocsStr.split(",");

                boolean allFound = true;

                for (int i = 0; i < neededAssociations.length; i++) {

                    boolean found = false;

                    for (int j = 0; j < assocs.length; j++) {
                        if (assocs[j].equals(neededAssociations[i])) {

                            if (traceLog) {
                                logger.trace("'" + entry.getHostname() +
                                             "' supports needed assoc '" +
                                             neededAssociations[i] + "'");
                            }

                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        if (traceLog) {
                            logger.trace("not all networks were " +
                                         "found in pool entry '" +
                                         entry.getHostname() + "'");
                        }

                        allFound = false;
                        break;
                    }
                }

                if (allFound) {
                    return entry;
                }
            }
        }

        return null; // nothing found
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

        final Hashtable resourcepools = db.currentResourcepools();

        boolean returned = false;
        
        final Enumeration en = resourcepools.keys();
        while (en.hasMoreElements()) {

            // get name for logging
            String poolname = (String) en.nextElement();

            Resourcepool pool = (Resourcepool) resourcepools.get(poolname);
            if (pool == null) {
                throw new ProgrammingError("all resource pools " +
                                    "in the hashmap should be non-null");
            }

            Hashtable entries = pool.getEntries();
            if (entries == null) {
                throw new ProgrammingError("all entries in the resource " +
                                    "pool should be non-null");
            }

            if (entries.containsKey(hostname)) {

                ResourcepoolEntry entry =
                                (ResourcepoolEntry)entries.get(hostname);

                entry.addMemCurrent(mem);

                // If the node's memory capacity was changed during this VM's
                // deployment, there can be a situation when this addition
                // will make the current memory exceed the maximum.  If this
                // happens, the current memory is adjusted to be the maximum.
                if (entry.getMemCurrent() > entry.getMemMax()) {
                    entry.setMemCurrent(entry.getMemMax());
                }

                db.replaceResourcepoolEntry(poolname, entry);

                returned = true;

                if (eventLog) {
                    logger.info(Lager.ev(vmid) + "'" + poolname +
                    "' resource pool entry '" + hostname + "': " + mem +
                    " MB given back, now has " + entry.getMemCurrent() +
                    " MB available");
                }

                break;
            }
        }

        if (!returned) {
            logger.warn(Lager.id(vmid) +
                    "could not find a resource pool entry to return " +
                    "lease -- the only way this should be possible is " +
                    "if a new configuration was loaded and a resource was " +
                    "deleted from the pools");
        }
    }

    static Hashtable loadResourcepools(String resourcepoolDirectory,
                                       Hashtable previous,
                                       boolean traceLog)
                                                    throws Exception {

        if (resourcepoolDirectory == null) {
            throw new IllegalArgumentException("null resourcepoolDirectory");
        }

        if (traceLog) {
            logger.trace("loadResourcepools(): resourcepoolDirectory = "
                                        + resourcepoolDirectory);
        }

        final File pooldirpath = new File(resourcepoolDirectory);

        if (!pooldirpath.exists() || !pooldirpath.isDirectory()) {
            throw new FileNotFoundException("Not found or not a directory: '" +
                                resourcepoolDirectory);
        }

        final Hashtable newPoolSet = new Hashtable();

        final String[] listing = pooldirpath.list();

        for (int i = 0; i < listing.length; i++) {

            final String path = pooldirpath.getAbsolutePath()
                                            + File.separator + listing[i];

            if (traceLog) {
                logger.trace("examining '" + path + "'");
            }

            final File resourcepoolFile = new File(path);

            if (!resourcepoolFile.isFile()) {
                logger.warn("not a file: '" + path + "'");
                continue;
            }

            Resourcepool oldpool = null;
            if (previous != null) {
                oldpool = (Resourcepool)
                            previous.get(resourcepoolFile.getName());

                // skip reading if file modification time isn't newer than last
                // container boot
                if (oldpool != null) {
                    if (oldpool.getFileTime() ==
                                    resourcepoolFile.lastModified()) {
                        logger.debug("file modification time for pool '"
                                + resourcepoolFile.getName()
                                + "' is not newer, using old configuration");

                        newPoolSet.put(resourcepoolFile.getName(),
                                       oldpool);
                        continue;
                    }
                }
            }

            final Resourcepool resourcepool = new Resourcepool();
            final Hashtable entries = new Hashtable();

            String line;
            InputStream in = null;
            InputStreamReader isr = null;
            BufferedReader bufrd = null;
            try {
                in = new FileInputStream(resourcepoolFile);
                isr = new InputStreamReader(in);
                bufrd = new BufferedReader(isr);

                while ((line = bufrd.readLine()) != null) {
                    line = line.trim();
                    if (line.length() > 0) {
                        ResourcepoolEntry entry =
                                getPoolEntry(line, oldpool);
                        if (entry != null) {
                            entries.put(entry.getHostname(), entry);
                        }
                    }
                    // can have an resourcepool with no entries
                }

            } finally {
                try {
                    if (bufrd != null) {
                        bufrd.close();
                    }
                    if (isr != null) {
                        isr.close();
                    }
                    if (in != null) {
                        in.close();
                    }
                } catch (Exception e) {
                    logger.error("",e);
                }
            }

            resourcepool.setEntries(entries);
            resourcepool.setFileTime(resourcepoolFile.lastModified());
            newPoolSet.put(resourcepoolFile.getName(), resourcepool);

            if (traceLog) {
                logger.debug("read in resourcepool " +
                              resourcepoolFile.getName() + ": " +
                              resourcepool);
            }
        }


        if (previous == null || previous.isEmpty()) {
            return newPoolSet;
        }

        // Now look at previous entries in database.
        // Deletions are OK if they are not in use.  If in use, not much
        // we can do.  When retired and the entry is not there, a warning
        // will trip but that is it.

        final Enumeration en = previous.keys();

        while (en.hasMoreElements()) {

            String poolname = (String) en.nextElement();
            Resourcepool pool = (Resourcepool) previous.get(poolname);
            if (pool == null) {
                throw new ProgrammingError("all resource pools " +
                                    "in the hashmap should be non-null");
            }
            if (newPoolSet.containsKey(poolname)) {
                logChangedPool(poolname,
                               (Resourcepool)newPoolSet.get(poolname),
                               pool);
            } else {
                logger.info("Previously configured pool '" + poolname +
                            "' is not present in the new configuration. " +
                            allPoolStatus(pool));
            }
            
        }

        return newPoolSet;
    }

    // the entire old pool was removed, log what we can
    private static String allPoolStatus(Resourcepool oldpool) {
        final Hashtable oldentries = oldpool.getEntries();

        if (oldentries == null || oldentries.isEmpty()) {
            return "There were no resources in that pool.";
        }

        final StringBuffer buf = new StringBuffer();
        buf.append("Contents: ");
        final Enumeration en = oldentries.elements();
        while (en.hasMoreElements()) {
            ResourcepoolEntry entry = (ResourcepoolEntry)en.nextElement();
            buf.append("host '")
                .append(entry.getHostname())
                .append("' ");
            if (entry.getMemCurrent() != entry.getMemMax()) {
                buf.append("in use ")
                   .append(entry.getMemCurrent())
                   .append("/")
                   .append(entry.getMemMax())
                   .append(" MB");
            } else {
                buf.append("not in use");
            }
            buf.append("; ");
        }
        return buf.toString();
    }

    // look for entries in old pool that are not in newpool
    private static void logChangedPool(String poolname,
                                       Resourcepool newpool,
                                       Resourcepool oldpool)
            throws Exception {

        final Hashtable oldentries = oldpool.getEntries();
        final Enumeration en = oldentries.keys();
        while (en.hasMoreElements()) {
            final String hostname = (String)en.nextElement();
            if (!newpool.getEntries().containsKey(hostname)) {
                String inuse = "";
                final ResourcepoolEntry entry =
                        (ResourcepoolEntry)oldentries.get(hostname);

                if (entry.getMemCurrent() != entry.getMemMax()) {
                    inuse = " Note it is currently in use " +
                            entry.getMemCurrent() + "/" + entry.getMemMax() +
                            " MB.";

                }

                logger.info("Hostname '" + hostname + "' is not present in " +
                        "new configuration, deleted from available " +
                        "resources in pool " + poolname + "." + inuse);
            }
        }
    }

    private static ResourcepoolEntry getPoolEntry(String line,
                                                  Resourcepool oldpool) {

        final ResourcepoolEntry entry = parseResourcepool(line);
        if (entry == null) {
            return null;
        }

        if (oldpool == null) {
            return entry;
        }

        final ResourcepoolEntry oldentry =
             (ResourcepoolEntry) oldpool.getEntries().get(entry.getHostname());
        
        if (oldentry == null) {
            return entry;
        }

        // there was a matching hostname in oldpool

        // If there is no difference between them, proceed.

        if (oldentry.getMemMax() == entry.getMemMax()
                &&
            oldentry.getSupportedAssociations().equals(
                                entry.getSupportedAssociations())) {

            logger.trace("entry in oldpool and newpool the same for " +
                             "vmm: " + entry.getHostname());
            return oldentry;
        }

        if (oldentry.getMemCurrent() == oldentry.getMemMax()) {
            return entry;
        }

        logger.trace("entry in newpool is different for vmm '" +
                         entry.getHostname() + "', it is currently in use");

        final int memInUse = oldentry.getMemMax() - oldentry.getMemCurrent();

        if (memInUse < 0) {
            logger.error("Current memory stored " + oldentry.getMemCurrent() +
                         "MB was larger than max memory " +
                         oldentry.getMemMax() + "?? vmm: " +
                         oldentry.getHostname());
            return null;
        }

        String largerSmaller = "more";
        if (oldentry.getMemMax() > entry.getMemMax()) {
            largerSmaller = "less";
        }

        int newValue = entry.getMemMax() - memInUse;
        if (newValue < 0) {
            newValue = 0;
        }
        entry.setMemCurrent(newValue);

        logger.info("VMM '" + entry.getHostname() + "' is currently in use " +
                    "and its configuration has been changed.  It has " +
                    largerSmaller + " memory in the new configuration.  New " +
                    "capacity is " + entry.getMemMax() +
                    " MB, old capacity was " + oldentry.getMemMax() + " MB. " +
                    memInUse + " MB is currently in use.  New available " +
                    "memory is therefore " + newValue + " MB.");

        return entry;
    }

    private static ResourcepoolEntry parseResourcepool(String line) {

        if (line == null) {
            return null;
        }

        String[] result = line.split("\\s");

        // don't note blank, cosmetic lines
        if (result.length == 0) {
            return null;
        }

        // ignore comments
        String hostname = result[0];
        if (hostname.startsWith(COMMENT_CHAR)) {
            return null;
        }

        if (result.length < 2) {
            logger.error("entry in resourcepool file does not have at " +
                    "least two components, which is currently unsupported" +
                    " -- line = '" + line + "'");
            return null;
        }

        String memory = result[1];
        int mem;
        try {
            mem = Integer.parseInt(memory.trim());
        } catch (NumberFormatException nfe) {
            if (logger.isDebugEnabled()) {
                logger.error("Problem with memory token in resource " +
                    "pool entry '" + line + "': " + nfe.getMessage(), nfe);
            } else {
                logger.error("Problem with memory token in resource " +
                    "pool entry '" + line + "': " + nfe.getMessage());
            }
            return null;
        }

        // supported associations
        String sa;
        if (result.length == 2) {
            sa = "*";
        } else {
            if (result.length > 3) {
                logger.error("supported networks in resource pool " +
                        "entry '" + line + "' appear to not be listed " +
                        "correctly -- you can not use spaces between " +
                        "items in the comma separated list.  Discarding " +
                        "this line.");
                return null;
            }
            sa = result[2];
        }

        // would not be possible here, but just to make this very clear
        // for programmer and doesn't hurt as a sanity check
        if (hostname == null || sa == null) {
            logger.error("hostname and sa should not be null here, " +
                                                            "line: " + line);
            return null;
        }

        return new ResourcepoolEntry(hostname, mem, mem, sa);
    }
}
