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
package org.globus.workspace.remoting.admin;

public class NodeReport {

    public static final String STATE_ADDED = "ADDED";
    public static final String STATE_REMOVED = "REMOVED";
    public static final String STATE_UPDATED = "UPDATED";
    public static final String STATE_NODE_EXISTS = "NODE_EXISTS";
    public static final String STATE_NODE_IN_USE = "NODE_IN_USE";
    public static final String STATE_NODE_NOT_FOUND = "NODE_NOT_FOUND";

    @SuppressWarnings({"UnusedDeclaration"}) // used by GSON deserialization
    public NodeReport() {}

    public NodeReport(String hostname, String state, VmmNode node) {
        this.hostname = hostname;
        this.state = state;
        this.node = node;
    }

    private String hostname;
    private String state;
    private VmmNode node;

    public String getHostname() {
        return hostname;
    }

    public String getState() {
        return state;
    }

    public VmmNode getNode() {
        return node;
    }
}


