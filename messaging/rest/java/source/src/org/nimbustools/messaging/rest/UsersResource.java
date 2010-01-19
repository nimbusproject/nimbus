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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;

import static org.nimbustools.messaging.rest.ResponseUtil.JSON_CONTENT_TYPE;
import org.nimbustools.messaging.rest.repr.User;
import org.springframework.beans.factory.InitializingBean;

/**
 * Query and modify the collection of Users
 */
public class UsersResource implements InitializingBean{

    private UsersService usersService;
    private ResponseUtil responseUtil;


    @GET
    @Path("/")
    @Produces(JSON_CONTENT_TYPE)
    Response getUsers() {

        List<User> users = this.usersService.getUsers();

        return this.responseUtil.createJsonResponse(users);
    }

    @GET
    @Path("/{id}")
    @Produces(JSON_CONTENT_TYPE)
    Response getUser(@PathParam("id") String id) {

        User user = this.usersService.getUserById(id);

        return this.responseUtil.createJsonResponse(user);

    }

    @POST
    @Path("/")
    @Consumes(JSON_CONTENT_TYPE)
    Response addUser(String userJson) {

        User user = responseUtil.fromJson(userJson, User.class);

        //TODO validate user

        usersService.addUser(user);

        return null;
    }

    public UsersService getUsersService() {
        return usersService;
    }

    public void setUsersService(UsersService usersService) {
        this.usersService = usersService;
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
    }
}
