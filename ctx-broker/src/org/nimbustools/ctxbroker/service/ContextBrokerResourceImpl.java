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

package org.nimbustools.ctxbroker.service;

import org.globus.wsrf.impl.security.descriptor.ResourceSecurityDescriptor;
import org.globus.wsrf.impl.security.descriptor.ResourceSecurityConfig;
import org.globus.wsrf.impl.SimpleResourceProperty;
import org.globus.wsrf.impl.SimpleResourcePropertySet;
import org.globus.wsrf.impl.ReflectionResourceProperty;
import org.globus.wsrf.config.ConfigException;
import org.globus.wsrf.ResourcePropertySet;
import org.globus.wsrf.ResourceProperty;
import org.nimbustools.ctxbroker.security.BootstrapInformation;
import org.nimbustools.ctxbroker.Identity;
import org.nimbustools.ctxbroker.BrokerConstants;
import org.nimbustools.ctxbroker.ContextBrokerException;
import org.nimbustools.ctxbroker.generated.gt4_0.types.Node_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.types.ContextualizationContext;
import org.nimbustools.ctxbroker.generated.gt4_0.description.Requires_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.description.IdentityProvides_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.description.Provides_Type;
import org.nimbustools.ctxbroker.blackboard.Blackboard;
import org.globus.security.gridmap.GridMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Calendar;

public class ContextBrokerResourceImpl implements ContextBrokerResource {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
        LogFactory.getLog(ContextBrokerResourceImpl.class.getName());


    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private String resourceID;

    private BootstrapInformation bootstrap;

    private Blackboard blackboard;

    private boolean allowInjections;
    private boolean noMoreInjections;
    private final Object statusLock = new Object();

    // stays null
    private Calendar terminationTime;

    // currently, mgmt policy can be only one DN, the factory-create() caller
    private String creatorDN;
    private ResourceSecurityDescriptor securityDescriptor;
    private String bootstrapDN;

    // resource properties
    private ResourcePropertySet propertySet;


    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    public ContextBrokerResourceImpl() throws Exception {

        this.propertySet = new SimpleResourcePropertySet(
                                  BrokerConstants.CONTEXTUALIZATION_RP_SET);

        try {
            ResourceProperty rp = new SimpleResourceProperty(
                              BrokerConstants.RP_CONTEXTUALIZATION_CONTEXT);
            rp.add(null);
            this.propertySet.add(rp);

            rp = new ReflectionResourceProperty(
                    BrokerConstants.RP_CONTEXTUALIZATION_CONTEXT, this);
            this.propertySet.add(rp);

        } catch(Exception e) {
            logger.fatal("",e);
        }
    }


    // -------------------------------------------------------------------------
    // GENERAL INFORMATION
    // -------------------------------------------------------------------------

    // ContextBrokerResource interface
    public void setID(String id) {
        this.resourceID = id;
    }

    /**
     * Matches {@link org.globus.wsrf.ResourceKey#getValue ResoureKey.getValue()}
     *
     * ResourceIdentifier interface
     *
     * @return group id
     */
    public Object getID() {
        return this.resourceID;
    }

    /**
     * WorkspaceContextualizationResource interface
     *
     * @return BootstrapInformation, never null
     */
    public BootstrapInformation getBootstrap() {
        return this.bootstrap;
    }

    public void setBootstrap(BootstrapInformation bootstrap) {
        this.bootstrap = bootstrap;
    }


    // -------------------------------------------------------------------------
    // BLACKBOARD
    // -------------------------------------------------------------------------

    // always access it this way internally in the class or there may
    // be a check then act on the cache set
    private synchronized Blackboard getBlackboard()
                                    throws ContextBrokerException {
        if (this.resourceID == null) {
            throw new ContextBrokerException("no resource id yet (?)");
        }
        if (this.blackboard == null) {
            this.blackboard =
                    Blackboard.createOrGetBlackboard(this.resourceID);
        }
        return this.blackboard;
    }


    // -------------------------------------------------------------------------
    // INJECT
    // -------------------------------------------------------------------------

    public void injectData(String name, String value)
            throws ContextBrokerException {
        synchronized (this.statusLock) {
            this.getBlackboard().injectData(name, value);
        }
    }

    // called at create time only
    public void setAllowInjections(boolean allowInjections) {
        this.allowInjections = allowInjections;
        if (!allowInjections) {
            this.noMoreInjections = true;
        }
    }

    public void noMoreInjections() throws ContextBrokerException {

        if (!this.allowInjections) {
            throw new ContextBrokerException("Erroneous: noMoreInjections " +
                    "called but injections weren't expected in the first" +
                    "place");
        }

        synchronized (this.statusLock) {
            if (this.noMoreInjections) {
                logger.warn("noMoreInjections called but this was already " +
                        "noMoreInjections: '" + this.resourceID + "'");
            } else {
                this.noMoreInjections = true;
            }
        }
    }

    public boolean isNoMoreInjections() {
        synchronized (this.statusLock) {
            return this.noMoreInjections;
        }
    }

