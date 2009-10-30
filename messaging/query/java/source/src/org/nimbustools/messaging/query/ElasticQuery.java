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

import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import java.util.Map;

@Path("/")
public class ElasticQuery {

    Map<String,ElasticAction> actions;

    public Map<String, ElasticAction> getActions() {
        return actions;
    }

    public void setActions(Map<String, ElasticAction> actions) {
        this.actions = actions;
    }

    @Path("/")
    public ElasticAction handle(@QueryParam("Action") String action,
                       @QueryParam("Version") String version) {

        // get appropriate action
        final ElasticAction theAction = actions.get(action);

        if (theAction == null) {
            throw new WebApplicationException(500);
        }

        return theAction;
    }
}
