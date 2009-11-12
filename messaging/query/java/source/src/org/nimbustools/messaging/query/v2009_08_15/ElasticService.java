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
import org.nimbustools.messaging.query.*;
import static org.nimbustools.messaging.query.QueryUtils.*;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.POST;
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
                actionMap.put(action.getName(), action);
            }
    }

    @Path("/")
    public ElasticAction handleAction(@QueryParam("Action") String action) {

        final ElasticAction theAction = actionMap.get(action);
        if (theAction != null) {
            return theAction;
        }

        throw new QueryException(QueryError.InvalidAction);
    }


    // using inner classes instead of methods for this becase JAX-RS doesn't
    // provide direct support for routing based on query parameters. I think
    // it is worth the inelegance tradeoff, but we'll see.


    public class CreateKeyPair implements ElasticAction {
        public String getName() {
            return "CreateKeyPair";
        }

        @GET @POST
        public CreateKeyPairResponseType handle(@QueryParam("KeyName") String keyName) {
            assureRequiredParameter("KeyName", keyName);

            final CreateKeyPairType createKeyPairType = new CreateKeyPairType(keyName);

            try {
                return service.createKeyPair(createKeyPairType);
            } catch (RemoteException e) {
                throw new QueryException(QueryError.GeneralError, e);
            }
        }
    }

    public class DeleteKeyPair implements ElasticAction {
        public String getName() {
            return "DeleteKeyPair";
        }

        @GET @POST
        public DeleteKeyPairResponseType handle(@QueryParam("KeyName") String keyName) {
            assureRequiredParameter("KeyName", keyName);

            final DeleteKeyPairType deleteKeyPairType = new DeleteKeyPairType(keyName);

            try {
                return service.deleteKeyPair(deleteKeyPairType);
            } catch (RemoteException e) {
                throw new QueryException(QueryError.GeneralError, e);
            }
        }
    }

    public class DescribeKeyPairs implements ElasticAction {
        public String getName() { return "DescribeKeyPairs"; }

        @GET @POST
        public DescribeKeyPairsResponseType handle(@Context UriInfo uriInfo) {
            final List<String> keyNames =
                    getParameterList(uriInfo, "KeyName");

            DescribeKeyPairsItemType[] keys = new DescribeKeyPairsItemType[keyNames.size()];
            for (int i = 0; i < keys.length; i++) {
                keys[i] = new DescribeKeyPairsItemType(keyNames.get(i));
            }
            final DescribeKeyPairsInfoType keySet = new DescribeKeyPairsInfoType(keys);

            try {
                return service.describeKeyPairs(new DescribeKeyPairsType(keySet));
            } catch (RemoteException e) {
                throw new QueryException(QueryError.GeneralError, e);
            }
        }
    }


    public class RunInstances implements ElasticAction {
        public String getName() {
            return "RunInstances";
        }

        @GET @POST
        public RunInstancesResponseType handle(
                @QueryParam("ImageId") String imageId,
                @QueryParam("MinCount") String minCount,
                @QueryParam("MaxCount") String maxCount,
                @QueryParam("KeyName") String keyName,
                @QueryParam("UserData") String userData,
                @QueryParam("InstanceType") String instanceType) {
            // only including parameters that are actually used right now

            assureRequiredParameter("ImageId", imageId);
            assureRequiredParameter("MinCount", minCount);
            assureRequiredParameter("MaxCount", maxCount);

            final RunInstancesType request = new RunInstancesType();
            request.setImageId(imageId);
            request.setMinCount(getIntParameter("MinCount", minCount));
            request.setMaxCount(getIntParameter("MaxCount", maxCount));
            request.setKeyName(keyName);
            if (userData != null) {
                final UserDataType data = new UserDataType();
                data.setData(userData);
                request.setUserData(data);
            }
            request.setInstanceType(instanceType);

            try {
                return service.runInstances(request);

            } catch (RemoteException e) {
                throw new QueryException(QueryError.GeneralError, e);
            }
        }
    }

    public class RebootInstances implements ElasticAction {
        public String getName() {
            return "RebootInstances";
        }

        @GET
        @POST
        public RebootInstancesResponseType handle(@Context UriInfo uriInfo) {
            final List<String> instanceIds =
                    getParameterList(uriInfo, "InstanceId");

            if (instanceIds.size() == 0) {
                throw new QueryException(QueryError.InvalidArgument,
                        "Specify at least one instance to reboot");
            }

            RebootInstancesItemType[] items =
                    new RebootInstancesItemType[instanceIds.size()];

            for (int i = 0; i < items.length; i++) {
                items[i] = new RebootInstancesItemType(instanceIds.get(i));
            }

            RebootInstancesInfoType info = new RebootInstancesInfoType(items);
            final RebootInstancesType request = new RebootInstancesType(info);

            try {
                return service.rebootInstances(request);

            } catch (RemoteException e) {
                throw new QueryException(QueryError.GeneralError, e);
            }
        }
    }

    public class DescribeInstances implements ElasticAction {
        public String getName() {
            return "DescribeInstances";
        }

        @GET
        @POST
        public DescribeInstancesResponseType handle(@Context UriInfo uriInfo) {
            final List<String> instanceIds =
                    getParameterList(uriInfo, "InstanceId");

            final DescribeInstancesItemType[] items =
                    new DescribeInstancesItemType[instanceIds.size()];

            for (int i = 0; i < items.length; i++) {
                items[i] = new DescribeInstancesItemType(instanceIds.get(i));
            }

            final DescribeInstancesInfoType info = new DescribeInstancesInfoType(items);
            final DescribeInstancesType request = new DescribeInstancesType(info);

            try {
                return service.describeInstances(request);

            } catch (RemoteException e) {
                throw new QueryException(QueryError.GeneralError, e);
            }
        }
    }


    public class TerminateInstances implements ElasticAction {
        public String getName() {
            return "TerminateInstances";
        }

        @GET
        @POST
        public TerminateInstancesResponseType handle(@Context UriInfo uriInfo) {
            final List<String> instanceIds =
                    getParameterList(uriInfo, "InstanceId");

            if (instanceIds.size() == 0) {
                throw new QueryException(QueryError.InvalidArgument,
                        "Specify at least one instance to terminate");
            }

            TerminateInstancesItemType[] items =
                    new TerminateInstancesItemType[instanceIds.size()];

            for (int i = 0; i < items.length; i++) {
                items[i] = new TerminateInstancesItemType(instanceIds.get(i));
            }

            TerminateInstancesInfoType info = new TerminateInstancesInfoType(items);

            final TerminateInstancesType request = new TerminateInstancesType(info);

            try {
                return service.terminateInstances(request);

            } catch (RemoteException e) {
                throw new QueryException(QueryError.GeneralError, e);
            }
        }
    }

    public class DescribeImages implements ElasticAction {
        public String getName() {
            return "DescribeImages";
        }

        @GET
        @POST
        public DescribeImagesResponseType handle(
                @QueryParam("ExecutableBy") String executableBy,
                @QueryParam("ImageId") String imageId,
                @QueryParam("Owner") String owner) {

            final DescribeImagesType request = new DescribeImagesType();

            if (executableBy != null) {
                // oh wtf
                final DescribeImagesExecutableBySetType executableBySet =
                        new DescribeImagesExecutableBySetType();
                executableBySet.setItem(new DescribeImagesExecutableByType[] {
                        new DescribeImagesExecutableByType(executableBy)
                });
                request.setExecutableBySet(executableBySet);
            }

            if (imageId != null) {
                DescribeImagesInfoType imagesSet =
                        new DescribeImagesInfoType(new DescribeImagesItemType[] {
                                new DescribeImagesItemType(imageId)
                        });
                request.setImagesSet(imagesSet);
            }

            if (owner != null) {
                DescribeImagesOwnersType ownersSet =
                        new DescribeImagesOwnersType(
                                new DescribeImagesOwnerType[] {
                                        new DescribeImagesOwnerType(owner)
                                }
                        );
                request.setOwnersSet(ownersSet);
            }

            try {
                return service.describeImages(request);

            } catch (RemoteException e) {
                throw new QueryException(QueryError.GeneralError, e);
            }
        }
    }

    public class DescribeAvailabilityZones implements ElasticAction {
        public String getName() {
            return "DescribeAvailabilityZones";
        }

        @GET
        @POST
        public DescribeAvailabilityZonesResponseType handle(
                @QueryParam("ZoneName") String zoneName) {

            DescribeAvailabilityZonesType request =
                    new DescribeAvailabilityZonesType();
            if (zoneName != null) {
                DescribeAvailabilityZonesSetType zoneSet =
                        new DescribeAvailabilityZonesSetType(
                                new DescribeAvailabilityZonesSetItemType[] {
                                        new DescribeAvailabilityZonesSetItemType(zoneName)
                                }
                        );
                request.setAvailabilityZoneSet(zoneSet);
            }

            try {
                return service.describeAvailabilityZones(request);

            } catch (RemoteException e) {
                throw new QueryException(QueryError.GeneralError, e);
            }
        }
    }
}
