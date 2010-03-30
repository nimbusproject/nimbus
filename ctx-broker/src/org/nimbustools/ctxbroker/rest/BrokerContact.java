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

public class BrokerContact {
    
    private final String brokerUri;
    private final String contextId;
    private final String secret;

    public BrokerContact(String brokerUri, String contextId, String secret) {
        if (brokerUri == null) {
            throw new IllegalArgumentException("brokerUri may not be null");
        }

        if (contextId == null) {
            throw new IllegalArgumentException("contextId may not be null");
        }

        if (secret == null) {
            throw new IllegalArgumentException("secret may not be null");
        }

        this.brokerUri = brokerUri;
        this.contextId = contextId;
        this.secret = secret;
    }

    public String getBrokerUri() {
        return brokerUri;
    }

    public String getContextId() {
        return contextId;
    }

    public String getSecret() {
        return secret;
    }
}
