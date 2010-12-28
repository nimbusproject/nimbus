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

package org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.rm;

/**
 * Translations between underlying manager IDs and elastic style.  Since the
 * elastic reservations can correspond to either a group or coscheduling
 * group, they are typed.  You can have duplicate keys of all three types
 * (instance, group, cosched group). Cosched is not in use at time of authoring
 */
public interface IDMappings {

    // -------------------------------------------------------------------------
    // NEW ELASTIC IDs
    // -------------------------------------------------------------------------

    /**
     * Returns new, unique instance elastic ID for the instance ID
     *
     * @param managerInstanceID Manager instance ID
     * @param elasticReservationID already created elastic resid
     * @param sshkeyUsed nickname of key customization task, may be null
     * @return new instance ID
     * @throws Exception could not obtain ID
     */
    public String newInstanceID(String managerInstanceID,
                                String elasticReservationID,
                                String sshkeyUsed)
            throws Exception;


    /**
     * Returns new, unique reservation elastic ID for the instance ID when
     * there is no outside group to make a reservation ID from
     *
     * @param managerInstanceID Manager instance ID
     * @param sshkeyUsed nickname of key customization task, may be null
     * @return new reservation ID
     * @throws Exception could not obtain ID
     */
    public String newGrouplessInstanceID(String managerInstanceID,
                                         String sshkeyUsed)
            throws Exception;
    

    /**
     * Returns new, unique reservation elastic ID for a group ID
     *
     * @param managerGroupID Manager group ID
     * @return new reservation ID
     * @throws Exception could not obtain ID
     */
    public String newGroupReservationID(String managerGroupID)
            throws Exception;

    /**
     * Returns new, unique reservation elastic ID for a cosched ID
     *
     * @param managerCoschedID Manager cosched ID
     * @return new reservation ID
     * @throws Exception could not obtain ID
     */
    public String newCoschedReservationID(String managerCoschedID)
            throws Exception;


    // -------------------------------------------------------------------------
    // GET-OR-NEW ELASTIC IDs
    // -------------------------------------------------------------------------

    /**
     * Returns reservation elastic ID for an instance ID (might be new, might
     * have been mapped already).
     *
     * ***Only use when there is no associated group/cosched ID***
     *
     * @param managerInstanceID Manager instance ID
     * @param sshkeyUsed nickname of key customization task, may be null
     * @return new reservation ID
     * @throws Exception could not obtain ID
     */
    public String getOrNewInstanceReservationID(String managerInstanceID,
                                                String sshkeyUsed)
            throws Exception;


    /**
     * Returns reservation elastic ID for a group ID (might be new, might
     * have been mapped already).
     *
     * @param managerGroupID Manager group ID
     * @return previously created or new reservation ID
     * @throws Exception could not obtain ID
     */
    public String getOrNewGroupReservationID(String managerGroupID)
            throws Exception;

    /**
     * Returns reservation elastic ID for a cosched ID (might be new, might
     * have been mapped already).
     *
     * @param managerCoschedID Manager cosched ID
     * @return previously created or new reservation ID
     * @throws Exception could not obtain ID
     */
    public String getOrNewCoschedReservationID(String managerCoschedID)
            throws Exception;


    /**
     * Ensures the manager instance ID has an elastic mapping, the reservation
     * is always already mapped.  And the underlying VM mgmt layer is telling
     * us the managerInstanceID is part of whatever this elastic reservation
     * maps to.  So if there is no elastic instance ID for this, do it now.
     * (this can happen when the instances are not created via this protocol
     * but with some other messaging layer) 
     *
     * @param managerInstanceID Manager instance ID
     * @param elasticReservationID already created elastic reservation ID
     * @return elastic instance ID
     * @throws Exception problem with args or could not obtain ID
     */
    public String checkInstanceAndReservation(String managerInstanceID,
                                              String elasticReservationID)
            throws Exception;
    

    // -------------------------------------------------------------------------
    // ELASTIC INSTANCE --> MANAGER INSTANCE
    // -------------------------------------------------------------------------
    
