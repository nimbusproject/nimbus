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

package org.nimbustools.messaging.gt4_0_elastic.rpc;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ws.security.WSSConfig;
import org.apache.ws.security.message.token.Timestamp;
import org.globus.wsrf.Resource;
import org.globus.wsrf.ResourceContext;
import org.globus.wsrf.ResourceContextException;
import org.globus.wsrf.ResourceException;
import org.globus.wsrf.impl.security.authentication.wssec.ReplayAttackFilter;
import org.globus.wsrf.impl.security.authentication.wssec.WSSecurityEngine;
import org.globus.wsrf.impl.security.authentication.wssec.WSSecurityException;
import org.globus.wsrf.impl.security.authentication.wssec.WSSecurityRequestEngine;
import org.globus.wsrf.impl.security.descriptor.SecurityPropertiesHelper;
import org.globus.wsrf.utils.ContextUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.rpc.handler.MessageContext;
import javax.xml.rpc.handler.soap.SOAPMessageContext;
import javax.xml.soap.SOAPHeaderElement;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class RequestEngine extends WSSecurityRequestEngine {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(RequestEngine.class.getName());

    protected static final SimpleDateFormat problematicFormat =
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    private static WSSecurityEngine engine;
    
    
    // -------------------------------------------------------------------------
    // overrides WSSecurityRequestEngine
    // -------------------------------------------------------------------------

    public synchronized static WSSecurityEngine getEngine() {
        if (engine == null) {
            engine = new RequestEngine();
        }

        return engine;
    }


    // -------------------------------------------------------------------------
    // overrides WSSecurityEngine
    // -------------------------------------------------------------------------
    
    /**
     * Overrides processTimestampHeader in WSSecurityEngine
     *
     * The secmsg Timestamp constructor fails on timestamps with milliseconds
     * in them.
     *
     * Since this is handled after the envelope integrity check, it's OK to
     * replace the timestamp if necessary (this allows the the constructor to
     * succeed and attack window analysis to proceed).
     *
     * @param givenTimestampElem timestampElem from soap
     * @param msgCtx msgCtx
     * @param messageIDHeader messageIDHeader
     * @throws Exception problem
     * @see #perhapsChangeTimestampFormat(Element);
     */
    protected void processTimestampHeader(Element givenTimestampElem,
                                          MessageContext msgCtx,
                                          SOAPHeaderElement messageIDHeader)
        throws Exception {

        final String servicePath =
                ContextUtils.getTargetServicePath(
                        (org.apache.axis.MessageContext)msgCtx);
        
        if (servicePath == null) {
            throw new Exception(i18n.getMessage("serviceNull"));
        }

        Resource resource = null;
        try {
            final ResourceContext resCtx = ResourceContext
                .getResourceContext((SOAPMessageContext) msgCtx);
            resource = resCtx.getResource();
        } catch (ResourceContextException exp) {
            // Not an issue
        } catch (ResourceException exp) {
            // Not an issue
        }

        if (givenTimestampElem != null) {

            final Element timestampElem =
                this.perhapsChangeTimestampFormat(givenTimestampElem);

            final String propertyValue =
                SecurityPropertiesHelper.getReplayAttackWindow(servicePath,
                                                               resource);
            final ReplayAttackFilter replayFilter =
                ReplayAttackFilter.getInstance(propertyValue);

            if (messageIDHeader == null) {
                final Timestamp timestamp =
                    new Timestamp(WSSConfig.getDefaultWSConfig(),
                                  timestampElem);
                final boolean stampOk =
                    verifyTimestamp(timestamp,
                                    replayFilter.getMessageWindow());
                if (!stampOk) {
                    throw new WSSecurityException(WSSecurityException.FAILURE,
                                                  "timestampNotOk");
                }
            } else {
                checkMessageValidity(replayFilter, timestampElem,
                                     messageIDHeader);
            }
        } else {
            final String propertyValue =
                SecurityPropertiesHelper.getReplayAttackFilter(servicePath,
                                                               resource);
            if (rejectMsgSansTimestampHeader(msgCtx, propertyValue)) {
                logger.debug("Required time stamp header was not added.");
                throw new WSSecurityException(WSSecurityException.FAILURE,
                                              "timestampRequired");
            }
        }
    }


    // -------------------------------------------------------------------------
    // TIMESTAMP CHANGES
    // -------------------------------------------------------------------------
    
    protected Element perhapsChangeTimestampFormat(Element timestampElem)
            throws Exception {

        if (timestampElem == null) {
            throw new Exception("timestampElem may not be null");
        }

        final Node firstChild = timestampElem.getFirstChild();
        if (firstChild == null) {
            logger.warn("Timestamp but no data enclosed?");
            return timestampElem;
        }

        final String firstChildLocal = firstChild.getLocalName();
        if (firstChildLocal != null && firstChildLocal.equals("Created")) {

            if (firstChildLocal.equals("Created")) {
                this.handleCreatedOrExpires(firstChild, "Created");
            } else {
                logger.warn("Timestamp with something besides " +
                        "Created: '" + firstChildLocal + "'");
                return timestampElem;
            }

            final Node sibling = firstChild.getNextSibling();
            if (sibling == null) {
                logger.warn("Timestamp with Created but no Expires?");
                return timestampElem;
            }

            final String siblingLocal = sibling.getLocalName();
            if (siblingLocal != null && siblingLocal.equals("Expires")) {
                this.handleCreatedOrExpires(sibling, "Expires");
            } else {
                logger.warn("Timestamp with Created but no Expires?");
                logger.warn("Timestamp with Created and then something " +
                        "besides Expires: '" + siblingLocal + "'");
                return timestampElem;
            }
        }

        return timestampElem;
    }

    protected void handleCreatedOrExpires(Node node, String name) {

        final Node firstChild = node.getFirstChild();
        final String firstChildNodeValue = firstChild.getNodeValue();
        
        if (firstChildNodeValue == null) {
            logger.warn("Timestamp with null value for " + name);
        } else {
            final String newValue = this.chop(firstChildNodeValue);
            if (newValue != null) {
                firstChild.setNodeValue(newValue);
            }
        }
    }

    protected String chop(String stamp) {

        try {
            problematicFormat.parse(stamp);
        } catch (ParseException e) {
            // not in the problematic format
            return null; // *** EARLY RETURN ***
        }

        // remove milliseconds, Timestamp class wants "yyyy-MM-ddTHH:mm:ssZ"
        // and the problematic one looks like "yyyy-MM-ddTHH:mm:ss.SSSZ"

        // get first 19 characters then tack on Z
        final String prefix = stamp.substring(0, 19);
        return prefix + 'Z';
    }
}
