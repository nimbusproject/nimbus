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

import org.globus.workspace.network.Association;
import org.globus.workspace.network.AssociationEntry;
import org.nimbustools.api.services.rm.ResourceRequestDeniedException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

public class MacUtil {

    private static final Log logger =
        LogFactory.getLog(MacUtil.class.getName());


    public static final String VALID_MAC_CHARACTERS = "0123456789ABCDEF";

    public static final char[] MAC_ARRAY = VALID_MAC_CHARACTERS.toCharArray();

    private static final Object list_lock = new Object();

    private static final Random random = new Random();
    
    
    public static List<String> handleMacs(Map<String, Association> previous,
                                          Map<String, Association> current,
                                          String macPrefix)
            throws ResourceRequestDeniedException {
        if (previous == null) {
            throw new IllegalArgumentException("previous may not be null");
        }
        if (current == null) {
            throw new IllegalArgumentException("current may not be null");
        }
        if (macPrefix == null) {
            throw new IllegalArgumentException("macPrefix may not be null");
        }

        final Map<String, AssociationEntry> inUse = getInUseMacMap(previous);


        final Set<String> explicit = new HashSet<String>();
        for (Map.Entry<String, Association> assocPair : current.entrySet()) {
            final String assocName = assocPair.getKey();
            final Association assoc = assocPair.getValue();
            if (assoc == null || assoc.getEntries() == null) {
                continue;
            }
            for (Object entryObject : assoc.getEntries()) {
                final AssociationEntry entry = (AssociationEntry) entryObject;
                if (entry.isExplicitMac()) {
                    final AssociationEntry inUseEntry = inUse.get(entry.getMac());
                    if (inUseEntry != null) {
                        final String inUseIp = inUseEntry.getIpAddress();
                        if (inUseIp == null || !inUseIp.equals(entry.getIpAddress())) {
                            logger.error("An explicit MAC in network '"+assocName+
                                    "' collides with an in-use network entry! "+
                                    "Explicit: "+ entry.toString() + "\nIn use: "+
                                    inUseEntry.toString());
                            entry.setMac(null);
                            entry.setExplicitMac(false);
                        }

                    }

                }
                if (entry.isExplicitMac()) {
                    if (!explicit.add(entry.getMac())) {
                        logger.warn("Duplicate explicit MAC? "+ entry.getMac());
                    }
                }
            }
        }

        final List<String> macList =
                new ArrayList<String>();
        macList.addAll(inUse.keySet());
        macList.addAll(explicit);

        for (Association assoc : current.values()) {
            for (Object entryObject : assoc.getEntries()) {
                final AssociationEntry entry = (AssociationEntry) entryObject;
                _setMac(entry, macPrefix, macList, explicit);
            }
        }

        return macList;
    }

    private static Map<String, AssociationEntry> getInUseMacMap(Map<String, Association> previous) {
        Map<String, AssociationEntry> inUse = new HashMap<String, AssociationEntry>();
        for (Association assoc : previous.values()) {
            for (Object entryObject : assoc.getEntries()) {
                final AssociationEntry entry = (AssociationEntry) entryObject;
                if (entry.isInUse()) {
                    final String mac = entry.getMac();
                    if (mac == null || mac.length() == 0) {
                        logger.warn("Network association entry is supposedly " +
                                "in use but has no MAC address..? "+ entry.toString());
                    }
                    final AssociationEntry clobbered = inUse.put(mac, entry);
                    if (clobbered != null) {
                        logger.warn("There appear to be duplicate MAC addresses in use: "+
                                clobbered.toString() + "\nand\n"+entry.toString());
                    }
                }
            }
        }
        return inUse;
    }
    /*
    public static void setMacs(Hashtable new_associations,
                               String macPrefix,
                               List macs)
            
            throws ResourceRequestDeniedException {

        if (macPrefix == null) {
            throw new IllegalArgumentException("macPrefix may not be null");
        }

        if (new_associations == null) {
            return; // *** EARLY RETURN ***
        }

        final Enumeration els = new_associations.elements();

        while (els.hasMoreElements()) {
            final Association assoc = (Association) els.nextElement();
            final List entries = assoc.getEntries();
            for (Object entry : entries) {
                _setMac((AssociationEntry) entry, macPrefix, macs);
            }
        }
    } */

