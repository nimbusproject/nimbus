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
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.*;
import org.nimbustools.messaging.query.*;
import static org.nimbustools.messaging.query.QueryUtils.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;

public class ElasticService implements ElasticVersion {

    private static final Log logger =
            LogFactory.getLog(ElasticService.class.getName());

    final ServiceRM serviceRM;
    final ServiceGeneral serviceGeneral;
    final ServiceImage serviceImage;
    final ServiceSecurity serviceSecurity;

    final HashMap<String, ElasticAction> actionMap;

    public ElasticService(ServiceRM serviceRM, ServiceGeneral serviceGeneral,
                          ServiceImage serviceImage, ServiceSecurity serviceSecurity) {

        if (serviceRM == null) {
            throw new IllegalArgumentException("serviceRM may not be null");
        }
        if (serviceGeneral == null) {
            throw new IllegalArgumentException("serviceGeneral may not be null");
        }
        if (serviceImage == null) {
            throw new IllegalArgumentException("serviceImage may not be null");
        }
        if (serviceSecurity == null) {
            throw new IllegalArgumentException("serviceSecurity may not be null");
        }

        logger.debug("Elastic Service creation");
        this.serviceRM = serviceRM;
        this.serviceGeneral = serviceGeneral;
        this.serviceImage = serviceImage;
        this.serviceSecurity = serviceSecurity;

        // terrible things
        final ElasticAction[] actions = new ElasticAction[]{
                new CreateKeyPair(), new DeleteKeyPair(), new DescribeKeyPairs(),
                new RunInstances(), new RebootInstances(), new DescribeInstances(),
                new TerminateInstances(), new DescribeImages(),
                new DescribeAvailabilityZones()
        };
        actionMap = new HashMap<String, ElasticAction>(actions.length);
        for (ElasticAction action : actions) {
            actionMap.put(action.getName(), action);
        }
    }

    @Path("/")
    public ElasticAction handleAction(@FormParam("Action") String action) {

        logger.info("Got "+action+" request");

        final ElasticAction theAction = actionMap.get(action);
        if (theAction != null) {
            return theAction;
        }

        throw new QueryException(QueryError.InvalidAction);
    }


    // using inner classes instead of methods for this because JAX-RS doesn't
    // provide direct support for routing based on query parameters. I think
    // it is worth the inelegance trade-off, but we'll see.


    @Path("/")
    public class CreateKeyPair implements ElasticAction {
        public String getName() {
            return "CreateKeyPair";
        }

        @GET
        public CreateKeyPairResponseType handleGet(@QueryParam("KeyName") String keyName) {
            assureRequiredParameter("KeyName", keyName);

            final CreateKeyPairType createKeyPairType = new CreateKeyPairType(keyName);

            try {
                return serviceSecurity.createKeyPair(createKeyPairType);
            } catch (RemoteException e) {
                throw new QueryException(QueryError.GeneralError, e);
            }
        }
        @POST
        public CreateKeyPairResponseType handlePost(@FormParam("KeyName") String keyName) {
            return handleGet(keyName);
        }
    }

    public class DeleteKeyPair implements ElasticAction {
        public String getName() {
            return "DeleteKeyPair";
        }

        @GET
        public DeleteKeyPairResponseType handleGet(@QueryParam("KeyName") String keyName) {
            assureRequiredParameter("KeyName", keyName);

            final DeleteKeyPairType deleteKeyPairType = new DeleteKeyPairType(keyName);

            try {
                return serviceSecurity.deleteKeyPair(deleteKeyPairType);
            } catch (RemoteException e) {
                throw new QueryException(QueryError.GeneralError, e);
            }
        }
        @POST
        public DeleteKeyPairResponseType handlePost(@FormParam("KeyName") String keyName) {
            return handleGet(keyName);
        }
    }

    public class DescribeKeyPairs implements ElasticAction {
        public String getName() { return "DescribeKeyPairs"; }

        @GET
        public DescribeKeyPairsResponseType handleGet(@Context UriInfo uriInfo) {
            final List<String> keyNames =
                    getParameterList(uriInfo, "KeyName");

            DescribeKeyPairsItemType[] keys = new DescribeKeyPairsItemType[keyNames.size()];
            for (int i = 0; i < keys.length; i++) {
                keys[i] = new DescribeKeyPairsItemType(keyNames.get(i));
            }
            final DescribeKeyPairsInfoType keySet = new DescribeKeyPairsInfoType(keys);

            try {
                return serviceSecurity.describeKeyPairs(new DescribeKeyPairsType(keySet));
            } catch (RemoteException e) {
                throw new QueryException(QueryError.GeneralError, e);
            }
        }
        @POST
        public DescribeKeyPairsResponseType handlePost(@Context UriInfo uriInfo) {
            return handleGet(uriInfo);
        }
    }

