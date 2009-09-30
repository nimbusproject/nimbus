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

package org.nimbustools.ctxbroker.blackboard;

import org.nimbustools.ctxbroker.generated.gt4_0.description.Requires_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.description.Provides_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.description.IdentityProvides_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.description.Provides_TypeRole;
import org.nimbustools.ctxbroker.generated.gt4_0.description.Requires_TypeIdentity;
import org.nimbustools.ctxbroker.generated.gt4_0.description.Requires_TypeRole;
import org.nimbustools.ctxbroker.generated.gt4_0.description.Requires_TypeData;
import org.nimbustools.ctxbroker.generated.gt4_0.types.ContextualizationContext;
import org.nimbustools.ctxbroker.generated.gt4_0.types.MatchedRole_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.types.Node_Type;
import org.nimbustools.ctxbroker.Identity;
import org.nimbustools.ctxbroker.ContextBrokerException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Hashtable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Enumeration;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.List;

/**
 * Each resource gets one blackboard. Setting this up to keep its own
 * Hashtable of all instances to leave room for persistence in the future.
 */
public class Blackboard {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(Blackboard.class.getName());

    // The "database" of Blackboard objects
    // String ID --> Blackboard object
    private static Hashtable all = new Hashtable(8);

    private static final Node_Type[] NO_NODES_RESPONSE = new Node_Type[0];

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private Requires_TypeIdentity[] allIdentityResponseCache = null;

    // All nodes this blackboard knows about.
    // Integer workspaceID --> Node object
    private final Hashtable allNodes = new Hashtable(64);

    // All provided roles this blackboard knows about.
    // String roleName --> ProvidedRole object
    private final Hashtable allProvidedRoles = new Hashtable(16);

    // All data this blackboard knows about.
    // String roleName --> RequiredData object
    private final Hashtable allRequiredDatas = new Hashtable(16);

    // All RequiredRole objects this blackboard knows about.
    // No key to use for a hashtable because each is not only the
    // name but host and key requirements also.  See RequiredRole
    // equals/hashCode method.  No need for a set lock because set is
    // always accessed and mutated under this.dbLock.
    private final Set allRequiredRoles = new HashSet(16);

    private boolean needsRefresh = true;

    // One lock per blackboard.  Locking "up high" for now, optimize later
    private final Object dbLock = new Object();

    // Mainly for the future DB implementation (and logging)
    private final String id;

    // flips once and only once to true when all report OK
    private boolean allOK = false;

    // flips once and only once to true when a node reports an error
    private boolean oneErrorOccured = false;

    // stopping condition + for resource prop
    private int numNodes = 0;
    private int totalNodes = 0;

    
    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    public Blackboard(String id) {
        if (id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }
        this.id = id;
    }

    
    // -------------------------------------------------------------------------
    // FACTORY
    // -------------------------------------------------------------------------

    /**
     * Get or create a blackboard for a contextualization context.
     *
     * @param id may not be null
     * @return Blackboard
     */
    public synchronized static Blackboard createOrGetBlackboard(String id) {

        if (id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }

        Blackboard bb = (Blackboard) all.get(id);
        if (bb == null) {
            bb = new Blackboard(id);
            all.put(id, bb);
            return bb;
        } else {
            return bb;
        }
    }


    // -------------------------------------------------------------------------
    // NEW CONTEXT (RESOURCE PROP STUFF)
    // -------------------------------------------------------------------------

    public ContextualizationContext newContext(boolean noMoreInjections) {

        final ContextualizationContext context = new ContextualizationContext();

        context.setNoMoreInjections(noMoreInjections);

        synchronized (this.dbLock) {

            context.setAllOK(this.allOK);
            context.setErrorPresent(this.oneErrorOccured);

            context.setComplete(this.isComplete());

            final MatchedRole_Type[] matchedRoles =
                    new MatchedRole_Type[this.allRequiredRoles.size()];
            final Iterator iter = this.allRequiredRoles.iterator();

            int idx = 0;
            while (iter.hasNext()) {
                
                final RequiredRole role = (RequiredRole) iter.next();
                final MatchedRole_Type matchedRole = new MatchedRole_Type();

                matchedRole.setName(role.getName());
                matchedRole.setNumFilledProviders(role.getFilledNum());
                matchedRole.setNumProvidersInContext(role.getProviderNum());

                matchedRoles[idx] = matchedRole;
                idx++;
            }
            
            context.setMatchedRole(matchedRoles);
        }
        return context;
    }

