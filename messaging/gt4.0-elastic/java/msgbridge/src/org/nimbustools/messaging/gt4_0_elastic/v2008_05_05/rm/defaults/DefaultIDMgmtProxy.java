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

package org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.rm.defaults;

import org.nimbustools.messaging.gt4_0_elastic.context.ElasticContext;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.rm.IDMappings;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * The purpose of this is to ensure that the same instance of the IDMappings/KeyCacheProvider
 * impl class is being used across *all* Spring contexts in the same JVM.  Because of the
 * multiple messaging containers in use, it's currently a reality that multiple Spring
 * contexts exist.  And thus, breaking the IoC abstraction is necessary if anything is
 * going to share resources that need to be consistent with each other.
 *
 * (It's not quite per-JVM.  Just like with the protocol layers, if you're really after
 * multiple Nimbus service stacks in the same JVM, you need to adjust the lookups in the
 * JNDI config in your alternate installation. That will also then mean you'll get multiple
 * DefaultIDMgmt instances, one per service stack).
 */
public class DefaultIDMgmtProxy implements IDMappings,
                                           ApplicationContextAware {

    public static final String ID_MAPPING_BEAN_NAME = "nimbus-elastic.rm.realidmappings";

    private ApplicationContext thisAppCtx;
    private IDMappings correctIDMappings;

    public void setApplicationContext(ApplicationContext applicationContext)
            throws BeansException {
        this.thisAppCtx = applicationContext;
    }

    /**
     * See class notes.
     * @return the proper IDMappings instance, never null
     * @throws Exception cannot find it
     */
    private IDMappings getReal() throws RuntimeException {
        if (this.correctIDMappings != null) {
            return this.correctIDMappings;
        }

        Object idMappings = null;
        try {
            if (this.thisAppCtx == null) {
                ElasticContext thatAppCtx = ElasticContext.discoverElasticContext();
                idMappings = thatAppCtx.findBeanByID(ID_MAPPING_BEAN_NAME);
            } else {

                try {
                    idMappings = this.thisAppCtx.getBean(ID_MAPPING_BEAN_NAME);
                } catch (NoSuchBeanDefinitionException e) {
                    ElasticContext thatAppCtx = ElasticContext.discoverElasticContext();
                    idMappings = thatAppCtx.findBeanByID(ID_MAPPING_BEAN_NAME);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Problem manually retrieving bean '" +
                                              ID_MAPPING_BEAN_NAME + "': " + e.getMessage(), e);
        }

        // future developer error catch:
        if (idMappings == null) {
            throw new RuntimeException("no bean? '" + ID_MAPPING_BEAN_NAME + "'");
        }
        if (idMappings instanceof IDMappings) {
            this.correctIDMappings = (IDMappings)idMappings;
        } else {
            throw new RuntimeException("bean does not implement" +
                                        " IDMappings? '" + ID_MAPPING_BEAN_NAME + "'");
        }

        return this.correctIDMappings;
    }


    // **************************************************************************************

    public String newInstanceID(String managerInstanceID, String elasticReservationID,
                                String sshkeyUsed) throws Exception {
        return this.getReal().newInstanceID(managerInstanceID, elasticReservationID, sshkeyUsed);
    }

    public String newGrouplessInstanceID(String managerInstanceID, String sshkeyUsed)
            throws Exception {
        return this.getReal().newGrouplessInstanceID(managerInstanceID, sshkeyUsed);
    }

    public String newGroupReservationID(String managerGroupID) throws Exception {
        return this.getReal().newGroupReservationID(managerGroupID);
    }

    public String newCoschedReservationID(String managerCoschedID) throws Exception {
        return this.getReal().newCoschedReservationID(managerCoschedID);
    }

    public String getOrNewInstanceReservationID(String managerInstanceID, String sshkeyUsed)
            throws Exception {
        return this.getReal().getOrNewInstanceReservationID(managerInstanceID, sshkeyUsed);
    }

    public String getOrNewGroupReservationID(String managerGroupID) throws Exception {
        return this.getReal().getOrNewGroupReservationID(managerGroupID);
    }

    public String getOrNewCoschedReservationID(String managerCoschedID) throws Exception {
        return this.getReal().getOrNewCoschedReservationID(managerCoschedID);
    }

    public String checkInstanceAndReservation(String managerInstanceID,
                                              String elasticReservationID) throws Exception {
        return this.getReal().checkInstanceAndReservation(managerInstanceID, elasticReservationID);
    }

    public boolean isInstanceID(String elasticKey) {
        return this.getReal().isInstanceID(elasticKey);
    }

    public String instanceToManager(String elasticInstanceID) {
        return this.getReal().instanceToManager(elasticInstanceID);
    }

    public boolean isManagerGroupID(String elasticKey) {
        return this.getReal().isManagerGroupID(elasticKey);
    }

    public String reservationToManagerGroup(String elasticReservationID) {
        return this.getReal().reservationToManagerGroup(elasticReservationID);
    }

    public boolean isCoschedReservationID(String elasticKey) {
        return this.getReal().isCoschedReservationID(elasticKey);
    }

    public String reservationToManagerCosched(String elasticReservationID) {
        return this.getReal().reservationToManagerCosched(elasticReservationID);
    }

    public boolean isElasticInstanceID(String managerKey) {
        return this.getReal().isElasticInstanceID(managerKey);
    }

    public String managerInstanceToElasticInstance(String managerInstanceID) {
        return this.getReal().managerInstanceToElasticInstance(managerInstanceID);
    }

    public String managerInstanceToElasticReservation(String managerInstanceID) {
        return this.getReal().managerInstanceToElasticReservation(managerInstanceID);
    }

    public boolean isElasticReservationIDGroup(String managerKey) {
        return this.getReal().isElasticReservationIDGroup(managerKey);
    }

    public String managerGroupToElasticReservation(String managerGroupID) {
        return this.getReal().managerGroupToElasticReservation(managerGroupID);
    }

    public boolean isElasticReservationIDCosched(String managerKey) {
        return this.getReal().isElasticReservationIDCosched(managerKey);
    }

    public String managerCoschedToElasticReservation(String managerCoschedID) {
        return this.getReal().managerCoschedToElasticReservation(managerCoschedID);
    }

    public String getKeyName(String elasticID) {
        return this.getReal().getKeyName(elasticID);
    }

    public String getOrNewInstanceID(String managerInstanceID,
                                     String elasticReservationID,
                                     String sshkeyUsed) throws Exception {
        return this.getReal().getOrNewInstanceID(managerInstanceID,
                                                 elasticReservationID,
                                                 sshkeyUsed);
    }
}
