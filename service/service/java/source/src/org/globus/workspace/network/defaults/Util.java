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

package org.globus.workspace.network.defaults;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.workspace.ProgrammingError;
import org.globus.workspace.Lager;
import org.globus.workspace.network.AssociationEntry;
import org.globus.workspace.network.Association;
import org.globus.workspace.persistence.PersistenceAdapter;
import org.globus.workspace.persistence.WorkspaceDatabaseException;
import org.nimbustools.api.services.rm.ResourceRequestDeniedException;
import org.nimbustools.api.services.rm.ManageException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Enumeration;

public class Util {

    private static final Log logger =
        LogFactory.getLog(Util.class.getName());

    private static final String NOENTRY = "none";
    private static final String COMMENT_CHAR = "#";

    /**
     * @param name network name
     * @param db db
     * @param vmid for logging
     * @param eventLog for logging
     * @return Object[] length 2
     *             AssociationEntry [0]
     *             String dns setting [1]
     * @throws ResourceRequestDeniedException denial
     */
    static Object[] getNextEntry(String name,
                                 PersistenceAdapter db,
                                 int vmid,
                                 boolean eventLog)

            throws ResourceRequestDeniedException {

        if (db == null) {
            throw new IllegalArgumentException("null persistence adapter");
        }

        final Hashtable associations;
        try {
            associations = db.currentAssociations();
        } catch (WorkspaceDatabaseException e) {
            logger.fatal(e.getMessage(), e);
            throw new ResourceRequestDeniedException(
                    "internal error, db problem");
        }
        
        final Object[] entryAndDns = nextAssociationEntry(name,
                                                          associations);

        final AssociationEntry entry;
        if (entryAndDns == null || entryAndDns[0] == null) {
            final String err = "network '" + name
                        + "' is not currently available";
            logger.error(err);
            throw new ResourceRequestDeniedException(err);
        } else {
            entry = (AssociationEntry) entryAndDns[0];
            logger.debug("entry picked = " + entry);
        }

        try {
            db.replaceAssociationEntry(name, entry);
        } catch (WorkspaceDatabaseException e) {
            logger.fatal(e.getMessage(), e);
            throw new ResourceRequestDeniedException(
                    "internal error, db problem");
        }

        if (eventLog) {
            logger.info(Lager.ev(vmid) + "'" + name + "' network " +
                            "entry leased, ip=" + entry.getIpAddress());
        }

        return entryAndDns;
    }

    static void retireEntry(String name,
                            String ipAddress,
                            PersistenceAdapter db,
                            int trackingID)

            throws ManageException {

        if (db == null) {
            throw new IllegalArgumentException("null persistence adapter");
        }

        final Hashtable associations = db.currentAssociations();
        final Association assoc = (Association)associations.get(name);
        if (assoc == null) {
            logger.error("no network '" + name + "'");
            return;
        }

        final List entries = assoc.getEntries();
        if (entries == null || entries.isEmpty()) {
            logger.error(Lager.id(trackingID) +
                    " network '" + name + "' has no entries");
            return;
        }

        // evidence for storing the entry list as an IP keyed hashtable...
        final Iterator iter = entries.iterator();
        AssociationEntry entry = null;
        boolean found = false;
        while (!found && iter.hasNext()) {
            entry = (AssociationEntry)iter.next();
            if (entry.getIpAddress().equals(ipAddress.trim())) {
                found = true;
            }
        }

        if (!found) {
            throw new ManageException(Lager.id(trackingID) + " entry was " +
                    "not found in '" + name + "': " + ipAddress);
        }
        entry.setInUse(false);
        db.replaceAssociationEntry(name, entry);

        logger.info(Lager.ev(trackingID) + "'" + name + "' network lease " +
                    "is over, ip=" + entry.getIpAddress());
    }


