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
import org.globus.workspace.remoting.admin.NodeReport;
import org.globus.workspace.remoting.admin.RemoteNodeManagement;
import org.globus.workspace.remoting.admin.VmmNode;
import org.globus.workspace.scheduler.NodeExistsException;
import org.globus.workspace.scheduler.NodeInUseException;
import org.globus.workspace.scheduler.NodeManagement;
import org.globus.workspace.scheduler.defaults.ResourcepoolEntry;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DefaultRemoteNodeManagement implements RemoteNodeManagement {
    private final Gson gson;
    private final TypeToken<Collection<VmmNode>> vmmNodeCollectionTypeToken;

    private NodeManagement nodeManagement;

    public DefaultRemoteNodeManagement() {
        this.gson = new Gson();
        this.vmmNodeCollectionTypeToken = new TypeToken<Collection<VmmNode>>(){};
    }

    public void initialize() throws Exception {
         if (nodeManagement == null) {
             throw new IllegalArgumentException("nodeManagement may not be null");
         }
    }

    public String addNodes(String nodeJson) throws RemoteException {
        Collection<VmmNode> nodes = gson.fromJson(nodeJson,
                vmmNodeCollectionTypeToken.getType());

        List<NodeReport> reports = new ArrayList<NodeReport>(nodes.size());
        for (VmmNode node : nodes) {
            final String hostname = node.getHostname();

            try {
                final ResourcepoolEntry entry =
                        nodeManagement.addNode(hostname,
                        node.getPoolName(),
                        node.getNetworkAssociations(),
                        node.getMemory());

                final VmmNode resultNode = translateResourcepoolEntry(entry);
                reports.add(new NodeReport(hostname,
                        NodeReport.STATE_ADDED, resultNode));

            } catch (NodeExistsException e) {
                reports.add(new NodeReport(hostname,
                        NodeReport.STATE_NODE_EXISTS, null));
            }
        }
        return gson.toJson(reports);
    }

    public String listNodes() {

        final List<ResourcepoolEntry> entries = nodeManagement.getNodes();
        final List<VmmNode> nodes = new ArrayList<VmmNode>(entries.size());
        for (ResourcepoolEntry entry : entries) {
            nodes.add(translateResourcepoolEntry(entry));
        }

        return gson.toJson(nodes);
    }

    public String getNode(String hostname) {

        final ResourcepoolEntry entry = nodeManagement.getNode(hostname);
        return gson.toJson(translateResourcepoolEntry(entry));
    }

    public String updateNodes(String nodeJson) {
        final Collection<VmmNode> nodes = gson.fromJson(nodeJson,
                this.vmmNodeCollectionTypeToken.getType());
        List<NodeReport> reports = new ArrayList<NodeReport>(nodes.size());
        for (VmmNode node : nodes) {
            final String hostname = node.getHostname();

            try {
                nodeManagement.updateNode(translateVmmNode(node));
                reports.add(new NodeReport(hostname, NodeReport.STATE_UPDATED,
                        node));
            } catch (NodeInUseException e) {
                reports.add(
                        new NodeReport(hostname,
                                NodeReport.STATE_NODE_IN_USE, null));
            }

        }
        return gson.toJson(reports);
    }

    public String removeNode(String hostname) {
        NodeReport report = _removeNode(hostname);
        return gson.toJson(report);
    }

    private NodeReport _removeNode(String hostname) {
        String state;
        try {

            if (nodeManagement.removeNode(hostname)) {
                state = NodeReport.STATE_REMOVED;
            } else {
                state = NodeReport.STATE_NODE_NOT_FOUND;
            }
        } catch (NodeInUseException e) {
            state = NodeReport.STATE_NODE_IN_USE;
        }

        return new NodeReport(hostname, state, null);
    }

    public String removeNodes(String[] hostnames) {
        List<NodeReport> reports = new ArrayList<NodeReport>(hostnames.length);
        for (String hostname : hostnames) {
            reports.add(this._removeNode(hostname));
        }
        return gson.toJson(reports);
    }

    public NodeManagement getNodeManagement() {
        return nodeManagement;
    }

    public void setNodeManagement(NodeManagement nodeManagement) {
        this.nodeManagement = nodeManagement;
    }

    private static VmmNode translateResourcepoolEntry(ResourcepoolEntry entry) {
        if (entry == null) {
             return null;
        }
        return new VmmNode(entry.getHostname(), entry.getResourcePool(),
                entry.getMemMax(), entry.getSupportedAssociations(),
                entry.isVacant());
    }

    private static ResourcepoolEntry translateVmmNode(VmmNode node) {
        if (node == null) {
            return null;
        }
        return new ResourcepoolEntry(node.getPoolName(), node.getHostname(),
                node.getMemory(), node.getMemory(), node.getNetworkAssociations());
    }
}
