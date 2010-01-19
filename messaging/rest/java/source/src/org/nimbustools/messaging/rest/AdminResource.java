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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import static org.nimbustools.messaging.rest.ResponseUtil.JSON_CONTENT_TYPE;
import org.nimbustools.messaging.rest.repr.User;
import org.springframework.beans.factory.InitializingBean;

import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("/admin/")
public class AdminResource implements InitializingBean {

    private static final Log logger =
            LogFactory.getLog(AdminResource.class.getName());

    //sub-resources
    private UsersResource usersResource;


    private ResponseUtil responseUtil;

    @GET
    @Path("/test")
    @Produces(JSON_CONTENT_TYPE)
    public Response test() {
        User u = new User();
        u.setId("32456743256");
        u.setName("Fakey McAllister");
        
        return responseUtil.createJsonResponse(u);
    }

    // subresources have annotated getters

    @Path("/users")
    public UsersResource getUsersResource() {
        return this.usersResource;
    }

    public void setUsersResource(UsersResource usersResource) {
        this.usersResource = usersResource;
    }

    public ResponseUtil getResponseUtil() {
        return responseUtil;
    }

    public void setResponseUtil(ResponseUtil responseUtil) {
        this.responseUtil = responseUtil;
    }

    public void afterPropertiesSet() throws Exception {
        if (responseUtil == null) {
            throw new IllegalArgumentException("responseUtil may not be null");
        }

        if (usersResource == null) {
            throw new IllegalArgumentException("usersResource may not be null");
        }
    }
}
