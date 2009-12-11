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
package org.nimbustools.messaging.query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.core.Response;
import java.util.UUID;

@Provider
public class QueryExceptionMapper implements ExceptionMapper<QueryException> {

    private static final Log logger =
            LogFactory.getLog(QueryExceptionMapper.class.getName());

    public Response toResponse(QueryException e) {

        final String requestID = UUID.randomUUID().toString();

        QueryError error = e.getError();

        logger.warn("Responding with "+error.toString()+" error for request "+requestID, e);

        String message = e.getMessage();
        if (message == null) {
            message = getRootMessage(e);
        }

        // so simple. easier to just print out the xml for now
        // (or for ever..)
        String respStr = "<?xml version=\"1.0\"?>\n" +
                "<Response><Errors><Error><Code>"+error.toString()+"</Code>"+
                "<Message>"+ message +"</Message></Error></Errors>" +
                "<RequestID>"+requestID+"</RequestID></Response>";

        return Response.ok(respStr).status(400).build();
    }

    private static String getRootMessage(Throwable throwable) {
        if (throwable == null) {
            return "";
        }

        Throwable t = throwable;
        while (t.getCause() != null) {
            t = t.getCause();
        }
        if (t.getMessage() != null) {
            return t.getMessage();
        }
        return "";
    }

}
