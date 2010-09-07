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

package org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.rm.defaults;

import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.rm.IDMappings;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.security.defaults.KeyCacheProvider;

import java.io.IOException;
import java.util.Random;
import java.net.URL;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.Resource;

/**
 * Translations between underlying manager IDs and elastic style.  Since the
 * elastic reservations can technically correspond to either a group or cosched
 * group, they are typed.  You can have duplicate keys of all three types
 * (instance, group, cosched group).
 */
public class DefaultIDMgmt implements IDMappings, KeyCacheProvider {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(DefaultIDMgmt.class.getName());
    
    private static final char[] legalIDchars =
                            {'0', '1', '2', '3', '4', '5', '6', '7',
                             '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    private static final String DISK_PROPKEY =
                                    "ehcache.disk.store.dir";
    private static final String SHUTDOWN_PROPKEY =
                                    "net.sf.ehcache.enableShutdownHook";


    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------
    
    // example instance id:    i-936e83fa
    // example reservation id: r-602bca09
    private final Random random = new Random();
    private final Cache cacheElasticKeys;
    private final Cache cacheManageKeys;
    private final Cache sshKeyCache;


    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    public DefaultIDMgmt(Resource diskStoreResource) throws IOException {

        if (diskStoreResource == null) {
            throw new IllegalArgumentException("diskStoreResource may not be null");
        }

        final String diskStorePath = diskStoreResource.getFile().getAbsolutePath();

        // TODO: Do dynamically. This is problematic for other ehcache+diskstore
        //       users, could not quickly see how to set on per-manager basis
        //       via spring etc.  (specifically, the to-disk part).
        System.setProperty(DISK_PROPKEY, diskStorePath);

        // TODO: We need a shutdown hook with disk-based.  This creates a jvm
        //       shutdown hook and is the least-recommended solution.
        System.setProperty(SHUTDOWN_PROPKEY, "true");

        final URL url = this.getClass().getResource("ehcache.xml");
        final CacheManager cacheManager = new CacheManager(url);
        this.cacheElasticKeys = cacheManager.getCache("nimbus-elasticCache");
        this.cacheManageKeys = cacheManager.getCache("nimbus-elasticCache2");
        this.sshKeyCache = cacheManager.getCache("nimbus-elasticCache3");
    }

    // -------------------------------------------------------------------------
    // implements KeyCacheProvider
    // -------------------------------------------------------------------------

    public Cache getKeyCache() {
        return this.sshKeyCache;
    }
    

    // -------------------------------------------------------------------------
    // NEW ELASTIC IDs
    // -------------------------------------------------------------------------
        
    public synchronized String newInstanceID(String managerInstanceID,
                                             String elasticReservationID,
                                             String sshkeyUsed)
            throws Exception {
        
        if (managerInstanceID == null) {
            throw new Exception("managerInstanceID may not be null");
        }
        if (elasticReservationID == null) {
            throw new Exception("elasticReservationID may not be null");
        }
        final String id = this.newUniqueInstanceID();
        final IDMap map = new IDMap(elasticReservationID, id,
                                    managerInstanceID, sshkeyUsed);
        this.addToCache(id, managerInstanceID, map, IDMap.INST);
        return id;
    }

    public synchronized String newGrouplessInstanceID(String managerInstanceID,
                                                      String sshkeyUsed)
            throws Exception {

        if (managerInstanceID == null) {
            throw new Exception("managerInstanceID may not be null");
        }
        final String id = this.newUniqueInstanceID();
        final String resid = this.newUniqueReservationID();
        final IDMap map = new IDMap(resid, id, managerInstanceID, sshkeyUsed);
        this.addToCache(id, managerInstanceID, map, IDMap.INST);
        return resid;
    }

    public synchronized String newGroupReservationID(String managerGroupID)
            throws Exception {

        if (managerGroupID == null) {
            throw new Exception("managerGroupID may not be null");
        }
        final String resid = this.newUniqueReservationID();
        final IDMap map = new IDMap(resid, managerGroupID, IDMap.GRP);
        this.addToCache(resid, managerGroupID, map, IDMap.GRP);
        return resid;
    }

    public synchronized String newCoschedReservationID(String managerCoschedID)
            throws Exception {

        if (managerCoschedID == null) {
            throw new Exception("managerCoschedID may not be null");
        }
        final String id = this.newUniqueReservationID();
        final IDMap map = new IDMap(id, managerCoschedID, IDMap.COSCH);
        this.addToCache(id, managerCoschedID, map, IDMap.COSCH);
        return id;
    }

    protected synchronized void addToCache(String elastic, String manager,
                                           IDMap map, int type)
            throws Exception {

        if (elastic == null) {
            throw new IllegalArgumentException("elastic may not be null");
        }
        if (manager == null) {
            throw new IllegalArgumentException("manager may not be null");
        }
        if (map == null) {
            throw new IllegalArgumentException("map may not be null");
        }

        if (this.cacheElasticKeys.isKeyInCache(elastic)) {
            final String err = "illegal, already mapped " + elastic;
            logger.fatal(err);
            throw new Exception(err);
        }
        final Element el = new Element(elastic, map);
        this.cacheElasticKeys.put(el);
        this.cacheElasticKeys.flush();

        final String id2 = type + manager;
        if (this.cacheManageKeys.isKeyInCache(id2)) {
            final String err = "illegal, already mapped " + id2;
            logger.fatal(err);
            throw new Exception(err);
        }
        final Element el2 = new Element(id2, map);
        this.cacheManageKeys.put(el2);
        this.cacheManageKeys.flush();
    }


    // -------------------------------------------------------------------------
    // GET-OR-NEW ELASTIC IDs
    // -------------------------------------------------------------------------

    public synchronized String getOrNewInstanceReservationID(String managerInstanceID,
                                                             String sshkeyUsed)
            throws Exception {
        
        if (!this.isElasticInstanceID(managerInstanceID)) {
            this.newGrouplessInstanceID(managerInstanceID, sshkeyUsed);
        }
        return this.managerInstanceToElasticReservation(managerInstanceID);
    }

    public synchronized String getOrNewGroupReservationID(String managerGroupID)
            throws Exception {
        if (this.isElasticReservationIDGroup(managerGroupID)) {
            return this.managerGroupToElasticReservation(managerGroupID);
        } else {
            return this.newGroupReservationID(managerGroupID);
        }
    }

    public synchronized String getOrNewCoschedReservationID(String managerCoschedID)
            throws Exception {
        if (this.isElasticReservationIDCosched(managerCoschedID)) {
            return this.managerCoschedToElasticReservation(managerCoschedID);
        } else {
            return this.newCoschedReservationID(managerCoschedID);
        }
    }

    public synchronized String checkInstanceAndReservation(
                                            String managerInstanceID,
                                            String elasticReservationID)
            throws Exception {

        if (!this.isCoschedReservationID(elasticReservationID)
                && !this.isManagerGroupID(elasticReservationID)) {
            throw new Exception("Expected to, but we do not know about " +
                    "elastic reservation ID '" + elasticReservationID + "'");
        }

        if (this.isElasticInstanceID(managerInstanceID)) {
            // nothing to do
            return this.managerInstanceToElasticInstance(managerInstanceID);
        }

        return this.newInstanceID(managerInstanceID,
                                  elasticReservationID,
                                  null);
    }


    // -------------------------------------------------------------------------
    // ELASTIC INSTANCE --> MANAGER INSTANCE
    // -------------------------------------------------------------------------
        
    public synchronized boolean isInstanceID(String elasticKey) {
        return elasticKey != null &&
                this.cacheElasticKeys.isKeyInCache(elasticKey);
    }

    public synchronized String instanceToManager(String elasticInstanceID) {
        return this.elasticToManager(elasticInstanceID, IDMap.INST);
    }


    // -------------------------------------------------------------------------
    // ELASTIC RESERVATION --> MANAGER GROUP
    // -------------------------------------------------------------------------

    public synchronized boolean isManagerGroupID(String elasticKey) {
        return elasticKey != null &&
                this.cacheElasticKeys.isKeyInCache(elasticKey);
    }

    public synchronized String reservationToManagerGroup(String elasticReservationID) {
        return this.elasticToManager(elasticReservationID, IDMap.GRP);
    }


    // -------------------------------------------------------------------------
    // ELASTIC RESERVATION --> MANAGER COSCHED
    // -------------------------------------------------------------------------

    public synchronized boolean isCoschedReservationID(String elasticKey) {
        return elasticKey != null &&
                this.cacheElasticKeys.isKeyInCache(elasticKey);
    }

    public synchronized String reservationToManagerCosched(String elasticReservationID) {
        return this.elasticToManager(elasticReservationID, IDMap.COSCH);
    }


    // -------------------------------------------------------------------------
    // MANAGER INSTANCE --> ELASTIC INSTANCE
    // -------------------------------------------------------------------------

    public synchronized boolean isElasticInstanceID(String managerKey) {
        return managerKey != null &&
                this.cacheManageKeys.isKeyInCache(IDMap.INST + managerKey);
    }

    public synchronized String managerInstanceToElasticInstance(String managerInstanceID) {
        return this.managerToElastic(managerInstanceID, IDMap.INST);
    }


    // -------------------------------------------------------------------------
    // MANAGER INSTANCE --> ELASTIC RESERVATION
    // -------------------------------------------------------------------------
    
    public synchronized String managerInstanceToElasticReservation(String managerInstanceID) {

        if (managerInstanceID == null) {
            return null;
        }
        final Element el =
                this.cacheManageKeys.get(IDMap.INST + managerInstanceID);
        if (el == null) {
            return null;
        }
        final IDMap map = (IDMap) el.getObjectValue();
        if (map == null) {
            logger.fatal("illegal object extension, no null values allowed");
            return null;
        }
        if (IDMap.INST == map.type) {
            return map.elasticReservationID;
        }
        return null;
    }


    // -------------------------------------------------------------------------
    // MANAGER GROUP --> RESERVATION
    // -------------------------------------------------------------------------

    public synchronized boolean isElasticReservationIDGroup(String managerKey) {
        return managerKey != null &&
                this.cacheManageKeys.isKeyInCache(IDMap.GRP + managerKey);
    }

    public synchronized String managerGroupToElasticReservation(String managerGroupID) {
        return this.managerToElastic(managerGroupID, IDMap.GRP);
    }


    // -------------------------------------------------------------------------
    // MANAGER COSCHED --> RESERVATION
    // -------------------------------------------------------------------------
    
    public synchronized boolean isElasticReservationIDCosched(String managerKey) {
        return managerKey != null &&
                this.cacheManageKeys.isKeyInCache(IDMap.COSCH + managerKey);
    }

    public synchronized String managerCoschedToElasticReservation(String managerCoschedID) {
        return this.managerToElastic(managerCoschedID, IDMap.COSCH);
    }


    // -------------------------------------------------------------------------
    // GENERIC ELASTIC --> MANAGER
    // -------------------------------------------------------------------------

    protected synchronized String elasticToManager(String elastic, int type) {
        if (elastic == null) {
            return null;
        }
        final Element el = this.cacheElasticKeys.get(elastic);
        if (el == null) {
            return null;
        }
        final IDMap map = (IDMap) el.getObjectValue();
        if (map == null) {
            logger.fatal("illegal object extension, no null values allowed");
            return null;
        }
        if (type == map.type) {
            return map.manager;
        }
        return null;
    }


    // -------------------------------------------------------------------------
    // GENERIC MANAGER --> ELASTIC
    // -------------------------------------------------------------------------

    protected synchronized String managerToElastic(String manager, int type) {
        if (manager == null) {
            return null;
        }
        final Element el = this.cacheManageKeys.get(type + manager);
        if (el == null) {
            return null;
        }
        final IDMap map = (IDMap) el.getObjectValue();
        if (map == null) {
            logger.fatal("illegal object extension, no null values allowed");
            return null;
        }
        if (type == map.type) {
            return map.elastic;
        }
        return null;
    }



    // -------------------------------------------------------------------------
    // OTHER
    // -------------------------------------------------------------------------

    /**
     * @param elasticID elastic instance ID
     * @return key name, may be null
     *        (e.g. if VM was not created with this protocol)
     */
    public String getKeyName(String elasticID) {
        if (elasticID == null) {
            return null;
        }
        final Element el = this.cacheElasticKeys.get(elasticID);
        if (el == null) {
            return null;
        }
        final IDMap map = (IDMap) el.getObjectValue();
        if (map == null) {
            logger.fatal("illegal object extension, no null values allowed");
            return null;
        }
        if (map.type != IDMap.INST) {
            return null;
        }
        return map.sshkeyUsed;
    }

    
    
    // -------------------------------------------------------------------------
    // ID GENERATOR
    // -------------------------------------------------------------------------

    /**
     * Returns new, unique instance ID.  Assuming cache locking implemented
     * by caller.
     *
     * @return new ID, never null
     * @throws Exception could not obtain unique ID
     */
    protected synchronized String newUniqueInstanceID() throws Exception {
        return this.newUniqueID(true);
    }

    /**
     * Returns new, unique reservation ID.  Assuming cache locking implemented
     * by caller.
     *
     * @return new ID, never null
     * @throws Exception could not obtain unique ID
     */
    protected synchronized String newUniqueReservationID() throws Exception {
        return this.newUniqueID(false);
    }
    
    protected synchronized String newUniqueID(boolean instance)
            throws Exception {

        final int TRIES = 512;
        String id = null;
        for (int i = 0; i < TRIES; i++) {
            id = this.randomID(instance);
            if (this.cacheElasticKeys.isKeyInCache(id)) {
                id = null; // keep looking
            } else {
                break; // found unique
            }
        }

        if (id == null) {
            throw new Exception("VERY special error.  Could not obtain " +
                    "unique ID after " + TRIES + " tries. Aborting.");
        }
        return id;
    }

    protected String randomID(boolean instance) {
        final char[] charArray = new char[10];
        if (instance) {
            charArray[0] = 'i';
        } else {
            charArray[0] = 'r';
        }
        charArray[1] = '-';
        for (int i = 2; i < 10; i++) {
            charArray[i] = legalIDchars[this.random.nextInt(16)];
        }
        return new String(charArray);
    }
}
