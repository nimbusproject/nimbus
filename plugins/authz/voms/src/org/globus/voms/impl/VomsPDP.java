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
import org.globus.security.gridmap.GridMap;
import org.globus.wsrf.security.authorization.attributes.AttributeInformation;
import org.globus.gsi.jaas.UserNamePrincipal;

import javax.security.auth.Subject;
import javax.xml.namespace.QName;
import javax.xml.rpc.handler.MessageContext;
import javax.xml.rpc.handler.soap.SOAPMessageContext;
import javax.xml.soap.SOAPMessage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
import java.util.HashMap;

public class VomsPDP implements VomsConstants {

    /* see this.findConfigFile(String fileName)      */
    public static final String[] CONFIG_LOCATIONS =
        {"",".", "/etc/grid-security"};

    /** If vomsPolicy is not null, the PDP consults dynamic policies in RAM.
       This is to use for associating VomsPDP to each resource instance.

       Again, cannot use a mixture of identity authorization PDP and
       an attribute PDP because GT does not allow for
       permit-overides in the authorization chain.

       @see #isPermitted
     */
    private VomsPDPPolicy vomsPolicy = null;

    private GridMap gridmap = null;

    /* attrs authz list */
    private ACLPDP attrauthz = null;

    /* attr mapping */
    private String attrMapFileName = null;
    private File attrGridMapFile = null;
    private GridMap attrgridmap = null;
    
    //private long attrConfigLastModified = 0;
    private boolean andLogic = false;

    private static Log logger = LogFactory.getLog(VomsPDP.class.getName());

    /**
     * Checks both DN/attrs -- it would be convenient, but these can not be
     * separate PDPs (one for DNs (gridmap) and one for VOMS attributes),
     * since chains are deny-overrides.  We need to allow either DN or
     * attributes.
     * @param peer subject
     * @param peerIdentity dn
     * @param msgCtx ctx
     * @param op op
     * @return Decision int
     */
    public int isPermitted(Subject peer,
                           String peerIdentity,
                           MessageContext msgCtx,
                           QName op) {

        boolean ret = false;

        try {
            ret = isPermittedImpl(peer, peerIdentity, msgCtx, op);
        } catch (Exception e) {
            // catch all, log and return false (DENY)
            logger.error(e);
        }

        /* In the future, isPermittedImpl will be more expressive (for 4.1+) */
        if (ret) {
            return PDPDecision.PERMIT;
        } else {
            return PDPDecision.DENY;
        }
    }