    /**
     * @param elasticKey some elastic key
     * @return true if this key has a corresponding Manager instance ID
     */
    public boolean isInstanceID(String elasticKey);
    
    /**
     * Returns manager ID for the elastic instance ID
     *
     * @param elasticInstanceID elastic instance ID
     * @return Manager instance ID or null if unknown
     */
    public String instanceToManager(String elasticInstanceID);


    // -------------------------------------------------------------------------
    // ELASTIC RESERVATION --> MANAGER GROUP
    // -------------------------------------------------------------------------
    
    /**
     * @param elasticKey some elastic key
     * @return true if this key has a corresponding Manager group ID
     */
    public boolean isManagerGroupID(String elasticKey);

    /**
     * Returns Manager group ID for a reservation ID
     *
     * @param elasticReservationID elastic reservation ID
     * @return Manager group ID or null if unknown
     */
    public String reservationToManagerGroup(String elasticReservationID);


    // -------------------------------------------------------------------------
    // ELASTIC RESERVATION --> MANAGER COSCHED
    // -------------------------------------------------------------------------
    
    /**
     * @param elasticKey some elastic key
     * @return true if this key has a corresponding Manager cosched ID
     */
    public boolean isCoschedReservationID(String elasticKey);

    /**
     * Returns Manager cosched ID for a reservation ID
     *
     * @param elasticReservationID elastic reservation ID
     * @return Manager cosched ID or null if unknown
     */
    public String reservationToManagerCosched(String elasticReservationID);


    // -------------------------------------------------------------------------
    // MANAGER INSTANCE --> ELASTIC INSTANCE
    // -------------------------------------------------------------------------

    /**
     * @param managerKey manager key
     * @return true if this key has a corresponding elastic instance ID
     */
    public boolean isElasticInstanceID(String managerKey);

    /**
     * Returns elastic ID for the manager instance ID
     *
     * @param managerInstanceID manager instance ID
     * @return elastic instance ID or null if unknown
     */
    public String managerInstanceToElasticInstance(String managerInstanceID);


    // -------------------------------------------------------------------------
    // MANAGER INSTANCE --> ELASTIC RESERVATION
    // -------------------------------------------------------------------------

    /**
     * Returns elastic reservation ID for the manager instance ID
     *
     * @param managerInstanceID manager instance ID
     * @return elastic instance ID or null if unknown
     */
    public String managerInstanceToElasticReservation(String managerInstanceID);


    // -------------------------------------------------------------------------
    // MANAGER GROUP --> RESERVATION
    // -------------------------------------------------------------------------

    /**
     * @param managerKey manager group key
     * @return true if this group key has a corresponding elastic reservation ID
     */
    public boolean isElasticReservationIDGroup(String managerKey);

    /**
     * Returns elastic reservation ID for the manager group ID
     *
     * @param managerGroupID elastic reservation ID
     * @return Manager group ID or null if unknown
     */
    public String managerGroupToElasticReservation(String managerGroupID);


    // -------------------------------------------------------------------------
    // MANAGER COSCHED --> RESERVATION
    // -------------------------------------------------------------------------

    /**
     * @param managerKey manager cosched key
     * @return true if this cosched key has a corresponding elastic res ID
     */
    public boolean isElasticReservationIDCosched(String managerKey);

    /**
     * Returns elastic reservation ID for the manager group ID
     *
     * @param managerCoschedID elastic reservation ID
     * @return Manager group ID or null if unknown
     */
    public String managerCoschedToElasticReservation(String managerCoschedID);


    // -------------------------------------------------------------------------
    // OTHER
    // -------------------------------------------------------------------------

    /**
     * @param elasticID elastic instance ID
     * @return key name, may be null
     *        (e.g. if VM was not created with this protocol)
     */
    public String getKeyName(String elasticID);

    public String getOrNewInstanceID(String managerInstanceID,
                                     String elasticReservationID,
                                     String sshkeyUsed) throws Exception;
}
