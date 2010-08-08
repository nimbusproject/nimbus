package org.nimbustools.messaging.query.v2009_08_15;

import java.rmi.RemoteException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_06_15.DescribeSpotPriceHistoryResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_06_15.DescribeSpotPriceHistoryType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_06_15.InstanceTypeSetItemType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_06_15.InstanceTypeSetType;
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
    
    @Override
    public String getName() {
        return "DescribeSpotPriceHistory";
    }
    
    @GET
    public DescribeSpotPriceHistoryResponseType handleGet(
                            MultivaluedMap<String, String> form) {

        final DescribeSpotPriceHistoryType request = new DescribeSpotPriceHistoryType();
        if(form != null && !form.isEmpty()){
            List<String> startTime = form.remove("StartTime");
            
            if(startTime != null && startTime.size() == 1){
                String startTimeStr = startTime.get(0);
                try {
                    Calendar startDate = convertTimeStampToCalendar(startTimeStr);
                    request.setStartTime(startDate);
                } catch (ParseException e) {
                    logger.warn("Could not convert DescribeSpotPriceHistory's " +
                    		    "start time String: " + startTimeStr, e);
                }
            }
            
            List<String> endTime = form.remove("EndTime");
            
            if(startTime != null && startTime.size() == 1){
                String endTimeStr = endTime.get(0);
                try {
                    Calendar endDate = convertTimeStampToCalendar(endTimeStr);
                    request.setEndTime(endDate);
                } catch (ParseException e) {
                    logger.warn("Could not convert DescribeSpotPriceHistory's " +
                                "end time String: " + endTimeStr, e);
                }
            }            
            
            //not used yet
            form.remove("ProductDescription");
            
            InstanceTypeSetItemType[] itsit = new InstanceTypeSetItemType[form.size()];            
            
            int i=0;
            for (Entry<String,List<String>> entrySet : form.entrySet()) {
                List<String> instanceType = entrySet.getValue();
                if(entrySet.getKey().startsWith(INSTANCE_TYPE_VAR) && instanceType.size() == 1) {
                    InstanceTypeSetItemType instType = new InstanceTypeSetItemType();
                    instType.setInstanceType(instanceType.get(0));
                    itsit[i++] = instType;                    
                }
            }
            
            InstanceTypeSetType instanceTypeSet = new InstanceTypeSetType();
            instanceTypeSet.setItem(itsit);
            
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
            MultivaluedMap<String, String> form) {
        return handleGet(form);
    }    
    
    public Calendar convertTimeStampToCalendar(String timeStamp) throws ParseException  {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.S'Z'");
        Date d = sdf.parse("timeStamp");
        
        Calendar result = Calendar.getInstance();
        result.setTime(d);

        return result;
    }

}