    boolean isComplete() {
        synchronized (this.dbLock) {
            boolean complete;
            if (this.needsRefresh()) {
                this.refresh();
                complete = !this.needsRefresh();
            } else {
                complete = true;
            }

            // maybe we haven't heard from all the agents yet
            if (this.totalNodes <= 0 ||
                    this.totalNodes != this.numNodes) {
                complete = false;
            }

            return complete;
        }
    }

    
    // -------------------------------------------------------------------------
    // MATCHING
    // -------------------------------------------------------------------------

    // assumed under lock
    private void refreshNowNeeded() {
        this.needsRefresh = true;
    }

    // assumed under lock
    private void refreshNotNeeded() {
        this.needsRefresh = false;
    }

    private boolean needsRefresh() {
        return this.needsRefresh;
    }

    // assumed under lock
    private void refresh() {

        if (!this.needsRefresh()) {
            return;
        }

        logger.debug("Beginning refresh of blackboard '" + this.id + "'");

        StringBuffer tracebuf = null;
        if (logger.isDebugEnabled()) {
            tracebuf = new StringBuffer();
            tracebuf.append("\n==========================================\n");
        }

        boolean stillNeedsRefresh = false;

        // look for provided roles not matched yet with required roles

        boolean oneNotCached = false;
        final Iterator iter = this.allRequiredRoles.iterator();
        while (iter.hasNext()) {

            final RequiredRole requiredRole = (RequiredRole) iter.next();

            if (tracebuf != null) {
                tracebuf.append("Required role: ")
                        .append(requiredRole)
                        .append("\n");
            }

            // delete old list and start over:
            requiredRole.clearProviders();

            final ProvidedRole providedRole = (ProvidedRole)
                        this.allProvidedRoles.get(requiredRole.getName());
            
            if (providedRole == null) {
                if (tracebuf != null) {
                    tracebuf.append("  - Found no providers\n");
                }
                stillNeedsRefresh = true;
                continue;
            }

            int count = 0;
            final Iterator iter2 = providedRole.getProviders();
            while (iter2.hasNext()) {
                requiredRole.addProvider((Identity) iter2.next());
                count += 1;
            }

            if (tracebuf != null) {
                tracebuf.append("  - Added ")
                        .append(count)
                        .append(" providers\n");
            }

            // Pre-fetch RequiredRole response cache, this also gives us
            // information about all identities.  Calling this with
            // suppressIncomplete=false would make no sense here.
            final Object o = requiredRole.getResponsePieces(true, false);
            if (o == null) {
                stillNeedsRefresh = true;
                oneNotCached = true;
                if (tracebuf != null) {
                    tracebuf.append("  - Its response is not entirely ")
                            .append("cachable yet\n");
                }
            }
        }
        
        if (tracebuf != null) {
            tracebuf.append(
                    "All-identities cached response:\n");
        }
        
        if (oneNotCached) {

            // If response is not cachable yet it means there is a required
            // role with an identity that has missing information, therefore
            // we cannot provide an allIdentity response yet.

            if (this.allIdentityResponseCache != null) {

                this.allIdentityResponseCache = null;
                if (tracebuf != null) {
                    tracebuf.append("  - Invalidated via oneNotCached\n");
                }
                
            } else {
                if (tracebuf != null) {
                    tracebuf.append("  - Not present\n");
                }
            }

            stillNeedsRefresh = true;
            
        } else {
            
            // The inverse is not entirely true though.  There could still be
            // identities to add to all-identities even if they fulfill no
            // current required role.
            
            // currently we assume that at least the hostname and IP must
            // exist for an identity for it to be considered valid.

            // check for any identities without IP and hostname

            boolean invalid = false;

            ArrayList allIdentities = new ArrayList();
            Enumeration nodes = this.allNodes.elements();
            while (nodes.hasMoreElements()) {
                Node node = (Node) nodes.nextElement();
                Enumeration ids = node.getIdentities();
                while (ids.hasMoreElements()) {

                    Identity id = (Identity) ids.nextElement();

                    if (id.getIp() == null) {
                        invalid = true;
                        if (tracebuf != null) {
                            tracebuf.append("Node #")
                                    .append(node.getId())
                                    .append(": Identity with interface '")
                                    .append(id.getIface())
                                    .append("' has no IP yet\n");
                        }
                    }

                    if (id.getHostname() == null) {
                        invalid = true;
                        if (tracebuf != null) {
                            tracebuf.append("Node #")
                                    .append(node.getId())
                                    .append(": Identity with interface '")
                                    .append(id.getIface())
                                    .append("' has no hostname yet\n");
                        }
                    }

                    if (invalid) {
                        break;
                    }

                    Requires_TypeIdentity responseID =
                                                new Requires_TypeIdentity();
                    responseID.setIp(id.getIp());
                    responseID.setHostname(id.getHostname());
                    responseID.setPubkey(id.getPubkey());
                    allIdentities.add(responseID);
                }
                
                if (invalid) {
                    break;
                }
            }

            if (invalid) {

                if (this.allIdentityResponseCache != null) {

                    this.allIdentityResponseCache = null;
                    if (tracebuf != null) {
                        tracebuf.append("  - Invalidated because of missing ")
                                .append("IP or hostname\n");
                    }
                    
                } else {
                    
                    if (tracebuf != null) {
                        tracebuf.append("  - Remains invalid because of ")
                                .append("missing IP or hostname\n");
                    }
                }

                stillNeedsRefresh = true;

            } else {
                
                this.allIdentityResponseCache =
                        (Requires_TypeIdentity[]) allIdentities.toArray(
                              new Requires_TypeIdentity[allIdentities.size()]);
                
                if (tracebuf != null) {
                    tracebuf.append("  - Instantiated, number of identities: ")
                            .append(this.allIdentityResponseCache.length)
                            .append("\n");
                }
            }
        }

        String tail = "\nFurther refresh";
        if (stillNeedsRefresh) {
            this.refreshNowNeeded();
        } else {
            this.refreshNotNeeded();
            tail += " not";
        }
        tail += " needed currently.\n";
        
        if (tracebuf != null) {
            logger.trace("\n\nRefreshed blackboard '" + this.id + "'" +
                             tracebuf.toString() + tail);
        } else {
            logger.debug("Refreshed blackboard '" + this.id + "'" + tail);
        }
    }