    // -------------------------------------------------------------------------
    // EXITING MESSAGES
    // -------------------------------------------------------------------------

    public void okExit(Integer workspaceID)
            throws ContextBrokerException {
        
        synchronized (this.statusLock) {
            this.getBlackboard().okExit(workspaceID);
        }
    }

    public void errorExit(Integer workspaceID,
                          short exitCode,
                          String errorMessage)
            throws ContextBrokerException {

        synchronized (this.statusLock) {
            this.getBlackboard().errorExit(workspaceID, exitCode, errorMessage);
        }
    }


    // -------------------------------------------------------------------------
    // IDENTITIES QUERY
    // -------------------------------------------------------------------------

    public Node_Type[] identityQueryAll()
            throws ContextBrokerException {
        synchronized (this.statusLock) {
            return this.getBlackboard().identities(true, null, null);
        }
    }

    public Node_Type[] identityQueryHost(String host)
            throws ContextBrokerException {
        synchronized (this.statusLock) {
            return this.getBlackboard().identities(false, host, null);
        }
    }

    public Node_Type[] identityQueryIP(String ip)
            throws ContextBrokerException {
        synchronized (this.statusLock) {
            return this.getBlackboard().identities(false, null, ip);
        }
    }
    
    // -------------------------------------------------------------------------
    // RETRIEVE
    // -------------------------------------------------------------------------

    // Only returning something if locked and complete.  In the future we can
    // mess with "chunks" being sent asynchronously in order to avoid provider
    // and requires deadlocks and to speed up VM configuration
    // (though increasing perhaps unecessarily the message sizes and XML
    // parsing/creation times).
    public Requires_Type retrieve(Integer workspaceID,
                                  IdentityProvides_Type[] identities)
            throws ContextBrokerException {

        synchronized (this.statusLock) {
            return this.getBlackboard().retrieve(workspaceID,
                                                 identities,
                                                 this.noMoreInjections);
        }
    }


    // -------------------------------------------------------------------------
    // ADD WORKSPACE
    // -------------------------------------------------------------------------

    public void addWorkspace(Integer workspaceID,
                             Identity[] identities,
                             Requires_Type requires,
                             Provides_Type provides,
                             int totalNodes)
            throws ContextBrokerException {

        this.getBlackboard().addWorkspace(workspaceID, identities,
                                          requires, provides, totalNodes);
    }
    

    // -------------------------------------------------------------------------
    // SECURE RESOURCE 
    // -------------------------------------------------------------------------

    // SecureResource interface
    public ResourceSecurityDescriptor getSecurityDescriptor() {
        return this.securityDescriptor;
    }

    // ContextBrokerResource interface
    public String getCreatorDN() {
        return this.creatorDN;
    }

    // ContextBrokerResource interface
    public String getBootstrapDN() {
        return this.bootstrapDN;
    }

    /**
     * Policy is the factory-create() caller that caused the resource to
     * be created as well as the bootstrap credential.
     *
     * Used for creating the resource object in the first place or bringing
     * out of persistence, no alteration of this policy is supported yet.
     *
     * WorkspaceContextualizationResource interface
     *
     * @param creatorDN factory-create() caller
     * @param bootstrapDN DN of bootstrap credential
     * @throws ConfigException if problem
     */
    public void initSecureResource(String creatorDN, String bootstrapDN)

            throws ConfigException {

        this.creatorDN = creatorDN;
        this.bootstrapDN = bootstrapDN;

        if (this.securityDescriptor == null) {
            final ResourceSecurityConfig securityConfig =
                    new ResourceSecurityConfig(
                               BrokerConstants.SERVICE_SECURITY_CONFIG);
            securityConfig.init();
            this.securityDescriptor = securityConfig.getSecurityDescriptor();
        }

        this.securityDescriptor.setInitialized(false);
        final GridMap map = new GridMap();
        if (this.creatorDN != null) {
            map.map(this.creatorDN, "fakeuserid");
        }
        if (this.bootstrapDN != null) {
            map.map(this.bootstrapDN, "fakeuserid");
        }
        this.securityDescriptor.setGridMap(map);
    }

    
    // -------------------------------------------------------------------------
    // implements ResourceProperties
    // -------------------------------------------------------------------------

    public ResourcePropertySet getResourcePropertySet() {
        return this.propertySet;
    }

    /*  For ReflectionResourceProperty RP_CONTEXTUALIZATION_CONTEXT */
    public ContextualizationContext getContextualizationContext() {
        try {
            return this.getBlackboard().newContext(this.noMoreInjections);
        } catch (ContextBrokerException e) {
            logger.error("Problem: returning null contextualization context " +
                        "RP for '" + this.resourceID + "': " + e.getMessage());
            return null;
        }
    }


    // -------------------------------------------------------------------------
    // implements ResourceLifetime
    // -------------------------------------------------------------------------

    public Calendar getCurrentTime() {
        return Calendar.getInstance();
    }

    public Calendar getTerminationTime() {
        return this.terminationTime;
    }

    public void setTerminationTime(Calendar time) {
        this.terminationTime = time;
    }
}
