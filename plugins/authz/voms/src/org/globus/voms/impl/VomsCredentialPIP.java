/*
 * Copyright 1999-2007 University of Chicago
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

package org.globus.voms.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.glite.security.voms.BasicVOMSTrustStore;
import org.glite.security.voms.VOMSAttribute;
import org.glite.security.voms.VOMSValidator;

import javax.security.auth.Subject;
import javax.xml.rpc.handler.MessageContext;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.HashMap;

public class VomsCredentialPIP implements VomsConstants {

    static long defaultRefresh = 0;

    private static Log logger =
	LogFactory.getLog(VomsCredentialPIP.class.getName());

    private String trustStore = null;
    private Long refreshTime = null;
    private BasicVOMSTrustStore store = null;
    private boolean validate = false;
    private String vald = null;

    public void initialize(HashMap
        configs, String name) throws Exception {

        if (trustStore == null) {
            trustStore =
                (String) configs.get(TRUST_STORE_DIR_PROP);
            logger.debug("found truststore configuration: " + trustStore);
        }

        if (refreshTime == null) {
            refreshTime = (Long) configs.get(REFRESH_TIME_PROP);
            if (refreshTime != null) {
                logger.debug("found refresh configuration: " +
                         refreshTime.longValue() + " milliseconds");
            }
        }

        if (vald == null) {
            vald = (String) configs.get(VALIDATE);
            if (vald == null) {
                vald = "";
            }
            if (vald.equalsIgnoreCase("true")) {
                validate = true;
            }
        }

        try {
            String trustStoreParam = null;
            long refreshParam = 0;
            if ((trustStore == null) && (refreshTime == null)) {
                store = new BasicVOMSTrustStore();
            } else if (trustStore == null) {
                // refresh time is not null
                trustStoreParam =
                        BasicVOMSTrustStore.DEFAULT_TRUST_STORE_LISTING;
                refreshParam = refreshTime.longValue();
            } else if (refreshTime == null) {
                // trust is not null
                trustStoreParam = trustStore;
                refreshParam = defaultRefresh;
            } else {
                // both are not null
                trustStoreParam = trustStore;
                refreshParam = refreshTime.longValue();
            }

            if (store == null) {
                store =
                    new BasicVOMSTrustStore(trustStoreParam, refreshParam);
            }
        } catch (IllegalArgumentException e) {
            store = null;
            logger.warn("VOMS trust store not enabled, VOMS cert parsing " +
                    "disabled");
        } catch (Exception e) {
            store = null;
            logger.error("Problem configuring VOMS trust store, VOMS cert " +
                    "parsing disabled");
        }
        
        logger.debug("VOMS PIP initialize complete");
    }

    public void collectAttributes(Subject peerSubject,
                                  String peerIdentity,
                                  MessageContext context) throws Exception {

        if (store == null) {
            logger.debug("no VOMS trust store, VOMS cert parsing disabled");
            return;
        }

        Set publicCreds =
            peerSubject.getPublicCredentials(X509Certificate[].class);
        logger.debug("cred set size: " + publicCreds.size());
        Iterator iter = publicCreds.iterator();

        Vector rolesVector = new Vector();
        String VO = null;
        String hostport = null;
        while (iter.hasNext()) {
            X509Certificate certRev[] = (X509Certificate[])iter.next();
            int size = certRev.length;
            X509Certificate cert[] = new X509Certificate[size];
            for (int i=0; i<size; i++) {
                cert[i] = certRev[size-i-1];
            }

            VOMSValidator.setTrustStore(store);
            logger.debug("set truststore to " + store);
            VOMSValidator validator = new VOMSValidator(cert).parse();

            if (validate) {
                logger.debug("validating");
                validator = validator.validate();
            }

            logger.debug("Parse Validator: " + validator.toString());
            List vector = validator.getVOMSAttributes();
            if (vector.size() != 0) {
                logger.debug("getVOMSAttributes() vector size "
                        + vector.size());
                for (int j=0; j<vector.size(); j++) {
                    VOMSAttribute attrib =
                        (VOMSAttribute)vector.get(j);
                    VO = attrib.getVO();
                    hostport = attrib.getHostPort();
                    List fqan = attrib.getFullyQualifiedAttributes();
                    for (int k=0; k<fqan.size(); k++) {
                        String str = (String)fqan.get(k);
                        rolesVector.add(str);
                    }
                }
            }
        }
        if (logger.isDebugEnabled()) {
            for (int i=0; i<rolesVector.size(); i++) {
                logger.debug("\nRoles " + rolesVector.get(i));
            }
            logger.debug("VO " + VO);
            logger.debug("hostport " + hostport);
        }

        VomsCredentialInformation info =
                new VomsCredentialInformation(rolesVector, VO, hostport);

        peerSubject.getPublicCredentials().add(info);

    }
}
