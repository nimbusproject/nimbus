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

package org.nimbustools.api.services.metadata;

public class MetadataServerException extends Exception {

    private final String clientVisibleMessage;

    public MetadataServerException(String message,
                                   String clientVisibleMessage) {
        super(message);
        this.clientVisibleMessage = clientVisibleMessage;
    }

    public MetadataServerException(String message) {
        super(message);
        this.clientVisibleMessage = null;
    }

    public MetadataServerException(String message, Exception e) {
        super(message, e);
        this.clientVisibleMessage = null;
    }

    public MetadataServerException(String message,
                                   String clientVisibleMessage,
                                   Exception e) {
        super(message, e);
        this.clientVisibleMessage = clientVisibleMessage;
    }

    public MetadataServerException(String message, Throwable t) {
        super(message, t);
        this.clientVisibleMessage = null;
    }

    public MetadataServerException(String message,
                                   String clientVisibleMessage,
                                   Throwable t) {
        super(message, t);
        this.clientVisibleMessage = clientVisibleMessage;
    }

    public MetadataServerException(Exception e) {
        super("", e);
        this.clientVisibleMessage = null;
    }

    public String getClientVisibleMessage() {
        return this.clientVisibleMessage;
    }
}
