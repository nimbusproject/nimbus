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

package org.globus.voms;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.wsrf.security.authorization.ChainConfig;
import org.globus.wsrf.security.authorization.InitializeException;
import org.globus.wsrf.security.authorization.PIPResponse;
import org.globus.wsrf.security.authorization.RequestAttributes;
import org.globus.wsrf.security.authorization.AttributeException;
import org.globus.wsrf.security.authorization.CloseException;
import org.globus.wsrf.security.authorization.EntityAttributes;
import org.globus.wsrf.impl.security.util.AuthzUtil;
import org.globus.wsrf.impl.security.util.AttributeUtil;
import org.globus.wsrf.impl.security.util.CredentialUtil;
import org.globus.wsrf.config.ConfigException;
import org.globus.voms.impl.VomsConstants;
import org.globus.voms.impl.VomsCredentialPIP;

import javax.xml.rpc.handler.MessageContext;
import javax.security.auth.Subject;
import java.util.HashMap;

/**
 * GT4.1+ compatible proxy to the VOMS authorization code.
 */
public class PIP implements org.globus.wsrf.security.authorization.PIP {
    
    private static Log logger = LogFactory.getLog(PIP.class.getName());

    private VomsCredentialPIP impl = new VomsCredentialPIP();

    public void initialize(String chainName,
                           String prefix,
                           ChainConfig config)
                    throws InitializeException {

        assert config != null;

        logger.debug("initialize() called. chainName = " + chainName +
                     ", prefix = " + prefix);

        try {
            AuthzUtil.parseNameValueParam(prefix, config);
        } catch (ConfigException e) {
            throw new InitializeException("problem parsing configuration",e);
        }

        HashMap configs = new HashMap();
        String[] keys = VomsConstants.ALL_CONFIG_KEYS;
        for (int i = 0; i < keys.length; i++) {
            Object o = config.getProperty(prefix, keys[i]);
            if (o != null) {
                configs.put(keys[i], o);
            }
        }

        try {
            this.impl.initialize(configs, chainName);
        } catch (Exception e) {
            throw new InitializeException("",e);
        }

    }

    public PIPResponse collectAttributes(RequestAttributes requestAttrs,
                                         MessageContext ctx)
            throws AttributeException {

        EntityAttributes requestor = requestAttrs.getRequestor();
        Subject peer = AttributeUtil.getPeerSubject(requestor);
        String peerIdentity = CredentialUtil.getIdentity(peer);

        logger.debug("found peer = " + peer);
        logger.debug("found peerIdentity = " + peerIdentity);

        try {
            this.impl.collectAttributes(peer, peerIdentity, ctx);
        } catch (Exception e) {
            throw new AttributeException("",e);
        }

        return null;
    }

    public void close() throws CloseException {
    }
}
