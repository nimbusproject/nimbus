package org.nimbustools.messaging.query.v2009_08_15;

import static org.nimbustools.messaging.query.QueryUtils.assureRequiredParameter;

import java.math.BigInteger;
import java.rmi.RemoteException;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;

import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.LaunchSpecificationRequestType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.RequestSpotInstancesResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.RequestSpotInstancesType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.UserDataType;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.ServiceRM;
import org.nimbustools.messaging.query.ElasticAction;
import org.nimbustools.messaging.query.QueryError;
import org.nimbustools.messaging.query.QueryException;

public class RequestSpotInstances implements ElasticAction {

    final ServiceRM serviceRM;
    
    public RequestSpotInstances(ServiceRM serviceRMImpl){
        this.serviceRM = serviceRMImpl;
    }
    
    public String getName() {
        return "RequestSpotInstances";
    }
    
    @GET
    public RequestSpotInstancesResponseType handleGet(
            @FormParam("InstanceCount") String instanceCount,
            @FormParam("SpotPrice") String spotPrice,
            @FormParam("Type") String type,                     
            @FormParam("LaunchSpecification.ImageId") String imageId,            
            @FormParam("LaunchSpecification.KeyName") String keyName,
            @FormParam("LaunchSpecification.UserData") String userData,
            @FormParam("LaunchSpecification.InstanceType") String instanceType) {
        // only including parameters that are actually used right now

        assureRequiredParameter("LaunchSpecification.ImageId", imageId);
        assureRequiredParameter("SpotPrice", spotPrice);        
        
        final LaunchSpecificationRequestType launchSpec = new LaunchSpecificationRequestType();
        
        launchSpec.setImageId(imageId);
        launchSpec.setKeyName(keyName);
        if (userData != null) {
            final UserDataType data = new UserDataType();
            data.setData(userData);
            launchSpec.setUserData(data);
        }
        launchSpec.setInstanceType(instanceType);

        final RequestSpotInstancesType request = new RequestSpotInstancesType();
        request.setInstanceCount(new BigInteger(instanceCount));
        request.setSpotPrice(spotPrice);
        request.setLaunchSpecification(launchSpec);
        request.setType(type);
        
        try {
            return serviceRM.requestSpotInstances(request);

        } catch (RemoteException e) {
            throw new QueryException(QueryError.GeneralError, e);
        }
    }
    @POST
    public RequestSpotInstancesResponseType handlePost(
            @FormParam("InstanceCount") String instanceCount,
            @FormParam("SpotPrice") String spotPrice,
            @FormParam("Type") String type,                     
            @FormParam("LaunchSpecification.ImageId") String imageId,            
            @FormParam("LaunchSpecification.KeyName") String keyName,
            @FormParam("LaunchSpecification.UserData") String userData,
            @FormParam("LaunchSpecification.InstanceType") String instanceType) {
        return handleGet(instanceCount, spotPrice, type, imageId, keyName, userData, instanceType);
    }    

}
