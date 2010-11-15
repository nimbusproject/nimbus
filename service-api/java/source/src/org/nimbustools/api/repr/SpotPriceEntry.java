package org.nimbustools.api.repr;

import java.util.Calendar;

public interface SpotPriceEntry {

    public Calendar getTimeStamp();
    
    public Double getSpotPrice();
}
