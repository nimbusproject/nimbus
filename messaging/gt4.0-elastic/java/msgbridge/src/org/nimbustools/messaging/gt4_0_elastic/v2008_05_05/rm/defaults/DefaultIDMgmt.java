/*
 * Copyright 1999-2011 University of Chicago
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.general.ElasticPersistence;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.rm.IDMappings;

import java.util.Random;

public class DefaultIDMgmt implements IDMappings {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(DefaultIDMgmt.class.getName());

    private static final char[] legalIDchars =
                            {'0', '1', '2', '3', '4', '5', '6', '7',
                             '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};


    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    // example instance id:    i-936e83fa
    // example reservation id: r-602bca09
    private final Random random = new Random();
    private final ElasticPersistence persistence;


    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    public DefaultIDMgmt(ElasticPersistence persistence) {
        this.persistence = persistence;
    }

    /**
     * Returns new, unique instance elastic ID for the instance ID
     *
     * @param managerInstanceID    Manager instance ID
     * @param elasticReservationID already created elastic resid
     * @param sshkeyUsed           nickname of key customization task, may be null
     * @return new instance ID
     * @throws Exception could not obtain ID
     */
    public synchronized String newInstanceID(String managerInstanceID,
                                String elasticReservationID,
                                String sshkeyUsed) throws Exception {

        if (managerInstanceID == null) {
            throw new Exception("managerInstanceID may not be null");
        }
        if (elasticReservationID == null) {
            throw new Exception("elasticReservationID may not be null");
        }
        final String id = this.newUniqueInstanceID();

        persistence.insertInstance(id, managerInstanceID, elasticReservationID, sshkeyUsed);

        return id;
    }

    /**
     * Returns new, unique reservation elastic ID for the instance ID when
     * there is no outside group to make a reservation ID from
     *
     * @param managerInstanceID Manager instance ID
     * @param sshkeyUsed        nickname of key customization task, may be null
     * @return new reservation ID
     * @throws Exception could not obtain ID
     */
    public synchronized String newGrouplessInstanceID(String managerInstanceID,
                                         String sshkeyUsed) throws Exception {
        if (managerInstanceID == null) {
            throw new Exception("managerInstanceID may not be null");
        }
        final String id = this.newUniqueInstanceID();
        final String resid = this.newUniqueReservationID();
        persistence.insertReservation(resid, null, null);
        persistence.insertInstance(id, managerInstanceID, resid, sshkeyUsed);
        return resid;
    }

    /**
     * Returns new, unique reservation elastic ID for a group ID
     *
     * @param managerGroupID Manager group ID
     * @return new reservation ID
     * @throws Exception could not obtain ID
     */
    public synchronized String newGroupReservationID(String managerGroupID) throws Exception {
        if (managerGroupID == null) {
            throw new Exception("managerGroupID may not be null");
        }
        final String resid = this.newUniqueReservationID();
        persistence.insertReservation(resid, managerGroupID, null);

        return resid;
    }

    /**
     * Returns new, unique reservation elastic ID for a cosched ID
     *
     * @param managerCoschedID Manager cosched ID
     * @return new reservation ID
     * @throws Exception could not obtain ID
     */
    public synchronized String newCoschedReservationID(String managerCoschedID) throws Exception {
        if (managerCoschedID == null) {
            throw new Exception("managerCoschedID may not be null");
        }
        final String resid = this.newUniqueReservationID();
        persistence.insertReservation(resid, null, managerCoschedID);

        return resid;
    }

   // -------------------------------------------------------------------------
    // GET-OR-NEW ELASTIC IDs
    // -------------------------------------------------------------------------

    public synchronized String getOrNewInstanceID(String managerInstanceID,
                                             String elasticReservationID,
                                             String sshkeyUsed) throws Exception {
        if (!this.isElasticInstanceID(managerInstanceID)) {
            return this.newInstanceID(managerInstanceID, elasticReservationID, sshkeyUsed);
        }
        return this.managerInstanceToElasticInstance(managerInstanceID);
    }

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

    /**
     * @param elasticKey some elastic key
     * @return true if this key has a corresponding Manager instance ID
     */
    public boolean isInstanceID(String elasticKey) {
        return this.instanceToManager(elasticKey) != null;
    }

    /**
     * Returns manager ID for the elastic instance ID
     *
     * @param elasticInstanceID elastic instance ID
     * @return Manager instance ID or null if unknown
     */
    public String instanceToManager(String elasticInstanceID) {
        if (elasticInstanceID == null) {
            throw new IllegalArgumentException("elasticInstanceID may not be null");
        }
        return persistence.selectIdFromId(
                ElasticPersistence.GET_MANAGER_FROM_ELASTIC_INSTANCE,
                elasticInstanceID);
    }

    /**
     * @param elasticKey some elastic key
     * @return true if this key has a corresponding Manager group ID
     */
    public boolean isManagerGroupID(String elasticKey) {
        return this.reservationToManagerGroup(elasticKey) != null;
    }

    /**
     * Returns Manager group ID for a reservation ID
     *
     * @param elasticReservationID elastic reservation ID
     * @return Manager group ID or null if unknown
     */
    public String reservationToManagerGroup(String elasticReservationID) {
        if (elasticReservationID == null) {
            throw new IllegalArgumentException("elasticReservationID may not be null");
        }
        return persistence.selectIdFromId(
                ElasticPersistence.GET_GROUP_FROM_ELASTIC_RESERVATION,
                elasticReservationID);
    }

    /**
     * @param elasticKey some elastic key
     * @return true if this key has a corresponding Manager cosched ID
     */
    public boolean isCoschedReservationID(String elasticKey) {
        return this.reservationToManagerCosched(elasticKey) != null;
    }

    /**
     * Returns Manager cosched ID for a reservation ID
     *
     * @param elasticReservationID elastic reservation ID
     * @return Manager cosched ID or null if unknown
     */
    public String reservationToManagerCosched(String elasticReservationID) {
        if (elasticReservationID == null) {
            throw new IllegalArgumentException("elasticReservationID may not be null");
        }
        return persistence.selectIdFromId(
                ElasticPersistence.GET_COSCHED_FROM_ELASTIC_RESERVATION,
                elasticReservationID);
    }

    /**
     * @param managerKey manager key
     * @return true if this key has a corresponding elastic instance ID
     */
    public boolean isElasticInstanceID(String managerKey) {
        return managerInstanceToElasticInstance(managerKey) != null;
    }

    /**
     * Returns elastic ID for the manager instance ID
     *
     * @param managerInstanceID manager instance ID
     * @return elastic instance ID or null if unknown
     */
    public String managerInstanceToElasticInstance(String managerInstanceID) {
        if (managerInstanceID == null) {
            throw new IllegalArgumentException("managerInstanceID may not be null");
        }
        return persistence.selectIdFromId(
                ElasticPersistence.GET_ELASTIC_FROM_MANAGER_INSTANCE,
                managerInstanceID);
    }

    /**
     * Returns elastic reservation ID for the manager instance ID
     *
     * @param managerInstanceID manager instance ID
     * @return elastic instance ID or null if unknown
     */
    public String managerInstanceToElasticReservation(String managerInstanceID) {
        if (managerInstanceID == null) {
            throw new IllegalArgumentException("managerInstanceID may not be null");
        }
        return persistence.selectIdFromId(
                ElasticPersistence.GET_RESERVATION_FROM_MANAGER_INSTANCE,
                managerInstanceID);
    }

    /**
     * @param managerKey manager group key
     * @return true if this group key has a corresponding elastic reservation ID
     */
    public boolean isElasticReservationIDGroup(String managerKey) {
        return managerGroupToElasticReservation(managerKey) != null;
    }

    /**
     * Returns elastic reservation ID for the manager group ID
     *
     * @param managerGroupID elastic reservation ID
     * @return Manager group ID or null if unknown
     */
    public String managerGroupToElasticReservation(String managerGroupID) {
        if (managerGroupID == null) {
            throw new IllegalArgumentException("managerGroupID may not be null");
        }
        return persistence.selectIdFromId(
                ElasticPersistence.GET_RESERVATION_FROM_GROUP,
                managerGroupID);
    }

    /**
     * @param managerKey manager cosched key
     * @return true if this cosched key has a corresponding elastic res ID
     */
    public boolean isElasticReservationIDCosched(String managerKey) {
        return this.managerCoschedToElasticReservation(managerKey) != null;
    }

    /**
     * Returns elastic reservation ID for the manager group ID
     *
     * @param managerCoschedID elastic reservation ID
     * @return Manager group ID or null if unknown
     */
    public String managerCoschedToElasticReservation(String managerCoschedID) {
        if (managerCoschedID == null) {
            throw new IllegalArgumentException("managerCoschedID may not be null");
        }
        return persistence.selectIdFromId(
                ElasticPersistence.GET_RESERVATION_FROM_COSCHED,
                managerCoschedID);
    }

    /**
     * @param elasticID elastic instance ID
     * @return key name, may be null
     *         (e.g. if VM was not created with this protocol)
     */
    public String getKeyName(String elasticID) {
        if (elasticID == null) {
            throw new IllegalArgumentException("elasticID may not be null");
        }
        return persistence.selectIdFromId(
                ElasticPersistence.GET_SSHKEY_FROM_ELASTIC_INSTANCE,
                elasticID);
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

        final String query;
        if (instance) {
            query = ElasticPersistence.GET_MANAGER_FROM_ELASTIC_INSTANCE;
        } else {
            query = ElasticPersistence.GET_RESERVATION;
        }

        final int TRIES = 512;
        String id = null;
        for (int i = 0; i < TRIES; i++) {
            id = this.randomID(instance);
            if (persistence.selectIdFromId(query, id) != null) {
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
