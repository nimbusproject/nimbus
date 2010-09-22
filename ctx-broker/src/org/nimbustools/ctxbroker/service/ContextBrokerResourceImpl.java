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
import org.nimbustools.ctxbroker.generated.gt4_0.description.*;
import org.nimbustools.ctxbroker.blackboard.*;
import org.globus.security.gridmap.GridMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Calendar;
import java.util.List;
import java.util.ArrayList;

public class ContextBrokerResourceImpl implements ContextBrokerResource {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
        LogFactory.getLog(ContextBrokerResourceImpl.class.getName());

    private static final Node_Type[] NO_NODES_RESPONSE = new Node_Type[0];


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
    public synchronized Blackboard getBlackboard()
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
            final List<NodeStatus> nodes =
                    this.getBlackboard().identities(true, null, null);
            return getNodeResponse(nodes);
        }
    }

    public Node_Type[] identityQueryHost(String host)
            throws ContextBrokerException {
        synchronized (this.statusLock) {
            final List<NodeStatus> nodes =
                    this.getBlackboard().identities(false, host, null);
            return getNodeResponse(nodes);
        }
    }

    public Node_Type[] identityQueryIP(String ip)
            throws ContextBrokerException {
        synchronized (this.statusLock) {
            final List<NodeStatus> nodes =
                    this.getBlackboard().identities(false, null, ip);
            return getNodeResponse(nodes);
        }
    }

    private static Node_Type[] getNodeResponse(List<NodeStatus> nodeList) {
        if (nodeList.isEmpty()) {
            return NO_NODES_RESPONSE;
        }
        final List<Node_Type> resultList = new ArrayList<Node_Type>(nodeList.size());
        for (final NodeStatus node : nodeList) {
            if (node != null) {
                resultList.add(getOneNodeResponse(node));
            }
        }
        return resultList.toArray(
                        new Node_Type[resultList.size()]);
    }

    private static Node_Type getOneNodeResponse(NodeStatus node) {

        final Node_Type xmlNode = new Node_Type();

        /* identities */
        final List<IdentityProvides_Type> xmlIdentsList = new ArrayList<IdentityProvides_Type>(3);
        for (Identity ident : node.getIdentities()) {
            if (ident != null) {
                xmlIdentsList.add(idToXML(ident));
            }
        }
        final IdentityProvides_Type[] xmlIdents =
                xmlIdentsList.toArray(
                        new IdentityProvides_Type[xmlIdentsList.size()]);

        xmlNode.setIdentity(xmlIdents);

        /* status */
        if (node.isOkOccurred()) {
            xmlNode.setExited(true);
            xmlNode.setOk(true);
        } else if (node.isErrorOccurred()) {
            xmlNode.setExited(true);
            xmlNode.setErrorCode(node.getErrorCode());
            xmlNode.setErrorMessage(node.getErrorMessage());
        } else {
            xmlNode.setExited(false);
        }

        return xmlNode;
    }

    private static IdentityProvides_Type idToXML(Identity ident) {
        final IdentityProvides_Type xml = new IdentityProvides_Type();
        xml.set_interface(ident.getIface());
        xml.setHostname(ident.getHostname());
        xml.setIp(ident.getIp());
        xml.setPubkey(ident.getPubkey());
        return xml;
    }
    
    // -------------------------------------------------------------------------
    // RETRIEVE
    // -------------------------------------------------------------------------

    // Only returning something if locked and complete.  In the future we can
    // mess with "chunks" being sent asynchronously in order to avoid provider
    // and requires deadlocks and to speed up VM configuration
    // (though increasing perhaps unecessarily the message sizes and XML
    // parsing/creation times).
    public Requires_Type retrieve(Integer workspaceID)
            throws ContextBrokerException {

        synchronized (this.statusLock) {
            if (this.noMoreInjections) {
                final NodeManifest nodeManifest =
                        this.getBlackboard().retrieve(workspaceID);

                if (nodeManifest == null) {
                    return null;
                }

                return translateNodeManifest(nodeManifest);
            }
            return null;
        }
    }

    private Requires_Type translateNodeManifest(NodeManifest manifest) {
        Requires_Type requires = new Requires_Type();

        final Requires_TypeRole[] roles =
                new Requires_TypeRole[manifest.getRequiredRoles().size()];
        for (int i = 0; i < roles.length; i++) {
            final RoleIdentityPair roleIdentityPair =
                    manifest.getRequiredRoles().get(i);

            final Requires_TypeRole role = new Requires_TypeRole();
            role.setName(roleIdentityPair.getRole());
            role.set_value(roleIdentityPair.getIdentity().getIp());

            roles[i] = role;
        }
        requires.setRole(roles);

        final Requires_TypeData[] datas =
                new Requires_TypeData[manifest.getData().size()];
        for (int i = 0; i < datas.length; i++) {
            final DataPair dataPair = manifest.getData().get(i);

            final Requires_TypeData data = new Requires_TypeData();
            data.setName(dataPair.getName());
            data.set_value(dataPair.getValue());
            datas[i] = data;
        }
        requires.setData(datas);

        final Requires_TypeIdentity[] ids =
                new Requires_TypeIdentity[manifest.getIdentities().size()];
        for (int i = 0; i < ids.length; i++) {
            final Identity identity = manifest.getIdentities().get(i);

            ids[i] = new Requires_TypeIdentity(
                    identity.getHostname(),
                    identity.getIp(),
                    identity.getPubkey());
        }
        requires.setIdentity(ids);

        return requires;
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

        if (provides == null && requires == null) {
            throw new IllegalArgumentException("Both provides and requires " +
                   "are null?  Don't add this workspace to the " +
                   "contextualization resource.  workspaceID #" + workspaceID);
        }

        boolean allIdentitiesRequired = false;

        if(requires != null){
            final Requires_TypeIdentity[] givenID = requires.getIdentity();

            if(givenID != null && givenID.length > 0){
                // next two exceptions are for forwards compatibility where it
                // may be possible to specify specific identities required
                // (without going through role finding which will always place
                // identities in the filled requires document for a role,
                // regardless if all identities are required or not).

                if (givenID.length > 1) {
                    throw new ContextBrokerException("Given requires " +
                            "section has multiple identity elements? Currently " +
                            "only supporting zero or one empty identity element " +
                            "in requires section (which signals all identities " +
                            "are desired).  Will not contextualize #" +
                            workspaceID + ".");
                }

                if (givenID[0].getHostname() != null ||
                        givenID[0].getIp() != null ||
                        givenID[0].getPubkey() != null) {

                    throw new ContextBrokerException("Given requires " +
                            "section has an identity element with information " +
                            "in it? Currently only supporting zero or one " +
                            "*empty* identity element in requires section " +
                            "(which signals all identities are desired).  Will " +
                            "not contextualize #" + workspaceID + ".");
                }

                allIdentitiesRequired = true;
            }
        }

        if(!allIdentitiesRequired){
            logger.trace("#" + workspaceID + " does not require all " +
                    "identities, no identity element in given requires " +
            "section");
        }

        this.getBlackboard().addWorkspace(
                workspaceID,
                identities,
                allIdentitiesRequired,
                getRequiredRoles(workspaceID, requires),
                getDataPairs(requires),
                getProvidedRoleDescriptions(workspaceID, provides),
                totalNodes);
    }

    private ProvidedRoleDescription[] getProvidedRoleDescriptions(Integer workspaceID,
                                                                  Provides_Type provides) {

        if(provides != null){
            Provides_TypeRole[] roles = provides.getRole();

            if (roles != null && roles.length > 0){
                ProvidedRoleDescription[] roleDescs = new ProvidedRoleDescription[roles.length];

                for (int i = 0; i < roles.length; i++) {
                    Provides_TypeRole role = roles[i];
                    roleDescs[i] = new ProvidedRoleDescription(role.get_value(), role.get_interface());
                }

                return roleDescs;
            }
        }

        //If there are no provided roles specified

        if (logger.isTraceEnabled()) {
            logger.trace("Provides section for #" + workspaceID + " has " +
                    "identities but has no role-provides elements.  " +
                    "Allowing, perhaps this is only to get identity " +
                    "into contextualization context's all-identity " +
            "list.");
        }

        return null;
    }

    private RequiredRole[] getRequiredRoles(Integer workspaceID, Requires_Type requires)
            throws ContextBrokerException {

        if(requires != null){
            final RequiredRole[] roles;
            final Requires_TypeRole[] requiredRoles = requires.getRole();
            if (requiredRoles != null && requiredRoles.length > 0) {
                roles = new RequiredRole[requiredRoles.length];

                for (int i = 0; i < requiredRoles.length; i++) {
                    Requires_TypeRole requiredRole = requiredRoles[i];
                    roles[i] = getRequiredRole(requiredRole);
                }
                return roles;
            }
        }

        //If there are no required roles specified

        if (logger.isTraceEnabled()) {
            logger.trace("Requires section for #" + workspaceID + " has " +
                    "no role-required elements." +
                    "  Allowing, perhaps the only thing required by " +
                    "this node is the contextualization context's " +
            "all-identity list and/or just data elements.");
        }

        return null;
    }

    private DataPair[] getDataPairs(Requires_Type requires)
            throws ContextBrokerException {

        if(requires != null){
            final DataPair[] dataPairs;
            final Requires_TypeData[] datas = requires.getData();
            if (datas != null) {
                dataPairs = new DataPair[datas.length];
                for (int i = 0; i < datas.length; i++) {
                    Requires_TypeData data = datas[i];
                    final String dataName = data.getName();

                    if (dataName == null || dataName.trim().length() == 0) {
                        // does not happen when object is created via XML (which is usual)
                        throw new ContextBrokerException("Empty data element name (?)");
                    }
                    dataPairs[i] = new DataPair(dataName, data.get_value());
                }
                return dataPairs;
            }
        }

        return null;
    }

    private RequiredRole getRequiredRole(Requires_TypeRole typeRole) throws ContextBrokerException {


        // SAMPLE
        //   <requires>
        //     <identity />
        //     <role name="torqueserver" hostname="true" pubkey="true" />
        //     <role name="nfsserver" />
        //  </requires>

        // name attribute is relevant for given requires roles, NOT value
        final String roleName = typeRole.getName();
        if (roleName == null || roleName.trim().equals("")) {
            // does not happen when object is created via XML (which is usual)
            throw new ContextBrokerException("Empty role name (?)");
        }

        boolean hostRequired = false;
        if (typeRole.getHostname() != null &&
                typeRole.getHostname()) {
            hostRequired = true;
        }

        boolean keyRequired = false;
        if (typeRole.getPubkey() != null &&
                typeRole.getPubkey()) {
            keyRequired = true;
        }

        return new RequiredRole(roleName,
                hostRequired,
                keyRequired);
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

        final CtxStatus status;
        try {
            status = this.getBlackboard().getStatus();
        } catch (ContextBrokerException e) {
            logger.error("Problem: returning null contextualization context " +
                    "RP for '" + this.resourceID + "': " + e.getMessage());
            return null;
        }

        final ContextualizationContext context = new ContextualizationContext();

        context.setNoMoreInjections(noMoreInjections);

        context.setAllOK(status.isAllOk());
        context.setErrorPresent(status.isErrorOccurred());

        context.setComplete(status.isComplete());

        return context;
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