    // -------------------------------------------------------------------------
    // DATA ADDITIONS
    // -------------------------------------------------------------------------

    public void injectData(String dataName, String value)
            throws ContextBrokerException {

        if (dataName == null || dataName.trim().length() == 0) {
            // does not happen when object is created via XML (which is usual)
            throw new ContextBrokerException("Empty data element name (?)");
        }

        synchronized (this.dbLock) {
            this._newData(dataName, value);
        }
    }
    
    // -------------------------------------------------------------------------
    // NODE ADDITIONS
    // -------------------------------------------------------------------------

    /**
     * Creates new Node, treats provides and requires documents in the
     * empty, provided interpretation.
     *
     * @param workspaceID workspace ID
     * @param identities identity objects filled by factory/service.
     *        What 'is' already based on creation request or initialization.
     *        Once passed to this method, caller must discard pointers
     *        (avoids needing to clone it).
     * @param requires the provided requires document if it exists
     * @param provides the provided provides document if it exists
     * @throws ContextBrokerException illegalities
     */
    public void addWorkspace(Integer workspaceID,
                             Identity[] identities,
                             Requires_Type requires,
                             Provides_Type provides,
                             int totalNodesFromAgent)
            throws ContextBrokerException {

        if (workspaceID == null) {
            throw new IllegalArgumentException("workspaceID cannot be null");
        }

        if (provides == null && requires == null) {
            throw new IllegalArgumentException("Both provides and requires " +
                   "are null?  Don't add this workspace to the " +
                   "contextualization resource.  workspaceID #" + workspaceID);
        }

        if (identities == null || identities.length == 0) {
            throw new IllegalArgumentException("'real' identities cannot be " +
                        "null or empty, contextualization is not possible. " +
                        "If a workspace has no NICs, requires and provides " +
                        "documents should not have been given.  If they " +
                        "existed, the factory/service should have rejected " +
                        "the request. " +
                        "However this happened it is a programming error.");
        }

        synchronized (this.dbLock) {

            if (this.totalNodes > 0) {
                if (this.totalNodes != totalNodesFromAgent) {
                    throw new ContextBrokerException("Context '" +
                        this.id + "' has received a conflicting " +
                        "total node count.  Was previously " + this.totalNodes +
                        "but has received a cluster definition with a total " +
                        "of " + totalNodesFromAgent);
                }
            } else {
                this.totalNodes = totalNodesFromAgent;
            }

            this.numNodes += 1;

            if (this.numNodes > this.totalNodes) {
                throw new ContextBrokerException("Context '" +
                        this.id + "' has heard from a new agent which " +
                        "makes the total node count exceed the theoretical" +
                        "maximum from the cluster definitions (" +
                        this.totalNodes + ").");
            }

            // invalidate cache if it existed
            this.allIdentityResponseCache = null;

            Node node = (Node) this.allNodes.get(workspaceID);
            if (node != null) {
                throw new ContextBrokerException("Blackboard has " +
                        "already added node with ID #" + workspaceID);
            }

            String[] requiredDatas = null;
            if (requires != null) {
                final Requires_TypeData[] datas = requires.getData();
                if (datas != null && datas.length > 0) {

                    // set up names of data this node needs

                    requiredDatas = new String[datas.length];
                    for (int i = 0; i < datas.length; i++) {
                        final String name = datas[i].getName();
                        if (name == null || name.trim().length() == 0) {
                            // does not happen when object is created via XML (which is usual)
                            throw new ContextBrokerException("Empty data element name (?)");
                        }
                        requiredDatas[i] = name;
                    }

                    // If the contextualization definition included a value for
                    // the data already, register it into the known-data store.
                    // _intakeData also creates new RequiredData objects for any
                    // newly seen data name (no matter if the value is present
                    // or not).
                    _intakeData(datas);
                }
            }

            node = new Node(workspaceID, requiredDatas);

            if (provides != null) {
                this.handleNewProvides(node, provides, identities);
                // TODO: if problem, back out additions
            }
            if (requires != null) {
                handleNewRequires(node, requires);
            }

            this.allNodes.put(workspaceID, node);
            this.refreshNowNeeded();
        }
    }

