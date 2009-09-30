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

import org.globus.wsrf.security.authorization.PDPConfig;
import org.globus.wsrf.impl.security.authorization.exceptions.InitializeException;
import org.globus.wsrf.impl.security.authorization.exceptions.CloseException;
import org.globus.wsrf.impl.security.authorization.exceptions.AttributeException;
import org.globus.wsrf.impl.security.util.AuthUtil;
import org.globus.voms.impl.VomsConstants;
import org.globus.voms.impl.VomsCredentialPIP;

import javax.security.auth.Subject;
import javax.xml.rpc.handler.MessageContext;
import javax.xml.namespace.QName;
import java.util.HashMap;

/**
 * GT4.0.x compatible proxy to the VOMS authorization code.
 */
public class PIP implements org.globus.wsrf.security.authorization.PIP {

    private VomsCredentialPIP impl = new VomsCredentialPIP();

    public void initialize(PDPConfig config,
                           String name,
                           String id) throws InitializeException {

        HashMap configs = new HashMap();
        String[] keys = VomsConstants.ALL_CONFIG_KEYS;
        for (int i = 0; i < keys.length; i++) {
            Object o = config.getProperty(name, keys[i]);
            if (o != null) {
                configs.put(keys[i], o);
            }
        }

        try {
            this.impl.initialize(configs, name);
        } catch (Exception e) {
            throw new InitializeException("",e);
        }
    }

    public void collectAttributes(Subject peer,
                                  MessageContext ctx,
                                  QName op) throws AttributeException {

        assert (this.impl != null);

        // AuthUtil.getIdentity() is 4.0 specific
        String peerIdentity = AuthUtil.getIdentity(peer);

        try {
            this.impl.collectAttributes(peer, peerIdentity, ctx);
        } catch (Exception e) {
            throw new AttributeException(e.getMessage(),e);
        }
    }

    public void close() throws CloseException {
    }
}
