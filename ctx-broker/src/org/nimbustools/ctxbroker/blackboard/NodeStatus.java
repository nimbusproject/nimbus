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
package org.nimbustools.ctxbroker.blackboard;

import org.nimbustools.ctxbroker.Identity;

import java.util.List;
import java.util.ArrayList;
import java.util.Enumeration;

public class NodeStatus {
    final private List<Identity> identities;
    final private boolean errorOccurred;
    final private boolean okOccurred;
    final private short errorCode;
    final private String errorMessage;


    public NodeStatus(Node node) {
        if (node == null) {
            throw new IllegalArgumentException("node may not be null");
        }

        identities = new ArrayList<Identity>();
        final Enumeration<Identity> idEnum = node.getIdentities();
        while (idEnum.hasMoreElements()) {
            Identity identity = idEnum.nextElement();
            identities.add(identity);
        }

        final CtxResult ctxResult = node.getCtxResult();
        this.okOccurred = ctxResult.hasOkOccurred();
        this.errorOccurred = ctxResult.hasErrorOccurred();

        this.errorCode = ctxResult.getErrorCode();
        this.errorMessage = ctxResult.getErrorMessage();
    }

    public List<Identity> getIdentities() {
        return identities;
    }

    public boolean isErrorOccurred() {
        return errorOccurred;
    }

    public boolean isOkOccurred() {
        return okOccurred;
    }

    public short getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
