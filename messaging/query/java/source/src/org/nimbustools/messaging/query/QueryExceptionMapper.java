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

import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.core.Response;

public class QueryExceptionMapper implements ExceptionMapper<QueryException> {

    public Response toResponse(QueryException e) {
        //TODO do this for reals

        QueryError error = e.getError();

        String respStr = "<?xml version=\"1.0\"?>\n" +
                "<Response><Errors><Error><Code>"+error.toString()+
                "<Message></Message></Error></Errors>" +
                "<RequestID>227831ec-fbf8-47d1-9654-e4125998f8b2</RequestID></Response>";

        return Response.ok(respStr).status(400).build();
    }

}
