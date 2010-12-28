package org.nimbustools.messaging.query.v2009_08_15;

import java.rmi.RemoteException;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.CancelSpotInstanceRequestsResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.CancelSpotInstanceRequestsType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.SpotInstanceRequestIdSetType;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.ServiceRM;
import org.nimbustools.messaging.query.ElasticAction;
import org.nimbustools.messaging.query.QueryError;
import org.nimbustools.messaging.query.QueryException;


public class CancelSpotInstanceRequests implements ElasticAction {

    final ServiceRM serviceRM;
    
    public CancelSpotInstanceRequests(ServiceRM serviceRMImpl){
        this.serviceRM = serviceRMImpl;
    }
    
    public String getName() {
        return "CancelSpotInstanceRequests";
    }
    
    @GET
    public CancelSpotInstanceRequestsResponseType handleGet(
                                                @Context UriInfo uriInfo) {

        if (uriInfo == null) {
            throw new IllegalArgumentException("uriInfo may not be null");
        }
        
        MultivaluedMap<String, String> form = uriInfo.getQueryParameters();
        
        final CancelSpotInstanceRequestsType request = new CancelSpotInstanceRequestsType();
        if(form != null && !form.isEmpty()){
            SpotInstanceRequestIdSetType spotInstanceRequestIdSet = 
                                                DescribeSpotInstanceRequests.extractRequestIdSet(form);
            request.setSpotInstanceRequestIdSet(spotInstanceRequestIdSet);
        }


        try {
            return serviceRM.cancelSpotInstanceRequests(request);

        } catch (RemoteException e) {
            throw new QueryException(QueryError.GeneralError, e);
        }
    }
    @POST
    public CancelSpotInstanceRequestsResponseType handlePost(
                                                @Context UriInfo uriInfo) {
        return handleGet(uriInfo);
    }    

}