    public class RunInstances implements ElasticAction {
        public String getName() {
            return "RunInstances";
        }

        @GET
        public RunInstancesResponseType handleGet(
                @FormParam("ImageId") String imageId,
                @FormParam("MinCount") String minCount,
                @FormParam("MaxCount") String maxCount,
                @FormParam("KeyName") String keyName,
                @FormParam("UserData") String userData,
                @FormParam("InstanceType") String instanceType) {
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
                return serviceRM.runInstances(request);

            } catch (RemoteException e) {
                throw new QueryException(QueryError.GeneralError, e);
            }
        }
        @POST
        public RunInstancesResponseType handlePost(
                @FormParam("ImageId") String imageId,
                @FormParam("MinCount") String minCount,
                @FormParam("MaxCount") String maxCount,
                @FormParam("KeyName") String keyName,
                @FormParam("UserData") String userData,
                @FormParam("InstanceType") String instanceType) {
            return handleGet(imageId, minCount, maxCount, keyName, userData, instanceType);
        }
    }

    public class RebootInstances implements ElasticAction {
        public String getName() {
            return "RebootInstances";
        }

        @GET
        public RebootInstancesResponseType handleGet(@Context UriInfo uriInfo) {
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
                return serviceRM.rebootInstances(request);

            } catch (RemoteException e) {
                throw new QueryException(QueryError.GeneralError, e);
            }
        }
        @POST
        public RebootInstancesResponseType handlePost(@Context UriInfo uriInfo) {
            return handleGet(uriInfo);
        }
    }

    public class DescribeInstances implements ElasticAction {
        public String getName() {
            return "DescribeInstances";
        }

        @GET
        public DescribeInstancesResponseType handleGet(@Context UriInfo uriInfo) {
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
                return serviceRM.describeInstances(request);

            } catch (RemoteException e) {
                throw new QueryException(QueryError.GeneralError, e);
            }
        }
        @POST
        public DescribeInstancesResponseType handlePost(@Context UriInfo uriInfo) {
            return handleGet(uriInfo);
        }
    }

    public class TerminateInstances implements ElasticAction {
        public String getName() {
            return "TerminateInstances";
        }

        @GET
        public TerminateInstancesResponseType handleGet(@Context UriInfo uriInfo) {
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
                return serviceRM.terminateInstances(request);

            } catch (RemoteException e) {
                throw new QueryException(QueryError.GeneralError, e);
            }
        }
        @POST
        public TerminateInstancesResponseType handlePost(@Context UriInfo uriInfo) {
            return handleGet(uriInfo);
        }
    }

    public class DescribeImages implements ElasticAction {
        public String getName() {
            return "DescribeImages";
        }

        @GET
        public DescribeImagesResponseType handleGet(
                @FormParam("ExecutableBy") String executableBy,
                @FormParam("ImageId") String imageId,
                @FormParam("Owner") String owner) {

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
                return serviceImage.describeImages(request);

            } catch (RemoteException e) {
                throw new QueryException(QueryError.GeneralError, e);
            }
        }
        @POST
        public DescribeImagesResponseType handlePost(
                @FormParam("ExecutableBy") String executableBy,
                @FormParam("ImageId") String imageId,
                @FormParam("Owner") String owner) {
            return handleGet(executableBy, imageId, owner);
        }
    }

    public class DescribeAvailabilityZones implements ElasticAction {
        public String getName() {
            return "DescribeAvailabilityZones";
        }

        @GET
        public DescribeAvailabilityZonesResponseType handleGet(
                @FormParam("ZoneName") String zoneName) {

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
                return serviceGeneral.describeAvailabilityZones(request);

            } catch (RemoteException e) {
                throw new QueryException(QueryError.GeneralError, e);
            }
        }
        @POST
        public DescribeAvailabilityZonesResponseType handlePost(
                @FormParam("ZoneName") String zoneName) {
            return handleGet(zoneName);
        }
    }
}
