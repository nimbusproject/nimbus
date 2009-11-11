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
package org.nimbustools.messaging.query.v2009_08_15;

import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.*;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.service.DelegatingService;
import org.nimbustools.messaging.query.ElasticAction;
import org.nimbustools.messaging.query.ElasticVersion;
import org.nimbustools.messaging.query.QueryUtils;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;

public class ElasticService implements ElasticVersion {

    DelegatingService service;

    final HashMap<String, ElasticAction> actionMap;

    public ElasticService() {
        // terrible things
        final ElasticAction[] actions = new ElasticAction[]{
                new DeleteKeyPair(), new DescribeKeyPairs()
        };
        actionMap = new HashMap<String, ElasticAction>(actions.length);
            for (ElasticAction action : actions) {
                actionMap.put(action.getClass().getSimpleName(), action);
            }
    }


    @Path("/")
    public ElasticAction handleAction(@QueryParam("Action") String action) {


        final ElasticAction theAction = actionMap.get(action);
        if (theAction != null) {
            return theAction;
        }

        throw new WebApplicationException(300);
    }


    public class DeleteKeyPair implements ElasticAction {
        @GET
        public DeleteKeyPairResponseType handle(@QueryParam("KeyName") String keyName) {
            return new DeleteKeyPairResponseType(true, null);
        }
    }

    public class DescribeKeyPairs implements ElasticAction {
        @GET
        public DescribeKeyPairsResponseType handle(@Context UriInfo uriInfo)
                throws RemoteException {
            final List<String> keyNames =
                    QueryUtils.getParameterList(uriInfo, "KeyName");

            DescribeKeyPairsItemType[] keys = new DescribeKeyPairsItemType[keyNames.size()];
            for (int i = 0; i < keys.length; i++) {
                keys[i] = new DescribeKeyPairsItemType(keyNames.get(i));
            }
            DescribeKeyPairsInfoType keySet = new DescribeKeyPairsInfoType(keys);

            return service.describeKeyPairs(new DescribeKeyPairsType(keySet));
        }
    }

}
