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

import org.globus.wsrf.impl.security.authorization.exceptions.InitializeException;
import org.globus.wsrf.impl.security.authorization.exceptions.InvalidPolicyException;
import org.globus.wsrf.impl.security.authorization.exceptions.AuthorizationException;
import org.globus.wsrf.impl.security.authorization.exceptions.CloseException;
import org.globus.wsrf.impl.security.util.AuthUtil;
import org.globus.wsrf.impl.security.descriptor.SecurityPropertiesHelper;
import org.globus.wsrf.security.authorization.PDPConfig;
import org.globus.wsrf.config.ConfigException;
import org.globus.voms.impl.PDPDecision;
import org.globus.voms.impl.VomsConstants;
import org.globus.voms.impl.VomsPDP;
import org.globus.security.gridmap.GridMap;
import org.w3c.dom.Node;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.security.auth.Subject;
import javax.xml.namespace.QName;
import javax.xml.rpc.handler.MessageContext;
import java.util.HashMap;

/**
 * GT4.0.x compatible proxy to the VOMS authorization code.
 */
public class PDP implements org.globus.wsrf.security.authorization.PDP {

    private static Log logger = LogFactory.getLog(PDP.class.getName());

    private VomsPDP impl = new VomsPDP();

    public void initialize(PDPConfig pdpConfig,
                           String name,
                           String id) throws InitializeException {

        HashMap configs = new HashMap();
        String[] keys = VomsConstants.ALL_CONFIG_KEYS;
        for (int i = 0; i < keys.length; i++) {
            Object o = pdpConfig.getProperty(name, keys[i]);
            if (o != null) {
                configs.put(keys[i], o);
            }
        }

        // programmatic, alternative use of the PDP could have resulted in
        // another default gridmap to use.  This makes gridmap retrieval
        // unnecessary
        Object o = pdpConfig.getProperty(name, VomsConstants.DEFAULT_GRIDMAP);
        if (o == null) {
            // getGridMap method is 4.0.x specific
            try {
                GridMap gridmap = SecurityPropertiesHelper.
                                                   getGridMap(id, null);
                if (gridmap != null) {
                    configs.put(VomsConstants.DEFAULT_GRIDMAP, gridmap);
                } else {
                    logger.warn("default gridmap is null");
                }
            } catch (ConfigException e) {
                throw new InitializeException("", e);
            }
        }

        try {
            this.impl.initialize(configs, name);
        } catch (Exception e) {
            throw new InitializeException("",e);
        }
    }

    public boolean isPermitted(Subject peer,
                               MessageContext context,
                               QName op) throws AuthorizationException {

        assert (this.impl != null);

        // AuthUtil.getIdentity() is 4.0 specific
        String peerIdentity = AuthUtil.getIdentity(peer);
        int dec;
        try {
            dec = this.impl.isPermitted(peer, peerIdentity, context, op);
        } catch (Exception e) {
            throw new AuthorizationException("", e);
        }
        return dec == PDPDecision.PERMIT;
    }

    public String[] getPolicyNames() {
        return null;
    }

    public Node getPolicy(Node node)
            throws InvalidPolicyException {
        return null;
    }

    public Node setPolicy(Node node)
            throws InvalidPolicyException {
        return null;
    }

    public void close() throws CloseException {
    }

}
