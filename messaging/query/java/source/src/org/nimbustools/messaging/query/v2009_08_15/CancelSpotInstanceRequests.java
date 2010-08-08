package org.nimbustools.messaging.query.v2009_08_15;

import java.rmi.RemoteException;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.core.MultivaluedMap;

import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_06_15.CancelSpotInstanceRequestsResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_06_15.CancelSpotInstanceRequestsType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_06_15.SpotInstanceRequestIdSetType;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.ServiceRM;
import org.nimbustools.messaging.query.ElasticAction;
import org.nimbustools.messaging.query.QueryError;
import org.nimbustools.messaging.query.QueryException;


public class CancelSpotInstanceRequests implements ElasticAction {

    final ServiceRM serviceRM;
    
    public CancelSpotInstanceRequests(ServiceRM serviceRMImpl){
        this.serviceRM = serviceRMImpl;
    }
    
    @Override
    public String getName() {
        return "CancelSpotInstanceRequests";
    }
    
    @GET
    public CancelSpotInstanceRequestsResponseType handleGet(
            MultivaluedMap<String, String> form) {

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
            MultivaluedMap<String, String> form) {
        return handleGet(form);
    }    

}