    public boolean isPermittedImpl(Subject peer, String peerIdentity,
                            MessageContext msgCtx, QName op) throws Exception {

        Boolean dnPassed = null;
        Boolean attrPassed = null;

        String operation = op.toString();

        logger.debug("Operation " + operation + " called by subject: "
                     + peerIdentity);

        if (this.vomsPolicy == null) {
            try {
                if (checkGridMapFile(peer, peerIdentity)) {
                    dnPassed = Boolean.TRUE;
                } else {
                    logger.debug("DN not in gridmap file configured " +
                            "(or no gridmap): " + peerIdentity);
                }
            } catch (IOException e) {
                throw new Exception("",e);
            }
        } else {
            logger.debug("using RAM policy for DN");
            Iterator DNs = this.vomsPolicy.getDNs();
            String DN;
            while (DNs.hasNext()) {
                DN = (String) DNs.next();
                logger.debug("found a DN in RAM policy: " + DN);
                if (peerIdentity.equals(DN)) {
                    dnPassed = Boolean.TRUE;
                    break;
                }
            }

        }

        AttributeInformation info = null;
        Set credSet = peer.getPublicCredentials();
        Iterator creds = credSet.iterator();
        while (creds.hasNext()) {
            Object o = creds.next();
            if (o instanceof AttributeInformation) {
                info = (AttributeInformation) o;
                break;
            }
        }

        Vector rolesVector = null;
        VomsCredentialInformation vomsinfo = null;

        /* It's OK if there is no attribute information in the message
           context, authz will be by DN only */
        if (info == null) {
            logger.info("cannot retrieve credential info from message context");
        } else {
            /* There is no unified attribute internal interface yet, this is
               here until that is in place */
            if (!(info instanceof VomsCredentialInformation)) {
                throw new Exception("credenital info from " +
                        "message context is not VOMS: incompatible PIP");
            } else {
                vomsinfo = (VomsCredentialInformation) info;
            }

            rolesVector = vomsinfo.getAttrs();
        }

        /* It's OK if there are no roles, see above */
        if (rolesVector == null) {
            logger.warn("cannot retrieve roles from credential information");
        } else {

            if (logger.isDebugEnabled()) {
                for (int i=0; i<rolesVector.size(); i++) {
                    logger.debug("\nRoles " + rolesVector.get(i));
                }
            }

            // Use policy in RAM
            if (this.vomsPolicy != null) {
                logger.debug("using RAM policy for attributes");
                Iterator attrs = this.vomsPolicy.getAttrs();
                String policyAttr;
                //current algorithm only cares if there is one passing attribute
                while ((attrPassed == null) && (attrs.hasNext())) {
                    policyAttr = (String) attrs.next();
                    logger.debug("found an attribute in RAM policy: "
                            + policyAttr);
                    for (int i=0; i<rolesVector.size(); i++) {
                        String attr = (String)rolesVector.get(i);
                        if (policyAttr.equals(attr)) {
                            attrPassed = Boolean.TRUE;
                            break;
                        }
                    }
                }
            } // end if (this.vomsPolicy != null)

            // else use policy in file
            if (this.vomsPolicy == null) {

                if (this.attrauthz == null) {
                    // init was not called
                    logger.error("no attribute authorization object (?)");
                } else {

                    for (int i=0; i<rolesVector.size(); i++) {
                        String attr = (String)rolesVector.get(i);
                        logger.debug("checking attribute " + attr);

                        if (this.attrauthz.isPermitted(peer, attr, msgCtx, op)
                                    == PDPDecision.PERMIT) {
                            
                            //current algorithm only cares if there is one
                            // passing attribute
                            attrPassed = Boolean.TRUE;
                            logger.info("Attribute passed: " + attr);

                            // get mappings, if any, for this attribute
                            checkAttrMapFile(attr, peer);

                        } else {
                            
                            logger.debug("attribute denied: " + attr);
                        }
                    }
                }
                
            } // end if (this.vomsPolicy == null)
        } // end if/else (rolesVector == null)

        if (vomsinfo != null) {
            String VO = vomsinfo.getVO();
            logger.debug("VO " + VO);
        }

        // now we have positive policy decision from either the ACL
        // or the RAM policy (either 'ACCEPT' or 'Not applicable').
        // Send parameters and message to any overriding PDP (this
        // class's impl. of checkCallAndContent is just a no-op).
        //
        // The result of this call will be combined in a deny-override
        // fashion with the current positive decisions (and not appl.
        // all around results in DENY)

        Boolean other;
        try {
            logger.debug("calling checkCallAndContent on PDP impl");
            other = checkCallAndContent(peerIdentity, rolesVector,
                    operation, getMessage(msgCtx));
        } catch (Exception e) {
            throw new Exception("",e);
        }

        // Not applicable all around results in DENY
        boolean result = combine(dnPassed, attrPassed, other);

        /* see bugzilla #2287

           if 'return false' based on attribute, the exception returned to
           client by ServiceAuthorizationChain claims DN is denied.

           One possible workaround, AuthorizationException, implies error in
           policy processing, so something along these lines is actually not
           a viable alternative:

           throw new AuthorizationException("primary attribute denied: "
           + primaryAttr);

           Also, now if denial is based on content of operation, this
           error message is also not 'correct'
        */

        if (result) {
            logger.info("ACCEPTED: Operation " + operation + " called " +
                    "by subject: " + peerIdentity);
            return true;
        } else {
            logger.info("DENIED: Operation " + operation + " called " +
                    "by subject: " + peerIdentity);
            return false;
        }
    }

    /**
     * Allows for extending PDP to do more specific checks on
     * caller and content.
     *
     * This is combined in the base PDP logic in a deny overrides
     * fashion.
     *
     * @param peerIdentity dn
     * @param attributes attrs
     * @param operation opname
     * @param content object representing message body,
     * @return Boolean.TRUE for ACCEPT, Boolean.FALSE for DENY, null for NOT APPLICABLE
     * @throws Exception exc
     */
    public Boolean checkCallAndContent(String peerIdentity,
                                       Vector attributes,
                                       String operation,
                                       SOAPMessage content) throws Exception {
        return null;
    }

