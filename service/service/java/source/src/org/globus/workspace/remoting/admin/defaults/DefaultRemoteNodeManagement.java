/*
 * Copyright 1999-2010 University of Chicago
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
package org.globus.workspace.remoting.admin.defaults;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.workspace.network.Association;
import org.globus.workspace.network.AssociationEntry;
import org.globus.workspace.persistence.PersistenceAdapter;
import org.globus.workspace.persistence.WorkspaceDatabaseException;
import org.globus.workspace.remoting.admin.NodeReport;
import org.nimbustools.api.services.admin.RemoteNodeManagement;
import org.globus.workspace.remoting.admin.VmmNode;
import org.globus.workspace.scheduler.NodeExistsException;
import org.globus.workspace.scheduler.NodeInUseException;
import org.globus.workspace.scheduler.NodeManagement;
import org.globus.workspace.scheduler.NodeManagementDisabled;
import org.globus.workspace.scheduler.NodeNotFoundException;
import org.globus.workspace.scheduler.defaults.ResourcepoolEntry;
import org.springframework.remoting.RemoteAccessException;

import java.rmi.RemoteException;
import java.util.*;

public class DefaultRemoteNodeManagement implements RemoteNodeManagement {

    private static final Log logger =
            LogFactory.getLog(DefaultRemoteNodeManagement.class.getName());

    private final Gson gson;
    private final TypeToken<Collection<VmmNode>> vmmNodeCollectionTypeToken;

    private NodeManagement nodeManagement = null;
    private PersistenceAdapter persistenceAdapter;

    public DefaultRemoteNodeManagement() {
        this.gson = new Gson();
        this.vmmNodeCollectionTypeToken = new TypeToken<Collection<VmmNode>>(){};
    }

    public void initialize() throws Exception {
         if (nodeManagement == null) {
             logger.warn("Node management disabled");
         }
    }

    public String addNodes(String nodeJson) throws RemoteException {

        if (nodeJson == null) {
            throw new IllegalArgumentException("nodeJson may not be null");
        }

        Collection<VmmNode> nodes = gson.fromJson(nodeJson,
                vmmNodeCollectionTypeToken.getType());

        if (nodes == null || nodes.isEmpty()) {
            throw new IllegalArgumentException(
                    "you must specify at least one node to add");
        }

        List<NodeReport> reports = new ArrayList<NodeReport>(nodes.size());
        for (VmmNode node : nodes) {
            String hostname = node.getHostname();

            if (hostname == null) {
                throw new IllegalArgumentException("hostname may not be null");
            }

            logger.info("Adding VMM node " + hostname);

            try {
                final ResourcepoolEntry entry =
                        nodeManagement.addNode(hostname,
                                node.getPoolName(),
                                node.getNetworkAssociations(),
                                node.getMemory(),
                                node.isActive());

                final VmmNode resultNode = translateResourcepoolEntry(entry);
                reports.add(new NodeReport(hostname,
                        NodeReport.STATE_ADDED, resultNode));

            } catch (NodeExistsException e) {
                logger.info("VMM node " + hostname + " already existed");
                reports.add(new NodeReport(hostname,
                        NodeReport.STATE_NODE_EXISTS, null));
            } catch (NodeManagementDisabled e) {
                throw new RemoteException(e.getMessage());
            } catch (WorkspaceDatabaseException e) {
                throw new RemoteException(e.getMessage());
            }
        }
        return gson.toJson(reports);
    }

    /**
     * If a mgmt ooperation is attempted and there is no active node management instance,
     * return a disabled message.
     */
    private void checkActive() throws RemoteException {
        if (this.nodeManagement == null) {
            throw new RemoteException("Remote node administration is disabled. " +
                    "Are you in pilot mode?");
        }
    }

    public String listNodes() throws RemoteException {

        checkActive();
        
        logger.debug("Listing VMM nodes");

        final List<ResourcepoolEntry> entries;
        try {
            entries = nodeManagement.getNodes();
        } catch (NodeManagementDisabled e) {
            throw new RemoteException(e.getMessage());
        } catch (WorkspaceDatabaseException e) {
            throw new RemoteException(e.getMessage());
        }
        final List<VmmNode> nodes = new ArrayList<VmmNode>(entries.size());
        for (ResourcepoolEntry entry : entries) {
            nodes.add(translateResourcepoolEntry(entry));
        }

        return gson.toJson(nodes);
    }

    public String getNode(String hostname) throws RemoteException {

        if (hostname == null) {
            throw new IllegalArgumentException("hostname may not be null");
        }

        logger.debug("Listing VMM node " + hostname);


        final ResourcepoolEntry entry;
        try {
            entry = nodeManagement.getNode(hostname);
        } catch (NodeManagementDisabled e) {
            throw new RemoteException(e.getMessage());
        } catch (WorkspaceDatabaseException e) {
            throw new RemoteException(e.getMessage());
        }
        return gson.toJson(translateResourcepoolEntry(entry));
    }

    public String updateNodes(String[] hostnames,
                              Boolean active,
                              String pool,
                              Integer memory,
                              String networks) throws RemoteException {

        if (hostnames == null) {
            throw new IllegalArgumentException("hostnames may not be null");
        }
        if (hostnames.length == 0) {
            throw new IllegalArgumentException(
                    "You must specify at least one VMM node to update");
        }

        if (active == null && pool == null && memory == null && networks == null) {
            throw new IllegalArgumentException(
                    "You must specify at least one node parameter to update");
        }

        final List<NodeReport> reports = new ArrayList<NodeReport>(hostnames.length);

        for (String hostname : hostnames) {
            if (hostname == null) {
                throw new IllegalArgumentException("update request has null node hostname");
            }

            logger.info("Updating VMM node: " + hostname);

            try {
                final ResourcepoolEntry entry;
                try {
                    entry = nodeManagement.updateNode(
                            hostname, pool, networks, memory, active);
                } catch (NodeManagementDisabled e) {
                    throw new RemoteException(e.getMessage());
                }

                final VmmNode node = translateResourcepoolEntry(entry);
                reports.add(new NodeReport(hostname, NodeReport.STATE_UPDATED,
                        node));
            } catch (NodeInUseException e) {
                logger.info("VMM node was in use, failed to update: " + hostname);
                reports.add(
                        new NodeReport(hostname,
                                NodeReport.STATE_NODE_IN_USE, null));
            } catch (NodeNotFoundException e) {
                logger.info("VMM node not found, failed to update: " + hostname);
                reports.add(
                        new NodeReport(hostname,
                                NodeReport.STATE_NODE_NOT_FOUND, null));
            } catch (WorkspaceDatabaseException e) {
                throw new RemoteException(e.getMessage());
            }

        }
        return gson.toJson(reports);
    }

    public String removeNode(String hostname) throws RemoteException {
        NodeReport report = _removeNode(hostname);
        return gson.toJson(report);
    }

    private NodeReport _removeNode(String hostname) throws RemoteException {
        if (hostname == null) {
            throw new IllegalArgumentException("hostname may not be null");
        }
        hostname = hostname.trim();
        if (hostname.length() == 0) {
            throw new IllegalArgumentException("hostname may not be empty");
        }
        logger.info("Removing VMM node: " + hostname);
        String state;
        try {

            if (nodeManagement.removeNode(hostname)) {
                state = NodeReport.STATE_REMOVED;
            } else {
                state = NodeReport.STATE_NODE_NOT_FOUND;
            }
        } catch (NodeInUseException e) {
            logger.warn("Node in use: " + hostname);
            state = NodeReport.STATE_NODE_IN_USE;
        } catch (NodeManagementDisabled e) {
            throw new RemoteException(e.getMessage());
        } catch (WorkspaceDatabaseException e) {
            throw new RemoteException(e.getMessage());
        }

        return new NodeReport(hostname, state, null);
    }

    public String removeNodes(String[] hostnames) throws RemoteException {
        if (hostnames == null || hostnames.length == 0) {
            throw new IllegalArgumentException("hostnames may not be null or empty");
        }
        List<NodeReport> reports = new ArrayList<NodeReport>(hostnames.length);
        for (String hostname : hostnames) {
            reports.add(this._removeNode(hostname));
        }
        return gson.toJson(reports);
    }

    public String getAllNetworkPools(int inUse) throws RemoteException {
        try {
            Hashtable cAssociations = persistenceAdapter.currentAssociations();
            List<Association> assocs = new ArrayList<Association>();
            Enumeration keys = cAssociations.keys();

            while(keys.hasMoreElements()) {
                Association a = (Association) cAssociations.get(keys.nextElement());
                assocs.add(a);
            }

            if(assocs == null || assocs.size() == 0)
                return null;

            List<AssociationEntry> allEntries = new ArrayList<AssociationEntry>();
            for(Association assoc: assocs) {
                Iterator it = assoc.getEntries().iterator();
                while(it.hasNext()) {
                    AssociationEntry next = (AssociationEntry) it.next();
                    if(inUse == ALL_ENTRIES) {
                        allEntries.add(next);
                    }
                    else if(inUse == FREE_ENTRIES) {
                        if(!next.isInUse())
                            allEntries.add(next);
                    }
                    else if(inUse == USED_ENTRIES) {
                        if(next.isInUse())
                            allEntries.add(next);
                    }
                }
            }

            if(allEntries == null || allEntries.isEmpty())
                return null;

            return gson.toJson(allEntries);
        }
        catch(WorkspaceDatabaseException e) {
            throw new RemoteException(e.getMessage());
        }
    }

    public String getNetworkPool(String pool, int inUse) throws RemoteException {
        try {
            Hashtable cAssociations = persistenceAdapter.currentAssociations();

            final Association assoc = (Association) cAssociations.get(pool);

            if (assoc == null)
                return null;

            List<AssociationEntry> entries = new ArrayList<AssociationEntry>();
            Iterator it = assoc.getEntries().iterator();
            while(it.hasNext()) {
                AssociationEntry next = (AssociationEntry) it.next();
                if(inUse == ALL_ENTRIES) {
                    entries.add(next);
                }
                else if(inUse == FREE_ENTRIES) {
                    if(!next.isInUse())
                        entries.add(next);
                }
                else if(inUse == USED_ENTRIES) {
                    if(next.isInUse())
                        entries.add(next);
                }
            }

            if (entries == null || entries.isEmpty())
                return null;

            return gson.toJson(entries);
        }
        catch (WorkspaceDatabaseException e) {
            throw new RemoteException(e.getMessage());
        }
    }

    public NodeManagement getNodeManagement() {
        return nodeManagement;
    }

    public void setNodeManagement(NodeManagement nodeManagement) {
        this.nodeManagement = nodeManagement;
    }

    public void setPersistenceAdapter(PersistenceAdapter persistenceAdapter) {
        this.persistenceAdapter = persistenceAdapter;
    }

    private static VmmNode translateResourcepoolEntry(ResourcepoolEntry entry) {
        if (entry == null) {
             return null;
        }
        return new VmmNode(entry.getHostname(), entry.isActive(),
                entry.getResourcePool(), entry.getMemMax(),
                entry.getSupportedAssociations(), entry.isVacant());
    }
}
