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
package org.nimbustools.messaging.rest;

import javax.ws.rs.core.Response;
import java.util.UUID;

public class NimbusWebException extends RuntimeException {

    private final int status;
    private final String requestId;

    public NimbusWebException(Response.Status status, String msg, Throwable t) {
        super(msg, t);

        if (status == null) {
            throw new IllegalArgumentException("status may not be null");
        }

        this.status = status.ordinal();
        this.requestId = UUID.randomUUID().toString();
    }

    public NimbusWebException(int status, String msg, Throwable t) {

        super(msg, t);

        this.status = status;
        this.requestId = UUID.randomUUID().toString();
    }

    public NimbusWebException(String msg, Throwable t) {
        this(400, msg, t);
    }

    public NimbusWebException(String msg) {
        this(400, msg, null);
    }

    public int getStatus() {
        return status;
    }

    public String getRequestId() {
        return requestId;
    }
}
