/*
 * Copyright 1999-2009 University of Chicago
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
package org.nimbustools.ctxbroker.rest;

import org.nimbustools.ctxbroker.blackboard.NodeStatus;

import java.util.ArrayList;
import java.util.List;

public class ContextStatus {

    private boolean isComplete;
    private boolean errorOccurred;
    private boolean allOk;
    private int nodeCount;
    private int expectedNodeCount;
    private List<NodeStatus> nodes;

    public boolean isComplete() {
        return isComplete;
    }

    public void setComplete(boolean complete) {
        isComplete = complete;
    }

    public boolean isErrorOccurred() {
        return errorOccurred;
    }

    public void setErrorOccurred(boolean errorOccurred) {
        this.errorOccurred = errorOccurred;
    }

    public boolean isAllOk() {
        return allOk;
    }

    public void setAllOk(boolean allOk) {
        this.allOk = allOk;
    }

    public int getNodeCount() {
        return nodeCount;
    }

    public void setNodeCount(int nodeCount) {
        this.nodeCount = nodeCount;
    }

    public int getExpectedNodeCount() {
        return expectedNodeCount;
    }

    public void setExpectedNodeCount(int expectedNodeCount) {
        this.expectedNodeCount = expectedNodeCount;
    }

    public List<NodeStatus> getNodes() {
        return nodes;
    }

    public void setNodes(List<NodeStatus> nodes) {
        this.nodes = nodes;
    }
}
