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

package org.nimbustools.messaging.gt4_0;

import org.apache.axis.message.addressing.EndpointReferenceType;
import org.nimbustools.api.repr.CannotTranslateException;
import org.globus.wsrf.ResourceKey;
import org.globus.wsrf.impl.SimpleResourceKey;
import org.globus.wsrf.utils.AddressingUtils;

import javax.xml.namespace.QName;
import java.net.URL;

public class EPRGenerator {

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected final URL baseURL;
    protected final String serviceName;
    protected final QName keyTypeName;


    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    /**
     * @param containerURL base url
     * @param serviceNameStr name of service, appended to URL, may not be null
     * @param keyTypeNameQname qname of EPR key, may not be null
     */
    public EPRGenerator(URL containerURL,
                        String serviceNameStr,
                        QName keyTypeNameQname) {
        
        if (containerURL == null) {
            throw new IllegalArgumentException("containerURL may not be null");
        }
        this.baseURL = containerURL;

        if (serviceNameStr == null) {
            throw new IllegalArgumentException("serviceNameStr may not be null");
        }
        this.serviceName = serviceNameStr;

        if (keyTypeNameQname == null) {
            throw new IllegalArgumentException("keyTypeNameQname may not be null");
        }
        this.keyTypeName = keyTypeNameQname;
        
    }


    // -------------------------------------------------------------------------
    // EPRs originating from this container
    // -------------------------------------------------------------------------

    /**
     * @param id EPR unique ID, may not be null
     * @return EPR
     * @throws CannotTranslateException inputs null/invalid or some general err
     */
    public EndpointReferenceType getEPR(String id)
            throws CannotTranslateException {

        if (id == null) {
            throw new CannotTranslateException("id may not be null");
        }

        try {
            final String addr = this.baseURL + this.serviceName;
            final ResourceKey key =
                    new SimpleResourceKey(this.keyTypeName, id);
            return AddressingUtils.createEndpointReference(addr, key);
        } catch (Exception e) {
            throw new CannotTranslateException(e.getMessage(), e);
        }
    }
    
}
