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

@Provider
public class QueryExceptionMapper implements ExceptionMapper<QueryException> {

    private static final Log logger =
            LogFactory.getLog(QueryExceptionMapper.class.getName());

    public Response toResponse(QueryException e) {
        //TODO do this for reals, figure out requestID business
        

        QueryError error = e.getError();

        logger.warn("Responding with "+error.toString()+" error for request", e);

        String respStr = "<?xml version=\"1.0\"?>\n" +
                "<Response><Errors><Error><Code>"+error.toString()+
                "<Message>"+ e.getMessage()+"</Message></Error></Errors>" +
                "<RequestID></RequestID></Response>";

        return Response.ok(respStr).status(400).build();
    }

}
