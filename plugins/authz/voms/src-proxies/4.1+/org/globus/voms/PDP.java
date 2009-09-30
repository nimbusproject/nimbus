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

import org.globus.wsrf.security.authorization.Decision;
import org.globus.wsrf.security.authorization.RequestAttributes;
import org.globus.wsrf.security.authorization.AuthorizationException;
import org.globus.wsrf.security.authorization.ChainConfig;
import org.globus.wsrf.security.authorization.InitializeException;
import org.globus.wsrf.security.authorization.CloseException;
import org.globus.wsrf.security.authorization.EntityAttributes;
import org.globus.wsrf.security.authorization.Attribute;
import org.globus.wsrf.security.SecureContainerConfig;
import org.globus.wsrf.impl.security.util.AuthzUtil;
import org.globus.wsrf.impl.security.util.AttributeUtil;
import org.globus.wsrf.impl.security.util.CredentialUtil;
import org.globus.wsrf.config.ConfigException;
import org.globus.voms.impl.VomsPDP;
import org.globus.voms.impl.VomsConstants;
import org.globus.voms.impl.PDPDecision;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.rpc.handler.MessageContext;
import javax.xml.namespace.QName;
import javax.security.auth.Subject;
import java.util.List;
import java.util.HashMap;
import java.util.Iterator;

public class PDP implements org.globus.wsrf.security.authorization.PDP {

    private static Log logger = LogFactory.getLog(PDP.class.getName());

    private VomsPDP impl = new VomsPDP();


    public void initialize(String chainName,
                           String prefix,
                           ChainConfig config) throws InitializeException {
        
        logger.debug("initialize() called. chainName = " + chainName +
                     ", prefix = " + prefix);

        assert config != null;

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

        // not inserting default gridmap to configs, in 4.1+ this can just
        // be another PDP in the front of a permit-override chain

        try {
            this.impl.initialize(configs, chainName);
        } catch (Exception e) {
            throw new InitializeException("",e);
        }
    }

    public Decision canAccess(List subjectAttributeCollection,
                              List resourceAttributeCollection,
                              List actionAttributeCollection,
                              RequestAttributes requestAttributes,
                              MessageContext msgCtx)
            throws AuthorizationException {

        return isPermitted(requestAttributes, msgCtx);
    }

    public Decision canAdminister(List subjectAttributeCollection,
                                  List resourceAttributeCollection,
                                  List actionAttributeCollection,
                                  RequestAttributes requestAttributes,
                                  MessageContext msgCtx)
            throws AuthorizationException {

        return isPermitted(requestAttributes, msgCtx);
    }

    private Decision isPermitted(RequestAttributes requestAttrs,
                                 MessageContext msgCtx) {

        /* container is the issuer of this decision */
        EntityAttributes issuerEntity =
            SecureContainerConfig.getSecurityDescriptor().getContainerEntity();

        /**
         * requestAttributes contains Requestor, Environment, Action,
         * and Resource bags of attributes.  The Decision object needs
         * Requestor.
         */
        EntityAttributes reqEntity = requestAttrs.getRequestor();

        /* requester information for impl */
        Subject peer = AttributeUtil.getPeerSubject(reqEntity);
        String peerIdentity = CredentialUtil.getIdentity(peer);

        /* operation name for impl */
        EntityAttributes actionEntity = requestAttrs.getAction();
        Attribute opAttr = AttributeUtil
            .getAttribute(actionEntity.getIdentityAttributes(),
                          AttributeUtil.getOperationAttrIdentifier());
        Iterator it = opAttr.getAttributeValueSet().iterator();
        QName qname = null;
        if (it.hasNext()) {
            qname = (QName)it.next();
        }

        assert this.impl != null;
        int dec = this.impl.isPermitted(peer, peerIdentity, msgCtx, qname);

        switch (dec) {
            case PDPDecision.PERMIT:
                return new Decision(issuerEntity, reqEntity,
                            Decision.PERMIT, null, null);
            case PDPDecision.DENY:
                return new Decision(issuerEntity, reqEntity,
                            Decision.DENY, null, null);
            case PDPDecision.NOT_APPLICABLE:
                return new Decision(issuerEntity, reqEntity,
                            Decision.NOT_APPLICABLE, null, null);
            default:
                return new Decision(issuerEntity, reqEntity,
                            Decision.INDETERMINATE, null, null);
        }
    }

    public void close() throws CloseException {
    }
}