    // no args are null
    private void handleNewProvides(Node node,
                                   Provides_Type provides,
                                   Identity[] identities)
            throws ContextBrokerException {

        int workspaceID = node.getId().intValue();

        IdentityProvides_Type[] givenIDs = provides.getIdentity();
        if (givenIDs == null || givenIDs.length == 0) {
            throw new ContextBrokerException("Provides section is " +
                    "present but has no identity elements.  Will not " +
                    "contextualize #" + workspaceID + ".");
        }

        Provides_TypeRole[] roles = provides.getRole();
        if (roles == null || roles.length == 0) {
            logger.trace("Provides section for #" + workspaceID + " has " +
                    "identities but has no role-provides elements.  " +
                    "Allowing, perhaps this is only to get identity " +
                    "into contextualization context's all-identity " +
                    "list.");
        }

        // Given ID length doesn't need to match real identity length, but it
        // does need to at least equal it.
        if (givenIDs.length > identities.length) {
            throw new ContextBrokerException("Provides section has " +
                    "more identities than the VM has NICs.  Cannot " +
                    "contextualize #" + workspaceID + ".  Provides section " +
                    "has " + givenIDs.length + " but there are only " +
                    identities.length + " NICs from Logistics.");
        }

        // match a real identity to each identity in provides

        ArrayList homeless = new ArrayList();
        for (int i = 0; i < givenIDs.length; i++) {
            IdentityProvides_Type givenID = givenIDs[i];
            String iface = givenID.get_interface();
            
            boolean matched = false;

            // (if iface is null, put it into homeless via matched being false)
            if (iface != null) {
                for (int j = 0; j < identities.length; j++) {

                    // if marked taken already:
                    if (identities[j] == null) {
                        continue;
                    }

                    if (identities[j].getIface() != null &&
                        identities[j].getIface().equals(iface)) {

                        matched = true;
                        node.addIdentity(iface, identities[j]);
                        identities[j] = null; // mark this as taken
                    }
                }
            }

            if (!matched) {
                homeless.add(givenID);
            }
        }

        // only handles allocate+configure

        if (!homeless.isEmpty()) {
            placeHomelessGivens(node, homeless, identities);
        }

        // sanity check
        if (node.numIdentities() != givenIDs.length) {
            throw new ContextBrokerException("Programming error, " +
                    "number of identities assigned does not equal number " +
                    "needed (?).  Cannot contextualize #" + workspaceID +
                    ". Assigned " + node.numIdentities() + " but " +
                    givenIDs.length + " were needed.");
        }

        if (logger.isDebugEnabled()) {
            int count = 0;
            StringBuffer buf = new StringBuffer();
            for (int i = 0; i < identities.length; i++) {
                if (identities[i] != null) {
                    count += 1;
                    buf.append("NIC not matched: ")
                       .append(identities[i])
                       .append("\n");
                }
            }
            if (count == 0) {
                logger.trace("CTX-" + workspaceID + ": " +
                             "All NICs were matched.");
            } else {
                logger.trace("CTX=" + workspaceID + ": " + count + " Some " +
                             "NICs were not matched:\n" + buf.toString());
            }
        }

        if (roles != null && roles.length > 0) {
            this.handleProvidesRoles(node, roles);
        }
    }

