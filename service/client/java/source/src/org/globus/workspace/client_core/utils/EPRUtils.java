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

package org.globus.workspace.client_core.utils;

import org.apache.axis.message.addressing.EndpointReferenceType;
import org.apache.axis.message.addressing.ReferencePropertiesType;
import org.apache.axis.message.MessageElement;
import org.nimbustools.messaging.gt4_0.common.Constants_GT4_0;
import org.globus.wsrf.encoding.SerializationException;
import org.globus.wsrf.ResourceKey;
import org.globus.wsrf.utils.AddressingUtils;
import org.globus.wsrf.impl.SimpleResourceKey;

import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;

public class EPRUtils {

    public static final String defaultFactoryUrlString =
            "https://localhost:8443/wsrf/services/WorkspaceFactoryService";

    public static final ResourceKey defaultFactoryKey;

    static {
        defaultFactoryKey =
                new SimpleResourceKey(
                        Constants_GT4_0.RESOURCE_KEY_QNAME,
                        Constants_GT4_0.FACTORY_DEFAULT_RSRC_KEY_NAME);
    }

    public static String eprToString(EndpointReferenceType epr)
            throws SerializationException, IOException {
        return StringUtils.eprToString(epr);
    }

    public static boolean isInstanceEPR(EndpointReferenceType endpoint) {

        if (endpoint == null) {
            return false;
        }

        final ReferencePropertiesType rpt = endpoint.getProperties();
        if (rpt == null) {
            return false;
        }

        final MessageElement me =
                    rpt.get(Constants_GT4_0.RESOURCE_KEY_QNAME);
        
        return me != null;
    }

    public static boolean isGroupEPR(EndpointReferenceType endpoint) {

        if (endpoint == null) {
            return false;
        }

        final ReferencePropertiesType rpt = endpoint.getProperties();
        if (rpt == null) {
            return false;
        }

        final MessageElement me =
                    rpt.get(Constants_GT4_0.GROUP_RESOURCE_KEY_QNAME);

        return me != null;
    }

    public static boolean isEnsembleEPR(EndpointReferenceType endpoint) {

        if (endpoint == null) {
            return false;
        }

        final ReferencePropertiesType rpt = endpoint.getProperties();
        if (rpt == null) {
            return false;
        }

        final MessageElement me =
                    rpt.get(Constants_GT4_0.ENSEMBLE_RESOURCE_KEY_QNAME);

        return me != null;
    }

    public static boolean isContextEPR(EndpointReferenceType endpoint) {

        if (endpoint == null) {
            return false;
        }

        final ReferencePropertiesType rpt = endpoint.getProperties();
        if (rpt == null) {
            return false;
        }

        final MessageElement me =
                    rpt.get(Constants_GT4_0.NIMBUS_CONTEXTUALIZATION_RESOURCE_KEY_QNAME);

        return me != null;
    }

    /**
     * @param epr workspace instance EPR
     * @return id integer
     * @throws IllegalArgumentException not a workspace instance EPR
     */
    public static int getIdFromEPR(EndpointReferenceType epr)
                                        throws IllegalArgumentException { 

        if (!isInstanceEPR(epr)) {
            throw new IllegalArgumentException(
                                "given epr not a workspace instance EPR");
        }

        final String keyStr = epr.getProperties().get(
                        Constants_GT4_0.RESOURCE_KEY_QNAME).getValue();

        return Integer.parseInt(keyStr);
    }

    /**
     * @param epr workspace group EPR
     * @return String id string (typically UUID)
     * @throws IllegalArgumentException not a workspace group EPR
     */
    public static String getGroupIdFromEPR(EndpointReferenceType epr)
                                        throws IllegalArgumentException {

        if (!isGroupEPR(epr)) {
            throw new IllegalArgumentException(
                                "given epr not a workspace group EPR");
        }

        return epr.getProperties().get(
                    Constants_GT4_0.GROUP_RESOURCE_KEY_QNAME).getValue();
    }

    /**
     * @param epr workspace ensemble EPR
     * @return String id string (typically UUID)
     * @throws IllegalArgumentException not a workspace ensemble EPR
     */
    public static String getEnsembleIdFromEPR(EndpointReferenceType epr)
                                        throws IllegalArgumentException { 

        if (!isEnsembleEPR(epr)) {
            throw new IllegalArgumentException(
                                "given epr not a workspace ensemble EPR");
        }

        return epr.getProperties().get(
                    Constants_GT4_0.ENSEMBLE_RESOURCE_KEY_QNAME).getValue();
    }

    /**
     * @param epr workspace context EPR
     * @return String id string (typically UUID)
     * @throws IllegalArgumentException not a workspace context EPR
     */
    public static String getContextIdFromEPR(EndpointReferenceType epr)
                                        throws IllegalArgumentException {

        if (!isContextEPR(epr)) {
            throw new IllegalArgumentException(
                                "given epr not a workspace context EPR");
        }

        return epr.getProperties().get(
                    Constants_GT4_0.NIMBUS_CONTEXTUALIZATION_RESOURCE_KEY_QNAME).getValue();
    }

    /**
     * @param epr any EPR
     * @return String uri (as String) or null (if epr is null or address in epr is null)
     */
    public static String getServiceURIAsString(EndpointReferenceType epr) {

        if (epr == null || epr.getAddress() == null) {
            return null;
        }

        return epr.getAddress().toString();
    }

    public static ResourceKey defaultFactoryKey() {
        return defaultFactoryKey;
    }
        
    public static EndpointReferenceType defaultFactoryEPR() {

        try {
            final URL factoryURL = new URL(defaultFactoryUrlString);
        } catch (MalformedURLException e) {
            throw new IllegalStateException(
                        "bad default URL: " + e.getMessage());
        }

        final EndpointReferenceType defaultFactoryEPR;
        try {
            defaultFactoryEPR = AddressingUtils.
                    createEndpointReference(defaultFactoryUrlString,
                                            defaultFactoryKey());
        } catch (Exception e) {
            throw new IllegalStateException("Problem creating factory EPR " +
                    "from URL and key: " + e.getMessage());
        }

        return defaultFactoryEPR;
    }
}
