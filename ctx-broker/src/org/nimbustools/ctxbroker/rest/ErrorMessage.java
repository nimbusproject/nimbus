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

import java.util.UUID;

/**
 * Simple error container shipped across the wire
 */
public class ErrorMessage {

    private final String message;
    private final String requestId;

    public ErrorMessage(String message) {
        this.message = message;
        this.requestId = UUID.randomUUID().toString();
    }

    public ErrorMessage(String msg, String requestId) {
        this.message = msg;
        this.requestId = requestId;
    }

    public String getMessage() {
        return message;
    }

    public String getRequestId() {
        return requestId;
    }

    @Override
    public String toString() {
        return "ErrorMessage{" +
                "message='" + message + '\'' +
                ", requestId='" + requestId + '\'' +
                '}';
    }
}
