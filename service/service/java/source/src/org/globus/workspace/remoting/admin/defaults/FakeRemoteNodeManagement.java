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
import org.nimbustools.api.services.admin.RemoteNodeManagement;
import org.globus.workspace.remoting.admin.VmmNode;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FakeRemoteNodeManagement implements RemoteNodeManagement {
    private final Gson gson;
    private final TypeToken<Collection<VmmNode>> vmmNodeCollectionTypeToken;

    public FakeRemoteNodeManagement() {
        this.gson = new Gson();
        this.vmmNodeCollectionTypeToken = new TypeToken<Collection<VmmNode>>(){};
    }

    private List<VmmNode> nodeList = new ArrayList<VmmNode>();

    public String addNodes(String nodeJson) throws RemoteException {
        Collection<VmmNode> nodes = gson.fromJson(nodeJson,
                vmmNodeCollectionTypeToken.getType());

        List<NodeReport> reports = new ArrayList<NodeReport>(nodes.size());
        for (VmmNode node : nodes) {
            this.nodeList.add(node);

            reports.add(new NodeReport(node.getHostname(), "ADDED", node));
        }
        return gson.toJson(reports);
    }

    public String listNodes() {
        return gson.toJson(nodeList);
    }

    public String getNode(String hostname) {
        for (VmmNode node : nodeList) {
            if (node.getHostname().equals(hostname)) {
                return gson.toJson(node);
            }
        }
        return null;
    }

    public String updateNodes(String[] hostnames,
                              Boolean active,
                              String pool,
                              Integer memory,
                              String networks)
            throws RemoteException {

        //not implemented
        return null;
    }

    public String removeNode(String hostname) {
        NodeReport report = _removeNode(hostname);
        return gson.toJson(report);
    }

    private NodeReport _removeNode(String hostname) {
        NodeReport report = null;
        for (int i = 0; i < this.nodeList.size(); i++) {
            if (nodeList.get(i).getHostname().equals(hostname)) {
                nodeList.remove(i);
                report = new NodeReport(hostname, "REMOVED", null);
            }
        }
        if (report == null) {
            report = new NodeReport(hostname, "NOT_FOUND", null);
        }
        return report;
    }

    public String removeNodes(String[] hostnames) {
        List<NodeReport> reports = new ArrayList<NodeReport>(hostnames.length);
        for (String hostname : hostnames) {
            reports.add(this._removeNode(hostname));
        }
        return gson.toJson(reports);
    }
}
