package org.nimbustools.messaging.query.v2009_08_15;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.DescribeSpotInstanceRequestsResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.DescribeSpotInstanceRequestsType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.SpotInstanceRequestIdSetItemType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.SpotInstanceRequestIdSetType;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.ServiceRM;
import org.nimbustools.messaging.query.ElasticAction;
import org.nimbustools.messaging.query.QueryError;
import org.nimbustools.messaging.query.QueryException;

public class DescribeSpotInstanceRequests implements ElasticAction {

    public static final String REQUEST_ID_VAR = "SpotInstanceRequestId";
    final ServiceRM serviceRM;
    
    public DescribeSpotInstanceRequests(ServiceRM serviceRMImpl){
        this.serviceRM = serviceRMImpl;
    }
    
    public String getName() {
        return "DescribeSpotInstanceRequests";
    }
    
    @GET
    public DescribeSpotInstanceRequestsResponseType handleGet(
                                            @Context UriInfo uriInfo) {

        if (uriInfo == null) {
            throw new IllegalArgumentException("uriInfo may not be null");
        }
        
        MultivaluedMap<String, String> form = uriInfo.getQueryParameters();        
        
        final DescribeSpotInstanceRequestsType request = new DescribeSpotInstanceRequestsType();
        if(form != null && !form.isEmpty()){
            SpotInstanceRequestIdSetType spotInstanceRequestIdSet = extractRequestIdSet(form);
            request.setSpotInstanceRequestIdSet(spotInstanceRequestIdSet);
        }

        try {
            return serviceRM.describeSpotInstanceRequests(request);

        } catch (RemoteException e) {
            throw new QueryException(QueryError.GeneralError, e);
        }
    }

    public static SpotInstanceRequestIdSetType extractRequestIdSet(
                                                    MultivaluedMap<String, String> form) {
        
        ArrayList<SpotInstanceRequestIdSetItemType> requestIdSet = new ArrayList<SpotInstanceRequestIdSetItemType>();            
        
        for (Entry<String,List<String>> entrySet : form.entrySet()) {
            List<String> requestId = entrySet.getValue();
            if(entrySet.getKey().startsWith(REQUEST_ID_VAR) && requestId.size() == 1) {
                SpotInstanceRequestIdSetItemType reqId = new SpotInstanceRequestIdSetItemType();
                reqId.setSpotInstanceRequestId(requestId.get(0));
                requestIdSet.add(reqId);                    
            }
        }
        
        SpotInstanceRequestIdSetType spotInstanceRequestIdSet = new SpotInstanceRequestIdSetType();
        spotInstanceRequestIdSet.setItem(requestIdSet.toArray(new SpotInstanceRequestIdSetItemType[0]));
        return spotInstanceRequestIdSet;
    }
    
    @POST
    public DescribeSpotInstanceRequestsResponseType handlePost(
                                            @Context UriInfo uriInfo) {
        return handleGet(uriInfo);
    }

}
