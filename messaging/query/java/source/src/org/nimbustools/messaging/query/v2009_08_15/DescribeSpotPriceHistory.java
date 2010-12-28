package org.nimbustools.messaging.query.v2009_08_15;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map.Entry;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.DescribeSpotPriceHistoryResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.DescribeSpotPriceHistoryType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.InstanceTypeSetItemType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.InstanceTypeSetType;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.ServiceRM;
import org.nimbustools.messaging.query.ElasticAction;
import org.nimbustools.messaging.query.QueryError;
import org.nimbustools.messaging.query.QueryException;

public class DescribeSpotPriceHistory implements ElasticAction {

    private static final String INSTANCE_TYPE_VAR = "InstanceType";

    private static final Log logger =
        LogFactory.getLog(DescribeSpotPriceHistory.class.getName());    
    
    final ServiceRM serviceRM;
    
    public DescribeSpotPriceHistory(ServiceRM serviceRMImpl){
        this.serviceRM = serviceRMImpl;
    }
    
    public String getName() {
        return "DescribeSpotPriceHistory";
    }
    
    @GET
    public DescribeSpotPriceHistoryResponseType handleGet(
                                            @Context UriInfo uriInfo) {

        MultivaluedMap<String, String> form = uriInfo.getQueryParameters();
        
        final DescribeSpotPriceHistoryType request = new DescribeSpotPriceHistoryType();
        if(form != null && !form.isEmpty()){
            List<String> startTime = form.remove("StartTime");
            
            if(startTime != null && startTime.size() == 1){
                String startTimeStr = startTime.get(0);
                try {
                    Calendar startDate = convertTimeStampToCalendar(startTimeStr);
                    request.setStartTime(startDate);
                } catch (IllegalArgumentException e) {
                    logger.warn("Could not convert DescribeSpotPriceHistory's " +
                    		    "start time String: " + startTimeStr, e);
                }
            }
            
            List<String> endTime = form.remove("EndTime");
            
            if(endTime != null && endTime.size() == 1){
                String endTimeStr = endTime.get(0);
                try {
                    Calendar endDate = convertTimeStampToCalendar(endTimeStr);
                    request.setEndTime(endDate);
                } catch (IllegalArgumentException e) {
                    logger.warn("Could not convert DescribeSpotPriceHistory's " +
                                "end time String: " + endTimeStr, e);
                }
            }            
            
            //not used yet
            form.remove("ProductDescription");
            
            ArrayList<InstanceTypeSetItemType> itsit = new ArrayList<InstanceTypeSetItemType>();            
            
            for (Entry<String,List<String>> entrySet : form.entrySet()) {
                List<String> instanceType = entrySet.getValue();
                if(entrySet.getKey().startsWith(INSTANCE_TYPE_VAR) && instanceType.size() == 1) {
                    InstanceTypeSetItemType instType = new InstanceTypeSetItemType();
                    instType.setInstanceType(instanceType.get(0));
                    itsit.add(instType);                   
                }
            }
            
            InstanceTypeSetType instanceTypeSet = new InstanceTypeSetType();
            instanceTypeSet.setItem(itsit.toArray(new InstanceTypeSetItemType[0]));
            
            request.setInstanceTypeSet(instanceTypeSet);
        }

        try {
            return serviceRM.describeSpotPriceHistory(request);

        } catch (RemoteException e) {
            throw new QueryException(QueryError.GeneralError, e);
        }
    }
    
    @POST
    public DescribeSpotPriceHistoryResponseType handlePost(
                        @Context UriInfo uriInfo) {
        return handleGet(uriInfo);
    }    
    
    public static Calendar convertTimeStampToCalendar(String timeStamp) throws IllegalArgumentException {
        DateTimeFormatter dateTime = ISODateTimeFormat.dateTimeParser();
        DateTime parsedDateTime = dateTime.parseDateTime(timeStamp);
        
        Calendar result = Calendar.getInstance();
        result.setTimeInMillis(parsedDateTime.getMillis());

        return result;
    }
}