    // no args are null, but some identity array entries could be
    // returns number of identities not matched to a given (for sanity check)
    private static void placeHomelessGivens(Node node,
                                            ArrayList homeless,
                                            Identity[] identities)
            throws ContextBrokerException {

        Iterator iter = homeless.iterator();
        while (iter.hasNext()) {
            IdentityProvides_Type givenID =
                            (IdentityProvides_Type) iter.next();

            if (givenID.get_interface() != null) {
                throw new ContextBrokerException("An interface " +
                        "specified in the provides section has no match in " +
                        "logistics: '" + givenID.get_interface() + "'");
            }

            boolean matched = false;
            for (int i = 0; i < identities.length; i++) {

                // if not marked as taken:
                if (identities[i] != null) {
                    matched = true;
                    node.addIdentity(identities[i].getIface(), identities[i]);
                    // no need to mark as null anymore except for logging
                    identities[i] = null;
                }
            }
            if (!matched) {
                // Should not happen, we know there must be enough left in
                // identities.  In the future when more is looked at to find
                // a homeless match (see EC2FUDGE comments above), this may
                // be a real possibility.
                throw new ContextBrokerException("Could not find an " +
                        "identity to match with given");
            }
        }
    }

    // no args are null and roles.length > 0
    private void handleProvidesRoles(Node node,
                                     Provides_TypeRole[] roles)
            throws ContextBrokerException {

        for (int i = 0; i < roles.length; i++) {

            String roleName = roles[i].get_value();
            if (roleName == null || roleName.trim().equals("")) {
                // would not happen when object is created via XML
                throw new ContextBrokerException("Empty role name (?)");
            }

            // we are still under this.dbLock lock, check then act here is ok
            ProvidedRole role =
                    (ProvidedRole) this.allProvidedRoles.get(roleName);
            if (role == null) {
                role = new ProvidedRole(roleName);
                this.allProvidedRoles.put(roleName, role);
            }

            final String iface = roles[i].get_interface();
            if (iface != null) {

                // only this specific interface provides the role
                
                final Identity identity = node.getParticularIdentity(iface);
                if (identity == null) {
                    throw new ContextBrokerException("There is an " +
                            "interface specification ('" + iface + "') in " +
                            "the provides section for role '" + roleName +
                            "' that does not have matching identity element " +
                            "with that interface name.  Cannot " +
                            "contextualize #" + node.getId());
                }
                role.addProvider(identity);
                
            } else {

                // each identity provides this role

                final Enumeration identities = node.getIdentities();
                while (identities.hasMoreElements()) {
                    role.addProvider((Identity)identities.nextElement());
                }
            }
        }
    }

    // no args are null
    private void handleNewRequires(Node node,
                                   Requires_Type requires)
            throws ContextBrokerException {

        final int workspaceID = node.getId().intValue();

        final Requires_TypeIdentity[] givenID = requires.getIdentity();
        if (givenID == null || givenID.length == 0) {
            
            logger.trace("#" + workspaceID + " does not require all " +
                    "identities, no identity element in given requires " +
                    "section");

            node.setAllIdentitiesRequired(false);

        } else {

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

            node.setAllIdentitiesRequired(true);
        }

        final Requires_TypeRole[] requiredRoles = requires.getRole();

        if (requiredRoles != null && requiredRoles.length > 0) {
            
            this.handleRequiredRoles(node, requiredRoles);

        } else if (logger.isTraceEnabled()) {
            
            logger.trace("Requires section for #" + workspaceID + " has " +
                    "no role-required elements." +
                    "  Allowing, perhaps the only thing required by " +
                    "this node is the contextualization context's " +
                    "all-identity list and/or just data elements.");
        }
    }

    // no args are null and datas.length > 0, always call under this.dbLock
    private void _intakeData(Requires_TypeData[] datas)
            throws ContextBrokerException {

        for (int i = 0; i < datas.length; i++) {

            final String dataName = datas[i].getName();

            if (dataName == null || dataName.trim().length() == 0) {
                // does not happen when object is created via XML (which is usual)
                throw new ContextBrokerException("Empty data element name (?)");
            }
            this._newData(dataName, datas[i].get_value());
        }
    }

    private void _newData(String name, String value)
            throws ContextBrokerException {

        if (name == null) {
            throw new IllegalArgumentException("name may not be null");
        }

        // we are under this.dbLock lock, check then act here is ok
        RequiredData data = (RequiredData) this.allRequiredDatas.get(name);
        
        if (data == null) {
            data = new RequiredData(name);
            this.allRequiredDatas.put(name, data);
        }

        if (value != null) {
            data.addNewValue(value);
        }
    }

    private boolean isOneDataValuePresent(String dataname) {
        final RequiredData data =
                (RequiredData) this.allRequiredDatas.get(dataname);
        return data != null && data.numValues() > 0;
    }

    // returns List of Requires_TypeData
    private List getDataValues(String dataname) {
        final RequiredData data =
                (RequiredData) this.allRequiredDatas.get(dataname);
        if (data != null) {
            return data.getDataList();
        } else {
            return null;
        }
    }