    private static Object[] nextAssociationEntry(String name,
                                                 Hashtable associations)
            throws ResourceRequestDeniedException {

        final Association assoc = (Association)associations.get(name);
        if (assoc == null) {
            final String err = "'" + name + "' is not a valid network name";
            logger.error(err);
            throw new ResourceRequestDeniedException(err);
        }

        final List entries = assoc.getEntries();
        if (entries == null || entries.isEmpty()) {
            return null; // *** EARLY RETURN ***
        }

        final Iterator iter = assoc.getEntries().iterator();
        AssociationEntry entry = null;
        while (iter.hasNext()) {
            entry = (AssociationEntry)iter.next();
            if (!entry.isInUse()) {
                entry.setInUse(true);
                break;
            }
            entry = null;
        }

        if (entry == null) {
            return null; // *** EARLY RETURN ***
        }
            
        final Object[] objs = new Object[2];

        objs[0] = entry;

        final String DNS = assoc.getDns();
        if (DNS == null || DNS.equalsIgnoreCase("none")) {
            objs[1] = "null";
        } else {
            objs[1] = DNS;
        }
        return objs;
    }

    /**
     * @param associationDir association directory, may not be null
     * @param previous previous entries
     * @return updated entries
     * @throws Exception problem
     */
    static Hashtable loadDirectory(File associationDir,
                                   Hashtable previous) throws Exception {

        if (associationDir == null) {
            throw new IllegalArgumentException("null associationDir");
        }

        final String[] listing = associationDir.list();

        if (listing == null) {
            // null return from list() is different than zero results, it denotes a real problem
            throw new Exception("Problem listing contents of directory '" +
                    associationDir.getAbsolutePath() + '\'');
        }

        final Hashtable newAssocSet = new Hashtable(listing.length);
        
        for (int i = 0; i < listing.length; i++) {

            final String path = associationDir.getAbsolutePath()
                                + File.separator + listing[i];

            final File associationFile = new File(path);

            if (!associationFile.isFile()) {
                logger.warn("not a file: '" + path + "'");
                continue;
            }

            final String assocName = associationFile.getName();

            Association oldassoc = null;
            if (previous != null) {
                oldassoc = (Association) previous.get(assocName);

                // skip reading if file modification time isn't newer than last
                // container boot
                if (oldassoc != null) {
                    if (oldassoc.getFileTime() ==
                            associationFile.lastModified()) {

                        logger.info("file modification time for network '"
                                + assocName
                                + "' is not newer, using old configuration");

                        newAssocSet.put(assocName,
                                        oldassoc);

                        continue;
                    }
                }
            }

            final Association newassoc =
                            getNewAssoc(assocName, associationFile, oldassoc);

            if (newassoc != null) {
                newAssocSet.put(assocName, newassoc);
            }
        }

        if (previous == null || previous.isEmpty()) {
            return newAssocSet;
        }

        // Now look at previous entries in database for entries that were
        // there and now entirely gone.
        // If in use, we don't do anything.  When retired and the entry is
        // not in DB, a warning will trip but that is it.  From then on, the
        // address will be gone.

        final Enumeration en = previous.keys();

        while (en.hasMoreElements()) {

            final String assocname = (String) en.nextElement();
            final Association oldassoc = (Association) previous.get(assocname);
            if (oldassoc == null) {
                throw new ProgrammingError("all networks " +
                                    "in the hashmap should be non-null");
            }
            if (newAssocSet.containsKey(assocname)) {
                logChangedAssoc(assocname,
                                (Association)newAssocSet.get(assocname),
                                oldassoc);
            } else {
                logger.info("Previously configured network '" + assocname +
                            "' is not present in the new configuration. " +
                            goneStatus(oldassoc));
            }
        }

        return newAssocSet;
    }

    // Parses a new association, if one with same name existed before,
    // this compares the two.
    // If an entry existed with the same IP address, the entry is reconfigured
    // entirely from the new file's information.  If the entry is currently
    // in use this is recorded.
    private static Association getNewAssoc(String assocName,
                                           File file,
                                           Association oldassoc)
            throws IOException {

        final Association assoc = loadOne(file);
        if (assoc == null) {
            return null;
        }

        if (oldassoc == null) {
            return assoc;
        }

        final List assocEntries = assoc.getEntries();
        if (assocEntries == null || assocEntries.isEmpty()) {
            // no conflicts are possible
            return assoc;
        }

        final List oldassocEntries = oldassoc.getEntries();
        if (oldassocEntries == null || oldassocEntries.isEmpty()) {
            return assoc;
        }

        if (assoc.getDns() != null && !assoc.getDns().equals(oldassoc.getDns())) {
            logger.info("Network '" + assocName + "': DNS changed from " +
                        oldassoc.getDns() + " to " + assoc.getDns());
        }

        final Iterator iter = assocEntries.iterator();
        while (iter.hasNext()) {
            final AssociationEntry entry = (AssociationEntry) iter.next();
            final AssociationEntry oldentry =
                                     getMatchingIpEntry(entry.getIpAddress(),
                                                        oldassocEntries);

            if (oldentry == null) {
                continue;
            }

            logDifferences(assocName, entry, oldentry);

            // Any change is OK.
            // We know it has same IP address and that is enough to retire
            // with.  But the in-use flag MUST match the old one.
            entry.setInUse(oldentry.isInUse());

            if (entry.isInUse()) {
                logger.debug("Network '" + assocName + "', ip " + 
                             entry.getIpAddress() + " is currently in use.");
            }
        }
        return assoc;
    }

