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

import org.springframework.security.access.annotation.Secured;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import com.google.gson.Gson;

@Path("/admin/")
public class Admin {

    private static final String JSON_CONTENT_TYPE = "application/json";
    private static final Log logger =
            LogFactory.getLog(Admin.class.getName());
    
    final Gson gson = new Gson();


    @GET
    @Path("/test")
    @Produces(JSON_CONTENT_TYPE)
    public Response test() {
        User u = new User();
        u.setId("32456743256");
        u.setName("Fakey McAllister");
        
        return createJsonResponse(u);
    }


    private Response createJsonResponse(Object obj) {

        final String json;
        try {
            json = gson.toJson(obj);
        } catch (Exception e) {
            logger.error("Failed to convert response to JSON! Bug!", e);
            return createServerErrorResponse();
        }

        return Response.ok(json, JSON_CONTENT_TYPE).build();
    }

    private Response createServerErrorResponse() {
        return Response.serverError().build();
    }
}
