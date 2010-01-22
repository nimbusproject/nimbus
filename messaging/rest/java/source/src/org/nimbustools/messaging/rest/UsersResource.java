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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.UriBuilder;
import java.util.List;
import java.lang.reflect.Method;

import static org.nimbustools.messaging.rest.ResponseUtil.JSON_CONTENT_TYPE;
import org.nimbustools.messaging.rest.repr.User;
import org.nimbustools.messaging.rest.repr.AccessKey;
import org.springframework.beans.factory.InitializingBean;

/**
 * Query and modify the collection of Users
 */
public class UsersResource implements InitializingBean{

    private UsersService usersService;
    private ResponseUtil responseUtil;

    private final Method getUserMethod;

    public UsersResource() {
        try {
            getUserMethod = this.getClass().getMethod("getUser", String.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @GET
    @Path("/")
    @Produces(JSON_CONTENT_TYPE)
    public Response getUsers() {

        List<User> users = this.usersService.getUsers();

        return this.responseUtil.createJsonResponse(users);
    }

    @GET
    @Path("/{id}")
    @Produces(JSON_CONTENT_TYPE)
    public Response getUser(@PathParam("id") String id) {

        final User user = getUserById(id);

        return this.responseUtil.createJsonResponse(user);
    }

    private User getUserById(String id) {
        User user;
        try {

            user = this.usersService.getUserById(id);

        } catch (UnknownUserException e) {
            throw new NimbusWebException(Response.Status.NOT_FOUND,
                    "No such user",e);
        }
        return user;
    }

    @POST
    @Path("/")
    @Consumes(JSON_CONTENT_TYPE)
    public Response addUser(@Context UriInfo uriInfo, String userJson) {

        User user = responseUtil.fromJson(userJson, User.class);

        //TODO validate user

        try {
            user = usersService.addUser(user);
        } catch (DuplicateUserException e) {
            throw new NimbusWebException("A user with the provided DN already exists",e);
        }

        final UriBuilder ub = uriInfo.getAbsolutePathBuilder().path(getUserMethod);
        return responseUtil.createCreatedResponse(ub.build(user.getId()), user);
    }

    @GET
    @Path("/{userId}/access_key")
    @Produces(JSON_CONTENT_TYPE)
    public Response getAccessKey(@PathParam("userId") String userId) {

        final User user = getUserById(userId);

        try {
            final AccessKey accessKey = usersService.getAccessKey(user);

            return responseUtil.createJsonResponse(accessKey);

        } catch (UnknownKeyException e) {
            throw new NimbusWebException(Response.Status.NOT_FOUND,
                    "An access key does not exist for this user", e);
        }
    }

    @POST
    @Path("/{userId}/access_key")
    @Produces(JSON_CONTENT_TYPE)
    public Response generateAccessKey(@PathParam("userId") String userId) {

        final User user = getUserById(userId);
        final AccessKey accessKey = usersService.createAccessKey(user);

        return responseUtil.createJsonResponse(accessKey);
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