    private static AssociationEntry getMatchingIpEntry(String ip,
                                                       List entries) {

        if (ip == null) {
            throw new IllegalArgumentException("ip is null");
        }

        final Iterator iter = entries.iterator();
        while (iter.hasNext()) {
            final AssociationEntry entry = (AssociationEntry) iter.next();
            if (ip.equals(entry.getIpAddress())) {
                return entry;
            }

        }
        return null;
    }

    private static void logDifferences(String assocName,
                                       AssociationEntry entry,
                                       AssociationEntry oldentry) {

        boolean same = true;
        final StringBuffer buf = new StringBuffer("Network '");
        buf.append(assocName)
           .append("', ip ")
           .append(entry.getIpAddress())
           .append(": has differences in new configuration. ");

        if (diffErator(buf,
                       "hostname",
                       entry.getHostname(),
                       oldentry.getHostname())) {
            same = false;
        }

        if (diffErator(buf,
                       "gateway",
                       entry.getGateway(),
                       oldentry.getGateway())) {
            same = false;
            buf.append("; ");
        }

        if (diffErator(buf,
                       "netmask",
                       entry.getSubnetMask(),
                       oldentry.getSubnetMask())) {
            same = false;
            buf.append("; ");
        }

        if (diffErator(buf,
                       "broadcast",
                       entry.getBroadcast(),
                       oldentry.getBroadcast())) {
            same = false;
            buf.append("; ");
        }

        if (!same) {
            logger.info(buf.toString());
        }
    }

    // adds string explanation of any difference, returns true if different
    private static boolean diffErator(StringBuffer buf,
                                      String fieldname,
                                      String newval,
                                      String oldval) {

        if (oldval == null && newval == null) {
            return false;
        }

        if (oldval != null && newval != null) {
            if (oldval.equals(newval)) {
                return false;
            }
        }

        String oldstr = "none";
        String newstr = "none";

        if (oldval != null) {
            oldstr = "'" + oldval + "'";
        }

        if (newval != null) {
            newstr = "'" + newval + "'";
        }

        buf.append(fieldname)
           .append(" ")
           .append(oldstr)
           .append("-->")
           .append(newstr)
           .append("; ");

        return true;
    }
    
    private static Association loadOne(File file)
            