    // no args are null and roles.length > 0
    private void handleRequiredRoles(Node node,
                                     Requires_TypeRole[] roles)
            throws ContextBrokerException {


        // SAMPLE
        //   <requires>
        //     <identity />
        //     <role name="torqueserver" hostname="true" pubkey="true" />
        //     <role name="nfsserver" />
        //  </requires>

        for (int i = 0; i < roles.length; i++) {

            // name attribute is relevant for given requires roles, NOT value
            final String roleName = roles[i].getName();
            if (roleName == null || roleName.trim().equals("")) {
                // does not happen when object is created via XML (which is usual)
                throw new ContextBrokerException("Empty role name (?)");
            }

            boolean hostRequired = false;
            if (roles[i].getHostname() != null &&
                roles[i].getHostname().booleanValue()) {
                hostRequired = true;
            }

            boolean keyRequired = false;
            if (roles[i].getPubkey() != null &&
                roles[i].getPubkey().booleanValue()) {
                keyRequired = true;
            }

            // we are still under this.dbLock lock, check then act here is ok

            // Create potential new one and use hashCode() to see if it already
            // exists (if so, throw away reference).  Not going to be 100s of
            // roles (yet...).
            RequiredRole role = new RequiredRole(roleName,
                                                 hostRequired,
                                                 keyRequired);

            // We expect a lot of duplicates, HashSet does not add if exists
            // in list already.
            boolean didNotExist = this.allRequiredRoles.add(role);
            if (didNotExist && logger.isTraceEnabled()) {
                logger.trace("Found new RequiredRole for blackboard '" +
                             this.id + "': " + role + " -- cardinality now " +
                             this.allRequiredRoles.size());
            }

            // Copy reference to node specific list.
            // There is a possibility client provided requires schema with
            // multiple duplicate elements for some reason.  Just silently
            // allowing that (i.e. with server side log message and not
            // exception client will see).

            if (!didNotExist) {
                // It existed already.  We need to add the exact object to the
                // node's required role's, not the duplicate we created above.
                boolean found = false;
                final Iterator iter = this.allRequiredRoles.iterator();
                while (iter.hasNext()) {
                    final RequiredRole aRole = (RequiredRole) iter.next();
                    if (aRole.equals(role)) {
                        role = aRole;
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    throw new ContextBrokerException("Set contained a " +
                           "required role already but we cannot find the " +
                           "object in the set's iterator (?).  Role: " + role);
                }
            }
            
            didNotExist = node.addRequiredRole(role);
            if (!didNotExist) {
                logger.warn("Client provided requires document with " +
                        "duplicate required role elements.  That is not " +
                        "expected but ignoring and handling it. Required " +
                        "role that trigged this: " + role + ".  Node #" +
                        node.getId());
            }
        }
    }


    // -------------------------------------------------------------------------
    // NODE RETRIEVAL/UPDATES
    // -------------------------------------------------------------------------

    // (no service or client updates, VM assertions allowed)
    
    public Requires_Type retrieve(Integer workspaceID,
                                  IdentityProvides_Type[] identities,
                                  boolean returnOK)

                throws ContextBrokerException {

        if (workspaceID == null) {
            throw new IllegalArgumentException("workspaceID cannot be null");
        }
        
        final Node node = (Node) this.allNodes.get(workspaceID);
        if (node == null) {
            throw new ContextBrokerException("Blackboard is not aware " +
                    "of node with ID #" + workspaceID);
        }

        synchronized(this.dbLock) {

            // request may send identity information
            if (identities != null) {

                // trusting nodes' self assertions are sane/correct, for now
                for (int i = 0; i < identities.length; i++) {
                    
                    final IdentityProvides_Type ident = identities[i];
                    if (ident == null) {
                        // would not happen if objects come from XML
                        logger.warn("CTX retrieve request concerning node #" +
                                node.getId() + " had null interface in list");
                        continue;
                    }
                    if (ident.get_interface() == null) {
                        logger.warn("CTX retrieve request concerning node #" +
                                node.getId() + " did not include interface");
                        continue;
                    }

                    final Identity nodeID =
                            node.getParticularIdentity(ident.get_interface());

                    if (nodeID == null) {
                        // this is OK, it means there is an interface that is
                        // not part of the provides document for this node,
                        // i.e., one that is not involved in contextualization
                        if (logger.isTraceEnabled()) {
                            logger.trace("Node #" + node.getId() +
                                 " reporting about interface '" +
                                 ident.get_interface() + "' that is not in " +
                                 "its provides document (that's OK): ip = " +
                                ident.getIp() + ", hostname = " +
                                ident.getHostname());
                        }
                        continue;
                    }

                    if (nodeID.getHostname() == null &&
                            ident.getHostname() != null) {

                        nodeID.setHostname(ident.getHostname());
                        this.refreshNowNeeded();
                    }

                    if (nodeID.getIp() == null &&
                            ident.getIp() != null) {

                        nodeID.setIp(ident.getIp());
                        this.refreshNowNeeded();
                    }

                    if (nodeID.getPubkey() == null &&
                            ident.getPubkey() != null) {

                        nodeID.setPubkey(ident.getPubkey());
                        this.refreshNowNeeded();
                    }
                }
            }
            
            if (returnOK) {
                this.refresh();
                return this.constructRetrieveReturn(node, true);
            } else {
                return null;
            }
        }
    }

    // arg can not be null, return can be null
    // only suppresIncomplete = true is handled right now.
    private Requires_Type constructRetrieveReturn(Node node,
                                                  boolean suppressIncomplete)
            throws ContextBrokerException {

        
        final Requires_Type requires = new Requires_Type();

        
        // Check if identities are available
        
        if (node.isAllIdentitiesRequired()) {
            
            if (suppressIncomplete && this.allIdentityResponseCache == null) {

                //    logger.trace("Not constructing retrieve return because " +
                //            "suppressIncomplete is true, all identities " +
                //            "list is required for this node (#" +
                //            node.getId() + ") and the identities cache is " +
                //            "invalid.");

                return null;

            } else if (!suppressIncomplete &&
                       this.allIdentityResponseCache == null) {

                // TODO: when something passes in suppressIncomplete = false
                // implement this branch ... caching all known so far perhaps

                throw new IllegalArgumentException("turning off " +
                        "suppressIncomplete is not implemented yet");

            } else {
                requires.setIdentity(this.allIdentityResponseCache);
            }
        }

        if (suppressIncomplete) {
            if (this.numNodes != this.totalNodes) {
                return null;
            }
        }


        // Check if all data is available.  At least one value of each
        // data requirement constitutes "available"

        if (suppressIncomplete) {

            final String[] reqs = node.getRequiredDataNames();
            boolean oneNotPresent = false;

            // reqs never null
            for (String req : reqs) {
                if (!this.isOneDataValuePresent(req)) {
                    oneNotPresent = true;
                    break;
                }
            }

            if (oneNotPresent) {

                if (logger.isTraceEnabled()) {
                   logger.trace("Not constructing retrieve return because " +
                            "suppressIncomplete is true, and a required " +
                            "data item for this node (#" +
                            node.getId() + ") is not present: ");
                }

                return null; // *** EARLY RETURN ***
            }
        }

        final ArrayList roleParts = new ArrayList();
        ArrayList idParts = null;
        if (!node.isAllIdentitiesRequired()) {
            idParts = new ArrayList();
        }

        final Iterator iter = node.getRequiredRoles();
        while (iter.hasNext()) {

            final RequiredRole aRole = (RequiredRole) iter.next();

            final ResponsePieces pieces =
                    aRole.getResponsePieces(suppressIncomplete,
                                            idParts != null);
            
            if (suppressIncomplete && pieces == null) {

                //logger.trace("Not constructing retrieve return because " +
                //    "suppressIncomplete is true, and a required " +
                //    "role for this node (#" +
                //    node.getId() + ") is incomplete: " + aRole);

                return null;
            }

            if (pieces.roles == null) {
                throw new ContextBrokerException("Programming problem: " +
                       "ResponsePieces should be null if roles field is null");
            }

            roleParts.addAll(Arrays.asList(pieces.roles));

            if (idParts != null) {

                if (pieces.identities == null) {
                    throw new ContextBrokerException("Programming " +
                            "problem: ResponsePieces should not have null " +
                            "identities in this situation.");
                }

                idParts.addAll(Arrays.asList(pieces.identities));
            }
        }

        // might not be any roles at all which is OK, node could just need
        // identities and/or data elements.  If suppressIncomplete is true and
        // there are no roles filled yet, there was already a return above.
        if (!roleParts.isEmpty()) {
            final Requires_TypeRole[] roles =
                (Requires_TypeRole[]) roleParts.toArray(
                                    new Requires_TypeRole[roleParts.size()]);

            requires.setRole(roles);
        }

        // If not all identities are needed, the identities that
        // fulfill each role still are:

        if (idParts != null) {
            final Requires_TypeIdentity[] ids =
                    (Requires_TypeIdentity[]) idParts.toArray(
                            new Requires_TypeIdentity[idParts.size()]);
            requires.setIdentity(ids);
            if (logger.isTraceEnabled()) {
                logger.trace("Set partial identity response for " +
                             "node #" + node.getId());
            }
        }

        final String[] reqs = node.getRequiredDataNames();
        if (reqs != null && reqs.length > 0) {

            final ArrayList reqDataList = new ArrayList();
            for (int i = 0; i < reqs.length; i++) {
                final List reqData = this.getDataValues(reqs[i]);
                if (reqData != null) {
                    reqDataList.addAll(reqData);
                }
            }

            final Requires_TypeData[] wsReqData =
                    (Requires_TypeData[]) reqDataList.toArray(
                            new Requires_TypeData[reqDataList.size()]);

            requires.setData(wsReqData);
        }

        return requires;
    }

    
    // -------------------------------------------------------------------------
    // NODE STATUS
    // -------------------------------------------------------------------------

    public void okExit(Integer workspaceID)
            throws ContextBrokerException {

        final Node node = (Node) this.allNodes.get(workspaceID); 
        if (node == null) {
            throw new ContextBrokerException("unknown workspace #" + workspaceID);
        }
        synchronized (this.dbLock) {
            CtxResult result = node.getCtxResult();

            if (result.okOccurred || result.errorOccurred) {
                throw new ContextBrokerException("already received " +
                        "exiting report from workspace #" + workspaceID);
            }

            result.okOccurred = true;

            // check if all are now OK
            for (Enumeration e = this.allNodes.elements(); e.hasMoreElements();) {
                final Node aNode = (Node) e.nextElement();
                if (!aNode.getCtxResult().okOccurred) {
                    return;
                }
            }
            this.allOK = true;
        }
    }

    public void errorExit(Integer workspaceID,
                          short exitCode,
                          String errorMessage)
            throws ContextBrokerException {

        final Node node = (Node) this.allNodes.get(workspaceID);
        if (node == null) {
            throw new ContextBrokerException("unknown workspace #" + workspaceID);
        }
        synchronized (this.dbLock) {
            CtxResult result = node.getCtxResult();

            if (result.okOccurred || result.errorOccurred) {
                throw new ContextBrokerException("already received " +
                        "exiting report from workspace #" + workspaceID);
            }

            result.errorOccurred = true;
            result.errorCode = exitCode;
            result.errorMessage = errorMessage;

            this.oneErrorOccured = true;
        }
    }
    

    public Node_Type[] identities(boolean allNodess, String host, String ip) {

        final List nodeList;
        if (allNodess) {
            nodeList = new ArrayList(this.allNodes.size());
        } else {
            nodeList = new ArrayList(1);
        }

        synchronized (this.dbLock) {

            if (allNodess) {
                for (Enumeration e = this.allNodes.elements(); e.hasMoreElements();) {
                    nodeList.add(e.nextElement());
                }
            } else {
                final Node node = this.findNode(host, ip);
                if (node != null) {
                    nodeList.add(node);
                }
            }
            
            return getNodeResponse(nodeList);
        }
    }

    private static Node_Type[] getNodeResponse(List nodeList) {
        if (nodeList.isEmpty()) {
            return NO_NODES_RESPONSE;
        }
        final List resultList = new ArrayList(nodeList.size());
        for (int i = 0; i < nodeList.size(); i++) {
            final Node node = (Node) nodeList.get(i);
            if (node != null) {
                resultList.add(getOneNodeResponse(node));
            }
        }
        return (Node_Type[]) resultList.toArray(
                        new Node_Type[resultList.size()]);
    }

    private static Node_Type getOneNodeResponse(Node node) {

        final Node_Type xmlNode = new Node_Type();

        /* identities */
        final List xmlIdentsList = new ArrayList(3);
        for (Enumeration e = node.getIdentities(); e.hasMoreElements();) {
            final Identity ident = (Identity) e.nextElement();
            if (ident != null) {
                xmlIdentsList.add(idToXML(ident));
            }
        }
        final IdentityProvides_Type[] xmlIdents = 
                (IdentityProvides_Type[]) xmlIdentsList.toArray(
                        new IdentityProvides_Type[xmlIdentsList.size()]);

        xmlNode.setIdentity(xmlIdents);

        /* status */
        final CtxResult status = node.getCtxResult();
        if (status.okOccurred) {
            xmlNode.setExited(true);
            xmlNode.setOk(true);
        } else if (status.errorOccurred) {
            xmlNode.setExited(true);
            xmlNode.setErrorCode(new Short(status.errorCode));
            xmlNode.setErrorMessage(status.errorMessage);
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

    private Node findNode(String host, String ip) {

        if (host == null && ip == null) {
            return null;
        }

        for (Enumeration e = this.allNodes.elements(); e.hasMoreElements();) {

            final Node node = (Node) e.nextElement();

            for (Enumeration e2 = node.getIdentities(); e2.hasMoreElements();) {
                final Identity ident = (Identity) e2.nextElement();

                if (host != null) {
                    if (host.equals(ident.getHostname())) {
                        return node;
                    }
                } else {
                    if (ip.equals(ident.getIp())) {
                        return node;
                    }
                }
            }
        }

        return null;
    }
    
    
}