    /**
     * nulls are not considered, rest are based on deny-override
     * Note that dnPassed and attrPassed should only be positive or null
     *
     * @param dnPassed bool
     * @param attrPassed bool
     * @param other bool
     * @return result
     */
    private boolean combine(Boolean dnPassed,
                            Boolean attrPassed,
                            Boolean other) {

        // This implementation treats the three distinctly in
        // order to present detailed logging.
        //
        // As for the best way to get the result, the 'way' to do
        // it in general would be for this method to accept an array
        // of Boolean objects, toss out nulls, combine based on
        // && booleanValue(), and make sure to convert 'all null'
        // into false.

        boolean dn = false;
        boolean attr = false;
        if (dnPassed != null) {
            dn = true;
            if (!dnPassed.booleanValue()) {
                logger.debug("what are you doing?");
            }
        }

        if (attrPassed != null) {
            attr = true;
            if (!attrPassed.booleanValue()) {
                logger.debug("what are you doing?");
            }
        }

        if (this.andLogic) {
            
            if (dn && attr) {
                if (other == null) {
                    logger.debug("dn and attr are in positive policy, " +
                            "'other' returns not applicable");
                    return true;
                } else {
                    logger.debug("dn and attr passed, 'other' decides, " +
                            "decision: " + other.booleanValue());
                    return other.booleanValue();
                }
            } else {

                if (other != null) {
                    logger.debug("dn AND attrs are not in positive policy, " +
                            "'other' decides: " + other.booleanValue());
                    return other.booleanValue();
                } else {
                    logger.debug("dn AND attrs are not in positive policy, " +
                            "and 'other' returns not applicable");
                    return false;
                }

            }

        } else if (dn || attr) {

            if (other == null) {
                logger.debug("dn OR attr are in positive policy, " +
                        "'other' returns not applicable");
                return true;
            } else {
                logger.debug("dn OR attr passed, 'other' decides, " +
                        "decision: " + other.booleanValue());
                return other.booleanValue();
            }

        } else {

            if (other != null) {
                logger.debug("dn OR attr not in positive policy, " +
                        "'other' decides: " + other.booleanValue());
                return other.booleanValue();
            } else {
                logger.debug("dn OR attr not in positive policy, and" +
                        " 'other' returns not applicable");
                return false;
            }
        }

    }

    private SOAPMessage getMessage(MessageContext messageContext) {

        SOAPMessageContext ctx = (SOAPMessageContext) messageContext;
        SOAPMessage message = ctx.getMessage();
        if (message == null) {
            logger.debug("Empty SOAPEnvelope");
            return null;
        } else {
            return message;
        }
    }

   
    /*
     * Check if DN maps to atleast one localUser in gridmap file
     */
    private boolean checkGridMapFile(Subject peer, String peerIdentity)
            throws IOException {
        boolean returnThis = false;
        try {
            if (this.gridmap != null) {
                try {
                    this.gridmap.refresh();
                } catch (IOException e) {
                    throw new Exception("gridmap refresh failure", e);
                }

                String[] usernames = this.gridmap.getUserIDs(peerIdentity);

                if ((usernames != null) && (usernames.length > 0)) {
                    returnThis = true;

                    for (int i = 0; i < usernames.length; i++) {
                        peer.getPrincipals()
                            .add(new UserNamePrincipal(usernames[i]));
                        logger.info("mapped to user: '" + usernames[i] + "'");
                    }
                }
            }
        } catch (Exception e) {
            throw new IOException(e.toString());
        }
        return returnThis;
    }

    private void checkAttrMapFile(String attr, Subject peer)
            throws IOException {

        if (this.attrMapFileName == null) {
            // mappings are not configured if this is not set
            return;
        }

        /* only null if InitializeException is ignored from initialize */
        if (this.attrgridmap == null) {
            if (this.attrGridMapFile == null) {
                this.attrGridMapFile = findConfigFile(this.attrMapFileName);
            }

            if (this.attrGridMapFile == null) {
                logger.error("cannot find " + this.attrMapFileName);
                return;
            } else {
                this.attrgridmap = new GridMap();
                this.attrgridmap.load(this.attrGridMapFile);
            }
        }

        try {
            if (this.attrgridmap != null) {
                try {
                    this.attrgridmap.refresh();
                } catch (IOException e) {
                    throw new Exception("attr-gridmap refresh failure", e);
                }

                String[] usernames = this.attrgridmap.getUserIDs(attr);

                if ((usernames != null) && (usernames.length > 0)) {
                    for (int i = 0; i < usernames.length; i++) {
                        peer.getPrincipals()
                            .add(new UserNamePrincipal(usernames[i]));
                        logger.info("MAPPED attribute '" + attr +
                                    "' to account '" + usernames[i] + "'");
                    }
                }
            }
        } catch (Exception e) {
            throw new IOException(e.toString());
        }
    }