    private static void _setMac(AssociationEntry entry,
                                String macPrefix,
                                List<String> macs, 
                                Set<String> explicit)
            throws ResourceRequestDeniedException {
        
        if (entry == null) {
            return; // *** EARLY RETURN ***
        }

        final String entryId = "[entry with ip " + entry.getIpAddress() + "]";

        final String previousMac = entry.getMac();
        boolean prefixReplacement = false;
        boolean explicitReplacement = false;
        if (previousMac != null) {

            if (entry.isExplicitMac()) {
                return; // *** EARLY RETURN ***
            }

            if (explicit.contains(previousMac)) {
                explicitReplacement = true;
            } else if (previousMac.startsWith(macPrefix)) {
                return; // *** EARLY RETURN ***
            } else {
               prefixReplacement = true;
            }
        }

        final String newMac = pickNew(macs, macPrefix);
        entry.setMac(newMac);

        if (prefixReplacement) {
            logger.debug("Replaced previous MAC '" + previousMac + "' with " +
                    "a MAC that has the new prefix: '" + newMac + "' " + entryId);
        } else if (explicitReplacement) {
            logger.warn("Replaced previous MAC '" + previousMac + "' with '" +
                    newMac + "' because it collided with an explicitly specified MAC. " +
                    entryId);
        }

    }

    static String pickNew(List macs, String macPrefix)

            throws ResourceRequestDeniedException {

        final int needed;
        final int len = macPrefix.length();
        switch (len) {
            case 0: throw new ResourceRequestDeniedException("prefix length 0?");
            case 1:  needed = 11; break;
            case 2:  needed = 10; break;
            case 3:  needed = 10; break;
            case 4:  needed = 9;  break;
            case 5:  needed = 8;  break;
            case 6:  needed = 8;  break;
            case 7:  needed = 7;  break;
            case 8:  needed = 6;  break;
            case 9:  needed = 6;  break;
            case 10: needed = 5;  break;
            case 11: needed = 4;  break;
            case 12: needed = 4;  break;
            case 13: needed = 3;  break;
            case 14: needed = 2;  break;
            case 15: needed = 2;  break;
            case 16: needed = 1;  break;
            case 17: needed = 0;  break;
            default: throw new ResourceRequestDeniedException("prefix length >17?");
        }

        if (needed == 0) {
            logger.warn("prefix is full MAC address, conflict detection OFF");
            return macPrefix;
        }

        synchronized (list_lock) {

            final String result;
            if (needed < 4) {
                // slow, but tries every single permutation
                result = _pickNewSerially(macs, macPrefix, needed);
            } else {
                // Faster.  Does not get at every single permutation but is very
                // unlikely to fail when address range is 16^4 or higher
                result = _pickNewWithRandomComponent(macs, macPrefix, needed);
            }

            if (result == null) {
                throw new ResourceRequestDeniedException(
                        "no unique MAC address is available");
            } else {
                macs.add(result);
                return result;
            }
        }
    }

    // slow, but tries every single permutation
    private static String _pickNewSerially(List macs,
                                           String prefix,
                                           int needed) {

        if (needed == 1) {
            for (int i = 0; i < MAC_ARRAY.length; i++) {
                final String attempt = prefix + MAC_ARRAY[i];
                if(uniqueMacTest(macs, attempt)) {
                    return attempt;
                }
            }
            return null;
        }

        final int L = prefix.length();
        if (L == 2 || L == 5 || L == 8 || L == 11 || L == 14) {
            prefix += ":";
        }

        for (int i = 0; i < MAC_ARRAY.length; i++) {
            final String answer = _pickNewSerially(macs,
                                                   prefix + MAC_ARRAY[i],
                                                   needed-1);
            if (answer != null) {
                return answer;
            }
        }
        
        return null;
    }