            throws IOException {

        Association association = null;
        final List associationList = new LinkedList();

        // Read the contents of file

        InputStream in = null;
        InputStreamReader isr = null;
        BufferedReader bufrd = null;
        String line;
        try {
            in = new FileInputStream(file);
            isr = new InputStreamReader(in);
            bufrd = new BufferedReader(isr);

            line = bufrd.readLine();
            if (line != null) {
                // find first non-comment, non-empty line
                boolean notfound = true;
                while (notfound) {
                    try {
                        association = parseDNS(line);
                        notfound = false;
                    } catch (Exception e) {
                        line = bufrd.readLine();
                        if (line == null) {
                            association = null;
                            break;
                        }
                    }
                }
                if (association == null) {
                    logger.error("DNS information incorrectly" +
                            " specified, skipping entire network. " +
                            "Path: " + file.getAbsolutePath());
                    return null;
                }

            } else {
                logger.warn("network file '" + file.getAbsolutePath() +
                                "' is empty, skipping");
                return null;
            }

            // the rest
            while ((line = bufrd.readLine()) != null) {
                line = line.trim();
                if (line.length() > 0) {
                    AssociationEntry entry = parseAssoc(line);
                    if (entry != null) {
                        associationList.add(entry);
                    }
                }
                // can have an association with no entries
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

        association.setEntries(associationList);
        association.setFileTime(file.lastModified());

        return association;
    }

    private static Association parseDNS(String line)
                    throws CommentException, BlankLineException {
        if (line == null) {
            return null;
        }

        final StringTokenizer st = new StringTokenizer(line);
        if (st.countTokens() == 0) {
            throw new BlankLineException();
        }

        final String dns = st.nextToken().trim();

        if (dns.startsWith(COMMENT_CHAR)) {
            throw new CommentException();
        }

        if (dns.equals(NOENTRY)) {
            return new Association(null);
        } else {
            return new Association(dns);
        }
    }

    private static AssociationEntry parseAssoc(String line) {

        if (line == null) {
            return null;
        }

        final StringTokenizer st = new StringTokenizer(line);

        // don't note blank, cosmetic lines
        if (st.countTokens() == 0) {
            return null;
        }

        // ignore comments
        final String hostname = st.nextToken().trim();
        if (hostname.startsWith(COMMENT_CHAR)) {
            return null;
        }

        // for now we do not handle anything but the full syntax
        // full syntax is actually 5 tokens, but we already got one above
        // to check for comments
        if (st.countTokens() != 4) {
            logger.error("entry in network file does not have " +
                    "five components, which is currently unsupported" +
                    " -- line = '" + line + "'");
            return null;
        }

        if (hostname.equals(NOENTRY)) {
            logger.error("network entry must contain hostname" +
                    " in first position -- line = '" + line + "'");
            return null;
        }
        final String ipaddress = st.nextToken().trim();
        if (ipaddress.equals(NOENTRY)) {
            logger.error("network entry must contain IP" +
                    " address in second position -- line = '" + line + "'");
            return null;
        }
        String gateway = st.nextToken().trim();
        if (gateway.equals(NOENTRY)) {
            // perhaps they do not need other network access
            gateway = null;
        }
        String broadcast = st.nextToken().trim();
        if (broadcast.equals(NOENTRY)) {
            broadcast = null;
        }
        String subnetmask = st.nextToken().trim();
        if (subnetmask.equals(NOENTRY)) {
            subnetmask = null;
        }

        // mac is not set from network file
        return new AssociationEntry(ipaddress, null, hostname,
                                    gateway, broadcast, subnetmask);

    }


    private static void logChangedAssoc(String assocname,
                                        Association newassoc,
                                        Association oldassoc) {

        final List oldentries = oldassoc.getEntries();
        final List newentries = newassoc.getEntries();
        final Iterator oldEntriesIter = oldentries.iterator();
        while (oldEntriesIter.hasNext()) {
            final AssociationEntry oldentry =
                        (AssociationEntry) oldEntriesIter.next();

            boolean foundOldEntry = false;

            final Iterator newEntriesIter = newentries.iterator();
            while (newEntriesIter.hasNext()) {
                final AssociationEntry newentry =
                            (AssociationEntry) newEntriesIter.next();
                if (oldentry.getIpAddress().equals(newentry.getIpAddress())) {
                    foundOldEntry = true;
                    break;
                }
            }

            if (!foundOldEntry) {
                String inuse = "";
                if (oldentry.isInUse()) {
                    inuse = " Note it is currently in use.";
                }
                logger.info("IP '" + oldentry.getIpAddress() +
                        "' is not present in the new " +
                        "configuration for network '" + assocname +
                        "'.  Deleted from available " +
                        "addresses in address pool." + inuse);
            }
        }
    }

    // the entire old pool was removed, log what we can
    private static String goneStatus(Association oldassoc) {

        final List oldentries = oldassoc.getEntries();

        if (oldentries == null || oldentries.isEmpty()) {
            return "There were no addresses in that network.";
        }

        final StringBuffer buf = new StringBuffer("Contents: ");
        final Iterator oldEntriesIter = oldentries.iterator();
        while (oldEntriesIter.hasNext()) {
            final AssociationEntry entry =
                    (AssociationEntry) oldEntriesIter.next();
            buf.append("ip '")
               .append(entry.getIpAddress())
               .append("' host '")
               .append(entry.getHostname())
               .append("' inuse? ")
               .append(entry.isInUse())
               .append("; ");
        }
        return buf.toString();
    }

    private static class CommentException extends Exception {}

    private static class BlankLineException extends Exception {}
}