    /**
     * Loads policies.  If VomsPDPPolicy is present in the PDPConfig, does
     * not consult policy files.  Otherwise, static files are used.  A
     * timestamp is taken, and the files m-times are compared each call to
     * the PDP.
     * @param configs hashmap
     * @param name chainname
     * @throws Exception exc
     */
    public void initialize(HashMap configs, String name) throws Exception {

        if (configs == null) {
            throw new Exception("no configuration object");
        }

        /* First check for VomsPDPPolicy, the dynamic policy used by
           accounts. Cannot use current solutions + attribute PDP in
           a chain, because chains are deny overrides */
        Object vomsPolicy = configs.get(VOMS_PDP_POLICY);
        if (vomsPolicy != null) {
            logger.debug( "voms policy is not null");
            try {
                this.vomsPolicy = (VomsPDPPolicy) vomsPolicy;


                // ** RETURNS **
                // If VomsPDPPolicy is present, files are not used.
                return;

            } catch (ClassCastException e) {
                logger.debug("vomsPDPPolicy is present but it is not the " +
                        "proper class, programmer error");
                throw new Exception("no vomsPDPPolicy configuration");
            }
        }


        logger.debug("Using static files");


        /*  Identity based authorization, preempts attribute based
            authorization

           Any exceptions are just logged.  */

        try {
            Object useGridmap = configs.get(CONSULT_GRIDMAP_KEY);

            if (useGridmap == null) {
                logger.debug("no use-gridmap configuration");
            } else {
                String useGridmapVal = (String) useGridmap;
                if (useGridmapVal.trim().equalsIgnoreCase("true")) {
                    Object gm = configs.get(DEFAULT_GRIDMAP);
                    if (gm == null) {
                        logger.warn("gridmap is requested but there is " +
                                "no default gridmap available");
                    } else {
                        try {
                            this.gridmap = (GridMap) gm;
                            logger.info("gridmap enabled");
                        } catch (ClassCastException e) {
                            logger.warn("default gridmap configuration " +
                                  "is present but it is not the proper " +
                                  "class (GridMap), programmer error: " +
                                  e.getMessage());
                        }
                    }
                }
            }

        } catch (ClassCastException e) {
            logger.warn("consultDefaultGridmap configuration is present " +
                    "but it is not the proper class (String), " +
                    "programmer error: " + e.getMessage());
        } catch (Exception e) {
            logger.warn(e.getMessage());
        }

        // attribute based authorization

        this.attrauthz = new ACLPDP();
        this.attrauthz.initialize(configs, name);
        

        // attribute to account mapping

        Object configFile = configs.get(ATTR_MAP_CONFIG_FILE);
        if (configFile == null) {
            logger.debug("no attribute authorization policy configuration");
        } else {
            this.attrMapFileName = (String) configFile;
            this.attrGridMapFile = findConfigFile(this.attrMapFileName);
            if (this.attrGridMapFile == null) {
                throw new Exception("can not find the given attribute " +
                        "mapping file: '" + this.attrMapFileName + "'");
            } else {
                this.attrgridmap = new GridMap();
                this.attrgridmap.load(this.attrGridMapFile);
            }
        }        

        // how to combine DN based account mappings and attributes
        
        Object andLogic = configs.get(VOMS_PDP_AND_LOGIC);
        if (andLogic != null) {
            String andStr = (String) andLogic;
            if (andStr.trim().equalsIgnoreCase("true")) {
                this.andLogic = true;
            }
        }
        if (this.andLogic) {
            logger.debug("using AND logic for DNs and attributes");
        } else {
            logger.debug("using OR logic for DNs and attributes");
        }
    }

    protected File findConfigFile(String fileName) {

        if (fileName == null) {
            return null;
        }

        File configFile = null;
        for(int i = 0; i < CONFIG_LOCATIONS.length; i++) {
            String filePath = (CONFIG_LOCATIONS[i].equals("") ? fileName :
                               CONFIG_LOCATIONS[i] + File.separator +
                               fileName);
            configFile = new File(filePath);
            logger.debug("Trying config file: " + configFile.getAbsolutePath());
            if (configFile.exists()) {
                logger.debug("Config file found: " +
                             configFile.getAbsolutePath());
                break;
            } else {
                configFile = null;
            }
        }

        return configFile;
    }

}
