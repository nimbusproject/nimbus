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
package org.nimbustools.messaging.query.actions;

import org.nimbustools.messaging.query.ElasticAction;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.GET;
import java.util.ArrayList;

public class DescribeInstances implements ElasticAction {

    @GET
    public Response handle(@Context UriInfo info) {
        // describe instances accepts N parameters, so can't use QueryParam

        final MultivaluedMap<String,String> queryParams =
                info.getQueryParameters();
        final ArrayList<String> instanceIds = new ArrayList<String>();


        // instance IDs are passed in like InstanceId.0=asdvb&InstanceId.1=12341&...

        String id;
        int i = 0;
        while ((id = queryParams.getFirst("InstanceId."+i)) != null) {
            id = id.trim();
            if (id.length() != 0) {
                instanceIds.add(id);
            }
        }

        //TODO do something about it

        return null;
    }
}