    private static boolean uniqueMacTest(List macs, String attempt) {
        for (Object mac1 : macs) {
            final String mac = (String) mac1;
            if (attempt.equals(mac)) {
                return false;
            }
        }
        return true;
    }

    // This is much faster than _pickNewSerially(), especially as the number
    // of uniques stored in the "macs" List increases into the 100s/1000s.
    // This will not get at every variation but in practice when a prefix is
    // short enough (more digits needed), this does not matter (unless setup
    // actually requires many many thousands of uniques at one time).
    private static String _pickNewWithRandomComponent(List macs,
                                                      String macPrefix,
                                                      long needed)
            throws ResourceRequestDeniedException {

        final long limit = (long) StrictMath.pow(16, needed);

        long count = 0;
        while (true) {

            if (count >= limit) {
                // Even at lowest allowable limit (~65000), this is very
                // unlikely to ever occur.
                throw new ResourceRequestDeniedException(
                    "Search limit reached (" + count +
                    ") looking for MAC to assign.  Please inform developers.");
            }

            String attempt = appendRandomCharacter(macPrefix);
            while (attempt.length() < 17) {
                attempt = appendRandomCharacter(attempt);
            }

            if(uniqueMacTest(macs, attempt)) {
                return attempt;
            } else {
                count += 1;
            }
        }
    }

    private static String appendRandomCharacter(String sofar) {
        final int L = sofar.length();
        String newString = sofar;
        if (L == 2 || L == 5 || L == 8 || L == 11 || L == 14) {
            newString += ":";
        }
        newString += randomChar();
        return newString;
    }

    private static char randomChar() {
        return MAC_ARRAY[random.nextInt(MAC_ARRAY.length)];
    }

    public static void main(String[] args) throws Exception {

        final long mstart = System.currentTimeMillis();
        final List macs = new LinkedList();
        for (int i = 0; i < 4096; i++) {
            final String result = pickNew(macs, "AB:CD:12:34:");
            if (result == null) {
                System.out.println("RAN OUT");
                break;
            } else {
                System.out.println(result);
            }
        }
        final long mstop = System.currentTimeMillis();
        System.out.println("ELAPSED: " + Long.toString(mstop - mstart) + "ms");
    }

    public static boolean isValidMac(String mac, boolean prefixOk) {
        if (mac == null) {
            throw new IllegalArgumentException("mac may not be null");
        }

        if (!prefixOk && mac.length() != 17) {
            throw new IllegalArgumentException("MAC must be 17 characters long");
        } else if (mac.length() > 17) {
            throw new IllegalArgumentException("MAC length cannot be more than 17 characters");
        }

        mac = mac.toUpperCase();
        final char[] macChars = mac.toCharArray();

        for (int i = 0; i < macChars.length; i++) {
            boolean thisOneOK = false;
            boolean expectedSeparator = false;

            if (i == 2 || i == 5 || i == 8 || i == 11 || i == 14) {
                if (':' == macChars[i]) {
                    thisOneOK = true;
                }
                expectedSeparator = true;
            } else {
                for (int j = 0; j < MAC_ARRAY.length; j++) {
                    if (MAC_ARRAY[j] == macChars[i]) {
                        thisOneOK = true;
                        break;
                    }
                }
            }
            if (!thisOneOK) {

                final String tail;
                if (expectedSeparator) {
                    tail = " (expected separator ':')" ;
                } else {
                    tail = " (expected hex character)" ;
                }

                logger.warn("Invalid character in MAC (" +
                        mac + "): '" + macChars[i] + "'" + tail);
                return false;
            }
        }
        return true;
    }
}
